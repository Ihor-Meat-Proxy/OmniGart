package com.omnigart.bodymap

import com.omnigart.content.Muscle

/**
 * Chooses front / back / both views for an exercise highlight from the static
 * taxonomy→region view hints (mirrored in code so commonMain stays dependency-light).
 *
 * Keep in sync with `design/body-map/mapping/taxonomy-to-regions.json`.
 */
object ViewChooser {

    private val frontPreferred = setOf(
        Muscle.PECTORALIS_MAJOR,
        Muscle.FRONT_DELTOID,
        Muscle.BICEPS,
        Muscle.RECTUS_ABDOMINIS,
        Muscle.OBLIQUES,
        Muscle.TRANSVERSE_ABDOMINIS,
        Muscle.SERRATUS_ANTERIOR,
        Muscle.ADDUCTORS,
        Muscle.HIP_FLEXORS,
        Muscle.QUADRICEPS,
        Muscle.TIBIALIS_ANTERIOR,
    )

    private val backPreferred = setOf(
        Muscle.REAR_DELTOID,
        Muscle.TRICEPS,
        Muscle.TRAPEZIUS,
        Muscle.RHOMBOIDS,
        Muscle.LATISSIMUS_DORSI,
        Muscle.ERECTOR_SPINAE,
        Muscle.GLUTEUS_MAXIMUS,
        Muscle.GLUTEUS_MEDIUS,
        Muscle.HAMSTRINGS,
        Muscle.ROTATOR_CUFF,
    )

    private val bothViews = setOf(
        Muscle.LATERAL_DELTOID,
        Muscle.FOREARMS,
        Muscle.CALVES,
        Muscle.NECK,
        Muscle.FULL_BODY,
    )

    fun viewsFor(primary: List<Muscle>, secondary: List<Muscle> = emptyList()): List<BodyMapView> {
        val all = primary + secondary
        var front = false
        var back = false
        for (m in all) {
            when {
                m in bothViews -> {
                    front = true
                    back = true
                }
                m in frontPreferred -> front = true
                m in backPreferred -> back = true
            }
        }
        return buildList {
            if (front) add(BodyMapView.FRONT)
            if (back) add(BodyMapView.BACK)
            if (isEmpty()) add(BodyMapView.FRONT)
        }
    }

    fun exerciseHighlight(
        exerciseId: String,
        primary: List<Muscle>,
        secondary: List<Muscle>,
    ): ExerciseHighlight = ExerciseHighlight(
        exerciseId = exerciseId,
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        views = viewsFor(primary, secondary),
    )

    fun figureKey(sex: BiologicalSex, view: BodyMapView): String {
        val sexKey = when (sex) {
            BiologicalSex.FEMALE -> "female"
            BiologicalSex.MALE, BiologicalSex.UNSPECIFIED -> "male"
        }
        val viewKey = when (view) {
            BodyMapView.FRONT -> "front"
            BodyMapView.BACK -> "back"
        }
        return "$sexKey-$viewKey"
    }
}
