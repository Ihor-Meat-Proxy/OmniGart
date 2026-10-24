package com.omnigart.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.omnigart.shared.health.store.pdf.PdfFontSpec
import com.omnigart.shared.health.store.pdf.WorkoutPdfDocument
import com.omnigart.shared.health.store.pdf.WorkoutPdfHeartRate
import com.omnigart.shared.health.store.pdf.WorkoutPdfPage
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** Renders [WorkoutPdfDocument] with Android [PdfDocument] and Noto asset fonts. */
@Suppress("TooManyFunctions")
object WorkoutPdfRenderer {
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792

    fun renderToFile(context: Context, document: WorkoutPdfDocument, fileName: String = "omnigart-workout.pdf"): File {
        val fonts = loadFonts(context, document.fontSpec)
        val pdf = PdfDocument()
        val pages = document.workouts.ifEmpty { listOf(null) }
        pages.forEachIndexed { index, workout ->
            val pageInfo =
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            var y = drawChrome(canvas, document, fonts, index, pages.size)
            if (workout != null) {
                y = drawWorkout(canvas, workout, document, fonts, y)
            }
            pdf.finishPage(page)
        }
        val out = File(context.cacheDir, fileName)
        FileOutputStream(out).use { pdf.writeTo(it) }
        pdf.close()
        return out
    }

    private data class Fonts(val body: Typeface, val bold: Typeface)

    private fun loadFonts(context: Context, spec: PdfFontSpec): Fonts {
        val assets = context.assets
        val regular =
            runCatching { Typeface.createFromAsset(assets, "fonts/NotoSans-Regular.ttf") }
                .getOrElse { Typeface.SANS_SERIF }
        val bold =
            runCatching { Typeface.createFromAsset(assets, "fonts/NotoSans-Bold.ttf") }
                .getOrElse { Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val georgian =
            if (spec.georgianFamily != null) {
                runCatching {
                    Typeface.createFromAsset(assets, "fonts/NotoSansGeorgian-Regular.ttf")
                }.getOrNull()
            } else {
                null
            }
        val cjk =
            if (spec.usePlatformCjkCascade) {
                Typeface.create("sans-serif", Typeface.NORMAL)
            } else {
                null
            }
        return Fonts(
            body = cjk ?: georgian ?: regular,
            bold = if (cjk != null) Typeface.create(cjk, Typeface.BOLD) else bold
        )
    }

    private fun drawChrome(canvas: Canvas, document: WorkoutPdfDocument, fonts: Fonts, pageIndex: Int, pageCount: Int): Float {
        var y = 40f
        val margin = 40f
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = fonts.bold
                textSize = 22f
                color = 0xFF1F734F.toInt()
            }
        canvas.drawText(document.brandTitle, margin, y, paint)
        y += 28f
        paint.textSize = 16f
        paint.color = 0xFF000000.toInt()
        canvas.drawText(document.reportTitle, margin, y, paint)
        y += 20f
        paint.typeface = fonts.body
        paint.textSize = 10f
        paint.color = 0xFF555555.toInt()
        canvas.drawText(document.generatedAtIso, margin, y, paint)
        y += 16f

        paint.textSize = 8f
        paint.color = 0xFF777777.toInt()
        drawMultiline(
            canvas,
            document.disclaimer,
            margin,
            PAGE_HEIGHT - 70f,
            PAGE_WIDTH - margin * 2,
            paint
        )
        paint.textSize = 9f
        canvas.drawText(
            "${pageIndex + 1} / $pageCount",
            PAGE_WIDTH - margin - 36f,
            PAGE_HEIGHT - 24f,
            paint
        )
        return y + 8f
    }

    private fun drawWorkout(canvas: Canvas, workout: WorkoutPdfPage, document: WorkoutPdfDocument, fonts: Fonts, startY: Float): Float {
        val margin = 40f
        val width = PAGE_WIDTH - margin * 2
        val bold = textPaint(fonts.bold, 11f)
        val body = textPaint(fonts.body, 11f)
        var y = drawLines(canvas, workout.headerLines(), margin, startY, bold, 16f) + 4f
        y = drawLines(canvas, workout.metricLines(), margin, y, body, 15f)
        y = drawHeartRateBlock(canvas, workout, margin, width, y, body)
        y = drawCardioBlock(canvas, workout, margin, y, body)
        y = drawStrengthBlock(canvas, workout, margin, y, bold, body)
        y = drawSegmentsBlock(canvas, workout, document.locale, margin, y, bold, body)
        return drawMuscleBlock(canvas, workout, document.locale, margin, width, y, bold, body)
    }

    private fun WorkoutPdfPage.headerLines(): List<String> = listOf(header.dateLabel, header.activityLabel, header.durationLabel)

    private fun WorkoutPdfPage.metricLines(): List<String> = listOfNotNull(
        metrics.activeKcalLabel,
        metrics.totalKcalLabel,
        metrics.avgHrLabel,
        metrics.maxHrLabel
    )

    private fun textPaint(typeface: Typeface, size: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        color = 0xFF000000.toInt()
    }

    private fun drawLines(canvas: Canvas, lines: List<String>, x: Float, startY: Float, paint: Paint, step: Float): Float {
        var y = startY
        lines.forEach {
            canvas.drawText(it, x, y, paint)
            y += step
        }
        return y
    }

    private fun drawHeartRateBlock(
        canvas: Canvas,
        workout: WorkoutPdfPage,
        margin: Float,
        width: Float,
        startY: Float,
        body: Paint
    ): Float {
        val hr = workout.heartRate ?: return startY
        var y = startY + 8f
        y = drawHrChart(canvas, hr, margin, y, width)
        hr.timeInZoneLines.forEach { line ->
            body.color = 0xFF444444.toInt()
            canvas.drawText(line, margin, y, body)
            y += 14f
        }
        body.color = 0xFF000000.toInt()
        return y
    }

    private fun drawCardioBlock(canvas: Canvas, workout: WorkoutPdfPage, margin: Float, startY: Float, body: Paint): Float {
        val cardio = workout.cardio ?: return startY
        return drawLines(
            canvas,
            listOfNotNull(cardio.distanceLabel, cardio.paceLabel, cardio.cadenceLabel),
            margin,
            startY + 6f,
            body,
            14f
        )
    }

    private fun drawStrengthBlock(canvas: Canvas, workout: WorkoutPdfPage, margin: Float, startY: Float, bold: Paint, body: Paint): Float {
        val strength = workout.strength ?: return startY
        var y = startY + 8f
        canvas.drawText(strength.totalWeightLabel, margin, y, bold)
        y += 16f
        strength.exercises.forEach { exercise ->
            canvas.drawText(exercise.title, margin, y, bold)
            y += 14f
            exercise.setLines.forEach { line ->
                body.color = 0xFF444444.toInt()
                canvas.drawText(line, margin + 12f, y, body)
                y += 13f
            }
            body.color = 0xFF000000.toInt()
        }
        return y
    }

    private fun drawSegmentsBlock(
        canvas: Canvas,
        workout: WorkoutPdfPage,
        locale: String,
        margin: Float,
        startY: Float,
        bold: Paint,
        body: Paint
    ): Float {
        if (workout.segments.isEmpty()) return startY
        var y = startY + 8f
        canvas.drawText(PdfSectionTitles.segments(locale), margin, y, bold)
        y += 15f
        workout.segments.forEach { segment ->
            canvas.drawText("${segment.label}: ${segment.detail}", margin, y, body)
            y += 14f
        }
        return y
    }

    private fun drawMuscleBlock(
        canvas: Canvas,
        workout: WorkoutPdfPage,
        locale: String,
        margin: Float,
        width: Float,
        startY: Float,
        bold: Paint,
        body: Paint
    ): Float {
        if (workout.muscleMap.isEmpty()) return startY
        var y = startY + 8f
        canvas.drawText(PdfSectionTitles.muscleMap(locale), margin, y, bold)
        y += 16f
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        workout.muscleMap.forEach { bar ->
            barPaint.color =
                android.graphics.Color.argb(
                    (90 + 140 * bar.intensity).toInt().coerceIn(0, 255),
                    31,
                    115,
                    79
                )
            canvas.drawRect(margin, y, margin + width * bar.intensity, y + 10f, barPaint)
            body.textSize = 9f
            body.color = 0xFF444444.toInt()
            canvas.drawText(bar.displayName, margin, y + 22f, body)
            body.textSize = 11f
            body.color = 0xFF000000.toInt()
            y += 28f
        }
        return y
    }

    private fun drawHrChart(canvas: Canvas, hr: WorkoutPdfHeartRate, margin: Float, startY: Float, width: Float): Float {
        val height = 110f
        val top = startY
        val bg = Paint().apply { color = 0xFFF5F5F5.toInt() }
        canvas.drawRect(margin, top, margin + width, top + height, bg)
        hr.zoneBands.forEach { band ->
            val y0 = top + height - band.upperY * height
            val y1 = top + height - band.lowerY * height
            val paint =
                Paint().apply {
                    color =
                        android.graphics.Color.argb(
                            46,
                            ((band.colorArgb shr 16) and 0xFF).toInt(),
                            ((band.colorArgb shr 8) and 0xFF).toInt(),
                            (band.colorArgb and 0xFF).toInt()
                        )
                }
            canvas.drawRect(margin, y0, margin + width, max(y0 + 1f, y1), paint)
        }
        val line =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFD93240.toInt()
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }
        val path = android.graphics.Path()
        hr.chartPoints.forEachIndexed { index, point ->
            val x = margin + point.x * width
            val y = top + height - point.y * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, line)
        return top + height + 18f
    }

    private fun drawMultiline(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        val words = text.split(' ')
        var line = StringBuilder()
        var cursor = y
        words.forEach { word ->
            val trial = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(trial) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, cursor, paint)
                cursor += paint.textSize + 2f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(trial)
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line.toString(), x, cursor, paint)
    }
}

private object PdfSectionTitles {
    fun segments(locale: String): String = com.omnigart.shared.health.store.pdf.PdfStrings.t("pdf_field_segments", locale)

    fun muscleMap(locale: String): String = com.omnigart.shared.health.store.pdf.PdfStrings.t("pdf_field_muscle_map", locale)
}
