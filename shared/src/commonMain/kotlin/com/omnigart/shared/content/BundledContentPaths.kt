package com.omnigart.shared.content

/**
 * Stable relative paths inside the offline content bundle / update packs.
 * Host apps mount `generated/content-bundle/` as Android assets / iOS bundle resources
 * under this logical root (usually the asset/bundle root itself).
 */
object BundledContentPaths {
    const val MANIFEST = "manifest.json"
    const val EXERCISES = "exercises"
    const val EXERCISES_DATA = "exercises/data"
    const val EXERCISES_TEMPLATES = "exercises/templates"
    const val EXERCISES_LOCALES = "exercises/locales"
    const val EVIDENCE = "evidence"
    const val ICONS = "icons"
    const val BODY_MAPS = "body-maps"
    const val ACHIEVEMENTS = "achievements"
    const val EXERCISE_IMAGES = "exercise-images"

    fun exercise(fileName: String): String = "$EXERCISES_DATA/$fileName"

    fun locale(tag: String): String = "$EXERCISES_LOCALES/$tag.json"
}
