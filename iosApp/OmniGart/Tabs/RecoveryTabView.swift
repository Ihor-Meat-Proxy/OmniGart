import Shared
import SwiftUI

/// Wellness recovery estimates with required disclaimer (not medical advice).
struct RecoveryTabView: View {
    let core: OmniGartCore
    let profileId: String
    @Binding var showInfoSheet: Bool

    private var profile: UserProfile? {
        core.profiles.get(id: profileId)
    }

    private var estimateLines: [String] {
        guard let profile else { return [] }
        return Array(core.summaryLines(profile: profile).dropFirst())
    }

    var body: some View {
        List {
            Section {
                Text("recovery_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            Section("recovery_estimates_title") {
                if estimateLines.isEmpty {
                    Text("recovery_empty")
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                } else {
                    ForEach(estimateLines, id: \.self) { line in
                        Text(line)
                    }
                }
            }
            .accessibilityIdentifier("recovery_estimates")
            Section {
                WellnessDisclaimerBanner()
                Button {
                    showInfoSheet = true
                } label: {
                    Label("info_sheet_title", systemImage: "info.circle")
                }
                .accessibilityIdentifier("info_sheet_toggle")
            }
        }
        .navigationTitle(Text("tab_recovery"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
    }
}
