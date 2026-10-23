package com.omnigart.shared.food

import com.omnigart.shared.food.catalogue.BundledUsdaCatalogue
import com.omnigart.shared.food.catalogue.CompositeFoodCatalogue
import com.omnigart.shared.food.catalogue.FoodCatalogue
import com.omnigart.shared.food.diary.FoodDiaryOps
import com.omnigart.shared.food.diary.FoodDiaryStore
import com.omnigart.shared.food.diary.InMemoryFoodDiaryStore
import com.omnigart.shared.food.health.DietaryNutritionSample
import com.omnigart.shared.food.health.FoodHealthSyncIds
import com.omnigart.shared.food.health.FoodSyncIdentity
import com.omnigart.shared.food.importing.FoodImportExport
import com.omnigart.shared.food.models.FoodLogDayTotals
import com.omnigart.shared.food.models.FoodLogEntry
import com.omnigart.shared.food.models.FoodSource
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.models.QuickMacroAdd
import com.omnigart.shared.food.nutrients.BestSources
import com.omnigart.shared.food.nutrients.BiologicalSexBand
import com.omnigart.shared.food.nutrients.NutrientGapAnalyzer
import com.omnigart.shared.food.nutrients.NutrientId
import com.omnigart.shared.food.nutrients.ReferenceIntakeResolver
import com.omnigart.shared.food.privacy.FoodPrivacy
import com.omnigart.shared.food.review.MorningFoodReview
import com.omnigart.shared.food.review.MorningFoodReviewService
import com.omnigart.shared.food.review.MorningReviewPromptInput
import com.omnigart.shared.food.review.OnDeviceAiAvailability
import com.omnigart.shared.food.settings.FoodSettings
import com.omnigart.shared.food.targets.FoodDayKeys
import com.omnigart.shared.food.targets.NutritionTargetsReader
import com.omnigart.shared.food.targets.StubNutritionTargetsReader
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Facade for food logging, catalogue search, gap analysis, import/export,
 * and optional morning review. Platform shells call into this from SwiftUI / Compose.
 */
class FoodLoggingService(
    val store: FoodDiaryStore = InMemoryFoodDiaryStore(),
    private val catalogue: FoodCatalogue = BundledUsdaCatalogue.demo(),
    private val targets: NutritionTargetsReader = StubNutritionTargetsReader(),
    private val reviewService: MorningFoodReviewService = MorningFoodReviewService(),
    private val clock: Clock = Clock.System
) {
    fun effectiveCatalogue(profileId: String): FoodCatalogue {
        val offlineOnly = store.settings(profileId).offlineOnly
        return if (catalogue is CompositeFoodCatalogue) {
            catalogue
        } else {
            CompositeFoodCatalogue(offline = catalogue, online = null, offlineOnly = offlineOnly)
        }
    }

    fun settings(profileId: String): FoodSettings = store.settings(profileId)

    fun saveSettings(settings: FoodSettings) = store.saveSettings(settings)

    fun dayStartMinutes(profileId: String): Int = store.settings(profileId).dayStartMinutes ?: targets.dayStartMinutes(profileId)

    fun dayKeyFor(profileId: String, at: Instant, utcOffsetMinutes: Int = 0): String =
        FoodDayKeys.dayKey(at.toEpochMilliseconds(), dayStartMinutes(profileId), utcOffsetMinutes)

    fun log(entry: FoodLogEntry): FoodLogEntry {
        store.upsert(entry)
        return entry
    }

    fun quickAdd(profileId: String, add: QuickMacroAdd, mealGroup: MealGroup = MealGroup.SNACKS, at: Instant = clock.now()): FoodLogEntry {
        val entry =
            FoodLogEntry(
                id = "food-${at.toEpochMilliseconds()}",
                profileId = profileId,
                name = add.name.trim(),
                mealGroup = mealGroup,
                source = FoodSource.CUSTOM,
                energyKcal = add.energyKcal,
                proteinG = add.proteinG,
                carbsG = add.carbsG,
                fatG = add.fatG,
                loggedAt = at
            )
        store.upsert(entry)
        return entry
    }

    fun logFromCatalogue(
        profileId: String,
        foodId: String,
        grams: Double,
        mealGroup: MealGroup,
        locale: String,
        at: Instant = clock.now()
    ): FoodLogEntry? {
        val food = effectiveCatalogue(profileId).all().firstOrNull { it.id == foodId } ?: return null
        val n = food.forGrams(grams)
        val entry =
            FoodLogEntry(
                id = "food-${at.toEpochMilliseconds()}",
                profileId = profileId,
                name = food.displayName(locale),
                mealGroup = mealGroup,
                source = food.source,
                barcode = food.barcode,
                catalogueId = food.id,
                servingGrams = grams,
                energyKcal = n.energyKcal,
                proteinG = n.proteinG,
                carbsG = n.carbsG,
                fatG = n.fatG,
                fibreG = n.fibreG,
                sugarsG = n.sugarsG,
                sodiumMg = n.sodiumMg,
                micros = n.micros,
                loggedAt = at
            )
        store.upsert(entry)
        return entry
    }

    fun dietarySampleFor(entry: FoodLogEntry): DietaryNutritionSample = DietaryNutritionSample(
        identity =
        FoodSyncIdentity(
            syncIdentifier = FoodHealthSyncIds.forEntry(entry.id),
            syncVersion = 1L
        ),
        displayName = entry.name,
        energyKcal = entry.energyKcal,
        proteinG = entry.proteinG,
        carbohydrateG = entry.carbsG,
        fatG = entry.fatG,
        fibreG = entry.fibreG,
        sugarsG = entry.sugarsG,
        sodiumMg = entry.sodiumMg,
        startEpochMs = entry.loggedAt.toEpochMilliseconds()
    )

    fun entriesForDay(profileId: String, dayStart: Instant, dayEnd: Instant): List<FoodLogEntry> =
        store.entriesForRange(profileId, dayStart, dayEnd)

    fun totalsForDay(profileId: String, dayStart: Instant, dayEnd: Instant): FoodLogDayTotals =
        FoodDiaryOps.dayTotals(entriesForDay(profileId, dayStart, dayEnd))

    fun search(profileId: String, query: String, locale: String): List<com.omnigart.shared.food.models.CatalogueFood> =
        effectiveCatalogue(profileId).search(query, locale)

    fun findBarcode(profileId: String, barcode: String) = effectiveCatalogue(profileId).findByBarcode(barcode)

    fun micronutrientGaps(profileId: String, totals: FoodLogDayTotals, ageYears: Int, sex: BiologicalSexBand) = NutrientGapAnalyzer.gaps(
        consumed = totals.micros,
        reference =
        ReferenceIntakeResolver(
            overrides =
            store.settings(profileId).referenceOverrides.mapNotNull { (k, v) ->
                NutrientId.fromRaw(k)?.let { it to v }
            }.toMap()
        ).resolve(ageYears, sex)
    )

    fun fillGaps(profileId: String, totals: FoodLogDayTotals, ageYears: Int, sex: BiologicalSexBand) = BestSources.fillGaps(
        gaps = micronutrientGaps(profileId, totals, ageYears, sex),
        foods = effectiveCatalogue(profileId).all()
    )

    fun copyDay(profileId: String, fromStart: Instant, fromEnd: Instant, toStart: Instant) = FoodDiaryOps.copyDay(
        store = store,
        profileId = profileId,
        fromStart = fromStart,
        fromEnd = fromEnd,
        toStart = toStart,
        idFactory = { e -> "food-copy-${e.id}-${toStart.toEpochMilliseconds()}" }
    )

    fun exportJson(profileId: String): String = FoodImportExport.exportJson(
        profileId = profileId,
        entries = store.entriesForRange(profileId, Instant.fromEpochMilliseconds(0), clock.now()),
        reviews = store.reviews(profileId),
        exportedAtEpochMs = clock.now().toEpochMilliseconds()
    )

    fun exportCsv(profileId: String): String {
        val entries =
            store.entriesForRange(profileId, Instant.fromEpochMilliseconds(0), clock.now())
        return FoodImportExport.exportCsv(entries)
    }

    fun importJson(profileId: String, text: String): Int {
        val imported = FoodImportExport.importJson(profileId, text)
        imported.forEach(store::upsert)
        return imported.size
    }

    fun importCsv(profileId: String, text: String): Int {
        val imported = FoodImportExport.importCsv(profileId, text)
        imported.forEach(store::upsert)
        return imported.size
    }

    fun eraseAll(profileId: String) = FoodPrivacy.eraseAllFoodData(store, profileId)

    /** Swift-friendly overload (epoch millis; avoids Instant bridging). */
    fun morningReviewEpoch(
        profileId: String,
        dayKey: String,
        dayStartEpochMs: Long,
        dayEndEpochMs: Long,
        locale: String,
        ageYears: Int,
        sex: BiologicalSexBand,
        availability: OnDeviceAiAvailability
    ): MorningFoodReview = morningReview(
        profileId = profileId,
        dayKey = dayKey,
        dayStart = Instant.fromEpochMilliseconds(dayStartEpochMs),
        dayEnd = Instant.fromEpochMilliseconds(dayEndEpochMs),
        locale = locale,
        ageYears = ageYears,
        sex = sex,
        availability = availability
    )

    fun morningReview(
        profileId: String,
        dayKey: String,
        dayStart: Instant,
        dayEnd: Instant,
        locale: String,
        ageYears: Int,
        sex: BiologicalSexBand,
        availability: OnDeviceAiAvailability
    ): MorningFoodReview {
        val entries = entriesForDay(profileId, dayStart, dayEnd)
        val totals = FoodDiaryOps.dayTotals(entries)
        val dayTargets = targets.targetsForDay(profileId, dayKey)
        val gaps = micronutrientGaps(profileId, totals, ageYears, sex)
        val suppress =
            targets.isUnderEighteen(profileId) ||
                targets.isPregnancyMode(profileId) ||
                targets.isLowIntakeGuardActive(profileId, dayKey)
        val settings = store.settings(profileId)
        val review =
            reviewService.review(
                input =
                MorningReviewPromptInput(
                    locale = locale,
                    dayKey = dayKey,
                    foodNames = entries.map { it.name },
                    totals = totals,
                    targets = dayTargets,
                    gaps = gaps,
                    suppressDeficitLanguage = suppress
                ),
                availability = availability,
                aiEnabled = settings.morningAiReviewEnabled,
                id = "review-$dayKey-${clock.now().toEpochMilliseconds()}",
                profileId = profileId,
                createdAtEpochMs = clock.now().toEpochMilliseconds()
            )
        store.saveReview(review)
        return review
    }
}
