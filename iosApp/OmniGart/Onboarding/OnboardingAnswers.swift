import Foundation
import Shared

/// Mutable answers collected during first-launch onboarding.
struct OnboardingAnswers {
    var biologicalSex: BiologicalSex = .female
    var primaryGoal: ProfileGoal = .generalFitness
    var strengthLevel: StrengthLevel = .beginner
    var activityLevel: ActivityLevel = .moderate
    var preferredUnits: UnitSystem = .metric
    var birthDate: Date = Calendar.current.date(
        from: DateComponents(year: 1994, month: 6, day: 15)
    ) ?? Date()
    var heightCm: Double = 168
    var weightKg: Double = 62
}
