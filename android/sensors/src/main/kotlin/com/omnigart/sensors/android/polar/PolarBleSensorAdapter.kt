package com.omnigart.sensors.android.polar

import android.content.Context
import com.omnigart.sensors.api.OfflineRecordingRequest
import com.omnigart.sensors.api.PolarSensorFeatures
import com.omnigart.sensors.model.AccSample
import com.omnigart.sensors.model.EcgSample
import com.omnigart.sensors.model.HeartRateMeasurement
import com.omnigart.sensors.model.OfflineRecordingDataType
import com.omnigart.sensors.model.OfflineRecordingEntry
import com.omnigart.sensors.model.OfflineRecordingPayload
import com.omnigart.sensors.model.SensorContactStatus
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarH10OfflineExerciseApi
import com.polar.sdk.api.model.EcgSample as PolarEcgVoltageSample
import com.polar.sdk.api.model.PolarExerciseEntry
import com.polar.sdk.api.model.PolarOfflineRecordingData
import com.polar.sdk.api.model.PolarOfflineRecordingEntry
import com.polar.sdk.api.model.PolarSensorSetting
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * Thin adapter over Polar BLE SDK 8.4.0.
 *
 * Maps Polar streams and H10 / Verity Sense offline APIs onto [PolarSensorFeatures]
 * so callers stay behind [com.omnigart.sensors.api.HeartRateSensorManager].
 *
 * Licence: Polar Software Development Kit Limited License Agreement
 * (see `docs/third_party/POLAR_SDK.md`). Not AGPL; redistribute object code with notice.
 */
class PolarBleSensorAdapter(
    context: Context,
    features: Set<PolarBleApi.PolarBleSdkFeature> = DEFAULT_FEATURES,
) : PolarSensorFeatures {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val api: PolarBleApi =
        PolarBleApiDefaultImpl.defaultImplementation(context.applicationContext, features)

    private var deviceId: String? = null
    private var hrJob: Job? = null
    private var ecgJob: Job? = null
    private var accJob: Job? = null

    private val _hr = MutableSharedFlow<HeartRateMeasurement>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val heartRateMeasurements: Flow<HeartRateMeasurement> = _hr.asSharedFlow()

    private val _ecg = MutableSharedFlow<EcgSample>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val ecg: Flow<EcgSample> = _ecg.asSharedFlow()

    private val _acc = MutableSharedFlow<AccSample>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val acc: Flow<AccSample> = _acc.asSharedFlow()

    fun sdkVersion(): String = PolarBleApiDefaultImpl.versionInfo()

    fun connect(polarDeviceId: String) {
        deviceId = polarDeviceId
        api.connectToDevice(polarDeviceId)
    }

    fun disconnect() {
        val id = deviceId ?: return
        scope.launch {
            stopHrStream()
            stopEcgStream()
            stopAccStream()
        }
        api.disconnectFromDevice(id)
        deviceId = null
    }

    override suspend fun startHrStream() {
        val id = requireDevice()
        hrJob?.cancel()
        hrJob = scope.launch {
            api.startHrStreaming(id).collect { data ->
                data.samples.forEach { sample ->
                    val contact = when {
                        !sample.contactStatusSupported -> SensorContactStatus.NotSupported
                        sample.contactStatus -> SensorContactStatus.SupportedDetected
                        else -> SensorContactStatus.SupportedNotDetected
                    }
                    _hr.tryEmit(
                        HeartRateMeasurement(
                            heartRateBpm = sample.hr,
                            sensorContact = contact,
                            energyExpendedKcal = null,
                            rrIntervalsMs = sample.rrsMs.map { it.toDouble() },
                            flags = 0,
                            heartRateUint16 = sample.hr > 255,
                        )
                    )
                }
            }
        }
    }

    override suspend fun stopHrStream() {
        hrJob?.cancel()
        hrJob = null
        val id = deviceId ?: return
        runCatching { api.stopHrStreaming(id) }
    }

    override suspend fun startEcgStream() {
        val id = requireDevice()
        val settings = api.requestStreamSettings(id, PolarBleApi.PolarDeviceDataType.ECG)
        ecgJob?.cancel()
        ecgJob = scope.launch {
            api.startEcgStreaming(id, settings.maxSettings()).collect { data ->
                data.samples.forEach { sample ->
                    val voltage = (sample as? PolarEcgVoltageSample)?.voltage ?: return@forEach
                    _ecg.tryEmit(
                        EcgSample(
                            timestamp = polarNanosToInstant(sample.timeStamp),
                            microVolts = voltage,
                        )
                    )
                }
            }
        }
    }

    override suspend fun stopEcgStream() {
        ecgJob?.cancel()
        ecgJob = null
    }

    override suspend fun startAccStream(sampleRateHz: Int) {
        val id = requireDevice()
        val available = api.requestStreamSettings(id, PolarBleApi.PolarDeviceDataType.ACC)
        val settings = try {
            PolarSensorSetting(
                mapOf(PolarSensorSetting.SettingType.SAMPLE_RATE to sampleRateHz)
            )
        } catch (_: Throwable) {
            available.maxSettings()
        }
        accJob?.cancel()
        accJob = scope.launch {
            api.startAccStreaming(id, settings).collect { data ->
                data.samples.forEach { sample ->
                    _acc.tryEmit(
                        AccSample(
                            timestamp = polarNanosToInstant(sample.timeStamp),
                            xMg = sample.x,
                            yMg = sample.y,
                            zMg = sample.z,
                        )
                    )
                }
            }
        }
    }

    override suspend fun stopAccStream() {
        accJob?.cancel()
        accJob = null
    }

    override suspend fun startOfflineRecording(types: Set<OfflineRecordingRequest>) {
        val id = requireDevice()
        if (types.contains(OfflineRecordingRequest.RrInterval) ||
            types.contains(OfflineRecordingRequest.HeartRate)
        ) {
            val sampleType = if (types.contains(OfflineRecordingRequest.RrInterval)) {
                PolarH10OfflineExerciseApi.SampleType.RR
            } else {
                PolarH10OfflineExerciseApi.SampleType.HR
            }
            api.startRecording(
                identifier = id,
                exerciseId = "omnigart-${UUID.randomUUID()}",
                interval = PolarH10OfflineExerciseApi.RecordingInterval.INTERVAL_1S,
                sampleType = sampleType,
            )
            return
        }
        for (type in types) {
            val dataType = type.toPolarDataType() ?: continue
            val settings = when (dataType) {
                PolarBleApi.PolarDeviceDataType.HR,
                PolarBleApi.PolarDeviceDataType.PPI,
                -> null
                else -> api.requestOfflineRecordingSettings(id, dataType).maxSettings()
            }
            api.startOfflineRecording(id, dataType, settings, secret = null)
        }
    }

    override suspend fun stopOfflineRecording() {
        val id = requireDevice()
        runCatching { api.stopRecording(id) }
        runCatching {
            for (type in api.getOfflineRecordingStatus(id)) {
                api.stopOfflineRecording(id, type)
            }
        }
    }

    override suspend fun listOfflineRecordings(): List<OfflineRecordingEntry> {
        val id = requireDevice()
        val h10 = mutableListOf<OfflineRecordingEntry>()
        runCatching {
            api.listExercises(id).collect { entry ->
                h10 += OfflineRecordingEntry(
                    id = entry.identifier,
                    deviceId = id,
                    path = entry.path,
                    date = entry.date.toInstant(),
                    dataTypes = setOf(
                        OfflineRecordingDataType.HeartRate,
                        OfflineRecordingDataType.RrInterval,
                    ),
                )
            }
        }
        if (h10.isNotEmpty()) return h10

        val sense = mutableListOf<OfflineRecordingEntry>()
        api.listOfflineRecordings(id).collect { entry ->
            sense += OfflineRecordingEntry(
                id = entry.path,
                deviceId = id,
                path = entry.path,
                sizeBytes = entry.size,
                date = entry.date.toInstant(),
                dataTypes = setOf(entry.type.toOmni()),
            )
        }
        return sense
    }

    override suspend fun downloadOfflineRecording(entry: OfflineRecordingEntry): OfflineRecordingPayload {
        val id = requireDevice()
        val h10Entry = PolarExerciseEntry(
            path = entry.path.orEmpty(),
            date = entry.date?.toLocalDateTime() ?: LocalDateTime.now(ZoneOffset.UTC),
            identifier = entry.id,
        )
        val exercise = runCatching { api.fetchExercise(id, h10Entry) }.getOrNull()
        if (exercise != null) {
            val start = entry.date ?: Instant.fromEpochMilliseconds(java.lang.System.currentTimeMillis())
            val intervalMs = exercise.recordingInterval.coerceAtLeast(1) * 1000L
            // H10 stores either HR bpm or RR ms in hrSamples depending on sample type.
            val looksLikeRr = exercise.hrSamples.any { it > 300 }
            return if (looksLikeRr) {
                OfflineRecordingPayload(
                    entry = entry,
                    rrIntervalsMs = exercise.hrSamples.map { null to it.toDouble() },
                )
            } else {
                OfflineRecordingPayload(
                    entry = entry,
                    heartRateBpm = exercise.hrSamples.mapIndexed { index, sample ->
                        Instant.fromEpochMilliseconds(
                            start.toEpochMilliseconds() + index * intervalMs
                        ) to sample
                    },
                )
            }
        }

        val polarEntry = PolarOfflineRecordingEntry(
            path = entry.path ?: entry.id,
            size = entry.sizeBytes ?: 0L,
            date = entry.date?.toLocalDateTime() ?: LocalDateTime.now(ZoneOffset.UTC),
            type = PolarBleApi.PolarDeviceDataType.HR,
        )
        return when (val data = api.getOfflineRecord(id, polarEntry, secret = null)) {
            is PolarOfflineRecordingData.HrOfflineRecording -> {
                val start = data.startTime.toInstant()
                OfflineRecordingPayload(
                    entry = entry,
                    heartRateBpm = data.data.samples.mapIndexed { index, sample ->
                        Instant.fromEpochMilliseconds(
                            start.toEpochMilliseconds() + index * 1000L
                        ) to sample.hr
                    },
                    rrIntervalsMs = data.data.samples.flatMap { sample ->
                        sample.rrsMs.map { null to it.toDouble() }
                    },
                )
            }
            else -> OfflineRecordingPayload(entry = entry)
        }
    }

    override suspend fun deleteOfflineRecording(entry: OfflineRecordingEntry) {
        val id = requireDevice()
        runCatching {
            api.removeExercise(
                id,
                PolarExerciseEntry(
                    path = entry.path.orEmpty(),
                    date = entry.date?.toLocalDateTime() ?: LocalDateTime.now(ZoneOffset.UTC),
                    identifier = entry.id,
                ),
            )
        }
        runCatching {
            api.removeOfflineRecord(
                id,
                PolarOfflineRecordingEntry(
                    path = entry.path ?: entry.id,
                    size = entry.sizeBytes ?: 0L,
                    date = entry.date?.toLocalDateTime() ?: LocalDateTime.now(ZoneOffset.UTC),
                    type = PolarBleApi.PolarDeviceDataType.HR,
                ),
            )
        }
    }

    fun close() {
        scope.cancel()
        api.shutDown()
    }

    private fun requireDevice(): String =
        deviceId ?: error("Polar device not connected")

    companion object {
        val DEFAULT_FEATURES: Set<PolarBleApi.PolarBleSdkFeature> = setOf(
            PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
            PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
        )

        /** Polar timestamps use epoch 2000-01-01 UTC, nanoseconds. */
        private val POLAR_EPOCH_MS =
            LocalDateTime.of(2000, 1, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

        fun polarNanosToInstant(nanosSincePolarEpoch: Long): Instant =
            Instant.fromEpochMilliseconds(POLAR_EPOCH_MS + nanosSincePolarEpoch / 1_000_000L)
    }
}

private fun OfflineRecordingRequest.toPolarDataType(): PolarBleApi.PolarDeviceDataType? =
    when (this) {
        OfflineRecordingRequest.HeartRate -> PolarBleApi.PolarDeviceDataType.HR
        OfflineRecordingRequest.Acc -> PolarBleApi.PolarDeviceDataType.ACC
        OfflineRecordingRequest.Ecg -> PolarBleApi.PolarDeviceDataType.ECG
        OfflineRecordingRequest.Ppi -> PolarBleApi.PolarDeviceDataType.PPI
        OfflineRecordingRequest.RrInterval -> null
    }

private fun PolarBleApi.PolarDeviceDataType.toOmni(): OfflineRecordingDataType =
    when (this) {
        PolarBleApi.PolarDeviceDataType.HR -> OfflineRecordingDataType.HeartRate
        PolarBleApi.PolarDeviceDataType.ECG -> OfflineRecordingDataType.Ecg
        PolarBleApi.PolarDeviceDataType.ACC -> OfflineRecordingDataType.Acc
        PolarBleApi.PolarDeviceDataType.PPI -> OfflineRecordingDataType.Ppi
        PolarBleApi.PolarDeviceDataType.PPG -> OfflineRecordingDataType.Ppg
        PolarBleApi.PolarDeviceDataType.GYRO -> OfflineRecordingDataType.Gyro
        PolarBleApi.PolarDeviceDataType.MAGNETOMETER -> OfflineRecordingDataType.Magnetometer
        else -> OfflineRecordingDataType.Unknown
    }

private fun LocalDateTime.toInstant(): Instant =
    Instant.fromEpochMilliseconds(toInstant(ZoneOffset.UTC).toEpochMilli())

private fun Instant.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(toEpochMilliseconds()),
        ZoneOffset.UTC,
    )
