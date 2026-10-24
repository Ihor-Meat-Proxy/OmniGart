import Shared
import SwiftUI

/// Calories eaten vs target, remaining, macros, and water with quick add.
struct FoodDailySummarySection: View {
    let core: OmniGartCore
    let profileId: String
    let dayOffset: Int
    let tick: Int
    var onEditGoal: () -> Void
    var onWaterChanged: () -> Void

    var body: some View {
        Section("food_summary_title") {
            let snap = snapshot
            HStack {
                labeled("food_eaten", value: String(format: "%.0f", snap.eatenKcal))
                Spacer()
                labeled("food_target", value: snap.targetText)
            }
            labeled("food_remaining", value: snap.remainingText)
                .accessibilityIdentifier("food_remaining")
            Text(snap.macroLine)
                .font(.caption)
                .monospacedDigit()
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                .accessibilityIdentifier("food_macro_vs_target")
            Text(snap.waterLine)
                .font(.subheadline)
                .accessibilityIdentifier("food_water_logged")
            Button("food_water_add_250") {
                NutritionLocalStore.shared.addWaterMl(250, dayKey: snap.dayKey)
                // dayKey from FoodDayWindow
                onWaterChanged()
            }
            .accessibilityIdentifier("food_water_add_250")
            Button("food_edit_goal", action: onEditGoal)
                .accessibilityIdentifier("food_edit_goal")
        }
        .accessibilityIdentifier("food_totals")
    }

    private func labeled(_ key: LocalizedStringKey, value: String) -> some View {
        HStack(spacing: 4) {
            Text(key)
            Text(value).monospacedDigit()
        }
    }

    private var snapshot: SummarySnapshot {
        _ = tick
        return SummarySnapshot.build(core: core, profileId: profileId, dayOffset: dayOffset)
    }
}

private struct SummarySnapshot {
    var eatenKcal: Double
    var targetText: String
    var remainingText: String
    var macroLine: String
    var waterLine: String
    var dayKey: String

    @MainActor
    static func build(core: OmniGartCore, profileId: String, dayOffset: Int) -> SummarySnapshot {
        let bounds = FoodDayBounds.range(dayOffset: dayOffset)
        let diaryTotals = core.foodLogging.totalsForDayEpoch(
            profileId: profileId,
            dayStartEpochMs: bounds.startMs,
            dayEndEpochMs: bounds.endMs
        )
        // Prefer rich diary; fall back to SQLDelight day totals for today when diary empty.
        var eaten = diaryTotals.energyKcal
        var protein = diaryTotals.proteinG
        var carbs = diaryTotals.carbsG
        var fat = diaryTotals.fatG
        if diaryTotals.entryCount == 0 {
            let legacy = core.foodDayTotalsForDayOffset(profileId: profileId, dayOffset: Int32(dayOffset))
            eaten = legacy.energyKcal
            protein = legacy.proteinG
            carbs = legacy.carbsG
            fat = legacy.fatG
        }
        let card = targetCard(core: core, profileId: profileId)
        let target = (card?.suggestedDailyKcal ?? -1) > 0 ? (card?.suggestedDailyKcal ?? 0) : 0
        let remaining = target > 0 ? target - eaten : 0
        let targetText =
            target > 0
            ? String(format: "%.0f %@", target, String(localized: "nutrition_kcal_unit"))
            : "—"
        let remainingText =
            target > 0
            ? String(format: "%.0f %@", remaining, String(localized: "nutrition_kcal_unit"))
            : "—"
        let pT = (card?.proteinG ?? -1) > 0 ? (card?.proteinG ?? 0) : 0
        let cT = (card?.carbsG ?? -1) > 0 ? (card?.carbsG ?? 0) : 0
        let fT = (card?.fatG ?? -1) > 0 ? (card?.fatG ?? 0) : 0
        let macroLine = String(
            format: String(localized: "food_macro_line"),
            protein, pT, carbs, cT, fat, fT
        )
        let waterMl = NutritionLocalStore.shared.waterMl(for: bounds.dayKey)
        let waterGoal = (card?.waterLitres ?? -1) > 0 ? (card?.waterLitres ?? 0) : 0
        let waterLine = String(
            format: String(localized: "food_water_logged"),
            waterMl,
            waterGoal
        )
        return SummarySnapshot(
            eatenKcal: eaten,
            targetText: targetText,
            remainingText: remainingText,
            macroLine: macroLine,
            waterLine: waterLine,
            dayKey: bounds.dayKey
        )
    }

    @MainActor
    private static func targetCard(core: OmniGartCore, profileId: String) -> NutritionCardSnapshot? {
        guard let profile = core.profiles.get(id: profileId) else { return nil }
        let store = NutritionLocalStore.shared
        let goal = store.loadGoal()
        let weights = store.loadWeights()
        let bodyFat = store.loadBodyFat().last?.bodyFatPercent
        let birthYear = Int(core.birthYear(profile: profile))
        let age = max(1, Calendar.current.component(.year, from: Date()) - birthYear)
        let previous = store.lastTargetKcal()
        return NutritionFacade.shared.computeCardSnapshot(
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
            addExerciseCalories: goal.addExerciseCalories,
            exerciseKcalToday: 0,
            previousTargetKcal: previous ?? 0,
            hasPreviousTarget: previous != nil,
            recentWeightKg: KotlinPrimitiveArrays.doubleArray(weights.map(\.massKg)),
            recentWeightEpochMs: KotlinPrimitiveArrays.longArray(weights.map(\.epochMs)),
            meanSteps: 8_000,
            dayStartMinutes: Int32(goal.dayStartMinutes),
            trainingHoursToday: 0
        )
    }
}
