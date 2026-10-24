import SwiftUI
import Shared

/// Phone shell: Food / Workouts / Recovery / Planner / Profile.
/// Colours and tab icons come from design/generated (DTCG → Swift / asset catalog).
struct PhoneRootView: View {
    let core: OmniGartCore
    let profileId: String
    @State private var selectedTab: PhoneTab = .profile
    @State private var showInfoSheet = false

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
                // Identifier must live on `Tab` so XCTest sees it on the tab bar button
                // (putting it on the page root only exposes the selected tab's content).
                // Keep a single identifier — labeling both Label and Tab makes UITest
                // `descendants["tab_*"]` ambiguous ("Multiple matching elements").
                .accessibilityIdentifier(tab.accessibilityId)
            }
        }
        // CI boots an iPad simulator; keep a bottom tab bar so tab accessibility
        // identifiers stay on `tabBars.buttons` instead of the adaptive sidebar.
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
