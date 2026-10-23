import Foundation

/// Orchestrates the swim-with-strap flow on iOS (deck start → swim → download → merge).
public final class SwimStrapSession {
    private let client: PolarDeviceClienting
    private let sink: HeartRateSink?

    public private(set) var deviceId: String?
    public private(set) var exerciseId: String?
    public private(set) var deckStart: Date?
    public private(set) var downloaded: H10DownloadedRecording?

    public init(client: PolarDeviceClienting, sink: HeartRateSink? = nil) {
        self.client = client
        self.sink = sink
    }

    public func startOnDeck(
        deviceId: String,
        exerciseId: String,
        sampleType: H10SampleType = .hr,
        now: Date = Date()
    ) async throws {
        self.deviceId = deviceId
        self.exerciseId = exerciseId
        try await client.connect(deviceId: deviceId)
        try await client.startH10Recording(
            deviceId: deviceId,
            exerciseId: exerciseId,
            sampleType: sampleType,
            intervalSeconds: 1
        )
        deckStart = now
    }

    public func finishAndDownload(deleteAfterDownload: Bool = true) async throws -> H10DownloadedRecording {
        guard let deviceId else { throw SensorError.notConnected }
        try await client.connect(deviceId: deviceId)
        let status = try await client.h10RecordingStatus(deviceId: deviceId)
        if status.ongoing {
            try await client.stopH10Recording(deviceId: deviceId)
        }
        let entries = try await client.listH10Recordings(deviceId: deviceId)
        guard let entry = entries.first else {
            throw SensorError.polarSdk("No H10 recordings after swim")
        }
        let recording = try await client.downloadH10Recording(deviceId: deviceId, entry: entry)
        if deleteAfterDownload {
            try await client.deleteH10Recording(deviceId: deviceId, entry: entry)
        }
        downloaded = recording
        return recording
    }

    /// Align strap samples onto the watch clock and optionally write to HealthKit via [sink].
    public func mergeWithWatchWorkout(
        watchSamples: [HeartRateSample],
        workoutStart: Date,
        workoutEnd: Date,
        offsetMillis: Int64
    ) async throws -> [HeartRateSample] {
        guard let downloaded else { throw SensorError.polarSdk("download first") }
        let startMs = Int64(workoutStart.timeIntervalSince1970 * 1000)
        let endMs = Int64(workoutEnd.timeIntervalSince1970 * 1000)

        let strap = downloaded.samples.map { sample in
            HeartRateSample(
                epochMillis: sample.epochMillis + offsetMillis,
                bpm: sample.bpm,
                source: .polarOffline,
                rrIntervalsMs: sample.rrIntervalsMs
            )
        }.filter { $0.epochMillis >= startMs && $0.epochMillis <= endMs }

        let watch = watchSamples.filter { $0.epochMillis >= startMs && $0.epochMillis <= endMs }
        // Prefer strap within 750 ms of a watch sample.
        var used = Set<Int>()
        var merged: [HeartRateSample] = []
        for w in watch {
            if let idx = strap.enumerated()
                .filter({ !used.contains($0.offset) })
                .min(by: { abs($0.element.epochMillis - w.epochMillis) < abs($1.element.epochMillis - w.epochMillis) })?
                .offset,
               abs(strap[idx].epochMillis - w.epochMillis) <= 750 {
                merged.append(strap[idx])
                used.insert(idx)
            } else {
                merged.append(w)
            }
        }
        for (i, s) in strap.enumerated() where !used.contains(i) {
            merged.append(s)
        }
        let sorted = merged.sorted { $0.epochMillis < $1.epochMillis }
        _ = try await sink?.writeHeartRate(sorted)
        return sorted
    }
}
