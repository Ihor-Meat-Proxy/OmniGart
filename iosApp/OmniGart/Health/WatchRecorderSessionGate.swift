import Foundation

/// Tracks phone session keys for which the Apple Watch is the HealthKit recorder.
/// Phone saves stay local-only (append-only ingest from the watch transfer) — no second HK write.
enum WatchRecorderSessionGate {
    private static let lock = NSLock()
    /// Protected by `lock`; `nonisolated(unsafe)` satisfies Swift 6 global mutable state rules.
    nonisolated(unsafe) private static var keys = Set<String>()

    static func markWatchRecorder(sessionKey: String) {
        guard !sessionKey.isEmpty else { return }
        lock.lock()
        keys.insert(sessionKey)
        lock.unlock()
    }

    static func isWatchRecorder(sessionKey: String) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        return keys.contains(sessionKey)
    }

    static func clear(sessionKey: String) {
        lock.lock()
        keys.remove(sessionKey)
        lock.unlock()
    }
}
