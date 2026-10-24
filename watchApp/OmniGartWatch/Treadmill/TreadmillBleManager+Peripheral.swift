import CoreBluetooth
import Foundation

extension TreadmillBleManager: CBPeripheralDelegate {
    // Hop only Sendable snapshots; use self.linkedPeripheral on the MainActor.
    nonisolated func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        Task { @MainActor in
            guard let device = self.linkedPeripheral, let services = device.services else { return }
            for service in services {
                appendDiag("SERVICE \(service.uuid.uuidString)")
                device.discoverCharacteristics(nil, for: service)
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
                  let service = device.services?.first(where: { $0.uuid.uuidString == serviceId })
            else { return }
            handleDiscoveredCharacteristics(peripheral: device, service: service)
        }
    }

    nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        let uuid = characteristic.uuid.uuidString
        let data = characteristic.value
        Task { @MainActor in
            handleValueUpdate(uuid: uuid, data: data)
        }
    }

    // swiftlint:disable:next cyclomatic_complexity function_body_length
    private func handleDiscoveredCharacteristics(peripheral: CBPeripheral, service: CBService) {
        guard let chars = service.characteristics else { return }
        var hasFtms = false
        var hasFitShow = false
        var hasRsc = false
        var hasIConsole = false
        var fitShowNotify: CBCharacteristic?
        var rscChar: CBCharacteristic?
        for char in chars {
            appendDiag(
                "CHAR svc=\(service.uuid.uuidString) char=\(char.uuid.uuidString) props=\(char.properties.rawValue)"
            )
            let uuid = char.uuid.uuidString.lowercased()
            if uuid.contains("2acd") {
                hasFtms = true
                peripheral.setNotifyValue(true, for: char)
            } else if uuid.contains("2ad9") {
                controlPoint = char
            } else if uuid.contains("fff1") {
                hasFitShow = true
                fitShowNotify = char
            } else if uuid.contains("fff2") {
                fitShowWrite = char
            } else if uuid.contains("2a53") {
                hasRsc = true
                rscChar = char
            } else if uuid == IConsoleSwiftParser.uartTx.lowercased() {
                hasIConsole = true
                peripheral.setNotifyValue(true, for: char)
            } else if uuid == IConsoleSwiftParser.uartRx.lowercased() {
                iconsoleRx = char
            }
        }
        if hasFtms {
            activeProtocol = .ftms
        } else if hasFitShow {
            activeProtocol = .fitshow
            if let fitShowNotify {
                peripheral.setNotifyValue(true, for: fitShowNotify)
            }
            if let fitShowWrite {
                startFitShowPolling(peripheral: peripheral, writeChar: fitShowWrite)
            }
        } else if hasRsc {
            activeProtocol = .rsc
            if let rscChar {
                peripheral.setNotifyValue(true, for: rscChar)
            }
        } else if hasIConsole {
            activeProtocol = .iconsole
            if let iconsoleRx {
                startIConsolePolling(peripheral: peripheral, rx: iconsoleRx)
            }
        }
        appendDiag("INFO active protocol \(activeProtocol.rawValue)")
    }

    private func handleValueUpdate(uuid: String, data: Data?) {
        guard let data else { return }
        let hex = data.map { String(format: "%02x", $0) }.joined(separator: " ")
        appendDiag("RAW \(uuid) \(hex)")
        let lower = uuid.lowercased()
        if lower.contains("2acd"), activeProtocol == .ftms,
           let parsed = FtmsTreadmillParser.parse(data) {
            apply(parsed)
        } else if lower.contains("fff1"), activeProtocol == .fitshow,
                  let parsed = FitShowSwiftParser.parseNotification(data) {
            apply(parsed)
        } else if lower.contains("2a53"), activeProtocol == .rsc,
                  let parsed = RscMeasurementSwiftParser.parse(data) {
            apply(parsed)
        } else if lower == IConsoleSwiftParser.uartTx.lowercased(),
                  activeProtocol == .iconsole,
                  let parsed = IConsoleSwiftParser.parseTelemetry(data) {
            apply(parsed)
        }
    }

    private func startIConsolePolling(peripheral: CBPeripheral, rx: CBCharacteristic) {
        iconsolePollTask?.cancel()
        iconsolePollTask = Task { @MainActor in
            peripheral.writeValue(IConsoleSwiftParser.ping(), for: rx, type: .withResponse)
            while !Task.isCancelled {
                peripheral.writeValue(IConsoleSwiftParser.read(), for: rx, type: .withResponse)
                try? await Task.sleep(nanoseconds: 500_000_000)
                peripheral.writeValue(IConsoleSwiftParser.ping(), for: rx, type: .withoutResponse)
            }
        }
    }

    private func startFitShowPolling(peripheral: CBPeripheral, writeChar: CBCharacteristic) {
        fitShowPollTask?.cancel()
        fitShowPollTask = Task { @MainActor in
            while !Task.isCancelled {
                peripheral.writeValue(FitShowSwiftParser.pollStatus(), for: writeChar, type: .withResponse)
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }
}
