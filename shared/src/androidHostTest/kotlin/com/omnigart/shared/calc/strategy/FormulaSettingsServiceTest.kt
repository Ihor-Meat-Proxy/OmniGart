package com.omnigart.shared.calc.strategy

import com.omnigart.shared.data.createInMemoryDatabase
import com.omnigart.shared.domain.BiologicalSex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class FormulaSettingsServiceTest {
    @Test
    fun unknownFormulaKeysMigrateToDefaults() {
        val selection =
            FormulaSelection(
                mode = FormulaMode.ADVANCED,
                bmrKey = "retired_bmr",
                hrMaxKey = "legacy_hr",
                bodyFatKey = "old_bf",
                oneRmKey = "gone",
                hrZonesKey = "z0"
            ).normalize()
        assertEquals(FormulaCatalog.DEFAULT_BMR, selection.bmrKey)
        assertEquals(FormulaCatalog.DEFAULT_HR_MAX, selection.hrMaxKey)
        assertEquals(FormulaCatalog.DEFAULT_BODY_FAT, selection.bodyFatKey)
        assertEquals(FormulaCatalog.DEFAULT_ONE_RM, selection.oneRmKey)
        assertEquals(FormulaCatalog.DEFAULT_HR_ZONES, selection.hrZonesKey)
    }

    @Test
    fun recalculationUpdatesStoredEstimatesAndAudits() {
        val db = createInMemoryDatabase()
        val service = FormulaSettingsService(db)
        val profileId = "p-calc"
        val now = Instant.parse("2026-10-23T12:00:00Z")

        service.upsertEstimate(
            DerivedEstimate(
                id = "est-bmr-1",
                profileId = profileId,
                domain = FormulaDomain.BMR,
                formulaKey = FormulaCatalog.DEFAULT_BMR,
                weightKg = 80.0,
                heightCm = 180.0,
                ageYears = 30,
                biologicalSex = BiologicalSex.MALE,
                value = 1780.0,
                unit = "kcal",
                computedAt = now
            )
        )

        val touched =
            service.recalculateDomain(
                profileId = profileId,
                domain = FormulaDomain.BMR,
                oldKey = FormulaCatalog.DEFAULT_BMR,
                newKey = "harris_benedict",
                now = Instant.parse("2026-10-23T12:01:00Z")
            )
        assertEquals(1, touched)
        val updated = service.estimates(profileId, FormulaDomain.BMR).single()
        assertEquals("harris_benedict", updated.formulaKey)
        assertEquals(1853.632, updated.value, absoluteTolerance = 0.01)

        val audit = service.audits(profileId).single()
        assertEquals(FormulaDomain.BMR, audit.domain)
        assertEquals(1, audit.rowsTouched)
    }

    @Test
    fun recalculationIsIdempotentForValues() {
        val db = createInMemoryDatabase()
        val service = FormulaSettingsService(db)
        val profileId = "p-idem"
        val t0 = Instant.parse("2026-10-23T12:00:00Z")
        service.upsertEstimate(
            DerivedEstimate(
                id = "est-1rm",
                profileId = profileId,
                domain = FormulaDomain.ONE_RM,
                formulaKey = "epley",
                weightKg = 100.0,
                reps = 5,
                value = 116.67,
                unit = "kg",
                computedAt = t0
            )
        )
        service.recalculateDomain(
            profileId,
            FormulaDomain.ONE_RM,
            "epley",
            "brzycki",
            Instant.parse("2026-10-23T12:01:00Z")
        )
        val first = service.estimates(profileId, FormulaDomain.ONE_RM).single().value
        service.recalculateDomain(
            profileId,
            FormulaDomain.ONE_RM,
            "brzycki",
            "brzycki",
            Instant.parse("2026-10-23T12:02:00Z")
        )
        val second = service.estimates(profileId, FormulaDomain.ONE_RM).single().value
        assertEquals(first, second, absoluteTolerance = 1e-9)
        assertEquals(112.5, second, absoluteTolerance = 0.001)
    }

    @Test
    fun simpleModeResetsToCuratedDefaults() {
        val db = createInMemoryDatabase()
        val service = FormulaSettingsService(db)
        val profileId = "p-simple"
        service.saveSelection(
            profileId,
            FormulaSelection(
                mode = FormulaMode.ADVANCED,
                bmrKey = "harris_benedict",
                hrMaxKey = "fox",
                oneRmKey = "brzycki",
                hrZonesKey = "karvonen_3"
            )
        )
        service.setMode(profileId, FormulaMode.SIMPLE)
        val selection = service.getSelection(profileId)
        assertEquals(FormulaMode.SIMPLE, selection.mode)
        assertEquals(FormulaCatalog.DEFAULT_BMR, selection.bmrKey)
        assertEquals(FormulaCatalog.DEFAULT_HR_MAX, selection.hrMaxKey)
        assertEquals(FormulaCatalog.DEFAULT_ONE_RM, selection.oneRmKey)
    }

    @Test
    fun simpleModeOptionsAreCuratedDefaultsOnly() {
        FormulaDomain.entries.forEach { domain ->
            val options = FormulaCatalog.optionsFor(domain, FormulaMode.SIMPLE)
            assertTrue(options.isNotEmpty())
            assertTrue(options.all { it.simpleDefault })
        }
        assertEquals(2, FormulaCatalog.optionsFor(FormulaDomain.BMR, FormulaMode.ADVANCED).size)
    }
}
