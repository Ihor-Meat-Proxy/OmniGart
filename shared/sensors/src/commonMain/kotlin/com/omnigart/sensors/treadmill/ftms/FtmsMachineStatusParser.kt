package com.omnigart.sensors.treadmill.ftms

import com.omnigart.sensors.ble.BleByteReaders
import com.omnigart.sensors.treadmill.TreadmillMachineState
import com.omnigart.sensors.treadmill.TreadmillProtocolKind
import com.omnigart.sensors.treadmill.TreadmillSample

/**
 * Fitness Machine Status (0x2ADA) notifications.
 * Opcode + optional parameter; used to update [TreadmillMachineState].
 */
object FtmsMachineStatusParser {
    const val OP_RESET = 0x01
    const val OP_STOPPED_OR_PAUSED_BY_USER = 0x02
    const val OP_STOPPED_BY_SAFETY_KEY = 0x03
    const val OP_STARTED_OR_RESUMED_BY_USER = 0x04
    const val OP_TARGET_SPEED_CHANGED = 0x05
    const val OP_TARGET_INCLINE_CHANGED = 0x06
    const val OP_CONTROL_PERMISSION_LOST = 0x13

    data class MachineStatusEvent(
        val opcode: Int,
        val state: TreadmillMachineState,
        val targetSpeedKmh: Double? = null,
        val targetInclinePercent: Double? = null
    )

    fun parse(bytes: ByteArray): Result<MachineStatusEvent> = runCatching {
        BleByteReaders.requireRemaining(bytes, 0, 1, "machine status opcode")
        val opcode = BleByteReaders.uint8(bytes, 0)
        when (opcode) {
            OP_RESET ->
                MachineStatusEvent(opcode, TreadmillMachineState.Idle)
            OP_STOPPED_OR_PAUSED_BY_USER -> {
                val param =
                    if (bytes.size > 1) BleByteReaders.uint8(bytes, 1) else 0x01
                val state =
                    if (param == 0x02) {
                        TreadmillMachineState.Paused
                    } else {
                        TreadmillMachineState.Stopped
                    }
                MachineStatusEvent(opcode, state)
            }
            OP_STOPPED_BY_SAFETY_KEY ->
                MachineStatusEvent(opcode, TreadmillMachineState.SafetyKeyRemoved)
            OP_STARTED_OR_RESUMED_BY_USER ->
                MachineStatusEvent(opcode, TreadmillMachineState.Running)
            OP_TARGET_SPEED_CHANGED -> {
                BleByteReaders.requireRemaining(bytes, 1, 2, "target speed")
                MachineStatusEvent(
                    opcode = opcode,
                    state = TreadmillMachineState.Running,
                    targetSpeedKmh = BleByteReaders.uint16Le(bytes, 1) / 100.0
                )
            }
            OP_TARGET_INCLINE_CHANGED -> {
                BleByteReaders.requireRemaining(bytes, 1, 2, "target incline")
                MachineStatusEvent(
                    opcode = opcode,
                    state = TreadmillMachineState.Running,
                    targetInclinePercent = BleByteReaders.sint16Le(bytes, 1) / 10.0
                )
            }
            OP_CONTROL_PERMISSION_LOST ->
                MachineStatusEvent(opcode, TreadmillMachineState.Error)
            else ->
                MachineStatusEvent(opcode, TreadmillMachineState.Unknown)
        }
    }

    fun toSamplePatch(event: MachineStatusEvent): TreadmillSample = TreadmillSample(
        speedKmh = event.targetSpeedKmh,
        inclinePercent = event.targetInclinePercent,
        protocol = TreadmillProtocolKind.Ftms,
        machineState = event.state
    )
}
