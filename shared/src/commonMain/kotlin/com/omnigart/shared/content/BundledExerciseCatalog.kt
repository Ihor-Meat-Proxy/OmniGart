package com.omnigart.shared.content

import com.omnigart.shared.domain.BodyweightFraction
import com.omnigart.shared.domain.Exercise
import com.omnigart.shared.domain.ExerciseLoadSemantics
import com.omnigart.shared.domain.ExerciseModality
import com.omnigart.shared.domain.LoadModel
import com.omnigart.shared.logging.DemoStrengthExercise
import com.omnigart.shared.logging.StrengthLoggingService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Offline exercise catalog from the content bundle (or an injectable [ContentStore] fixture).
 *
 * Reads JSON under [BundledContentPaths.EXERCISES_DATA] via [ContentStore] /
 * [LayeredContentLoader], maps enough fields for strength logging ([Exercise],
 * [ExerciseLoadSemantics], display name), and falls back to
 * [StrengthLoggingService.demoCatalog] when the pack has no parseable exercises.
 */
object BundledExerciseCatalog {
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = false
        }

    fun load(
        store: ContentStore,
        fallback: () -> List<DemoStrengthExercise> = { StrengthLoggingService.demoCatalog() }
    ): List<DemoStrengthExercise> = loadFromPaths(
        paths = store.list(BundledContentPaths.EXERCISES_DATA).filter { it.endsWith(".json") },
        readText = store::readText,
        fallback = fallback
    )

    fun load(
        loader: LayeredContentLoader,
        fallback: () -> List<DemoStrengthExercise> = { StrengthLoggingService.demoCatalog() }
    ): List<DemoStrengthExercise> = loadFromPaths(
        paths = loader.list(BundledContentPaths.EXERCISES_DATA).filter { it.endsWith(".json") },
        readText = loader::readText,
        fallback = fallback
    )

    private fun loadFromPaths(
        paths: List<String>,
        readText: (String) -> String?,
        fallback: () -> List<DemoStrengthExercise>
    ): List<DemoStrengthExercise> {
        if (paths.isEmpty()) return fallback()
        val localeNames = loadEnGbNames(readText)
        val parsed =
            paths.mapNotNull { path ->
                val text = readText(path) ?: return@mapNotNull null
                parseExercise(text, localeNames)
            }
        return parsed.ifEmpty { fallback() }
    }

    private fun loadEnGbNames(readText: (String) -> String?): Map<String, String> {
        val text = readText(BundledContentPaths.locale("en-GB")) ?: return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, String>>(text)
        }.getOrDefault(emptyMap())
    }

    internal fun parseExercise(text: String, localeNames: Map<String, String> = emptyMap()): DemoStrengthExercise? {
        val dto =
            runCatching {
                json.decodeFromString(BundledExerciseDto.serializer(), text)
            }.getOrNull() ?: return null
        if (dto.id.isBlank()) return null
        val contentKey = contentKeyFor(dto)
        val displayName =
            dto.nameKeys?.enGB?.let { localeNames[it] }
                ?: humanizeId(dto.id)
        val loadSemantics =
            ExerciseLoadSemantics.fromOptional(
                loadModel = dto.loadModel,
                bodyweightFraction = dto.bodyweightFraction,
                allowsAddedWeight = dto.allowsAddedWeight,
                allowsAssistance = dto.allowsAssistance
            )
        return DemoStrengthExercise(
            exercise =
            Exercise(
                id = dto.id,
                contentKey = contentKey,
                modality = modalityFor(dto.discipline),
                primaryMuscles = dto.primaryMuscles
            ),
            unilateral = dto.unilateral,
            loadSemantics = loadSemantics,
            displayName = displayName
        )
    }

    private fun contentKeyFor(dto: BundledExerciseDto): String {
        val fromName =
            dto.nameKeys?.enGB
                ?.removePrefix("exercise.")
                ?.removeSuffix(".name")
                ?.takeIf { it.isNotBlank() }
        if (fromName != null) return "ex.$fromName"
        return "ex.${dto.id.replace('-', '_')}"
    }

    private fun modalityFor(discipline: String?): ExerciseModality = when (discipline?.lowercase()) {
        "strength" -> ExerciseModality.STRENGTH
        "running", "swimming" -> ExerciseModality.CARDIO
        "yoga", "pilates" -> ExerciseModality.MOBILITY
        else -> ExerciseModality.STRENGTH
    }

    private fun humanizeId(id: String): String = id.split('-').joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

@Serializable
private data class BundledExerciseDto(
    val id: String,
    val nameKeys: BundledNameKeys? = null,
    val discipline: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val unilateral: Boolean = false,
    val loadModel: LoadModel? = null,
    val bodyweightFraction: BodyweightFraction? = null,
    val allowsAddedWeight: Boolean? = null,
    val allowsAssistance: Boolean? = null
)

@Serializable
private data class BundledNameKeys(val enGB: String? = null, val uk: String? = null)
