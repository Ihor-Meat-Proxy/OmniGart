package com.omnigart.hydration

import com.omnigart.hydration.quicklog.QuickLogLogic
import com.omnigart.hydration.quicklog.ServingPreset
import com.omnigart.hydration.reminders.QuietHours
import com.omnigart.hydration.reminders.ReminderConfig
import com.omnigart.hydration.reminders.SleepWindow
import com.omnigart.hydration.reminders.SmartReminders
import com.omnigart.hydration.models.Beverage
import com.omnigart.hydration.models.BeverageCategory
import com.omnigart.hydration.models.LocalizedNameKeys
import com.omnigart.hydration.models.MacrosPer100ml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartRemindersTest {

    @Test
    fun stopsWhenTargetHit() {
        val decision =
            SmartReminders.decide(
                nowMinutesFromMidnight = 12 * 60,
                countedMl = 1600.0,
                targetMl = 1600.0,
                minutesSinceLastReminder = 200,
                sleepWindow = null,
            )
        assertFalse(decision.shouldNotify)
        assertEquals("target_reached", decision.reason)
    }

    @Test
    fun respectsQuietHours() {
        val decision =
            SmartReminders.decide(
                nowMinutesFromMidnight = 23 * 60,
                countedMl = 400.0,
                targetMl = 1600.0,
                minutesSinceLastReminder = 200,
                sleepWindow = null,
                config = ReminderConfig(quietHours = QuietHours(22 * 60, 7 * 60)),
            )
        assertFalse(decision.shouldNotify)
        assertEquals("quiet_hours", decision.reason)
    }

    @Test
    fun pausesDuringSleep() {
        val decision =
            SmartReminders.decide(
                nowMinutesFromMidnight = 3 * 60,
                countedMl = 400.0,
                targetMl = 1600.0,
                minutesSinceLastReminder = 200,
                sleepWindow = SleepWindow(0, 7 * 60),
                // Quiet hours deliberately exclude 03:00 so sleep pause is what fires.
                config = ReminderConfig(quietHours = QuietHours(22 * 60, 23 * 60)),
            )
        assertFalse(decision.shouldNotify)
        assertEquals("asleep", decision.reason)
    }

    @Test
    fun spacingBlocksUntilElapsed() {
        val decision =
            SmartReminders.decide(
                nowMinutesFromMidnight = 12 * 60,
                countedMl = 400.0,
                targetMl = 1600.0,
                minutesSinceLastReminder = 10,
                sleepWindow = null,
                config = ReminderConfig(baseSpacingMinutes = 90, quietHours = QuietHours(23 * 60, 6 * 60)),
            )
        assertFalse(decision.shouldNotify)
        assertEquals("spacing", decision.reason)
    }

    @Test
    fun dueWhenBehindAndSpacingMet() {
        val decision =
            SmartReminders.decide(
                nowMinutesFromMidnight = 15 * 60,
                countedMl = 200.0,
                targetMl = 1600.0,
                minutesSinceLastReminder = 200,
                sleepWindow = null,
                config = ReminderConfig(quietHours = QuietHours(23 * 60, 6 * 60)),
            )
        assertTrue(decision.shouldNotify)
        assertEquals("due", decision.reason)
    }

    @Test
    fun catchUpShortensSpacing() {
        val catchUp =
            SmartReminders.adaptedSpacingMinutes(
                nowMinutesFromMidnight = 15 * 60,
                countedMl = 100.0,
                targetMl = 1600.0,
                wakeMinutes = 7 * 60,
                dayEndMinutes = 22 * 60,
                config = ReminderConfig(baseSpacingMinutes = 100, catchUpSpacingFactor = 0.7),
            )
        val ahead =
            SmartReminders.adaptedSpacingMinutes(
                nowMinutesFromMidnight = 10 * 60,
                countedMl = 1400.0,
                targetMl = 1600.0,
                wakeMinutes = 7 * 60,
                dayEndMinutes = 22 * 60,
                config = ReminderConfig(baseSpacingMinutes = 100, aheadSpacingFactor = 1.3),
            )
        assertTrue(catchUp < 100)
        assertTrue(ahead > 100)
    }

    @Test
    fun quickLogPayloadsAndRecents() {
        assertEquals(250, QuickLogLogic.notificationPayloadWater250().volumeMl)
        assertEquals(QuickLogLogic.ACTION_ADD_WATER_250, QuickLogLogic.notificationPayloadWater250().type)

        val coffee =
            Beverage(
                id = "espresso",
                nameKeys = LocalizedNameKeys("beverage.espresso.name", "beverage.espresso.name"),
                category = BeverageCategory.COFFEE,
                defaultServingMl = 30,
                caffeineMgPerServing = 63.0,
                macrosPer100ml = MacrosPer100ml(9.0, 0.1, 1.7, 0.2),
                hydrationFactor = 1.0,
            )
        val coffeeAction = QuickLogLogic.notificationPayloadCoffee(coffee)
        assertEquals(QuickLogLogic.ACTION_ADD_COFFEE_DEFAULT, coffeeAction.type)
        assertEquals(30, coffeeAction.volumeMl)

        val recents =
            QuickLogLogic.recents(listOf("water", "espresso", "water", "latte", "espresso"), limit = 3)
        assertEquals(listOf("water", "espresso", "latte"), recents)

        val merged =
            QuickLogLogic.mergeQuickTargets(
                favourites = listOf("water", "filter-coffee"),
                recents = listOf("espresso", "water"),
                limit = 3,
            )
        assertEquals(listOf("water", "filter-coffee", "espresso"), merged)

        val preset = ServingPreset("p1", "water", 330, "+330 ml water")
        assertEquals(QuickLogLogic.ACTION_ADD_PRESET, QuickLogLogic.notificationPayloadPreset(preset).type)
    }
}
