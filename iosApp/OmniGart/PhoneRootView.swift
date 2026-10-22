import SwiftUI

/// Phase 0 phone shell: Food / Workouts / Recovery / Planner / Profile.
/// Colours and tab icons come from design/generated (DTCG → Swift / asset catalog).
struct PhoneRootView: View {
    let summaryLines: [String]
    @State private var selectedTab: PhoneTab = .profile

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(PhoneTab.allCases) { tab in
                TabContent(tab: tab, summaryLines: summaryLines)
                    .tabItem {
                        Label {
                            Text(tab.titleKey)
                        } icon: {
                            Image(tab.iconAssetName)
                                .renderingMode(.template)
                        }
                    }
                    .tag(tab)
                    .accessibilityIdentifier(tab.accessibilityId)
            }
        }
        .tint(Color.OmniGart.colorBrandAccent)
        .accessibilityIdentifier("phone_shell")
    }
}

private struct TabContent: View {
    let tab: PhoneTab
    let summaryLines: [String]

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("app_name")
                        .font(.largeTitle.bold())
                        .foregroundStyle(Color.OmniGart.colorBrandAccent)
                        .accessibilityIdentifier("brand_title")
                    Text("shell_placeholder")
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                }
                Section(tab.titleKey) {
                    ForEach(summaryLines, id: \.self) { line in
                        Text(line)
                    }
                }
            }
            .navigationTitle(Text(tab.titleKey))
            .scrollContentBackground(.hidden)
            .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
        }
    }
}

enum PhoneTab: String, CaseIterable, Identifiable {
    case food
    case workouts
    case recovery
    case planner
    case profile

    var id: String { rawValue }

    var titleKey: LocalizedStringKey {
        switch self {
        case .food: "tab_food"
        case .workouts: "tab_workouts"
        case .recovery: "tab_recovery"
        case .planner: "tab_planner"
        case .profile: "tab_profile"
        }
    }

    /// Imageset names from design/generated/ios/OmniGartIcons.xcassets.
    var iconAssetName: String {
        switch self {
        case .food: "food"
        case .workouts: "workouts"
        case .recovery: "recovery"
        case .planner: "planner"
        case .profile: "profile"
        }
    }

    var accessibilityId: String { "tab_\(rawValue)" }
}

#Preview {
    PhoneRootView(summaryLines: ["OmniGart", "BMR 1349 kcal"])
}
