package com.omnigart.shared.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Host-side coverage for locale resolution and catalog tables.
 * Mirrors [StringCatalogTest] / [OmniLocalesTest] so `:shared:testAndroidHostTest` exercises them.
 */
class OmniLocalesHostTest {
    @Test
    fun resolveExactLanguageAndFallback() {
        assertEquals("fr", OmniLocales.resolve("fr"))
        assertEquals("fr", OmniLocales.resolve("fr-CA"))
        assertEquals("de", OmniLocales.resolve("de_AT"))
        assertEquals("en-GB", OmniLocales.resolve("en-US"))
        assertEquals("en-GB", OmniLocales.resolve("pt-BR"))
        assertEquals("zh-Hans", OmniLocales.resolve("zh-CN"))
        assertEquals("zh-Hans", OmniLocales.resolve("zh_Hans_CN"))
        assertEquals("en-GB", OmniLocales.resolve("ru"))
        assertEquals("en-GB", OmniLocales.resolve("ru-RU"))
    }

    @Test
    fun catalogServesAllRequiredLocales() {
        assertEquals(OmniLocales.REQUIRED.toSet(), StringCatalog.supportedLocales())
        assertFalse(StringCatalog.supportedLocales().any { it.startsWith("ru") })
        assertEquals("Food", StringCatalog.t(StringKeys.TAB_FOOD, "en-GB"))
        assertEquals("Alimentation", StringCatalog.t(StringKeys.TAB_FOOD, "fr"))
        assertEquals("饮食", StringCatalog.t(StringKeys.TAB_FOOD, "zh-Hans"))
        assertEquals("Їжа", StringCatalog.t(StringKeys.TAB_FOOD, "uk"))
        assertTrue(OmniLocales.REQUIRED.none { it.startsWith("ru") })
    }
}
