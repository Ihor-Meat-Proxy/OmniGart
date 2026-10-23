package com.omnigart.shared.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StringCatalogTest {
    @Test
    fun fallbackIsEnGb() {
        assertEquals("Food", StringCatalog.t(StringKeys.TAB_FOOD, "en-GB"))
        assertEquals("Food", StringCatalog.t(StringKeys.TAB_FOOD, "fr"))
    }

    @Test
    fun ukTranslation() {
        assertEquals("Їжа", StringCatalog.t(StringKeys.TAB_FOOD, "uk"))
    }

    @Test
    fun neverListsRussian() {
        assertFalse(StringCatalog.supportedLocales().any { it.startsWith("ru") })
    }
}
