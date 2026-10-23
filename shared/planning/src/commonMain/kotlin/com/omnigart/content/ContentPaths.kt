package com.omnigart.content

/**
 * Logical layout of the exercise section inside the offline content bundle
 * (`generated/content-bundle/exercises/…`) or the repo `content/exercises/` tree.
 * No I/O here — [com.omnigart.shared.content.LayeredContentLoader] mounts concrete stores.
 */
object ContentPaths {
    const val EXERCISES_DATA = "data"
    const val TEMPLATES = "templates"
    const val LOCALES = "locales"
    const val SCHEMA_EXERCISE = "schema/exercise.schema.json"
    const val SCHEMA_TEMPLATE = "schema/workout-template.schema.json"
    const val EQUIPMENT_PRESETS = "equipment-presets.json"
}
