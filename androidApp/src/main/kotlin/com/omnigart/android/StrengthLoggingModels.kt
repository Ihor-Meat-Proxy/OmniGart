package com.omnigart.android

import android.content.Context
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.WorkoutSet
import com.omnigart.shared.logging.DemoStrengthExercise

internal data class SetDrafts(
    val weight: Map<String, String>,
    val reps: Map<String, String>,
    val onWeight: (String, String) -> Unit,
    val onReps: (String, String) -> Unit
)

internal data class ExerciseCardModel(
    val session: Session,
    val exerciseId: String,
    val displayName: String,
    val massUnit: MassUnit,
    val replaceOptions: List<DemoStrengthExercise>
)

internal data class ExerciseCardActions(
    val onComplete: (WorkoutSet) -> Unit,
    val onUpdateSet: (WorkoutSet) -> Unit,
    val onAddSet: () -> Unit,
    val onReplace: (String) -> Unit,
    val onRemove: () -> Unit
)

internal data class SetRowModel(val session: Session, val exerciseId: String, val set: WorkoutSet, val massUnit: MassUnit)

internal data class LiveHeaderModel(
    val elapsedLabel: String,
    val volumeDisplay: Double,
    val unit: MassUnit,
    val setCount: Int,
    val restRemaining: Int,
    val restRunning: Boolean,
    val muscles: List<String>
)

internal data class StrengthSaveRequest(
    val context: Context,
    val core: OmniGartCore,
    val profileId: String,
    val sessionId: String,
    val demos: List<DemoStrengthExercise>,
    val includeWarmups: Boolean,
    val massUnit: MassUnit
)
