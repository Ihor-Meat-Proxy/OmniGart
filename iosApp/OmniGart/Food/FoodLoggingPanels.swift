import Shared
import SwiftUI

/// Catalogue search — separate file for SwiftLint length limits.
struct FoodSearchPanel: View {
    let core: OmniGartCore
    let profileId: String
    @Binding var tick: Int
    @State private var query = ""

    private var results: [CatalogueFood] {
        _ = tick
        return Array(
            core.foodLogging.search(
                profileId: profileId,
                query: query,
                locale: "en-GB"
            )
        )
    }

    var body: some View {
        Section("food_search_title") {
            TextField("food_search_placeholder", text: $query)
                .accessibilityIdentifier("food_search")
            ForEach(results, id: \.id) { food in
                Button {
                    logCatalogueFood(food)
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(food.name)
                        Text(String(format: "%.0f kcal / 100 g", food.energyKcalPer100g))
                            .font(.caption)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                }
                .accessibilityIdentifier("food_catalogue_\(food.id)")
            }
        }
    }

    private func logCatalogueFood(_ food: CatalogueFood) {
        _ = core.foodLogging.logFromCatalogue(
            profileId: profileId,
            foodId: food.id,
            grams: 100.0,
            mealGroup: .breakfast,
            locale: "en-GB"
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
}

struct FoodGapsPanel: View {
    var body: some View {
        Section("food_gaps_title") {
            Text("food_gaps_hint")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                .accessibilityIdentifier("food_gaps")
        }
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
                Text("food_review_ai_ready")
                    .font(.footnote)
            } else {
                Text(availability.reason ?? String(localized: "food_ai_unavailable"))
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            Button("food_review_run") {
                runReview()
            }
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
        let dayStartMinutes = core.foodLogging.dayStartMinutes(profileId: profileId)
        let dayKey = FoodDayKeys.shared.dayKey(
            epochMs: startMs,
            dayStartMinutes: dayStartMinutes,
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
            locale: "en-GB",
            ageYears: age,
            sex: .unspecified,
            availability: FoodMorningReviewSupport.checkAvailability().toShared()
        )
        summary = review.summary
    }
}
