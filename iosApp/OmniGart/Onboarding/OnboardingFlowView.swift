import HealthKit
import Shared
import SwiftUI

/// First-launch onboarding. Persists a completed local profile through Shared `OmniGartCore`.
struct OnboardingFlowView: View {
    let core: OmniGartCore
    let onFinished: (UserProfile) -> Void

    @State private var step: OnboardingStep = .welcome
    @State private var biologicalSex: BiologicalSex = .female
    @State private var primaryGoal: ProfileGoal = .generalFitness
    @State private var strengthLevel: StrengthLevel = .beginner
    @State private var activityLevel: ActivityLevel = .moderate
    @State private var preferredUnits: UnitSystem = .metric
    @State private var birthDate: Date = Calendar.current.date(
        from: DateComponents(year: 1994, month: 6, day: 15)
    ) ?? Date()
    @State private var heightCm: Double = 168
    @State private var weightKg: Double = 62
    @State private var requestingHealth = false

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: OmniGartMetrics.space3) {
                Text("app_name")
                    .font(.largeTitle.bold())
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)
                    .accessibilityIdentifier("onboarding_brand")

                stepContent
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

                HStack {
                    if step != .welcome {
                        Button("onboarding_back") {
                            step = step.previous
                        }
                        .accessibilityIdentifier("onboarding_back")
                    }
                    Spacer()
                    Button("onboarding_skip") {
                        finish(skipRemaining: true)
                    }
                    .accessibilityIdentifier("onboarding_skip")
                    if step == .healthKit {
                        Button("onboarding_finish") {
                            finish(skipRemaining: false)
                        }
                        .buttonStyle(.borderedProminent)
                        .accessibilityIdentifier("onboarding_finish")
                    } else {
                        Button("onboarding_next") {
                            step = step.next
                        }
                        .buttonStyle(.borderedProminent)
                        .accessibilityIdentifier("onboarding_next")
                    }
                }
            }
            .padding()
            .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
            .navigationTitle(Text(step.titleKey))
            .navigationBarTitleDisplayMode(.inline)
        }
        .accessibilityIdentifier("onboarding_flow")
    }

    @ViewBuilder
    private var stepContent: some View {
        switch step {
        case .welcome:
            VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
                Text("onboarding_welcome_title")
                    .font(.title2.bold())
                Text("onboarding_welcome_body")
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                WellnessDisclaimerBanner()
            }
            .accessibilityIdentifier("onboarding_step_welcome")
        case .goal:
            choiceList(
                titleKey: "onboarding_goal_title",
                options: [
                    ProfileGoal.generalFitness,
                    ProfileGoal.strength,
                    ProfileGoal.endurance,
                    ProfileGoal.mobility,
                    ProfileGoal.fatLossSupport
                ],
                selected: primaryGoal,
                label: { goalLabel($0) },
                idPrefix: "onboarding_goal",
                onSelect: { primaryGoal = $0 }
            )
            .accessibilityIdentifier("onboarding_step_goal")
        case .sex:
            choiceList(
                titleKey: "onboarding_sex_title",
                subtitleKey: "onboarding_sex_body",
                options: [BiologicalSex.male, BiologicalSex.female],
                selected: biologicalSex,
                label: { sexLabel($0) },
                idPrefix: "onboarding_sex",
                onSelect: { biologicalSex = $0 }
            )
            .accessibilityIdentifier("onboarding_step_sex")
        case .birthDate:
            VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
                Text("onboarding_birth_title")
                    .font(.title3.bold())
                Text("onboarding_birth_body")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                DatePicker(
                    "onboarding_birth_title",
                    selection: $birthDate,
                    in: ...Date(),
                    displayedComponents: .date
                )
                .datePickerStyle(.wheel)
                .labelsHidden()
                .accessibilityIdentifier("onboarding_birth_picker")
            }
            .accessibilityIdentifier("onboarding_step_birth")
        case .height:
            measurementStepper(
                titleKey: "onboarding_height_title",
                value: heightDisplay,
                unitLabel: preferredUnits == .metric ? "onboarding_unit_cm" : "onboarding_unit_in",
                decrement: { adjustHeight(-1) },
                increment: { adjustHeight(1) },
                id: "onboarding_height"
            )
            .accessibilityIdentifier("onboarding_step_height")
        case .weight:
            measurementStepper(
                titleKey: "onboarding_weight_title",
                value: weightDisplay,
                unitLabel: preferredUnits == .metric ? "onboarding_unit_kg" : "onboarding_unit_lb",
                decrement: { adjustWeight(-0.5) },
                increment: { adjustWeight(0.5) },
                id: "onboarding_weight"
            )
            .accessibilityIdentifier("onboarding_step_weight")
        case .experience:
            choiceList(
                titleKey: "onboarding_experience_title",
                options: [
                    StrengthLevel.beginner,
                    StrengthLevel.novice,
                    StrengthLevel.intermediate,
                    StrengthLevel.advanced
                ],
                selected: strengthLevel,
                label: { experienceLabel($0) },
                idPrefix: "onboarding_experience",
                onSelect: { strengthLevel = $0 }
            )
            .accessibilityIdentifier("onboarding_step_experience")
        case .units:
            choiceList(
                titleKey: "onboarding_units_title",
                options: [UnitSystem.metric, UnitSystem.imperial],
                selected: preferredUnits,
                label: { unitsLabel($0) },
                idPrefix: "onboarding_units",
                onSelect: { preferredUnits = $0 }
            )
            .accessibilityIdentifier("onboarding_step_units")
        case .activity:
            choiceList(
                titleKey: "onboarding_activity_title",
                options: [
                    ActivityLevel.sedentary,
                    ActivityLevel.light,
                    ActivityLevel.moderate,
                    ActivityLevel.active,
                    ActivityLevel.veryActive
                ],
                selected: activityLevel,
                label: { activityLabel($0) },
                idPrefix: "onboarding_activity",
                onSelect: { activityLevel = $0 }
            )
            .accessibilityIdentifier("onboarding_step_activity")
        case .healthKit:
            VStack(alignment: .leading, spacing: OmniGartMetrics.space3) {
                Text("perm_explainer_health_title")
                    .font(.title3.bold())
                Text("perm_explainer_health_body")
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                Text("onboarding_health_privacy")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                HStack {
                    Button("perm_not_now") {
                        // Permission optional — finish still saves the profile.
                    }
                    .accessibilityIdentifier("onboarding_health_not_now")
                    Spacer()
                    Button("perm_continue") {
                        requestHealthAccess()
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(requestingHealth)
                    .accessibilityIdentifier("onboarding_health_continue")
                }
            }
            .accessibilityIdentifier("onboarding_step_health")
        }
    }

    private func choiceList<T: Hashable>(
        titleKey: LocalizedStringKey,
        subtitleKey: LocalizedStringKey? = nil,
        options: [T],
        selected: T,
        label: @escaping (T) -> LocalizedStringKey,
        idPrefix: String,
        onSelect: @escaping (T) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text(titleKey)
                .font(.title3.bold())
            if let subtitleKey {
                Text(subtitleKey)
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            ForEach(Array(options.enumerated()), id: \.offset) { _, option in
                Button {
                    onSelect(option)
                } label: {
                    HStack {
                        Text(label(option))
                        Spacer()
                        if option == selected {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(Color.OmniGart.colorBrandAccent)
                        }
                    }
                }
                .buttonStyle(.plain)
                .padding(.vertical, 6)
                .accessibilityIdentifier("\(idPrefix)_\(String(describing: option).lowercased())")
            }
        }
    }

    private func measurementStepper(
        titleKey: LocalizedStringKey,
        value: String,
        unitLabel: LocalizedStringKey,
        decrement: @escaping () -> Void,
        increment: @escaping () -> Void,
        id: String
    ) -> some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text(titleKey)
                .font(.title3.bold())
            HStack {
                Button("-", action: decrement)
                    .accessibilityIdentifier("\(id)_decrement")
                HStack(spacing: 4) {
                    Text(value)
                    Text(unitLabel)
                }
                .accessibilityIdentifier("\(id)_value")
                Button("+", action: increment)
                    .accessibilityIdentifier("\(id)_increment")
            }
            .font(.title2.monospacedDigit())
        }
    }

    private var heightDisplay: String {
        if preferredUnits == .metric {
            return String(format: "%.0f", heightCm)
        }
        let inches = heightCm / 2.54
        return String(format: "%.0f", inches)
    }

    private var weightDisplay: String {
        if preferredUnits == .metric {
            return String(format: "%.1f", weightKg)
        }
        let pounds = weightKg / 0.453_592_37
        return String(format: "%.1f", pounds)
    }

    private func adjustHeight(_ delta: Double) {
        if preferredUnits == .metric {
            heightCm = max(120, min(230, heightCm + delta))
        } else {
            heightCm = max(120, min(230, heightCm + delta * 2.54))
        }
    }

    private func adjustWeight(_ delta: Double) {
        if preferredUnits == .metric {
            weightKg = max(30, min(250, weightKg + delta))
        } else {
            weightKg = max(30, min(250, weightKg + delta * 0.453_592_37))
        }
    }

    private func requestHealthAccess() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        requestingHealth = true
        var read: Set<HKObjectType> = []
        var write: Set<HKSampleType> = []
        if let heart = HealthKitPlatformBridge.heartRateType() {
            read.insert(heart)
            write.insert(heart)
        }
        if let resting = HealthKitPlatformBridge.restingHeartRateType() { read.insert(resting) }
        if let hrv = HealthKitPlatformBridge.hrvType() { read.insert(hrv) }
        if let sleep = HealthKitPlatformBridge.sleepType() { read.insert(sleep) }
        if let water = HealthKitPlatformBridge.dietaryWaterType() {
            read.insert(water)
            write.insert(water)
        }
        if let active = HealthKitPlatformBridge.activeEnergyType() {
            read.insert(active)
            write.insert(active)
        }
        if let basal = HealthKitPlatformBridge.basalEnergyType() {
            read.insert(basal)
            write.insert(basal)
        }
        // Workouts: read existing Fitness history + write OmniGart sessions.
        read.insert(HealthKitPlatformBridge.workoutType())
        write.insert(HealthKitPlatformBridge.workoutType())
        Task {
            defer { requestingHealth = false }
            do {
                try await HealthKitPlatformBridge.shared.requestAuthorization(
                    readTypes: read,
                    writeTypes: write
                )
            } catch {
                // Declined or unavailable — onboarding still completes.
            }
        }
    }

    private func finish(skipRemaining _: Bool) {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: birthDate)
        let completed = core.completeOnboardingAnswers(
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
        onFinished(completed)
    }

    private func goalLabel(_ goal: ProfileGoal) -> LocalizedStringKey {
        switch goal {
        case .generalFitness: "onboarding_goal_general_fitness"
        case .strength: "onboarding_goal_strength"
        case .endurance: "onboarding_goal_endurance"
        case .mobility: "onboarding_goal_mobility"
        case .fatLossSupport: "onboarding_goal_fat_loss_support"
        default: "onboarding_goal_general_fitness"
        }
    }

    private func sexLabel(_ sex: BiologicalSex) -> LocalizedStringKey {
        switch sex {
        case .male: "onboarding_sex_male"
        case .female: "onboarding_sex_female"
        default: "onboarding_sex_female"
        }
    }

    private func experienceLabel(_ level: StrengthLevel) -> LocalizedStringKey {
        switch level {
        case .beginner: "onboarding_experience_beginner"
        case .novice: "onboarding_experience_novice"
        case .intermediate: "onboarding_experience_intermediate"
        case .advanced: "onboarding_experience_advanced"
        default: "onboarding_experience_beginner"
        }
    }

    private func unitsLabel(_ units: UnitSystem) -> LocalizedStringKey {
        switch units {
        case .metric: "onboarding_units_metric"
        case .imperial: "onboarding_units_imperial"
        default: "onboarding_units_metric"
        }
    }

    private func activityLabel(_ level: ActivityLevel) -> LocalizedStringKey {
        switch level {
        case .sedentary: "onboarding_activity_sedentary"
        case .light: "onboarding_activity_light"
        case .moderate: "onboarding_activity_moderate"
        case .active: "onboarding_activity_active"
        case .veryActive: "onboarding_activity_very_active"
        default: "onboarding_activity_moderate"
        }
    }
}

private enum OnboardingStep: Int, CaseIterable {
    case welcome
    case goal
    case sex
    case birthDate
    case height
    case weight
    case experience
    case units
    case activity
    case healthKit

    var titleKey: LocalizedStringKey {
        switch self {
        case .welcome: "onboarding_welcome_title"
        case .goal: "onboarding_goal_title"
        case .sex: "onboarding_sex_title"
        case .birthDate: "onboarding_birth_title"
        case .height: "onboarding_height_title"
        case .weight: "onboarding_weight_title"
        case .experience: "onboarding_experience_title"
        case .units: "onboarding_units_title"
        case .activity: "onboarding_activity_title"
        case .healthKit: "perm_explainer_health_title"
        }
    }

    var next: OnboardingStep {
        let all = OnboardingStep.allCases
        let idx = all.firstIndex(of: self) ?? 0
        return all[min(idx + 1, all.count - 1)]
    }

    var previous: OnboardingStep {
        let all = OnboardingStep.allCases
        let idx = all.firstIndex(of: self) ?? 0
        return all[max(idx - 1, 0)]
    }
}
