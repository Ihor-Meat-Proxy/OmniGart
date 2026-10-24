import Shared
import SwiftUI

/// Edit onboarding profile data, calculation settings, and wellness info.
struct ProfileTabView: View {
    let core: OmniGartCore
    let profileId: String
    @Binding var showInfoSheet: Bool

    @State private var biologicalSex: BiologicalSex = .female
    @State private var primaryGoal: ProfileGoal = .generalFitness
    @State private var strengthLevel: StrengthLevel = .beginner
    @State private var activityLevel: ActivityLevel = .moderate
    @State private var preferredUnits: UnitSystem = .metric
    @State private var birthDate = Date()
    @State private var heightCm: Double = 168
    @State private var weightKg: Double = 62
    @State private var loaded = false
    @State private var savedBanner = false

    var body: some View {
        List {
            Section {
                Text("profile_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            Section("profile_edit_title") {
                Picker("onboarding_sex_title", selection: $biologicalSex) {
                    Text("onboarding_sex_male").tag(BiologicalSex.male)
                    Text("onboarding_sex_female").tag(BiologicalSex.female)
                }
                .accessibilityIdentifier("profile_sex")
                Picker("onboarding_goal_title", selection: $primaryGoal) {
                    Text("onboarding_goal_general_fitness").tag(ProfileGoal.generalFitness)
                    Text("onboarding_goal_strength").tag(ProfileGoal.strength)
                    Text("onboarding_goal_endurance").tag(ProfileGoal.endurance)
                    Text("onboarding_goal_mobility").tag(ProfileGoal.mobility)
                    Text("onboarding_goal_fat_loss_support").tag(ProfileGoal.fatLossSupport)
                }
                .accessibilityIdentifier("profile_goal")
                DatePicker(
                    "onboarding_birth_title",
                    selection: $birthDate,
                    in: ...Date(),
                    displayedComponents: .date
                )
                .accessibilityIdentifier("profile_birth")
                Stepper(
                    value: $heightCm,
                    in: 120...230,
                    step: 1
                ) {
                    Text("\(String(localized: "onboarding_height_title")): \(Int(heightCm)) cm")
                }
                .accessibilityIdentifier("profile_height")
                Stepper(
                    value: $weightKg,
                    in: 30...250,
                    step: 0.5
                ) {
                    Text(
                        "\(String(localized: "onboarding_weight_title")): \(String(format: "%.1f", weightKg)) kg"
                    )
                }
                .accessibilityIdentifier("profile_weight")
                Picker("onboarding_experience_title", selection: $strengthLevel) {
                    Text("onboarding_experience_beginner").tag(StrengthLevel.beginner)
                    Text("onboarding_experience_novice").tag(StrengthLevel.novice)
                    Text("onboarding_experience_intermediate").tag(StrengthLevel.intermediate)
                    Text("onboarding_experience_advanced").tag(StrengthLevel.advanced)
                }
                .accessibilityIdentifier("profile_experience")
                Picker("onboarding_units_title", selection: $preferredUnits) {
                    Text("onboarding_units_metric").tag(UnitSystem.metric)
                    Text("onboarding_units_imperial").tag(UnitSystem.imperial)
                }
                .accessibilityIdentifier("profile_units")
                Picker("onboarding_activity_title", selection: $activityLevel) {
                    Text("onboarding_activity_sedentary").tag(ActivityLevel.sedentary)
                    Text("onboarding_activity_light").tag(ActivityLevel.light)
                    Text("onboarding_activity_moderate").tag(ActivityLevel.moderate)
                    Text("onboarding_activity_active").tag(ActivityLevel.active)
                    Text("onboarding_activity_very_active").tag(ActivityLevel.veryActive)
                }
                .accessibilityIdentifier("profile_activity")
                Button("profile_save") {
                    save()
                }
                .accessibilityIdentifier("profile_save")
                if savedBanner {
                    Text("profile_saved")
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorBrandAccent)
                        .accessibilityIdentifier("profile_saved")
                }
            }
            Section {
                CalculationsSettingsView(core: core, profileId: profileId)
            }
            NutritionGoalSettingsView()
            NutritionReminderSettingsView()
            Section("workout_watch_settings_title") {
                StartWorkoutsOnWatchSettingsToggle()
            }
            Section("workout_mindful_settings_title") {
                MindfulMinutesSettingsToggle()
            }
            DataPrivacySection(core: core)
            Section {
                WellnessDisclaimerBanner()
                Button {
                    showInfoSheet = true
                } label: {
                    Label("info_sheet_title", systemImage: "info.circle")
                }
                .accessibilityIdentifier("info_sheet_toggle")
            }
            #if DEBUG
            Section {
                DebugMenu()
            }
            #endif
        }
        .navigationTitle(Text("tab_profile"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
        .onAppear(perform: loadIfNeeded)
    }

    private func loadIfNeeded() {
        guard !loaded else { return }
        loaded = true
        guard let profile = core.profiles.get(id: profileId) else { return }
        biologicalSex = profile.biologicalSex
        primaryGoal = profile.primaryGoal
        strengthLevel = profile.strengthLevel
        activityLevel = profile.activityLevel
        preferredUnits = profile.preferredUnits
        heightCm = profile.heightCm
        weightKg = profile.weightKg
        var components = DateComponents()
        components.year = Int(core.birthYear(profile: profile))
        components.month = Int(core.birthMonth(profile: profile))
        components.day = Int(core.birthDay(profile: profile))
        if let date = Calendar.current.date(from: components) {
            birthDate = date
        }
    }

    private func save() {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: birthDate)
        _ = core.completeOnboardingAnswers(
            biologicalSex: biologicalSex,
            heightCm: heightCm,
            weightKg: weightKg,
            birthYear: Int32(components.year ?? 1994),
            birthMonth: Int32(components.month ?? 6),
            birthDay: Int32(components.day ?? 15),
            preferredUnits: preferredUnits,
            primaryGoal: primaryGoal,
            strengthLevel: strengthLevel,
            activityLevel: activityLevel
        )
        savedBanner = true
    }
}
