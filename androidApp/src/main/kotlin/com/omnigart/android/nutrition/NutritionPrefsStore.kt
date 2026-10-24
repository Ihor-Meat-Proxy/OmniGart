package com.omnigart.android.nutrition

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Simple SharedPreferences-backed local store for nutrition prefs and measurements. */
@Suppress("TooManyFunctions")
object NutritionPrefsStore {
    private const val PREFS = "nutrition_prefs_v1"
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class GoalPrefs(
        var kind: String = "MAINTAIN",
        var rateFractionPerWeek: Double = 0.0,
        var targetWeightKg: Double? = null,
        var mode: String = "GUIDED",
        var manualDailyKcal: Double? = null,
        var dayStartMinutes: Int = 4 * 60,
        var addExerciseCalories: Boolean = false,
        var weeklyCheckInWeekday: Int = 0,
        var remindersPaused: Boolean = false,
        var weighInNudgeEnabled: Boolean = true,
        var measurementReminderWeeks: Int = 3,
        var firstGoalAcknowledged: Boolean = false
    )

    data class WeightSample(val id: String, val epochMs: Long, val massKg: Double)

    data class GirthSample(val id: String, val epochMs: Long, val site: String, val valueCm: Double)

    data class BodyFatSample(val id: String, val epochMs: Long, val bodyFatPercent: Double)

    fun loadGoal(): GoalPrefs {
        val raw = prefs().getString("goal", null) ?: return GoalPrefs()
        val o = JSONObject(raw)
        return GoalPrefs(
            kind = o.optString("kind", "MAINTAIN"),
            rateFractionPerWeek = o.optDouble("rateFractionPerWeek", 0.0),
            targetWeightKg = o.optDouble("targetWeightKg").takeIf { o.has("targetWeightKg") && !o.isNull("targetWeightKg") },
            mode = o.optString("mode", "GUIDED"),
            manualDailyKcal = o.optDouble("manualDailyKcal").takeIf { o.has("manualDailyKcal") && !o.isNull("manualDailyKcal") },
            dayStartMinutes = o.optInt("dayStartMinutes", 4 * 60),
            addExerciseCalories = o.optBoolean("addExerciseCalories", false),
            weeklyCheckInWeekday = o.optInt("weeklyCheckInWeekday", 0),
            remindersPaused = o.optBoolean("remindersPaused", false),
            weighInNudgeEnabled = o.optBoolean("weighInNudgeEnabled", true),
            measurementReminderWeeks = o.optInt("measurementReminderWeeks", 3),
            firstGoalAcknowledged = o.optBoolean("firstGoalAcknowledged", false)
        )
    }

    fun saveGoal(goal: GoalPrefs) {
        val o =
            JSONObject()
                .put("kind", goal.kind)
                .put("rateFractionPerWeek", goal.rateFractionPerWeek)
                .put("mode", goal.mode)
                .put("dayStartMinutes", goal.dayStartMinutes)
                .put("addExerciseCalories", goal.addExerciseCalories)
                .put("weeklyCheckInWeekday", goal.weeklyCheckInWeekday)
                .put("remindersPaused", goal.remindersPaused)
                .put("weighInNudgeEnabled", goal.weighInNudgeEnabled)
                .put("measurementReminderWeeks", goal.measurementReminderWeeks)
                .put("firstGoalAcknowledged", goal.firstGoalAcknowledged)
        goal.targetWeightKg?.let { o.put("targetWeightKg", it) }
        goal.manualDailyKcal?.let { o.put("manualDailyKcal", it) }
        prefs().edit().putString("goal", o.toString()).apply()
    }

    fun loadWeights(): List<WeightSample> {
        val arr = JSONArray(prefs().getString("weights", "[]"))
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    WeightSample(
                        id = o.getString("id"),
                        epochMs = o.getLong("epochMs"),
                        massKg = o.getDouble("massKg")
                    )
                )
            }
        }
    }

    fun appendWeight(massKg: Double): WeightSample {
        val sample =
            WeightSample(
                id = java.util.UUID.randomUUID().toString(),
                epochMs = System.currentTimeMillis(),
                massKg = massKg
            )
        val arr = JSONArray(prefs().getString("weights", "[]"))
        arr.put(
            JSONObject()
                .put("id", sample.id)
                .put("epochMs", sample.epochMs)
                .put("massKg", sample.massKg)
        )
        prefs().edit().putString("weights", arr.toString()).apply()
        return sample
    }

    fun loadGirths(): List<GirthSample> {
        val arr = JSONArray(prefs().getString("girths", "[]"))
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    GirthSample(
                        id = o.getString("id"),
                        epochMs = o.getLong("epochMs"),
                        site = o.getString("site"),
                        valueCm = o.getDouble("valueCm")
                    )
                )
            }
        }
    }

    fun appendGirth(site: String, valueCm: Double) {
        val arr = JSONArray(prefs().getString("girths", "[]"))
        arr.put(
            JSONObject()
                .put("id", java.util.UUID.randomUUID().toString())
                .put("epochMs", System.currentTimeMillis())
                .put("site", site)
                .put("valueCm", valueCm)
        )
        prefs().edit().putString("girths", arr.toString()).apply()
    }

    fun loadBodyFat(): List<BodyFatSample> {
        val arr = JSONArray(prefs().getString("bodyFat", "[]"))
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    BodyFatSample(
                        id = o.getString("id"),
                        epochMs = o.getLong("epochMs"),
                        bodyFatPercent = o.getDouble("bodyFatPercent")
                    )
                )
            }
        }
    }

    fun appendBodyFat(percent: Double) {
        val arr = JSONArray(prefs().getString("bodyFat", "[]"))
        arr.put(
            JSONObject()
                .put("id", java.util.UUID.randomUUID().toString())
                .put("epochMs", System.currentTimeMillis())
                .put("bodyFatPercent", percent)
        )
        prefs().edit().putString("bodyFat", arr.toString()).apply()
    }

    fun lastTargetKcal(): Double? {
        val v = prefs().getFloat("lastTarget", -1f)
        return if (v > 0) v.toDouble() else null
    }

    fun saveLastTargetKcal(kcal: Double) {
        prefs().edit().putFloat("lastTarget", kcal.toFloat()).apply()
    }

    fun waterMl(dayKey: String): Double {
        val o = JSONObject(prefs().getString("waterByDay", "{}") ?: "{}")
        return o.optDouble(dayKey, 0.0)
    }

    fun addWaterMl(dayKey: String, ml: Double) {
        val o = JSONObject(prefs().getString("waterByDay", "{}") ?: "{}")
        o.put(dayKey, o.optDouble(dayKey, 0.0) + ml)
        prefs().edit().putString("waterByDay", o.toString()).apply()
    }
}
