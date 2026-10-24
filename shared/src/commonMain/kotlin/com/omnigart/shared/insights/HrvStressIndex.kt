package com.omnigart.shared.insights

import kotlin.math.round

/**
 * Baevsky-style stress index **estimate** from RR intervals, or a bounded proxy
 * from RMSSD/SDNN when RR data is unavailable. Wellness only — not a clinical SI.
 */
object HrvStressIndex {
    data class Result(val index: Double?, val fromRrIntervals: Boolean, val labelledEstimate: Boolean = true)

    /**
     * @param rrMs successive RR intervals in milliseconds (at least ~30 preferred).
     */
    fun fromRrIntervals(rrMs: List<Double>): Result {
        val clean = rrMs.filter { it in 300.0..2000.0 }
        if (clean.size < 20) return Result(null, fromRrIntervals = true)
        val sorted = clean.sorted()
        val mo = modeRounded(sorted)
        val amo = clean.count { kotlin.math.abs(it - mo) <= 25.0 }.toDouble() / clean.size
        val mxDMn = (sorted.last() - sorted.first()).coerceAtLeast(1.0) / 1000.0 // seconds
        val moSec = mo / 1000.0
        // SI ≈ AMo(%) / (2 · Mo · MxDMn); AMo as percent.
        val si = (amo * 100.0) / (2.0 * moSec * mxDMn)
        return Result(round1(si.coerceIn(0.0, 1000.0)), fromRrIntervals = true)
    }

    /**
     * Proxy when only HRV magnitude is known: lower HRV → higher index (0–10 scale-ish).
     * @param hrvMs RMSSD or SDNN in milliseconds.
     */
    fun fromHrvMagnitude(hrvMs: Double?): Result {
        if (hrvMs == null || hrvMs <= 0) return Result(null, fromRrIntervals = false)
        // Soft map: 100 ms → ~2, 20 ms → ~8 (wellness sketch).
        val clamped = hrvMs.coerceIn(10.0, 150.0)
        val index = (10.0 - (clamped - 10.0) * (8.0 / 140.0)).coerceIn(1.0, 10.0)
        return Result(round1(index), fromRrIntervals = false)
    }

    private fun modeRounded(sortedMs: List<Double>): Double {
        // Mode of values rounded to 50 ms bins.
        val bins = sortedMs.groupingBy { (it / 50.0).toInt() * 50.0 }.eachCount()
        return bins.maxByOrNull { it.value }?.key ?: sortedMs[sortedMs.size / 2]
    }

    private fun round1(v: Double): Double = round(v * 10.0) / 10.0
}
