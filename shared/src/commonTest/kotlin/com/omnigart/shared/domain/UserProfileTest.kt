package com.omnigart.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate

class UserProfileTest {
    @Test
    fun validProfile() {
        val profile =
            UserProfile(
                id = "u1",
                biologicalSex = BiologicalSex.MALE,
                heightCm = 180.0,
                weightKg = 80.0,
                dateOfBirth = LocalDate(1990, 1, 1)
            )
        assertEquals(UnitSystem.METRIC, profile.preferredUnits)
    }

    @Test
    fun rejectsNonPositiveHeight() {
        assertFailsWith<IllegalArgumentException> {
            UserProfile(
                id = "u1",
                biologicalSex = BiologicalSex.FEMALE,
                heightCm = 0.0,
                weightKg = 60.0,
                dateOfBirth = LocalDate(1990, 1, 1)
            )
        }
    }
}
