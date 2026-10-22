package com.omnigart.sensors.ble

/**
 * Body Sensor Location characteristic (0x2A38) values per Bluetooth SIG HRS 1.0.
 */
enum class BodySensorLocation(val code: Int) {
    OTHER(0),
    CHEST(1),
    WRIST(2),
    FINGER(3),
    HAND(4),
    EAR_LOBE(5),
    FOOT(6),
    ;

    companion object {
        fun fromByte(value: UByte): BodySensorLocation =
            entries.firstOrNull { it.code == value.toInt() } ?: OTHER

        fun parse(payload: ByteArray): BodySensorLocation {
            require(payload.isNotEmpty()) { "Body Sensor Location characteristic is empty" }
            return fromByte(payload[0].toUByte())
        }
    }
}
