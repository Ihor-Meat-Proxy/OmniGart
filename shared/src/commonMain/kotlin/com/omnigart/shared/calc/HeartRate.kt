package com.omnigart.shared.calc

/**
 * Maximum heart rate and training zones.
 *
 * HRmax reference: Tanaka H, Monahan KD, Seals DR. Age-predicted maximal heart rate revisited.
 * J Am Coll Cardiol. 2001;37(1):153–156. DOI: 10.1016/S0735-1097(00)01054-8
 * Formula: HRmax = 208 − 0.7 × age
 *
 * Karvonen: target HR = HRrest + %intensity × (HRmax − HRrest)
 * Karvonen J, Vuorimaa T. Heart rate and exercise intensity during sports activities.
 * Sports Med. 1988;5(5):303–311.
 */
data class HeartRateZone(val name: String, val lowerBpm: Double, val upperBpm: Double)

object HeartRate {
    fun tanakaMax(ageYears: Int): Double {
        require(ageYears > 0)
        return 208.0 - 0.7 * ageYears
    }

    fun karvonenTarget(restingBpm: Double, maxBpm: Double, intensityFraction: Double): Double {
        require(restingBpm > 0 && maxBpm > restingBpm)
        require(intensityFraction in 0.0..1.0)
        val reserve = maxBpm - restingBpm
        return restingBpm + intensityFraction * reserve
    }

    /**
     * Five Karvonen zones (ACS M-style fractions of HRR):
     * Z1 50–60%, Z2 60–70%, Z3 70–80%, Z4 80–90%, Z5 90–100%.
     */
    fun karvonenFiveZones(restingBpm: Double, maxBpm: Double): List<HeartRateZone> {
        val bounds = listOf(0.50, 0.60, 0.70, 0.80, 0.90, 1.00)
        val names = listOf("Z1", "Z2", "Z3", "Z4", "Z5")
        return names.mapIndexed { i, name ->
            HeartRateZone(
                name = name,
                lowerBpm = karvonenTarget(restingBpm, maxBpm, bounds[i]),
                upperBpm = karvonenTarget(restingBpm, maxBpm, bounds[i + 1])
            )
        }
    }

    /**
     * Three Karvonen zones: Easy 50–70%, Tempo 70–85%, Hard 85–100% of HRR.
     */
    fun karvonenThreeZones(restingBpm: Double, maxBpm: Double): List<HeartRateZone> {
        val bounds = listOf(0.50, 0.70, 0.85, 1.00)
        val names = listOf("Easy", "Tempo", "Hard")
        return names.mapIndexed { i, name ->
            HeartRateZone(
                name = name,
                lowerBpm = karvonenTarget(restingBpm, maxBpm, bounds[i]),
                upperBpm = karvonenTarget(restingBpm, maxBpm, bounds[i + 1])
            )
        }
    }

    /**
     * Five zones as fractions of HRmax (not HRR): 50–60, 60–70, 70–80, 80–90, 90–100 %.
     */
    fun percentHrMaxFiveZones(maxBpm: Double): List<HeartRateZone> {
        require(maxBpm > 0)
        val bounds = listOf(0.50, 0.60, 0.70, 0.80, 0.90, 1.00)
        val names = listOf("Z1", "Z2", "Z3", "Z4", "Z5")
        return names.mapIndexed { i, name ->
            HeartRateZone(
                name = name,
                lowerBpm = maxBpm * bounds[i],
                upperBpm = maxBpm * bounds[i + 1]
            )
        }
    }
}
