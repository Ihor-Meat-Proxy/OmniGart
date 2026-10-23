import SwiftUI

/// Glanceable live session for strength / yoga / pilates (and shared chrome).
struct OmniGartLiveWorkoutView: View {
    @StateObject private var session: OmniGartLiveWorkoutSession
    @State private var summary: OmniGartLiveWorkoutSummary?
    @State private var segmentLabel: String = "1"

    init(kind: OmniGartWorkoutKind) {
        _session = StateObject(wrappedValue: OmniGartLiveWorkoutSession(kind: kind))
    }

    var body: some View {
        Group {
            if let summary {
                OmniGartLiveWorkoutSummaryView(summary: summary) {
                    self.summary = nil
                }
            } else {
                liveContent
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if !session.isRunning, summary == nil {
                await session.start()
            }
        }
    }

    private var liveContent: some View {
        TimelineView(.periodic(from: .now, by: 1)) { _ in
            VStack(spacing: 4) {
                Text(session.kind.titleKey)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

                Text(Zone2Summary.formatDuration(session.elapsed))
                    .font(.title3.monospacedDigit().weight(.semibold))
                    .accessibilityLabel(Text("watch.workout.duration"))

                Text("\(session.heartRateBpm)")
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)
                    .accessibilityLabel(Text("watch.zone2.hr_label"))

                if session.activeEnergyKcal > 0 {
                    Text("\(Zone2Summary.formatEnergyKcal(session.activeEnergyKcal)) kcal")
                        .font(.caption.monospacedDigit())
                        .accessibilityLabel(Text("watch.zone2.active_kcal"))
                }

                HStack(spacing: 6) {
                    Button {
                        if session.isPaused {
                            session.resume()
                        } else {
                            session.pause()
                        }
                    } label: {
                        Text(session.isPaused ? "watch.workout.resume" : "watch.workout.pause")
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .disabled(!session.isRunning)

                    Button {
                        session.addSegment(label: segmentLabel)
                        if let n = Int(segmentLabel) {
                            segmentLabel = String(n + 1)
                        }
                    } label: {
                        Text("watch.workout.segment")
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .disabled(!session.isRunning)
                }

                if session.segmentCount > 0 {
                    Text("\(session.segmentCount)")
                        .font(.caption2)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        .accessibilityLabel(Text("watch.workout.segments"))
                }

                if let err = session.lastError {
                    Text(err)
                        .font(.caption2)
                        .foregroundStyle(Color.OmniGart.colorSemanticDanger)
                        .lineLimit(2)
                }

                Button {
                    Task {
                        if let result = await session.stop() {
                            summary = result
                        }
                    }
                } label: {
                    Text("watch.workout.stop")
                }
                .tint(Color.OmniGart.colorBrandAccent)
            }
            .padding(.horizontal, 4)
        }
    }
}

struct OmniGartLiveWorkoutSummaryView: View {
    let summary: OmniGartLiveWorkoutSummary
    var onDone: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 6) {
                Text(summary.kind.titleKey)
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)

                Text("watch.workout.summary.wellness")
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .fixedSize(horizontal: false, vertical: true)

                row("watch.workout.duration", Zone2Summary.formatDuration(summary.elapsed))
                row(
                    "watch.zone2.summary.active_kcal",
                    Zone2Summary.formatEnergyKcal(summary.activeEnergyKcal)
                )
                row(
                    "watch.zone2.summary.total_kcal",
                    Zone2Summary.formatEnergyKcal(summary.totalEnergyKcal)
                )
                if summary.averageHeartRateBpm > 0 {
                    row(
                        "watch.zone2.summary.avg_hr",
                        String(format: "%.0f", summary.averageHeartRateBpm)
                    )
                }
                if summary.maxHeartRateBpm > 0 {
                    row(
                        "watch.zone2.summary.max_hr",
                        String(format: "%.0f", summary.maxHeartRateBpm)
                    )
                }
                if summary.segmentCount > 0 {
                    row("watch.workout.segments", "\(summary.segmentCount)")
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

    private func row(_ key: LocalizedStringKey, _ value: String) -> some View {
        HStack {
            Text(key)
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Spacer(minLength: 4)
            Text(value)
                .font(.caption.monospacedDigit().weight(.semibold))
        }
    }
}
