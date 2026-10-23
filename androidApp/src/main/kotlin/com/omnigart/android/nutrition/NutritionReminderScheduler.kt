package com.omnigart.android.nutrition

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.omnigart.android.R

/** Local-only nutrition reminder scheduling (no remote push). */
object NutritionReminderScheduler {
    private const val CHANNEL_ID = "nutrition_local"
    private const val ACTION = "com.omnigart.android.NUTRITION_REMINDER"

    fun reschedule(context: Context) {
        ensureChannel(context)
        val goal = runCatching {
            NutritionPrefsStore.init(context)
            NutritionPrefsStore.loadGoal()
        }.getOrNull() ?: return
        cancel(context, 1001)
        cancel(context, 1002)
        cancel(context, 1003)
        if (goal.remindersPaused) return
        if (goal.weighInNudgeEnabled) {
            scheduleAfterDays(context, requestCode = 1001, days = 3, kind = "weigh_in")
        }
        scheduleAfterDays(
            context,
            requestCode = 1002,
            days = goal.measurementReminderWeeks.coerceIn(2, 4) * 7,
            kind = "measurements",
        )
        scheduleAfterDays(context, requestCode = 1003, days = 7, kind = "checkin")
    }

    private fun scheduleAfterDays(context: Context, requestCode: Int, days: Int, kind: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent =
            Intent(context, NutritionReminderReceiver::class.java)
                .setAction(ACTION)
                .putExtra("kind", kind)
        val pi =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val triggerAt = System.currentTimeMillis() + days.toLong() * 86_400_000L
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    private fun cancel(context: Context, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NutritionReminderReceiver::class.java).setAction(ACTION)
        val pi =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        am.cancel(pi)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.nutrition_reminders_title),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun show(context: Context, kind: String) {
        ensureChannel(context)
        val (title, body) =
            when (kind) {
                "weigh_in" ->
                    R.string.nutrition_reminders_weigh_in to R.string.nutrition_reason_logging_break
                "measurements" ->
                    R.string.nutrition_reminders_measurements to R.string.nutrition_open_measurements
                else ->
                    R.string.nutrition_checkin_title to R.string.nutrition_target_disclaimer
            }
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(title))
                .setContentText(context.getString(body))
                .setAutoCancel(true)
                .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(kind.hashCode(), notification)
    }
}

class NutritionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val kind = intent?.getStringExtra("kind") ?: "checkin"
        NutritionReminderScheduler.show(context, kind)
    }
}
