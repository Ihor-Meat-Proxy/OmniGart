import CoreText
import Shared
import UIKit

extension WorkoutPdfRenderer {
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

    static func colorFromArgb(_ argb: Int64) -> UIColor {
        let alpha = CGFloat((argb >> 24) & 0xFF) / 255
        let red = CGFloat((argb >> 16) & 0xFF) / 255
        let green = CGFloat((argb >> 8) & 0xFF) / 255
        let blue = CGFloat(argb & 0xFF) / 255
        return UIColor(red: red, green: green, blue: blue, alpha: alpha)
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

    static func registerFontsIfNeeded() {
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
