package com.omnigart.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.calc.MassUnits
import com.omnigart.shared.calc.MuscleMassKindInfer
import com.omnigart.shared.calc.OneRepMax
import com.omnigart.shared.calc.ResistanceEnergyInput
import com.omnigart.shared.calc.ResistanceEnergySetInput
import com.omnigart.shared.calc.ResistanceWorkoutEnergy
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.UnitSystem
import com.omnigart.shared.domain.WorkoutSet
import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.SdkHealthConnectGateway
import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.logging.DEFAULT_REST_SECONDS
import com.omnigart.shared.logging.DemoStrengthExercise
import com.omnigart.shared.logging.InWorkoutLogging
import com.omnigart.shared.logging.ProgressionChip
import com.omnigart.shared.logging.StrengthSessionSummary
import com.omnigart.shared.util.formatOneDecimal
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StrengthLoggingSection(core: OmniGartCore, profileId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val demos = remember { core.strengthLogging.ensureDemoExercises() }
    val profile = core.profiles.get(profileId)
    val massUnit = MassUnits.fromUnitSystem(profile?.preferredUnits ?: UnitSystem.METRIC)
    var sessionId by remember { mutableStateOf<String?>(null) }
    var lastSessionId by remember { mutableStateOf<String?>(null) }
    var includeWarmups by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<StrengthSessionSummary?>(null) }
    var setCount by remember { mutableIntStateOf(0) }
    var volumeDisplay by remember { mutableStateOf(0.0) }
    var elapsedLabel by remember { mutableStateOf("0:00") }
    var restRemaining by remember { mutableIntStateOf(0) }
    var restRunning by remember { mutableStateOf(false) }
    var energyLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var draftWeight by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var draftReps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val refresh: (String) -> Unit = { id ->
        summary =
            core.strengthLogging.sessionSummary(
                sessionId = id,
                includeWarmups = includeWarmups,
                bodyweightKg = profile?.weightKg,
                unit = massUnit,
                profileId = profileId
            )
        val session = core.strengthLogging.getSession(id)
        setCount = session?.sets?.count { it.completed } ?: 0
        volumeDisplay = summary?.volume?.totalVolumeDisplay ?: 0.0
    }

    LaunchedEffect(sessionId, restRunning) {
        while (sessionId != null) {
            val id = sessionId ?: break
            val startMs = core.strengthLogging.sessionStartEpochMillis(id)
            val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).coerceAtLeast(0)
            elapsedLabel = formatElapsed(elapsedSec)
            if (restRunning) {
                if (restRemaining <= 0) {
                    restRunning = false
                } else {
                    restRemaining -= 1
                }
            }
            delay(1_000)
        }
    }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .testTag("strength_logging")
            .padding(top = OmniGartSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Text(
            text = stringResource(R.string.workout_intro),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        if (sessionId == null) {
            Text(
                text = stringResource(R.string.workout_idle),
                modifier = Modifier.testTag("workout_status")
            )
            Button(
                onClick = {
                    val started = core.strengthLogging.startSession()
                    sessionId = started.id
                    lastSessionId = started.id
                    summary = null
                    energyLines = emptyList()
                    demos.take(2).forEach { demo ->
                        core.strengthLogging.addSet(
                            sessionId = started.id,
                            exerciseId = demo.exercise.id,
                            reps = 8,
                            weightKg = 60.0,
                            rir = 2.0,
                            isWarmup = false,
                            loggedPerHand = demo.unilateral,
                            completed = false,
                            restSeconds = DEFAULT_REST_SECONDS,
                            targetWeightKg = 60.0,
                            targetReps = 8
                        )
                    }
                    refresh(started.id)
                },
                modifier = Modifier.testTag("workout_start")
            ) {
                Text(stringResource(R.string.workout_start))
            }
        } else {
            val id = sessionId!!
            val session = core.strengthLogging.getSession(id)
            LiveHeader(
                elapsedLabel = elapsedLabel,
                volumeDisplay = volumeDisplay,
                unit = massUnit,
                setCount = setCount,
                restRemaining = restRemaining,
                restRunning = restRunning,
                muscles = session?.let { activeMuscles(it, demos) }.orEmpty(),
                onRestToggle = {
                    if (restRunning) {
                        restRunning = false
                        restRemaining = 0
                    } else {
                        restRemaining = DEFAULT_REST_SECONDS
                        restRunning = true
                    }
                },
                onFinish = {
                    scope.launch {
                        finishAndSave(
                            context = context,
                            core = core,
                            profileId = profileId,
                            sessionId = id,
                            demos = demos,
                            includeWarmups = includeWarmups,
                            massUnit = massUnit,
                            onDone = { lines ->
                                energyLines = lines
                                refresh(id)
                                lastSessionId = id
                                sessionId = null
                                restRunning = false
                            }
                        )
                    }
                }
            )
            session?.let { s ->
                InWorkoutLogging.exercisesInOrder(s).forEach { exerciseId ->
                    val demo = demos.firstOrNull { it.exercise.id == exerciseId }
                    ExerciseCard(
                        session = s,
                        exerciseId = exerciseId,
                        displayName = demo?.displayName ?: exerciseId,
                        massUnit = massUnit,
                        draftWeight = draftWeight,
                        draftReps = draftReps,
                        onDraftWeight = { k, v -> draftWeight = draftWeight + (k to v) },
                        onDraftReps = { k, v -> draftReps = draftReps + (k to v) },
                        onComplete = { set ->
                            val wKey = "${set.id}-w"
                            val rKey = "${set.id}-r"
                            val display =
                                draftWeight[wKey]?.toDoubleOrNull()
                                    ?: MassUnits.kgToDisplay(set.weightKg ?: set.targetWeightKg ?: 0.0, massUnit)
                            val kg = MassUnits.displayToKg(display, massUnit)
                            val reps = draftReps[rKey]?.toIntOrNull() ?: set.reps ?: 0
                            core.strengthLogging.updateSet(
                                id,
                                set.copy(weightKg = kg, reps = reps, completed = true)
                            )
                            restRemaining = core.strengthLogging.completeSet(id, set.id)
                            restRunning = restRemaining > 0
                            refresh(id)
                        },
                        onUpdateSet = { set ->
                            core.strengthLogging.updateSet(id, set)
                            refresh(id)
                        },
                        onAddSet = {
                            val prev =
                                InWorkoutLogging.findPreviousSet(
                                    session = s,
                                    exerciseId = exerciseId,
                                    beforeSetIndex = s.sets.size
                                )
                            core.strengthLogging.addSet(
                                sessionId = id,
                                exerciseId = exerciseId,
                                reps = prev?.reps ?: 8,
                                weightKg = prev?.weightKg ?: 60.0,
                                loggedPerHand = demo?.unilateral == true,
                                completed = false,
                                restSeconds = DEFAULT_REST_SECONDS,
                                targetWeightKg = prev?.weightKg ?: 60.0,
                                targetReps = prev?.reps ?: 8
                            )
                            refresh(id)
                        },
                        onReplace = { toId ->
                            core.strengthLogging.replaceExercise(id, exerciseId, toId)
                            refresh(id)
                        },
                        onRemove = {
                            core.strengthLogging.removeExercise(id, exerciseId)
                            refresh(id)
                        },
                        replaceOptions = demos
                    )
                }
            }
        }

        Text(
            text =
            if (includeWarmups) {
                "● ${stringResource(R.string.workout_include_warmups)}"
            } else {
                "○ ${stringResource(R.string.workout_include_warmups)}"
            },
            modifier =
            Modifier
                .clickable {
                    includeWarmups = !includeWarmups
                    (sessionId ?: lastSessionId)?.let(refresh)
                }
                .testTag("workout_include_warmups")
        )

        summary?.let {
            SessionSummaryBlock(
                summary = it,
                unitLabel = if (massUnit == MassUnit.LB) "lb" else "kg",
                energyLines = energyLines,
                onExportPdf = { exportStrengthPdf(context, core, lastSessionId, summary) }
            )
        }
    }
}

@Composable
private fun LiveHeader(
    elapsedLabel: String,
    volumeDisplay: Double,
    unit: MassUnit,
    setCount: Int,
    restRemaining: Int,
    restRunning: Boolean,
    muscles: List<String>,
    onRestToggle: () -> Unit,
    onFinish: () -> Unit
) {
    val unitLabel = if (unit == MassUnit.LB) "lb" else "kg"
    Row(
        modifier = Modifier.fillMaxWidth().testTag("workout_status"),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HeaderStat(stringResource(R.string.workout_summary_duration), elapsedLabel)
        HeaderStat(
            stringResource(R.string.workout_volume_short),
            "${formatOneDecimal(volumeDisplay)} $unitLabel"
        )
        HeaderStat(stringResource(R.string.workout_sets_count), "$setCount")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Text(
            text =
            if (muscles.isEmpty()) {
                stringResource(R.string.workout_body_map)
            } else {
                muscles.take(3).joinToString(" · ")
            },
            style = MaterialTheme.typography.labelSmall,
            color = OmniGartColors.colorTextLightSecondary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRestToggle, modifier = Modifier.testTag("workout_rest_timer")) {
            Text(
                if (restRunning) {
                    "${stringResource(R.string.workout_rest_timer)} ${restRemaining}s"
                } else {
                    stringResource(R.string.workout_rest_timer)
                }
            )
        }
        Button(onClick = onFinish, modifier = Modifier.testTag("workout_end")) {
            Text(stringResource(R.string.workout_finish))
        }
    }
}

@Composable
private fun HeaderStat(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = OmniGartColors.colorTextLightSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ExerciseCard(
    session: Session,
    exerciseId: String,
    displayName: String,
    massUnit: MassUnit,
    draftWeight: Map<String, String>,
    draftReps: Map<String, String>,
    onDraftWeight: (String, String) -> Unit,
    onDraftReps: (String, String) -> Unit,
    onComplete: (WorkoutSet) -> Unit,
    onUpdateSet: (WorkoutSet) -> Unit,
    onAddSet: () -> Unit,
    onReplace: (String) -> Unit,
    onRemove: () -> Unit,
    replaceOptions: List<DemoStrengthExercise>
) {
    var menuOpen by remember { mutableStateOf(false) }
    val sets = InWorkoutLogging.setsForExercise(session, exerciseId)
    val chip =
        ProgressionChip.label(
            targetWeightKg = sets.firstOrNull()?.targetWeightKg,
            targetReps = sets.firstOrNull()?.targetReps,
            unit = massUnit
        )
    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space1)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.testTag("workout_exercise_$exerciseId")
            )
            if (chip != null) {
                Text(text = chip, style = MaterialTheme.typography.labelSmall, color = OmniGartColors.colorBrandAccent)
            }
            TextButton(onClick = { menuOpen = true }) {
                Text(stringResource(R.string.workout_set_menu))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                replaceOptions.filter { it.exercise.id != exerciseId }.forEach { demo ->
                    DropdownMenuItem(
                        text = { Text("${stringResource(R.string.workout_replace_exercise)}: ${demo.displayName}") },
                        onClick = {
                            menuOpen = false
                            onReplace(demo.exercise.id)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.workout_remove_exercise)) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.workout_col_set), modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.workout_col_previous), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            Text(
                if (massUnit == MassUnit.LB) stringResource(R.string.workout_col_lb) else stringResource(R.string.workout_col_kg),
                modifier = Modifier.width(56.dp),
                style = MaterialTheme.typography.labelSmall
            )
            Text(stringResource(R.string.workout_col_reps), modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.workout_col_done), modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
        }
        sets.forEach { set ->
            SetRow(
                session = session,
                exerciseId = exerciseId,
                set = set,
                massUnit = massUnit,
                draftWeight = draftWeight,
                draftReps = draftReps,
                onDraftWeight = onDraftWeight,
                onDraftReps = onDraftReps,
                onComplete = onComplete,
                onUpdateSet = onUpdateSet
            )
        }
        Button(onClick = onAddSet, modifier = Modifier.testTag("workout_add_set")) {
            Text(stringResource(R.string.workout_add_set))
        }
    }
}

@Composable
private fun SetRow(
    session: Session,
    exerciseId: String,
    set: WorkoutSet,
    massUnit: MassUnit,
    draftWeight: Map<String, String>,
    draftReps: Map<String, String>,
    onDraftWeight: (String, String) -> Unit,
    onDraftReps: (String, String) -> Unit,
    onComplete: (WorkoutSet) -> Unit,
    onUpdateSet: (WorkoutSet) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val previous =
        InWorkoutLogging.findPreviousSet(session, exerciseId, set.setIndex)
    val prevText = InWorkoutLogging.previousDisplay(previous, massUnit) ?: "—"
    val label = InWorkoutLogging.setLabel(InWorkoutLogging.setsForExercise(session, exerciseId), set.id)
    val wKey = "${set.id}-w"
    val rKey = "${set.id}-r"
    val weightText =
        draftWeight[wKey]
            ?: formatOneDecimal(
                MassUnits.kgToDisplay(
                    set.weightKg ?: set.targetWeightKg ?: previous?.weightKg ?: 0.0,
                    massUnit
                )
            )
    val repsText = draftReps[rKey] ?: (set.reps ?: set.targetReps ?: previous?.reps ?: 8).toString()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = prevText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        OutlinedTextField(
            value = weightText,
            onValueChange = { onDraftWeight(wKey, it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(64.dp).testTag("workout_weight"),
            singleLine = true
        )
        OutlinedTextField(
            value = repsText,
            onValueChange = { onDraftReps(rKey, it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(56.dp).testTag("workout_reps"),
            singleLine = true
        )
        Checkbox(
            checked = set.completed,
            onCheckedChange = { onComplete(set) },
            modifier = Modifier.testTag("workout_set_done_${set.id}")
        )
        TextButton(onClick = { menuOpen = true }) { Text("···") }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_mark_warmup)) },
                onClick = {
                    menuOpen = false
                    onUpdateSet(set.copy(isWarmup = !set.isWarmup))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_mark_drop)) },
                onClick = {
                    menuOpen = false
                    onUpdateSet(set.copy(isDropSet = !set.isDropSet))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_mark_failure)) },
                onClick = {
                    menuOpen = false
                    onUpdateSet(set.copy(isFailure = !set.isFailure))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_record_rpe)) },
                onClick = {
                    menuOpen = false
                    onUpdateSet(set.copy(rpe = 8.0))
                }
            )
        }
    }
}

@Composable
private fun SessionSummaryBlock(summary: StrengthSessionSummary, unitLabel: String, energyLines: List<String>, onExportPdf: (() -> Unit)?) {
    Text(
        text = stringResource(R.string.workout_summary_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.testTag("workout_summary")
    )
    energyLines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    Text(
        text =
        "${stringResource(R.string.workout_volume_load)}: " +
            "${formatOneDecimal(summary.volume.totalVolumeDisplay)} $unitLabel"
    )
    Text(
        text =
        "${stringResource(R.string.workout_total_weight_lifted)}: " +
            "${formatOneDecimal(summary.totalWeightLiftedDisplay)} $unitLabel"
    )
    Text(text = "${stringResource(R.string.workout_hard_sets)}: ${summary.volume.hardSets}")
    Text(text = stringResource(R.string.workout_per_exercise), style = MaterialTheme.typography.bodyMedium)
    summary.volume.perExercise.forEach { row ->
        Text(text = "${row.exerciseId}: ${formatOneDecimal(row.volumeKg)} $unitLabel · ${row.hardSets}")
    }
    if (onExportPdf != null) {
        Button(onClick = onExportPdf, modifier = Modifier.testTag("pdf_export_button")) {
            Text(text = stringResource(R.string.pdf_export_button))
        }
    }
}

private fun activeMuscles(session: Session, demos: List<DemoStrengthExercise>): List<String> {
    val keys =
        InWorkoutLogging.exercisesInOrder(session).flatMap { id ->
            demos.firstOrNull { it.exercise.id == id }?.exercise?.primaryMuscles.orEmpty()
        }
    return keys.distinct().sorted()
}

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

private suspend fun finishAndSave(
    context: android.content.Context,
    core: OmniGartCore,
    profileId: String,
    sessionId: String,
    demos: List<DemoStrengthExercise>,
    includeWarmups: Boolean,
    massUnit: MassUnit,
    onDone: (List<String>) -> Unit
) {
    core.strengthLogging.endSession(sessionId)
    val profile = core.profiles.get(profileId) ?: run {
        onDone(emptyList())
        return
    }
    val session = core.strengthLogging.getSession(sessionId) ?: run {
        onDone(emptyList())
        return
    }
    val startMs = core.strengthLogging.sessionStartEpochMillis(sessionId)
    val endMs = core.strengthLogging.sessionEndEpochMillis(sessionId)
    val durationSec = ((endMs - startMs) / 1000.0).coerceAtLeast(0.0)
    val age = core.ageYears(profile)
    val energySets =
        session.sets.map { set ->
            val muscles = demos.firstOrNull { it.exercise.id == set.exerciseId }?.exercise?.primaryMuscles.orEmpty()
            val kind = MuscleMassKindInfer.infer(muscles, compound = muscles.size >= 2)
            val load = set.weightKg ?: 0.0
            val reps = set.reps ?: 0
            ResistanceEnergySetInput(
                loadKg = load,
                reps = reps,
                estimated1RmKg = if (load > 0 && reps >= 1) OneRepMax.epley(load, reps) else null,
                rpe = set.rpe,
                rir = set.rir,
                isWarmup = set.isWarmup,
                isDropSet = set.isDropSet,
                isFailure = set.isFailure,
                completed = set.completed,
                muscleMassKind = kind
            )
        }
    val breakdown =
        ResistanceWorkoutEnergy.estimate(
            ResistanceEnergyInput(
                weightKg = profile.weightKg,
                heightCm = profile.heightCm,
                ageYears = age,
                sex = profile.biologicalSex,
                sessionDurationSeconds = durationSec,
                sets = energySets,
                restSecondsTotal = 0.0
            )
        )
    val sets = session.sets
    val syncId =
        core.localHealth.recordOmniGartWorkoutWithSets(
            localId = sessionId,
            activityTypeName = "STRENGTH",
            startEpochMs = startMs,
            endEpochMs = endMs,
            activeEnergyKcal = breakdown.activeKcal,
            basalEnergyKcal = breakdown.basalKcal,
            distanceMetres = null,
            averageHeartRateBpm = null,
            maxHeartRateBpm = null,
            sessionId = sessionId,
            planId = null,
            heartRateEpochMs = LongArray(0),
            heartRateBpm = DoubleArray(0),
            setIds = sets.map { it.id }.toTypedArray(),
            setExerciseKeys = sets.map { it.exerciseId }.toTypedArray(),
            setIndexes = sets.map { it.setIndex }.toIntArray(),
            setReps = sets.map { it.reps ?: -1 }.toIntArray(),
            setLoadKg = sets.map { it.weightKg ?: -1.0 }.toDoubleArray(),
            setIsWarmup = sets.map { it.isWarmup }.toBooleanArray(),
            setRir = sets.map { it.rir ?: -1.0 }.toDoubleArray(),
            setRpe = sets.map { it.rpe ?: -1.0 }.toDoubleArray(),
            setIsDrop = sets.map { it.isDropSet }.toBooleanArray(),
            setIsFailure = sets.map { it.isFailure }.toBooleanArray(),
            syncVersion = 1
        )
    runCatching {
        if (SdkHealthConnectGateway.isAvailable(context)) {
            val gateway = SdkHealthConnectGateway(context)
            val start = Instant.fromEpochMilliseconds(startMs)
            val end = Instant.fromEpochMilliseconds(endMs)
            if (!gateway.clientRecordIdExists(syncId, start, end)) {
                gateway.writeWorkouts(
                    listOf(
                        HealthWorkout(
                            id = sessionId,
                            activityType = WorkoutActivityType.STRENGTH,
                            start = start,
                            end = end,
                            energyKcal = breakdown.activeKcal,
                            basalEnergyKcal = breakdown.basalKcal,
                            clientRecordVersion = 1
                        )
                    )
                )
                core.localHealth.markPlatformLinked(sessionId, syncId)
            }
        }
    }
    val unitLabel = if (massUnit == MassUnit.LB) "lb" else "kg"
    val summary =
        core.strengthLogging.sessionSummary(
            sessionId = sessionId,
            includeWarmups = includeWarmups,
            bodyweightKg = profile.weightKg,
            unit = massUnit,
            profileId = profileId
        )
    onDone(
        listOf(
            "${context.getString(R.string.workout_summary_active_kcal)}: ${breakdown.activeKcal.toLong()}",
            "${context.getString(R.string.workout_summary_total_kcal)}: ${breakdown.totalKcal.toLong()}",
            context.getString(R.string.workout_summary_energy_estimate),
            "${context.getString(R.string.workout_volume_load)}: ${formatOneDecimal(summary.volume.totalVolumeDisplay)} $unitLabel"
        )
    )
}

private fun exportStrengthPdf(
    context: android.content.Context,
    core: OmniGartCore,
    lastSessionId: String?,
    summary: StrengthSessionSummary?
) {
    val id = lastSessionId ?: return
    val strengthJson =
        summary?.let { current ->
            val names =
                current.volume.perExercise.joinToString(",") {
                    "${it.exerciseId}=${it.exerciseId}"
                }
            val muscles =
                current.volume.perMuscleVolumeKg.entries.joinToString(",") {
                    "${it.key}=${it.value}"
                }
            "${current.totalWeightLiftedKg}|$names|$muscles"
        }
    OmnigartPdfShare.shareWorkout(
        context = context,
        core = core,
        workoutId = id,
        strengthJson = strengthJson
    )
}
