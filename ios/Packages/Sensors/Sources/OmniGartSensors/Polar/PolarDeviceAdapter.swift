import CoreBluetooth
import Foundation
import PolarBleSdk

/// Polar BLE SDK **8.4.0** adapter implementing [PolarDeviceClienting].
///
/// Uses the SDK’s Swift concurrency APIs (async streams / await). H10 offline
/// exercise recording: `startRecording` / `stopRecording` / `requestRecordingStatus`
/// / `listExercises` / `fetchExercise` / `removeExercise`.
public final class PolarDeviceAdapter: NSObject, PolarDeviceClienting {
    private let api: PolarBleApi
    private var hrTask: Task<Void, Never>?
    private var ecgTask: Task<Void, Never>?

    public var onDeviceConnected: ((PolarDeviceInfo) -> Void)?
    public var onDeviceDisconnected: ((String) -> Void)?
    public var onBattery: ((String, UInt) -> Void)?

    public init(restorationIdentifier: String? = "com.omnigart.sensors.polar") {
        api = PolarBleApiDefaultImpl.polarImplementation(
            DispatchQueue.main,
            features: [
                .feature_hr,
                .feature_polar_online_streaming,
                .feature_polar_offline_recording,
                .feature_polar_h10_exercise_recording,
                .feature_battery_info,
                .feature_device_info,
            ],
            restoreIdentifier: restorationIdentifier
        )
        super.init()
        api.polarFilter(true)
        api.observer = self
        api.powerStateObserver = self
        api.deviceInfoObserver = self
        api.deviceFeaturesObserver = self
    }

    deinit {
        hrTask?.cancel()
        ecgTask?.cancel()
    }

    // MARK: - PolarDeviceClienting

    public func searchDevices() async throws -> [PolarDeviceInfo] {
        var found: [String: PolarDeviceInfo] = [:]
        let clock = ContinuousClock()
        let deadline = clock.now + .seconds(8)
        for try await device in api.searchForDevice() {
            found[device.deviceId] = PolarDeviceInfo(
                deviceId: device.deviceId,
                address: device.address.uuidString,
                name: device.name,
                isH10: device.name.uppercased().contains("H10")
            )
            if clock.now >= deadline { break }
        }
        return Array(found.values)
    }

    public func connect(deviceId: String) async throws {
        try api.connectToDevice(deviceId)
    }

    public func disconnect(deviceId: String) async throws {
        try api.disconnectFromDevice(deviceId)
    }

    public func startHrStreaming(deviceId: String, onSample: @escaping (Int, [Double], Date) -> Void) {
        hrTask?.cancel()
        hrTask = Task {
            do {
                for try await data in api.startHrStreaming(deviceId) {
                    if Task.isCancelled { break }
                    let now = Date()
                    for sample in data.samples {
                        let rr = sample.rrsMs.map { Double($0) }
                        onSample(Int(sample.hr), rr, now)
                    }
                }
            } catch {
                // Stream ended or failed — caller may restart.
            }
        }
    }

    public func stopHrStreaming() {
        hrTask?.cancel()
        hrTask = nil
    }

    @discardableResult
    public func startEcgStreaming(deviceId: String, onSample: @escaping (Int, Date) -> Void) -> Bool {
        ecgTask?.cancel()
        ecgTask = Task {
            do {
                let settings = try await api.requestStreamSettings(deviceId, feature: .ecg)
                // Prefer highest available resolution/rate for ECG (H10 ≈ 130 Hz).
                let streamSettings = settings.maxSettings()
                for try await data in api.startEcgStreaming(deviceId, settings: streamSettings) {
                    if Task.isCancelled { break }
                    let now = Date()
                    for item in data {
                        onSample(Int(item.voltage), now)
                    }
                }
            } catch {
                // ECG unavailable (non-H10) or stream ended.
            }
        }
        return true
    }

    public func stopEcgStreaming() {
        ecgTask?.cancel()
        ecgTask = nil
    }

    public func startH10Recording(
        deviceId: String,
        exerciseId: String,
        sampleType: H10SampleType,
        intervalSeconds: Int
    ) async throws {
        let interval: PolarH10OfflineExerciseApi.RecordingInterval
        switch intervalSeconds {
        case 5: interval = .interval_5s
        case 15: interval = .interval_15s
        case 60: interval = .interval_60s
        default: interval = .interval_1s
        }
        let type: PolarH10OfflineExerciseApi.SampleType = (sampleType == .rr) ? .rr : .hr
        try await api.startRecording(
            deviceId,
            exerciseId: exerciseId,
            interval: interval,
            sampleType: type
        )
    }

    public func stopH10Recording(deviceId: String) async throws {
        try await api.stopRecording(deviceId)
    }

    public func h10RecordingStatus(deviceId: String) async throws -> (ongoing: Bool, entryId: String?) {
        let pair = try await api.requestRecordingStatus(deviceId)
        return (pair.ongoing, pair.entryId.isEmpty ? nil : pair.entryId)
    }

    public func listH10Recordings(deviceId: String) async throws -> [H10RecordingEntry] {
        var entries: [H10RecordingEntry] = []
        for try await entry in api.listExercises(deviceId) {
            entries.append(
                H10RecordingEntry(
                    id: entry.entryId,
                    path: entry.path,
                    date: entry.date
                )
            )
        }
        return entries
    }

    public func downloadH10Recording(
        deviceId: String,
        entry: H10RecordingEntry
    ) async throws -> H10DownloadedRecording {
        // Re-resolve the SDK entry by listing (PolarExerciseEntry is SDK-typed).
        var match: PolarExerciseEntry?
        for try await e in api.listExercises(deviceId) {
            if e.entryId == entry.id || e.path == entry.path {
                match = e
                break
            }
        }
        guard let match else {
            throw SensorError.polarSdk("H10 entry not found: \(entry.id)")
        }
        let data = try await api.fetchExercise(deviceId, entry: match)
        let start = entry.date ?? Date()
        // H10 HR offline samples are typically 1 Hz — index → timestamp.
        let samples: [HeartRateSample] = data.samples.enumerated().map { index, bpm in
            HeartRateSample(
                epochMillis: Int64((start.timeIntervalSince1970 + Double(index)) * 1000),
                bpm: Int(bpm),
                source: .polarOffline
            )
        }
        return H10DownloadedRecording(entry: entry, samples: samples)
    }

    public func deleteH10Recording(deviceId: String, entry: H10RecordingEntry) async throws {
        var match: PolarExerciseEntry?
        for try await e in api.listExercises(deviceId) {
            if e.entryId == entry.id || e.path == entry.path {
                match = e
                break
            }
        }
        guard let match else {
            throw SensorError.polarSdk("H10 entry not found: \(entry.id)")
        }
        try await api.removeExercise(deviceId, entry: match)
    }
}

// MARK: - Observers

extension PolarDeviceAdapter: PolarBleApiObserver {
    public func deviceConnecting(_ polarDeviceInfo: PolarBleSdk.PolarDeviceInfo) {}

    public func deviceConnected(_ polarDeviceInfo: PolarBleSdk.PolarDeviceInfo) {
        onDeviceConnected?(
            PolarDeviceInfo(
                deviceId: polarDeviceInfo.deviceId,
                address: polarDeviceInfo.address.uuidString,
                name: polarDeviceInfo.name,
                isH10: polarDeviceInfo.name.uppercased().contains("H10")
            )
        )
    }

    public func deviceDisconnected(_ polarDeviceInfo: PolarBleSdk.PolarDeviceInfo, pairingError: Bool) {
        onDeviceDisconnected?(polarDeviceInfo.deviceId)
    }
}

extension PolarDeviceAdapter: PolarBleApiPowerStateObserver {
    public func blePowerOn() {}
    public func blePowerOff() {}
}

extension PolarDeviceAdapter: PolarBleApiDeviceInfoObserver {
    public func batteryLevelReceived(_ identifier: String, batteryLevel: UInt) {
        onBattery?(identifier, batteryLevel)
    }

    public func batteryChargingStatusReceived(
        _ identifier: String,
        chargingStatus: BleBasClient.ChargeState
    ) {}

    public func disInformationReceived(_ identifier: String, uuid: CBUUID, value: String) {}

    public func disInformationReceivedWithKeysAsStrings(
        _ identifier: String,
        key: String,
        value: String
    ) {}
}

extension PolarDeviceAdapter: PolarBleApiDeviceFeaturesObserver {
    public func bleSdkFeatureReady(
        _ identifier: String,
        feature: PolarBleApi.PolarBleSdkFeature
    ) {}
}
