import SwiftUI
import Shared

/// Phone shell: Food / Workouts / Recovery / Planner / Profile.
/// Colours and tab icons come from design/generated (DTCG → Swift / asset catalog).
struct PhoneRootView: View {
    let core: OmniGartCore
    let profileId: String
    @State private var selectedTab: PhoneTab = Self.initialTab
    @State private var showInfoSheet = false

    /// UITest launches (`-ui-testing`) open on Food so content identifiers are in the
    /// hierarchy without relying on iPad tab-bar a11y (often empty under XCTest).
    private static var initialTab: PhoneTab {
        ProcessInfo.processInfo.arguments.contains("-ui-testing") ? .food : .profile
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(PhoneTab.allCases) { tab in
                Tab(value: tab) {
                    NavigationStack {
                        tabRoot(tab)
                    }
                } label: {
                    Label {
                        Text(tab.titleKey)
                    } icon: {
                        Image(tab.iconAssetName)
                            .renderingMode(.template)
                    }
                }
                // Identifier must live on `Tab` so XCTest can target the tab item.
                .accessibilityIdentifier(tab.accessibilityId)
            }
        }
        // Prefer a bottom tab bar on iPad too (CI destination is an iPad simulator).
        .tabViewStyle(.tabBarOnly)
        .tint(Color.OmniGart.colorBrandAccent)
        .accessibilityIdentifier("phone_shell")
        .sheet(isPresented: $showInfoSheet) {
            WellnessInfoSheet()
        }
    }

    @ViewBuilder
    private func tabRoot(_ tab: PhoneTab) -> some View {
        switch tab {
        case .food:
            FoodTabView(core: core, profileId: profileId)
        case .workouts:
            WorkoutsTabView(core: core, profileId: profileId)
        case .recovery:
            RecoveryTabView(core: core, profileId: profileId, showInfoSheet: $showInfoSheet)
        case .planner:
            PlannerTabView(core: core, profileId: profileId)
        case .profile:
            ProfileTabView(core: core, profileId: profileId, showInfoSheet: $showInfoSheet)
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
