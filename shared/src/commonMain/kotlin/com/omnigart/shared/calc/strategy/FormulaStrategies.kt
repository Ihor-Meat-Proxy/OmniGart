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
    val bmrOptions: Map<String, BmrFormula> =
        mapOf(
            "mifflin_st_jeor" to MifflinStJeorBmr,
            "harris_benedict" to HarrisBenedictBmr
        )
    val hrMaxOptions: Map<String, HrMaxFormula> =
        mapOf(
            "tanaka" to TanakaHrMax,
            "fox" to FoxHrMax
        )
    val bodyFatOptions: Map<String, BodyFatFormula> =
        mapOf("cun_bae" to CunBaeBodyFat)
    val oneRepMaxOptions: Map<String, OneRepMaxFormula> =
        mapOf(
            "epley" to EpleyOneRepMax,
            "brzycki" to BrzyckiOneRepMax
        )
    val hrZoneOptions: Map<String, HrZoneFormula> =
        mapOf(
            "karvonen_5" to KarvonenFiveZoneFormula,
            "karvonen_3" to KarvonenThreeZoneFormula,
            "percent_hrmax_5" to PercentHrMaxFiveZoneFormula
        )

    fun preferencesFromKeys(
        bmrKey: String = "mifflin_st_jeor",
        hrMaxKey: String = "tanaka",
        bodyFatKey: String = "cun_bae",
        oneRepMaxKey: String = "epley",
        hrZonesKey: String = "karvonen_5"
    ): FormulaPreferences = FormulaPreferences(
        bmr = bmrOptions[bmrKey] ?: MifflinStJeorBmr,
        hrMax = hrMaxOptions[hrMaxKey] ?: TanakaHrMax,
        bodyFat = bodyFatOptions[bodyFatKey] ?: CunBaeBodyFat,
        oneRepMax = oneRepMaxOptions[oneRepMaxKey] ?: EpleyOneRepMax,
        hrZones = hrZoneOptions[hrZonesKey] ?: KarvonenFiveZoneFormula
    )
}
