package com.omnigart.shared

import com.omnigart.shared.calc.Bmi
import com.omnigart.shared.calc.WaterTarget
import com.omnigart.shared.calc.strategy.FormulaSettingsService
import com.omnigart.shared.content.ContentStore
import com.omnigart.shared.data.DatabaseDriverFactory
import com.omnigart.shared.data.ExerciseRepository
import com.omnigart.shared.data.FoodEntryRepository
import com.omnigart.shared.data.SessionRepository
import com.omnigart.shared.data.UserProfileRepository
import com.omnigart.shared.data.WorkoutRepository
import com.omnigart.shared.data.createDatabase
import com.omnigart.shared.db.OmniGartDatabase
import com.omnigart.shared.domain.ActivityLevel
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.domain.FoodDayTotals
import com.omnigart.shared.domain.FoodEntry
import com.omnigart.shared.domain.ProfileGoal
import com.omnigart.shared.domain.StrengthLevel
import com.omnigart.shared.domain.UnitSystem
import com.omnigart.shared.domain.UserProfile
import com.omnigart.shared.food.FoodLoggingService
import com.omnigart.shared.health.store.HealthSyncEngine
import com.omnigart.shared.health.store.LocalHealthBridge
import com.omnigart.shared.health.store.LocalHealthStore
import com.omnigart.shared.health.store.SqlDelightLocalHealthStore
import com.omnigart.shared.health.store.export.HealthExportService
import com.omnigart.shared.logging.StrengthLoggingService
import com.omnigart.shared.strength.EquipmentProfileService
import com.omnigart.shared.strength.StrengthProgramService
import com.omnigart.shared.util.formatOneDecimal
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn

/**
 * Facade used by thin native UI shells.
 *
 * [contentStore] is the offline content pack (Android/Wear assets). When null, strength
 * logging falls back to the built-in demo exercise catalog.
 */
class OmniGartCore private constructor(database: OmniGartDatabase, contentStore: ContentStore?) {
    constructor(driverFactory: DatabaseDriverFactory, contentStore: ContentStore?) :
        this(createDatabase(driverFactory), contentStore)

    /** Swift-friendly overload (Kotlin default args are not exported to Apple). */
    constructor(driverFactory: DatabaseDriverFactory) : this(driverFactory, contentStore = null)

    val profiles = UserProfileRepository(database)
    val foodEntries = FoodEntryRepository(database)
    val exercises = ExerciseRepository(database)
    val workouts = WorkoutRepository(database)
    val sessions = SessionRepository(database)
    val formulaSettings = FormulaSettingsService(database)

    /** Food diary, catalogue, gaps, import/export, optional morning review. */
    val foodLogging = FoodLoggingService()

    /**
     * Swift-friendly food day duplicate (epoch millis).
     *
     * Named without a `copy*` prefix: ObjC/Swift treats `copy…` as NSCopying and
     * drops or remaps those Kotlin/Native exports, so Swift never sees them.
     */
    fun duplicateFoodDayMs(profileId: String, fromStartEpochMs: Long, fromEndEpochMs: Long, toStartEpochMs: Long): Int =
        foodLogging.duplicateDayByEpochMs(profileId, fromStartEpochMs, fromEndEpochMs, toStartEpochMs)

    val strengthLogging =
        StrengthLoggingService(
            sessions = sessions,
            exercises = exercises,
            profiles = profiles,
            formulaSettings = formulaSettings,
            contentStore = contentStore
        )
    val strengthPrograms =
        StrengthProgramService(
            equipmentService = EquipmentProfileService(database),
            profiles = profiles
        )

    /** Local-first health store (SQLDelight). Always available without platform permission. */
    val localHealthStore: LocalHealthStore = SqlDelightLocalHealthStore(database)
    val healthSync: HealthSyncEngine = HealthSyncEngine(localHealthStore)
    val healthExport: HealthExportService = HealthExportService(localHealthStore)
    val localHealth: LocalHealthBridge = LocalHealthBridge(localHealthStore, healthSync, healthExport)

    /** True when a completed local profile exists (first-launch onboarding finished). */
    fun needsOnboarding(): Boolean {
        val local = profiles.get(LOCAL_PROFILE_ID)
        if (local?.onboardingCompleted == true) return false
        // Legacy demo profile without onboarding completion still needs the flow.
        val demo = profiles.get(DEMO_PROFILE_ID)
        return demo?.onboardingCompleted != true && local?.onboardingCompleted != true
    }

    /**
     * Active profile for the shell. Prefers the completed local profile; otherwise returns
     * an in-progress draft (or creates one). Does not mark onboarding complete.
     */
    fun activeProfile(): UserProfile {
        profiles.get(LOCAL_PROFILE_ID)?.let { return it }
        profiles.get(DEMO_PROFILE_ID)?.takeIf { it.onboardingCompleted }?.let { return it }
        return ensureDraftProfile()
    }

    /** Draft profile used while onboarding is incomplete. */
    fun ensureDraftProfile(): UserProfile {
        val existing = profiles.get(LOCAL_PROFILE_ID)
        if (existing != null) return existing
        val draft =
            UserProfile(
                id = LOCAL_PROFILE_ID,
                biologicalSex = BiologicalSex.FEMALE,
                heightCm = 168.0,
                weightKg = 62.0,
                dateOfBirth = LocalDate(1994, 6, 15),
                preferredUnits = UnitSystem.METRIC,
                primaryGoal = ProfileGoal.GENERAL_FITNESS,
                strengthLevel = StrengthLevel.BEGINNER,
                activityLevel = ActivityLevel.MODERATE,
                onboardingCompleted = false
            )
        profiles.upsert(draft)
        return draft
    }

    /**
     * Persist onboarding answers and mark the local profile complete.
     * Replaces any prior demo profile as the active identity.
     */
    fun completeOnboarding(profile: UserProfile): UserProfile {
        val completed = profile.copy(id = LOCAL_PROFILE_ID, onboardingCompleted = true)
        profiles.upsert(completed)
        if (profiles.get(DEMO_PROFILE_ID) != null) {
            profiles.delete(DEMO_PROFILE_ID)
        }
        return completed
    }

    /**
     * Swift-friendly onboarding save (avoids exporting kotlinx-datetime constructors).
     * [birthYear]/[birthMonth]/[birthDay] form the date of birth.
     */
    fun completeOnboardingAnswers(
        biologicalSex: BiologicalSex,
        heightCm: Double,
        weightKg: Double,
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int,
        preferredUnits: UnitSystem,
        primaryGoal: ProfileGoal,
        strengthLevel: StrengthLevel,
        activityLevel: ActivityLevel
    ): UserProfile {
        val profile =
            UserProfile(
                id = LOCAL_PROFILE_ID,
                biologicalSex = biologicalSex,
                heightCm = heightCm,
                weightKg = weightKg,
                dateOfBirth = LocalDate(birthYear, birthMonth, birthDay),
                preferredUnits = preferredUnits,
                primaryGoal = primaryGoal,
                strengthLevel = strengthLevel,
                activityLevel = activityLevel,
                onboardingCompleted = false
            )
        return completeOnboarding(profile)
    }

    /** UI-test helper: seed a completed profile without showing onboarding. */
    fun ensureUiTestProfile(): UserProfile {
        val seeded =
            UserProfile(
                id = LOCAL_PROFILE_ID,
                biologicalSex = BiologicalSex.FEMALE,
                heightCm = 168.0,
                weightKg = 62.0,
                dateOfBirth = LocalDate(1994, 6, 15),
                preferredUnits = UnitSystem.METRIC,
                waistCm = 74.0,
                hipCm = 98.0,
                primaryGoal = ProfileGoal.GENERAL_FITNESS,
                strengthLevel = StrengthLevel.BEGINNER,
                activityLevel = ActivityLevel.MODERATE,
                onboardingCompleted = true
            )
        profiles.upsert(seeded)
        return seeded
    }

    @Deprecated("Use activeProfile() / completeOnboarding()", ReplaceWith("activeProfile()"))
    fun ensureDemoProfile(): UserProfile = activeProfile()

    /** Persist profile edits from the Profile tab (post-onboarding). */
    fun updateProfile(profile: UserProfile): UserProfile {
        val saved = profile.copy(id = LOCAL_PROFILE_ID, onboardingCompleted = true)
        profiles.upsert(saved)
        return saved
    }

    fun logFood(
        profileId: String,
        name: String,
        energyKcal: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        loggedAt: Instant = Clock.System.now()
    ): FoodEntry {
        val entry =
            FoodEntry(
                id = "food-${loggedAt.toEpochMilliseconds()}",
                profileId = profileId,
                name = name.trim(),
                energyKcal = energyKcal,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                loggedAt = loggedAt
            )
        foodEntries.upsert(entry)
        return entry
    }

    /** Swift-friendly overload (no default Instant). */
    fun logFood(profileId: String, name: String, energyKcal: Double, proteinG: Double, carbsG: Double, fatG: Double): FoodEntry =
        logFood(profileId, name, energyKcal, proteinG, carbsG, fatG, Clock.System.now())

    fun foodEntriesForToday(profileId: String, timeZone: TimeZone): List<FoodEntry> {
        val today = Clock.System.todayIn(timeZone)
        val start = today.atStartOfDayIn(timeZone)
        val end = start + 1.days
        return foodEntries.forProfileDay(profileId, start, end)
    }

    /** Swift-friendly overload (ObjC/Swift do not see Kotlin default parameters). */
    fun foodEntriesForToday(profileId: String): List<FoodEntry> = foodEntriesForToday(profileId, TimeZone.currentSystemDefault())

    fun foodDayTotals(profileId: String): FoodDayTotals {
        val entries = foodEntriesForToday(profileId)
        return FoodDayTotals(
            energyKcal = entries.sumOf { it.energyKcal },
            proteinG = entries.sumOf { it.proteinG },
            carbsG = entries.sumOf { it.carbsG },
            fatG = entries.sumOf { it.fatG },
            entryCount = entries.size
        )
    }

    /** Swift-friendly birth-date parts (avoids kotlinx-datetime Month bridging). */
    fun birthYear(profile: UserProfile): Int = profile.dateOfBirth.year

    fun birthMonth(profile: UserProfile): Int = profile.dateOfBirth.month.ordinal + 1

    fun birthDay(profile: UserProfile): Int = profile.dateOfBirth.day

    fun ageYears(profile: UserProfile, today: LocalDate): Int {
        var age = today.year - profile.dateOfBirth.year
        val birthdayThisYear =
            LocalDate(today.year, profile.dateOfBirth.month, profile.dateOfBirth.day)
        if (today < birthdayThisYear) age -= 1
        return age.coerceAtLeast(0)
    }

    /** Swift-friendly overload (ObjC/Swift do not see Kotlin default parameters). */
    fun ageYears(profile: UserProfile): Int = ageYears(profile, Clock.System.todayIn(TimeZone.currentSystemDefault()))

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
        const val LOCAL_PROFILE_ID = "local-profile"
        const val VERSION = "0.1.0-phase1"

        /** Host-test helper: build a core against an existing SQLDelight database. */
        fun forTesting(database: OmniGartDatabase, contentStore: ContentStore? = null): OmniGartCore = OmniGartCore(database, contentStore)
    }
}
