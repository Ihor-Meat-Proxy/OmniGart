import SwiftUI

struct OnboardingMeasurementConfig {
    var titleKey: LocalizedStringKey
    var value: String
    var unitLabel: LocalizedStringKey
    var decrement: () -> Void
    var increment: () -> Void
    var id: String
}

struct OnboardingMeasurementStepper: View {
    let config: OnboardingMeasurementConfig

    var body: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text(config.titleKey)
                .font(.title3.bold())
            HStack {
                Button("-", action: config.decrement)
                    .accessibilityIdentifier("\(config.id)_decrement")
                HStack(spacing: 4) {
                    Text(config.value)
                    Text(config.unitLabel)
                }
                .accessibilityIdentifier("\(config.id)_value")
                Button("+", action: config.increment)
                    .accessibilityIdentifier("\(config.id)_increment")
            }
            .font(.title2.monospacedDigit())
        }
    }
}
