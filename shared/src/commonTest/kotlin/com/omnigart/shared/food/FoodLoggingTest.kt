package com.omnigart.shared.food

import com.omnigart.shared.food.barcode.BarcodeCheckDigit
import com.omnigart.shared.food.catalogue.BundledUsdaCatalogue
import com.omnigart.shared.food.diary.InMemoryFoodDiaryStore
import com.omnigart.shared.food.health.FakeFoodNutritionHealthWriter
import com.omnigart.shared.food.health.FoodHealthSyncIds
import com.omnigart.shared.food.importing.FoodImportExport
import com.omnigart.shared.food.label.NutritionLabelParser
import com.omnigart.shared.food.models.FoodLogEntry
import com.omnigart.shared.food.models.FoodSource
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.models.QuickMacroAdd
import com.omnigart.shared.food.nutrients.BestSources
import com.omnigart.shared.food.nutrients.BiologicalSexBand
import com.omnigart.shared.food.nutrients.NutrientGapAnalyzer
import com.omnigart.shared.food.nutrients.NutrientId
import com.omnigart.shared.food.nutrients.NutrientRankBasis
import com.omnigart.shared.food.nutrients.ReferenceIntakeResolver
import com.omnigart.shared.food.review.MorningReviewPromptBuilder
import com.omnigart.shared.food.review.MorningReviewPromptInput
import com.omnigart.shared.food.review.OnDeviceAiAvailability
import com.omnigart.shared.food.review.RuleBasedMorningReview
import com.omnigart.shared.food.targets.FoodDayKeys
import com.omnigart.shared.food.targets.NutritionDayTargets
import com.omnigart.shared.food.targets.StubNutritionTargetsReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class FoodLoggingTest {
    @Test
    fun barcodeCheckDigit_validEan13() {
        // 4006381333931 is a commonly cited valid EAN-13 pattern; use computed digit.
        val payload = "400638133393"
        val check = BarcodeCheckDigit.expectedCheckDigit(payload)
        assertTrue(BarcodeCheckDigit.isValid(payload + check))
        assertFalse(BarcodeCheckDigit.isValid(payload + ((check + 1) % 10)))
    }

    @Test
    fun labelParser_extractsMacros() {
        val text =
            """
            Nutrition information per serving
            Energy 250 kcal
            Fat 8.0 g
            Carbohydrate 25 g
            of which sugars 3.2 g
            Fibre 4 g
            Protein 20 g
            Salt 0.5 g
            """.trimIndent()
        val parsed = NutritionLabelParser.parse(text)
        assertNotNull(parsed)
        assertEquals(250.0, parsed.nutrients.energyKcal, 0.01)
        assertEquals(20.0, parsed.nutrients.proteinG, 0.01)
        assertEquals(25.0, parsed.nutrients.carbsG, 0.01)
        assertEquals(8.0, parsed.nutrients.fatG, 0.01)
        assertTrue(parsed.matchedFields.contains("energyKcal"))
    }

    @Test
    fun dayKeys_respectDayStart() {
        // 2024-01-15 03:00 UTC with day start 04:00 → still previous nutrition day.
        val epoch = Instant.parse("2024-01-15T03:00:00Z").toEpochMilliseconds()
        val key = FoodDayKeys.dayKey(epoch, dayStartMinutes = 4 * 60, utcOffsetMinutes = 0)
        assertEquals("2024-01-14", key)
        val key2 =
            FoodDayKeys.dayKey(
                Instant.parse("2024-01-15T05:00:00Z").toEpochMilliseconds(),
                dayStartMinutes = 4 * 60,
                utcOffsetMinutes = 0
            )
        assertEquals("2024-01-15", key2)
    }

    @Test
    fun diary_quickAdd_totals_and_export() {
        val svc = FoodLoggingService(store = InMemoryFoodDiaryStore())
        val at = Instant.parse("2024-06-01T12:00:00Z")
        svc.quickAdd(
            profileId = "p1",
            add = QuickMacroAdd("Oats", 350.0, 12.0, 55.0, 8.0),
            mealGroup = MealGroup.BREAKFAST,
            at = at
        )
        val start = Instant.parse("2024-06-01T00:00:00Z")
        val end = Instant.parse("2024-06-02T00:00:00Z")
        val totals = svc.totalsForDay("p1", start, end)
        assertEquals(350.0, totals.energyKcal, 0.01)
        assertEquals(1, totals.entryCount)
        val json = svc.exportJson("p1")
        assertTrue(json.contains("Oats"))
        val csv = svc.exportCsv("p1")
        assertTrue(csv.contains("Oats"))
        val imported = FoodImportExport.importCsv("p2", csv)
        assertEquals(1, imported.size)
    }

    @Test
    fun bestSources_and_gaps() {
        val foods = BundledUsdaCatalogue.demo().all()
        val ranks =
            BestSources.rank(
                foods,
                NutrientId.VITAMIN_K,
                basis = NutrientRankBasis.PER_100_KCAL,
                limit = 3
            )
        assertTrue(ranks.isNotEmpty())
        assertEquals("usda-demo-spinach", ranks.first().food.id)
        val ref = ReferenceIntakeResolver().resolve(30, BiologicalSexBand.FEMALE)
        val gaps =
            NutrientGapAnalyzer.gaps(
                consumed = mapOf(NutrientId.VITAMIN_C.raw to 10.0),
                reference = ref
            )
        assertTrue(gaps.any { it.nutrient == NutrientId.VITAMIN_C && it.gap > 0 })
    }

    @Test
    fun healthWriter_appendOnlySkipsExisting() = runTest {
        val writer = FakeFoodNutritionHealthWriter()
        val svc = FoodLoggingService(store = InMemoryFoodDiaryStore())
        val entry =
            FoodLogEntry(
                id = "e1",
                profileId = "p1",
                name = "Yogurt",
                energyKcal = 120.0,
                proteinG = 10.0,
                carbsG = 12.0,
                fatG = 3.0,
                source = FoodSource.CUSTOM,
                loggedAt = Instant.parse("2024-06-01T08:00:00Z")
            )
        svc.log(entry)
        val sample = svc.dietarySampleFor(entry)
        assertEquals(FoodHealthSyncIds.forEntry("e1"), sample.identity.syncIdentifier)
        writer.appendDietary(sample)
        assertEquals(1, writer.written.size)
        writer.appendDietary(sample)
        assertEquals(1, writer.written.size)
    }

    @Test
    fun ruleBasedMorningReview_groundsEnergy() {
        val review =
            RuleBasedMorningReview.build(
                id = "r1",
                profileId = "p1",
                dayKey = "2024-06-01",
                locale = "en-GB",
                totals =
                com.omnigart.shared.food.models.FoodLogDayTotals(
                    energyKcal = 1800.0,
                    proteinG = 70.0,
                    carbsG = 200.0,
                    fatG = 60.0,
                    entryCount = 3
                ),
                targets = NutritionDayTargets(energyKcal = 2000.0, proteinG = 50.0),
                gaps = emptyList(),
                foodNames = listOf("Oats", "Salad"),
                suppressDeficitLanguage = true,
                createdAtEpochMs = 1L
            )
        assertEquals(1800.0, review.groundedEnergyKcal)
        assertTrue(review.summary.contains("1800"))
        assertTrue(review.notMedicalAdviceFooter.contains("not medical advice"))
    }

    @Test
    fun promptBuilder_includesTotalsNotInvented() {
        val prompt =
            MorningReviewPromptBuilder.build(
                MorningReviewPromptInput(
                    locale = "en-GB",
                    dayKey = "2024-06-01",
                    foodNames = listOf("Oats"),
                    totals =
                    com.omnigart.shared.food.models.FoodLogDayTotals(
                        energyKcal = 400.0,
                        entryCount = 1
                    ),
                    targets = StubNutritionTargetsReader().targetsForDay("p", "2024-06-01"),
                    gaps = emptyList(),
                    suppressDeficitLanguage = false
                )
            )
        assertTrue(prompt.contains("400"))
        assertTrue(prompt.contains("Never invent"))
    }

    @Test
    fun morningReview_fallsBackWhenAiUnavailable() {
        val svc = FoodLoggingService(store = InMemoryFoodDiaryStore())
        svc.quickAdd(
            "p1",
            QuickMacroAdd("Oats", 350.0, 12.0, 55.0, 8.0),
            at = Instant.parse("2024-06-01T12:00:00Z")
        )
        val review =
            svc.morningReview(
                profileId = "p1",
                dayKey = "2024-06-01",
                dayStart = Instant.parse("2024-06-01T00:00:00Z"),
                dayEnd = Instant.parse("2024-06-02T00:00:00Z"),
                locale = "en-GB",
                ageYears = 30,
                sex = BiologicalSexBand.UNSPECIFIED,
                availability =
                OnDeviceAiAvailability(
                    deviceEligible = false,
                    featureEnabled = false,
                    modelReady = false,
                    reasonIfUnavailable = "On-device model not available"
                )
            )
        assertEquals(350.0, review.groundedEnergyKcal)
        assertEquals(1, svc.store.reviews("p1").size)
    }

    @Test
    fun copyDay_duplicatesEntries() {
        val svc = FoodLoggingService(store = InMemoryFoodDiaryStore())
        svc.quickAdd(
            "p1",
            QuickMacroAdd("Soup", 200.0),
            mealGroup = MealGroup.LUNCH,
            at = Instant.parse("2024-06-01T12:00:00Z")
        )
        val copied =
            svc.copyDay(
                "p1",
                Instant.parse("2024-06-01T00:00:00Z"),
                Instant.parse("2024-06-02T00:00:00Z"),
                Instant.parse("2024-06-02T00:00:00Z")
            )
        assertEquals(1, copied.size)
        assertEquals(
            2,
            svc.store.entriesForRange(
                "p1",
                Instant.parse("2024-06-01T00:00:00Z"),
                Instant.parse("2024-06-03T00:00:00Z")
            ).size
        )
    }
}
