package com.omnigart.android

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.omnigart.shared.OmniGartCore
import java.util.Calendar
import java.util.Locale

/** Builds and shares OmniGart workout PDFs via the system share sheet / SAF. */
object OmnigartPdfShare {
    fun shareWorkout(context: Context, core: OmniGartCore, workoutId: String, strengthJson: String? = null) {
        val locale = Locale.getDefault().toLanguageTag()
        val document =
            core.localHealth.buildWorkoutPdf(
                workoutId = workoutId,
                locale = locale,
                hrMaxBpm = 190,
                hrRestBpm = 60,
                strengthJson = strengthJson
            ) ?: return
        val file =
            WorkoutPdfRenderer.renderToFile(
                context = context,
                document = document,
                fileName = "omnigart-$workoutId.pdf"
            )
        shareFile(context, file.name)
    }

    fun shareLatest(context: Context, core: OmniGartCore) {
        val id = core.localHealth.latestWorkoutId() ?: return
        shareWorkout(context, core, id)
    }

    fun shareLast30Days(context: Context, core: OmniGartCore) {
        val end = System.currentTimeMillis()
        val start =
            Calendar.getInstance().apply {
                timeInMillis = end
                add(Calendar.DAY_OF_YEAR, -30)
            }.timeInMillis
        val locale = Locale.getDefault().toLanguageTag()
        val document =
            core.localHealth.buildRangePdf(
                startEpochMs = start,
                endEpochMs = end,
                locale = locale,
                hrMaxBpm = 190,
                hrRestBpm = 60
            )
        val file =
            WorkoutPdfRenderer.renderToFile(
                context = context,
                document = document,
                fileName = "omnigart-workouts.pdf"
            )
        shareFile(context, file.name)
    }

    private fun shareFile(context: Context, fileName: String) {
        val file = java.io.File(context.cacheDir, fileName)
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.pdf_export_button))
        )
    }
}
