import Foundation

/// Platform-facing Polar client contract (mirrors shared KMP `PolarDeviceClient`).
public protocol PolarDeviceClienting: AnyObject {
    func searchDevices() async throws -> [PolarDeviceInfo]
    func connect(deviceId: String) async throws
    func disconnect(deviceId: String) async throws

    func startHrStreaming(deviceId: String, onSample: @escaping (Int, [Double], Date) -> Void)
    func stopHrStreaming()

    /// ECG for H10 (~130 Hz). Returns false when unsupported.
    @discardableResult
    func startEcgStreaming(deviceId: String, onSample: @escaping (Int, Date) -> Void) -> Bool
    func stopEcgStreaming()

    func startH10Recording(
        deviceId: String,
        exerciseId: String,
        sampleType: H10SampleType,
        intervalSeconds: Int
    ) async throws

    func stopH10Recording(deviceId: String) async throws
    func h10RecordingStatus(deviceId: String) async throws -> (ongoing: Bool, entryId: String?)
    func listH10Recordings(deviceId: String) async throws -> [H10RecordingEntry]
    func downloadH10Recording(deviceId: String, entry: H10RecordingEntry) async throws -> H10DownloadedRecording
    func deleteH10Recording(deviceId: String, entry: H10RecordingEntry) async throws
}

public struct H10RecordingEntry: Sendable, Equatable {
    public let id: String
    public let path: String?
    public let date: Date?

    public init(id: String, path: String? = nil, date: Date? = nil) {
        self.id = id
        self.path = path
        self.date = date
    }
}

public struct H10DownloadedRecording: Sendable {
    public let entry: H10RecordingEntry
    public let samples: [HeartRateSample]

    public init(entry: H10RecordingEntry, samples: [HeartRateSample]) {
        self.entry = entry
        self.samples = samples
    }
}
