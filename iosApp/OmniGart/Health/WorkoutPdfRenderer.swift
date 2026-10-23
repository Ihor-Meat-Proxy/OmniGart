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
                WorkoutPdfDrawing.drawChrome(
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
                let cursorY = WorkoutPdfDrawing.drawChrome(
                    context: context.cgContext,
                    pageRect: pageRect,
                    document: document,
                    pageIndex: index,
                    pageCount: pages
                )
                _ = WorkoutPdfDrawing.drawWorkout(
                    workout,
                    startY: cursorY,
                    pageRect: pageRect,
                    document: document
                )
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
            pop.sourceRect = CGRect(
                x: controller.view.bounds.midX,
                y: controller.view.bounds.midY,
                width: 1,
                height: 1
            )
        }
        controller.present(activity, animated: true)
    }

    static func bodyFont(size: CGFloat, spec: PdfFontSpec) -> UIFont {
        if spec.usePlatformCjkCascade, let cjk = UIFont(name: "PingFangSC-Regular", size: size) {
            return cjk
        }
        if let georgian = spec.georgianFamily, let font = UIFont(name: georgian, size: size) {
            return font
        }
        return UIFont(name: "NotoSans", size: size) ?? .systemFont(ofSize: size)
    }

    static func boldFont(size: CGFloat, spec: PdfFontSpec) -> UIFont {
        if spec.usePlatformCjkCascade, let cjk = UIFont(name: "PingFangSC-Semibold", size: size) {
            return cjk
        }
        return UIFont(name: "NotoSans-Bold", size: size)
            ?? UIFont(name: "NotoSans", size: size)
            ?? .boldSystemFont(ofSize: size)
    }

    static func colorFromArgb(_ argb: Int64) -> UIColor {
        let alpha = CGFloat((argb >> 24) & 0xFF) / 255
        let red = CGFloat((argb >> 16) & 0xFF) / 255
        let green = CGFloat((argb >> 8) & 0xFF) / 255
        let blue = CGFloat(argb & 0xFF) / 255
        return UIColor(red: red, green: green, blue: blue, alpha: alpha)
    }

    static func drawText(
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
