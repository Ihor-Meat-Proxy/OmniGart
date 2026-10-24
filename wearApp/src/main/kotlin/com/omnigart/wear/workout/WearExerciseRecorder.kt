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
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Records with Health Services [ExerciseClient], keeps a Wear local copy, then sends to the phone
 * via [WearPhoneWorkoutTransfer]. The Wear copy is dropped when the phone acks.
 */
class WearExerciseRecorder(
    context: Context,
    private val core: OmniGartCore,
    private val transfer: WearPhoneWorkoutTransfer,
    private val exerciseClient: ExerciseClient =
        HealthServices.getClient(context.applicationContext).exerciseClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val directExecutor = Executor { it.run() }
    private val running = AtomicBoolean(false)
    private val startedAtMs = AtomicReference<Long?>(null)
    private val hrBpmSamples = CopyOnWriteArrayList<Double>()
    private val activeEnergyKcal = AtomicReference(0.0)
    private val distanceMetres = AtomicReference(0.0)
    private val lastError = AtomicReference<String?>(null)

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
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

            override fun onRegistered() = Unit

            override fun onRegistrationFailed(throwable: Throwable) {
                lastError.set(throwable.message)
            }

            override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit
        }

    fun isRunning(): Boolean = running.get()

    fun lastError(): String? = lastError.get()

    fun start() {
        if (!running.compareAndSet(false, true)) return
        lastError.set(null)
        hrBpmSamples.clear()
        activeEnergyKcal.set(0.0)
        distanceMetres.set(0.0)
        startedAtMs.set(System.currentTimeMillis())
        exerciseClient.setUpdateCallback(callback)
        val config =
            ExerciseConfig.builder(ExerciseType.RUNNING)
                .setDataTypes(
                    setOf(
                        DataType.HEART_RATE_BPM,
                        DataType.CALORIES_TOTAL,
                        DataType.DISTANCE_TOTAL
                    )
                )
                .setIsAutoPauseAndResumeEnabled(false)
                .build()
        scope.launch {
            runCatching { awaitFuture(exerciseClient.startExerciseAsync(config)) }
                .onFailure {
                    lastError.set(it.message)
                    running.set(false)
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
            val syncId =
                runCatching {
                    awaitFuture(exerciseClient.endExerciseAsync())
                    publishLocalAndTransfer(
                        startEpochMs = startedAtMs.get() ?: (System.currentTimeMillis() - 60_000L),
                        endEpochMs = System.currentTimeMillis(),
                        activeEnergyKcal = activeEnergyKcal.get().takeIf { it > 0 },
                        distanceMetres = distanceMetres.get().takeIf { it > 0 },
                        hrBpms = hrBpmSamples.toList()
                    )
                }.getOrElse {
                    lastError.set(it.message)
                    null
                }
            running.set(false)
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
            hrBpms = listOf(118.0, 122.0)
        )
    }

    private fun publishLocalAndTransfer(
        startEpochMs: Long,
        endEpochMs: Long,
        activeEnergyKcal: Double?,
        distanceMetres: Double?,
        hrBpms: List<Double>
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
        val syncId =
            core.localHealth.recordOmniGartWorkout(
                localId = localId,
                activityTypeName = "RUNNING",
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
