import SwiftUI
import Shared

@main
struct OmniGartApp: App {
    private let summary: [String]

    init() {
        let core = OmniGartCore(driverFactory: DatabaseDriverFactory())
        let profile = core.ensureDemoProfile()
        let lines = core.summaryLines(profile: profile)
        var mapped: [String] = []
        let count = Int(lines.size)
        if count > 0 {
            for index in 0..<count {
                if let line = lines.get(index: Int32(index)) as String? {
                    mapped.append(line)
                }
            }
        }
        summary = mapped.isEmpty ? ["OmniGart"] : mapped
    }

    var body: some Scene {
        WindowGroup {
            PhoneRootView(summaryLines: summary)
        }
    }
}
