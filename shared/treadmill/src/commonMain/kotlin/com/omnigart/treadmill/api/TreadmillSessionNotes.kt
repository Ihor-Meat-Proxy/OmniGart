package com.omnigart.treadmill.api

import com.omnigart.treadmill.model.TreadmillProtocol
import com.omnigart.treadmill.model.TreadmillWorkoutSummary

/**
 * Compact notes marker stored on [LocalHealthWorkout.notes] so exports and
 * Health Connect writers can detect treadmill sessions without a schema migration.
 *
 * Format: `omnigart.treadmill|proto=FTMS|dist=1234.5|spd=10.2|inc=1.5|est=0|cal=`
 */
object TreadmillSessionNotes {
    const val PREFIX = "omnigart.treadmill"

    fun encode(summary: TreadmillWorkoutSummary): String {
        val parts =
            listOf(
                PREFIX,
                "proto=${summary.protocol.name}",
                "dist=${summary.distanceMetres?.toString().orEmpty()}",
                "spd=${summary.averageSpeedKmh?.toString().orEmpty()}",
                "inc=${summary.averageInclinePercent?.toString().orEmpty()}",
                "mhr=${summary.machineAverageHeartRateBpm?.toString().orEmpty()}",
                "kcal=${summary.energyKcal?.toString().orEmpty()}",
                "steps=${summary.steps?.toString().orEmpty()}",
                "est=${if (summary.usedWatchEstimatedDistance) "1" else "0"}",
                "cal=${summary.calibrationFactor?.toString().orEmpty()}",
                "dev=${summary.deviceName.orEmpty().replace('|', '/')}"
            )
        return parts.joinToString("|")
    }

    fun isTreadmillNotes(notes: String?): Boolean =
        notes?.startsWith(PREFIX) == true

    fun protocolOf(notes: String?): TreadmillProtocol? {
        if (notes == null || !isTreadmillNotes(notes)) return null
        val proto = notes.split('|').firstOrNull { it.startsWith("proto=") }?.removePrefix("proto=")
        return proto?.let { runCatching { TreadmillProtocol.valueOf(it) }.getOrNull() }
    }

    fun parseField(notes: String?, key: String): String? {
        if (notes == null || !isTreadmillNotes(notes)) return null
        return notes.split('|').firstOrNull { it.startsWith("$key=") }?.removePrefix("$key=")
            ?.takeIf { it.isNotEmpty() }
    }
}
