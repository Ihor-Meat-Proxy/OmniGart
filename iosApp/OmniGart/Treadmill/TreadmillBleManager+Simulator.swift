import CoreBluetooth
import Foundation

extension TreadmillBleManager {
    /// Attach the shared-architecture FTMS simulator (DEBUG / Simulator / BLE unavailable).
    func useSimulator() {
        stopScan()
        reconnectTask?.cancel()
        iconsolePollTask?.cancel()
        fitShowPollTask?.cancel()
        if let linkedPeripheral {
            central?.cancelPeripheralConnection(linkedPeripheral)
        }
        linkedPeripheral = nil
        let session = SimulatedTreadmillSession(hrMax: hrMax)
        session.watchHeartRateBpm = watchHeartRateBpm
        _ = session.attach()
        simulator = session
        activeProtocol = .simulated
        state = .connected
        discovered = [
            DiscoveredDevice(id: UUID(), name: session.deviceName, protocolHint: "SIMULATED")
        ]
        live = session.live
        appendDiag("INFO simulator attached")
        startSimulatorForwarding()
    }

    func startSimulatorBelt() {
        guard let simulator else { return }
        _ = simulator.startBelt()
        startSimulatorForwarding()
    }

    func stopSimulatorBelt() {
        simulator?.stopBelt()
        if let simulator {
            live = simulator.live
        }
    }

    func startSimulatorForwarding() {
        simulatorForwardTask?.cancel()
        guard let simulator else { return }
        simulatorForwardTask = Task { @MainActor in
            while !Task.isCancelled, self.simulator != nil {
                live = simulator.live
                try? await Task.sleep(nanoseconds: 500_000_000)
            }
        }
    }

    func clearSimulator() {
        simulatorForwardTask?.cancel()
        simulatorForwardTask = nil
        simulator?.reset()
        simulator = nil
    }
}
