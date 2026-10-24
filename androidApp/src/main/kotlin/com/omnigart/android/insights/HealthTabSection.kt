package com.omnigart.android.insights

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.insights.HealthMetricCard
import com.omnigart.shared.insights.HealthMetricsAssembler
import com.omnigart.shared.insights.HealthMetricsDashboard
import com.omnigart.shared.insights.HealthSamplePoint
import com.omnigart.shared.insights.HrvStressIndex
import com.omnigart.shared.insights.SleepDonutModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HealthTabSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dashboard by remember {
        mutableStateOf(HealthMetricsAssembler.emptyPermissionDashboard())
    }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        loading = true
        val result =
            withContext(Dispatchers.IO) {
                loadHealthDashboard(context)
            }
        dashboard = result.second
        loading = false
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) {
            scope.launch { reload() }
        }

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(InsightsChartPalette.canvas)
            .padding(vertical = OmniGartSpacing.space2)
            .testTag("health_tab"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space3)
    ) {
        Text(
            text = stringResource(R.string.health_metrics_intro),
            color = InsightsChartPalette.subtitle,
            style = MaterialTheme.typography.bodySmall
        )
        if (loading) {
            CircularProgressIndicator(color = InsightsChartPalette.current)
        }
        Section(stringResource(R.string.health_section_today))
        MetricCard(dashboard.todayActiveEnergy, chart = true) { request(permissionLauncher) }
        MetricCard(dashboard.todayRestingEnergy) { request(permissionLauncher) }
        MetricCard(dashboard.todayExerciseMinutes) { request(permissionLauncher) }
        MetricCard(dashboard.todaySteps) { request(permissionLauncher) }

        Section(stringResource(R.string.health_section_heart))
        MetricCard(dashboard.restingHr) { request(permissionLauncher) }
        MetricCard(dashboard.hrv) { request(permissionLauncher) }
        MetricCard(dashboard.hrvStress) { request(permissionLauncher) }
        MetricCard(dashboard.recoveryHrv) { request(permissionLauncher) }

        Section(stringResource(R.string.health_section_vitals))
        MetricCard(dashboard.bloodPressure) { request(permissionLauncher) }
        MetricCard(dashboard.oxygenSaturation) { request(permissionLauncher) }
        MetricCard(dashboard.respiratoryRate) { request(permissionLauncher) }
        MetricCard(dashboard.wristTemperature, chart = true) { request(permissionLauncher) }

        Section(stringResource(R.string.health_section_nutrition))
        MetricCard(dashboard.dietaryEnergy) { request(permissionLauncher) }
        MetricCard(dashboard.dietaryMacros) { request(permissionLauncher) }

        Section(stringResource(R.string.health_section_sleep))
        SleepCard(dashboard.sleep) { request(permissionLauncher) }

        Section(stringResource(R.string.health_section_mindful))
        MetricCard(dashboard.mindfulMinutes) { request(permissionLauncher) }

        Text(
            text = stringResource(R.string.health_wellness_note),
            color = InsightsChartPalette.subtitle,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun request(launcher: androidx.activity.result.ActivityResultLauncher<Set<String>>) {
    launcher.launch(HealthMetricsPermissions.readPermissions())
}

@Composable
private fun Section(title: String) {
    Text(title, color = InsightsChartPalette.title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun MetricCard(card: HealthMetricCard, chart: Boolean = false, onRequest: () -> Unit) {
    DarkInsightCard(title = titleFor(card.titleKey), sourceName = card.sourceName) {
        if (card.needsPermission || card.empty) {
            Text(
                text =
                stringResource(
                    if (card.needsPermission) R.string.health_permission_needed else R.string.health_no_data
                ),
                color = InsightsChartPalette.subtitle
            )
            if (card.needsPermission) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = InsightsChartPalette.current),
                    modifier = Modifier.testTag("health_request_access")
                ) {
                    Text(stringResource(R.string.health_request_access))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(card.valueText.orEmpty(), color = InsightsChartPalette.title, style = MaterialTheme.typography.titleLarge)
                card.unitKey?.let {
                    Text(unitFor(it), color = InsightsChartPalette.subtitle, style = MaterialTheme.typography.labelSmall)
                }
            }
            card.estimateLabelKey?.let {
                Text(
                    stringResource(R.string.health_hrv_stress_estimate),
                    color = InsightsChartPalette.current,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (chart && card.series.isNotEmpty()) {
                HealthLineChart(card.series, card.comparisonSeries, card.nowMarkerFraction)
            }
        }
    }
}

@Composable
private fun SleepCard(sleep: SleepDonutModel, onRequest: () -> Unit) {
    DarkInsightCard(title = stringResource(R.string.health_sleep), sourceName = sleep.sourceName) {
        if (sleep.needsPermission || (sleep.asleepMinutes == 0.0 && sleep.awakeMinutes == 0.0)) {
            Text(
                text =
                stringResource(
                    if (sleep.needsPermission) R.string.health_permission_needed else R.string.health_no_data
                ),
                color = InsightsChartPalette.subtitle
            )
            if (sleep.needsPermission) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = InsightsChartPalette.current)
                ) {
                    Text(stringResource(R.string.health_request_access))
                }
            }
        } else {
            val total = max(sleep.asleepMinutes + sleep.awakeMinutes, 1.0)
            Text(
                text = stringResource(R.string.health_asleep) + " · %.0f%%".format(sleep.asleepMinutes / total * 100),
                color = InsightsChartPalette.title
            )
            sleep.startLabel?.let {
                Text("${stringResource(R.string.health_sleep_start)} $it", color = InsightsChartPalette.subtitle)
            }
            sleep.endLabel?.let {
                Text("${stringResource(R.string.health_sleep_end)} $it", color = InsightsChartPalette.subtitle)
            }
        }
    }
}

@Composable
private fun HealthLineChart(current: List<HealthSamplePoint>, comparison: List<HealthSamplePoint>, nowFraction: Double) {
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val pts = current.ifEmpty { comparison }
        if (pts.isEmpty()) return@Canvas
        val maxX = max(pts.maxOf { it.dayOffset }.toFloat(), 1f)
        val values = current.map { it.value } + comparison.map { it.value }
        val minY = (values.minOrNull() ?: 0.0).toFloat()
        val maxY = max((values.maxOrNull() ?: 1.0).toFloat(), minY + 1f)
        fun x(day: Int) = (day / maxX) * size.width
        fun y(v: Double) = size.height - ((v.toFloat() - minY) / (maxY - minY)) * size.height
        fun strokeSeries(points: List<HealthSamplePoint>, color: androidx.compose.ui.graphics.Color, dashed: Boolean) {
            if (points.size < 2) return
            val path = Path()
            points.forEachIndexed { i, p ->
                val pt = Offset(x(p.dayOffset), y(p.value))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(
                path,
                color,
                style =
                Stroke(
                    width = if (dashed) 2.5f else 4f,
                    cap = StrokeCap.Round,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
                )
            )
        }
        strokeSeries(comparison, InsightsChartPalette.comparison, true)
        strokeSeries(current, InsightsChartPalette.current, false)
        val nowX = nowFraction.coerceIn(0.0, 1.0).toFloat() * size.width
        drawLine(
            color = InsightsChartPalette.now,
            start = Offset(nowX, 0f),
            end = Offset(nowX, size.height),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun titleFor(key: String): String = when (key) {
    "health_active_energy" -> stringResource(R.string.health_active_energy)
    "health_resting_energy" -> stringResource(R.string.health_resting_energy)
    "health_exercise_minutes" -> stringResource(R.string.health_exercise_minutes)
    "health_steps" -> stringResource(R.string.health_steps)
    "health_resting_hr" -> stringResource(R.string.health_resting_hr)
    "health_hrv" -> stringResource(R.string.health_hrv)
    "health_hrv_stress" -> stringResource(R.string.health_hrv_stress)
    "health_recovery_hrv" -> stringResource(R.string.health_recovery_hrv)
    "health_blood_pressure" -> stringResource(R.string.health_blood_pressure)
    "health_spo2" -> stringResource(R.string.health_spo2)
    "health_respiratory_rate" -> stringResource(R.string.health_respiratory_rate)
    "health_wrist_temperature" -> stringResource(R.string.health_wrist_temperature)
    "health_dietary_energy" -> stringResource(R.string.health_dietary_energy)
    "health_dietary_macros" -> stringResource(R.string.health_dietary_macros)
    "health_mindful_minutes" -> stringResource(R.string.health_mindful_minutes)
    else -> key
}

@Composable
private fun unitFor(key: String): String = when (key) {
    "health_unit_kcal" -> stringResource(R.string.health_unit_kcal)
    "health_unit_min" -> stringResource(R.string.health_unit_min)
    "health_unit_steps" -> stringResource(R.string.health_unit_steps)
    "health_unit_bpm" -> stringResource(R.string.health_unit_bpm)
    "health_unit_ms" -> stringResource(R.string.health_unit_ms)
    "health_unit_percent" -> stringResource(R.string.health_unit_percent)
    "health_unit_brpm" -> stringResource(R.string.health_unit_brpm)
    "health_unit_celsius" -> stringResource(R.string.health_unit_celsius)
    "health_unit_mmhg" -> stringResource(R.string.health_unit_mmhg)
    "health_unit_g" -> stringResource(R.string.health_unit_g)
    else -> key
}

@Suppress("LongMethod")
private suspend fun loadHealthDashboard(context: android.content.Context): Pair<Boolean, HealthMetricsDashboard> {
    val status = HealthConnectClient.getSdkStatus(context)
    if (status != HealthConnectClient.SDK_AVAILABLE) {
        return false to HealthMetricsAssembler.emptyPermissionDashboard()
    }
    val client = HealthConnectClient.getOrCreate(context)
    val granted = client.permissionController.getGrantedPermissions()
    val needed = HealthMetricsPermissions.readPermissions()
    if (granted.intersect(needed).isEmpty()) {
        return false to HealthMetricsAssembler.emptyPermissionDashboard()
    }

    val zone = ZoneId.systemDefault()
    val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
    val end = Instant.now()
    val range = TimeRangeFilter.between(start, end)

    val steps =
        runCatching {
            client.readRecords(ReadRecordsRequest(StepsRecord::class, range)).records.sumOf { it.count }
        }.getOrNull()
    val active =
        runCatching {
            client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, range)).records
                .sumOf { it.energy.inKilocalories }
        }.getOrNull()
    val rhr =
        runCatching {
            client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, range)).records
                .lastOrNull()?.beatsPerMinute?.toDouble()
        }.getOrNull()
    val hrv =
        runCatching {
            client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, range)).records
                .lastOrNull()?.heartRateVariabilityMillis
        }.getOrNull()
    val sleepSessions =
        runCatching {
            client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range)).records
        }.getOrDefault(emptyList())

    val stress = HrvStressIndex.fromHrvMagnitude(hrv)
    val nowHour = LocalTime.now(zone).hour + LocalTime.now(zone).minute / 60.0
    val activeByHour = List(24) { if (it <= nowHour) (active ?: 0.0) / max(nowHour, 1.0) else 0.0 }
    val avgByHour = List(24) { (active ?: 0.0) / 24.0 / 14.0 }

    fun card(id: String, title: String, value: Double?, unit: String, source: String? = "Health Connect") = HealthMetricCard(
        id = id,
        titleKey = title,
        valueText = value?.let { "%.0f".format(it) },
        unitKey = unit,
        sourceName = source,
        empty = value == null
    )

    val sleep =
        if (sleepSessions.isEmpty()) {
            SleepDonutModel(0.0, 0.0, null, null, null, needsPermission = false)
        } else {
            val session = sleepSessions.maxByOrNull { it.endTime }!!
            val minutes = java.time.Duration.between(session.startTime, session.endTime).toMinutes().toDouble()
            SleepDonutModel(
                asleepMinutes = minutes * 0.9,
                awakeMinutes = minutes * 0.1,
                startLabel = ZonedDateTime.ofInstant(session.startTime, zone).toLocalTime().toString(),
                endLabel = ZonedDateTime.ofInstant(session.endTime, zone).toLocalTime().toString(),
                sourceName = session.metadata.dataOrigin.packageName,
                needsPermission = false
            )
        }

    val base = HealthMetricsAssembler.emptyPermissionDashboard()
    return true to
        base.copy(
            todayActiveEnergy =
            HealthMetricsAssembler.cumulativeTodayVersusAverage(
                id = "active_energy",
                titleKey = "health_active_energy",
                unitKey = "health_unit_kcal",
                todayByHour = activeByHour,
                averageByHour = avgByHour,
                sourceName = "Health Connect",
                nowHourFraction = nowHour
            ),
            todayRestingEnergy = card(
                "resting_energy",
                "health_resting_energy",
                null,
                "health_unit_kcal"
            ).copy(needsPermission = false, empty = true),
            todayExerciseMinutes = card(
                "exercise_minutes",
                "health_exercise_minutes",
                null,
                "health_unit_min"
            ).copy(needsPermission = false, empty = true),
            todaySteps = card("steps", "health_steps", steps?.toDouble(), "health_unit_steps"),
            restingHr = card("resting_hr", "health_resting_hr", rhr, "health_unit_bpm"),
            hrv = card("hrv", "health_hrv", hrv, "health_unit_ms"),
            hrvStress =
            HealthMetricCard(
                id = "hrv_stress",
                titleKey = "health_hrv_stress",
                valueText = stress.index?.let { "%.1f".format(it) },
                unitKey = null,
                sourceName = "Health Connect",
                empty = stress.index == null,
                estimateLabelKey = "health_hrv_stress_estimate"
            ),
            recoveryHrv = card("recovery_hrv", "health_recovery_hrv", hrv, "health_unit_ms"),
            bloodPressure = card(
                "blood_pressure",
                "health_blood_pressure",
                null,
                "health_unit_mmhg"
            ).copy(needsPermission = false, empty = true),
            oxygenSaturation = card("spo2", "health_spo2", null, "health_unit_percent").copy(needsPermission = false, empty = true),
            respiratoryRate = card(
                "respiratory_rate",
                "health_respiratory_rate",
                null,
                "health_unit_brpm"
            ).copy(needsPermission = false, empty = true),
            wristTemperature = card(
                "wrist_temperature",
                "health_wrist_temperature",
                null,
                "health_unit_celsius"
            ).copy(needsPermission = false, empty = true),
            dietaryEnergy = card(
                "dietary_energy",
                "health_dietary_energy",
                null,
                "health_unit_kcal"
            ).copy(needsPermission = false, empty = true),
            dietaryMacros = card(
                "dietary_macros",
                "health_dietary_macros",
                null,
                "health_unit_g"
            ).copy(needsPermission = false, empty = true),
            sleep = sleep,
            mindfulMinutes = card("mindful", "health_mindful_minutes", null, "health_unit_min").copy(needsPermission = false, empty = true),
            past7Days = emptyList(),
            past30Days = emptyList(),
            allTime = emptyList()
        )
}
