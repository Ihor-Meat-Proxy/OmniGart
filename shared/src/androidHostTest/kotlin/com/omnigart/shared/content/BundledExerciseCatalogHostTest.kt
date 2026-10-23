package com.omnigart.shared.content

import com.omnigart.shared.calc.strategy.FormulaSettingsService
import com.omnigart.shared.data.ExerciseRepository
import com.omnigart.shared.data.SessionRepository
import com.omnigart.shared.data.UserProfileRepository
import com.omnigart.shared.data.createInMemoryDatabase
import com.omnigart.shared.domain.LoadModel
import com.omnigart.shared.logging.StrengthLoggingService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BundledExerciseCatalogHostTest {
    private fun fixtureStore(): MapContentStore {
        val manifest =
            """
            {
              "schemaVersion": 1,
              "packId": "omnigart-test-fixture",
              "packVersion": 1,
              "offline": true,
              "sha256": "fixture-digest",
              "sections": {
                "exercises": { "fileCount": 2, "sha256": "ex" }
              },
              "localePolicy": {
                "source": "en-GB",
                "included": ["en-GB", "uk"],
                "forbidden": ["ru"]
              }
            }
            """.trimIndent()
        val pushUp =
            """
            {
              "id": "push-up",
              "nameKeys": {
                "enGB": "exercise.push_up.name",
                "uk": "exercise.push_up.name"
              },
              "discipline": "strength",
              "primaryMuscles": ["pectoralis_major", "triceps"],
              "unilateral": false,
              "loadModel": "bodyweight_plus_external",
              "bodyweightFraction": {
                "proportion": 0.64,
                "basis": "estimate",
                "rationale": "Fixture coefficient for host tests"
              },
              "allowsAddedWeight": true,
              "allowsAssistance": true
            }
            """.trimIndent()
        val lunge =
            """
            {
              "id": "reverse-lunge-dumbbell",
              "nameKeys": {
                "enGB": "exercise.reverse_lunge_dumbbell.name",
                "uk": "exercise.reverse_lunge_dumbbell.name"
              },
              "discipline": "strength",
              "primaryMuscles": ["quadriceps", "gluteus_maximus"],
              "unilateral": true,
              "loadModel": "external_only",
              "bodyweightFraction": {
                "proportion": 0.0,
                "basis": "estimate",
                "rationale": "External load"
              },
              "allowsAddedWeight": true,
              "allowsAssistance": false
            }
            """.trimIndent()
        val locales =
            """
            {
              "exercise.push_up.name": "Push-up",
              "exercise.reverse_lunge_dumbbell.name": "Dumbbell reverse lunge"
            }
            """.trimIndent()
        return MapContentStore.ofText(
            mapOf(
                "manifest.json" to manifest,
                "exercises/data/push-up.json" to pushUp,
                "exercises/data/reverse-lunge-dumbbell.json" to lunge,
                "exercises/locales/en-GB.json" to locales
            )
        )
    }

    @Test
    fun layeredLoader_backedCatalog_returnsExercises() {
        val store = fixtureStore()
        val loader = LayeredContentLoader(bundled = store)
        val catalog = BundledExerciseCatalog.load(loader)
        assertEquals(2, catalog.size)
        val pushUp = catalog.single { it.exercise.id == "push-up" }
        assertEquals("Push-up", pushUp.displayName)
        assertEquals("ex.push_up", pushUp.exercise.contentKey)
        assertEquals(listOf("pectoralis_major", "triceps"), pushUp.exercise.primaryMuscles)
        assertEquals(LoadModel.BODYWEIGHT_PLUS_EXTERNAL, pushUp.loadSemantics.loadModel)
        assertEquals(0.64, pushUp.loadSemantics.bodyweightFraction.proportion, absoluteTolerance = 1e-9)
        val lunge = catalog.single { it.exercise.id == "reverse-lunge-dumbbell" }
        assertTrue(lunge.unilateral)
        assertEquals(LoadModel.EXTERNAL_ONLY, lunge.loadSemantics.loadModel)
    }

    @Test
    fun emptyStore_fallsBackToDemoCatalog() {
        val empty = MapContentStore.ofText(emptyMap())
        val catalog = BundledExerciseCatalog.load(empty)
        assertEquals(StrengthLoggingService.demoCatalog().map { it.exercise.id }, catalog.map { it.exercise.id })
    }

    @Test
    fun strengthLoggingService_seedsFromInjectableStore() {
        val db = createInMemoryDatabase()
        val exerciseRepo = ExerciseRepository(db)
        val svc =
            StrengthLoggingService(
                sessions = SessionRepository(db),
                exercises = exerciseRepo,
                profiles = UserProfileRepository(db),
                formulaSettings = FormulaSettingsService(db),
                contentStore = fixtureStore()
            )
        val loaded = svc.loadExercises()
        assertEquals(2, loaded.size)
        assertEquals("push-up", exerciseRepo.get("push-up")?.id)
        assertEquals("ex.push_up", exerciseRepo.get("push-up")?.contentKey)
    }
}
