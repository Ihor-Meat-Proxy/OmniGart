import Foundation

/// Exponential backoff with full jitter (matches KMP `ReconnectBackoff`).
public struct ReconnectBackoff: Sendable {
    public var initialDelayMs: Int64
    public var maxDelayMs: Int64
    public var multiplier: Double
    public var maxAttempts: Int

    public init(
        initialDelayMs: Int64 = 1_000,
        maxDelayMs: Int64 = 60_000,
        multiplier: Double = 2.0,
        maxAttempts: Int = Int.max
    ) {
        self.initialDelayMs = initialDelayMs
        self.maxDelayMs = maxDelayMs
        self.multiplier = multiplier
        self.maxAttempts = maxAttempts
    }

    public func delayMs(forAttempt attempt: Int) -> Int64 {
        guard attempt >= 0, attempt < maxAttempts else { return -1 }
        let exp = Double(initialDelayMs) * pow(multiplier, Double(attempt))
        let capped = Int64(min(Double(maxDelayMs), exp))
        guard capped > 0 else { return 0 }
        return Int64.random(in: 0...capped)
    }
}
