import SwiftUI

struct WatchStartView: View {
    let summaryLines: [String]
    @ObservedObject private var pending = PendingWatchWorkout.shared

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: OmniGartMetrics.space2) {
                    Text("app_name")
                        .font(.headline)
                        .foregroundStyle(Color.OmniGart.colorBrandAccent)
                        .accessibilityIdentifier("watch_brand")
                    Text("watch_subtitle")
                        .font(.caption2)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        .multilineTextAlignment(.center)
                    ForEach(summaryLines.prefix(2), id: \.self) { line in
                        Text(line)
                            .font(.caption)
                            .multilineTextAlignment(.center)
                    }

                    NavigationLink {
                        Zone2LiveView()
                    } label: {
                        Text("watch.zone2.start")
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .accessibilityIdentifier("watch_start_button")

                    NavigationLink {
                        OmniGartLiveWorkoutView(kind: .traditionalStrength)
                    } label: {
                        Text("watch.workout.strength_traditional")
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .accessibilityIdentifier("watch_strength_start")

                    NavigationLink {
                        OmniGartLiveWorkoutView(kind: .functionalStrength)
                    } label: {
                        Text("watch.workout.strength_functional")
                    }
                    .tint(Color.OmniGart.colorBrandAccent)

                    NavigationLink {
                        OmniGartLiveWorkoutView(kind: .yoga)
                    } label: {
                        Text("watch.workout.yoga")
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .accessibilityIdentifier("watch_yoga_start")

                    NavigationLink {
                        OmniGartLiveWorkoutView(kind: .pilates)
                    } label: {
                        Text("watch.workout.pilates")
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .accessibilityIdentifier("watch_pilates_start")
                }
                .padding(OmniGartMetrics.space2)
            }
            .accessibilityIdentifier("wear_start")
            .navigationDestination(item: $pending.kind) { kind in
                if kind == .zone2Run {
                    Zone2LiveView()
                } else {
                    OmniGartLiveWorkoutView(kind: kind)
                }
            }
        }
    }
}

#Preview {
    WatchStartView(summaryLines: ["OmniGart", "BMR 1349 kcal"])
}
