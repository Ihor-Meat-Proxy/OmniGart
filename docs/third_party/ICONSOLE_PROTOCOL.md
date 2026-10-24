# iConsole protocol references

OmniGart’s iConsole support in `shared/treadmill` is a **clean-room AGPL-3.0** implementation.
The following projects were consulted as **protocol documentation only** (not vendored):

| Project | URL | Notes |
|---------|-----|--------|
| haraldh/iconsole | https://github.com/haraldh/iconsole | Classic SPP framing, READ telemetry layout, PING/START/STOP/LEVEL |
| iconsole-plus | https://github.com/christopher-david-smith/iconsole-plus | GPL-2.0; BLE UART handshake / heartbeat behaviour |
| Microchip RN4870 / BM70 | Microchip transparent UART docs | GATT TX/RX UUIDs used by some iConsole BLE modules |

Do not copy source from GPL trees into OmniGart. Extend parsers from diagnostics captures when a treadmill variant differs.
