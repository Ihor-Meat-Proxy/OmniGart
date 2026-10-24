package com.omnigart.shared.strength

import com.omnigart.shared.db.OmniGartDatabase
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists [UserEquipmentProfile] and [StrengthProgramPreferences] in SQLDelight.
 */
class EquipmentProfileService(private val db: OmniGartDatabase) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun getEquipment(profileId: String): UserEquipmentProfile {
        val row = db.omniGartQueries.selectEquipmentProfile(profileId).executeAsOneOrNull()
        if (row == null) return UserEquipmentProfile.defaultFor(profileId)
        val owned =
            row.owned_ids
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        val presets =
            runCatching {
                json.decodeFromString<List<SavedPresetDto>>(row.saved_presets_json)
            }.getOrDefault(emptyList())
                .map {
                    SavedEquipmentPreset(
                        label = it.label,
                        kind = runCatching { EquipmentPresetKind.valueOf(it.kind) }.getOrDefault(EquipmentPresetKind.CUSTOM),
                        ownedIds = it.ownedIds.toSet()
                    )
                }
        return UserEquipmentProfile(
            profileId = profileId,
            ownedIds = owned,
            activePreset =
            runCatching { EquipmentPresetKind.valueOf(row.active_preset) }
                .getOrDefault(EquipmentPresetKind.CUSTOM),
            dumbbellMinKg = row.dumbbell_min_kg,
            dumbbellMaxKg = row.dumbbell_max_kg,
            savedPresets = presets.ifEmpty { UserEquipmentProfile.defaultFor(profileId).savedPresets }
        )
    }

    fun saveEquipment(profile: UserEquipmentProfile, now: Instant = Clock.System.now()): UserEquipmentProfile {
        val presetsJson =
            json.encodeToString(
                profile.savedPresets.map {
                    SavedPresetDto(it.label, it.kind.name, it.ownedIds.sorted())
                }
            )
        db.omniGartQueries.upsertEquipmentProfile(
            profile_id = profile.profileId,
            owned_ids = profile.ownedIds.sorted().joinToString(","),
            active_preset = profile.activePreset.name,
            dumbbell_min_kg = profile.dumbbellMinKg,
            dumbbell_max_kg = profile.dumbbellMaxKg,
            saved_presets_json = presetsJson,
            updated_at = now.toString()
        )
        return getEquipment(profile.profileId)
    }

    /** Swift-friendly overload. */
    fun saveEquipment(profile: UserEquipmentProfile): UserEquipmentProfile = saveEquipment(profile, Clock.System.now())

    fun applyPreset(profileId: String, kind: EquipmentPresetKind): UserEquipmentProfile {
        val current = getEquipment(profileId)
        val owned =
            current.savedPresets.find { it.kind == kind }?.ownedIds
                ?: UserEquipmentProfile.presetOwned(kind)
        val next =
            current.copy(
                ownedIds = owned,
                activePreset = kind,
                dumbbellMinKg = if (EquipmentItem.DUMBBELL.id in owned) current.dumbbellMinKg ?: 2.0 else null,
                dumbbellMaxKg = if (EquipmentItem.DUMBBELL.id in owned) current.dumbbellMaxKg ?: 24.0 else null
            )
        return saveEquipment(next)
    }

    fun saveNamedPreset(profileId: String, label: String, kind: EquipmentPresetKind): UserEquipmentProfile {
        val current = getEquipment(profileId)
        val preset = SavedEquipmentPreset(label = label, kind = kind, ownedIds = current.ownedIds)
        val others = current.savedPresets.filterNot { it.kind == kind || it.label.equals(label, ignoreCase = true) }
        return saveEquipment(current.copy(savedPresets = others + preset, activePreset = kind))
    }

    fun toggleEquipment(profileId: String, equipmentId: String, enabled: Boolean): UserEquipmentProfile {
        val current = getEquipment(profileId)
        val owned =
            if (enabled) {
                current.ownedIds + equipmentId
            } else {
                current.ownedIds - equipmentId
            }
        return saveEquipment(
            current.copy(
                ownedIds = owned,
                activePreset = EquipmentPresetKind.CUSTOM
            )
        )
    }

    fun setDumbbellRange(profileId: String, minKg: Double, maxKg: Double): UserEquipmentProfile {
        val current = getEquipment(profileId)
        val lo = minOf(minKg, maxKg).coerceAtLeast(0.0)
        val hi = maxOf(minKg, maxKg).coerceAtLeast(lo)
        return saveEquipment(current.copy(dumbbellMinKg = lo, dumbbellMaxKg = hi))
    }

    fun getProgramPrefs(profileId: String): StrengthProgramPreferences {
        val row = db.omniGartQueries.selectStrengthProgramPrefs(profileId).executeAsOneOrNull()
        if (row == null) return StrengthProgramPreferences(profileId = profileId)
        return StrengthProgramPreferences(
            profileId = profileId,
            split = runCatching { ProgramSplit.valueOf(row.split) }.getOrDefault(ProgramSplit.FULL_BODY),
            daysPerWeek = row.days_per_week.toInt().coerceIn(2, 6),
            weekIndex = row.week_index.toInt().coerceAtLeast(1)
        )
    }

    fun saveProgramPrefs(prefs: StrengthProgramPreferences, now: Instant = Clock.System.now()): StrengthProgramPreferences {
        db.omniGartQueries.upsertStrengthProgramPrefs(
            profile_id = prefs.profileId,
            split = prefs.split.name,
            days_per_week = prefs.daysPerWeek.toLong(),
            week_index = prefs.weekIndex.toLong(),
            updated_at = now.toString()
        )
        return getProgramPrefs(prefs.profileId)
    }

    fun saveProgramPrefs(prefs: StrengthProgramPreferences): StrengthProgramPreferences = saveProgramPrefs(prefs, Clock.System.now())

    fun catalogueIds(): List<String> = EquipmentItem.selectable().map { it.id }

    @Serializable
    private data class SavedPresetDto(val label: String, val kind: String, val ownedIds: List<String>)
}
