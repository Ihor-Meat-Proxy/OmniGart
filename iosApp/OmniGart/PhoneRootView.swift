import SwiftUI
import Shared

/// Phone shell: Food / Workouts / Recovery / Planner / Profile.
/// Colours and tab icons come from design/generated (DTCG → Swift / asset catalog).
struct PhoneRootView: View {
    let core: OmniGartCore
    let profileId: String
    let summaryLines: [String]
    @State private var selectedTab: PhoneTab = .profile
    @State private var showInfoSheet = false

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(PhoneTab.allCases) { tab in
                TabContent(
                    tab: tab,
                    core: core,
                    profileId: profileId,
                    summaryLines: summaryLines,
                    showInfoSheet: $showInfoSheet
                )
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
        .sheet(isPresented: $showInfoSheet) {
            WellnessInfoSheet()
        }
    }
}

private struct TabContent: View {
    let tab: PhoneTab
    let core: OmniGartCore
    let profileId: String
    let summaryLines: [String]
    @Binding var showInfoSheet: Bool

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("app_name")
                        .font(.largeTitle.bold())
                        .foregroundStyle(Color.OmniGart.colorBrandAccent)
                        .accessibilityIdentifier("brand_title")
                    Text("shell_intro")
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                }
                Section(tab.titleKey) {
                    ForEach(summaryLines, id: \.self) { line in
                        Text(line)
                    }
                }
                if tab == .profile || tab == .recovery {
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
                if tab == .profile {
                    Section {
                        CalculationsSettingsView(core: core, profileId: profileId)
                    }
                }
                #if DEBUG
                Section {
                    DebugMenu()
                }
                #endif
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
