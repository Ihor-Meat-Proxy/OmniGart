import Foundation
import Shared

/// Apple Foundation Models / Apple Intelligence availability for morning food review.
enum FoodMorningAiAvailability {
    static func check() -> OnDeviceAiAvailability {
        #if canImport(FoundationModels)
        // When the SDK is present, probe system readiness. Model download may still be pending.
        if #available(iOS 26.0, *) {
            return OnDeviceAiAvailability(
                deviceEligible: true,
                featureEnabled: true,
                modelReady: false,
                reasonIfUnavailable: String(localized: "food_ai_model_not_ready")
            )
        }
        #endif
        return OnDeviceAiAvailability(
            deviceEligible: false,
            featureEnabled: false,
            modelReady: false,
            reasonIfUnavailable: String(localized: "food_ai_unavailable")
        )
    }

    /// Optional on-device generator. Returns nil when Foundation Models are unavailable.
    static func generator() -> OnDeviceAiGenerator? {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            return FoundationModelsFoodReviewGenerator()
        }
        #endif
        return nil
    }
}

#if canImport(FoundationModels)
@available(iOS 26.0, *)
private final class FoundationModelsFoodReviewGenerator: OnDeviceAiGenerator {
    func generate(prompt: String) -> String? {
        // Placeholder until product wires LanguageModelSession; keep rule-based fallback.
        nil
    }
}
#endif
