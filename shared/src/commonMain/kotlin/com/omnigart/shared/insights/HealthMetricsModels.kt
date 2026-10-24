package com.omnigart.shared.insights

import kotlinx.serialization.Serializable

/**
 * Platform-agnostic Health tab models. Native layers fill samples from
 * HealthKit / Health Connect (read-only) and call [HealthMetricsAssembler].
 */
@Serializable
data class HealthSamplePoint(val dayOffset: Int, val value: Double, val hourFraction: Double? = null)

@Serializable
data class HealthMetricCard(
    val id: String,
    val titleKey: String,
    val valueText: String?,
    val unitKey: String?,
    val sourceName: String?,
    val series: List<HealthSamplePoint> = emptyList(),
    val comparisonSeries: List<HealthSamplePoint> = emptyList(),
    val nowMarkerFraction: Double = 1.0,
    val empty: Boolean = false,
    val needsPermission: Boolean = false,
    val estimateLabelKey: String? = null
)

@Serializable
data class SleepDonutModel(
    val asleepMinutes: Double,
    val awakeMinutes: Double,
    val startLabel: String?,
    val endLabel: String?,
    val sourceName: String?,
    val needsPermission: Boolean = false
)

@Serializable
data class HealthMetricsDashboard(
    val todayActiveEnergy: HealthMetricCard,
    val todayRestingEnergy: HealthMetricCard,
    val todayExerciseMinutes: HealthMetricCard,
    val todaySteps: HealthMetricCard,
    val restingHr: HealthMetricCard,
    val hrv: HealthMetricCard,
    val hrvStress: HealthMetricCard,
    val recoveryHrv: HealthMetricCard,
    val bloodPressure: HealthMetricCard,
    val oxygenSaturation: HealthMetricCard,
    val respiratoryRate: HealthMetricCard,
    val wristTemperature: HealthMetricCard,
    val dietaryEnergy: HealthMetricCard,
    val dietaryMacros: HealthMetricCard,
    val sleep: SleepDonutModel,
    val mindfulMinutes: HealthMetricCard,
    val past7Days: List<HealthMetricCard>,
    val past30Days: List<HealthMetricCard>,
    val allTime: List<HealthMetricCard>
)

object HealthMetricsAssembler {
    fun emptyPermissionDashboard(): HealthMetricsDashboard {
        fun empty(id: String, titleKey: String) = HealthMetricCard(
            id = id,
            titleKey = titleKey,
            valueText = null,
            unitKey = null,
            sourceName = null,
            empty = true,
            needsPermission = true
        )
        return HealthMetricsDashboard(
            todayActiveEnergy = empty("active_energy", "health_active_energy"),
            todayRestingEnergy = empty("resting_energy", "health_resting_energy"),
            todayExerciseMinutes = empty("exercise_minutes", "health_exercise_minutes"),
            todaySteps = empty("steps", "health_steps"),
            restingHr = empty("resting_hr", "health_resting_hr"),
            hrv = empty("hrv", "health_hrv"),
            hrvStress =
            empty("hrv_stress", "health_hrv_stress").copy(
                estimateLabelKey = "health_hrv_stress_estimate"
            ),
            recoveryHrv = empty("recovery_hrv", "health_recovery_hrv"),
            bloodPressure = empty("blood_pressure", "health_blood_pressure"),
            oxygenSaturation = empty("spo2", "health_spo2"),
            respiratoryRate = empty("respiratory_rate", "health_respiratory_rate"),
            wristTemperature = empty("wrist_temperature", "health_wrist_temperature"),
            dietaryEnergy = empty("dietary_energy", "health_dietary_energy"),
            dietaryMacros = empty("dietary_macros", "health_dietary_macros"),
            sleep =
            SleepDonutModel(
                asleepMinutes = 0.0,
                awakeMinutes = 0.0,
                startLabel = null,
                endLabel = null,
                sourceName = null,
                needsPermission = true
            ),
            mindfulMinutes = empty("mindful", "health_mindful_minutes"),
            past7Days = emptyList(),
            past30Days = emptyList(),
            allTime = emptyList()
        )
    }

    fun cumulativeTodayVersusAverage(
        id: String,
        titleKey: String,
        unitKey: String,
        todayByHour: List<Double>,
        averageByHour: List<Double>,
        sourceName: String?,
        nowHourFraction: Double
    ): HealthMetricCard {
        if (todayByHour.all { it == 0.0 } && averageByHour.all { it == 0.0 }) {
            return HealthMetricCard(
                id = id,
                titleKey = titleKey,
                valueText = null,
                unitKey = unitKey,
                sourceName = sourceName,
                empty = true
            )
        }
        var t = 0.0
        var a = 0.0
        val current = mutableListOf<HealthSamplePoint>()
        val comparison = mutableListOf<HealthSamplePoint>()
        val n = maxOf(todayByHour.size, averageByHour.size, 1)
        for (i in 0 until n) {
            t += todayByHour.getOrElse(i) { 0.0 }
            a += averageByHour.getOrElse(i) { 0.0 }
            current += HealthSamplePoint(i, t, hourFraction = i / 24.0)
            comparison += HealthSamplePoint(i, a, hourFraction = i / 24.0)
        }
        return HealthMetricCard(
            id = id,
            titleKey = titleKey,
            valueText = t.toInt().toString(),
            unitKey = unitKey,
            sourceName = sourceName,
            series = current,
            comparisonSeries = comparison,
            nowMarkerFraction = (nowHourFraction / 24.0).coerceIn(0.0, 1.0)
        )
    }
}
