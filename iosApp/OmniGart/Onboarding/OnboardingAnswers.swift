import Foundation
import Shared

/// Mutable answers collected during first-launch onboarding.
struct OnboardingAnswers {
    var biologicalSex: BiologicalSex = .female
    var primaryGoal: ProfileGoal = .generalFitness
    /// Nutrition energy direction (lose / maintain / gain) — separate from training ProfileGoal.
    var energyGoalKind: String = "MAINTAIN"
    var energyTargetWeightKg: Double?
    var energyRatePercent: Double = 0.5
    var strengthLevel: StrengthLevel = .beginner
    var activityLevel: ActivityLevel = .moderate
    var preferredUnits: UnitSystem = .metric
    var birthDate: Date = Calendar.current.date(
        from: DateComponents(year: 1994, month: 6, day: 15)
    ) ?? Date()
    var heightCm: Double = 168
    var weightKg: Double = 62
}
