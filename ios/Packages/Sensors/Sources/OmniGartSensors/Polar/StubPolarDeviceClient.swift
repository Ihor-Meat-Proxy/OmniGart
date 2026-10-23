import Foundation

/// In-memory Polar client for unit tests and UI previews (no SDK / no hardware).
public final class StubPolarDeviceClient: PolarDeviceClienting {
    public var devices: [PolarDeviceInfo]
    public var recordings: [H10DownloadedRecording]
    public var recordingOngoing = false
    public private(set) var connectedId: String?

    public init(
        devices: [PolarDeviceInfo] = [],
        recordings: [H10DownloadedRecording] = []
    ) {
        self.devices = devices
        self.recordings = recordings
    }

    public func searchDevices() async throws -> [PolarDeviceInfo] { devices }

    public func connect(deviceId: String) async throws { connectedId = deviceId }

    public func disconnect(deviceId: String) async throws {
        if connectedId == deviceId { connectedId = nil }
    }

    public func startHrStreaming(deviceId: String, onSample: @escaping (Int, [Double], Date) -> Void) {}
    public func stopHrStreaming() {}
    public func startEcgStreaming(deviceId: String, onSample: @escaping (Int, Date) -> Void) -> Bool {
        devices.contains { $0.deviceId == deviceId && $0.isH10 }
    }
    public func stopEcgStreaming() {}

    public func startH10Recording(
        deviceId: String,
        exerciseId: String,
        sampleType: H10SampleType,
        intervalSeconds: Int
    ) async throws {
        recordingOngoing = true
    }

    public func stopH10Recording(deviceId: String) async throws {
        recordingOngoing = false
    }

    public func h10RecordingStatus(deviceId: String) async throws -> (ongoing: Bool, entryId: String?) {
        (recordingOngoing, recordings.first?.entry.id)
    }

    public func listH10Recordings(deviceId: String) async throws -> [H10RecordingEntry] {
        recordings.map(\.entry)
    }

    public func downloadH10Recording(
        deviceId: String,
        entry: H10RecordingEntry
    ) async throws -> H10DownloadedRecording {
        guard let found = recordings.first(where: { $0.entry.id == entry.id }) else {
            throw SensorError.polarSdk("recording not found: \(entry.id)")
        }
        return found
    }

    public func deleteH10Recording(deviceId: String, entry: H10RecordingEntry) async throws {
        recordings.removeAll { $0.entry.id == entry.id }
    }
}
