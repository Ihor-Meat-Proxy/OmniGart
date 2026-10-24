import CoreBluetooth
import Foundation

/// BLE Heart Rate Service (0x180D) overlay for treadmill sessions on iPhone.
@MainActor
final class TreadmillChestStrapHr: NSObject, ObservableObject {
    @Published private(set) var bpm: Int?
    @Published private(set) var connected = false
    @Published private(set) var scanning = false

    private var central: CBCentralManager?
    private var linkedPeripheral: CBPeripheral?
    private var lastSampleAt: Date?

    var secondsSinceLastSample: Double? {
        guard let lastSampleAt else { return nil }
        return Date().timeIntervalSince(lastSampleAt)
    }

    func start() {
        if central == nil {
            central = CBCentralManager(delegate: self, queue: nil)
        }
    }

    func startScan() {
        start()
        guard let central, central.state == .poweredOn else { return }
        scanning = true
        let service = CBUUID(string: "180D")
        central.scanForPeripherals(withServices: [service], options: nil)
    }

    func stop() {
        central?.stopScan()
        if let linkedPeripheral {
            central?.cancelPeripheralConnection(linkedPeripheral)
        }
        scanning = false
        connected = false
        bpm = nil
    }
}

extension TreadmillChestStrapHr: CBCentralManagerDelegate {
    // Hop only Sendable values; use self.linkedPeripheral / self.central on MainActor.
    nonisolated func centralManagerDidUpdateState(_ central: CBCentralManager) {}

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        let id = peripheral.identifier
        Task { @MainActor in
            scanning = false
            guard let central = self.central else { return }
            central.stopScan()
            guard let found = central.retrievePeripherals(withIdentifiers: [id]).first else { return }
            linkedPeripheral = found
            found.delegate = self
            central.connect(found, options: nil)
        }
    }

    nonisolated func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        Task { @MainActor in
            connected = true
            let device = self.linkedPeripheral
            device?.discoverServices([CBUUID(string: "180D")])
        }
    }

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        Task { @MainActor in
            connected = false
            bpm = nil
        }
    }
}

extension TreadmillChestStrapHr: CBPeripheralDelegate {
    nonisolated func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        Task { @MainActor in
            guard let device = self.linkedPeripheral, let services = device.services else { return }
            for service in services where service.uuid == CBUUID(string: "180D") {
                device.discoverCharacteristics([CBUUID(string: "2A37")], for: service)
            }
        }
    }

    nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: Error?
    ) {
        let serviceId = service.uuid.uuidString
        Task { @MainActor in
            guard let device = self.linkedPeripheral,
                  let service = device.services?.first(where: { $0.uuid.uuidString == serviceId }),
                  let chars = service.characteristics
            else { return }
            for char in chars where char.uuid == CBUUID(string: "2A37") {
                device.setNotifyValue(true, for: char)
            }
        }
    }

    nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        let data = characteristic.value
        Task { @MainActor in
            guard let data, !data.isEmpty else { return }
            let flags = Int(data[0])
            let value: Int
            if flags & 0x01 != 0, data.count >= 3 {
                value = Int(data[1]) | (Int(data[2]) << 8)
            } else if data.count >= 2 {
                value = Int(data[1])
            } else {
                return
            }
            if value > 0 {
                bpm = value
                lastSampleAt = Date()
            }
        }
    }
}
