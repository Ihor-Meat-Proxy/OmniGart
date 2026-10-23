import Foundation
import Shared

/// On-device AI availability for morning food review (Apple Foundation Models).
/// Fully on-device; no cloud fallback. When unavailable, UI shows a rule-based summary.
enum FoodMorningReviewSupport {
    static func checkAvailability() -> OnDeviceAiAvailability {
        FoodMorningAiAvailability.check()
    }
}
