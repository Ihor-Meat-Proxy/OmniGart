import SwiftUI

/// Glanceable live session for strength / yoga / pilates (and shared chrome).
struct OmniGartLiveWorkoutView: View {
    @StateObject private var session: OmniGartLiveWorkoutSession
    @State private var summary: OmniGartLiveWorkoutSummary?
    @State private var segmentLabel: String = "1"
    @State private var crownField: StrengthCrownField = .weight
    @State private var crownValue: Double = 60

    private enum StrengthCrownField {
        case weight
        case reps
    }

    init(kind: OmniGartWorkoutKind) {
        _session = StateObject(wrappedValue: OmniGartLiveWorkoutSession(kind: kind))
    }

    var body: some View {
        Group {
            if let summary {
                OmniGartLiveWorkoutSummaryView(summary: summary) {
                    self.summary = nil
                }
            } else if session.kind.isStrength {
                strengthLiveContent
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
        .onAppear {
            crownValue = session.weightKg
        }
    }

    private var strengthLiveContent: some View {
        TimelineView(.periodic(from: .now, by: 1)) { _ in
            ScrollView {
                VStack(spacing: 4) {
                    Text(session.kind.titleKey)
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

                    Text(session.currentExerciseName)
                        .font(.caption.weight(.semibold))
                        .lineLimit(2)
                        .multilineTextAlignment(.center)

                    Text("Set \(session.currentSetNumber)")
                        .font(.caption2.monospacedDigit())
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        .accessibilityLabel(Text("watch.strength.set"))

                    if !session.currentTarget.isEmpty {
                        Text(session.currentTarget)
                            .font(.caption2)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                            .accessibilityLabel(Text("watch.strength.target"))
                    }

                    HStack(spacing: 8) {
                        crownMetric(
                            labelKey: "watch.strength.weight",
                            value: String(format: "%.1f", session.weightKg),
                            selected: crownField == .weight
                        ) {
                            crownField = .weight
                            crownValue = session.weightKg
                        }
                        crownMetric(
                            labelKey: "watch.strength.reps",
                            value: "\(session.reps)",
                            selected: crownField == .reps
                        ) {
                            crownField = .reps
                            crownValue = Double(session.reps)
                        }
                    }
                    .focusable(true)
                    .digitalCrownRotation(
                        $crownValue,
                        from: crownField == .weight ? 0 : 1,
                        through: crownField == .weight ? 400 : 50,
                        by: crownField == .weight ? 2.5 : 1,
                        sensitivity: .medium,
                        isContinuous: false,
                        isHapticFeedbackEnabled: true
                    )
                    .onChange(of: crownValue) { _, newValue in
                        applyCrown(newValue)
                    }

                    if session.restRemaining > 0 {
                        Text("\(String(localized: String.LocalizationValue("watch.strength.rest"))) \(session.restRemaining)s")
                            .font(.caption.monospacedDigit().weight(.semibold))
                            .foregroundStyle(Color.OmniGart.colorBrandAccent)
                            .accessibilityLabel(Text("watch.strength.rest"))
                    }

                    Text("\(session.heartRateBpm)")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(Color.OmniGart.colorBrandAccent)
                        .accessibilityLabel(Text("watch.zone2.hr_label"))

                    if session.activeEnergyKcal > 0 {
                        Text("\(Zone2Summary.formatEnergyKcal(session.activeEnergyKcal)) kcal")
                            .font(.caption2.monospacedDigit())
                            .accessibilityLabel(Text("watch.zone2.active_kcal"))
                    }

                    Text(Zone2Summary.formatDuration(session.elapsed))
                        .font(.caption2.monospacedDigit())
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        .accessibilityLabel(Text("watch.workout.duration"))

                    Button {
                        session.logSet()
                    } label: {
                        Text("watch.strength.done")
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .disabled(!session.isRunning || session.restRemaining > 0)

                    HStack(spacing: 6) {
                        Button {
                            if session.isPaused {
                                session.resume()
                            } else {
                                session.pause()
                            }
                        } label: {
                            // Ternary of String uses Text.init(String) (no localization).
                            Text(
                                session.isPaused
                                    ? LocalizedStringKey("watch.workout.resume")
                                    : LocalizedStringKey("watch.workout.pause")
                            )
                        }
                        .tint(Color.OmniGart.colorBrandAccent)
                        .disabled(!session.isRunning)

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

                    if !session.loggedSets.isEmpty {
                        Text("\(session.loggedSets.count)")
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
                }
                .padding(.horizontal, 4)
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
                        Text(
                            session.isPaused
                                ? LocalizedStringKey("watch.workout.resume")
                                : LocalizedStringKey("watch.workout.pause")
                        )
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .disabled(!session.isRunning)

                    Button {
                        session.addSegment(label: segmentLabel)
                        if let segmentNumber = Int(segmentLabel) {
                            segmentLabel = String(segmentNumber + 1)
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

    private func crownMetric(
        labelKey: LocalizedStringKey,
        value: String,
        selected: Bool,
        onSelect: @escaping () -> Void
    ) -> some View {
        Button(action: onSelect) {
            VStack(spacing: 1) {
                Text(labelKey)
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                Text(value)
                    .font(.caption.monospacedDigit().weight(.semibold))
                    .foregroundStyle(
                        selected ? Color.OmniGart.colorBrandAccent : Color.OmniGart.colorTextLightPrimary
                    )
            }
        }
        .buttonStyle(.plain)
    }

    private func applyCrown(_ value: Double) {
        switch crownField {
        case .weight:
            session.weightKg = (value * 2).rounded() / 2
        case .reps:
            session.reps = max(1, Int(value.rounded()))
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
                if !summary.sets.isEmpty {
                    row("watch.strength.sets", "\(summary.sets.count)")
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
