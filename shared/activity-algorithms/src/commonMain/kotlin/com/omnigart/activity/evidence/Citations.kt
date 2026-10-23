package com.omnigart.activity.evidence

/**
 * Evidence citations for formulas in this module.
 * Prefer peer-reviewed / standards-body sources; omit rather than invent DOIs.
 */
object Citations {
    const val UTH_SORENSEN_2004 =
        "Uth N, Sørensen H, Overgaard K, Pedersen PK. Estimation of VO2max from the " +
            "ratio between HRmax and HRrest — the Heart Rate Ratio Method. " +
            "Eur J Appl Physiol. 2004;91(1):111-115. doi:10.1007/s00421-003-0988-y"

    const val COOPER_1968 =
        "Cooper KH. A means of assessing maximal oxygen intake: correlation between " +
            "field and treadmill testing. JAMA. 1968;203(3):201-204. " +
            "Common field equation: VO2max (ml·kg⁻¹·min⁻¹) = 22.351 × distance_km − 11.288"

    const val ACSM_RUNNING_METABOLIC =
        "American College of Sports Medicine. ACSM's Guidelines for Exercise Testing " +
            "and Prescription / metabolic equations. Running: " +
            "VO2 = 3.5 + (0.2 × S) + (0.9 × S × G) with S in m·min⁻¹, G as decimal grade. " +
            "See also ACSM Health Fit J. 2020;24(4) metabolic calculations series."

    const val KARVONEN_1957 =
        "Karvonen MJ, Kentala E, Mustala O. The effects of training on heart rate: " +
            "a longitudinal study. Ann Med Exp Biol Fenn. 1957;35(3):307-315. " +
            "Target HR = HRrest + % × (HRmax − HRrest)."

    const val DI_PRAMPERO_COST_OF_RUNNING =
        "di Prampero PE, et al. The energy cost of human locomotion on land and in water. " +
            "Int J Sports Med. 1986;7(2):55-72. Typical flat running cost of transport " +
            "Cr ≈ 3.6–4.0 J·kg⁻¹·m⁻¹ used for optional mechanical-power estimate: " +
            "P ≈ m · v · (Cr + g · grade)."

    const val SWOLF_DEFINITION =
        "SWOLF (swim golf) = stroke count + time in seconds for one pool length. " +
            "Lower is more efficient. Widely used coaching metric; not a peer-reviewed " +
            "physiological constant. Documented here as strokes + elapsed seconds per length."

    const val SWIM_ACCEL_SPEC =
        "Watch-accelerometer swim stroke/lap detection: band-pass the dominant wrist axis, " +
            "peak-pick stroke cycles above a relative amplitude threshold with refractory " +
            "period; detect turns as high-magnitude deceleration/orientation bursts followed " +
            "by a quiet window. Spec + synthetic fixtures in this module; field thresholds " +
            "tuned later against pool sessions (see OPEN_QUESTIONS A#)."
}
