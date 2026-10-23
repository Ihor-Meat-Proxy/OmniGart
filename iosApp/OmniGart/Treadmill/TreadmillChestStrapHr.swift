import CoreBluetooth
import Foundation

/// BLE Heart Rate Service (0x180D) overlay for treadmill sessions on iPhone.
@MainActor
final class TreadmillChestStrapHr: NSObject, ObservableObject {
    @Published private(set) var bpm: Int?
    @Published private(set) var connected = false
    @Published private(set) var scanning = false

    private var central: CBCentralManager?
    private var peripheral: CBPeripheral?
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
        if let peripheral {
            central?.cancelPeripheralConnection(peripheral)
        }
        scanning = false
        connected = false
        bpm = nil
    }
}

extension TreadmillChestStrapHr: CBCentralManagerDelegate {
    nonisolated func centralManagerDidUpdateState(_ central: CBCentralManager) {}

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        Task { @MainActor in
            self.scanning = false
            central.stopScan()
            self.peripheral = peripheral
            peripheral.delegate = self
            central.connect(peripheral, options: nil)
        }
    }

    nonisolated func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        Task { @MainActor in
            self.connected = true
            peripheral.discoverServices([CBUUID(string: "180D")])
        }
    }

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        Task { @MainActor in
            self.connected = false
            self.bpm = nil
        }
    }
}

extension TreadmillChestStrapHr: CBPeripheralDelegate {
    nonisolated func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        Task { @MainActor in
            guard let services = peripheral.services else { return }
            for service in services where service.uuid == CBUUID(string: "180D") {
                peripheral.discoverCharacteristics([CBUUID(string: "2A37")], for: service)
            }
        }
    }

    nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: Error?
    ) {
        Task { @MainActor in
            guard let chars = service.characteristics else { return }
            for char in chars where char.uuid == CBUUID(string: "2A37") {
                peripheral.setNotifyValue(true, for: char)
            }
        }
    }

    nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        Task { @MainActor in
            guard let data = characteristic.value, !data.isEmpty else { return }
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
                self.bpm = value
                self.lastSampleAt = Date()
            }
        }
    }
}
