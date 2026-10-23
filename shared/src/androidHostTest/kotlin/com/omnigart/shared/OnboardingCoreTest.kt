package com.omnigart.shared

import com.omnigart.shared.data.UserProfileRepository
import com.omnigart.shared.data.createInMemoryDatabase
import com.omnigart.shared.domain.ActivityLevel
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.domain.ProfileGoal
import com.omnigart.shared.domain.StrengthLevel
import com.omnigart.shared.domain.UnitSystem
import com.omnigart.shared.domain.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class OnboardingCoreTest {
    @Test
    fun needsOnboarding_whenNoCompletedProfile() {
        val core = OmniGartCore.forTesting(createInMemoryDatabase())
        assertTrue(core.needsOnboarding())
    }

    @Test
    fun completeOnboardingAnswers_marksCompleteAndRemovesDemo() {
        val core = OmniGartCore.forTesting(createInMemoryDatabase())
        core.profiles.upsert(
            UserProfile(
                id = OmniGartCore.DEMO_PROFILE_ID,
                biologicalSex = BiologicalSex.MALE,
                heightCm = 180.0,
                weightKg = 80.0,
                dateOfBirth = LocalDate(1990, 1, 1),
                onboardingCompleted = false
            )
        )

        val completed =
            core.completeOnboardingAnswers(
                biologicalSex = BiologicalSex.FEMALE,
                heightCm = 168.0,
                weightKg = 62.0,
                birthYear = 1994,
                birthMonth = 6,
                birthDay = 15,
                preferredUnits = UnitSystem.METRIC,
                primaryGoal = ProfileGoal.GENERAL_FITNESS,
                strengthLevel = StrengthLevel.BEGINNER,
                activityLevel = ActivityLevel.MODERATE
            )

        assertEquals(OmniGartCore.LOCAL_PROFILE_ID, completed.id)
        assertTrue(completed.onboardingCompleted)
        assertFalse(core.needsOnboarding())
        assertNull(core.profiles.get(OmniGartCore.DEMO_PROFILE_ID))
        assertEquals(completed, core.activeProfile())
    }

    @Test
    fun ensureUiTestProfile_skipsOnboarding() {
        val core = OmniGartCore.forTesting(createInMemoryDatabase())
        val seeded = core.ensureUiTestProfile()
        assertTrue(seeded.onboardingCompleted)
        assertFalse(core.needsOnboarding())
    }

    @Test
    fun profileRepository_roundTripsOnboardingColumns() {
        val db = createInMemoryDatabase()
        val profiles = UserProfileRepository(db)
        val profile =
            UserProfile(
                id = "local-profile",
                biologicalSex = BiologicalSex.MALE,
                heightCm = 182.0,
                weightKg = 84.0,
                dateOfBirth = LocalDate(1988, 11, 2),
                preferredUnits = UnitSystem.IMPERIAL,
                primaryGoal = ProfileGoal.ENDURANCE,
                strengthLevel = StrengthLevel.ADVANCED,
                activityLevel = ActivityLevel.VERY_ACTIVE,
                onboardingCompleted = true
            )
        profiles.upsert(profile)
        assertEquals(profile, profiles.get("local-profile"))
    }

    @Test
    fun foodLog_totalsForToday() {
        val core = OmniGartCore.forTesting(createInMemoryDatabase())
        val profile = core.ensureUiTestProfile()
        core.logFood(profile.id, "Oats", 350.0, 12.0, 55.0, 8.0)
        core.logFood(profile.id, "Yogurt", 120.0, 10.0, 12.0, 3.0)
        val totals = core.foodDayTotals(profile.id)
        assertEquals(470.0, totals.energyKcal)
        assertEquals(22.0, totals.proteinG)
        assertEquals(2, totals.entryCount)
        assertEquals(2, core.foodEntriesForToday(profile.id).size)
    }
}
