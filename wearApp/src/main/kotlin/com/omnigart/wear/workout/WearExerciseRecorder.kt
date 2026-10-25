package com.omnigart.wear.workout

import android.content.Context
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.google.common.util.concurrent.ListenableFuture
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferDocument
import com.omnigart.shared.logging.DEFAULT_REST_SECONDS
import com.omnigart.shared.logging.RestTimerState
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** One in-memory strength set pending transfer with the finished workout. */
data class WearLoggedStrengthSet(
    val id: String = UUID.randomUUID().toString(),
    val exerciseKey: String,
    val exerciseName: String,
    val setIndex: Int,
    val reps: Int,
    val loadKg: Double,
    val isWarmup: Boolean = false,
    val target: String? = null
)

enum class WearWorkoutMode {
    RUNNING,
    STRENGTH
}

/**
 * Records with Health Services [ExerciseClient], keeps a Wear local copy, then sends to the phone
 * via [WearPhoneWorkoutTransfer]. The Wear copy is dropped when the phone acks.
 *
 * Strength mode uses WEIGHTLIFTING / STRENGTH_TRAINING when the device supports them,
 * tracks per-set logs in memory, and transfers `activityTypeName = STRENGTH` with schema v2 sets.
 */
@Suppress("TooManyFunctions")
class WearExerciseRecorder(
    context: Context,
    private val core: OmniGartCore,
    private val transfer: WearPhoneWorkoutTransfer,
    private val exerciseClient: ExerciseClient =
        HealthServices.getClient(context.applicationContext).exerciseClient,
    private val lifecycleJob: Job = SupervisorJob()
) {
    private val scope = CoroutineScope(lifecycleJob + Dispatchers.Main.immediate)
    private val directExecutor = Executor { it.run() }
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val startedAtMs = AtomicReference<Long?>(null)
    private val accumulatedPauseMs = AtomicReference(0L)
    private val pauseBeganAtMs = AtomicReference<Long?>(null)
    private val hrBpmSamples = CopyOnWriteArrayList<Double>()
    private val activeEnergyKcal = AtomicReference(0.0)
    private val distanceMetres = AtomicReference(0.0)
    private val lastError = AtomicReference<String?>(null)
    private val mode = AtomicReference(WearWorkoutMode.RUNNING)
    private val loggedSets = CopyOnWriteArrayList<WearLoggedStrengthSet>()
    private val restTimer = AtomicReference(RestTimerState())
    private val currentExerciseName = AtomicReference("Exercise")
    private val currentExerciseKey = AtomicReference("exercise")
    private val currentSetNumber = AtomicInteger(1)
    private val weightKg = AtomicReference(60.0)
    private val reps = AtomicInteger(8)
    private val uiEpoch = AtomicInteger(0)

    private val callback =
        object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                update.latestMetrics.getData(DataType.HEART_RATE_BPM).forEach { sample ->
                    hrBpmSamples.add(sample.value)
                }
                update.latestMetrics.getData(DataType.CALORIES_TOTAL)?.let { point ->
                    activeEnergyKcal.set(point.total)
                }
                update.latestMetrics.getData(DataType.DISTANCE_TOTAL)?.let { point ->
                    distanceMetres.set(point.total)
                }
                bumpUi()
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

            override fun onRegistered() = Unit

            override fun onRegistrationFailed(throwable: Throwable) {
                lastError.set(throwable.message)
                bumpUi()
            }

            override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit
        }

    fun isRunning(): Boolean = running.get()

    fun isPaused(): Boolean = paused.get()

    fun lastError(): String? = lastError.get()

    fun mode(): WearWorkoutMode = mode.get()

    /** Active elapsed ms (wall time minus pauses), including an open pause. */
    fun elapsedActiveMs(nowMs: Long = System.currentTimeMillis()): Long {
        val started = startedAtMs.get() ?: return 0L
        return WearWorkoutElapsed.activeElapsedMs(
            nowMs = nowMs,
            startedAtMs = started,
            accumulatedPauseMs = accumulatedPauseMs.get(),
            pauseBeganAtMs = pauseBeganAtMs.get()
        )
    }

    fun loggedSets(): List<WearLoggedStrengthSet> = loggedSets.toList()

    fun restRemainingSeconds(): Int = restTimer.get().remainingSeconds

    fun isRestRunning(): Boolean = restTimer.get().running

    fun currentExerciseName(): String = currentExerciseName.get()

    fun currentSetNumber(): Int = currentSetNumber.get()

    fun weightKg(): Double = weightKg.get()

    fun reps(): Int = reps.get()

    fun heartRateBpm(): Int = hrBpmSamples.lastOrNull()?.toInt() ?: 0

    fun activeEnergyKcal(): Double = activeEnergyKcal.get()

    fun uiEpoch(): Int = uiEpoch.get()

    /** True while [destroy] has not cancelled the recorder coroutine scope. */
    fun isLifecycleActive(): Boolean = lifecycleJob.isActive

    /**
     * Cancel outstanding Health Services work. Call from the host Activity
     * [android.app.Activity.onDestroy] (or equivalent stop) so the supervisor
     * scope cannot leak after the UI is gone.
     */
    fun destroy() {
        scope.cancel()
    }

    fun setCurrentExercise(name: String, key: String = name.lowercase().replace(' ', '-')) {
        currentExerciseName.set(name)
        currentExerciseKey.set(key)
        bumpUi()
    }

    fun setWeightKg(value: Double) {
        weightKg.set(value.coerceAtLeast(0.0))
        bumpUi()
    }

    fun setReps(value: Int) {
        reps.set(value.coerceAtLeast(1))
        bumpUi()
    }

    fun adjustWeight(deltaKg: Double) {
        setWeightKg(weightKg.get() + deltaKg)
    }

    fun adjustReps(delta: Int) {
        setReps(reps.get() + delta)
    }

    fun start(workoutMode: WearWorkoutMode = WearWorkoutMode.RUNNING) {
        if (!running.compareAndSet(false, true)) return
        lastError.set(null)
        mode.set(workoutMode)
        hrBpmSamples.clear()
        activeEnergyKcal.set(0.0)
        distanceMetres.set(0.0)
        loggedSets.clear()
        restTimer.set(RestTimerState())
        currentSetNumber.set(1)
        paused.set(false)
        accumulatedPauseMs.set(0L)
        pauseBeganAtMs.set(null)
        startedAtMs.set(System.currentTimeMillis())
        bumpUi()
        exerciseClient.setUpdateCallback(callback)
        // Outdoor-style RUNNING remains the default Health Services type; treadmill
        // BLE distance overrides the live UI / local save when a belt is linked.
        // STRENGTH uses a resolved strength ExerciseType without distance.
        scope.launch {
            val exerciseType =
                if (workoutMode == WearWorkoutMode.STRENGTH) {
                    resolveStrengthExerciseType()
                } else {
                    ExerciseType.RUNNING
                }
            val dataTypes =
                buildSet {
                    add(DataType.HEART_RATE_BPM)
                    add(DataType.CALORIES_TOTAL)
                    if (workoutMode == WearWorkoutMode.RUNNING) {
                        add(DataType.DISTANCE_TOTAL)
                    }
                }
            val config =
                ExerciseConfig.builder(exerciseType)
                    .setDataTypes(dataTypes)
                    .setIsAutoPauseAndResumeEnabled(false)
                    .build()
            runCatching { awaitFuture(exerciseClient.startExerciseAsync(config)) }
                .onFailure {
                    lastError.set(it.message)
                    running.set(false)
                    bumpUi()
                }
        }
    }

    /**
     * Pause via Health Services. Pause/resume are recorded by ExerciseClient —
     * do not invent duplicate local platform events.
     */
    fun pause() {
        if (!running.get() || !paused.compareAndSet(false, true)) return
        pauseBeganAtMs.set(System.currentTimeMillis())
        bumpUi()
        scope.launch {
            runCatching { awaitFuture(exerciseClient.pauseExerciseAsync()) }
                .onFailure {
                    lastError.set(it.message)
                    paused.set(false)
                    pauseBeganAtMs.set(null)
                    bumpUi()
                }
        }
    }

    fun resume() {
        if (!running.get() || !paused.compareAndSet(true, false)) return
        val began = pauseBeganAtMs.getAndSet(null)
        if (began != null) {
            accumulatedPauseMs.updateAndGet { it + (System.currentTimeMillis() - began).coerceAtLeast(0L) }
        }
        bumpUi()
        scope.launch {
            runCatching { awaitFuture(exerciseClient.resumeExerciseAsync()) }
                .onFailure {
                    lastError.set(it.message)
                    // Keep local resume accounting; platform may already be running.
                    bumpUi()
                }
        }
    }

    /** End session → local store → Data Layer. Returns syncIdentifier. */
    fun stop(onDone: (String?) -> Unit) {
        if (!running.get()) {
            onDone(null)
            return
        }
        scope.launch {
            // Ending while paused: fold open pause into accumulated, then end once.
            val began = pauseBeganAtMs.getAndSet(null)
            if (began != null) {
                accumulatedPauseMs.updateAndGet {
                    it + (System.currentTimeMillis() - began).coerceAtLeast(0L)
                }
            }
            val syncId =
                runCatching {
                    // endExerciseAsync works while paused; Health Services owns pause markers.
                    awaitFuture(exerciseClient.endExerciseAsync())
                    publishLocalAndTransfer(
                        startEpochMs = startedAtMs.get() ?: (System.currentTimeMillis() - 60_000L),
                        endEpochMs = System.currentTimeMillis(),
                        activeEnergyKcal = activeEnergyKcal.get().takeIf { it > 0 },
                        distanceMetres =
                        distanceMetres.get().takeIf {
                            it > 0 && mode.get() == WearWorkoutMode.RUNNING
                        },
                        hrBpms = hrBpmSamples.toList(),
                        sets = loggedSets.toList(),
                        activityTypeName =
                        if (mode.get() == WearWorkoutMode.STRENGTH) "STRENGTH" else "RUNNING"
                    )
                }.getOrElse {
                    lastError.set(it.message)
                    null
                }
            running.set(false)
            paused.set(false)
            pauseBeganAtMs.set(null)
            restTimer.set(RestTimerState())
            bumpUi()
            onDone(syncId)
        }
    }

    /**
     * Emulator / test path when ExerciseClient cannot run: still local-store + Data Layer transfer.
     */
    fun recordManualAndTransfer(durationMs: Long = 60_000L): String {
        val end = System.currentTimeMillis()
        return publishLocalAndTransfer(
            startEpochMs = end - durationMs,
            endEpochMs = end,
            activeEnergyKcal = 50.0,
            distanceMetres = 400.0,
            hrBpms = listOf(118.0, 122.0),
            sets = emptyList(),
            activityTypeName = "RUNNING"
        )
    }

    /** Strength smoke path: publish STRENGTH with optional in-memory sets. */
    fun recordManualStrengthAndTransfer(durationMs: Long = 60_000L, sets: List<WearLoggedStrengthSet> = loggedSets.toList()): String {
        val end = System.currentTimeMillis()
        return publishLocalAndTransfer(
            startEpochMs = end - durationMs,
            endEpochMs = end,
            activeEnergyKcal = 80.0,
            distanceMetres = null,
            hrBpms = listOf(110.0, 125.0),
            sets = sets,
            activityTypeName = "STRENGTH"
        )
    }

    fun logSet(isWarmup: Boolean = false, restSeconds: Int = DEFAULT_REST_SECONDS): WearLoggedStrengthSet {
        val set =
            WearLoggedStrengthSet(
                exerciseKey = currentExerciseKey.get(),
                exerciseName = currentExerciseName.get(),
                setIndex = currentSetNumber.get(),
                reps = reps.get(),
                loadKg = weightKg.get(),
                isWarmup = isWarmup
            )
        loggedSets.add(set)
        if (!isWarmup) {
            currentSetNumber.incrementAndGet()
        }
        restTimer.set(RestTimerState().start(restSeconds))
        bumpUi()
        return set
    }

    /** Host UI should call about once per second while a session is active. */
    fun tickRest() {
        val next = restTimer.get().tick()
        if (next != restTimer.get()) {
            restTimer.set(next)
            bumpUi()
        }
    }

    fun skipRest() {
        restTimer.set(restTimer.get().skip())
        bumpUi()
    }

    private fun bumpUi() {
        uiEpoch.incrementAndGet()
    }

    private suspend fun resolveStrengthExerciseType(): ExerciseType {
        val caps =
            runCatching { awaitFuture(exerciseClient.getCapabilitiesAsync()) }.getOrNull()
                ?: return ExerciseType.STRENGTH_TRAINING
        val supported = caps.supportedExerciseTypes
        return when {
            ExerciseType.WEIGHTLIFTING in supported -> ExerciseType.WEIGHTLIFTING
            ExerciseType.STRENGTH_TRAINING in supported -> ExerciseType.STRENGTH_TRAINING
            else -> ExerciseType.RUNNING
        }
    }

    private fun publishLocalAndTransfer(
        startEpochMs: Long,
        endEpochMs: Long,
        activeEnergyKcal: Double?,
        distanceMetres: Double?,
        hrBpms: List<Double>,
        sets: List<WearLoggedStrengthSet>,
        activityTypeName: String
    ): String {
        val localId = UUID.randomUUID().toString()
        val avg = hrBpms.takeIf { it.isNotEmpty() }?.average()
        val max = hrBpms.maxOrNull()
        val hrEpoch =
            if (hrBpms.isEmpty()) {
                longArrayOf()
            } else {
                LongArray(hrBpms.size) { i ->
                    startEpochMs + ((endEpochMs - startEpochMs) * i / (hrBpms.size - 1).coerceAtLeast(1))
                }
            }
        val setIds = sets.map { it.id }.toTypedArray()
        val setExerciseKeys = sets.map { it.exerciseKey }.toTypedArray()
        val setIndexes = sets.map { it.setIndex }.toIntArray()
        val setReps = sets.map { it.reps }.toIntArray()
        val setLoadKg = sets.map { it.loadKg }.toDoubleArray()
        val setIsWarmup = sets.map { it.isWarmup }.toBooleanArray()
        val setRir = DoubleArray(sets.size) { -1.0 }
        val setRpe = DoubleArray(sets.size) { -1.0 }
        val setIsDrop = BooleanArray(sets.size)
        val setIsFailure = BooleanArray(sets.size)
        val syncId =
            if (sets.isEmpty()) {
                core.localHealth.recordOmniGartWorkout(
                    localId = localId,
                    activityTypeName = activityTypeName,
                    startEpochMs = startEpochMs,
                    endEpochMs = endEpochMs,
                    activeEnergyKcal = activeEnergyKcal,
                    basalEnergyKcal = null,
                    distanceMetres = distanceMetres,
                    averageHeartRateBpm = avg,
                    maxHeartRateBpm = max,
                    sessionId = null,
                    planId = null,
                    heartRateEpochMs = hrEpoch,
                    heartRateBpm = hrBpms.toDoubleArray(),
                    syncVersion = 1
                )
            } else {
                core.localHealth.recordOmniGartWorkoutWithSets(
                    localId = localId,
                    activityTypeName = activityTypeName,
                    startEpochMs = startEpochMs,
                    endEpochMs = endEpochMs,
                    activeEnergyKcal = activeEnergyKcal,
                    basalEnergyKcal = null,
                    distanceMetres = distanceMetres,
                    averageHeartRateBpm = avg,
                    maxHeartRateBpm = max,
                    sessionId = null,
                    planId = null,
                    heartRateEpochMs = hrEpoch,
                    heartRateBpm = hrBpms.toDoubleArray(),
                    setIds = setIds,
                    setExerciseKeys = setExerciseKeys,
                    setIndexes = setIndexes,
                    setReps = setReps,
                    setLoadKg = setLoadKg,
                    setIsWarmup = setIsWarmup,
                    setRir = setRir,
                    setRpe = setRpe,
                    setIsDrop = setIsDrop,
                    setIsFailure = setIsFailure,
                    syncVersion = 1
                )
            }
        val json =
            core.localHealth.encodeDeviceTransferJson(
                localId,
                DeviceWorkoutTransferDocument.SOURCE_WEAR_OS
            ) ?: return syncId
        transfer.enqueue(localId, syncId, json)
        return syncId
    }

    private suspend fun <T> awaitFuture(future: ListenableFuture<T>): T = suspendCancellableCoroutine { cont ->
        future.addListener(
            {
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    cont.resume(future.get())
                } catch (e: java.util.concurrent.ExecutionException) {
                    cont.resumeWithException(e.cause ?: e)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    cont.resumeWithException(e)
                }
            },
            directExecutor
        )
    }
}
