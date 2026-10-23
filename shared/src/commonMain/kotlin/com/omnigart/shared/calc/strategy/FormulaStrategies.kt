package com.omnigart.shared.calc.strategy

import com.omnigart.shared.calc.Bmr
import com.omnigart.shared.calc.BodyFatCunBae
import com.omnigart.shared.calc.HeartRate
import com.omnigart.shared.calc.HeartRateZone
import com.omnigart.shared.calc.OneRepMax
import com.omnigart.shared.domain.BiologicalSex

/** User-selectable BMR formula. */
fun interface BmrFormula {
    fun bmrKcal(weightKg: Double, heightCm: Double, ageYears: Int, sex: BiologicalSex): Double
}

object MifflinStJeorBmr : BmrFormula {
    override fun bmrKcal(weightKg: Double, heightCm: Double, ageYears: Int, sex: BiologicalSex): Double =
        Bmr.mifflinStJeor(weightKg, heightCm, ageYears, sex)
}

/**
 * Harris–Benedict (revised Roza & Shizgal 1984) as an alternative.
 * Men: 88.362 + 13.397·w + 4.799·h − 5.677·age
 * Women: 447.593 + 9.247·w + 3.098·h − 4.330·age
 */
object HarrisBenedictBmr : BmrFormula {
    override fun bmrKcal(weightKg: Double, heightCm: Double, ageYears: Int, sex: BiologicalSex): Double {
        require(weightKg > 0 && heightCm > 0 && ageYears > 0)
        return when (sex) {
            BiologicalSex.MALE ->
                88.362 + 13.397 * weightKg + 4.799 * heightCm - 5.677 * ageYears
            BiologicalSex.FEMALE ->
                447.593 + 9.247 * weightKg + 3.098 * heightCm - 4.330 * ageYears
        }
    }
}

fun interface HrMaxFormula {
    fun hrMax(ageYears: Int): Double
}

object TanakaHrMax : HrMaxFormula {
    override fun hrMax(ageYears: Int): Double = HeartRate.tanakaMax(ageYears)
}

/** Fox / traditional: 220 − age. */
object FoxHrMax : HrMaxFormula {
    override fun hrMax(ageYears: Int): Double {
        require(ageYears > 0)
        return 220.0 - ageYears
    }
}

fun interface BodyFatFormula {
    fun percent(weightKg: Double, heightCm: Double, ageYears: Int, sex: BiologicalSex): Double
}

object CunBaeBodyFat : BodyFatFormula {
    override fun percent(weightKg: Double, heightCm: Double, ageYears: Int, sex: BiologicalSex): Double =
        BodyFatCunBae.percent(weightKg, heightCm, ageYears, sex)
}

fun interface OneRepMaxFormula {
    fun e1rm(weightKg: Double, reps: Int): Double
}

object EpleyOneRepMax : OneRepMaxFormula {
    override fun e1rm(weightKg: Double, reps: Int): Double = OneRepMax.epley(weightKg, reps)
}

object BrzyckiOneRepMax : OneRepMaxFormula {
    override fun e1rm(weightKg: Double, reps: Int): Double = OneRepMax.brzycki(weightKg, reps)
}

enum class HrZoneScheme {
    KARVONEN_5,
    KARVONEN_3,
    PERCENT_HRMAX_5
}

fun interface HrZoneFormula {
    fun zones(restingBpm: Double, maxBpm: Double): List<HeartRateZone>
}

object KarvonenFiveZoneFormula : HrZoneFormula {
    override fun zones(restingBpm: Double, maxBpm: Double): List<HeartRateZone> = HeartRate.karvonenFiveZones(restingBpm, maxBpm)
}

object KarvonenThreeZoneFormula : HrZoneFormula {
    override fun zones(restingBpm: Double, maxBpm: Double): List<HeartRateZone> = HeartRate.karvonenThreeZones(restingBpm, maxBpm)
}

object PercentHrMaxFiveZoneFormula : HrZoneFormula {
    override fun zones(restingBpm: Double, maxBpm: Double): List<HeartRateZone> = HeartRate.percentHrMaxFiveZones(maxBpm)
}

/**
 * Registry of formula strategies the UI / profile can select.
 * Defaults match Phase 0 behaviour.
 */
data class FormulaPreferences(
    val bmr: BmrFormula = MifflinStJeorBmr,
    val hrMax: HrMaxFormula = TanakaHrMax,
    val bodyFat: BodyFatFormula = CunBaeBodyFat,
    val oneRepMax: OneRepMaxFormula = EpleyOneRepMax,
    val hrZones: HrZoneFormula = KarvonenFiveZoneFormula
)

object FormulaCatalog {
    const val DEFAULT_BMR = "mifflin_st_jeor"
    const val DEFAULT_HR_MAX = "tanaka"
    const val DEFAULT_BODY_FAT = "cun_bae"
    const val DEFAULT_ONE_RM = "epley"
    const val DEFAULT_HR_ZONES = "karvonen_5"

    val bmrOptions: Map<String, BmrFormula> =
        mapOf(
            DEFAULT_BMR to MifflinStJeorBmr,
            "harris_benedict" to HarrisBenedictBmr
        )
    val hrMaxOptions: Map<String, HrMaxFormula> =
        mapOf(
            DEFAULT_HR_MAX to TanakaHrMax,
            "fox" to FoxHrMax
        )
    val bodyFatOptions: Map<String, BodyFatFormula> =
        mapOf(DEFAULT_BODY_FAT to CunBaeBodyFat)
    val oneRepMaxOptions: Map<String, OneRepMaxFormula> =
        mapOf(
            DEFAULT_ONE_RM to EpleyOneRepMax,
            "brzycki" to BrzyckiOneRepMax
        )
    val hrZoneOptions: Map<String, HrZoneFormula> =
        mapOf(
            DEFAULT_HR_ZONES to KarvonenFiveZoneFormula,
            "karvonen_3" to KarvonenThreeZoneFormula,
            "percent_hrmax_5" to PercentHrMaxFiveZoneFormula
        )

    val bmrMeta: List<FormulaOptionMeta> =
        listOf(
            FormulaOptionMeta(
                DEFAULT_BMR,
                "formula.bmr.mifflin_st_jeor.title",
                "formula.bmr.mifflin_st_jeor.explain",
                simpleDefault = true
            ),
            FormulaOptionMeta(
                "harris_benedict",
                "formula.bmr.harris_benedict.title",
                "formula.bmr.harris_benedict.explain",
                simpleDefault = false
            )
        )

    val hrMaxMeta: List<FormulaOptionMeta> =
        listOf(
            FormulaOptionMeta(
                DEFAULT_HR_MAX,
                "formula.hr_max.tanaka.title",
                "formula.hr_max.tanaka.explain",
                simpleDefault = true
            ),
            FormulaOptionMeta(
                "fox",
                "formula.hr_max.fox.title",
                "formula.hr_max.fox.explain",
                simpleDefault = false
            )
        )

    val bodyFatMeta: List<FormulaOptionMeta> =
        listOf(
            FormulaOptionMeta(
                DEFAULT_BODY_FAT,
                "formula.body_fat.cun_bae.title",
                "formula.body_fat.cun_bae.explain",
                simpleDefault = true
            )
        )

    val oneRmMeta: List<FormulaOptionMeta> =
        listOf(
            FormulaOptionMeta(
                DEFAULT_ONE_RM,
                "formula.one_rm.epley.title",
                "formula.one_rm.epley.explain",
                simpleDefault = true
            ),
            FormulaOptionMeta(
                "brzycki",
                "formula.one_rm.brzycki.title",
                "formula.one_rm.brzycki.explain",
                simpleDefault = false
            )
        )

    val hrZonesMeta: List<FormulaOptionMeta> =
        listOf(
            FormulaOptionMeta(
                DEFAULT_HR_ZONES,
                "formula.hr_zones.karvonen_5.title",
                "formula.hr_zones.karvonen_5.explain",
                simpleDefault = true
            ),
            FormulaOptionMeta(
                "karvonen_3",
                "formula.hr_zones.karvonen_3.title",
                "formula.hr_zones.karvonen_3.explain",
                simpleDefault = false
            ),
            FormulaOptionMeta(
                "percent_hrmax_5",
                "formula.hr_zones.percent_hrmax_5.title",
                "formula.hr_zones.percent_hrmax_5.explain",
                simpleDefault = false
            )
        )

    fun optionsFor(domain: FormulaDomain, mode: FormulaMode): List<FormulaOptionMeta> {
        val all =
            when (domain) {
                FormulaDomain.BMR -> bmrMeta
                FormulaDomain.HR_MAX -> hrMaxMeta
                FormulaDomain.BODY_FAT -> bodyFatMeta
                FormulaDomain.ONE_RM -> oneRmMeta
                FormulaDomain.HR_ZONES -> hrZonesMeta
            }
        return if (mode == FormulaMode.SIMPLE) all.filter { it.simpleDefault } else all
    }

    fun resolveBmrKey(key: String): String = if (bmrOptions.containsKey(key)) key else DEFAULT_BMR

    fun resolveHrMaxKey(key: String): String = if (hrMaxOptions.containsKey(key)) key else DEFAULT_HR_MAX

    fun resolveBodyFatKey(key: String): String = if (bodyFatOptions.containsKey(key)) key else DEFAULT_BODY_FAT

    fun resolveOneRmKey(key: String): String = if (oneRepMaxOptions.containsKey(key)) key else DEFAULT_ONE_RM

    fun resolveHrZonesKey(key: String): String = if (hrZoneOptions.containsKey(key)) key else DEFAULT_HR_ZONES

    fun preferencesFromKeys(
        bmrKey: String = DEFAULT_BMR,
        hrMaxKey: String = DEFAULT_HR_MAX,
        bodyFatKey: String = DEFAULT_BODY_FAT,
        oneRepMaxKey: String = DEFAULT_ONE_RM,
        hrZonesKey: String = DEFAULT_HR_ZONES
    ): FormulaPreferences = FormulaPreferences(
        bmr = bmrOptions[resolveBmrKey(bmrKey)] ?: MifflinStJeorBmr,
        hrMax = hrMaxOptions[resolveHrMaxKey(hrMaxKey)] ?: TanakaHrMax,
        bodyFat = bodyFatOptions[resolveBodyFatKey(bodyFatKey)] ?: CunBaeBodyFat,
        oneRepMax = oneRepMaxOptions[resolveOneRmKey(oneRepMaxKey)] ?: EpleyOneRepMax,
        hrZones = hrZoneOptions[resolveHrZonesKey(hrZonesKey)] ?: KarvonenFiveZoneFormula
    )
}
