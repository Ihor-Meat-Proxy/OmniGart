import HealthKit
import Shared
import SwiftUI

/// First-launch onboarding. Persists a completed local profile through Shared `OmniGartCore`.
struct OnboardingFlowView: View {
    let core: OmniGartCore
    let onFinished: (UserProfile) -> Void

    @State private var step: OnboardingStep = .welcome
    @State private var answers = OnboardingAnswers()
    @State private var requestingHealth = false

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: OmniGartMetrics.space3) {
                Text("app_name")
                    .font(.largeTitle.bold())
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)
                    .accessibilityIdentifier("onboarding_brand")

                OnboardingStepContentView(
                    step: step,
                    answers: $answers,
                    requestingHealth: requestingHealth,
                    onRequestHealth: requestHealthAccess
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

                footerButtons
            }
            .padding()
            .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
            .navigationTitle(Text(step.titleKey))
            .navigationBarTitleDisplayMode(.inline)
        }
        .accessibilityIdentifier("onboarding_flow")
    }

    private var footerButtons: some View {
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

    private func requestHealthAccess() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        requestingHealth = true
        var read: Set<HKObjectType> = []
        var write: Set<HKSampleType> = []
        if let heart = HealthKitPlatformBridge.heartRateType() { read.insert(heart) }
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
        let components = Calendar.current.dateComponents(
            [.year, .month, .day],
            from: answers.birthDate
        )
        let completed = core.completeOnboardingAnswers(
            biologicalSex: answers.biologicalSex,
            heightCm: answers.heightCm,
            weightKg: answers.weightKg,
            birthYear: Int32(components.year ?? 1994),
            birthMonth: Int32(components.month ?? 6),
            birthDay: Int32(components.day ?? 15),
            preferredUnits: answers.preferredUnits,
            primaryGoal: answers.primaryGoal,
            strengthLevel: answers.strengthLevel,
            activityLevel: answers.activityLevel
        )
        onFinished(completed)
    }
}
