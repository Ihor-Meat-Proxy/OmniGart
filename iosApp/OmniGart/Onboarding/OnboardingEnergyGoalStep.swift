import Shared
import SwiftUI

/// Energy goal (lose / maintain / gain) collected during onboarding.
struct OnboardingEnergyGoalStep: View {
    @Binding var answers: OnboardingAnswers

    var body: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text("onboarding_energy_goal_title")
                .font(.title3.bold())
            Text("onboarding_energy_goal_body")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            energyChoice("LOSE", label: "onboarding_energy_lose")
            energyChoice("MAINTAIN", label: "onboarding_energy_maintain")
            energyChoice("GAIN", label: "onboarding_energy_gain")
            if answers.energyGoalKind != "MAINTAIN" {
                Stepper(
                    value: $answers.energyRatePercent,
                    in: answers.energyGoalKind == "GAIN" ? 0.25...0.5 : 0.25...1.0,
                    step: 0.25
                ) {
                    Text(String(format: "%.2f%% / wk", answers.energyRatePercent))
                }
                .accessibilityIdentifier("onboarding_energy_rate")
            }
        }
        .accessibilityIdentifier("onboarding_step_energy_goal")
        .onAppear {
            if answers.primaryGoal == .fatLossSupport, answers.energyGoalKind == "MAINTAIN" {
                answers.energyGoalKind = "LOSE"
                answers.energyRatePercent = 0.5
            }
        }
    }

    private func energyChoice(_ kind: String, label: LocalizedStringKey) -> some View {
        Button {
            answers.energyGoalKind = kind
            if kind == "GAIN" {
                answers.energyRatePercent = min(answers.energyRatePercent, 0.5)
            }
            if kind == "LOSE", answers.primaryGoal == .fatLossSupport {
                answers.energyRatePercent = max(answers.energyRatePercent, 0.5)
            }
        } label: {
            HStack {
                Text(label)
                Spacer()
                if answers.energyGoalKind == kind {
                    Image(systemName: "checkmark")
                }
            }
        }
        .accessibilityIdentifier("onboarding_energy_\(kind.lowercased())")
    }
}
