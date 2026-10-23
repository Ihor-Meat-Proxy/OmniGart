import SwiftUI

/// One glanceable Zone 2 screen: HR, zone colour, time in zone, cadence, distance.
/// Always-On friendly (no dense chrome). Elapsed shown only as a compact trailing label.
struct Zone2LiveView: View {
    @StateObject private var session: Zone2WorkoutSession
    @AppStorage("watch.zone2.haptics.enabled") private var hapticsEnabled = true

    init(band: Zone2Band = .karvonen(hrMax: 180, hrRest: 60)) {
        _session = StateObject(wrappedValue: Zone2WorkoutSession(band: band))
    }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { _ in
            VStack(spacing: 2) {
                HStack {
                    Text("watch.zone2.title")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    Spacer(minLength: 0)
                    if session.isRunning {
                        Text(timeString(session.elapsed))
                            .font(.caption2.monospacedDigit())
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                            .accessibilityLabel(Text("watch.zone2.elapsed"))
                    }
                }

                Text("\(session.heartRateBpm)")
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .minimumScaleFactor(0.7)
                    .foregroundStyle(zoneColor)
                    .accessibilityLabel(Text("watch.zone2.hr_label"))
                    .accessibilityValue(Text("\(session.heartRateBpm)"))

                Text(zoneCaptionKey)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(zoneColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(zoneColor.opacity(0.18), in: Capsule())

                HStack {
                    metric(labelKey: "watch.zone2.time_in_zone", value: timeString(session.timeInZone))
                    metric(labelKey: "watch.zone2.cadence", value: String(format: "%.0f", session.cadenceSpm))
                    metric(labelKey: "watch.zone2.distance", value: distanceString(session.distanceMetres))
                }
                .padding(.top, 2)

                if let err = session.lastError {
                    Text(err)
                        .font(.caption2)
                        .foregroundStyle(Color.OmniGart.colorStateError)
                        .lineLimit(2)
                }

                Button {
                    Task {
                        if session.isRunning {
                            await session.stop()
                        } else {
                            session.setHapticsEnabled(hapticsEnabled)
                            await session.start()
                        }
                    }
                } label: {
                    Text(session.isRunning ? "watch.zone2.stop" : "watch.zone2.start")
                }
                .tint(Color.OmniGart.colorBrandAccent)

                Toggle(isOn: $hapticsEnabled) {
                    Text("watch.zone2.haptics_toggle")
                        .font(.caption2)
                }
                .tint(Color.OmniGart.colorBrandAccent)
                .onChange(of: hapticsEnabled) { _, enabled in
                    session.setHapticsEnabled(enabled)
                }
            }
            .padding(.horizontal, 4)
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            session.setHapticsEnabled(hapticsEnabled)
        }
    }

    private var zoneColor: Color {
        switch session.zoneState {
        case .inTarget: return Color.OmniGart.colorHrZoneZ2
        case .below: return Color.OmniGart.colorHrZoneZ1
        case .above: return Color.OmniGart.colorHrZoneZ3
        }
    }

    private var zoneCaptionKey: LocalizedStringKey {
        switch session.zoneState {
        case .inTarget: return "watch.zone2.zone_in"
        case .below: return "watch.zone2.zone_below"
        case .above: return "watch.zone2.zone_above"
        }
    }

    private func metric(labelKey: LocalizedStringKey, value: String) -> some View {
        VStack(spacing: 0) {
            Text(value)
                .font(.caption.monospacedDigit().weight(.semibold))
                .minimumScaleFactor(0.6)
            Text(labelKey)
                .font(.system(size: 9))
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
        .frame(maxWidth: .infinity)
    }

    private func timeString(_ t: TimeInterval) -> String {
        let s = Int(t.rounded())
        let m = s / 60
        let r = s % 60
        return String(format: "%d:%02d", m, r)
    }

    private func distanceString(_ metres: Double) -> String {
        if metres >= 1000 {
            return String(format: "%.2f", metres / 1000)
        }
        return String(format: "%.0f", metres)
    }

}

#Preview {
    NavigationStack {
        Zone2LiveView()
    }
}
