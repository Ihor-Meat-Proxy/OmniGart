package com.omnigart.shared.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringCatalogTest {
    @Test
    fun enGbSource() {
        assertEquals("Food", StringCatalog.t(StringKeys.TAB_FOOD, "en-GB"))
    }

    @Test
    fun allRequiredLocalesServeDistinctFoodLabel() {
        val expected =
            mapOf(
                "en-GB" to "Food",
                "uk" to "Їжа",
                "pl" to "Jedzenie",
                "be" to "Ежа",
                "kk" to "Тағам",
                "ka" to "კვება",
                "es" to "Comida",
                "fr" to "Alimentation",
                "de" to "Ernährung",
                "cs" to "Jídlo",
                "zh-Hans" to "饮食",
            )
        for ((locale, label) in expected) {
            assertEquals(label, StringCatalog.t(StringKeys.TAB_FOOD, locale), locale)
        }
    }

    @Test
    fun supportedLocalesMatchRequiredAndNeverRussian() {
        assertEquals(OmniLocales.REQUIRED.toSet(), StringCatalog.supportedLocales())
        assertFalse(StringCatalog.supportedLocales().any { it.startsWith("ru") })
    }

    @Test
    fun unknownKeyFallsBackToKeyLiteral() {
        assertEquals("missing.key", StringCatalog.t("missing.key", "fr"))
    }
}

class OmniLocalesTest {
    @Test
    fun exactLocaleWins() {
        assertEquals("fr", OmniLocales.resolve("fr"))
        assertEquals("zh-Hans", OmniLocales.resolve("zh-Hans"))
        assertEquals("en-GB", OmniLocales.resolve("en-GB"))
        assertEquals("uk", OmniLocales.resolve("uk"))
    }

    @Test
    fun languageThenFallback() {
        assertEquals("fr", OmniLocales.resolve("fr-CA"))
        assertEquals("de", OmniLocales.resolve("de_AT"))
        assertEquals("en-GB", OmniLocales.resolve("en"))
        assertEquals("en-GB", OmniLocales.resolve("en-US"))
        assertEquals("en-GB", OmniLocales.resolve("pt-BR"))
        assertEquals("en-GB", OmniLocales.resolve(""))
    }

    @Test
    fun chineseMapsToZhHans() {
        assertEquals("zh-Hans", OmniLocales.resolve("zh"))
        assertEquals("zh-Hans", OmniLocales.resolve("zh-CN"))
        assertEquals("zh-Hans", OmniLocales.resolve("zh_CN"))
        assertEquals("zh-Hans", OmniLocales.resolve("zh-Hans-CN"))
        assertEquals("zh-Hans", OmniLocales.resolve("zh-SG"))
    }

    @Test
    fun russianNeverSelected() {
        assertEquals("en-GB", OmniLocales.resolve("ru"))
        assertEquals("en-GB", OmniLocales.resolve("ru-RU"))
        assertEquals("en-GB", OmniLocales.resolve("ru_UA"))
        assertTrue(OmniLocales.REQUIRED.none { it.startsWith("ru") })
    }
}
