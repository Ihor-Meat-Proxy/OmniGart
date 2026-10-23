import Shared
import UIKit

// MARK: - Workout sections

extension WorkoutPdfDrawing {
    static func drawHeaderAndMetrics(
        _ workout: WorkoutPdfPage,
        cursorY startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        fonts: SectionFonts
    ) -> CGFloat {
        var cursorY = startY
        for line in [workout.header.dateLabel, workout.header.activityLabel, workout.header.durationLabel] {
            WorkoutPdfRenderer.drawText(
                line as NSString,
                at: CGPoint(x: margin, y: cursorY),
                font: fonts.bold,
                color: .black,
                width: width
            )
            cursorY += 16
        }
        cursorY += 4
        for line in [
            workout.metrics.activeKcalLabel,
            workout.metrics.totalKcalLabel,
            workout.metrics.avgHrLabel,
            workout.metrics.maxHrLabel
        ].compactMap({ $0 }) {
            WorkoutPdfRenderer.drawText(
                line as NSString,
                at: CGPoint(x: margin, y: cursorY),
                font: fonts.body,
                color: .black,
                width: width
            )
            cursorY += 15
        }
        return cursorY
    }

    static func drawHeartRateSection(
        _ heartRate: WorkoutPdfHeartRate?,
        cursorY startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        guard let heartRate else { return startY }
        let body = sectionFonts(document: document).body
        var cursorY = startY + 8
        cursorY = drawHeartRateChart(
            heartRate,
            at: cursorY,
            margin: margin,
            width: width,
            document: document
        )
        let body = WorkoutPdfRenderer.bodyFont(size: 11, spec: document.fontSpec)
        for line in heartRate.timeInZoneLines {
            WorkoutPdfRenderer.drawText(
                line as NSString,
                at: CGPoint(x: margin, y: cursorY),
                font: body,
                color: .darkGray,
                width: width
            )
            cursorY += 14
        }
        return cursorY
    }

    static func drawCardioSection(
        _ cardio: WorkoutPdfCardio?,
        cursorY startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        body: UIFont
    ) -> CGFloat {
        guard let cardio else { return startY }
        var cursorY = startY + 6
        for line in [cardio.distanceLabel, cardio.paceLabel, cardio.cadenceLabel].compactMap({ $0 }) {
            WorkoutPdfRenderer.drawText(
                line as NSString,
                at: CGPoint(x: margin, y: cursorY),
                font: body,
                color: .black,
                width: width
            )
            cursorY += 14
        }
        return cursorY
    }

    static func drawStrengthSection(
        _ strength: WorkoutPdfStrength?,
        cursorY startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        fonts: SectionFonts
    ) -> CGFloat {
        guard let strength else { return startY }
        var cursorY = startY + 8
        WorkoutPdfRenderer.drawText(
            strength.totalWeightLabel as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: fonts.bold,
            color: .black,
            width: width
        )
        cursorY += 16
        for exercise in strength.exercises {
            WorkoutPdfRenderer.drawText(
                exercise.title as NSString,
                at: CGPoint(x: margin, y: cursorY),
                font: fonts.bold,
                color: .black,
                width: width
            )
            cursorY += 14
            for setLine in exercise.setLines {
                WorkoutPdfRenderer.drawText(
                    setLine as NSString,
                    at: CGPoint(x: margin + 12, y: cursorY),
                    font: fonts.body,
                    color: .darkGray,
                    width: width - 12
                )
                cursorY += 13
            }
        }
        return cursorY
    }

    static func drawSegmentsSection(
        _ segments: [WorkoutPdfSegment],
        cursorY startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        fonts: SectionFonts
    ) -> CGFloat {
        guard !segments.isEmpty else { return startY }
        var cursorY = startY + 8
        WorkoutPdfRenderer.drawText(
            String(localized: "pdf_field_segments") as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: fonts.bold,
            color: .black,
            width: width
        )
        cursorY += 15
        for segment in segments {
            WorkoutPdfRenderer.drawText(
                "\(segment.label): \(segment.detail)" as NSString,
                at: CGPoint(x: margin, y: cursorY),
                font: fonts.body,
                color: .black,
                width: width
            )
            cursorY += 14
        }
        return cursorY
    }

    static func drawMuscleMapSection(
        _ muscleMap: [WorkoutPdfMuscleBar],
        cursorY startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        guard !muscleMap.isEmpty else { return startY }
        var cursorY = startY + 8
        let body = WorkoutPdfRenderer.bodyFont(size: 9, spec: document.fontSpec)
        WorkoutPdfRenderer.drawText(
            String(localized: "pdf_field_muscle_map") as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: WorkoutPdfRenderer.boldFont(size: 11, spec: document.fontSpec),
            color: .black,
            width: width
        )
        cursorY += 16
        for bar in muscleMap {
            let barWidth = width * CGFloat(bar.intensity)
            let rect = CGRect(x: margin, y: cursorY, width: barWidth, height: 10)
            let fillAlpha = 0.35 + 0.65 * CGFloat(bar.intensity)
            UIColor(red: 0.12, green: 0.45, blue: 0.38, alpha: fillAlpha).setFill()
            UIBezierPath(roundedRect: rect, cornerRadius: 2).fill()
            WorkoutPdfRenderer.drawText(
                bar.displayName as NSString,
                at: CGPoint(x: margin, y: cursorY + 12),
                font: body,
                color: .darkGray,
                width: width
            )
            cursorY += 28
        }
        return cursorY
    }
}

// MARK: - Heart rate chart

extension WorkoutPdfDrawing {
    static func drawHeartRateChart(
        _ heartRate: WorkoutPdfHeartRate,
        at startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        let height: CGFloat = 110
        let chart = CGRect(x: margin, y: startY, width: width, height: height)
        UIColor(white: 0.96, alpha: 1).setFill()
        UIBezierPath(rect: chart).fill()
        fillZoneBands(heartRate.zoneBands, chart: chart, width: width, height: height)
        strokeHeartRatePath(heartRate.chartPoints, chart: chart, width: width, height: height)
        WorkoutPdfRenderer.drawText(
            String(localized: "pdf_field_hr_chart") as NSString,
            at: CGPoint(x: margin, y: chart.maxY + 4),
            font: WorkoutPdfRenderer.bodyFont(size: 10, spec: document.fontSpec),
            color: .darkGray,
            width: width
        )
        return chart.maxY + 20
    }

    static func fillZoneBands(
        _ bands: [PdfZoneBand],
        chart: CGRect,
        width: CGFloat,
        height: CGFloat
    ) {
        for band in bands {
            let bandTop = chart.maxY - CGFloat(band.upperY) * height
            let bandBottom = chart.maxY - CGFloat(band.lowerY) * height
            let bandRect = CGRect(
                x: chart.minX,
                y: bandTop,
                width: width,
                height: max(1, bandBottom - bandTop)
            )
            WorkoutPdfRenderer.colorFromArgb(band.colorArgb).withAlphaComponent(0.18).setFill()
            UIBezierPath(rect: bandRect).fill()
        }
    }

    static func strokeHeartRatePath(
        _ points: [PdfChartPoint],
        chart: CGRect,
        width: CGFloat,
        height: CGFloat
    ) {
        let path = UIBezierPath()
        for (index, point) in points.enumerated() {
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
    }
}
