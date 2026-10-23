import Shared
import SwiftUI
import UIKit

/// SwiftUI helpers to build and share OmniGart workout PDFs.
enum WorkoutPdfExport {
    static func localeTag() -> String {
        Locale.current.identifier.replacingOccurrences(of: "_", with: "-")
    }

    static func shareSingle(
        core: OmniGartCore,
        workoutId: String,
        strengthJson: String? = nil
    ) {
        guard let document = core.localHealth.buildWorkoutPdf(
            workoutId: workoutId,
            locale: localeTag(),
            hrMaxBpm: 190,
            hrRestBpm: 60,
            strengthJson: strengthJson
        ) else { return }
        presentShare(document: document, fileName: "omnigart-\(workoutId).pdf")
    }

    static func shareLatest(core: OmniGartCore) {
        guard let id = core.localHealth.latestWorkoutId() else { return }
        shareSingle(core: core, workoutId: id)
    }

    static func shareRange(
        core: OmniGartCore,
        start: Date,
        end: Date
    ) {
        let document = core.localHealth.buildRangePdf(
            startEpochMs: Int64(start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(end.timeIntervalSince1970 * 1000),
            locale: localeTag(),
            hrMaxBpm: 190,
            hrRestBpm: 60
        )
        presentShare(document: document, fileName: "omnigart-workouts.pdf")
    }

    private static func presentShare(document: WorkoutPdfDocument, fileName: String) {
        let data = WorkoutPdfRenderer.render(document: document)
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        do {
            try data.write(to: url, options: .atomic)
        } catch {
            return
        }
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        let root = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        guard let presenter = topController(from: root) else { return }
        let activity = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        if let pop = activity.popoverPresentationController {
            pop.sourceView = presenter.view
            pop.sourceRect = CGRect(
                x: presenter.view.bounds.midX,
                y: presenter.view.bounds.midY,
                width: 1,
                height: 1
            )
            pop.permittedArrowDirections = []
        }
        presenter.present(activity, animated: true)
    }

    private static func topController(from root: UIViewController?) -> UIViewController? {
        if let nav = root as? UINavigationController {
            return topController(from: nav.visibleViewController)
        }
        if let tab = root as? UITabBarController {
            return topController(from: tab.selectedViewController)
        }
        if let presented = root?.presentedViewController {
            return topController(from: presented)
        }
        return root
    }
}
