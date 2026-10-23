import Shared
import UIKit

/// Page layout helpers for [WorkoutPdfRenderer].
enum WorkoutPdfDrawing {
    @discardableResult
    static func drawChrome(
        context _: CGContext,
        pageRect: CGRect,
        document: WorkoutPdfDocument,
        pageIndex: Int,
        pageCount: Int
    ) -> CGFloat {
        var cursorY: CGFloat = 36
        let margin: CGFloat = 40
        let width = pageRect.width - margin * 2
        cursorY = drawChromeHeader(
            document: document,
            margin: margin,
            width: width,
            cursorY: cursorY
        )
        drawChromeFooter(
            document: document,
            pageRect: pageRect,
            margin: margin,
            width: width,
            pageIndex: pageIndex,
            pageCount: pageCount
        )
        return cursorY + 8
    }

    static func drawWorkout(
        _ workout: WorkoutPdfPage,
        startY: CGFloat,
        pageRect: CGRect,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        let margin: CGFloat = 40
        let width = pageRect.width - margin * 2
        let fonts = sectionFonts(document: document)
        var cursorY = drawHeaderAndMetrics(
            workout,
            cursorY: startY,
            margin: margin,
            width: width,
            fonts: fonts
        )
        cursorY = drawHeartRateSection(
            workout.heartRate,
            cursorY: cursorY,
            margin: margin,
            width: width,
            document: document,
            body: fonts.body
        )
        cursorY = drawCardioSection(
            workout.cardio,
            cursorY: cursorY,
            margin: margin,
            width: width,
            body: fonts.body
        )
        cursorY = drawStrengthSection(
            workout.strength,
            cursorY: cursorY,
            margin: margin,
            width: width,
            fonts: fonts
        )
        cursorY = drawSegmentsSection(
            workout.segments,
            cursorY: cursorY,
            margin: margin,
            width: width,
            fonts: fonts
        )
        return drawMuscleMapSection(
            workout.muscleMap,
            cursorY: cursorY,
            margin: margin,
            width: width,
            document: document
        )
    }

    struct SectionFonts {
        var body: UIFont
        var bold: UIFont
    }

    static func sectionFonts(document: WorkoutPdfDocument) -> SectionFonts {
        SectionFonts(
            body: WorkoutPdfRenderer.bodyFont(size: 11, spec: document.fontSpec),
            bold: WorkoutPdfRenderer.boldFont(size: 11, spec: document.fontSpec)
        )
    }
}

// MARK: - Chrome

private extension WorkoutPdfDrawing {
    static func drawChromeHeader(
        document: WorkoutPdfDocument,
        margin: CGFloat,
        width: CGFloat,
        cursorY startY: CGFloat
    ) -> CGFloat {
        var cursorY = startY
        WorkoutPdfRenderer.drawText(
            document.brandTitle as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: WorkoutPdfRenderer.boldFont(size: 22, spec: document.fontSpec),
            color: UIColor(red: 0.12, green: 0.45, blue: 0.38, alpha: 1),
            width: width
        )
        cursorY += 28
        WorkoutPdfRenderer.drawText(
            document.reportTitle as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: WorkoutPdfRenderer.boldFont(size: 16, spec: document.fontSpec),
            color: .black,
            width: width
        )
        cursorY += 22
        WorkoutPdfRenderer.drawText(
            document.generatedAtIso as NSString,
            at: CGPoint(x: margin, y: cursorY),
            font: WorkoutPdfRenderer.bodyFont(size: 10, spec: document.fontSpec),
            color: .darkGray,
            width: width
        )
        return cursorY + 18
    }

    static func drawChromeFooter(
        document: WorkoutPdfDocument,
        pageRect: CGRect,
        margin: CGFloat,
        width: CGFloat,
        pageIndex: Int,
        pageCount: Int
    ) {
        let footer = document.disclaimer as NSString
        let footerFont = WorkoutPdfRenderer.bodyFont(size: 8, spec: document.fontSpec)
        let footerHeight = footer.boundingRect(
            with: CGSize(width: width, height: 80),
            options: [.usesLineFragmentOrigin],
            attributes: [.font: footerFont],
            context: nil
        ).height
        WorkoutPdfRenderer.drawText(
            footer,
            at: CGPoint(x: margin, y: pageRect.height - 28 - footerHeight),
            font: footerFont,
            color: .gray,
            width: width
        )
        let pageLabel = "\(pageIndex + 1) / \(pageCount)" as NSString
        WorkoutPdfRenderer.drawText(
            pageLabel,
            at: CGPoint(x: pageRect.width - margin - 40, y: pageRect.height - 24),
            font: WorkoutPdfRenderer.bodyFont(size: 9, spec: document.fontSpec),
            color: .gray,
            width: 40
        )
    }
}

