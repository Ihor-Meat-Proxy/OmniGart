package com.omnigart.bodymap

import com.omnigart.content.Muscle

/**
 * Canonical expansion of exercise taxonomy muscles → fine-grained SVG region ids.
 * Must stay aligned with `design/body-map/mapping/taxonomy-to-regions.json`.
 */
object TaxonomyRegions {

    /** All drawable region ids across front + back (excluding outline/canvas / wildcards). */
    val allDrawableRegions: Set<String> by lazy {
        mappings
            .filterKeys { it != Muscle.FULL_BODY }
            .values
            .flatten()
            .filter { it != "*" }
            .toSet()
    }

    val mappings: Map<Muscle, List<String>> = mapOf(
        Muscle.PECTORALIS_MAJOR to listOf(
            "pectoralis_major_clavicular_l",
            "pectoralis_major_clavicular_r",
            "pectoralis_major_sternal_l",
            "pectoralis_major_sternal_r",
        ),
        Muscle.FRONT_DELTOID to listOf("front_deltoid_l", "front_deltoid_r"),
        Muscle.LATERAL_DELTOID to listOf(
            "lateral_deltoid_l",
            "lateral_deltoid_r",
            "lateral_deltoid_back_l",
            "lateral_deltoid_back_r",
        ),
        Muscle.REAR_DELTOID to listOf("rear_deltoid_l", "rear_deltoid_r"),
        Muscle.BICEPS to listOf("biceps_l", "biceps_r", "brachialis_l", "brachialis_r"),
        Muscle.TRICEPS to listOf("triceps_l", "triceps_r"),
        Muscle.FOREARMS to listOf(
            "forearm_flexors_l",
            "forearm_flexors_r",
            "forearm_extensors_l",
            "forearm_extensors_r",
        ),
        Muscle.RECTUS_ABDOMINIS to listOf("rectus_abdominis"),
        Muscle.OBLIQUES to listOf("obliques_l", "obliques_r"),
        Muscle.TRANSVERSE_ABDOMINIS to listOf("rectus_abdominis", "obliques_l", "obliques_r"),
        Muscle.SERRATUS_ANTERIOR to listOf("serratus_anterior_l", "serratus_anterior_r"),
        Muscle.TRAPEZIUS to listOf("trapezius_upper_l", "trapezius_upper_r", "trapezius_middle"),
        Muscle.RHOMBOIDS to listOf("rhomboids_l", "rhomboids_r"),
        Muscle.LATISSIMUS_DORSI to listOf("latissimus_dorsi_l", "latissimus_dorsi_r"),
        Muscle.ERECTOR_SPINAE to listOf("erector_spinae_l", "erector_spinae_r"),
        Muscle.GLUTEUS_MAXIMUS to listOf("gluteus_maximus_l", "gluteus_maximus_r"),
        Muscle.GLUTEUS_MEDIUS to listOf("gluteus_medius_l", "gluteus_medius_r"),
        Muscle.ADDUCTORS to listOf("adductors_l", "adductors_r"),
        Muscle.HIP_FLEXORS to listOf("hip_flexors_l", "hip_flexors_r"),
        Muscle.QUADRICEPS to listOf(
            "rectus_femoris_l",
            "rectus_femoris_r",
            "vastus_lateralis_l",
            "vastus_lateralis_r",
            "vastus_medialis_l",
            "vastus_medialis_r",
        ),
        Muscle.HAMSTRINGS to listOf("hamstrings_l", "hamstrings_r"),
        Muscle.CALVES to listOf(
            "gastrocnemius_l",
            "gastrocnemius_r",
            "soleus_l",
            "soleus_r",
            "gastrocnemius_front_l",
            "gastrocnemius_front_r",
        ),
        Muscle.TIBIALIS_ANTERIOR to listOf("tibialis_anterior_l", "tibialis_anterior_r"),
        Muscle.NECK to listOf("neck_front", "neck_back"),
        Muscle.ROTATOR_CUFF to listOf("rotator_cuff_l", "rotator_cuff_r"),
        Muscle.FULL_BODY to listOf("*"),
    )

    fun regionsFor(muscles: Collection<Muscle>): Set<String> {
        val out = linkedSetOf<String>()
        for (m in muscles) {
            val regions = mappings[m] ?: continue
            if (regions == listOf("*") || regions.contains("*")) {
                out.addAll(allDrawableRegions)
            } else {
                out.addAll(regions)
            }
        }
        return out
    }

    fun covers(muscle: Muscle): Boolean = mappings.containsKey(muscle)
}
