import Foundation
import HealthKit
import Shared
import SwiftUI

/// Receives a mirrored `HKWorkoutSession` from the Apple Watch (`startMirroringToCompanionDevice`)
/// so the iPhone can show live metrics and pause / end. Does **not** write a second workout to
/// Apple Health — the watch remains the single HealthKit recorder.
@MainActor
final class MirroredWorkoutController: NSObject, ObservableObject, HKWorkoutSessionDelegate {
    static let shared = MirroredWorkoutController()

    @Published private(set) var isMirroring = false
    @Published private(set) var isPaused = false
    @Published private(set) var heartRateBpm: Int = 0
    @Published private(set) var activeEnergyKcal: Double = 0
    @Published private(set) var elapsedLabel: String = "0:00"
    @Published private(set) var statusKey: String?

    private var session: HKWorkoutSession?
    private var startedAt: Date?
    private var elapsedTimer: Timer?

    private override init() {
        super.init()
    }

    /// Install once at app launch (HealthKit workoutSessionMirroringStartHandler).
    func installMirroringHandler() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        let store = HKHealthStore()
        store.workoutSessionMirroringStartHandler = { [weak self] mirrored in
            Task { @MainActor in
                self?.attach(mirroredSession: mirrored)
            }
        }
    }

    func attach(mirroredSession: HKWorkoutSession) {
        session?.delegate = nil
        stopElapsedTimer()
        session = mirroredSession
        mirroredSession.delegate = self
        startedAt = Date()
        isMirroring = true
        isPaused = mirroredSession.state == .paused
        statusKey = "workout_watch_mirroring"
        startElapsedTimer()
    }

    func pause() {
        session?.pause()
    }

    func resume() {
        session?.resume()
    }

    func end() {
        session?.end()
        clear()
    }

    private func clear() {
        stopElapsedTimer()
        session?.delegate = nil
        session = nil
        isMirroring = false
        isPaused = false
        heartRateBpm = 0
        activeEnergyKcal = 0
        elapsedLabel = "0:00"
        statusKey = nil
        startedAt = nil
    }

    private func startElapsedTimer() {
        stopElapsedTimer()
        elapsedTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.tickElapsed()
            }
        }
    }

    private func stopElapsedTimer() {
        elapsedTimer?.invalidate()
        elapsedTimer = nil
    }

    private func tickElapsed() {
        guard let startedAt else { return }
        let seconds = Int(Date().timeIntervalSince(startedAt))
        let minutes = seconds / 60
        let rem = seconds % 60
        elapsedLabel = String(format: "%d:%02d", minutes, rem)
    }

    nonisolated func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didChangeTo toState: HKWorkoutSessionState,
        from fromState: HKWorkoutSessionState,
        date: Date
    ) {
        Task { @MainActor in
            switch toState {
            case .running:
                isPaused = false
            case .paused:
                isPaused = true
            case .ended, .stopped:
                clear()
            default:
                break
            }
        }
    }

    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {
        Task { @MainActor in
            statusKey = "workout_watch_unavailable"
            clear()
        }
    }

    nonisolated func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didReceiveDataFromRemoteWorkoutSession data: [Data]
    ) {
        // Optional remote metric packets from the watch (HR / energy). Best-effort decode.
        for packet in data {
            guard let text = String(data: packet, encoding: .utf8) else { continue }
            Task { @MainActor in
                applyRemoteMetricPacket(text)
            }
        }
    }

    private func applyRemoteMetricPacket(_ text: String) {
        // Lightweight "hr=142;kcal=12.5" packets — ignore unknown formats.
        for part in text.split(separator: ";") {
            let pair = part.split(separator: "=", maxSplits: 1)
            guard pair.count == 2 else { continue }
            let key = String(pair[0])
            let value = String(pair[1])
            if key == "hr", let bpm = Int(value) {
                heartRateBpm = bpm
            } else if key == "kcal", let kcal = Double(value) {
                activeEnergyKcal = kcal
            }
        }
    }
}

/// Compact live strip when a mirrored watch workout is active.
struct MirroredWorkoutBanner: View {
    @ObservedObject private var controller = MirroredWorkoutController.shared

    var body: some View {
        if controller.isMirroring {
            VStack(alignment: .leading, spacing: 6) {
                Text("workout_watch_mirroring")
                    .font(.caption.weight(.semibold))
                HStack(spacing: 12) {
                    Text(controller.elapsedLabel)
                        .monospacedDigit()
                    if controller.heartRateBpm > 0 {
                        Text("\(controller.heartRateBpm) bpm")
                            .monospacedDigit()
                    }
                    if controller.activeEnergyKcal > 0 {
                        Text(String(format: "%.0f kcal", controller.activeEnergyKcal))
                            .monospacedDigit()
                    }
                }
                .font(.caption)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                HStack(spacing: 12) {
                    if controller.isPaused {
                        Button("watch.workout.resume") { controller.resume() }
                    } else {
                        Button("watch.workout.pause") { controller.pause() }
                    }
                    Button("workout_end") { controller.end() }
                }
                .font(.caption)
            }
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityIdentifier("mirrored_workout_banner")
        }
    }
}
