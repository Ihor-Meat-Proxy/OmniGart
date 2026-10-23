package com.omnigart.body

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class BodyLocalesTest {

    private val root: Path =
        Path.of("").toAbsolutePath().resolve("../../content/body").normalize().let {
            // Resolve from module dir or repo root depending on Gradle cwd.
            val candidates =
                listOf(
                    Path.of("content/body"),
                    Path.of("../content/body"),
                    Path.of("../../content/body"),
                    Path.of("/workspace/content/body"),
                )
            candidates.first { Files.isDirectory(it) }
        }

    @Test
    fun localesEnGbAndUkMatchKeysAndNoRu() {
        val en = load("locales/en-GB.json")
        val uk = load("locales/uk.json")
        assertEquals(en.keys, uk.keys)
        assertTrue(en.keys.any { it.startsWith("body.method.cun_bae") })
        assertFalse(Files.exists(root.resolve("locales/ru.json")))
        val localeNames =
            Files.list(root.resolve("locales")).use { stream ->
                stream.map { it.fileName.toString() }.toList()
            }
        assertFalse(localeNames.any { it.startsWith("ru") })
    }

    private fun load(relative: String): Map<String, String> {
        val text = Files.readString(root.resolve(relative))
        val obj = Json.parseToJsonElement(text) as JsonObject
        return obj.mapValues { it.value.jsonPrimitive.content }
    }
}
