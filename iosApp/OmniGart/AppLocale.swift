import Foundation
import Shared

/// Device locale resolved to a shipped OmniGart catalog tag (never Russian).
enum AppLocale {
    static var tag: String {
        let raw = Locale.current.identifier.replacingOccurrences(of: "_", with: "-")
        return StringCatalog.shared.resolveLocale(locale: raw)
    }
}
