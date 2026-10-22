package com.omnigart.bodymap

/**
 * Legend entries for workout / weekly heatmaps.
 * Hex values mirror `design/body-map/mapping/intensity-scale.json` (design tokens).
 */
data class LegendEntry(
    val intensity: IntensityLevel,
    val label: String,
    val hexLight: String,
    val hexDark: String,
    val token: String,
)

object HeatmapLegend {
    val entries: List<LegendEntry> = listOf(
        LegendEntry(IntensityLevel.L1, "Very low", "#0072B2", "#4BA3D0", "color.hrZone.z1"),
        LegendEntry(IntensityLevel.L2, "Low", "#009E73", "#33B892", "color.hrZone.z2"),
        LegendEntry(IntensityLevel.L3, "Moderate", "#F0E442", "#F5EB6E", "color.hrZone.z3"),
        LegendEntry(IntensityLevel.L4, "High", "#E69F00", "#F0B333", "color.hrZone.z4"),
        LegendEntry(IntensityLevel.L5, "Very high", "#D55E00", "#E87A26", "color.hrZone.z5"),
    )

    fun colorFor(intensity: IntensityLevel, dark: Boolean = false): String? {
        if (intensity == IntensityLevel.IDLE) {
            return if (dark) "#334155" else "#E2E8F0"
        }
        val entry = entries.firstOrNull { it.intensity == intensity } ?: return null
        return if (dark) entry.hexDark else entry.hexLight
    }
}
