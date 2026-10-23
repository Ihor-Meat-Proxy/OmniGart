import Shared
import UIKit

/// Shared fonts and geometry for drawing the sections of a single workout page.
struct WorkoutPdfSectionStyle {
    let width: CGFloat
    let body: UIFont
    let bold: UIFont
    let spec: PdfFontSpec

    var margin: CGFloat { WorkoutPdfRenderer.margin }
}

extension WorkoutPdfRenderer {
    // MARK: - Page chrome

    @discardableResult
    static func drawChrome(
        context: CGContext,
        pageRect: CGRect,
        document: WorkoutPdfDocument,
        pageIndex: Int,
        pageCount: Int
    ) -> CGFloat {
        let width = pageRect.width - margin * 2
        let cursorY = drawChromeHeader(document: document, width: width)
        drawChromeFooter(
            document: document,
            pageRect: pageRect,
            width: width,
            pageIndex: pageIndex,
            pageCount: pageCount
        )
        return cursorY + 8
    }

    private static func drawChromeHeader(document: WorkoutPdfDocument, width: CGFloat) -> CGFloat {
        var cursorY: CGFloat = 36
        drawText(
            document.brandTitle as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: boldFont(size: 22, spec: document.fontSpec),
            color: UIColor(red: 0.12, green: 0.45, blue: 0.38, alpha: 1),
            width: width
        )
        cursorY += 28
        drawText(
            document.reportTitle as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: boldFont(size: 16, spec: document.fontSpec),
            color: .black,
            width: width
        )
        cursorY += 22
        drawText(
            document.generatedAtIso as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: bodyFont(size: 10, spec: document.fontSpec),
            color: .darkGray,
            width: width
        )
        return cursorY + 18
    }

    private static func drawChromeFooter(
        document: WorkoutPdfDocument,
        pageRect: CGRect,
        width: CGFloat,
        pageIndex: Int,
        pageCount: Int
    ) {
        let footer = document.disclaimer as NSString
        let footerFont = bodyFont(size: 8, spec: document.fontSpec)
        let footerHeight = footer.boundingRect(
            with: CGSize(width: width, height: 80),
            options: [.usesLineFragmentOrigin],
            attributes: [.font: footerFont],
            context: nil
        ).height
        drawText(
            footer,
            at: CGPoint(x: margin, y: pageRect.height - 28 - footerHeight),
            font: footerFont,
            color: .gray,
            width: width
        )
        let pageLabel = "\(pageIndex + 1) / \(pageCount)" as NSString
        drawText(
            pageLabel,
            at: CGPoint(x: pageRect.width - margin - 40, y: pageRect.height - 24),
            font: bodyFont(size: 9, spec: document.fontSpec),
            color: .gray,
            width: 40
        )
    }

    // MARK: - Workout body

    static func drawWorkout(
        _ workout: WorkoutPdfPage,
        startY: CGFloat,
        pageRect: CGRect,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        let style = WorkoutPdfSectionStyle(
            width: pageRect.width - margin * 2,
            body: bodyFont(size: 11, spec: document.fontSpec),
            bold: boldFont(size: 11, spec: document.fontSpec),
            spec: document.fontSpec
        )
        var cursorY = startY
        cursorY = drawHeaderAndMetrics(workout, startY: cursorY, style: style)
        if let heartRate = workout.heartRate {
            cursorY = drawHeartRateSection(heartRate, startY: cursorY, style: style)
        }
        if let cardio = workout.cardio {
            cursorY = drawCardioSection(cardio, startY: cursorY, style: style)
        }
        if let strength = workout.strength {
            cursorY = drawStrengthSection(strength, startY: cursorY, style: style)
        }
        if !workout.segments.isEmpty {
            cursorY = drawSegmentsSection(workout.segments, startY: cursorY, style: style)
        }
        if !workout.muscleMap.isEmpty {
            cursorY = drawMuscleMapSection(workout.muscleMap, startY: cursorY, style: style)
        }
        return cursorY
    }

    private static func drawLine(
        _ text: String,
        cursorY: CGFloat,
        indent: CGFloat = 0,
        font: UIFont,
        color: UIColor,
        style: WorkoutPdfSectionStyle
    ) {
        drawText(
            text as NSString,
            at: CGPoint(x: style.margin + indent, y: cursorY),
            font: font,
            color: color,
            width: style.width - indent
        )
    }

    private static func drawHeaderAndMetrics(
        _ workout: WorkoutPdfPage,
        startY: CGFloat,
        style: WorkoutPdfSectionStyle
    ) -> CGFloat {
        var cursorY = startY
        let headerLines = [workout.header.dateLabel, workout.header.activityLabel, workout.header.durationLabel]
        for line in headerLines {
            drawLine(line, cursorY: cursorY, font: style.bold, color: .black, style: style)
            cursorY += 16
        }
        cursorY += 4
        let metricLines = [
            workout.metrics.activeKcalLabel,
            workout.metrics.totalKcalLabel,
            workout.metrics.avgHrLabel,
            workout.metrics.maxHrLabel
        ].compactMap { $0 }
        for line in metricLines {
            drawLine(line, cursorY: cursorY, font: style.body, color: .black, style: style)
            cursorY += 15
        }
        return cursorY
    }

    private static func drawHeartRateSection(
        _ heartRate: WorkoutPdfHeartRate,
        startY: CGFloat,
        style: WorkoutPdfSectionStyle
    ) -> CGFloat {
        var cursorY = startY + 8
        cursorY = drawHeartRateChart(
            heartRate,
            at: cursorY,
            margin: style.margin,
            width: style.width,
            fontSpec: style.spec
        )
        for line in heartRate.timeInZoneLines {
            drawLine(line, cursorY: cursorY, font: style.body, color: .darkGray, style: style)
            cursorY += 14
        }
        return cursorY
    }

    private static func drawCardioSection(
        _ cardio: WorkoutPdfCardio,
        startY: CGFloat,
        style: WorkoutPdfSectionStyle
    ) -> CGFloat {
        var cursorY = startY + 6
        for line in [cardio.distanceLabel, cardio.paceLabel, cardio.cadenceLabel].compactMap({ $0 }) {
            drawLine(line, cursorY: cursorY, font: style.body, color: .black, style: style)
            cursorY += 14
        }
        return cursorY
    }

    private static func drawStrengthSection(
        _ strength: WorkoutPdfStrength,
        startY: CGFloat,
        style: WorkoutPdfSectionStyle
    ) -> CGFloat {
        var cursorY = startY + 8
        drawLine(strength.totalWeightLabel, cursorY: cursorY, font: style.bold, color: .black, style: style)
        cursorY += 16
        for exercise in strength.exercises {
            drawLine(exercise.title, cursorY: cursorY, font: style.bold, color: .black, style: style)
            cursorY += 14
            for setLine in exercise.setLines {
                drawLine(setLine, cursorY: cursorY, indent: 12, font: style.body, color: .darkGray, style: style)
                cursorY += 13
            }
        }
        return cursorY
    }

    private static func drawSegmentsSection(
        _ segments: [WorkoutPdfSegment],
        startY: CGFloat,
        style: WorkoutPdfSectionStyle
    ) -> CGFloat {
        var cursorY = startY + 8
        drawLine(
            String(localized: "pdf_field_segments"),
            cursorY: cursorY,
            font: style.bold,
            color: .black,
            style: style
        )
        cursorY += 15
        for segment in segments {
            drawLine(
                "\(segment.label): \(segment.detail)",
                cursorY: cursorY,
                font: style.body,
                color: .black,
                style: style
            )
            cursorY += 14
        }
        return cursorY
    }

    private static func drawMuscleMapSection(
        _ muscleMap: [WorkoutPdfMuscleBar],
        startY: CGFloat,
        style: WorkoutPdfSectionStyle
    ) -> CGFloat {
        var cursorY = startY + 8
        drawLine(
            String(localized: "pdf_field_muscle_map"),
            cursorY: cursorY,
            font: style.bold,
            color: .black,
            style: style
        )
        cursorY += 16
        for bar in muscleMap {
            let barWidth = style.width * CGFloat(bar.intensity)
            let rect = CGRect(x: style.margin, y: cursorY, width: barWidth, height: 10)
            UIColor(red: 0.12, green: 0.45, blue: 0.38, alpha: 0.35 + 0.65 * CGFloat(bar.intensity))
                .setFill()
            UIBezierPath(roundedRect: rect, cornerRadius: 2).fill()
            drawText(
                bar.displayName as NSString,
                at: CGPoint(x: style.margin, y: cursorY + 12),
                font: bodyFont(size: 9, spec: style.spec),
                color: .darkGray,
                width: style.width
            )
            cursorY += 28
        }
        return cursorY
    }

    // MARK: - Heart rate chart

    private static func drawHeartRateChart(
        _ heartRate: WorkoutPdfHeartRate,
        at startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        fontSpec: PdfFontSpec
    ) -> CGFloat {
        let height: CGFloat = 110
        let chart = CGRect(x: margin, y: startY, width: width, height: height)
        UIColor(white: 0.96, alpha: 1).setFill()
        UIBezierPath(rect: chart).fill()

        for band in heartRate.zoneBands {
            let top = chart.maxY - CGFloat(band.upperY) * height
            let bottom = chart.maxY - CGFloat(band.lowerY) * height
            let bandRect = CGRect(x: chart.minX, y: top, width: width, height: max(1, bottom - top))
            colorFromArgb(band.colorArgb).withAlphaComponent(0.18).setFill()
            UIBezierPath(rect: bandRect).fill()
        }

        let path = UIBezierPath()
        for (index, point) in heartRate.chartPoints.enumerated() {
            let pointX = chart.minX + CGFloat(point.x) * width
            let pointY = chart.maxY - CGFloat(point.y) * height
            if index == 0 {
                path.move(to: CGPoint(x: pointX, y: pointY))
            } else {
                path.addLine(to: CGPoint(x: pointX, y: pointY))
            }
        }
        UIColor(red: 0.85, green: 0.2, blue: 0.25, alpha: 1).setStroke()
        path.lineWidth = 1.5
        path.stroke()

        drawText(
            String(localized: "pdf_field_hr_chart") as NSString,
            at: CGPoint(x: margin, y: chart.maxY + 4),
            font: bodyFont(size: 10, spec: fontSpec),
            color: .darkGray,
            width: width
        )
        return chart.maxY + 20
    }
}
