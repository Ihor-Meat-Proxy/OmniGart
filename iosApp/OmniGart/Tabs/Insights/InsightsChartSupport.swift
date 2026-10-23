import Charts
import Shared
import SwiftUI

/// Chart accents: orange = current period, grey = comparison, vertical now marker.
enum InsightsChartPalette {
    static let current = Color(red: 0.90, green: 0.62, blue: 0.0)
    static let comparison = Color(white: 0.55)
    static let band = Color(red: 0.90, green: 0.62, blue: 0.0).opacity(0.22)
    static let now = Color.white.opacity(0.55)
    static let card = Color(red: 0.08, green: 0.11, blue: 0.17)
    static let cardStroke = Color.white.opacity(0.08)
    static let title = Color.white.opacity(0.92)
    static let subtitle = Color.white.opacity(0.55)
}

struct DarkInsightCard<Content: View>: View {
    let titleKey: LocalizedStringKey
    var sourceName: String?
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(titleKey)
                    .font(.headline)
                    .foregroundStyle(InsightsChartPalette.title)
                Spacer(minLength: 8)
                if let sourceName, !sourceName.isEmpty {
                    Text(sourceName)
                        .font(.caption2)
                        .foregroundStyle(InsightsChartPalette.subtitle)
                        .lineLimit(1)
                }
            }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(InsightsChartPalette.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(InsightsChartPalette.cardStroke, lineWidth: 1)
                )
        )
    }
}

struct CompareLineChart: View {
    let current: [ChartPoint]
    let comparison: [ChartPoint]
    let nowFraction: Double
    var showBand: Bool = false

    var body: some View {
        let maxX = Double(max(current.map { Int($0.dayOffset) }.max() ?? 1, 1))
        Chart {
            if showBand {
                ForEach(Array(current.enumerated()), id: \.offset) { _, point in
                    if let low = optionalDouble(point.low), let high = optionalDouble(point.high) {
                        AreaMark(
                            x: .value("day", Double(point.dayOffset)),
                            yStart: .value("low", low),
                            yEnd: .value("high", high)
                        )
                        .foregroundStyle(InsightsChartPalette.band)
                    }
                }
            }
            ForEach(Array(comparison.enumerated()), id: \.offset) { _, point in
                LineMark(
                    x: .value("day", Double(point.dayOffset)),
                    y: .value("cmp", point.value)
                )
                .foregroundStyle(InsightsChartPalette.comparison)
                .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [4, 3]))
            }
            ForEach(Array(current.enumerated()), id: \.offset) { _, point in
                LineMark(
                    x: .value("day", Double(point.dayOffset)),
                    y: .value("cur", point.value)
                )
                .foregroundStyle(InsightsChartPalette.current)
                .lineStyle(StrokeStyle(lineWidth: 2.2))
                .opacity(point.projected ? 0.45 : 1)
            }
            RuleMark(x: .value("now", max(0, min(1, nowFraction)) * maxX))
                .foregroundStyle(InsightsChartPalette.now)
                .lineStyle(StrokeStyle(lineWidth: 1, dash: [3, 3]))
        }
        .chartXAxis(.hidden)
        .chartYAxis {
            AxisMarks(position: .leading) { _ in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                    .foregroundStyle(Color.white.opacity(0.08))
                AxisValueLabel()
                    .foregroundStyle(InsightsChartPalette.subtitle)
            }
        }
        .frame(height: 140)
        .accessibilityLabel(Text("insights_chart_a11y"))
    }
}

struct HeatmapGrid: View {
    let cells: [HeatmapCell]
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 3), count: 12)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 3) {
            ForEach(Array(cells.enumerated()), id: \.offset) { _, cell in
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(
                        InsightsChartPalette.current.opacity(
                            min(1, 0.12 + cell.intensity * 0.88)
                        )
                    )
                    .frame(height: 12)
            }
        }
        .accessibilityLabel(Text("insights_heatmap_a11y"))
    }
}

func optionalDouble(_ value: KotlinDouble?) -> Double? {
    guard let value else { return nil }
    return value.doubleValue
}
