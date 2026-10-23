package com.omnigart.sensors.hrv

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HrvAndArtifactTest {

    @Test
    fun rmssdSdnnPnn50OnSyntheticStableSeries() {
        // Constant 1000 ms → RMSSD=0, SDNN=0, pNN50=0
        val constant = List(20) { 1000.0 }
        val h = HrvMetrics.timeDomain(constant)
        assertEquals(1000.0, h.meanRrMs, 1e-9)
        assertEquals(0.0, h.sdnnMs, 1e-9)
        assertEquals(0.0, h.rmssdMs, 1e-9)
        assertEquals(0.0, h.pnn50Percent, 1e-9)
        assertEquals(ln(1e-9), h.lnRmssd, 1e-6)
    }

    @Test
    fun rmssdMatchesHandCalculation() {
        val rr = listOf(800.0, 850.0, 820.0, 870.0, 810.0)
        val diffs = listOf(50.0, -30.0, 50.0, -60.0)
        val expectedRmssd = sqrt(diffs.sumOf { it * it } / diffs.size)
        val h = HrvMetrics.timeDomain(rr)
        assertEquals(expectedRmssd, h.rmssdMs, 1e-9)
        assertEquals(ln(expectedRmssd), h.lnRmssd, 1e-9)
        // |50| is not >50; |-30|; |50|; |-60| → 1 of 4 = 25%
        assertEquals(25.0, h.pnn50Percent, 1e-9)
    }

    @Test
    fun sdnnSampleVariance() {
        val rr = listOf(1000.0, 1100.0, 900.0)
        val mean = 1000.0
        val expected = sqrt(
            listOf(0.0, 100.0, -100.0).sumOf { it * it } / 2.0
        )
        assertEquals(expected, HrvMetrics.timeDomain(rr).sdnnMs, 1e-9)
        assertEquals(mean, HrvMetrics.timeDomain(rr).meanRrMs, 1e-9)
    }

    @Test
    fun correctsExtraBeat() {
        // ~1000 ms series with an inserted short interval (extra beat)
        val clean = List(30) { 1000.0 }
        val withExtra = clean.toMutableList().also {
            it.add(15, 400.0)
            it.add(16, 400.0) // split of a beat into two shorts often accompanies extras
        }
        // Simpler: replace one interval with a very short one and next compensating
        val series = List(40) { 1000.0 }.toMutableList()
        series[20] = 300.0
        val result = RrArtifactCorrection.correct(series)
        assertTrue(result.correctedRrMs.all { it in 200.0..2000.0 })
        // Correction should not explode length
        assertTrue(abs(result.correctedRrMs.size - series.size) <= 3)
    }

    @Test
    fun correctsMissedBeatLongInterval() {
        val series = List(40) { 1000.0 }.toMutableList()
        series[20] = 2000.0 // missed beat → long RR
        val result = RrArtifactCorrection.correct(series)
        assertTrue(result.artifacts.any { it.kind == RrArtifactCorrection.ArtifactKind.Missed })
        assertTrue(result.correctedRrMs.none { it >= 1900.0 })
        assertTrue(result.correctedRrMs.isNotEmpty())
    }

    @Test
    fun baevskySiCanonicalFormula() {
        // Construct histogram with clear mode at 1000 ms
        val rr = buildList {
            repeat(40) { add(1000.0) }
            repeat(10) { add(900.0) }
            repeat(10) { add(1100.0) }
        }
        val si = BaevskyStressIndex.compute(rr, binWidthMs = 50.0)
        assertEquals(BaevskyVariant.HistogramMode, si.variant)
        assertEquals(50.0, si.binWidthMs)
        // Mo near 1.0 s, AMo = 40/60, MxDMn = 0.2 s
        // SI = (40/60) / (2 * ~1.0 * 0.2) ≈ 0.6667 / 0.4 ≈ 1.667
        val expected = si.amoFraction / (2.0 * si.moSeconds * si.mxDMnSeconds)
        assertEquals(expected, si.si, 1e-12)
        assertEquals(sqrt(si.si), si.sqrtSi, 1e-12)
        assertTrue(si.amoFraction > 0.5)
        assertTrue(si.moSeconds in 0.95..1.05)
        assertEquals(0.2, si.mxDMnSeconds, 1e-9)
    }

    @Test
    fun baevskyMedianVariant() {
        val rr = listOf(800.0, 900.0, 1000.0, 1100.0, 1200.0)
        val si = BaevskyStressIndex.compute(rr, modeAsMedian = true)
        assertEquals(BaevskyVariant.MedianAsMode, si.variant)
        assertEquals(1.0, si.moSeconds, 1e-9)
    }

    @Test
    fun qualityScoreDegradesWithArtifactsAndBadContact() {
        val raw = List(50) { 1000.0 }
        val good = SignalQuality.score(
            QualityInput(raw, raw, artifactCount = 0, sensorContactOk = true)
        )
        val bad = SignalQuality.score(
            QualityInput(raw, raw.take(40), artifactCount = 20, sensorContactOk = false)
        )
        assertTrue(good > bad)
        assertTrue(good in 0.0..1.0)
        assertTrue(bad in 0.0..1.0)
    }

    @Test
    fun rrProcessorPipeline() {
        val rr = List(60) { i -> 900.0 + (i % 5) * 10.0 }
        val out = RrProcessor.process(rr, sensorContactOk = true)
        assertTrue(out.correctedRrMs.size >= 2)
        assertTrue(out.hrv != null)
        assertTrue(out.baevsky != null)
        assertTrue(out.quality > 0.5)
    }

    /**
     * Published-style resting sample: synthetic tachogram approximating ~60 bpm with
     * modest sinus arrhythmia (not a copyrighted PhysioNet dump; hand-built vector).
     * Expected: RMSSD and SDNN in a plausible resting band.
     */
    @Test
    fun publishedStyleRestingTachogramBand() {
        val rr = listOf(
            980.0, 1010.0, 995.0, 1025.0, 970.0, 1005.0, 990.0, 1015.0,
            985.0, 1000.0, 975.0, 1020.0, 960.0, 1008.0, 992.0, 1012.0,
            978.0, 1002.0, 988.0, 1018.0, 965.0, 1006.0, 994.0, 1014.0,
            982.0, 998.0, 986.0, 1022.0, 972.0, 1004.0, 996.0, 1010.0,
        )
        val corrected = RrArtifactCorrection.correct(rr).correctedRrMs
        val h = HrvMetrics.timeDomain(corrected)
        assertTrue(h.meanRrMs in 950.0..1050.0, "mean=${h.meanRrMs}")
        assertTrue(h.rmssdMs in 5.0..80.0, "rmssd=${h.rmssdMs}")
        assertTrue(h.sdnnMs in 5.0..80.0, "sdnn=${h.sdnnMs}")
        assertTrue(h.lnRmssd in 1.5..5.0)
    }
}
