package com.omnigart.shared.strength

import com.omnigart.shared.data.UserProfileRepository
import com.omnigart.shared.domain.ProfileGoal
import com.omnigart.shared.domain.StrengthLevel
import com.omnigart.shared.domain.UserProfile

/**
 * Facade for equipment + strength programme generation used by thin native shells.
 */
class StrengthProgramService(
    private val equipmentService: EquipmentProfileService,
    private val profiles: UserProfileRepository,
    private val generator: StrengthProgramGenerator = StrengthProgramGenerator()
) {
    fun equipment(profileId: String): UserEquipmentProfile = equipmentService.getEquipment(profileId)

    fun saveEquipment(profile: UserEquipmentProfile): UserEquipmentProfile = equipmentService.saveEquipment(profile)

    fun applyPreset(profileId: String, kind: EquipmentPresetKind): UserEquipmentProfile = equipmentService.applyPreset(profileId, kind)

    fun saveNamedPreset(profileId: String, label: String, kind: EquipmentPresetKind): UserEquipmentProfile =
        equipmentService.saveNamedPreset(profileId, label, kind)

    fun toggleEquipment(profileId: String, equipmentId: String, enabled: Boolean): UserEquipmentProfile =
        equipmentService.toggleEquipment(profileId, equipmentId, enabled)

    fun setDumbbellRange(profileId: String, minKg: Double, maxKg: Double): UserEquipmentProfile =
        equipmentService.setDumbbellRange(profileId, minKg, maxKg)

    fun catalogueIds(): List<String> = equipmentService.catalogueIds()

    fun programPrefs(profileId: String): StrengthProgramPreferences = equipmentService.getProgramPrefs(profileId)

    fun saveProgramPrefs(prefs: StrengthProgramPreferences): StrengthProgramPreferences = equipmentService.saveProgramPrefs(prefs)

    fun setSplit(profileId: String, split: ProgramSplit, daysPerWeek: Int): StrengthProgramPreferences {
        val allowed = generator.allowedDays(split)
        val days = daysPerWeek.coerceIn(allowed.first, allowed.last)
        val current = equipmentService.getProgramPrefs(profileId)
        return equipmentService.saveProgramPrefs(current.copy(split = split, daysPerWeek = days))
    }

    fun advanceWeek(profileId: String): StrengthProgramPreferences {
        val current = equipmentService.getProgramPrefs(profileId)
        return equipmentService.saveProgramPrefs(current.copy(weekIndex = current.weekIndex + 1))
    }

    fun generateProgram(profileId: String, sessionEquipmentIds: List<String>? = null): GeneratedStrengthProgram {
        val profile = profiles.get(profileId) ?: defaultProfile(profileId)
        val equipment = equipmentService.getEquipment(profileId)
        val prefs = equipmentService.getProgramPrefs(profileId)
        return generator.generate(
            StrengthProgramRequest(
                equipment = equipment,
                split = prefs.split,
                daysPerWeek = prefs.daysPerWeek,
                goal = profile.primaryGoal,
                level = profile.strengthLevel,
                bodyMassKg = profile.weightKg,
                weekIndex = prefs.weekIndex,
                sessionEquipmentIds = sessionEquipmentIds?.toSet()
            )
        )
    }

    /** Swift-friendly overload (no nullable List default). */
    fun generateProgram(profileId: String): GeneratedStrengthProgram = generateProgram(profileId, sessionEquipmentIds = null)

    fun regenerateSession(profileId: String, dayIndex: Int, sessionEquipmentIds: List<String>): GeneratedStrengthSession {
        val program = generateProgram(profileId)
        val profile = profiles.get(profileId) ?: defaultProfile(profileId)
        val equipment = equipmentService.getEquipment(profileId)
        val prefs = equipmentService.getProgramPrefs(profileId)
        return generator.regenerateSession(
            program = program,
            dayIndex = dayIndex,
            request =
            StrengthProgramRequest(
                equipment = equipment,
                split = prefs.split,
                daysPerWeek = prefs.daysPerWeek,
                goal = profile.primaryGoal,
                level = profile.strengthLevel,
                bodyMassKg = profile.weightKg,
                weekIndex = prefs.weekIndex,
                sessionEquipmentIds = sessionEquipmentIds.toSet()
            )
        )
    }

    fun allowedDays(split: ProgramSplit): List<Int> {
        val range = generator.allowedDays(split)
        return (range.first..range.last).toList()
    }

    /** Ready-made workouts matching the profile's (or session) equipment. */
    fun readyMadeWorkouts(profileId: String, sessionEquipmentIds: List<String>? = null): List<ReadyMadeWorkout> {
        val equipment = equipmentService.getEquipment(profileId)
        val effective =
            if (sessionEquipmentIds == null) {
                equipment
            } else {
                equipment.withSessionOverride(sessionEquipmentIds.toSet())
            }
        return ReadyMadeWorkoutCatalog.availableFor(effective)
    }

    fun readyMadeWorkouts(profileId: String): List<ReadyMadeWorkout> = readyMadeWorkouts(profileId, null)

    fun startReadyMade(profileId: String, workoutId: String, sessionEquipmentIds: List<String>? = null): GeneratedStrengthSession? {
        val workout = ReadyMadeWorkoutCatalog.byId(workoutId) ?: return null
        val profile = profiles.get(profileId) ?: defaultProfile(profileId)
        val equipment = equipmentService.getEquipment(profileId)
        val effective =
            if (sessionEquipmentIds == null) {
                equipment
            } else {
                equipment.withSessionOverride(sessionEquipmentIds.toSet())
            }
        val maxLevel =
            when (profile.strengthLevel) {
                StrengthLevel.BEGINNER, StrengthLevel.NOVICE -> "beginner"
                StrengthLevel.INTERMEDIATE -> "intermediate"
                StrengthLevel.ADVANCED -> "advanced"
            }
        return ReadyMadeWorkoutCatalog.materialize(
            workout = workout,
            equipment = effective,
            bodyMassKg = profile.weightKg,
            maxLevel = maxLevel
        )
    }

    fun startReadyMade(profileId: String, workoutId: String): GeneratedStrengthSession? = startReadyMade(profileId, workoutId, null)

    private fun defaultProfile(profileId: String): UserProfile = UserProfile(
        id = profileId,
        biologicalSex = com.omnigart.shared.domain.BiologicalSex.FEMALE,
        heightCm = 170.0,
        weightKg = 70.0,
        dateOfBirth = kotlinx.datetime.LocalDate(1990, 1, 1),
        preferredUnits = com.omnigart.shared.domain.UnitSystem.METRIC,
        primaryGoal = ProfileGoal.GENERAL_FITNESS,
        strengthLevel = StrengthLevel.BEGINNER
    )
}
