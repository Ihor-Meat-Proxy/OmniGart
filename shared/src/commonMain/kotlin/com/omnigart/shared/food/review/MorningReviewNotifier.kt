package com.omnigart.shared.food.review

/**
 * Schedules the optional local notification for morning food review.
 * Platforms implement with UserNotifications / Android NotificationManager.
 * No-op when notifications feature flag is off or permission denied.
 */
interface MorningReviewNotifier {
    /** [minutesAfterMidnight] local wall-clock time for the daily reminder. */
    fun scheduleDaily(profileId: String, minutesAfterMidnight: Int, enabled: Boolean)

    fun cancel(profileId: String)
}

object NoopMorningReviewNotifier : MorningReviewNotifier {
    override fun scheduleDaily(profileId: String, minutesAfterMidnight: Int, enabled: Boolean) = Unit

    override fun cancel(profileId: String) = Unit
}
