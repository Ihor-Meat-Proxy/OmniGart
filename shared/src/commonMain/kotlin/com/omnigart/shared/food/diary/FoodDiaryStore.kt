package com.omnigart.shared.food.diary

import com.omnigart.shared.food.models.CustomFood
import com.omnigart.shared.food.models.FoodLogDayTotals
import com.omnigart.shared.food.models.FoodLogEntry
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.review.MorningFoodReview
import com.omnigart.shared.food.settings.FoodSettings
import kotlin.time.Instant

/**
 * Encrypted-at-rest diary persistence is planned with SQLCipher (Phase 5).
 * This store is the logical API; [InMemoryFoodDiaryStore] for tests and
 * [SqlDelightFoodDiaryStore] for the on-device DB.
 */
interface FoodDiaryStore {
    fun upsert(entry: FoodLogEntry)

    fun delete(id: String)

    fun entry(id: String): FoodLogEntry?

    fun entriesForRange(profileId: String, startInclusive: Instant, endExclusive: Instant): List<FoodLogEntry>

    fun favourites(profileId: String): List<FoodLogEntry>

    fun recents(profileId: String, limit: Int = 20): List<FoodLogEntry>

    fun upsertCustomFood(food: CustomFood)

    fun customFoods(profileId: String): List<CustomFood>

    fun deleteCustomFood(id: String)

    fun settings(profileId: String): FoodSettings

    fun saveSettings(settings: FoodSettings)

    fun saveReview(review: MorningFoodReview)

    fun reviews(profileId: String): List<MorningFoodReview>

    fun eraseAll(profileId: String)
}

class InMemoryFoodDiaryStore : FoodDiaryStore {
    private val entries = linkedMapOf<String, FoodLogEntry>()
    private val customs = linkedMapOf<String, CustomFood>()
    private val settingsByProfile = linkedMapOf<String, FoodSettings>()
    private val reviewsById = linkedMapOf<String, MorningFoodReview>()

    override fun upsert(entry: FoodLogEntry) {
        entries[entry.id] = entry
    }

    override fun delete(id: String) {
        entries.remove(id)
    }

    override fun entry(id: String): FoodLogEntry? = entries[id]

    override fun entriesForRange(profileId: String, startInclusive: Instant, endExclusive: Instant): List<FoodLogEntry> = entries.values
        .filter {
            it.profileId == profileId &&
                it.loggedAt >= startInclusive &&
                it.loggedAt < endExclusive
        }.sortedByDescending { it.loggedAt }

    override fun favourites(profileId: String): List<FoodLogEntry> = entries.values
        .filter { it.profileId == profileId && it.favourite }
        .distinctBy { it.name.lowercase() }
        .sortedBy { it.name }

    override fun recents(profileId: String, limit: Int): List<FoodLogEntry> = entries.values
        .filter { it.profileId == profileId }
        .sortedByDescending { it.loggedAt }
        .distinctBy { it.name.lowercase() }
        .take(limit)

    override fun upsertCustomFood(food: CustomFood) {
        customs[food.id] = food
    }

    override fun customFoods(profileId: String): List<CustomFood> = customs.values.filter { it.profileId == profileId }.sortedBy { it.name }

    override fun deleteCustomFood(id: String) {
        customs.remove(id)
    }

    override fun settings(profileId: String): FoodSettings = settingsByProfile[profileId] ?: FoodSettings(profileId = profileId)

    override fun saveSettings(settings: FoodSettings) {
        settingsByProfile[settings.profileId] = settings
    }

    override fun saveReview(review: MorningFoodReview) {
        reviewsById[review.id] = review
    }

    override fun reviews(profileId: String): List<MorningFoodReview> = reviewsById.values
        .filter { it.profileId == profileId }
        .sortedByDescending { it.createdAtEpochMs }

    override fun eraseAll(profileId: String) {
        entries.keys.filter { entries[it]?.profileId == profileId }.forEach { entries.remove(it) }
        customs.keys.filter { customs[it]?.profileId == profileId }.forEach { customs.remove(it) }
        settingsByProfile.remove(profileId)
        reviewsById.keys
            .filter { reviewsById[it]?.profileId == profileId }
            .forEach { reviewsById.remove(it) }
    }
}

object FoodDiaryOps {
    fun dayTotals(entries: List<FoodLogEntry>): FoodLogDayTotals = FoodLogDayTotals.from(entries)

    fun copyDay(
        store: FoodDiaryStore,
        profileId: String,
        fromStart: Instant,
        fromEnd: Instant,
        toStart: Instant,
        idFactory: (FoodLogEntry) -> String
    ): List<FoodLogEntry> {
        val source = store.entriesForRange(profileId, fromStart, fromEnd)
        val shiftMs = toStart.toEpochMilliseconds() - fromStart.toEpochMilliseconds()
        val copied =
            source.map { e ->
                val newAt =
                    Instant.fromEpochMilliseconds(e.loggedAt.toEpochMilliseconds() + shiftMs)
                e.copy(id = idFactory(e), loggedAt = newAt, favourite = false)
            }
        copied.forEach(store::upsert)
        return copied
    }

    fun repeatItem(
        store: FoodDiaryStore,
        entry: FoodLogEntry,
        at: Instant,
        newId: String,
        mealGroup: MealGroup = entry.mealGroup
    ): FoodLogEntry {
        val copy =
            entry.copy(
                id = newId,
                loggedAt = at,
                mealGroup = mealGroup,
                favourite = entry.favourite
            )
        store.upsert(copy)
        return copy
    }
}
