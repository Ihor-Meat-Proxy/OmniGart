import SwiftUI

struct OnboardingChoiceConfig<T: Hashable> {
    var titleKey: LocalizedStringKey
    var subtitleKey: LocalizedStringKey?
    var options: [T]
    var selected: T
    var label: (T) -> LocalizedStringKey
    var idPrefix: String
    var onSelect: (T) -> Void
}

struct OnboardingChoiceList<T: Hashable>: View {
    let config: OnboardingChoiceConfig<T>

    var body: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text(config.titleKey)
                .font(.title3.bold())
            if let subtitleKey = config.subtitleKey {
                Text(subtitleKey)
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            ForEach(Array(config.options.enumerated()), id: \.offset) { _, option in
                Button {
                    config.onSelect(option)
                } label: {
                    HStack {
                        Text(config.label(option))
                        Spacer()
                        if option == config.selected {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(Color.OmniGart.colorBrandAccent)
                        }
                    }
                }
                .buttonStyle(.plain)
                .padding(.vertical, 6)
                .accessibilityIdentifier(
                    "\(config.idPrefix)_\(String(describing: option).lowercased())"
                )
            }
        }
    }
}
