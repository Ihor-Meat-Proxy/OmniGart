package com.omnigart.android.food

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.omnigart.android.MainActivity
import com.omnigart.shared.food.review.MorningReviewNotifier
import java.util.Calendar

/**
 * Schedules (or cancels) the optional local morning food-review reminder.
 * Uses AlarmManager; UI launches the Food tab when the alarm fires.
 */
class FoodMorningReviewNotifier(private val context: Context) : MorningReviewNotifier {
    override fun scheduleDaily(profileId: String, minutesAfterMidnight: Int, enabled: Boolean) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(profileId)
        am.cancel(pending)
        if (!enabled) return
        val cal =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60)
                set(Calendar.MINUTE, minutesAfterMidnight % 60)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }

    override fun cancel(profileId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(profileId))
    }

    private fun pendingIntent(profileId: String): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
                putExtra(EXTRA_OPEN_FOOD_REVIEW, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            profileId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_PROFILE_ID = "omnigart.food.morning.profile"
        const val EXTRA_OPEN_FOOD_REVIEW = "omnigart.food.morning.open"
    }
}
