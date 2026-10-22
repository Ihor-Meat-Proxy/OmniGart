package com.omnigart.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Discipline {
    @SerialName("strength")
    STRENGTH,

    @SerialName("yoga")
    YOGA,

    @SerialName("pilates")
    PILATES,

    @SerialName("swimming")
    SWIMMING,

    @SerialName("running")
    RUNNING
}

@Serializable
enum class MovementPattern {
    @SerialName("squat")
    SQUAT,

    @SerialName("hinge")
    HINGE,

    @SerialName("push")
    PUSH,

    @SerialName("pull")
    PULL,

    @SerialName("carry")
    CARRY,

    @SerialName("lunge")
    LUNGE,

    @SerialName("rotation")
    ROTATION,

    @SerialName("gait")
    GAIT,

    @SerialName("core_brace")
    CORE_BRACE,

    @SerialName("mobility")
    MOBILITY,

    @SerialName("balance")
    BALANCE,

    @SerialName("swim_stroke")
    SWIM_STROKE,

    @SerialName("swim_drill")
    SWIM_DRILL,

    @SerialName("run_drill")
    RUN_DRILL,

    @SerialName("isometric")
    ISOMETRIC,

    @SerialName("other")
    OTHER
}

@Serializable
enum class Muscle {
    @SerialName("quadriceps")
    QUADRICEPS,

    @SerialName("hamstrings")
    HAMSTRINGS,

    @SerialName("gluteus_maximus")
    GLUTEUS_MAXIMUS,

    @SerialName("gluteus_medius")
    GLUTEUS_MEDIUS,

    @SerialName("calves")
    CALVES,

    @SerialName("hip_flexors")
    HIP_FLEXORS,

    @SerialName("adductors")
    ADDUCTORS,

    @SerialName("pectoralis_major")
    PECTORALIS_MAJOR,

    @SerialName("latissimus_dorsi")
    LATISSIMUS_DORSI,

    @SerialName("trapezius")
    TRAPEZIUS,

    @SerialName("rhomboids")
    RHOMBOIDS,

    @SerialName("rear_deltoid")
    REAR_DELTOID,

    @SerialName("front_deltoid")
    FRONT_DELTOID,

    @SerialName("lateral_deltoid")
    LATERAL_DELTOID,

    @SerialName("biceps")
    BICEPS,

    @SerialName("triceps")
    TRICEPS,

    @SerialName("forearms")
    FOREARMS,

    @SerialName("rectus_abdominis")
    RECTUS_ABDOMINIS,

    @SerialName("obliques")
    OBLIQUES,

    @SerialName("transverse_abdominis")
    TRANSVERSE_ABDOMINIS,

    @SerialName("erector_spinae")
    ERECTOR_SPINAE,

    @SerialName("full_body")
    FULL_BODY,

    @SerialName("rotator_cuff")
    ROTATOR_CUFF,

    @SerialName("serratus_anterior")
    SERRATUS_ANTERIOR,

    @SerialName("tibialis_anterior")
    TIBIALIS_ANTERIOR,

    @SerialName("neck")
    NECK
}

@Serializable
enum class Equipment {
    @SerialName("barbell")
    BARBELL,

    @SerialName("dumbbell")
    DUMBBELL,

    @SerialName("kettlebell")
    KETTLEBELL,

    @SerialName("cable")
    CABLE,

    @SerialName("machine")
    MACHINE,

    @SerialName("bands")
    BANDS,

    @SerialName("bodyweight")
    BODYWEIGHT,

    @SerialName("bench")
    BENCH,

    @SerialName("pull_up_bar")
    PULL_UP_BAR,

    @SerialName("trap_bar")
    TRAP_BAR,

    @SerialName("ez_bar")
    EZ_BAR,

    @SerialName("medicine_ball")
    MEDICINE_BALL,

    @SerialName("swiss_ball")
    SWISS_BALL,

    @SerialName("foam_roller")
    FOAM_ROLLER,

    @SerialName("yoga_mat")
    YOGA_MAT,

    @SerialName("yoga_block")
    YOGA_BLOCK,

    @SerialName("yoga_strap")
    YOGA_STRAP,

    @SerialName("pilates_reformer")
    PILATES_REFORMER,

    @SerialName("pilates_mat")
    PILATES_MAT,

    @SerialName("pool")
    POOL,

    @SerialName("kickboard")
    KICKBOARD,

    @SerialName("pull_buoy")
    PULL_BUOY,

    @SerialName("fins")
    FINS,

    @SerialName("paddle")
    PADDLE,

    @SerialName("treadmill")
    TREADMILL,

    @SerialName("track")
    TRACK,

    @SerialName("road")
    ROAD,

    @SerialName("sled")
    SLED,

    @SerialName("landmine")
    LANDMINE,

    @SerialName("suspension")
    SUSPENSION,

    @SerialName("box")
    BOX,

    @SerialName("rope")
    ROPE,

    @SerialName("none")
    NONE
}

@Serializable
enum class Level {
    @SerialName("beginner")
    BEGINNER,

    @SerialName("intermediate")
    INTERMEDIATE,

    @SerialName("advanced")
    ADVANCED
}

@Serializable
enum class Mechanics {
    @SerialName("compound")
    COMPOUND,

    @SerialName("isolation")
    ISOLATION,

    @SerialName("isometric")
    ISOMETRIC,

    @SerialName("skill")
    SKILL
}

@Serializable
enum class TrackingType {
    @SerialName("weight_reps")
    WEIGHT_REPS,

    @SerialName("reps")
    REPS,

    @SerialName("timed")
    TIMED,

    @SerialName("distance")
    DISTANCE,

    @SerialName("distance_time")
    DISTANCE_TIME,

    @SerialName("hold")
    HOLD,

    @SerialName("laps")
    LAPS,

    @SerialName("bodyweight_reps")
    BODYWEIGHT_REPS
}

@Serializable
data class LocalizedNameKeys(val enGB: String, val uk: String)

@Serializable
data class ExerciseLink(val exerciseId: String, val note: String)

@Serializable
data class EvidenceRef(val kind: String, val id: String, val label: String, val supports: List<String>)

@Serializable
data class MediaPlaceholders(val illustrationKey: String? = null, val videoKey: String? = null, val animationKey: String? = null)

@Serializable
enum class LoadModel {
    @SerialName("bodyweight_plus_external") BODYWEIGHT_PLUS_EXTERNAL,
    @SerialName("bodyweight_minus_assistance") BODYWEIGHT_MINUS_ASSISTANCE,
    @SerialName("external_only") EXTERNAL_ONLY,
}

@Serializable
data class BodyweightFraction(
    val proportion: Double,
    val basis: String,
    val rationale: String,
)

@Serializable
data class Exercise(
    val id: String,
    val nameKeys: LocalizedNameKeys,
    val discipline: Discipline,
    val movementPattern: MovementPattern,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle> = emptyList(),
    val equipment: List<Equipment>,
    val level: Level,
    val unilateral: Boolean,
    val mechanics: Mechanics,
    val trackingType: TrackingType,
    val technique: List<String>,
    val setup: List<String>,
    val breathing: String,
    val commonMistakes: List<String>,
    val safety: List<String>,
    val contraindications: List<String> = emptyList(),
    val regressions: List<ExerciseLink> = emptyList(),
    val progressions: List<ExerciseLink> = emptyList(),
    val evidenceRefs: List<EvidenceRef> = emptyList(),
    val media: MediaPlaceholders = MediaPlaceholders(),
    val aliases: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val loadModel: LoadModel = LoadModel.EXTERNAL_ONLY,
    val bodyweightFraction: BodyweightFraction =
        BodyweightFraction(
            proportion = 0.0,
            basis = "estimate",
            rationale = "Default external-only placeholder; content JSON should override."
        ),
    val allowsAddedWeight: Boolean = true,
    val allowsAssistance: Boolean = false
)
