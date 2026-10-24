package com.omnigart.shared.food.diary

import com.omnigart.shared.food.models.CustomFood
import com.omnigart.shared.food.models.FoodLogEntry
import com.omnigart.shared.food.privacy.FoodAtRestEncryptor
import com.omnigart.shared.food.privacy.NoopFoodAtRestEncryptor
import com.omnigart.shared.food.review.MorningFoodReview
import com.omnigart.shared.food.settings.FoodSettings
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * File-/string-backed diary so food logging does not fight SQLDelight migrations
 * with parallel nutrition-model work. Platforms persist sealed blobs via
 * [FoodAtRestEncryptor] (AES-GCM / CryptoKit).
 */
class JsonFoodDiaryStore(
    private val load: () -> String?,
    private val save: (String) -> Unit,
    private val encryptor: FoodAtRestEncryptor = NoopFoodAtRestEncryptor
) : FoodDiaryStore {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private var state: Snapshot =
        load()
            ?.let { sealed ->
                runCatching {
                    json.decodeFromString<Snapshot>(encryptor.open(sealed))
                }.getOrNull()
            } ?: Snapshot()

    private fun persist() {
        save(encryptor.seal(json.encodeToString(state)))
    }

    override fun upsert(entry: FoodLogEntry) {
        state = state.copy(entries = state.entries.filterNot { it.id == entry.id } + entry)
        persist()
    }

    override fun delete(id: String) {
        state = state.copy(entries = state.entries.filterNot { it.id == id })
        persist()
    }

    override fun entry(id: String): FoodLogEntry? = state.entries.firstOrNull { it.id == id }

    override fun entriesForRange(profileId: String, startInclusive: Instant, endExclusive: Instant): List<FoodLogEntry> = state.entries
        .filter {
            it.profileId == profileId &&
                it.loggedAt >= startInclusive &&
                it.loggedAt < endExclusive
        }.sortedByDescending { it.loggedAt }

    override fun favourites(profileId: String): List<FoodLogEntry> = state.entries
        .filter { it.profileId == profileId && it.favourite }
        .distinctBy { it.name.lowercase() }
        .sortedBy { it.name }

    override fun recents(profileId: String, limit: Int): List<FoodLogEntry> = state.entries
        .filter { it.profileId == profileId }
        .sortedByDescending { it.loggedAt }
        .distinctBy { it.name.lowercase() }
        .take(limit)

    override fun upsertCustomFood(food: CustomFood) {
        state = state.copy(customs = state.customs.filterNot { it.id == food.id } + food)
        persist()
    }

    override fun customFoods(profileId: String): List<CustomFood> = state.customs.filter { it.profileId == profileId }.sortedBy { it.name }

    override fun deleteCustomFood(id: String) {
        state = state.copy(customs = state.customs.filterNot { it.id == id })
        persist()
    }

    override fun settings(profileId: String): FoodSettings =
        state.settings.firstOrNull { it.profileId == profileId } ?: FoodSettings(profileId = profileId)

    override fun saveSettings(settings: FoodSettings) {
        state =
            state.copy(
                settings = state.settings.filterNot { it.profileId == settings.profileId } + settings
            )
        persist()
    }

    override fun saveReview(review: MorningFoodReview) {
        state = state.copy(reviews = state.reviews.filterNot { it.id == review.id } + review)
        persist()
    }

    override fun reviews(profileId: String): List<MorningFoodReview> =
        state.reviews.filter { it.profileId == profileId }.sortedByDescending { it.createdAtEpochMs }

    override fun eraseAll(profileId: String) {
        state =
            Snapshot(
                entries = state.entries.filterNot { it.profileId == profileId },
                customs = state.customs.filterNot { it.profileId == profileId },
                settings = state.settings.filterNot { it.profileId == profileId },
                reviews = state.reviews.filterNot { it.profileId == profileId }
            )
        persist()
    }

    @Serializable
    data class Snapshot(
        val entries: List<FoodLogEntry> = emptyList(),
        val customs: List<CustomFood> = emptyList(),
        val settings: List<FoodSettings> = emptyList(),
        val reviews: List<MorningFoodReview> = emptyList()
    )
}
