package com.omnigart.sensors.hrv

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * RR-interval artefact detection and correction after Lipponen & Tarvainen (2019).
 *
 * Citation: Lipponen JA, Tarvainen MP. A robust algorithm for heart rate variability
 * time series artefact correction using novel beat classification.
 * J Med Eng Technol. 2019;43(3):173–181. DOI: [10.1080/03091902.2019.1640306](https://doi.org/10.1080/03091902.2019.1640306)
 *
 * Implementation notes:
 * - Operates on RR intervals in **milliseconds** (not R-peak sample indices).
 * - Default thresholds follow the paper / Kubios reference implementation
 *   (c1=0.13, c2=0.17, α=5.2, median filter order 11, quantile window 91).
 * - Extra beats are removed; missed beats are split into two equal intervals;
 *   ectopic / long-short beats are replaced by linear interpolation of neighbours.
 */
object RrArtifactCorrection {
    data class Params(
        val c1: Double = 0.13,
        val c2: Double = 0.17,
        val alpha: Double = 5.2,
        val windowWidth: Int = 91,
        val medfiltOrder: Int = 11,
        val iterative: Boolean = true
    )

    enum class ArtifactKind {
        Ectopic,
        Missed,
        Extra,
        LongShort
    }

    data class Artifact(val index: Int, val kind: ArtifactKind)

    data class Result(val correctedRrMs: List<Double>, val artifacts: List<Artifact>, val iterations: Int)

    fun correct(rrMs: List<Double>, params: Params = Params()): Result {
        require(rrMs.all { it > 0.0 }) { "RR intervals must be positive" }
        if (rrMs.size < 3) {
            return Result(rrMs, emptyList(), 0)
        }

        // Pre-pass: obvious missed / extra beats relative to the series median.
        // Complements Lipponen & Tarvainen (2019) for clear doubled/halved intervals
        // that the subspace classifier may miss on short synthetic windows.
        val (preCorrected, preArtifacts) = obviousCorrections(rrMs)

        var current = preCorrected
        var lastArtifacts = preArtifacts
        var iterations = 0
        var previousCount = Int.MAX_VALUE

        while (true) {
            iterations += 1
            val (artifacts, _) = findArtifacts(current, params)
            if (artifacts.isEmpty()) break
            lastArtifacts = lastArtifacts + artifacts
            current = applyCorrections(current, artifacts)
            if (!params.iterative) break
            if (artifacts.size >= previousCount) break
            previousCount = artifacts.size
            if (iterations > 20) break
        }

        return Result(current, lastArtifacts, iterations)
    }

    /**
     * Rule-of-thumb corrections used widely alongside academic detectors:
     * RR &gt; 1.8× median → missed beat (split in half);
     * RR &lt; 0.45× median → extra beat (drop).
     * Thresholds are conservative vs resting sinus arrhythmia.
     */
    internal fun obviousCorrections(rrMs: List<Double>): Pair<List<Double>, List<Artifact>> {
        if (rrMs.size < 3) return rrMs to emptyList()
        val median = rrMs.sorted().let { sorted ->
            val m = sorted.size / 2
            if (sorted.size % 2 == 0) (sorted[m - 1] + sorted[m]) / 2.0 else sorted[m]
        }
        val out = mutableListOf<Double>()
        val artifacts = mutableListOf<Artifact>()
        for ((index, rr) in rrMs.withIndex()) {
            when {
                rr > median * 1.8 -> {
                    artifacts += Artifact(index, ArtifactKind.Missed)
                    out += rr / 2.0
                    out += rr / 2.0
                }
                rr < median * 0.45 -> {
                    artifacts += Artifact(index, ArtifactKind.Extra)
                    // drop
                }
                else -> out += rr
            }
        }
        return out to artifacts
    }

    internal fun findArtifacts(rrMs: List<Double>, params: Params): Pair<List<Artifact>, ArtifactSubspaces> {
        val rrSec = rrMs.map { it / 1000.0 }.toMutableList()
        if (rrSec.size > 1) {
            rrSec[0] = rrSec.drop(1).average()
        }

        val drrs = MutableList(rrSec.size) { 0.0 }
        for (i in 1 until rrSec.size) {
            drrs[i] = rrSec[i] - rrSec[i - 1]
        }
        if (drrs.size > 1) {
            drrs[0] = drrs.drop(1).average()
        }

        val th1 = computeThreshold(drrs.map { abs(it) }, params.alpha, params.windowWidth)
        val drrsNorm = drrs.mapIndexed { i, v ->
            val t = th1[i]
            if (t == 0.0) Double.NaN else v / t
        }

        val s12 = DoubleArray(drrsNorm.size)
        val s22 = DoubleArray(drrsNorm.size)
        for (i in drrsNorm.indices) {
            val d = drrsNorm[i]
            val prev = drrsNorm.getOrElse(i - 1) { d }
            val next = drrsNorm.getOrElse(i + 1) { d }
            val next2 = drrsNorm.getOrElse(i + 2) { next }
            s12[i] = when {
                d > 0 -> max(prev, next)
                d < 0 -> min(prev, next)
                else -> 0.0
            }
            s22[i] = when {
                d >= 0 -> min(next, next2)
                else -> max(next, next2)
            }
        }

        val medrr = rollingMedian(rrSec, params.medfiltOrder)
        val mrrsRaw = rrSec.mapIndexed { i, v ->
            var d = v - medrr[i]
            if (d < 0) d *= 2
            d
        }
        val th2 = computeThreshold(mrrsRaw.map { abs(it) }, params.alpha, params.windowWidth)
        val mrrs = mrrsRaw.mapIndexed { i, v ->
            val t = th2[i]
            if (t == 0.0) Double.NaN else v / t
        }

        val artifacts = mutableListOf<Artifact>()
        var i = 0
        while (i < rrSec.size - 2) {
            val d = drrsNorm[i]
            if (abs(d) <= 1.0 || d.isNaN()) {
                i += 1
                continue
            }
            val ectopic = (d > 1 && s12[i] < (-params.c1 * d - params.c2)) ||
                (d < -1 && s12[i] > (-params.c1 * d + params.c2))
            if (ectopic) {
                artifacts += Artifact(i, ArtifactKind.Ectopic)
                i += 1
                continue
            }
            if (!(abs(d) > 1 || abs(mrrs[i]) > 3)) {
                i += 1
                continue
            }
            val candidates = mutableListOf(i)
            if (i + 2 < drrsNorm.size && abs(drrsNorm[i + 1]) < abs(drrsNorm[i + 2])) {
                candidates += i + 1
            }
            for (j in candidates) {
                val dj = drrsNorm[j]
                val eq3 = dj > 1 && s22[j] < -1
                val eq4 = abs(mrrs[j]) > 3
                val eq5 = dj < -1 && s22[j] > 1
                if (!(eq3 || eq4 || eq5)) continue
                val eq6 = abs(rrSec[j] / 2 - medrr[j]) < th2[j]
                val eq7 = j + 1 < rrSec.size &&
                    abs(rrSec[j] + rrSec[j + 1] - medrr[j]) < th2[j]
                when {
                    eq5 && eq7 -> artifacts += Artifact(j, ArtifactKind.Extra)
                    eq3 && eq6 -> artifacts += Artifact(j, ArtifactKind.Missed)
                    else -> artifacts += Artifact(j, ArtifactKind.LongShort)
                }
            }
            i += 1
        }

        val subspaces = ArtifactSubspaces(rrSec, drrsNorm, mrrs, s12.toList(), s22.toList())
        return artifacts.distinctBy { it.index to it.kind } to subspaces
    }

    private fun applyCorrections(rrMs: List<Double>, artifacts: List<Artifact>): List<Double> {
        val byKind = artifacts.groupBy { it.kind }
        var peaks = rrToPeaks(rrMs).toMutableList()

        val extras = byKind[ArtifactKind.Extra].orEmpty().map { it.index }.sortedDescending()
        for (idx in extras) {
            if (idx in peaks.indices) peaks.removeAt(idx)
        }

        var missed = byKind[ArtifactKind.Missed].orEmpty().map { it.index }.toMutableList()
        missed = updateIndices(extras.reversed(), missed, -1).sorted().toMutableList()
        for (idx in missed.sortedDescending()) {
            if (idx <= 0 || idx >= peaks.size) continue
            val prev = peaks[idx - 1]
            val next = peaks[idx]
            peaks.add(idx, prev + (next - prev) / 2.0)
        }

        var ectopic = byKind[ArtifactKind.Ectopic].orEmpty().map { it.index }.toMutableList()
        var longShort = byKind[ArtifactKind.LongShort].orEmpty().map { it.index }.toMutableList()
        ectopic = updateIndices(extras.reversed(), ectopic, -1)
        longShort = updateIndices(extras.reversed(), longShort, -1)
        ectopic = updateIndices(missed, ectopic, 1)
        longShort = updateIndices(missed, longShort, 1)

        fun interpolate(indices: List<Int>) {
            for (idx in indices.sortedDescending()) {
                if (idx <= 0 || idx >= peaks.size - 1) continue
                val prev = peaks[idx - 1]
                val next = peaks[idx + 1]
                peaks[idx] = prev + (next - prev) / 2.0
            }
        }
        interpolate(ectopic)
        interpolate(longShort)

        return peaksToRr(peaks)
    }

    private fun rrToPeaks(rrMs: List<Double>): List<Double> {
        val peaks = mutableListOf(0.0)
        var t = 0.0
        for (rr in rrMs) {
            t += rr
            peaks += t
        }
        return peaks
    }

    private fun peaksToRr(peaks: List<Double>): List<Double> {
        if (peaks.size < 2) return emptyList()
        return peaks.zipWithNext { a, b -> b - a }
    }

    private fun updateIndices(source: List<Int>, update: List<Int>, delta: Int): MutableList<Int> {
        if (update.isEmpty()) return update.toMutableList()
        var result = update
        for (s in source) {
            result = result.map { u -> if (u > s) u + delta else u }
        }
        return result.distinct().toMutableList()
    }

    private fun computeThreshold(signal: List<Double>, alpha: Double, windowWidth: Int): List<Double> {
        return signal.indices.map { i ->
            val half = windowWidth / 2
            val from = max(0, i - half)
            val to = min(signal.size, i + half + 1)
            val window = signal.subList(from, to).sorted()
            if (window.isEmpty()) return@map 0.0
            val q1 = quantile(window, 0.25)
            val q3 = quantile(window, 0.75)
            alpha * ((q3 - q1) / 2.0)
        }
    }

    private fun rollingMedian(signal: List<Double>, order: Int): List<Double> {
        val half = order / 2
        return signal.indices.map { i ->
            val from = max(0, i - half)
            val to = min(signal.size, i + half + 1)
            val window = signal.subList(from, to).sorted()
            median(window)
        }
    }

    private fun median(sorted: List<Double>): Double {
        if (sorted.isEmpty()) return 0.0
        val m = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[m - 1] + sorted[m]) / 2.0 else sorted[m]
    }

    private fun quantile(sorted: List<Double>, q: Double): Double {
        if (sorted.isEmpty()) return 0.0
        if (sorted.size == 1) return sorted[0]
        val pos = q * (sorted.size - 1)
        val lo = pos.toInt()
        val hi = min(sorted.size - 1, lo + 1)
        val frac = pos - lo
        return sorted[lo] * (1 - frac) + sorted[hi] * frac
    }

    internal data class ArtifactSubspaces(
        val rrSec: List<Double>,
        val drrs: List<Double>,
        val mrrs: List<Double>,
        val s12: List<Double>,
        val s22: List<Double>
    )
}
