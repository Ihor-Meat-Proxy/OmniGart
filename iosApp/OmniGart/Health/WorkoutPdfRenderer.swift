import CoreText
import Shared
import UIKit

/// Renders a [WorkoutPdfDocument] with UIGraphicsPDFRenderer using bundled Noto fonts.
enum WorkoutPdfRenderer {
    static func render(document: WorkoutPdfDocument) -> Data {
        registerFontsIfNeeded()
        let pageRect = CGRect(x: 0, y: 0, width: 612, height: 792) // US Letter
        let renderer = UIGraphicsPDFRenderer(bounds: pageRect)
        return renderer.pdfData { context in
            if document.workouts.isEmpty {
                context.beginPage()
                drawChrome(
                    context: context.cgContext,
                    pageRect: pageRect,
                    document: document,
                    pageIndex: 0,
                    pageCount: 1
                )
                return
            }
            let pages = max(document.workouts.count, 1)
            for (index, workout) in document.workouts.enumerated() {
                context.beginPage()
                var y = drawChrome(
                    context: context.cgContext,
                    pageRect: pageRect,
                    document: document,
                    pageIndex: index,
                    pageCount: pages
                )
                y = drawWorkout(workout, startY: y, pageRect: pageRect, document: document)
                _ = y
            }
        }
    }

    static func share(
        document: WorkoutPdfDocument,
        from controller: UIViewController,
        fileName: String = "omnigart-workout.pdf"
    ) {
        let data = render(document: document)
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        try? data.write(to: url, options: .atomic)
        let activity = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        if let pop = activity.popoverPresentationController {
            pop.sourceView = controller.view
            pop.sourceRect = CGRect(x: controller.view.bounds.midX, y: controller.view.bounds.midY, width: 1, height: 1)
        }
        controller.present(activity, animated: true)
    }

    // MARK: - Drawing

    @discardableResult
    private static func drawChrome(
        context: CGContext,
        pageRect: CGRect,
        document: WorkoutPdfDocument,
        pageIndex: Int,
        pageCount: Int
    ) -> CGFloat {
        var y: CGFloat = 36
        let margin: CGFloat = 40
        let width = pageRect.width - margin * 2

        drawText(
            document.brandTitle as NSString,
            at: CGPoint(x: margin, y: y),
            font: boldFont(size: 22, spec: document.fontSpec),
            color: UIColor(red: 0.12, green: 0.45, blue: 0.38, alpha: 1),
            width: width
        )
        y += 28
        drawText(
            document.reportTitle as NSString,
            at: CGPoint(x: margin, y: y),
            font: boldFont(size: 16, spec: document.fontSpec),
            color: .black,
            width: width
        )
        y += 22
        drawText(
            document.generatedAtIso as NSString,
            at: CGPoint(x: margin, y: y),
            font: bodyFont(size: 10, spec: document.fontSpec),
            color: .darkGray,
            width: width
        )
        y += 18

        // Footer disclaimer
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
        return y + 8
    }

    private static func drawWorkout(
        _ workout: WorkoutPdfPage,
        startY: CGFloat,
        pageRect: CGRect,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        let margin: CGFloat = 40
        let width = pageRect.width - margin * 2
        var y = startY
        let body = bodyFont(size: 11, spec: document.fontSpec)
        let bold = boldFont(size: 11, spec: document.fontSpec)

        for line in [workout.header.dateLabel, workout.header.activityLabel, workout.header.durationLabel] {
            drawText(line as NSString, at: CGPoint(x: margin, y: y), font: bold, color: .black, width: width)
            y += 16
        }
        y += 4
        for line in [
            workout.metrics.activeKcalLabel,
            workout.metrics.totalKcalLabel,
            workout.metrics.avgHrLabel,
            workout.metrics.maxHrLabel
        ].compactMap({ $0 }) {
            drawText(line as NSString, at: CGPoint(x: margin, y: y), font: body, color: .black, width: width)
            y += 15
        }

        if let hr = workout.heartRate {
            y += 8
            y = drawHeartRateChart(hr, at: y, margin: margin, width: width, document: document)
            for line in hr.timeInZoneLines {
                drawText(line as NSString, at: CGPoint(x: margin, y: y), font: body, color: .darkGray, width: width)
                y += 14
            }
        }

        if let cardio = workout.cardio {
            y += 6
            for line in [cardio.distanceLabel, cardio.paceLabel, cardio.cadenceLabel].compactMap({ $0 }) {
                drawText(line as NSString, at: CGPoint(x: margin, y: y), font: body, color: .black, width: width)
                y += 14
            }
        }

        if let strength = workout.strength {
            y += 8
            drawText(
                strength.totalWeightLabel as NSString,
                at: CGPoint(x: margin, y: y),
                font: bold,
                color: .black,
                width: width
            )
            y += 16
            for exercise in strength.exercises {
                drawText(
                    exercise.title as NSString,
                    at: CGPoint(x: margin, y: y),
                    font: bold,
                    color: .black,
                    width: width
                )
                y += 14
                for setLine in exercise.setLines {
                    drawText(
                        setLine as NSString,
                        at: CGPoint(x: margin + 12, y: y),
                        font: body,
                        color: .darkGray,
                        width: width - 12
                    )
                    y += 13
                }
            }
        }

        if !workout.segments.isEmpty {
            y += 8
            drawText(
                String(localized: "pdf_field_segments") as NSString,
                at: CGPoint(x: margin, y: y),
                font: bold,
                color: .black,
                width: width
            )
            y += 15
            for segment in workout.segments {
                drawText(
                    "\(segment.label): \(segment.detail)" as NSString,
                    at: CGPoint(x: margin, y: y),
                    font: body,
                    color: .black,
                    width: width
                )
                y += 14
            }
        }

        if !workout.muscleMap.isEmpty {
            y += 8
            drawText(
                String(localized: "pdf_field_muscle_map") as NSString,
                at: CGPoint(x: margin, y: y),
                font: bold,
                color: .black,
                width: width
            )
            y += 16
            for bar in workout.muscleMap {
                let barWidth = width * CGFloat(bar.intensity)
                let rect = CGRect(x: margin, y: y, width: barWidth, height: 10)
                UIColor(red: 0.12, green: 0.45, blue: 0.38, alpha: 0.35 + 0.65 * CGFloat(bar.intensity))
                    .setFill()
                UIBezierPath(roundedRect: rect, cornerRadius: 2).fill()
                drawText(
                    bar.displayName as NSString,
                    at: CGPoint(x: margin, y: y + 12),
                    font: bodyFont(size: 9, spec: document.fontSpec),
                    color: .darkGray,
                    width: width
                )
                y += 28
            }
        }
        return y
    }

    private static func drawHeartRateChart(
        _ hr: WorkoutPdfHeartRate,
        at startY: CGFloat,
        margin: CGFloat,
        width: CGFloat,
        document: WorkoutPdfDocument
    ) -> CGFloat {
        let height: CGFloat = 110
        let chart = CGRect(x: margin, y: startY, width: width, height: height)
        UIColor(white: 0.96, alpha: 1).setFill()
        UIBezierPath(rect: chart).fill()

        for band in hr.zoneBands {
            let y0 = chart.maxY - CGFloat(band.upperY) * height
            let y1 = chart.maxY - CGFloat(band.lowerY) * height
            let bandRect = CGRect(x: chart.minX, y: y0, width: width, height: max(1, y1 - y0))
            colorFromArgb(band.colorArgb).withAlphaComponent(0.18).setFill()
            UIBezierPath(rect: bandRect).fill()
        }

        let path = UIBezierPath()
        for (index, point) in hr.chartPoints.enumerated() {
            let x = chart.minX + CGFloat(point.x) * width
            let y = chart.maxY - CGFloat(point.y) * height
            if index == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }
        UIColor(red: 0.85, green: 0.2, blue: 0.25, alpha: 1).setStroke()
        path.lineWidth = 1.5
        path.stroke()

        drawText(
            String(localized: "pdf_field_hr_chart") as NSString,
            at: CGPoint(x: margin, y: chart.maxY + 4),
            font: bodyFont(size: 10, spec: document.fontSpec),
            color: .darkGray,
            width: width
        )
        return chart.maxY + 20
    }

    private static func drawText(
        _ text: NSString,
        at point: CGPoint,
        font: UIFont,
        color: UIColor,
        width: CGFloat
    ) {
        let attrs: [NSAttributedString.Key: Any] = [
            .font: font,
            .foregroundColor: color
        ]
        text.draw(
            with: CGRect(x: point.x, y: point.y, width: width, height: 200),
            options: [.usesLineFragmentOrigin, .truncatesLastVisibleLine],
            attributes: attrs,
            context: nil
        )
    }

    private static func colorFromArgb(_ argb: Int64) -> UIColor {
        let a = CGFloat((argb >> 24) & 0xFF) / 255
        let r = CGFloat((argb >> 16) & 0xFF) / 255
        let g = CGFloat((argb >> 8) & 0xFF) / 255
        let b = CGFloat(argb & 0xFF) / 255
        return UIColor(red: r, green: g, blue: b, alpha: a)
    }

    private static func bodyFont(size: CGFloat, spec: PdfFontSpec) -> UIFont {
        if spec.usePlatformCjkCascade, let cjk = UIFont(name: "PingFangSC-Regular", size: size) {
            return cjk
        }
        if let georgian = spec.georgianFamily, let font = UIFont(name: georgian, size: size) {
            return font
        }
        return UIFont(name: "NotoSans", size: size) ?? .systemFont(ofSize: size)
    }

    private static func boldFont(size: CGFloat, spec: PdfFontSpec) -> UIFont {
        if spec.usePlatformCjkCascade, let cjk = UIFont(name: "PingFangSC-Semibold", size: size) {
            return cjk
        }
        return UIFont(name: "NotoSans-Bold", size: size)
            ?? UIFont(name: "NotoSans", size: size)
            ?? .boldSystemFont(ofSize: size)
    }

    private static var fontsRegistered = false
    private static func registerFontsIfNeeded() {
        guard !fontsRegistered else { return }
        fontsRegistered = true
        let names = ["NotoSans-Regular", "NotoSans-Bold", "NotoSansGeorgian-Regular"]
        for name in names {
            guard let url = Bundle.main.url(forResource: name, withExtension: "ttf", subdirectory: "Fonts")
                ?? Bundle.main.url(forResource: name, withExtension: "ttf")
            else { continue }
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }
}
