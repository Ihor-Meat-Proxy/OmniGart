package com.omnigart.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DisciplineLevelTest {
    @Test
    fun strengthLevels() {
        assertEquals(StrengthLevel.BEGINNER, DisciplineClassifier.classifyStrength(0))
        assertEquals(StrengthLevel.BEGINNER, DisciplineClassifier.classifyStrength(5))
        assertEquals(StrengthLevel.NOVICE, DisciplineClassifier.classifyStrength(6))
        assertEquals(StrengthLevel.INTERMEDIATE, DisciplineClassifier.classifyStrength(18))
        assertEquals(StrengthLevel.ADVANCED, DisciplineClassifier.classifyStrength(48))
    }

    @Test
    fun running_volumeAlone() {
        assertEquals(RunningLevel.NEWBIE, DisciplineClassifier.classifyRunning(5.0))
        assertEquals(RunningLevel.LEVEL_2, DisciplineClassifier.classifyRunning(25.0))
        assertEquals(RunningLevel.LEVEL_5, DisciplineClassifier.classifyRunning(80.0))
    }

    @Test
    fun running_takesLowerOnMismatch() {
        // Volume says L4 (55 km), easy pace says Newbie (>7.5) → Newbie
        assertEquals(
            RunningLevel.NEWBIE,
            DisciplineClassifier.classifyRunning(
                weeklyKm = 55.0,
                easyPaceMinPerKm = 8.0
            )
        )
        // Volume Newbie, 5k time Level 3 → Newbie
        assertEquals(
            RunningLevel.NEWBIE,
            DisciplineClassifier.classifyRunning(
                weeklyKm = 5.0,
                fiveKmTimeMinutes = 25.0
            )
        )
    }

    @Test
    fun swimmingLevels() {
        assertEquals(
            SwimmingLevel.NEWBIE,
            DisciplineClassifier.classifySwimming(100.0, canSwimContinuously = false)
        )
        assertEquals(
            SwimmingLevel.LEVEL_2,
            DisciplineClassifier.classifySwimming(3_000.0, canSwimContinuously = true)
        )
        assertEquals(
            SwimmingLevel.LEVEL_4,
            DisciplineClassifier.classifySwimming(12_000.0, canSwimContinuously = true)
        )
    }
}
