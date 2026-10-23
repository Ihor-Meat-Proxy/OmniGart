package com.omnigart.shared

import com.omnigart.shared.calc.Bmi
import com.omnigart.shared.calc.WaterTarget
import com.omnigart.shared.calc.strategy.FormulaSettingsService
import com.omnigart.shared.content.ContentStore
import com.omnigart.shared.data.DatabaseDriverFactory
import com.omnigart.shared.data.ExerciseRepository
import com.omnigart.shared.data.SessionRepository
import com.omnigart.shared.data.UserProfileRepository
import com.omnigart.shared.data.WorkoutRepository
import com.omnigart.shared.data.createDatabase
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.domain.UserProfile
import com.omnigart.shared.logging.StrengthLoggingService
import com.omnigart.shared.util.formatOneDecimal
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Facade used by thin native UI shells.
 *
 * [contentStore] is the offline content pack (Android/Wear assets). When null, strength
 * logging falls back to the built-in demo exercise catalog.
 */
class OmniGartCore(driverFactory: DatabaseDriverFactory, contentStore: ContentStore? = null) {
    private val database = createDatabase(driverFactory)
    val profiles = UserProfileRepository(database)
    val exercises = ExerciseRepository(database)
    val workouts = WorkoutRepository(database)
    val sessions = SessionRepository(database)
    val formulaSettings = FormulaSettingsService(database)
    val strengthLogging =
        StrengthLoggingService(
            sessions = sessions,
            exercises = exercises,
            profiles = profiles,
            formulaSettings = formulaSettings,
            contentStore = contentStore
        )

    fun ensureDemoProfile(): UserProfile {
        val existing = profiles.get(DEMO_PROFILE_ID)
        if (existing != null) return existing
        val demo =
            UserProfile(
                id = DEMO_PROFILE_ID,
                biologicalSex = BiologicalSex.FEMALE,
                heightCm = 168.0,
                weightKg = 62.0,
                dateOfBirth = LocalDate(1994, 6, 15),
                waistCm = 74.0,
                hipCm = 98.0
            )
        profiles.upsert(demo)
        return demo
    }

    fun ageYears(profile: UserProfile, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Int {
        var age = today.year - profile.dateOfBirth.year
        val birthdayThisYear =
            LocalDate(today.year, profile.dateOfBirth.month, profile.dateOfBirth.day)
        if (today < birthdayThisYear) age -= 1
        return age.coerceAtLeast(0)
    }

    fun summaryLines(profile: UserProfile): List<String> {
        val age = ageYears(profile)
        val prefs = formulaSettings.getSelection(profile.id).toPreferences()
        val bmr = prefs.bmr.bmrKcal(profile.weightKg, profile.heightCm, age, profile.biologicalSex)
        val bmi = Bmi.value(profile.weightKg, profile.heightCm)
        val hrMax = prefs.hrMax.hrMax(age)
        val water = WaterTarget.litresPerDay(profile.biologicalSex, profile.weightKg)
        return listOf(
            "OmniGart",
            "BMR ${bmr.toInt()} kcal",
            "BMI ${formatOneDecimal(bmi)} (${Bmi.whoCategory(bmi).name})",
            "HRmax ${hrMax.toInt()} bpm",
            "Water ${formatOneDecimal(water)} L"
        )
    }

    companion object {
        const val DEMO_PROFILE_ID = "demo-profile"
        const val VERSION = "0.1.0-phase1"
    }
}
