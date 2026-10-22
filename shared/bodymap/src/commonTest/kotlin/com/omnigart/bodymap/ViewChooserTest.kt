package com.omnigart.bodymap

import com.omnigart.content.Muscle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewChooserTest {
    @Test
    fun benchPressPrefersFront() {
        val views = ViewChooser.viewsFor(
            listOf(Muscle.PECTORALIS_MAJOR, Muscle.TRICEPS, Muscle.FRONT_DELTOID),
            listOf(Muscle.SERRATUS_ANTERIOR),
        )
        // triceps is back → both
        assertTrue(BodyMapView.FRONT in views)
        assertTrue(BodyMapView.BACK in views)
    }

    @Test
    fun pullUpPrefersBack() {
        val views = ViewChooser.viewsFor(
            listOf(Muscle.LATISSIMUS_DORSI, Muscle.BICEPS),
            listOf(Muscle.TRAPEZIUS, Muscle.RHOMBOIDS, Muscle.REAR_DELTOID, Muscle.FOREARMS),
        )
        assertTrue(BodyMapView.BACK in views)
        assertTrue(BodyMapView.FRONT in views) // biceps + forearms
    }

    @Test
    fun figureKeyUsesSex() {
        assertEquals("female-front", ViewChooser.figureKey(BiologicalSex.FEMALE, BodyMapView.FRONT))
        assertEquals("male-back", ViewChooser.figureKey(BiologicalSex.UNSPECIFIED, BodyMapView.BACK))
    }

    @Test
    fun taxonomyCoversEveryMuscle() {
        for (m in Muscle.entries) {
            assertTrue(TaxonomyRegions.covers(m), "missing mapping for $m")
        }
    }
}
