package com.omnigart.body

import com.omnigart.body.evidence.BodyEvidenceRegistry
import com.omnigart.body.fat.BodyFatStrategies
import com.omnigart.body.fat.BmiWaistHeightStrategy
import com.omnigart.body.fat.CunBaeStrategy
import com.omnigart.body.models.BiologicalSex
import com.omnigart.body.models.BodyFatMethodId
import com.omnigart.body.models.BodyMeasurementSnapshot
import com.omnigart.body.models.BodyMetrics
import com.omnigart.body.models.BodyProfile
import com.omnigart.body.models.CircumferenceSite
import com.omnigart.body.models.SkinfoldSite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyFatStrategiesTest {

    @Test
    fun defaultStrategyIsCunBae() {
        assertEquals(BodyFatMethodId.CUN_BAE, BodyFatStrategies.DEFAULT.id)
        assertTrue(BodyFatStrategies.DEFAULT === CunBaeStrategy)
    }

    @Test
    fun cunBaeProducesPlausiblePercentForAdultFemale() {
        val snap =
            BodyMeasurementSnapshot(
                profile = BodyProfile(BiologicalSex.FEMALE, ageYears = 35, heightCm = 165.0),
                weightKg = 65.0,
            )
        val result = CunBaeStrategy.estimate(snap)
        assertTrue(result.ok)
        val bf = assertNotNull(result.bodyFatPercent)
        assertTrue(bf in 15.0..45.0, "unexpected bf=$bf")
        assertEquals(BodyMetrics.bmi(65.0, 165.0), result.bodyMassIndex!!, absoluteTolerance = 1e-9)
        assertTrue(result.evidence.any { it.refId.contains("10.2337/dc11-1334") })
    }

    @Test
    fun usNavyMaleRequiresAbdomenAndNeck() {
        val snap =
            BodyMeasurementSnapshot(
                profile = BodyProfile(BiologicalSex.MALE, ageYears = 30, heightCm = 180.0),
                circumferencesCm =
                    mapOf(
                        CircumferenceSite.NECK to 38.0,
                        CircumferenceSite.ABDOMEN to 90.0,
                    ),
            )
        val result = BodyFatStrategies.byId(BodyFatMethodId.US_NAVY).estimate(snap)
        assertTrue(result.ok)
        assertNotNull(result.bodyFatPercent)
    }

    @Test
    fun jacksonPollock3Female() {
        val snap =
            BodyMeasurementSnapshot(
                profile = BodyProfile(BiologicalSex.FEMALE, ageYears = 28, heightCm = 168.0),
                skinfoldsMm =
                    mapOf(
                        SkinfoldSite.TRICEPS to 18.0,
                        SkinfoldSite.SUPRAILIAC to 15.0,
                        SkinfoldSite.THIGH to 22.0,
                    ),
            )
        val result = BodyFatStrategies.byId(BodyFatMethodId.JACKSON_POLLOCK_3).estimate(snap)
        assertTrue(result.ok)
        val bf = assertNotNull(result.bodyFatPercent)
        assertTrue(bf in 10.0..40.0, "unexpected bf=$bf")
    }

    @Test
    fun jacksonPollock7Male() {
        val sites =
            mapOf(
                SkinfoldSite.CHEST to 10.0,
                SkinfoldSite.MIDAXILLARY to 12.0,
                SkinfoldSite.TRICEPS to 8.0,
                SkinfoldSite.SUBSCAPULAR to 14.0,
                SkinfoldSite.ABDOMEN to 20.0,
                SkinfoldSite.SUPRAILIAC to 15.0,
                SkinfoldSite.THIGH to 12.0,
            )
        val snap =
            BodyMeasurementSnapshot(
                profile = BodyProfile(BiologicalSex.MALE, ageYears = 40, heightCm = 178.0),
                skinfoldsMm = sites,
            )
        val result = BodyFatStrategies.byId(BodyFatMethodId.JACKSON_POLLOCK_7).estimate(snap)
        assertTrue(result.ok)
        assertNotNull(result.bodyFatPercent)
    }

    @Test
    fun bmiWaistHeightUsesWellnessFramingWithoutPercent() {
        val snap =
            BodyMeasurementSnapshot(
                profile = BodyProfile(BiologicalSex.UNSPECIFIED, ageYears = 40, heightCm = 170.0),
                weightKg = 70.0,
                circumferencesCm = mapOf(CircumferenceSite.WAIST to 80.0),
            )
        val result = BmiWaistHeightStrategy.estimate(snap)
        assertTrue(result.ok)
        assertNull(result.bodyFatPercent)
        assertNotNull(result.bodyMassIndex)
        assertEquals(80.0 / 170.0, result.waistToHeightRatio!!, absoluteTolerance = 1e-9)
        assertTrue(result.notesEnGb.any { it.contains("not diagnoses", ignoreCase = true) })
        val band = assertNotNull(result.wellnessBandKey)
        assertTrue(band.contains("body.wellness.bmi_mid"))
        assertTrue(band.contains("whtr_below_half"))
    }

    @Test
    fun evidenceRegistryCoversEveryMethod() {
        for (id in BodyFatMethodId.entries) {
            assertTrue(BodyEvidenceRegistry.forMethod(id).isNotEmpty(), "missing evidence for $id")
        }
    }
}
