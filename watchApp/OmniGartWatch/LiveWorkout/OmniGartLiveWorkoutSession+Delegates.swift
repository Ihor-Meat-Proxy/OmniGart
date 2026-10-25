import Foundation
import HealthKit

extension OmniGartLiveWorkoutSession: HKWorkoutSessionDelegate {
    nonisolated func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didChangeTo toState: HKWorkoutSessionState,
        from fromState: HKWorkoutSessionState,
        date: Date
    ) {
        Task { @MainActor in
            switch toState {
            case .ended, .stopped:
                isRunning = false
                isPaused = false
            case .paused:
                isPaused = true
            case .running:
                isPaused = false
                isRunning = true
            default:
                break
            }
        }
    }

    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {
        Task { @MainActor in
            lastError = error.localizedDescription
            isRunning = false
        }
    }
}

extension OmniGartLiveWorkoutSession: HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {}

    nonisolated func workoutBuilder(
        _ workoutBuilder: HKLiveWorkoutBuilder,
        didCollectDataOf collectedTypes: Set<HKSampleType>
    ) {
        Task { @MainActor in
            let heartRateType = HKQuantityType.quantityType(forIdentifier: .heartRate)
            let activeEnergyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)
            let basalEnergyType = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
            for type in collectedTypes {
                guard let quantityType = type as? HKQuantityType else { continue }
                guard let stats = workoutBuilder.statistics(for: quantityType) else { continue }
                if quantityType == heartRateType {
                    let unit = HKUnit.count().unitDivided(by: .minute())
                    if let heartRate = stats.mostRecentQuantity()?.doubleValue(for: unit) {
                        ingestHeartRate(heartRate, at: Date())
                    }
                } else if quantityType == activeEnergyType {
                    activeEnergyKcal =
                        stats.sumQuantity()?.doubleValue(for: .kilocalorie()) ?? activeEnergyKcal
                } else if quantityType == basalEnergyType {
                    basalEnergyKcal =
                        stats.sumQuantity()?.doubleValue(for: .kilocalorie()) ?? basalEnergyKcal
                }
            }
            sendMirrorMetricsIfNeeded()
        }
    }
}
