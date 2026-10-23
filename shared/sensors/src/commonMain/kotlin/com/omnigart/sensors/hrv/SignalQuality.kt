package com.omnigart.sensors.hrv

import kotlin.math.min

/**
 * Simple RR-series quality score in \[0, 1\].
 *
 * Factors (multiplicative, then clamped):
 * - Fraction of intervals retained after artefact correction
 * - Physiological RR range share (300–2000 ms)
 * - Contact / packet validity hints from the caller
 */
data class QualityInput(
    val rawRrMs: List<Double>,
    val correctedRrMs: List<Double>,
    val artifactCount: Int,
    val sensorContactOk: Boolean? = null,
    val packetParseSuccessRate: Double = 1.0
)

object SignalQuality {
    fun score(input: QualityInput): Double {
        if (input.rawRrMs.isEmpty()) return 0.0

        val retention = if (input.rawRrMs.isEmpty()) {
            0.0
        } else {
            min(1.0, input.correctedRrMs.size.toDouble() / input.rawRrMs.size.toDouble())
        }
        val physio = input.correctedRrMs.count { it in 300.0..2000.0 }
            .toDouble() / input.correctedRrMs.size.coerceAtLeast(1)
        val artifactPenalty = 1.0 - min(
            1.0,
            input.artifactCount.toDouble() / input.rawRrMs.size.coerceAtLeast(1)
        )
        val contact = when (input.sensorContactOk) {
            null -> 1.0
            true -> 1.0
            false -> 0.4
        }
        val parse = input.packetParseSuccessRate.coerceIn(0.0, 1.0)

        val raw = retention * physio * artifactPenalty * contact * parse
        return raw.coerceIn(0.0, 1.0)
    }
}

/**
 * End-to-end RR processing: correct artefacts, compute HRV + SI + quality.
 */
data class RrProcessingResult(
    val correctedRrMs: List<Double>,
    val artifacts: List<RrArtifactCorrection.Artifact>,
    val hrv: HrvTimeDomain?,
    val baevsky: BaevskyResult?,
    val quality: Double
)

object RrProcessor {
    fun process(
        rrMs: List<Double>,
        sensorContactOk: Boolean? = null,
        packetParseSuccessRate: Double = 1.0,
        correctionParams: RrArtifactCorrection.Params = RrArtifactCorrection.Params()
    ): RrProcessingResult {
        if (rrMs.size < 2) {
            return RrProcessingResult(
                correctedRrMs = rrMs,
                artifacts = emptyList(),
                hrv = null,
                baevsky = null,
                quality = if (rrMs.isEmpty()) 0.0 else 0.2
            )
        }
        val correction = RrArtifactCorrection.correct(rrMs, correctionParams)
        val nn = correction.correctedRrMs
        val hrv = if (nn.size >= 2) HrvMetrics.timeDomain(nn) else null
        val baevsky = if (nn.size >= 2 && nn.min() != nn.max()) {
            BaevskyStressIndex.compute(nn)
        } else {
            null
        }
        val quality = SignalQuality.score(
            QualityInput(
                rawRrMs = rrMs,
                correctedRrMs = nn,
                artifactCount = correction.artifacts.size,
                sensorContactOk = sensorContactOk,
                packetParseSuccessRate = packetParseSuccessRate
            )
        )
        return RrProcessingResult(nn, correction.artifacts, hrv, baevsky, quality)
    }
}
