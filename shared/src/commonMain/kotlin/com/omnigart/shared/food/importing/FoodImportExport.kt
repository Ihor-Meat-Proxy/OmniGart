package com.omnigart.shared.food.importing

import com.omnigart.shared.food.models.FoodLogEntry
import com.omnigart.shared.food.models.FoodSource
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.review.MorningFoodReview
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FoodExportDocument(
    val schemaVersion: Int = 1,
    val profileId: String,
    val exportedAtEpochMs: Long,
    val entries: List<FoodLogEntryDto>,
    val reviews: List<MorningFoodReview> = emptyList()
)

@Serializable
data class FoodLogEntryDto(
    val id: String,
    val name: String,
    val mealGroup: String = MealGroup.SNACKS.name,
    val customMealLabel: String? = null,
    val source: String = FoodSource.CUSTOM.name,
    val barcode: String? = null,
    val energyKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fibreG: Double = 0.0,
    val sugarsG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val micros: Map<String, Double> = emptyMap(),
    val favourite: Boolean = false,
    val loggedAtEpochMs: Long
)

object FoodImportExport {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    fun toDto(e: FoodLogEntry): FoodLogEntryDto = FoodLogEntryDto(
        id = e.id,
        name = e.name,
        mealGroup = e.mealGroup.name,
        customMealLabel = e.customMealLabel,
        source = e.source.name,
        barcode = e.barcode,
        energyKcal = e.energyKcal,
        proteinG = e.proteinG,
        carbsG = e.carbsG,
        fatG = e.fatG,
        fibreG = e.fibreG,
        sugarsG = e.sugarsG,
        sodiumMg = e.sodiumMg,
        micros = e.micros,
        favourite = e.favourite,
        loggedAtEpochMs = e.loggedAt.toEpochMilliseconds()
    )

    fun fromDto(profileId: String, dto: FoodLogEntryDto): FoodLogEntry = FoodLogEntry(
        id = dto.id,
        profileId = profileId,
        name = dto.name,
        mealGroup = runCatching { MealGroup.valueOf(dto.mealGroup) }.getOrDefault(MealGroup.SNACKS),
        customMealLabel = dto.customMealLabel,
        source = runCatching { FoodSource.valueOf(dto.source) }.getOrDefault(FoodSource.CUSTOM),
        barcode = dto.barcode,
        energyKcal = dto.energyKcal,
        proteinG = dto.proteinG,
        carbsG = dto.carbsG,
        fatG = dto.fatG,
        fibreG = dto.fibreG,
        sugarsG = dto.sugarsG,
        sodiumMg = dto.sodiumMg,
        micros = dto.micros,
        favourite = dto.favourite,
        loggedAt = Instant.fromEpochMilliseconds(dto.loggedAtEpochMs)
    )

    fun exportJson(profileId: String, entries: List<FoodLogEntry>, reviews: List<MorningFoodReview>, exportedAtEpochMs: Long): String {
        val doc =
            FoodExportDocument(
                profileId = profileId,
                exportedAtEpochMs = exportedAtEpochMs,
                entries = entries.map(::toDto),
                reviews = reviews
            )
        return json.encodeToString(doc)
    }

    fun importJson(profileId: String, text: String): List<FoodLogEntry> {
        val doc = json.decodeFromString<FoodExportDocument>(text)
        return doc.entries.map { fromDto(profileId, it) }
    }

    /**
     * CSV columns: id,name,meal_group,source,energy_kcal,protein_g,carbs_g,fat_g,logged_at_epoch_ms
     */
    fun exportCsv(entries: List<FoodLogEntry>): String {
        val header =
            "id,name,meal_group,source,energy_kcal,protein_g,carbs_g,fat_g,logged_at_epoch_ms"
        val rows =
            entries.map { e ->
                listOf(
                    e.id,
                    escapeCsv(e.name),
                    e.mealGroup.name,
                    e.source.name,
                    e.energyKcal.toString(),
                    e.proteinG.toString(),
                    e.carbsG.toString(),
                    e.fatG.toString(),
                    e.loggedAt.toEpochMilliseconds().toString()
                ).joinToString(",")
            }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun importCsv(profileId: String, text: String): List<FoodLogEntry> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val data = if (lines.first().startsWith("id,")) lines.drop(1) else lines
        return data.mapNotNull { line ->
            val cols = parseCsvLine(line)
            if (cols.size < 9) return@mapNotNull null
            FoodLogEntry(
                id = cols[0],
                profileId = profileId,
                name = cols[1],
                mealGroup = runCatching { MealGroup.valueOf(cols[2]) }.getOrDefault(MealGroup.SNACKS),
                source = runCatching { FoodSource.valueOf(cols[3]) }.getOrDefault(FoodSource.CUSTOM),
                energyKcal = cols[4].toDoubleOrNull() ?: 0.0,
                proteinG = cols[5].toDoubleOrNull() ?: 0.0,
                carbsG = cols[6].toDoubleOrNull() ?: 0.0,
                fatG = cols[7].toDoubleOrNull() ?: 0.0,
                loggedAt = Instant.fromEpochMilliseconds(cols[8].toLongOrNull() ?: 0L)
            )
        }
    }

    private fun escapeCsv(value: String): String = if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    out += sb.toString()
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString()
        return out
    }
}
