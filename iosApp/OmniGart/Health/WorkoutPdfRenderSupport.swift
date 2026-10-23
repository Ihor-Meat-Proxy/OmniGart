import CoreText
import Shared
import UIKit

/// Shared fonts/text helpers for PDF rendering.
enum WorkoutPdfRenderSupport {
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

    static func registerFontsIfNeeded(fontsRegistered: inout Bool) {
        guard !fontsRegistered else { return }
        fontsRegistered = true
        let names = ["NotoSans-Regular", "NotoSans-Bold", "NotoSansGeorgian-Regular"]
        for name in names {
            guard let url = Bundle.main.url(
                forResource: name,
                withExtension: "ttf",
                subdirectory: "Fonts"
            ) ?? Bundle.main.url(forResource: name, withExtension: "ttf")
            else { continue }
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }

    @discardableResult
    static func drawChrome(
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
            pageIndex: pageIndex,
            pageCount: pageCount,
            margin: margin,
            width: width
        )
        return cursorY + 8
    }

    private static func drawChromeHeader(
        document: WorkoutPdfDocument,
        margin: CGFloat,
        width: CGFloat,
        cursorY: CGFloat
    ) -> CGFloat {
        var yPos = cursorY
        drawText(
            document.brandTitle as NSString,
            at: CGPoint(x: margin, y: yPos),
            font: boldFont(size: 22, spec: document.fontSpec),
            color: UIColor(red: 0.12, green: 0.45, blue: 0.38, alpha: 1),
            width: width
        )
        yPos += 28
        drawText(
            document.reportTitle as NSString,
            at: CGPoint(x: margin, y: yPos),
            font: boldFont(size: 16, spec: document.fontSpec),
            color: .black,
            width: width
        )
        yPos += 22
        drawText(
            document.generatedAtIso as NSString,
            at: CGPoint(x: margin, y: yPos),
            font: bodyFont(size: 10, spec: document.fontSpec),
            color: .darkGray,
            width: width
        )
        return yPos + 18
    }

    private static func drawChromeFooter(
        document: WorkoutPdfDocument,
        pageRect: CGRect,
        pageIndex: Int,
        pageCount: Int,
        margin: CGFloat,
        width: CGFloat
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
}
