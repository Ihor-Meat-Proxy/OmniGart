import SwiftUI

/// Onboarding / profile wellness disclaimer (not medical advice).
struct WellnessDisclaimerBanner: View {
    var body: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text("onboarding_wellness_title")
                .font(.headline)
            Text("wellness_disclaimer")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Text("health_warning_chest_pain")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
        .accessibilityIdentifier("wellness_disclaimer")
    }
}

/// ⓘ sheet: estimate framing + required disclaimer footer.
struct WellnessInfoSheet: View {
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: OmniGartMetrics.space3) {
                    Text("info_sheet_title")
                        .font(.title2.bold())
                    Text("info_sheet_body")
                    Text("info_sheet_not_medical_advice")
                        .font(.footnote.weight(.semibold))
                        .accessibilityIdentifier("info_sheet_disclaimer")
                }
                .padding()
            }
            .navigationTitle(Text("info_sheet_title"))
        }
        .accessibilityIdentifier("wellness_info_sheet")
    }
}

/// Just-in-time pre-permission explainer; app stays usable if the user declines.
struct PermissionExplainerView: View {
    let titleKey: LocalizedStringKey
    let bodyKey: LocalizedStringKey
    let onContinue: () -> Void
    let onNotNow: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space3) {
            Text(titleKey)
                .font(.title2.bold())
            Text(bodyKey)
                .font(.body)
            HStack {
                Button("perm_not_now", action: onNotNow)
                Spacer()
                Button("perm_continue", action: onContinue)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .accessibilityIdentifier("permission_explainer")
    }
}
