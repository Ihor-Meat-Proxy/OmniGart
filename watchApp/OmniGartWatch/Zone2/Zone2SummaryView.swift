import SwiftUI
#if canImport(Charts)
import Charts
#endif

/// Post-run Zone 2 summary: time in zone, distance, average cadence, HR series.
/// Wellness framing only — training reflection, not medical advice.
struct Zone2SummaryView: View {
    let summary: Zone2Summary
    var onDone: () -> Void

    private var chartSamples: [Zone2HeartRateSample] {
        Zone2Summary.downsample(summary.heartRateSamples)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 6) {
                Text("watch.zone2.summary.title")
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)

                Text("watch.zone2.summary.wellness")
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .fixedSize(horizontal: false, vertical: true)

                metricRow(
                    labelKey: "watch.zone2.summary.time_in_zone",
                    value: Zone2Summary.formatDuration(summary.timeInZone)
                )
                metricRow(
                    labelKey: "watch.zone2.summary.distance",
                    value: Zone2Summary.formatDistance(summary.distanceMetres)
                )
                metricRow(
                    labelKey: "watch.zone2.summary.avg_cadence",
                    value: Zone2Summary.formatCadence(summary.averageCadenceSpm)
                )

                if !chartSamples.isEmpty {
                    Text("watch.zone2.summary.hr_chart")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        .padding(.top, 4)

                    hrChart
                        .frame(height: 72)
                        .accessibilityLabel(Text("watch.zone2.summary.hr_chart"))
                }

                Button(action: onDone) {
                    Text("watch.zone2.summary.done")
                }
                .tint(Color.OmniGart.colorBrandAccent)
                .padding(.top, 4)
            }
            .padding(.horizontal, 4)
        }
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private var hrChart: some View {
        #if canImport(Charts)
        Chart {
            ForEach(Array(chartSamples.enumerated()), id: \.offset) { _, sample in
                LineMark(
                    x: .value("t", sample.offsetSeconds),
                    y: .value("HR", sample.bpm)
                )
                .foregroundStyle(Color.OmniGart.colorHrZoneZ2)
                .interpolationMethod(.catmullRom)
            }
        }
        .chartXAxis(.hidden)
        .chartYAxis {
            AxisMarks(position: .leading, values: .automatic(desiredCount: 3))
        }
        #else
        Zone2HRSparkline(samples: chartSamples)
        #endif
    }

    private func metricRow(labelKey: LocalizedStringKey, value: String) -> some View {
        HStack {
            Text(labelKey)
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Spacer(minLength: 4)
            Text(value)
                .font(.caption.monospacedDigit().weight(.semibold))
        }
    }
}

/// Fallback when Charts is unavailable on the watch target.
struct Zone2HRSparkline: View {
    let samples: [Zone2HeartRateSample]

    var body: some View {
        GeometryReader { geo in
            let pts = points(in: geo.size)
            if pts.count >= 2 {
                Path { path in
                    path.move(to: pts[0])
                    for point in pts.dropFirst() {
                        path.addLine(to: point)
                    }
                }
                .stroke(Color.OmniGart.colorHrZoneZ2, lineWidth: 1.5)
            }
        }
    }

    private func points(in size: CGSize) -> [CGPoint] {
        guard samples.count >= 2,
              let minBpm = samples.map(\.bpm).min(),
              let maxBpm = samples.map(\.bpm).max(),
              let firstSample = samples.first,
              let lastSample = samples.last
        else { return [] }
        let span = max(1, maxBpm - minBpm)
        let startOffset = firstSample.offsetSeconds
        let endOffset = lastSample.offsetSeconds
        let offsetSpan = max(0.001, endOffset - startOffset)
        return samples.map { sample in
            let pointX = CGFloat((sample.offsetSeconds - startOffset) / offsetSpan) * size.width
            let yNorm = CGFloat(sample.bpm - minBpm) / CGFloat(span)
            let pointY = size.height - (yNorm * size.height)
            return CGPoint(x: pointX, y: pointY)
        }
    }
}

#Preview {
    NavigationStack {
        Zone2SummaryView(
            summary: Zone2Summary(
                timeInZone: 612,
                distanceMetres: 3200,
                averageCadenceSpm: 168,
                elapsed: 900,
                heartRateSamples: (0..<40).map { sampleIndex in
                    Zone2HeartRateSample(
                        offsetSeconds: Double(sampleIndex) * 20,
                        bpm: 130 + (sampleIndex % 7)
                    )
                }
            ),
            onDone: {}
        )
    }
}
