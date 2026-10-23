package com.omnigart.achievements

/**
 * Persistence boundary for achievement state. App wiring supplies SQLDelight / DataStore;
 * tests use [InMemoryAchievementStore].
 */
interface AchievementStore {
    fun hasProcessedEvent(eventId: String): Boolean
    fun markProcessed(eventId: String)

    fun getProgress(achievementId: String): AchievementProgress?
    fun putProgress(progress: AchievementProgress)

    fun allProgress(): Map<String, AchievementProgress>
}

class InMemoryAchievementStore : AchievementStore {
    private val processed = mutableSetOf<String>()
    private val progressById = mutableMapOf<String, AchievementProgress>()

    override fun hasProcessedEvent(eventId: String): Boolean = eventId in processed

    override fun markProcessed(eventId: String) {
        processed += eventId
    }

    override fun getProgress(achievementId: String): AchievementProgress? = progressById[achievementId]

    override fun putProgress(progress: AchievementProgress) {
        progressById[progress.achievementId] = progress
    }

    override fun allProgress(): Map<String, AchievementProgress> = progressById.toMap()
}
