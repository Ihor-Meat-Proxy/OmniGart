package com.omnigart.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.strength.EquipmentPresetKind
import com.omnigart.shared.strength.GeneratedExerciseSlot
import com.omnigart.shared.strength.GeneratedStrengthProgram
import com.omnigart.shared.strength.GeneratedStrengthSession
import com.omnigart.shared.strength.ProgramSplit
import com.omnigart.shared.strength.ReadyMadeWorkout
import com.omnigart.shared.strength.StrengthProgramPreferences
import com.omnigart.shared.strength.StrengthProgramService
import com.omnigart.shared.strength.UserEquipmentProfile
import java.util.Locale

private data class ProgramUiState(
    val epoch: Int = 0,
    val selectedDay: Int = 0,
    val showEquipment: Boolean = false,
    val sessionOverride: Set<String>? = null,
    val selectedReadyId: String? = null
)

@Composable
fun StrengthProgramSection(core: OmniGartCore, profileId: String) {
    val programs = core.strengthPrograms
    var ui by remember { mutableStateOf(ProgramUiState()) }
    val overrideList = ui.sessionOverride?.toList()
    val prefs = remember(ui.epoch) { programs.programPrefs(profileId) }
    val equipment = remember(ui.epoch) { programs.equipment(profileId) }
    val program = remember(ui.epoch, ui.sessionOverride) { programs.generateProgram(profileId, overrideList) }
    val readyWorkouts =
        remember(ui.epoch, ui.sessionOverride) { programs.readyMadeWorkouts(profileId, overrideList) }
    val readySession =
        remember(ui.epoch, ui.sessionOverride, ui.selectedReadyId) {
            ui.selectedReadyId?.let { programs.startReadyMade(profileId, it, overrideList) }
        }
    StrengthProgramContent(
        programs = programs,
        profileId = profileId,
        ui = ui,
        prefs = prefs,
        equipment = equipment,
        program = program,
        readyWorkouts = readyWorkouts,
        readySession = readySession,
        onUi = { ui = it }
    )
}

@Composable
private fun StrengthProgramContent(
    programs: StrengthProgramService,
    profileId: String,
    ui: ProgramUiState,
    prefs: StrengthProgramPreferences,
    equipment: UserEquipmentProfile,
    program: GeneratedStrengthProgram,
    readyWorkouts: List<ReadyMadeWorkout>,
    readySession: GeneratedStrengthSession?,
    onUi: (ProgramUiState) -> Unit
) {
    val session =
        program.sessions.getOrNull(ui.selectedDay.coerceIn(0, (program.sessions.size - 1).coerceAtLeast(0)))
    val toggleAdjust = {
        onUi(
            ui.copy(
                sessionOverride = if (ui.sessionOverride == null) equipment.ownedIds else null,
                epoch = ui.epoch + 1
            )
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth().testTag("strength_program_section"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        ReadyMadeBlock(
            workouts = readyWorkouts,
            selectedId = ui.selectedReadyId,
            session = readySession,
            onSelect = { id -> onUi(ui.copy(selectedReadyId = id)) },
            onClear = { onUi(ui.copy(selectedReadyId = null)) },
            onAdjust = toggleAdjust
        )
        ProgramPickerBlock(
            selectedSplit = prefs.split,
            daysPerWeek = prefs.daysPerWeek,
            allowedDays = programs.allowedDays(prefs.split),
            onSplit = { split ->
                programs.setSplit(profileId, split, prefs.daysPerWeek)
                onUi(ui.copy(selectedDay = 0, epoch = ui.epoch + 1))
            },
            onDays = { day ->
                programs.setSplit(profileId, prefs.split, day)
                onUi(ui.copy(selectedDay = 0, epoch = ui.epoch + 1))
            }
        )
        EquipmentSummaryBlock(
            ownedPreview = equipment.ownedIds.sorted().take(5).joinToString(", "),
            showEquipment = ui.showEquipment,
            onToggleEditor = { onUi(ui.copy(showEquipment = !ui.showEquipment)) },
            owned = equipment.ownedIds,
            catalogue = programs.catalogueIds(),
            onToggle = { id, enabled ->
                programs.toggleEquipment(profileId, id, enabled)
                onUi(ui.copy(epoch = ui.epoch + 1))
            },
            onPreset = { kind ->
                programs.applyPreset(profileId, kind)
                onUi(ui.copy(epoch = ui.epoch + 1))
            },
            onSavePreset = { kind ->
                programs.saveNamedPreset(profileId, presetLabel(kind), kind)
                onUi(ui.copy(epoch = ui.epoch + 1))
            }
        )
        WeekSessionBlock(
            sessions = program.sessions,
            selectedDay = ui.selectedDay,
            onSelectDay = { day -> onUi(ui.copy(selectedDay = day)) },
            session = session,
            sessionOverride = ui.sessionOverride,
            catalogue = programs.catalogueIds(),
            onAdjust = toggleAdjust,
            onOverrideToggle = { id, enabled ->
                val current = ui.sessionOverride.orEmpty()
                onUi(
                    ui.copy(
                        sessionOverride = if (enabled) current + id else current - id,
                        epoch = ui.epoch + 1
                    )
                )
            }
        )
    }
}

@Composable
private fun ReadyMadeBlock(
    workouts: List<ReadyMadeWorkout>,
    selectedId: String?,
    session: GeneratedStrengthSession?,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onAdjust: () -> Unit
) {
    Text(
        text = stringResource(R.string.workouts_ready_title),
        style = MaterialTheme.typography.titleMedium,
        color = OmniGartColors.colorBrandAccent
    )
    Text(
        text = stringResource(R.string.workouts_ready_intro),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    workouts.forEach { workout ->
        FilterChip(
            selected = selectedId == workout.id,
            onClick = { onSelect(workout.id) },
            label = {
                Text("${humanize(workout.id.removePrefix("ready-"))} · ${workout.sessionMinutes} min")
            },
            modifier = Modifier.testTag("workouts_ready_${workout.id}")
        )
    }
    session?.let { ready ->
        Text(
            text = ready.workedMuscles.joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary,
            modifier = Modifier.testTag("workouts_ready_muscles")
        )
        ready.slots.forEach { SlotRow(it) }
        TextButton(onClick = onAdjust, modifier = Modifier.testTag("workouts_ready_adjust")) {
            Text(stringResource(R.string.workouts_adjust_equipment))
        }
        TextButton(onClick = onClear, modifier = Modifier.testTag("workouts_ready_clear")) {
            Text(stringResource(R.string.workouts_ready_clear))
        }
    }
}

@Composable
private fun ProgramPickerBlock(
    selectedSplit: ProgramSplit,
    daysPerWeek: Int,
    allowedDays: List<Int>,
    onSplit: (ProgramSplit) -> Unit,
    onDays: (Int) -> Unit
) {
    Text(
        text = stringResource(R.string.workouts_program_title),
        style = MaterialTheme.typography.titleMedium,
        color = OmniGartColors.colorBrandAccent
    )
    Text(
        text = stringResource(R.string.workouts_program_intro),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    Text(text = stringResource(R.string.workouts_program_split), style = MaterialTheme.typography.labelLarge)
    ProgramSplit.entries.forEach { split ->
        FilterChip(
            selected = selectedSplit == split,
            onClick = { onSplit(split) },
            label = { Text(splitLabel(split)) },
            modifier = Modifier.testTag("program_split_${split.name}")
        )
    }
    Text(text = stringResource(R.string.workouts_program_days), style = MaterialTheme.typography.labelLarge)
    allowedDays.forEach { day ->
        FilterChip(
            selected = daysPerWeek == day,
            onClick = { onDays(day) },
            label = { Text("$day") },
            modifier = Modifier.testTag("program_days_$day")
        )
    }
}

@Composable
private fun EquipmentSummaryBlock(
    ownedPreview: String,
    showEquipment: Boolean,
    onToggleEditor: () -> Unit,
    owned: Set<String>,
    catalogue: List<String>,
    onToggle: (String, Boolean) -> Unit,
    onPreset: (EquipmentPresetKind) -> Unit,
    onSavePreset: (EquipmentPresetKind) -> Unit
) {
    OutlinedButton(onClick = onToggleEditor, modifier = Modifier.testTag("workouts_equipment_edit")) {
        Text(stringResource(R.string.workouts_equipment_edit))
    }
    Text(
        text = ownedPreview,
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    if (showEquipment) {
        EquipmentEditor(
            owned = owned,
            catalogue = catalogue,
            onToggle = onToggle,
            onPreset = onPreset,
            onSavePreset = onSavePreset
        )
    }
}

@Composable
private fun WeekSessionBlock(
    sessions: List<GeneratedStrengthSession>,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    session: GeneratedStrengthSession?,
    sessionOverride: Set<String>?,
    catalogue: List<String>,
    onAdjust: () -> Unit,
    onOverrideToggle: (String, Boolean) -> Unit
) {
    Text(text = stringResource(R.string.workouts_week_title), style = MaterialTheme.typography.titleSmall)
    sessions.forEachIndexed { index, generated ->
        FilterChip(
            selected = selectedDay == index,
            onClick = { onSelectDay(index) },
            label = { Text(sessionTitle(generated.titleKey)) },
            modifier = Modifier.testTag("workouts_week_day_$index")
        )
    }
    session ?: return
    Text(text = stringResource(R.string.workouts_muscles_title), style = MaterialTheme.typography.labelLarge)
    Text(
        text = session.workedMuscles.joinToString(", "),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary,
        modifier = Modifier.testTag("workouts_session_muscles")
    )
    session.slots.forEach { SlotRow(it) }
    TextButton(onClick = onAdjust, modifier = Modifier.testTag("workouts_adjust_equipment")) {
        Text(stringResource(R.string.workouts_adjust_equipment))
    }
    sessionOverride?.let { override ->
        EquipmentEditor(
            owned = override,
            catalogue = catalogue,
            onToggle = onOverrideToggle,
            onPreset = null,
            onSavePreset = null
        )
    }
}

@Composable
private fun EquipmentEditor(
    owned: Set<String>,
    catalogue: List<String>,
    onToggle: (String, Boolean) -> Unit,
    onPreset: ((EquipmentPresetKind) -> Unit)?,
    onSavePreset: ((EquipmentPresetKind) -> Unit)?
) {
    Column(
        modifier = Modifier.padding(vertical = OmniGartSpacing.space2).testTag("equipment_editor"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (onPreset != null) {
            Text(text = stringResource(R.string.workouts_equipment_presets), style = MaterialTheme.typography.labelLarge)
            Button(onClick = { onPreset(EquipmentPresetKind.HOME) }) {
                Text(stringResource(R.string.equipment_preset_home))
            }
            Button(onClick = { onPreset(EquipmentPresetKind.GYM) }) {
                Text(stringResource(R.string.equipment_preset_gym))
            }
            Button(onClick = { onPreset(EquipmentPresetKind.TRAVEL) }) {
                Text(stringResource(R.string.equipment_preset_travel))
            }
            onSavePreset?.let { save ->
                OutlinedButton(onClick = { save(EquipmentPresetKind.HOME) }) {
                    Text(stringResource(R.string.equipment_preset_save_home))
                }
            }
        }
        catalogue.forEach { id ->
            FilterChip(
                selected = id in owned,
                onClick = { onToggle(id, id !in owned) },
                label = { Text(equipmentLabel(id)) },
                modifier = Modifier.testTag("equipment_toggle_$id")
            )
        }
    }
}

@Composable
private fun SlotRow(slot: GeneratedExerciseSlot) {
    Column(modifier = Modifier.padding(vertical = 2.dp).testTag("workouts_slot_${slot.exerciseId}")) {
        Text(humanize(slot.exerciseId), style = MaterialTheme.typography.bodyMedium)
        val load =
            slot.suggestedLoadKg?.let { kg ->
                val rounded = String.format(Locale.UK, "%.1f kg", kg)
                if (slot.loadIncludesBodyMass) "~$rounded" else rounded
            }
        val sub = if (slot.substitutedFromId != null) stringResource(R.string.workouts_substituted) else null
        Text(
            text = listOfNotNull("${slot.sets}×${slot.repsLabel}", "RIR ${slot.rirLabel}", load, sub).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
    }
}

@Composable
private fun splitLabel(split: ProgramSplit): String = stringResource(
    when (split) {
        ProgramSplit.FULL_BODY -> R.string.program_split_full_body
        ProgramSplit.UPPER_LOWER -> R.string.program_split_upper_lower
        ProgramSplit.PUSH_PULL_LEGS -> R.string.program_split_ppl
        ProgramSplit.BRO_SPLIT -> R.string.program_split_bro
    }
)

private val sessionTitleRes =
    mapOf(
        "program_session_full_a" to R.string.program_session_full_a,
        "program_session_full_b" to R.string.program_session_full_b,
        "program_session_full_c" to R.string.program_session_full_c,
        "program_session_upper_a" to R.string.program_session_upper_a,
        "program_session_upper_b" to R.string.program_session_upper_b,
        "program_session_lower_a" to R.string.program_session_lower_a,
        "program_session_lower_b" to R.string.program_session_lower_b,
        "program_session_push" to R.string.program_session_push,
        "program_session_pull" to R.string.program_session_pull,
        "program_session_legs" to R.string.program_session_legs,
        "program_session_chest" to R.string.program_session_chest,
        "program_session_back" to R.string.program_session_back,
        "program_session_shoulders" to R.string.program_session_shoulders,
        "program_session_arms" to R.string.program_session_arms
    )

@Composable
private fun sessionTitle(key: String): String = stringResource(sessionTitleRes[key] ?: R.string.workouts_week_title)

private val equipmentLabelRes =
    mapOf(
        "pull_up_bar" to R.string.equipment_item_pull_up_bar,
        "dip_bars" to R.string.equipment_item_dip_bars,
        "bands" to R.string.equipment_item_bands,
        "dumbbell" to R.string.equipment_item_dumbbell,
        "kettlebell" to R.string.equipment_item_kettlebell,
        "barbell" to R.string.equipment_item_barbell,
        "plates" to R.string.equipment_item_plates,
        "bench_flat" to R.string.equipment_item_bench_flat,
        "bench_adjustable" to R.string.equipment_item_bench_adjustable,
        "squat_rack" to R.string.equipment_item_squat_rack,
        "cable" to R.string.equipment_item_cable,
        "smith_machine" to R.string.equipment_item_smith_machine,
        "leg_press" to R.string.equipment_item_leg_press,
        "lat_pulldown" to R.string.equipment_item_lat_pulldown,
        "assisted_pullup_dip_machine" to R.string.equipment_item_assisted_pullup_dip_machine,
        "machine" to R.string.equipment_item_machine,
        "trap_bar" to R.string.equipment_item_trap_bar,
        "ez_bar" to R.string.equipment_item_ez_bar,
        "suspension" to R.string.equipment_item_suspension,
        "box" to R.string.equipment_item_box,
        "landmine" to R.string.equipment_item_landmine,
        "yoga_mat" to R.string.equipment_item_yoga_mat
    )

@Composable
private fun equipmentLabel(id: String): String {
    val resId = equipmentLabelRes[id]
    return if (resId != null) stringResource(resId) else humanize(id)
}

private fun presetLabel(kind: EquipmentPresetKind): String = when (kind) {
    EquipmentPresetKind.HOME -> "Home"
    EquipmentPresetKind.GYM -> "Gym"
    EquipmentPresetKind.TRAVEL -> "Travel"
    EquipmentPresetKind.CUSTOM -> "Custom"
}

private fun humanize(id: String): String = id.split('-', '_').joinToString(" ") { part ->
    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}
