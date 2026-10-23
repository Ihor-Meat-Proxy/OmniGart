import Shared
import SwiftUI

/// Step body for the onboarding wizard (kept separate to satisfy SwiftLint length limits).
struct OnboardingStepContentView: View {
    let step: OnboardingStep
    @Binding var answers: OnboardingAnswers
    let requestingHealth: Bool
    let onRequestHealth: () -> Void

    var body: some View {
        switch step {
        case .welcome:
            welcomeStep
        case .goal:
            goalStep
        case .sex:
            sexStep
        case .birthDate:
            birthStep
        case .height:
            heightStep
        case .weight:
            weightStep
        case .experience:
            experienceStep
        case .units:
            unitsStep
        case .activity:
            activityStep
        case .healthKit:
            healthStep
        }
    }

    private var welcomeStep: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text("onboarding_welcome_title")
                .font(.title2.bold())
            Text("onboarding_welcome_body")
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            WellnessDisclaimerBanner()
        }
        .accessibilityIdentifier("onboarding_step_welcome")
    }

    private var goalStep: some View {
        OnboardingChoiceList(
            config: OnboardingChoiceConfig(
                titleKey: "onboarding_goal_title",
                subtitleKey: nil,
                options: [
                    ProfileGoal.generalFitness,
                    ProfileGoal.strength,
                    ProfileGoal.endurance,
                    ProfileGoal.mobility,
                    ProfileGoal.fatLossSupport
                ],
                selected: answers.primaryGoal,
                label: OnboardingLabels.goal,
                idPrefix: "onboarding_goal",
                onSelect: { answers.primaryGoal = $0 }
            )
        )
        .accessibilityIdentifier("onboarding_step_goal")
    }

    private var sexStep: some View {
        OnboardingChoiceList(
            config: OnboardingChoiceConfig(
                titleKey: "onboarding_sex_title",
                subtitleKey: "onboarding_sex_body",
                options: [BiologicalSex.male, BiologicalSex.female],
                selected: answers.biologicalSex,
                label: OnboardingLabels.sex,
                idPrefix: "onboarding_sex",
                onSelect: { answers.biologicalSex = $0 }
            )
        )
        .accessibilityIdentifier("onboarding_step_sex")
    }

    private var birthStep: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text("onboarding_birth_title")
                .font(.title3.bold())
            Text("onboarding_birth_body")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            DatePicker(
                "onboarding_birth_title",
                selection: $answers.birthDate,
                in: ...Date(),
                displayedComponents: .date
            )
            .datePickerStyle(.wheel)
            .labelsHidden()
            .accessibilityIdentifier("onboarding_birth_picker")
        }
        .accessibilityIdentifier("onboarding_step_birth")
    }

    private var heightStep: some View {
        OnboardingMeasurementStepper(
            config: OnboardingMeasurementConfig(
                titleKey: "onboarding_height_title",
                value: heightDisplay,
                unitLabel: answers.preferredUnits == .metric
                    ? "onboarding_unit_cm" : "onboarding_unit_in",
                decrement: { adjustHeight(-1) },
                increment: { adjustHeight(1) },
                id: "onboarding_height"
            )
        )
        .accessibilityIdentifier("onboarding_step_height")
    }

    private var weightStep: some View {
        OnboardingMeasurementStepper(
            config: OnboardingMeasurementConfig(
                titleKey: "onboarding_weight_title",
                value: weightDisplay,
                unitLabel: answers.preferredUnits == .metric
                    ? "onboarding_unit_kg" : "onboarding_unit_lb",
                decrement: { adjustWeight(-0.5) },
                increment: { adjustWeight(0.5) },
                id: "onboarding_weight"
            )
        )
        .accessibilityIdentifier("onboarding_step_weight")
    }

    private var experienceStep: some View {
        OnboardingChoiceList(
            config: OnboardingChoiceConfig(
                titleKey: "onboarding_experience_title",
                subtitleKey: nil,
                options: [
                    StrengthLevel.beginner,
                    StrengthLevel.novice,
                    StrengthLevel.intermediate,
                    StrengthLevel.advanced
                ],
                selected: answers.strengthLevel,
                label: OnboardingLabels.experience,
                idPrefix: "onboarding_experience",
                onSelect: { answers.strengthLevel = $0 }
            )
        )
        .accessibilityIdentifier("onboarding_step_experience")
    }

    private var unitsStep: some View {
        OnboardingChoiceList(
            config: OnboardingChoiceConfig(
                titleKey: "onboarding_units_title",
                subtitleKey: nil,
                options: [UnitSystem.metric, UnitSystem.imperial],
                selected: answers.preferredUnits,
                label: OnboardingLabels.units,
                idPrefix: "onboarding_units",
                onSelect: { answers.preferredUnits = $0 }
            )
        )
        .accessibilityIdentifier("onboarding_step_units")
    }

    private var activityStep: some View {
        OnboardingChoiceList(
            config: OnboardingChoiceConfig(
                titleKey: "onboarding_activity_title",
                subtitleKey: nil,
                options: [
                    ActivityLevel.sedentary,
                    ActivityLevel.light,
                    ActivityLevel.moderate,
                    ActivityLevel.active,
                    ActivityLevel.veryActive
                ],
                selected: answers.activityLevel,
                label: OnboardingLabels.activity,
                idPrefix: "onboarding_activity",
                onSelect: { answers.activityLevel = $0 }
            )
        )
        .accessibilityIdentifier("onboarding_step_activity")
    }

    private var healthStep: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space3) {
            Text("perm_explainer_health_title")
                .font(.title3.bold())
            Text("perm_explainer_health_body")
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Text("onboarding_health_privacy")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            HStack {
                Button("perm_not_now") {
                    // Permission optional — finish still saves the profile.
                }
                .accessibilityIdentifier("onboarding_health_not_now")
                Spacer()
                Button("perm_continue") {
                    onRequestHealth()
                }
                .buttonStyle(.borderedProminent)
                .disabled(requestingHealth)
                .accessibilityIdentifier("onboarding_health_continue")
            }
        }
        .accessibilityIdentifier("onboarding_step_health")
    }

    private var heightDisplay: String {
        if answers.preferredUnits == .metric {
            return String(format: "%.0f", answers.heightCm)
        }
        return String(format: "%.0f", answers.heightCm / 2.54)
    }

    private var weightDisplay: String {
        if answers.preferredUnits == .metric {
            return String(format: "%.1f", answers.weightKg)
        }
        return String(format: "%.1f", answers.weightKg / 0.453_592_37)
    }

    private func adjustHeight(_ delta: Double) {
        if answers.preferredUnits == .metric {
            answers.heightCm = max(120, min(230, answers.heightCm + delta))
        } else {
            answers.heightCm = max(120, min(230, answers.heightCm + delta * 2.54))
        }
    }

    private func adjustWeight(_ delta: Double) {
        if answers.preferredUnits == .metric {
            answers.weightKg = max(30, min(250, answers.weightKg + delta))
        } else {
            answers.weightKg = max(30, min(250, answers.weightKg + delta * 0.453_592_37))
        }
    }
}
