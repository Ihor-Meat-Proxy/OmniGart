package com.omnigart.android

import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.logging.StrengthSessionSummary
import com.omnigart.shared.util.formatOneDecimal

@Composable
internal fun SessionSummaryBlock(
    summary: StrengthSessionSummary,
    unitLabel: String,
    energyLines: List<String>,
    onExportPdf: (() -> Unit)?
) {
    Text(
        text = stringResource(R.string.workout_summary_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.testTag("workout_summary")
    )
    energyLines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    Text(
        text =
        "${stringResource(R.string.workout_volume_load)}: " +
            "${formatOneDecimal(summary.volume.totalVolumeDisplay)} $unitLabel"
    )
    Text(
        text =
        "${stringResource(R.string.workout_total_weight_lifted)}: " +
            "${formatOneDecimal(summary.totalWeightLiftedDisplay)} $unitLabel"
    )
    Text(text = "${stringResource(R.string.workout_hard_sets)}: ${summary.volume.hardSets}")
    Text(text = stringResource(R.string.workout_per_exercise), style = MaterialTheme.typography.bodyMedium)
    summary.volume.perExercise.forEach { row ->
        Text(text = "${row.exerciseId}: ${formatOneDecimal(row.volumeKg)} $unitLabel · ${row.hardSets}")
    }
    if (onExportPdf != null) {
        Button(onClick = onExportPdf, modifier = Modifier.testTag("pdf_export_button")) {
            Text(text = stringResource(R.string.pdf_export_button))
        }
    }
}

internal fun exportStrengthPdf(context: Context, core: OmniGartCore, lastSessionId: String?, summary: StrengthSessionSummary?) {
    val id = lastSessionId ?: return
    val strengthJson =
        summary?.let { current ->
            val names =
                current.volume.perExercise.joinToString(",") {
                    "${it.exerciseId}=${it.exerciseId}"
                }
            val muscles =
                current.volume.perMuscleVolumeKg.entries.joinToString(",") {
                    "${it.key}=${it.value}"
                }
            "${current.totalWeightLiftedKg}|$names|$muscles"
        }
    OmnigartPdfShare.shareWorkout(
        context = context,
        core = core,
        workoutId = id,
        strengthJson = strengthJson
    )
}
