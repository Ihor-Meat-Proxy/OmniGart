import Shared
import SwiftUI

/// Offline food log: daily summary, per-meal add, goals, water, weigh-in prompt.
struct FoodTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var dayOffset = 0
    @State private var tick = 0
    @State private var addMeal: MealGroup?
    @State private var showAddSheet = false
    @State private var showGoalEditor = false
    @State private var showMeasurements = false

    var body: some View {
        List {
            Section {
                Text("food_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            dayNavSection
            weighInSection
            FoodDailySummarySection(
                core: core,
                profileId: profileId,
                dayOffset: dayOffset,
                tick: tick,
                onEditGoal: { showGoalEditor = true },
                onWaterChanged: { tick += 1 }
            )
            mealSection(.breakfast, title: "food_meal_breakfast")
            mealSection(.lunch, title: "food_meal_lunch")
            mealSection(.dinner, title: "food_meal_dinner")
            mealSection(.snacks, title: "food_meal_snacks")
            NutritionTargetCard(core: core, profileId: profileId)
            FoodGapsPanel(core: core, profileId: profileId, tick: tick)
            FoodToolsPanel(core: core, profileId: profileId, tick: $tick)
            FoodMorningReviewPanel(core: core, profileId: profileId)
            FoodSettingsPanel(core: core, profileId: profileId)
            Section {
                Text("food_attribution_short")
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .accessibilityIdentifier("food_attribution")
            }
        }
        .navigationTitle(Text("tab_food"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
        .sheet(isPresented: $showAddSheet) {
            if let meal = addMeal {
                FoodAddFoodSheet(core: core, profileId: profileId, meal: meal) {
                    tick += 1
                    showAddSheet = false
                }
            }
        }
        .sheet(isPresented: $showGoalEditor) {
            NavigationStack {
                Form {
                    NutritionGoalSettingsView()
                }
                .navigationTitle(Text("food_edit_goal"))
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("onboarding_finish") { showGoalEditor = false }
                    }
                }
            }
        }
        .sheet(isPresented: $showMeasurements) {
            NavigationStack {
                BodyMeasurementsView()
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("onboarding_finish") {
                                showMeasurements = false
                                tick += 1
                            }
                        }
                    }
            }
        }
    }

    private var dayNavSection: some View {
        Section {
            HStack {
                Button {
                    dayOffset -= 1
                    tick += 1
                } label: {
                    Image(systemName: "chevron.left")
                }
                .accessibilityIdentifier("food_day_previous")
                .accessibilityLabel(Text("food_day_previous"))
                Spacer()
                Text(FoodDayBounds.title(dayOffset: dayOffset))
                    .font(.headline)
                    .accessibilityIdentifier("food_day_label")
                Spacer()
                Button {
                    dayOffset = min(dayOffset + 1, 0)
                    tick += 1
                } label: {
                    Image(systemName: "chevron.right")
                }
                .disabled(dayOffset >= 0)
                .accessibilityIdentifier("food_day_next")
                .accessibilityLabel(Text("food_day_next"))
            }
        }
    }

    @ViewBuilder
    private var weighInSection: some View {
        if shouldPromptWeighIn {
            Section {
                Text("food_weigh_in_prompt")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                Button("food_weigh_in_action") {
                    showMeasurements = true
                }
                .accessibilityIdentifier("food_weigh_in_action")
            }
        }
    }

    private var shouldPromptWeighIn: Bool {
        _ = tick
        let weights = NutritionLocalStore.shared.loadWeights()
        let prefs = NutritionLocalStore.shared.loadReminders()
        guard prefs.weighInNudgeEnabled, !prefs.paused else { return false }
        guard let last = weights.last else { return true }
        let days = Int((Date().timeIntervalSince1970 * 1000 - Double(last.epochMs)) / 86_400_000)
        return days >= 3
    }

    private func mealSection(_ meal: MealGroup, title: LocalizedStringKey) -> some View {
        Section(title) {
            let entries = mealEntries(meal)
            if entries.isEmpty {
                Text("food_empty")
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            } else {
                ForEach(entries, id: \.id) { entry in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entry.name)
                        Text(
                            String(
                                format: "%.0f kcal · P %.0f · C %.0f · F %.0f",
                                entry.energyKcal,
                                entry.proteinG,
                                entry.carbsG,
                                entry.fatG
                            )
                        )
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                    .accessibilityIdentifier("food_entry_\(entry.id)")
                }
            }
            Button("food_add_meal") {
                addMeal = meal
                showAddSheet = true
            }
            // UITest looks for food_add without scrolling; attach to breakfast only.
            .accessibilityIdentifier(meal == .breakfast ? "food_add" : "food_add_\(meal.name)")
        }
    }

    private func mealEntries(_ meal: MealGroup) -> [FoodLogEntry] {
        _ = tick
        let bounds = FoodDayBounds.range(dayOffset: dayOffset)
        let all = Array(
            core.foodLogging.entriesForDayEpoch(
                profileId: profileId,
                dayStartEpochMs: bounds.startMs,
                dayEndEpochMs: bounds.endMs
            )
        )
        return all.filter { $0.mealGroup == meal }
    }
}
