import Shared
import UIKit

/// Strength, segment, and muscle-map sections for workout PDF pages.
enum WorkoutPdfExtrasDrawing {
    static func drawStrength(
        _ strength: WorkoutPdfStrength,
        yPos: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        body: UIFont,
        bold: UIFont
    ) -> CGFloat {
        var cursor = yPos + 8
        WorkoutPdfRenderSupport.drawText(
            strength.totalWeightLabel as NSString,
            at: CGPoint(x: margin, y: cursor),
            font: bold,
            color: .black,
            width: width
        )
        cursor += 16
        for exercise in strength.exercises {
            WorkoutPdfRenderSupport.drawText(
                exercise.title as NSString,
                at: CGPoint(x: margin, y: cursor),
                font: bold,
                color: .black,
                width: width
            )
            cursor += 14
            for setLine in exercise.setLines {
                WorkoutPdfRenderSupport.drawText(
                    setLine as NSString,
                    at: CGPoint(x: margin + 12, y: cursor),
                    font: body,
                    color: .darkGray,
                    width: width - 12
                )
                cursor += 13
            }
        }
        return cursor
    }

    static func drawSegments(
        _ segments: [WorkoutPdfSegment],
        yPos: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        body: UIFont,
        bold: UIFont
    ) -> CGFloat {
        var cursor = yPos + 8
        WorkoutPdfRenderSupport.drawText(
            String(localized: "pdf_field_segments") as NSString,
            at: CGPoint(x: margin, y: cursor),
            font: bold,
            color: .black,
            width: width
        )
        cursor += 15
        for segment in segments {
            WorkoutPdfRenderSupport.drawText(
                "\(segment.label): \(segment.detail)" as NSString,
                at: CGPoint(x: margin, y: cursor),
                font: body,
                color: .black,
                width: width
            )
            cursor += 14
        }
        return cursor
    }

    static func drawMuscleMap(
        _ muscleMap: [WorkoutPdfMuscleBar],
        yPos: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        document: WorkoutPdfDocument,
        bold: UIFont
    ) -> CGFloat {
        var cursor = yPos + 8
        WorkoutPdfRenderSupport.drawText(
            String(localized: "pdf_field_muscle_map") as NSString,
            at: CGPoint(x: margin, y: cursor),
            font: bold,
            color: .black,
            width: width
        )
        cursor += 16
        let small = WorkoutPdfRenderSupport.bodyFont(size: 9, spec: document.fontSpec)
        for bar in muscleMap {
            let barWidth = width * CGFloat(bar.intensity)
            let rect = CGRect(x: margin, y: cursor, width: barWidth, height: 10)
            UIColor(red: 0.12, green: 0.45, blue: 0.38, alpha: 0.35 + 0.65 * CGFloat(bar.intensity))
                .setFill()
            UIBezierPath(roundedRect: rect, cornerRadius: 2).fill()
            WorkoutPdfRenderSupport.drawText(
                bar.displayName as NSString,
                at: CGPoint(x: margin, y: cursor + 12),
                font: small,
                color: .darkGray,
                width: width
            )
            cursor += 28
        }
        return cursor
    }
}
