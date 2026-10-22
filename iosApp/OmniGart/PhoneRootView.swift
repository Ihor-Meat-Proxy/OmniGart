import SwiftUI

/// Phase 0 phone shell: Food / Workouts / Recovery / Planner / Profile.
/// Targets iOS 26+ Liquid Glass; appearance is system-driven on supported SDKs.
struct PhoneRootView: View {
    let summaryLines: [String]
    @State private var selectedTab: PhoneTab = .profile

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(PhoneTab.allCases) { tab in
                TabContent(tab: tab, summaryLines: summaryLines)
                    .tabItem {
                        Label(tab.titleKey, systemImage: tab.systemImage)
                    }
                    .tag(tab)
                    .accessibilityIdentifier(tab.accessibilityId)
            }
        }
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
                        .accessibilityIdentifier("brand_title")
                    Text("shell_placeholder")
                        .foregroundStyle(.secondary)
                }
                Section(tab.titleKey) {
                    ForEach(summaryLines, id: \.self) { line in
                        Text(line)
                    }
                }
            }
            .navigationTitle(Text(tab.titleKey))
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

    var systemImage: String {
        switch self {
        case .food: "fork.knife"
        case .workouts: "dumbbell"
        case .recovery: "heart.text.square"
        case .planner: "calendar"
        case .profile: "person.crop.circle"
        }
    }

    var accessibilityId: String { "tab_\(rawValue)" }
}

#Preview {
    PhoneRootView(summaryLines: ["OmniGart", "BMR 1349 kcal"])
}
