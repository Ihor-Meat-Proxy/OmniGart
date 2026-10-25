package com.omnigart.shared.health.store

import com.omnigart.shared.health.store.device.PhoneEquipmentAdjustment
import com.omnigart.shared.health.store.device.PhonePlannedTargets
import com.omnigart.shared.health.store.device.PhoneWorkoutKindIds
import com.omnigart.shared.health.store.device.PhoneWorkoutSelection
import com.omnigart.shared.health.store.device.PhoneWorkoutSelectionCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneWorkoutSelectionTest {
    private fun sample(
        kind: String = PhoneWorkoutKindIds.ZONE2_RUN,
        programId: String? = null,
        routineId: String? = "routine-zone2-w1",
        equipmentIds: List<String> = emptyList(),
        adjustments: List<PhoneEquipmentAdjustment> = emptyList(),
        targets: PhonePlannedTargets? =
            PhonePlannedTargets(
                durationSeconds = 2_400,
                distanceMetres = 5_000.0,
                zoneLowerBpm = 120,
                zoneUpperBpm = 140
            )
    ): PhoneWorkoutSelection = PhoneWorkoutSelection(
        kind = kind,
        programId = programId,
        routineId = routineId,
        dayIndex = if (programId != null) 0 else null,
        equipmentIds = equipmentIds,
        equipmentAdjustments = adjustments,
        plannedTargets = targets,
        sessionKey = "session-abc",
        watchIsRecorder = true
    )

    @Test
    fun codec_roundTripsFullSelection() {
        val original =
            sample(
                kind = PhoneWorkoutKindIds.GUIDED_STRENGTH,
                programId = "strength-full-body",
                routineId = "routine-a",
                equipmentIds = listOf("barbell", "bench", "dumbbell"),
                adjustments =
                listOf(
                    PhoneEquipmentAdjustment("band-light", enabled = true, loadMultiplier = 0.5),
                    PhoneEquipmentAdjustment("cable", enabled = false)
                ),
                targets =
                PhonePlannedTargets(
                    durationSeconds = 3_600,
                    targetSets = 4,
                    targetReps = 8,
                    targetWeightKg = 60.0
                )
            )
        val encoded = PhoneWorkoutSelectionCodec.encode(original)
        val decoded = PhoneWorkoutSelectionCodec.decode(encoded)
        assertEquals(original, decoded)
        assertEquals(PhoneWorkoutSelection.SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals(PhoneWorkoutSelection.PAYLOAD_KEY, "omnigart.workout.selection.json")
        assertEquals(PhoneWorkoutSelection.LAUNCH_PATH, "/omnigart/workout/launch")
    }

    @Test
    fun codec_roundTripsEachKnownKind() {
        for (kind in PhoneWorkoutKindIds.ALL) {
            val decoded =
                PhoneWorkoutSelectionCodec.decode(
                    PhoneWorkoutSelectionCodec.encode(sample(kind = kind, targets = null))
                )
            assertEquals(kind, decoded.kind)
        }
    }

    @Test
    fun codec_ignoresUnknownKeys() {
        val json =
            """
            {
              "schema": "omnigart.phone.workout.selection",
              "schemaVersion": 1,
              "kind": "treadmillRun",
              "futureField": true,
              "equipmentIds": ["treadmill"],
              "watchIsRecorder": true
            }
            """.trimIndent()
        val decoded = PhoneWorkoutSelectionCodec.decode(json)
        assertEquals(PhoneWorkoutKindIds.TREADMILL_RUN, decoded.kind)
        assertEquals(listOf("treadmill"), decoded.equipmentIds)
    }

    @Test
    fun decodeOrNull_returnsNullOnGarbage() {
        assertNull(PhoneWorkoutSelectionCodec.decodeOrNull("not-json"))
        assertNull(PhoneWorkoutSelectionCodec.decodeOrNull("""{"schema":"other","kind":"yoga"}"""))
    }

    @Test
    fun rejectsUnknownKind() {
        assertFailsWith<IllegalArgumentException> {
            PhoneWorkoutSelection(kind = "spinBike")
        }
    }

    @Test
    fun rejectsBadZoneOrder() {
        assertFailsWith<IllegalArgumentException> {
            PhonePlannedTargets(zoneLowerBpm = 150, zoneUpperBpm = 120)
        }
    }

    @Test
    fun platformStrengthFamily_mapsGuidedToTraditional() {
        assertEquals(
            PhoneWorkoutKindIds.TRADITIONAL_STRENGTH,
            PhoneWorkoutKindIds.platformStrengthFamily(PhoneWorkoutKindIds.GUIDED_STRENGTH)
        )
        assertEquals(
            PhoneWorkoutKindIds.FUNCTIONAL_STRENGTH,
            PhoneWorkoutKindIds.platformStrengthFamily(PhoneWorkoutKindIds.FUNCTIONAL_STRENGTH)
        )
        assertTrue(PhoneWorkoutKindIds.isKnown(PhoneWorkoutKindIds.YOGA))
        assertFalse(PhoneWorkoutKindIds.isKnown("unknown"))
    }

    @Test
    fun factory_createSimple_roundTrips() {
        val selection =
            com.omnigart.shared.health.store.device.PhoneWorkoutSelectionFactory.createSimple(
                kind = PhoneWorkoutKindIds.FUNCTIONAL_STRENGTH,
                sessionKey = "s-1"
            )
        assertEquals(PhoneWorkoutKindIds.FUNCTIONAL_STRENGTH, selection.kind)
        assertEquals("s-1", selection.sessionKey)
        assertNull(selection.plannedTargets)
        val again = PhoneWorkoutSelectionCodec.decode(PhoneWorkoutSelectionCodec.encode(selection))
        assertEquals(selection, again)
    }
}
