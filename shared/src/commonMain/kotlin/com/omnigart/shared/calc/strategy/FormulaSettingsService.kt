package com.omnigart.shared.calc.strategy

import com.omnigart.shared.db.OmniGartDatabase
import com.omnigart.shared.domain.BiologicalSex
import kotlin.time.Clock
import kotlin.time.Instant

data class DerivedEstimate(
    val id: String,
    val profileId: String,
    val domain: FormulaDomain,
    val formulaKey: String,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val ageYears: Int? = null,
    val biologicalSex: BiologicalSex? = null,
    val reps: Int? = null,
    val restingBpm: Double? = null,
    val maxBpm: Double? = null,
    val value: Double,
    val unit: String,
    val computedAt: Instant
)

/**
 * Loads / saves [FormulaSelection] and recalculates [derived_estimate] rows when a formula changes.
 */
class FormulaSettingsService(private val db: OmniGartDatabase) {
    fun getSelection(profileId: String): FormulaSelection {
        val row = db.omniGartQueries.selectFormulaSettings(profileId).executeAsOneOrNull()
        if (row == null) return FormulaSelection().normalize()
        return FormulaSelection(
            mode = runCatching { FormulaMode.valueOf(row.mode) }.getOrDefault(FormulaMode.SIMPLE),
            bmrKey = row.bmr_key,
            hrMaxKey = row.hr_max_key,
            bodyFatKey = row.body_fat_key,
            oneRmKey = row.one_rm_key,
            hrZonesKey = row.hr_zones_key
        ).normalize()
    }

    fun saveSelection(profileId: String, selection: FormulaSelection, now: Instant = Clock.System.now()) {
        val normalized = selection.normalize()
        // When switching to SIMPLE, force curated defaults for every domain.
        val toStore =
            if (normalized.mode == FormulaMode.SIMPLE) {
                FormulaSelection(mode = FormulaMode.SIMPLE)
            } else {
                normalized
            }
        val previous = getSelection(profileId)
        db.omniGartQueries.upsertFormulaSettings(
            profile_id = profileId,
            mode = toStore.mode.name,
            bmr_key = toStore.bmrKey,
            hr_max_key = toStore.hrMaxKey,
            body_fat_key = toStore.bodyFatKey,
            one_rm_key = toStore.oneRmKey,
            hr_zones_key = toStore.hrZonesKey,
            updated_at = now.toString()
        )
        FormulaDomain.entries.forEach { domain ->
            val oldKey = previous.keyFor(domain)
            val newKey = toStore.keyFor(domain)
            if (oldKey != newKey) {
                recalculateDomain(profileId, domain, oldKey, newKey, now)
            }
        }
    }

    fun updateDomain(profileId: String, domain: FormulaDomain, key: String, now: Instant = Clock.System.now()): FormulaSelection {
        val current = getSelection(profileId)
        val next = current.withKey(domain, key).normalize()
        // Domain edits imply advanced mode when picking a non-default.
        val withMode =
            if (next.keyFor(domain) != FormulaSelection().keyFor(domain)) {
                next.copy(mode = FormulaMode.ADVANCED)
            } else {
                next
            }
        saveSelection(profileId, withMode, now)
        return getSelection(profileId)
    }

    fun setMode(profileId: String, mode: FormulaMode, now: Instant = Clock.System.now()): FormulaSelection {
        val current = getSelection(profileId)
        saveSelection(profileId, current.copy(mode = mode), now)
        return getSelection(profileId)
    }

    fun upsertEstimate(estimate: DerivedEstimate) {
        db.omniGartQueries.upsertDerivedEstimate(
            id = estimate.id,
            profile_id = estimate.profileId,
            domain = estimate.domain.name,
            formula_key = estimate.formulaKey,
            weight_kg = estimate.weightKg,
            height_cm = estimate.heightCm,
            age_years = estimate.ageYears?.toLong(),
            biological_sex = estimate.biologicalSex?.name,
            reps = estimate.reps?.toLong(),
            resting_bpm = estimate.restingBpm,
            max_bpm = estimate.maxBpm,
            estimate_value = estimate.value,
            unit = estimate.unit,
            computed_at = estimate.computedAt.toString()
        )
    }

    fun estimates(profileId: String, domain: FormulaDomain? = null): List<DerivedEstimate> {
        val rows =
            if (domain == null) {
                db.omniGartQueries.selectEstimatesForProfile(profileId).executeAsList()
            } else {
                db.omniGartQueries.selectEstimatesForProfileDomain(profileId, domain.name).executeAsList()
            }
        return rows.map { it.toDomain() }
    }

    fun audits(profileId: String): List<RecalcAuditEntry> = db.omniGartQueries.selectRecalcAuditsForProfile(profileId).executeAsList().map {
        RecalcAuditEntry(
            id = it.id,
            profileId = it.profile_id,
            domain = FormulaDomain.valueOf(it.domain),
            oldFormulaKey = it.old_formula_key,
            newFormulaKey = it.new_formula_key,
            rowsTouched = it.rows_touched.toInt(),
            atIso = it.at
        )
    }

    /**
     * Recomputes every stored estimate for [domain] with [newKey].
     * Idempotent: repeating with the same key leaves values unchanged and still audits 0 rows
     * only when [oldKey] differs (callers skip when equal).
     */
    fun recalculateDomain(
        profileId: String,
        domain: FormulaDomain,
        oldKey: String,
        newKey: String,
        now: Instant = Clock.System.now()
    ): Int {
        val prefs = FormulaCatalog.preferencesFromKeys(
            bmrKey = if (domain == FormulaDomain.BMR) newKey else FormulaCatalog.DEFAULT_BMR,
            hrMaxKey = if (domain == FormulaDomain.HR_MAX) newKey else FormulaCatalog.DEFAULT_HR_MAX,
            bodyFatKey = if (domain == FormulaDomain.BODY_FAT) newKey else FormulaCatalog.DEFAULT_BODY_FAT,
            oneRepMaxKey = if (domain == FormulaDomain.ONE_RM) newKey else FormulaCatalog.DEFAULT_ONE_RM,
            hrZonesKey = if (domain == FormulaDomain.HR_ZONES) newKey else FormulaCatalog.DEFAULT_HR_ZONES
        )
        val rows = db.omniGartQueries.selectEstimatesForProfileDomain(profileId, domain.name).executeAsList()
        var touched = 0
        rows.forEach { row ->
            val newValue = recompute(row.toDomain(), domain, prefs) ?: return@forEach
            db.omniGartQueries.updateDerivedEstimateValue(
                formula_key = newKey,
                estimate_value = newValue,
                computed_at = now.toString(),
                id = row.id
            )
            touched += 1
        }
        db.omniGartQueries.insertRecalcAudit(
            id = "audit-$profileId-${domain.name}-$now",
            profile_id = profileId,
            domain = domain.name,
            old_formula_key = oldKey,
            new_formula_key = newKey,
            rows_touched = touched.toLong(),
            at = now.toString()
        )
        return touched
    }

    private fun recompute(estimate: DerivedEstimate, domain: FormulaDomain, prefs: FormulaPreferences): Double? = when (domain) {
        FormulaDomain.BMR -> {
            val w = estimate.weightKg ?: return null
            val h = estimate.heightCm ?: return null
            val age = estimate.ageYears ?: return null
            val sex = estimate.biologicalSex ?: return null
            prefs.bmr.bmrKcal(w, h, age, sex)
        }
        FormulaDomain.HR_MAX -> {
            val age = estimate.ageYears ?: return null
            prefs.hrMax.hrMax(age)
        }
        FormulaDomain.BODY_FAT -> {
            val w = estimate.weightKg ?: return null
            val h = estimate.heightCm ?: return null
            val age = estimate.ageYears ?: return null
            val sex = estimate.biologicalSex ?: return null
            prefs.bodyFat.percent(w, h, age, sex)
        }
        FormulaDomain.ONE_RM -> {
            val w = estimate.weightKg ?: return null
            val reps = estimate.reps ?: return null
            prefs.oneRepMax.e1rm(w, reps)
        }
        FormulaDomain.HR_ZONES -> {
            // Stored value is zone-1 lower bound for auditability of zone scheme changes.
            val rest = estimate.restingBpm ?: return null
            val max = estimate.maxBpm ?: return null
            prefs.hrZones.zones(rest, max).firstOrNull()?.lowerBpm
        }
    }
}

private fun com.omnigart.shared.db.Derived_estimate.toDomain(): DerivedEstimate = DerivedEstimate(
    id = id,
    profileId = profile_id,
    domain = FormulaDomain.valueOf(domain),
    formulaKey = formula_key,
    weightKg = weight_kg,
    heightCm = height_cm,
    ageYears = age_years?.toInt(),
    biologicalSex = biological_sex?.let { BiologicalSex.valueOf(it) },
    reps = reps?.toInt(),
    restingBpm = resting_bpm,
    maxBpm = max_bpm,
    value = estimate_value,
    unit = unit,
    computedAt = Instant.parse(computed_at)
)
