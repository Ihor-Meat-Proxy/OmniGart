package com.omnigart.treadmill.hr

import kotlin.math.roundToInt

/**
 * Live heart-rate zone display and safety / program alerts for treadmill sessions.
 *
 * Wellness wording only — no medical claims. All alerts are user-toggleable.
 */
enum class TreadmillHrZoneModel {
    PERCENT_HR_MAX,
    KARVONEN
}

data class TreadmillHrZoneBand(
    val name: String,
    val lowerBpm: Double,
    val upperBpm: Double
)

data class TreadmillHrSettings(
    /** Editable max HR; callers may seed from 220−age or Tanaka. */
    val maxBpm: Int,
    val restingBpm: Int? = null,
    val zoneModel: TreadmillHrZoneModel = TreadmillHrZoneModel.PERCENT_HR_MAX,
    val alertsEnabled: Boolean = true,
    /** Fraction of max HR for the high safety alert (default ~95%). */
    val highLimitFraction: Double = 0.95,
    /** Sustained low HR during effort (bpm). */
    val lowEffortBpm: Int = 90,
    /** Seconds of missing samples before dropout alert. */
    val dropoutSeconds: Int = 12,
    val programZoneAlertsEnabled: Boolean = true,
    val safetyAlertsEnabled: Boolean = true
) {
    init {
        require(maxBpm in 100..230)
        require(highLimitFraction in 0.70..1.05)
        require(lowEffortBpm in 40..140)
        require(dropoutSeconds in 3..120)
    }

    fun zones(): List<TreadmillHrZoneBand> {
        val bounds = listOf(0.50, 0.60, 0.70, 0.80, 0.90, 1.00)
        val names = listOf("Z1", "Z2", "Z3", "Z4", "Z5")
        return when (zoneModel) {
            TreadmillHrZoneModel.PERCENT_HR_MAX ->
                names.mapIndexed { i, name ->
                    TreadmillHrZoneBand(
                        name = name,
                        lowerBpm = maxBpm * bounds[i],
                        upperBpm = maxBpm * bounds[i + 1]
                    )
                }
            TreadmillHrZoneModel.KARVONEN -> {
                val rest = restingBpm
                if (rest == null || rest !in 30 until maxBpm) {
                    return TreadmillHrSettings(
                        maxBpm = maxBpm,
                        zoneModel = TreadmillHrZoneModel.PERCENT_HR_MAX
                    ).zones()
                }
                val reserve = (maxBpm - rest).toDouble()
                names.mapIndexed { i, name ->
                    TreadmillHrZoneBand(
                        name = name,
                        lowerBpm = rest + bounds[i] * reserve,
                        upperBpm = rest + bounds[i + 1] * reserve
                    )
                }
            }
        }
    }

    fun highLimitBpm(): Int = (maxBpm * highLimitFraction).roundToInt()

    companion object {
        /** Fallback max HR: 220 − age (editable in settings). */
        fun foxMax(ageYears: Int): Int = (220 - ageYears).coerceIn(120, 210)

        fun default(ageYears: Int): TreadmillHrSettings =
            TreadmillHrSettings(maxBpm = foxMax(ageYears))
    }
}

enum class TreadmillHrAlertKind {
    ABOVE_TARGET_ZONE,
    BELOW_TARGET_ZONE,
    SAFETY_HIGH,
    SAFETY_LOW,
    SENSOR_DROPOUT
}

data class TreadmillHrAlert(
    val kind: TreadmillHrAlertKind,
    /** Wellness copy key — e.g. treadmill_hr_alert_high */
    val messageKey: String,
    val bpm: Int?
)

object TreadmillHrAlertEngine {
    fun currentZone(bpm: Int?, settings: TreadmillHrSettings): Int? {
        if (bpm == null || bpm <= 0) return null
        val zones = settings.zones()
        zones.forEachIndexed { index, zone ->
            if (bpm.toDouble() >= zone.lowerBpm && bpm.toDouble() < zone.upperBpm) {
                return index + 1
            }
        }
        if (bpm.toDouble() >= zones.last().upperBpm) return 5
        return 1
    }

    fun zoneBounds(zoneIndex: Int, settings: TreadmillHrSettings): TreadmillHrZoneBand? {
        val zones = settings.zones()
        if (zoneIndex !in 1..zones.size) return null
        return zones[zoneIndex - 1]
    }

    fun evaluate(
        bpm: Int?,
        settings: TreadmillHrSettings,
        targetZone: Int?,
        effortActive: Boolean,
        secondsSinceLastSample: Double?,
        highStreakSeconds: Double,
        lowStreakSeconds: Double
    ): List<TreadmillHrAlert> {
        if (!settings.alertsEnabled) return emptyList()
        val out = mutableListOf<TreadmillHrAlert>()

        if (settings.safetyAlertsEnabled) {
            if (secondsSinceLastSample != null && secondsSinceLastSample >= settings.dropoutSeconds) {
                out +=
                    TreadmillHrAlert(
                        kind = TreadmillHrAlertKind.SENSOR_DROPOUT,
                        messageKey = "treadmill_hr_alert_dropout",
                        bpm = bpm
                    )
            }
            if (bpm != null && highStreakSeconds >= 8.0 && bpm >= settings.highLimitBpm()) {
                out +=
                    TreadmillHrAlert(
                        kind = TreadmillHrAlertKind.SAFETY_HIGH,
                        messageKey = "treadmill_hr_alert_high",
                        bpm = bpm
                    )
            }
            if (
                bpm != null &&
                effortActive &&
                lowStreakSeconds >= 20.0 &&
                bpm <= settings.lowEffortBpm
            ) {
                out +=
                    TreadmillHrAlert(
                        kind = TreadmillHrAlertKind.SAFETY_LOW,
                        messageKey = "treadmill_hr_alert_low",
                        bpm = bpm
                    )
            }
        }

        if (settings.programZoneAlertsEnabled && bpm != null && targetZone != null) {
            val bounds = zoneBounds(targetZone, settings) ?: return out
            if (bpm.toDouble() > bounds.upperBpm) {
                out +=
                    TreadmillHrAlert(
                        kind = TreadmillHrAlertKind.ABOVE_TARGET_ZONE,
                        messageKey = "treadmill_hr_alert_above_zone",
                        bpm = bpm
                    )
            } else if (bpm.toDouble() < bounds.lowerBpm) {
                out +=
                    TreadmillHrAlert(
                        kind = TreadmillHrAlertKind.BELOW_TARGET_ZONE,
                        messageKey = "treadmill_hr_alert_below_zone",
                        bpm = bpm
                    )
            }
        }
        return out.distinctBy { it.kind }
    }
}
