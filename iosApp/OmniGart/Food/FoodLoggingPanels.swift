import Shared
import SwiftUI

struct FoodSearchPanel: View {
    let core: OmniGartCore
    let profileId: String
    @Binding var tick: Int
    @State private var query = ""
    @State private var meal = MealGroup.breakfast

    private var results: [CatalogueFood] {
        _ = tick
        return Array(core.foodLogging.search(profileId: profileId, query: query, locale: AppLocale.tag))
    }

    var body: some View {
        Section("food_search_title") {
            TextField("food_search_placeholder", text: $query)
                .accessibilityIdentifier("food_search")
            Picker("food_meal_group", selection: $meal) {
                Text("food_meal_breakfast").tag(MealGroup.breakfast)
                Text("food_meal_lunch").tag(MealGroup.lunch)
                Text("food_meal_dinner").tag(MealGroup.dinner)
                Text("food_meal_snacks").tag(MealGroup.snacks)
            }
            .accessibilityIdentifier("food_meal_group")
            ForEach(results, id: \.id) { food in
                FoodCatalogueRow(core: core, profileId: profileId, food: food, meal: meal, tick: $tick)
            }
        }
    }
}

struct FoodCatalogueRow: View {
    let core: OmniGartCore
    let profileId: String
    let food: CatalogueFood
    let meal: MealGroup
    @Binding var tick: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(food.name)
            Text(String(format: "%.0f kcal / 100 g", food.energyKcalPer100g))
                .font(.caption)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            HStack {
                servingButton("food_serving_100g", .per100G)
                servingButton("food_serving_one", .oneServing)
                servingButton("food_serving_pack", .pack)
            }
        }
        .accessibilityIdentifier("food_catalogue_\(food.id)")
    }

    private func servingButton(_ key: LocalizedStringKey, _ serving: QuickServing) -> some View {
        Button(key) {
            _ = core.foodLogging.logQuickServingNow(
                profileId: profileId,
                foodId: food.id,
                serving: serving,
                mealGroup: meal,
                locale: AppLocale.tag
            )
            _ = core.logFood(
                profileId: profileId,
                name: food.name,
                energyKcal: food.energyKcalPer100g,
                proteinG: food.proteinGPer100g,
                carbsG: food.carbsGPer100g,
                fatG: food.fatGPer100g
            )
            tick += 1
        }
        .font(.caption)
    }
}

struct FoodRecentsFavouritesPanel: View {
    let core: OmniGartCore
    let profileId: String
    @Binding var tick: Int

    var body: some View {
        Section("food_recents_title") {
            let recents = Array(core.foodLogging.recents(profileId: profileId, limit: 8))
            if recents.isEmpty {
                Text("food_recents_empty").font(.footnote)
            } else {
                ForEach(recents, id: \.id) { entry in
                    HStack {
                        Button {
                            _ = core.foodLogging.repeatRecentNow(
                                profileId: profileId,
                                entryId: entry.id,
                                mealGroup: entry.mealGroup
                            )
                            _ = core.logFood(
                                profileId: profileId,
                                name: entry.name,
                                energyKcal: entry.energyKcal,
                                proteinG: entry.proteinG,
                                carbsG: entry.carbsG,
                                fatG: entry.fatG
                            )
                            tick += 1
                        } label: {
                            Text(entry.name)
                        }
                        Spacer()
                        Button {
                            _ = core.foodLogging.toggleFavourite(entryId: entry.id)
                            tick += 1
                        } label: {
                            Image(systemName: entry.favourite ? "star.fill" : "star")
                        }
                        .accessibilityIdentifier("food_fav_toggle_\(entry.id)")
                    }
                    .accessibilityIdentifier("food_recent_\(entry.id)")
                }
            }
        }
        Section("food_favourites_title") {
            let favs = Array(core.foodLogging.favourites(profileId: profileId))
            if favs.isEmpty {
                Text("food_favourites_empty").font(.footnote)
            } else {
                ForEach(favs, id: \.id) { entry in
                    Button {
                        _ = core.foodLogging.toggleFavourite(entryId: entry.id)
                        tick += 1
                    } label: {
                        Text(entry.name)
                    }
                    .accessibilityIdentifier("food_favourite_\(entry.id)")
                }
            }
        }
    }
}

struct FoodGapsPanel: View {
    let core: OmniGartCore
    let profileId: String
    let tick: Int

    var body: some View {
        Section("food_gaps_title") {
            Text(gapLines)
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                .accessibilityIdentifier("food_gaps")
            Text(bestSourcesLine)
                .font(.caption)
                .accessibilityIdentifier("food_best_sources")
        }
    }

    private var gapLines: String {
        _ = tick
        let endMs = Int64(Date().timeIntervalSince1970 * 1000)
        let age: Int32
        if let profile = core.profiles.get(id: profileId) {
            age = core.ageYears(profile: profile)
        } else {
            age = 30
        }
        let summary = core.foodLogging.fillGapsSummary(
            profileId: profileId,
            dayStartEpochMs: endMs - 86_400_000,
            dayEndEpochMs: endMs,
            ageYears: age,
            sex: .unspecified
        )
        if summary.isEmpty {
            return String(localized: "food_gaps_none")
        }
        return String(localized: "food_gaps_hint") + "\n" + summary
    }

    private var bestSourcesLine: String {
        _ = tick
        let ranks = core.foodLogging.bestSources(
            profileId: profileId,
            nutrient: NutrientId.vitaminC,
            basis: NutrientRankBasis.per100Kcal,
            categoryFilter: nil,
            limit: 3
        )
        guard !ranks.isEmpty else { return String(localized: "food_best_sources_title") }
        let names = ranks.map { $0.food.name }.joined(separator: ", ")
        return String(localized: "food_best_sources_title") + ": " + names
    }
}

struct FoodMorningReviewPanel: View {
    let core: OmniGartCore
    let profileId: String
    @State private var summary = ""

    var body: some View {
        Section("food_review_title") {
            let availability = FoodMorningReviewSupport.checkAvailability()
            if FeatureFlags.shared.morningFoodReviewEnabled, availability.usable {
                Text("food_review_ai_ready").font(.footnote)
            } else {
                Text(availability.reasonIfUnavailable ?? String(localized: "food_ai_unavailable"))
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            Button("food_review_run") { runReview() }
                .accessibilityIdentifier("food_review_run")
            if !summary.isEmpty {
                Text(summary)
                    .font(.footnote)
                    .accessibilityIdentifier("food_review_summary")
                Text("food_review_footer")
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
        }
    }

    private func runReview() {
        let endMs = Int64(Date().timeIntervalSince1970 * 1000)
        let startMs = endMs - 86_400_000
        let dayKey = FoodDayKeys.shared.dayKey(
            epochMs: startMs,
            dayStartMinutes: core.foodLogging.dayStartMinutes(profileId: profileId),
            utcOffsetMinutes: 0
        )
        let age: Int32
        if let profile = core.profiles.get(id: profileId) {
            age = core.ageYears(profile: profile)
        } else {
            age = 30
        }
        let review = core.foodLogging.morningReviewEpoch(
            profileId: profileId,
            dayKey: dayKey,
            dayStartEpochMs: startMs,
            dayEndEpochMs: endMs,
            locale: AppLocale.tag,
            ageYears: age,
            sex: .unspecified,
            availability: FoodMorningReviewSupport.checkAvailability()
        )
        summary = review.summary
    }
}

struct FoodSettingsPanel: View {
    let core: OmniGartCore
    let profileId: String
    @State private var offlineOnly = false
    @State private var dayStartHour = 4
    @State private var reviewHour = 8
    @State private var erased = false

    var body: some View {
        Section("food_settings_title") {
            Toggle("food_offline_only", isOn: $offlineOnly)
                .onChange(of: offlineOnly) { _, value in
                    persist(offline: value)
                }
                .accessibilityIdentifier("food_offline_only")
            Stepper(value: $dayStartHour, in: 0 ... 12) {
                Text("food_day_start \(dayStartHour):00")
            }
            .onChange(of: dayStartHour) { _, _ in persist(offline: offlineOnly) }
            Stepper(value: $reviewHour, in: 5 ... 11) {
                Text("food_review_time \(reviewHour):00")
            }
            .onChange(of: reviewHour) { _, _ in persist(offline: offlineOnly) }
            Button("food_erase_all", role: .destructive) {
                core.foodLogging.eraseAllAndCancelNotifications(profileId: profileId)
                erased = true
            }
            .accessibilityIdentifier("food_erase_all")
            if erased {
                Text("food_erase_done").font(.caption)
            }
            Text("food_encryption_note")
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Text("food_online_privacy_note")
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Text("food_reference_intake_note")
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
        .onAppear {
            let settings = core.foodLogging.settings(profileId: profileId)
            offlineOnly = settings.offlineOnly
            let startMins = settings.dayStartMinutes?.intValue ?? 240
            dayStartHour = Int(startMins / 60)
            reviewHour = Int(settings.morningReviewMinutes / 60)
        }
    }

    private func persist(offline: Bool) {
        core.foodLogging.updateFoodPreferences(
            profileId: profileId,
            offlineOnly: offline,
            dayStartMinutes: Int32(dayStartHour * 60),
            morningReviewMinutes: Int32(reviewHour * 60),
            morningAiReviewEnabled: false
        )
    }
}
