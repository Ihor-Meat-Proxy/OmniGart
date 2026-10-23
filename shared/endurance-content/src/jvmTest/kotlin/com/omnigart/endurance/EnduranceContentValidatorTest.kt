package com.omnigart.endurance

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnduranceContentValidatorTest {

    private val runningRoot =
        Path.of(System.getProperty("omnigart.running.root") ?: error("omnigart.running.root missing"))
    private val swimmingRoot =
        Path.of(System.getProperty("omnigart.swimming.root") ?: error("omnigart.swimming.root missing"))

    @Test
    fun bundledRunningAndSwimmingContentValidates() {
        val report = EnduranceContentValidator(runningRoot, swimmingRoot).validate()
        assertTrue(
            report.ok,
            report.issues.joinToString("\n") { "${it.path}: ${it.message}" },
        )
        assertEquals(25, report.runningWorkoutTypes)
        assertEquals(8, report.runningPlans)
        assertEquals(5, report.swimStrokes)
        assertEquals(30, report.swimDrills)
        assertEquals(20, report.swimSessions)
    }
}
