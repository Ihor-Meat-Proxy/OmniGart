import Shared
import UIKit

/// Per-workout page drawing for PDF export.
enum WorkoutPdfPageDrawing {
    static func drawWorkout(
        _ workout: WorkoutPdfPage,
        startY: CGFloat,
        pageRect: CGRect,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        let margin: CGFloat = 40
        let width = pageRect.width - margin * 2
        var cursorY = startY
        let body = WorkoutPdfRenderSupport.bodyFont(size: 11, spec: document.fontSpec)
        let bold = WorkoutPdfRenderSupport.boldFont(size: 11, spec: document.fontSpec)

        cursorY = drawHeaderMetrics(
            workout,
            cursorY: cursorY,
            margin: margin,
            width: width,
            body: body,
            bold: bold
        )
        if let hr = workout.heartRate {
            cursorY += 8
            cursorY = WorkoutPdfHrDrawing.drawHeartRateChart(
                hr,
                at: cursorY,
                margin: margin,
                width: width,
                document: document
            )
            for line in hr.timeInZoneLines {
                WorkoutPdfRenderSupport.drawText(
                    line as NSString,
                    at: CGPoint(x: margin, y: cursorY),
                    font: body,
                    color: .darkGray,
                    width: width
                )
                cursorY += 14
            }
        }
        return drawOptionalSections(
            workout,
            cursorY: cursorY,
            margin: margin,
            width: width,
            body: body,
            bold: bold,
            document: document
        )
    }

    private static func drawHeaderMetrics(
        _ workout: WorkoutPdfPage,
        cursorY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        body: UIFont,
        bold: UIFont
    ) -> CGFloat {
        var yPos = cursorY
        for line in [workout.header.dateLabel, workout.header.activityLabel, workout.header.durationLabel] {
            WorkoutPdfRenderSupport.drawText(
                line as NSString,
                at: CGPoint(x: margin, y: yPos),
                font: bold,
                color: .black,
                width: width
            )
            yPos += 16
        }
        yPos += 4
        for line in [
            workout.metrics.activeKcalLabel,
            workout.metrics.totalKcalLabel,
            workout.metrics.avgHrLabel,
            workout.metrics.maxHrLabel
        ].compactMap({ $0 }) {
            WorkoutPdfRenderSupport.drawText(
                line as NSString,
                at: CGPoint(x: margin, y: yPos),
                font: body,
                color: .black,
                width: width
            )
            yPos += 15
        }
        return yPos
    }

    private static func drawOptionalSections(
        _ workout: WorkoutPdfPage,
        cursorY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        body: UIFont,
        bold: UIFont,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        var yPos = cursorY
        if let cardio = workout.cardio {
            yPos = drawCardio(cardio, yPos: yPos, margin: margin, width: width, body: body)
        }
        if let strength = workout.strength {
            yPos = WorkoutPdfExtrasDrawing.drawStrength(
                strength,
                yPos: yPos,
                margin: margin,
                width: width,
                body: body,
                bold: bold
            )
        }
        if !workout.segments.isEmpty {
            yPos = WorkoutPdfExtrasDrawing.drawSegments(
                workout.segments,
                yPos: yPos,
                margin: margin,
                width: width,
                body: body,
                bold: bold
            )
        }
        if !workout.muscleMap.isEmpty {
            yPos = WorkoutPdfExtrasDrawing.drawMuscleMap(
                workout.muscleMap,
                yPos: yPos,
                margin: margin,
                width: width,
                document: document,
                bold: bold
            )
        }
        return yPos
    }

    private static func drawCardio(
        _ cardio: WorkoutPdfCardio,
        yPos: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        body: UIFont
    ) -> CGFloat {
        var cursor = yPos + 6
        for line in [cardio.distanceLabel, cardio.paceLabel, cardio.cadenceLabel].compactMap({ $0 }) {
            WorkoutPdfRenderSupport.drawText(
                line as NSString,
                at: CGPoint(x: margin, y: cursor),
                font: body,
                color: .black,
                width: width
            )
            cursor += 14
        }
        return cursor
    }
}
