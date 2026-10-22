package com.omnigart.content

/**
 * Logical layout of the content pack relative to the repo (or bundled assets later).
 * No I/O here — loaders pass concrete roots from the host app / tests.
 */
object ContentPaths {
    const val EXERCISES_DATA = "data"
    const val TEMPLATES = "templates"
    const val LOCALES = "locales"
    const val SCHEMA_EXERCISE = "schema/exercise.schema.json"
    const val SCHEMA_TEMPLATE = "schema/workout-template.schema.json"
    const val EQUIPMENT_PRESETS = "equipment-presets.json"
}
