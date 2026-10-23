import Shared
import UIKit

/// Renders a [WorkoutPdfDocument] with UIGraphicsPDFRenderer using bundled Noto fonts.
enum WorkoutPdfRenderer {
    static func render(document: WorkoutPdfDocument) -> Data {
        WorkoutPdfRenderSupport.registerFontsIfNeeded(fontsRegistered: &fontsRegistered)
        let pageRect = CGRect(x: 0, y: 0, width: 612, height: 792) // US Letter
        let renderer = UIGraphicsPDFRenderer(bounds: pageRect)
        return renderer.pdfData { context in
            if document.workouts.isEmpty {
                context.beginPage()
                _ = WorkoutPdfRenderSupport.drawChrome(
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
                var cursorY = WorkoutPdfRenderSupport.drawChrome(
                    pageRect: pageRect,
                    document: document,
                    pageIndex: index,
                    pageCount: pages
                )
                cursorY = WorkoutPdfPageDrawing.drawWorkout(
                    workout,
                    startY: cursorY,
                    pageRect: pageRect,
                    document: document
                )
                _ = cursorY
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

    private static var fontsRegistered = false
}
