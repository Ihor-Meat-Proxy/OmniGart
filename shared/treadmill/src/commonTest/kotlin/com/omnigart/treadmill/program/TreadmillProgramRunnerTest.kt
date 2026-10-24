package com.omnigart.treadmill.program

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TreadmillProgramRunnerTest {
    @Test
    fun catalogHasRunAndWalkPrograms() {
        assertTrue(TreadmillProgramCatalog.all.size >= 8)
        assertTrue(TreadmillProgramCatalog.forMode(TreadmillActivityMode.WALK).isNotEmpty())
        assertTrue(TreadmillProgramCatalog.forMode(TreadmillActivityMode.RUN).size >= 6)
        assertNotNull(TreadmillProgramCatalog.byId("easy_run"))
        assertNotNull(TreadmillProgramCatalog.byId("recovery_walk"))
    }

    @Test
    fun advancesByElapsedSeconds() {
        val program = TreadmillProgramCatalog.byId("easy_run")!!
        val early =
            TreadmillProgramRunner.prompt(
                program = program,
                elapsedSeconds = 10.0,
                distanceMetres = 0.0,
                allowRemoteControl = true,
                controlAvailable = true
            )
        assertEquals(0, early.stepIndex)
        assertFalse(early.completed)
        assertEquals(7.0, early.autoSpeedKmh!!, 1e-9)

        val mid =
            TreadmillProgramRunner.prompt(
                program = program,
                elapsedSeconds = 400.0,
                distanceMetres = 0.0,
                allowRemoteControl = false,
                controlAvailable = true
            )
        assertEquals(1, mid.stepIndex)
        assertEquals(null, mid.autoSpeedKmh)
        assertEquals("treadmill_program_prompt_step", mid.promptKey)
    }

    @Test
    fun completesAfterAllSteps() {
        val program = TreadmillProgramCatalog.byId("recovery_walk")!!
        val total = program.steps.sumOf { it.goalValue }
        val done =
            TreadmillProgramRunner.prompt(
                program = program,
                elapsedSeconds = total + 1.0,
                distanceMetres = 0.0,
                allowRemoteControl = true,
                controlAvailable = true
            )
        assertTrue(done.completed)
        assertEquals("treadmill_program_complete", done.promptKey)
    }
}
