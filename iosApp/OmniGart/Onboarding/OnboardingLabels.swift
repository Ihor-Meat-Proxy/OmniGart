import Shared
import SwiftUI

enum OnboardingLabels {
    static func goal(_ goal: ProfileGoal) -> LocalizedStringKey {
        switch goal {
        case .generalFitness: "onboarding_goal_general_fitness"
        case .strength: "onboarding_goal_strength"
        case .endurance: "onboarding_goal_endurance"
        case .mobility: "onboarding_goal_mobility"
        case .fatLossSupport: "onboarding_goal_fat_loss_support"
        default: "onboarding_goal_general_fitness"
        }
    }

    static func sex(_ sex: BiologicalSex) -> LocalizedStringKey {
        switch sex {
        case .male: "onboarding_sex_male"
        case .female: "onboarding_sex_female"
        default: "onboarding_sex_female"
        }
    }

    static func experience(_ level: StrengthLevel) -> LocalizedStringKey {
        switch level {
        case .beginner: "onboarding_experience_beginner"
        case .novice: "onboarding_experience_novice"
        case .intermediate: "onboarding_experience_intermediate"
        case .advanced: "onboarding_experience_advanced"
        default: "onboarding_experience_beginner"
        }
    }

    static func units(_ units: UnitSystem) -> LocalizedStringKey {
        switch units {
        case .metric: "onboarding_units_metric"
        case .imperial: "onboarding_units_imperial"
        default: "onboarding_units_metric"
        }
    }

    static func activity(_ level: ActivityLevel) -> LocalizedStringKey {
        switch level {
        case .sedentary: "onboarding_activity_sedentary"
        case .light: "onboarding_activity_light"
        case .moderate: "onboarding_activity_moderate"
        case .active: "onboarding_activity_active"
        case .veryActive: "onboarding_activity_very_active"
        default: "onboarding_activity_moderate"
        }
    }
}
