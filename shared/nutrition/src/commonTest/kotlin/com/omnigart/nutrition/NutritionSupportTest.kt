package com.omnigart.nutrition

import com.omnigart.nutrition.body.InMemoryBodyMeasurementStore
import com.omnigart.nutrition.body.LocalMeasurementEntry
import com.omnigart.nutrition.body.LocalWeightEntry
import com.omnigart.nutrition.body.MeasurementSite
import com.omnigart.nutrition.body.WeighInSyncIds
import com.omnigart.nutrition.checkin.WeeklyCheckIn
import com.omnigart.nutrition.health.FakeNutritionHealth
import com.omnigart.nutrition.health.NutritionWeighInPolicy
import com.omnigart.nutrition.macros.MacroTargets
import com.omnigart.nutrition.reminders.NutritionReminders
import com.omnigart.nutrition.tdee.AdaptiveTdee
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class NutritionSupportTest {

    @Test
    fun bodyStore_appendOnlyWeight() {
        val store = InMemoryBodyMeasurementStore()
        val a =
            LocalWeightEntry(
                id = "w1",
                epochMs = 1L,
                massKg = 70.0,
                syncIdentifier = WeighInSyncIds.syncIdentifier("w1"),
            )
        store.appendWeight(a)
        store.appendWeight(a.copy(massKg = 71.0))
        assertEquals(1, store.listWeights().size)
        assertEquals(70.0, store.listWeights().first().massKg)
    }

    @Test
    fun waistToHeight_notRequiredForEnergy_butDisplayed() {
        val store = InMemoryBodyMeasurementStore()
        store.appendWeight(
            LocalWeightEntry("w1", 1L, 70.0, WeighInSyncIds.syncIdentifier("w1")),
        )
        store.appendMeasurement(
            LocalMeasurementEntry("m1", 1L, MeasurementSite.WAIST, 80.0),
        )
        val snap = store.latestSnapshot(heightCm = 170.0)
        assertNotNull(snap.waistToHeightRatio)
        val ratio = requireNotNull(snap.waistToHeightRatio)
        assertEquals(80.0 / 170.0, ratio, absoluteTolerance = 0.001)
    }

    @Test
    fun weighInWriter_skipsExistingSyncId() =
        runTest {
            val fake = FakeNutritionHealth()
            val entry =
                LocalWeightEntry(
                    id = "w1",
                    epochMs = 1L,
                    massKg = 70.0,
                    syncIdentifier = WeighInSyncIds.syncIdentifier("w1"),
                )
            val first = fake.appendIfAbsent(entry)
            assertTrue(first.ok)
            assertFalse(first.skippedAlreadyPresent)
            val second = fake.appendIfAbsent(entry)
            assertTrue(second.skippedAlreadyPresent)
            assertEquals(1, fake.written.size)
        }

    @Test
    fun prepareForPlatform_nullWhenAlreadyLinked() {
        val linked =
            LocalWeightEntry(
                id = "w1",
                epochMs = 1L,
                massKg = 70.0,
                syncIdentifier = WeighInSyncIds.syncIdentifier("w1"),
                platformLinked = true,
            )
        assertNull(NutritionWeighInPolicy.prepareForPlatform(linked))
    }

    @Test
    fun reminders_weighInAfter3Days() {
        val due =
            NutritionReminders.weighInNudge(
                daysSinceLastWeighIn = 3,
                config = NutritionReminders.ReminderConfig(),
            )
        assertTrue(due.shouldNotify)
        val paused =
            NutritionReminders.weighInNudge(
                10,
                NutritionReminders.ReminderConfig(paused = true),
            )
        assertFalse(paused.shouldNotify)
    }

    @Test
    fun weeklyCheckIn_skippableSections() {
        val state =
            WeeklyCheckIn.build(
                weekday = 1,
                incompleteDayKeys = listOf("2026-01-02"),
                needsWeighIn = true,
                ambiguousUnloggedDayKeys = listOf("2026-01-03"),
                proposedChange =
                    WeeklyCheckIn.ProposedTargetChange(2000.0, 2050.0, "nutrition_reason_adaptive_ewma"),
            )
        assertTrue(state.sections.contains(WeeklyCheckIn.Section.PROPOSED_TARGET_CHANGE))
        val decision =
            WeeklyCheckIn.applyUserChoices(
                state = state,
                acceptProposedTarget = true,
                markFastingDayKeys = listOf("2026-01-03"),
                skipSections = setOf(WeeklyCheckIn.Section.INCOMPLETE_DAYS),
            )
        assertEquals(2050.0, decision.acceptedTargetKcal)
        assertTrue(decision.remainingIncomplete.isEmpty())
    }

    @Test
    fun macros_carbsAtLeast130() {
        val macros =
            MacroTargets.compute(
                energyKcal = 1200.0,
                weightKg = 80.0,
                heightCm = 180.0,
                goalKind = NutritionGoalKind.LOSE,
            )
        assertTrue(macros.carbsG >= 130.0)
        assertTrue(macros.fibreG >= 25.0)
    }

    @Test
    fun energyBalance21_insufficientWeights_notEligible() {
        val result =
            AdaptiveTdee.energyBalance21(
                intakeDays = listOf(IntakeDay("a", 2000.0, DayLogStatus.COMPLETE)),
                weights = listOf(WeightPoint(1L, 70.0)),
            )
        assertNotNull(result)
        assertFalse(result.eligible)
    }
}
