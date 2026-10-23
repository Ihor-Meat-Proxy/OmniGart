import Shared
import SwiftUI

/// Food-tab card: suggested energy, confidence, and why the target changed.
struct NutritionTargetCard: View {
    let core: OmniGartCore
    let profileId: String

    @State private var suggestedText = "—"
    @State private var confidenceKey = "nutrition_confidence_rough"
    @State private var reasonKeys: [String] = []
    @State private var showSupportive = false
    @State private var floorWarn = false

    var body: some View {
        Section {
            VStack(alignment: .leading, spacing: 8) {
                Text("nutrition_target_title")
                    .font(.headline)
                Text(suggestedText)
                    .font(.title2.monospacedDigit())
                    .accessibilityIdentifier("nutrition_target_kcal")
                Text("nutrition_target_disclaimer")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                HStack {
                    Text("nutrition_confidence_title")
                    Spacer()
                    Text(LocalizedStringKey(confidenceKey))
                        .accessibilityIdentifier("nutrition_confidence")
                }
                .font(.subheadline)
                if !reasonKeys.isEmpty {
                    Text("nutrition_reason_title")
                        .font(.subheadline.weight(.semibold))
                    ForEach(reasonKeys, id: \.self) { key in
                        Text(LocalizedStringKey(key))
                            .font(.footnote)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                }
                if floorWarn {
                    Text("nutrition_floor_warning")
                        .font(.footnote)
                        .foregroundStyle(Color.red)
                        .accessibilityIdentifier("nutrition_floor_warning")
                }
                if showSupportive {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("nutrition_low_intake_title")
                            .font(.subheadline.weight(.semibold))
                        Text("nutrition_low_intake_body")
                            .font(.footnote)
                    }
                    .accessibilityIdentifier("nutrition_supportive")
                }
                NavigationLink("nutrition_open_measurements") {
                    BodyMeasurementsView()
                }
            }
            .padding(.vertical, 4)
        }
        .onAppear(perform: refresh)
    }

    private func refresh() {
        guard let profile = core.profiles.get(id: profileId) else { return }
        let store = NutritionLocalStore.shared
        let goal = store.loadGoal()
        let weights = store.loadWeights()
        let bodyFat = store.loadBodyFat().last?.bodyFatPercent
        let birthYear = Int(core.birthYear(profile: profile))
        let age = max(1, Calendar.current.component(.year, from: Date()) - birthYear)
        let weightValues = KotlinDoubleArray(size: Int32(weights.count))
        let epochValues = KotlinLongArray(size: Int32(weights.count))
        for (index, sample) in weights.enumerated() {
            weightValues.set(index: Int32(index), value: sample.massKg)
            epochValues.set(index: Int32(index), value: sample.epochMs)
        }
        let previous = store.lastTargetKcal()
        let result = NutritionFacade.shared.computeForProfile(
            sexCode: profile.biologicalSex == .male ? "MALE" : "FEMALE",
            ageYears: Int32(age),
            heightCm: profile.heightCm,
            weightKg: weights.last?.massKg ?? profile.weightKg,
            bodyFatPercent: bodyFat ?? 0,
            hasBodyFat: bodyFat != nil,
            goalKindCode: goal.kind,
            rateFractionPerWeek: goal.rateFractionPerWeek,
            targetWeightKg: goal.targetWeightKg ?? 0,
            hasTargetWeight: goal.targetWeightKg != nil,
            modeCode: goal.mode,
            manualDailyKcal: goal.manualDailyKcal ?? 0,
            hasManualDaily: goal.manualDailyKcal != nil,
            previousTargetKcal: previous ?? 0,
            hasPreviousTarget: previous != nil,
            recentWeightKg: weightValues,
            recentWeightEpochMs: epochValues,
            meanSteps: 8_000,
            dayStartMinutes: Int32(goal.dayStartMinutes)
        )
        let kcal = NutritionFacade.shared.suggestedKcalOrNull(result: result)
        if kcal > 0 {
            suggestedText = String(
                format: "%.0f %@",
                kcal,
                String(localized: "nutrition_kcal_unit")
            )
            store.saveLastTargetKcal(kcal)
        } else {
            suggestedText = "—"
        }
        confidenceKey = NutritionFacade.shared.confidenceLabelKey(level: result.confidence)
        reasonKeys = NutritionFacade.shared.changeReasonKeys(result: result).map { $0 as String }
        floorWarn = result.floorWarning?.belowFloor == true
        showSupportive = result.lowIntakeSupportive
    }
}
