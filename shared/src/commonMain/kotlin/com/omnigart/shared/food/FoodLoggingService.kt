package com.omnigart.shared.food

import com.omnigart.shared.compliance.FeatureFlags
import com.omnigart.shared.food.catalogue.BundledUsdaCatalogue
import com.omnigart.shared.food.catalogue.CompositeFoodCatalogue
import com.omnigart.shared.food.catalogue.FoodCatalogue
import com.omnigart.shared.food.catalogue.OffOnlineSearchClient
import com.omnigart.shared.food.diary.FoodDiaryOps
import com.omnigart.shared.food.diary.FoodDiaryStore
import com.omnigart.shared.food.diary.InMemoryFoodDiaryStore
import com.omnigart.shared.food.health.DietaryNutritionSample
import com.omnigart.shared.food.health.FoodHealthSyncIds
import com.omnigart.shared.food.health.FoodSyncIdentity
import com.omnigart.shared.food.importing.FoodImportExport
import com.omnigart.shared.food.models.CatalogueFood
import com.omnigart.shared.food.models.CustomFood
import com.omnigart.shared.food.models.FoodLogDayTotals
import com.omnigart.shared.food.models.FoodLogEntry
import com.omnigart.shared.food.models.FoodSource
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.models.QuickMacroAdd
import com.omnigart.shared.food.models.QuickServing
import com.omnigart.shared.food.models.gramsFor
import com.omnigart.shared.food.nutrients.BestSources
import com.omnigart.shared.food.nutrients.BiologicalSexBand
import com.omnigart.shared.food.nutrients.FoodNutrientRank
import com.omnigart.shared.food.nutrients.NutrientGapAnalyzer
import com.omnigart.shared.food.nutrients.NutrientId
import com.omnigart.shared.food.nutrients.NutrientRankBasis
import com.omnigart.shared.food.nutrients.ReferenceIntakeResolver
import com.omnigart.shared.food.privacy.FoodPrivacy
import com.omnigart.shared.food.review.MorningFoodReview
import com.omnigart.shared.food.review.MorningFoodReviewService
import com.omnigart.shared.food.review.MorningReviewNotifier
import com.omnigart.shared.food.review.MorningReviewPromptInput
import com.omnigart.shared.food.review.NoopMorningReviewNotifier
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
    private val offOnline: OffOnlineSearchClient? = null,
    private val morningNotifier: MorningReviewNotifier = NoopMorningReviewNotifier,
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

    fun saveSettings(settings: FoodSettings) {
        store.saveSettings(settings)
        // Schedule when the user opted into morning review (AI or rule-based path).
        morningNotifier.scheduleDaily(
            profileId = settings.profileId,
            minutesAfterMidnight = settings.morningReviewMinutes,
            enabled = settings.morningAiReviewEnabled
        )
    }

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

    /** Swift-friendly (no Instant / default-arg bridging). */
    fun quickAddNow(profileId: String, add: QuickMacroAdd, mealGroup: MealGroup): FoodLogEntry =
        quickAdd(profileId, add, mealGroup, clock.now())

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

    /** Swift-friendly overload (epoch millis; avoids Instant bridging). */
    fun entriesForDayEpoch(profileId: String, dayStartEpochMs: Long, dayEndEpochMs: Long): List<FoodLogEntry> = entriesForDay(
        profileId,
        Instant.fromEpochMilliseconds(dayStartEpochMs),
        Instant.fromEpochMilliseconds(dayEndEpochMs)
    )

    fun totalsForDay(profileId: String, dayStart: Instant, dayEnd: Instant): FoodLogDayTotals =
        FoodDiaryOps.dayTotals(entriesForDay(profileId, dayStart, dayEnd))

    /** Swift-friendly overload (epoch millis; avoids Instant bridging). */
    fun totalsForDayEpoch(profileId: String, dayStartEpochMs: Long, dayEndEpochMs: Long): FoodLogDayTotals = totalsForDay(
        profileId,
        Instant.fromEpochMilliseconds(dayStartEpochMs),
        Instant.fromEpochMilliseconds(dayEndEpochMs)
    )

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

    /**
     * Human-readable "fill my gaps" lines for UI (Swift/Compose without Map bridging).
     * Empty string when no large gaps versus reference intakes.
     */
    fun fillGapsSummary(
        profileId: String,
        dayStartEpochMs: Long,
        dayEndEpochMs: Long,
        ageYears: Int,
        sex: BiologicalSexBand = BiologicalSexBand.UNSPECIFIED
    ): String {
        val totals = totalsForDayEpoch(profileId, dayStartEpochMs, dayEndEpochMs)
        val filled = fillGaps(profileId, totals, ageYears, sex)
        if (filled.isEmpty()) return ""
        return filled.entries.joinToString("\n") { (nutrient, ranks) ->
            val names = ranks.take(2).joinToString(", ") { it.food.name }
            "${nutrient.raw}: $names"
        }
    }

    fun copyDay(profileId: String, fromStart: Instant, fromEnd: Instant, toStart: Instant) = FoodDiaryOps.copyDay(
        store = store,
        profileId = profileId,
        fromStart = fromStart,
        fromEnd = fromEnd,
        toStart = toStart,
        idFactory = { e -> "food-copy-${e.id}-${toStart.toEpochMilliseconds()}" }
    )

    /**
     * Duplicate a day's diary entries using epoch millis; returns count.
     * From Swift prefer OmniGartCore.duplicateFoodDayMs (avoid Kotlin `copy*` ObjC names).
     */
    fun duplicateDayByEpochMs(profileId: String, fromStartEpochMs: Long, fromEndEpochMs: Long, toStartEpochMs: Long): Int {
        val copied =
            copyDay(
                profileId = profileId,
                fromStart = Instant.fromEpochMilliseconds(fromStartEpochMs),
                fromEnd = Instant.fromEpochMilliseconds(fromEndEpochMs),
                toStart = Instant.fromEpochMilliseconds(toStartEpochMs)
            )
        return copied.size
    }

    /** JVM/Android alias; prefer [duplicateDayByEpochMs] for new call sites. */
    fun copyDayByEpochMs(profileId: String, fromStartEpochMs: Long, fromEndEpochMs: Long, toStartEpochMs: Long): Int =
        duplicateDayByEpochMs(profileId, fromStartEpochMs, fromEndEpochMs, toStartEpochMs)

    /** JVM/Android alias for [duplicateDayByEpochMs]. */
    fun copyDayEpoch(profileId: String, fromStartEpochMs: Long, fromEndEpochMs: Long, toStartEpochMs: Long): Int =
        duplicateDayByEpochMs(profileId, fromStartEpochMs, fromEndEpochMs, toStartEpochMs)

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

    fun favourites(profileId: String): List<FoodLogEntry> = store.favourites(profileId)

    fun recents(profileId: String, limit: Int = 20): List<FoodLogEntry> = store.recents(profileId, limit)

    fun toggleFavourite(entryId: String): FoodLogEntry? {
        val e = store.entry(entryId) ?: return null
        val updated = e.copy(favourite = !e.favourite)
        store.upsert(updated)
        return updated
    }

    fun repeatRecent(
        profileId: String,
        entryId: String,
        mealGroup: MealGroup = MealGroup.SNACKS,
        at: Instant = clock.now()
    ): FoodLogEntry? {
        val e = store.entry(entryId) ?: return null
        return FoodDiaryOps.repeatItem(
            store = store,
            entry = e,
            at = at,
            newId = "food-repeat-${at.toEpochMilliseconds()}",
            mealGroup = mealGroup
        )
    }

    /** Swift-friendly (no Instant bridging). */
    fun repeatRecentNow(profileId: String, entryId: String, mealGroup: MealGroup): FoodLogEntry? =
        repeatRecent(profileId, entryId, mealGroup, clock.now())

    fun upsertCustomFood(food: CustomFood) = store.upsertCustomFood(food)

    /** Swift-friendly CustomFood constructor (Kotlin defaults are not exported). */
    fun upsertCustomFoodSimple(id: String, profileId: String, name: String, energyKcalPer100g: Double) {
        upsertCustomFood(
            CustomFood(
                id = id,
                profileId = profileId,
                name = name,
                energyKcalPer100g = energyKcalPer100g
            )
        )
    }

    fun customFoods(profileId: String): List<CustomFood> = store.customFoods(profileId)

    fun logQuickServing(
        profileId: String,
        foodId: String,
        serving: QuickServing,
        mealGroup: MealGroup,
        locale: String,
        packGrams: Double = 100.0,
        at: Instant = clock.now()
    ): FoodLogEntry? {
        val food = effectiveCatalogue(profileId).all().firstOrNull { it.id == foodId } ?: return null
        return logFromCatalogue(
            profileId = profileId,
            foodId = food.id,
            grams = food.gramsFor(serving, packGrams),
            mealGroup = mealGroup,
            locale = locale,
            at = at
        )
    }

    /** Swift-friendly (no Instant / default-arg bridging). */
    fun logQuickServingNow(profileId: String, foodId: String, serving: QuickServing, mealGroup: MealGroup, locale: String): FoodLogEntry? =
        logQuickServing(profileId, foodId, serving, mealGroup, locale, 100.0, clock.now())

    fun logFromCatalogueNow(profileId: String, foodId: String, grams: Double, mealGroup: MealGroup, locale: String): FoodLogEntry? =
        logFromCatalogue(profileId, foodId, grams, mealGroup, locale, clock.now())

    fun bestSources(
        profileId: String,
        nutrient: NutrientId,
        basis: NutrientRankBasis = NutrientRankBasis.PER_100_KCAL,
        categoryFilter: String? = null,
        limit: Int = 20
    ): List<FoodNutrientRank> = BestSources.rank(
        foods = effectiveCatalogue(profileId).all(),
        nutrient = nutrient,
        basis = basis,
        categoryFilter = categoryFilter,
        limit = limit
    )

    fun referenceIntakes(profileId: String, ageYears: Int, sex: BiologicalSexBand) = ReferenceIntakeResolver(
        overrides =
        store.settings(profileId).referenceOverrides.mapNotNull { (k, v) ->
            NutrientId.fromRaw(k)?.let { it to v }
        }.toMap()
    ).resolve(ageYears, sex)

    fun dismissReviewForProfile(profileId: String, reviewId: String): MorningFoodReview? {
        val review = store.reviews(profileId).firstOrNull { it.id == reviewId } ?: return null
        val updated = review.copy(dismissed = true)
        store.saveReview(updated)
        return updated
    }

    /**
     * Online OFF search (Search-a-licious → legacy). No-op when offline-only or client absent.
     */
    suspend fun searchOnline(profileId: String, query: String, locale: String, limit: Int = 25): List<CatalogueFood> {
        if (store.settings(profileId).offlineOnly) return emptyList()
        val client = offOnline ?: return emptyList()
        return client.search(query, locale, limit)
    }

    suspend fun findBarcodeOnline(profileId: String, barcode: String): CatalogueFood? {
        if (store.settings(profileId).offlineOnly) return null
        val local = findBarcode(profileId, barcode)
        if (local != null) return local
        return offOnline?.findByBarcode(barcode)
    }

    fun eraseAllAndCancelNotifications(profileId: String) {
        eraseAll(profileId)
        morningNotifier.cancel(profileId)
    }

    /** Swift/Compose-friendly settings update (avoids nullable Int bridging). */
    fun updateFoodPreferences(
        profileId: String,
        offlineOnly: Boolean,
        dayStartMinutes: Int,
        morningReviewMinutes: Int,
        morningAiReviewEnabled: Boolean = false
    ) {
        val current = store.settings(profileId)
        saveSettings(
            current.copy(
                offlineOnly = offlineOnly,
                dayStartMinutes = dayStartMinutes,
                morningReviewMinutes = morningReviewMinutes,
                morningAiReviewEnabled = morningAiReviewEnabled
            )
        )
        morningNotifier.scheduleDaily(
            profileId = profileId,
            minutesAfterMidnight = morningReviewMinutes,
            enabled =
            morningAiReviewEnabled &&
                FeatureFlags.morningFoodReviewEnabled &&
                FeatureFlags.notificationsEnabled
        )
    }

    /** Set a micronutrient reference-intake override (amount in nutrient units). */
    fun setReferenceOverrideAmount(profileId: String, nutrientRaw: String, amount: Double) {
        val current = store.settings(profileId)
        saveSettings(current.copy(referenceOverrides = current.referenceOverrides + (nutrientRaw to amount)))
    }

    fun clearReferenceOverride(profileId: String, nutrientRaw: String) {
        val current = store.settings(profileId)
        saveSettings(current.copy(referenceOverrides = current.referenceOverrides - nutrientRaw))
    }
}
