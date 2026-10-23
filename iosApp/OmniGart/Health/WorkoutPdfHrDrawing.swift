import Shared
import UIKit

/// Heart-rate chart drawing for workout PDF pages.
enum WorkoutPdfHrDrawing {
    static func drawHeartRateChart(
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
        drawZoneBands(hr.zoneBands, chart: chart, width: width, height: height)
        drawHrPath(hr.chartPoints, chart: chart, width: width, height: height)
        WorkoutPdfRenderSupport.drawText(
            String(localized: "pdf_field_hr_chart") as NSString,
            at: CGPoint(x: margin, y: chart.maxY + 4),
            font: WorkoutPdfRenderSupport.bodyFont(size: 10, spec: document.fontSpec),
            color: .darkGray,
            width: width
        )
        return chart.maxY + 20
    }

    private static func drawZoneBands(
        _ bands: [PdfZoneBand],
        chart: CGRect,
        width: CGFloat,
        height: CGFloat
    ) {
        for band in bands {
            let topY = chart.maxY - CGFloat(band.upperY) * height
            let bottomY = chart.maxY - CGFloat(band.lowerY) * height
            let bandRect = CGRect(
                x: chart.minX,
                y: topY,
                width: width,
                height: max(1, bottomY - topY)
            )
            WorkoutPdfRenderSupport.colorFromArgb(band.colorArgb)
                .withAlphaComponent(0.18)
                .setFill()
            UIBezierPath(rect: bandRect).fill()
        }
    }

    private static func drawHrPath(
        _ points: [PdfChartPoint],
        chart: CGRect,
        width: CGFloat,
        height: CGFloat
    ) {
        let path = UIBezierPath()
        for (index, point) in points.enumerated() {
            let posX = chart.minX + CGFloat(point.x) * width
            let posY = chart.maxY - CGFloat(point.y) * height
            if index == 0 {
                path.move(to: CGPoint(x: posX, y: posY))
            } else {
                path.addLine(to: CGPoint(x: posX, y: posY))
            }
        }
        UIColor(red: 0.85, green: 0.2, blue: 0.25, alpha: 1).setStroke()
        path.lineWidth = 1.5
        path.stroke()
    }
}
