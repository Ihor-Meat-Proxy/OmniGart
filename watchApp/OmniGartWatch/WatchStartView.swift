import Shared
import SwiftUI

struct WatchStartView: View {
    let core: OmniGartCore
    let profileId: String
    let summaryLines: [String]

    var body: some View {
        NavigationStack {
            VStack(spacing: OmniGartMetrics.space2) {
                Text("app_name")
                    .font(.headline)
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)
                    .accessibilityIdentifier("watch_brand")
                Text("watch_subtitle")
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .multilineTextAlignment(.center)
                ForEach(summaryLines.prefix(2), id: \.self) { line in
                    Text(line)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }
                NavigationLink {
                    WatchStrengthStartView(core: core, profileId: profileId)
                } label: {
                    Text("watch_strength_start")
                }
                .tint(Color.OmniGart.colorBrandAccent)
                .accessibilityIdentifier("watch_strength_start")
                NavigationLink {
                    Zone2LiveView()
                } label: {
                    Text("watch.zone2.start")
                }
                .tint(Color.OmniGart.colorBrandAccent)
                .accessibilityIdentifier("watch_start_button")
            }
            .padding(OmniGartMetrics.space2)
            .accessibilityIdentifier("wear_start")
        }
    }
}

#Preview {
    WatchStartView(
        core: OmniGartCore(driverFactory: DatabaseDriverFactory()),
        profileId: "local-profile",
        summaryLines: ["OmniGart", "BMR 1349 kcal"]
    )
}
