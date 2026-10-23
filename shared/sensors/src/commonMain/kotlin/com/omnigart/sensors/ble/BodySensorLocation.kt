package com.omnigart.sensors.ble

/**
 * Body Sensor Location characteristic (0x2A38) values from the Heart Rate Service.
 */
enum class BodySensorLocation(val code: Int) {
    Other(0),
    Chest(1),
    Wrist(2),
    Finger(3),
    Hand(4),
    EarLobe(5),
    Foot(6)
    ;

    companion object {
        fun fromByte(value: Int): BodySensorLocation? = entries.firstOrNull { it.code == (value and 0xFF) }
    }
}
