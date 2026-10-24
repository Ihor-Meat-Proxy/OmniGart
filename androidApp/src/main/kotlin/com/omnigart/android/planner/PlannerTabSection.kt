package com.omnigart.android.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.planning.AdaptiveSuggestion
import com.omnigart.planning.AdaptiveSuggestionAction
import com.omnigart.planning.PlannerCalendar
import com.omnigart.planning.PlannerIntensity
import com.omnigart.planning.PlannerSchemeSplit
import com.omnigart.planning.PlannerSessionKind
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.planner.GenerateSchemeOptions
import com.omnigart.shared.planner.PlannedWorkout

@Composable
fun PlannerTabSection(core: OmniGartCore, profileId: String) {
    val planner = core.planner
    var epoch by remember { mutableIntStateOf(0) }
    var selectedDateIso by remember { mutableStateOf(planner.todayIso()) }
    val calendar =
        remember(epoch, selectedDateIso, profileId) {
            planner.calendar(profileId, selectedDateIso)
        }
    val suggestions =
        remember(epoch, profileId) {
            planner.suggestions(profileId).suggestions
        }
    val profile = remember(profileId, epoch) { core.profiles.get(profileId) }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .testTag("planner_tab"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space3)
    ) {
        Text(
            text = stringResource(R.string.planner_intro),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        Text(
            text = stringResource(R.string.planner_wellness_note),
            style = MaterialTheme.typography.labelSmall,
            color = OmniGartColors.colorTextLightSecondary
        )

        Text(
            text = stringResource(R.string.planner_calendar_week),
            style = MaterialTheme.typography.titleSmall
        )
        WeekStrip(
            days = calendar.week,
            selectedDateIso = calendar.selectedDateIso,
            onSelect = {
                selectedDateIso = it
                epoch += 1
            }
        )

        Text(
            text = stringResource(R.string.planner_calendar_month),
            style = MaterialTheme.typography.titleSmall
        )
        MonthGrid(
            days = calendar.month,
            selectedDateIso = calendar.selectedDateIso,
            onSelect = {
                selectedDateIso = it
                epoch += 1
            }
        )

        Text(
            text = "${stringResource(R.string.planner_selected_day)}: ${calendar.selectedDateIso}",
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        if (calendar.completedCountForSelected > 0) {
            Text(
                text = stringResource(R.string.planner_completed_marker),
                style = MaterialTheme.typography.labelSmall,
                color = OmniGartColors.colorTextLightSecondary
            )
        }

        PlannedList(
            planned = calendar.plannedForSelected,
            onMoveTomorrow = { id, date ->
                val next = LocalDateIso.plusDays(date, 1)
                planner.movePlanned(id, next)
                epoch += 1
            },
            onDelete = {
                planner.deletePlanned(it)
                epoch += 1
            }
        )

        Text(
            text = stringResource(R.string.planner_add_session),
            style = MaterialTheme.typography.titleSmall
        )
        AddSessionRow { kind ->
            planner.addSession(
                profileId = profileId,
                dateIso = calendar.selectedDateIso,
                kind = kind,
                intensity =
                if (kind == PlannerSessionKind.ZONE2 || kind == PlannerSessionKind.REST) {
                    PlannerIntensity.EASY
                } else {
                    PlannerIntensity.MODERATE
                }
            )
            epoch += 1
        }

        Text(
            text = stringResource(R.string.planner_scheme_title),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(R.string.planner_scheme_replace_hint),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        Button(
            onClick = {
                val weekStart = planner.mondayOfWeekContaining(calendar.selectedDateIso)
                planner.generateAndPersistScheme(
                    profileId,
                    GenerateSchemeOptions(
                        weekStartIso = weekStart,
                        availableDaysPerWeek = 4,
                        primaryGoal = profile?.primaryGoal?.name ?: "GENERAL_FITNESS",
                        split = PlannerSchemeSplit.MIXED,
                        includeRuns = true,
                        includeTreadmill = false,
                        includeZone2 = true,
                        hasStrengthEquipment = true,
                        replaceWeek = true
                    )
                )
                epoch += 1
            },
            modifier = Modifier.testTag("planner_scheme_generate")
        ) {
            Text(stringResource(R.string.planner_scheme_generate))
        }

        if (suggestions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.planner_suggestions_title),
                style = MaterialTheme.typography.titleSmall
            )
            suggestions.forEach { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    onAccept = {
                        planner.acceptSuggestion(profileId, suggestion)
                        epoch += 1
                    },
                    onDismiss = {
                        planner.dismissSuggestion(suggestion)
                        epoch += 1
                    }
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(
    days: List<PlannerCalendar.DayCell>,
    selectedDateIso: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("planner_week_strip"),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { day ->
            DayCell(
                day = day,
                selected = day.dateIso == selectedDateIso,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(day.dateIso) }
            )
        }
    }
}

@Composable
private fun MonthGrid(
    days: List<PlannerCalendar.DayCell>,
    selectedDateIso: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("planner_month_grid"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        selected = day.dateIso == selectedDateIso,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(day.dateIso) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: PlannerCalendar.DayCell,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier =
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) OmniGartColors.colorBrandAccentMuted else OmniGartColors.colorSurfaceLightRaised)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag("planner_day_${day.dateIso}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            color =
            if (day.inMonth) {
                OmniGartColors.colorTextLightPrimary
            } else {
                OmniGartColors.colorTextLightSecondary.copy(alpha = 0.45f)
            },
            textAlign = TextAlign.Center
        )
        Spacer(Modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (day.hasPlanned) {
                Box(
                    modifier =
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(OmniGartColors.colorBrandAccent)
                )
            }
            if (day.hasCompleted) {
                Box(
                    modifier =
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(OmniGartColors.colorReadinessEasy)
                )
            }
        }
    }
}

@Composable
private fun PlannedList(
    planned: List<PlannedWorkout>,
    onMoveTomorrow: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (planned.isEmpty()) {
        Text(
            text = stringResource(R.string.planner_empty),
            color = OmniGartColors.colorTextLightSecondary
        )
        return
    }
    planned.forEach { workout ->
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, OmniGartColors.colorBrandAccentMuted, RoundedCornerShape(8.dp))
                .padding(OmniGartSpacing.space2)
                .testTag("planner_workout_${workout.id}"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = titleFor(workout), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = workout.kind.name,
                style = MaterialTheme.typography.labelSmall,
                color = OmniGartColors.colorTextLightSecondary
            )
            Row {
                TextButton(onClick = { onMoveTomorrow(workout.id, workout.scheduledDateIso) }) {
                    Text(stringResource(R.string.planner_move_tomorrow))
                }
                TextButton(onClick = { onDelete(workout.id) }) {
                    Text(stringResource(R.string.planner_delete))
                }
            }
        }
    }
}

@Composable
private fun AddSessionRow(onAdd: (PlannerSessionKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // PR #84 distinct strength labels.
        OutlinedButton(onClick = { onAdd(PlannerSessionKind.GUIDED_STRENGTH) }) {
            Text(stringResource(R.string.watch_strength_start))
        }
        OutlinedButton(onClick = { onAdd(PlannerSessionKind.FREE_STRENGTH) }) {
            Text(stringResource(R.string.wear_strength_start))
        }
        OutlinedButton(onClick = { onAdd(PlannerSessionKind.FUNCTIONAL) }) {
            Text(stringResource(R.string.planner_kind_functional))
        }
        OutlinedButton(onClick = { onAdd(PlannerSessionKind.ZONE2) }) {
            Text(stringResource(R.string.planner_kind_zone2))
        }
        OutlinedButton(onClick = { onAdd(PlannerSessionKind.RUN) }) {
            Text(stringResource(R.string.planner_kind_run))
        }
        OutlinedButton(onClick = { onAdd(PlannerSessionKind.TREADMILL) }) {
            Text(stringResource(R.string.planner_kind_treadmill))
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: AdaptiveSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(OmniGartColors.colorSurfaceLightRaised, RoundedCornerShape(8.dp))
            .padding(OmniGartSpacing.space2),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = reasonText(suggestion),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = actionText(suggestion),
            style = MaterialTheme.typography.labelSmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        Row {
            Button(
                onClick = onAccept,
                modifier = Modifier.testTag("planner_suggest_accept_${suggestion.id}")
            ) {
                Text(stringResource(R.string.planner_suggest_accept))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("planner_suggest_dismiss_${suggestion.id}")
            ) {
                Text(stringResource(R.string.planner_suggest_dismiss))
            }
        }
    }
}

@Composable
private fun titleFor(workout: PlannedWorkout): String {
    val resId =
        when (workout.titleKey) {
            "watch_strength_start" -> R.string.watch_strength_start
            "wear_strength_start" -> R.string.wear_strength_start
            "planner_kind_functional" -> R.string.planner_kind_functional
            "planner_kind_run" -> R.string.planner_kind_run
            "planner_kind_treadmill" -> R.string.planner_kind_treadmill
            "planner_kind_zone2" -> R.string.planner_kind_zone2
            "planner_kind_rest" -> R.string.planner_kind_rest
            else -> R.string.watch_strength_start
        }
    return stringResource(resId)
}

@Composable
private fun reasonText(suggestion: AdaptiveSuggestion): String {
    val resId =
        when (suggestion.reasonKey) {
            "planner_suggest_reason_readiness_rest" -> R.string.planner_suggest_reason_readiness_rest
            "planner_suggest_reason_readiness_low" -> R.string.planner_suggest_reason_readiness_low
            "planner_suggest_reason_load_elevated" -> R.string.planner_suggest_reason_load_elevated
            "planner_suggest_reason_recovery_signals" -> R.string.planner_suggest_reason_recovery_signals
            "planner_suggest_reason_muscle_recovery" -> R.string.planner_suggest_reason_muscle_recovery
            "planner_suggest_reason_back_to_back" -> R.string.planner_suggest_reason_back_to_back
            else -> R.string.planner_suggest_reason_recovery_signals
        }
    return stringResource(resId)
}

@Composable
private fun actionText(suggestion: AdaptiveSuggestion): String =
    when (suggestion.action) {
        AdaptiveSuggestionAction.MOVE -> {
            val base = stringResource(R.string.planner_suggest_action_move)
            val date = suggestion.proposedDateIso.orEmpty()
            if (date.isEmpty()) base else "$base ($date)"
        }
        AdaptiveSuggestionAction.SWAP -> stringResource(R.string.planner_suggest_action_swap)
        AdaptiveSuggestionAction.SKIP -> stringResource(R.string.planner_suggest_action_skip)
    }

private object LocalDateIso {
    fun plusDays(iso: String, days: Int): String {
        val parts = iso.split('-').map { it.toInt() }
        require(parts.size == 3)
        var y = parts[0]
        var m = parts[1]
        var d = parts[2] + days
        fun dim(year: Int, month: Int): Int =
            when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
                else -> 30
            }
        while (d > dim(y, m)) {
            d -= dim(y, m)
            m += 1
            if (m > 12) {
                m = 1
                y += 1
            }
        }
        while (d < 1) {
            m -= 1
            if (m < 1) {
                m = 12
                y -= 1
            }
            d += dim(y, m)
        }
        return "%04d-%02d-%02d".format(y, m, d)
    }
}
