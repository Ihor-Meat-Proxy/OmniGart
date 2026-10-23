import Foundation
import Shared

/// On-device AI availability for morning food review (Apple Foundation Models / Apple Intelligence).
/// Fully on-device; no cloud fallback. When unavailable, UI shows a rule-based summary.
enum FoodMorningReviewSupport {
    struct Availability {
        let deviceEligible: Bool
        let featureEnabled: Bool
        let modelReady: Bool
        let reason: String?

        var usable: Bool { deviceEligible && featureEnabled && modelReady }

        func toShared() -> OnDeviceAiAvailability {
            OnDeviceAiAvailability(
                deviceEligible: deviceEligible,
                featureEnabled: featureEnabled,
                modelReady: modelReady,
                reasonIfUnavailable: reason
            )
        }
    }

    /// Best-effort check. Foundation Models require a recent OS; CI builds may lack the SDK.
    static func checkAvailability() -> Availability {
        #if canImport(FoundationModels)
        // Runtime path when the SDK is linked on eligible devices.
        return Availability(
            deviceEligible: true,
            featureEnabled: true,
            modelReady: false,
            reason: String(localized: "food_ai_model_not_ready")
        )
        #else
        return Availability(
            deviceEligible: false,
            featureEnabled: false,
            modelReady: false,
            reason: String(localized: "food_ai_unavailable")
        )
        #endif
    }
}
