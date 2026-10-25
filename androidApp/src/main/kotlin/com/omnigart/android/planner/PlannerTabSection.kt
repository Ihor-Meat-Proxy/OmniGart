package com.omnigart.android.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.planning.PlannerIntensity
import com.omnigart.planning.PlannerSchemeSplit
import com.omnigart.planning.PlannerSessionKind
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.planner.GenerateSchemeOptions
import com.omnigart.shared.planner.PlannerCalendarSnapshot

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
    val bump = { epoch += 1 }
    PlannerTabBody(
        calendar = calendar,
        suggestions = suggestions,
        onSelectDay = {
            selectedDateIso = it
            bump()
        },
        onMoveTomorrow = { id, date ->
            planner.movePlanned(id, LocalDateIso.plusDays(date, 1))
            bump()
        },
        onDelete = {
            planner.deletePlanned(it)
            bump()
        },
        onAdd = { kind ->
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
            bump()
        },
        onGenerateScheme = {
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
            bump()
        },
        onAcceptSuggestion = { suggestion ->
            planner.acceptSuggestion(profileId, suggestion)
            bump()
        },
        onDismissSuggestion = { suggestion ->
            planner.dismissSuggestion(suggestion)
            bump()
        }
    )
}

@Composable
private fun PlannerTabBody(
    calendar: PlannerCalendarSnapshot,
    suggestions: List<com.omnigart.planning.AdaptiveSuggestion>,
    onSelectDay: (String) -> Unit,
    onMoveTomorrow: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (PlannerSessionKind) -> Unit,
    onGenerateScheme: () -> Unit,
    onAcceptSuggestion: (com.omnigart.planning.AdaptiveSuggestion) -> Unit,
    onDismissSuggestion: (com.omnigart.planning.AdaptiveSuggestion) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("planner_tab"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space3)
    ) {
        PlannerIntro()
        PlannerCalendarBlock(calendar = calendar, onSelectDay = onSelectDay)
        PlannedList(
            planned = calendar.plannedForSelected,
            onMoveTomorrow = onMoveTomorrow,
            onDelete = onDelete
        )
        Text(text = stringResource(R.string.planner_add_session), style = MaterialTheme.typography.titleSmall)
        AddSessionRow(onAdd = onAdd)
        PlannerSchemeBlock(onGenerate = onGenerateScheme)
        PlannerSuggestionsBlock(
            suggestions = suggestions,
            onAccept = onAcceptSuggestion,
            onDismiss = onDismissSuggestion
        )
    }
}

@Composable
private fun PlannerIntro() {
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
}

@Composable
private fun PlannerCalendarBlock(calendar: PlannerCalendarSnapshot, onSelectDay: (String) -> Unit) {
    Text(text = stringResource(R.string.planner_calendar_week), style = MaterialTheme.typography.titleSmall)
    WeekStrip(days = calendar.week, selectedDateIso = calendar.selectedDateIso, onSelect = onSelectDay)
    Text(text = stringResource(R.string.planner_calendar_month), style = MaterialTheme.typography.titleSmall)
    MonthGrid(days = calendar.month, selectedDateIso = calendar.selectedDateIso, onSelect = onSelectDay)
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
}

@Composable
private fun PlannerSchemeBlock(onGenerate: () -> Unit) {
    Text(text = stringResource(R.string.planner_scheme_title), style = MaterialTheme.typography.titleSmall)
    Text(
        text = stringResource(R.string.planner_scheme_replace_hint),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    Button(onClick = onGenerate, modifier = Modifier.testTag("planner_scheme_generate")) {
        Text(stringResource(R.string.planner_scheme_generate))
    }
}

@Composable
private fun PlannerSuggestionsBlock(
    suggestions: List<com.omnigart.planning.AdaptiveSuggestion>,
    onAccept: (com.omnigart.planning.AdaptiveSuggestion) -> Unit,
    onDismiss: (com.omnigart.planning.AdaptiveSuggestion) -> Unit
) {
    if (suggestions.isEmpty()) return
    Text(text = stringResource(R.string.planner_suggestions_title), style = MaterialTheme.typography.titleSmall)
    suggestions.forEach { suggestion ->
        SuggestionCard(
            suggestion = suggestion,
            onAccept = { onAccept(suggestion) },
            onDismiss = { onDismiss(suggestion) }
        )
    }
}
