import CoreBluetooth
import Foundation
import os.log

/// Standard BLE Heart Rate Service central with iOS state restoration.
///
/// Background mode: enable `bluetooth-central` in UIBackgroundModes and pass a
/// stable [restorationIdentifier] so iOS can relaunch the app and restore centrals.
public final class BleHeartRateCentral: NSObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    public static let hrsService = CBUUID(string: "180D")
    public static let hrMeasurement = CBUUID(string: "2A37")
    public static let bodySensorLocation = CBUUID(string: "2A38")
    public static let batteryService = CBUUID(string: "180F")
    public static let batteryLevel = CBUUID(string: "2A19")

    public var onPeripheralFound: ((CBPeripheral, NSNumber) -> Void)?
    public var onMeasurement: ((ParsedHeartRateMeasurement, CBPeripheral) -> Void)?
    public var onConnectionChange: ((CBPeripheral, CBPeripheralState) -> Void)?
    public var onBattery: ((Int, CBPeripheral) -> Void)?
    public var onBodyLocation: ((BodySensorLocation, CBPeripheral) -> Void)?

    private let log = Logger(subsystem: "com.omnigart.sensors", category: "BLE")
    private var central: CBCentralManager!
    private var peripherals: [UUID: CBPeripheral] = [:]
    private var autoReconnectIds: Set<UUID> = []
    private let restorationIdentifier: String?

    public init(restorationIdentifier: String? = "com.omnigart.sensors.ble.central") {
        self.restorationIdentifier = restorationIdentifier
        super.init()
        var options: [String: Any] = [:]
        if let restorationIdentifier {
            options[CBCentralManagerOptionRestoreIdentifierKey] = restorationIdentifier
        }
        central = CBCentralManager(delegate: self, queue: .main, options: options)
    }

    public func startScan() {
        guard central.state == .poweredOn else { return }
        central.scanForPeripherals(
            withServices: [Self.hrsService],
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
    }

    public func stopScan() {
        central.stopScan()
    }

    public func connect(_ peripheral: CBPeripheral) {
        peripherals[peripheral.identifier] = peripheral
        peripheral.delegate = self
        central.connect(peripheral, options: [
            CBConnectPeripheralOptionNotifyOnDisconnectionKey: true,
        ])
    }

    public func disconnect(_ peripheral: CBPeripheral) {
        autoReconnectIds.remove(peripheral.identifier)
        central.cancelPeripheralConnection(peripheral)
    }

    public func enableAutoReconnect(_ peripheral: CBPeripheral, enabled: Bool) {
        if enabled {
            autoReconnectIds.insert(peripheral.identifier)
        } else {
            autoReconnectIds.remove(peripheral.identifier)
        }
    }

    // MARK: - CBCentralManagerDelegate

    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        log.info("central state=\(String(describing: central.state.rawValue))")
        if central.state == .poweredOn {
            for id in autoReconnectIds {
                if let p = peripherals[id] {
                    self.central.connect(p, options: nil)
                }
            }
        }
    }

    public func centralManager(
        _ central: CBCentralManager,
        willRestoreState dict: [String: Any]
    ) {
        log.info("state restoration")
        if let restored = dict[CBCentralManagerRestoredStatePeripheralsKey] as? [CBPeripheral] {
            for peripheral in restored {
                peripherals[peripheral.identifier] = peripheral
                peripheral.delegate = self
                autoReconnectIds.insert(peripheral.identifier)
            }
        }
    }

    public func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        peripherals[peripheral.identifier] = peripheral
        onPeripheralFound?(peripheral, RSSI)
    }

    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        onConnectionChange?(peripheral, peripheral.state)
        peripheral.discoverServices([Self.hrsService, Self.batteryService])
    }

    public func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        onConnectionChange?(peripheral, peripheral.state)
        if autoReconnectIds.contains(peripheral.identifier) {
            central.connect(peripheral, options: nil)
        }
    }

    // MARK: - CBPeripheralDelegate

    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        for service in services {
            if service.uuid == Self.hrsService {
                peripheral.discoverCharacteristics(
                    [Self.hrMeasurement, Self.bodySensorLocation],
                    for: service
                )
            } else if service.uuid == Self.batteryService {
                peripheral.discoverCharacteristics([Self.batteryLevel], for: service)
            }
        }
    }

    public func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: Error?
    ) {
        guard let chars = service.characteristics else { return }
        for characteristic in chars {
            switch characteristic.uuid {
            case Self.hrMeasurement:
                peripheral.setNotifyValue(true, for: characteristic)
            case Self.bodySensorLocation, Self.batteryLevel:
                peripheral.readValue(for: characteristic)
            default:
                break
            }
        }
    }

    public func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        guard let data = characteristic.value else { return }
        switch characteristic.uuid {
        case Self.hrMeasurement:
            if let parsed = try? HeartRateMeasurementParser.parse(data) {
                onMeasurement?(parsed, peripheral)
            }
        case Self.batteryLevel:
            if let first = data.first {
                onBattery?(Int(first), peripheral)
            }
        case Self.bodySensorLocation:
            if let first = data.first {
                let loc = BodySensorLocation(rawValue: Int(first)) ?? .other
                onBodyLocation?(loc, peripheral)
            }
        default:
            break
        }
    }
}
