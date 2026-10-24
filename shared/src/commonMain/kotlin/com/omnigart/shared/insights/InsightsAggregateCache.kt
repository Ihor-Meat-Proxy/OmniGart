package com.omnigart.shared.insights

/**
 * In-process cache for Insights aggregates.
 *
 * Keyed by profile + workout revision fingerprint so background rebuilds skip
 * redundant work. Does not mutate HealthKit / Health Connect.
 */
class InsightsAggregateCache {
    private data class Entry(val fingerprint: String, val dashboard: InsightsDashboard)

    private var entry: Entry? = null

    fun get(fingerprint: String): InsightsDashboard? {
        val current = entry ?: return null
        return if (current.fingerprint == fingerprint) current.dashboard else null
    }

    fun put(fingerprint: String, dashboard: InsightsDashboard) {
        entry = Entry(fingerprint, dashboard)
    }

    fun clear() {
        entry = null
    }
}
