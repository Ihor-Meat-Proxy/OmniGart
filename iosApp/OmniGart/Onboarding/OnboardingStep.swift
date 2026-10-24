import SwiftUI

enum OnboardingStep: Int, CaseIterable {
    case welcome
    case goal
    case sex
    case birthDate
    case height
    case weight
    case energyGoal
    case experience
    case units
    case activity
    case healthKit

    var titleKey: LocalizedStringKey {
        switch self {
        case .welcome: "onboarding_welcome_title"
        case .goal: "onboarding_goal_title"
        case .sex: "onboarding_sex_title"
        case .birthDate: "onboarding_birth_title"
        case .height: "onboarding_height_title"
        case .weight: "onboarding_weight_title"
        case .energyGoal: "onboarding_energy_goal_title"
        case .experience: "onboarding_experience_title"
        case .units: "onboarding_units_title"
        case .activity: "onboarding_activity_title"
        case .healthKit: "perm_explainer_health_title"
        }
    }

    var next: OnboardingStep {
        let all = OnboardingStep.allCases
        let idx = all.firstIndex(of: self) ?? 0
        return all[min(idx + 1, all.count - 1)]
    }

    var previous: OnboardingStep {
        let all = OnboardingStep.allCases
        let idx = all.firstIndex(of: self) ?? 0
        return all[max(idx - 1, 0)]
    }
}
