package com.omnigart.sensors.android.store

import android.content.Context
import com.omnigart.sensors.model.SensorDevice
import com.omnigart.sensors.model.SensorKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight remembered-device store (SharedPreferences). App shells may later
 * replace this with SQLDelight-backed preferences from the shared core.
 */
class RememberedDevicesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _devices = MutableStateFlow(load())
    val devices: StateFlow<List<SensorDevice>> = _devices.asStateFlow()

    fun remember(device: SensorDevice) {
        val next = (_devices.value.filterNot { it.id == device.id } + device)
        persist(next)
        _devices.value = next
    }

    fun forget(deviceId: String) {
        val next = _devices.value.filterNot { it.id == deviceId }
        persist(next)
        _devices.value = next
    }

    private fun load(): List<SensorDevice> {
        val raw = prefs.getString(KEY, "[]") ?: return emptyList()
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    SensorDevice(
                        id = o.getString("id"),
                        name = o.optString("name").ifEmpty { null },
                        kind = runCatching { SensorKind.valueOf(o.getString("kind")) }
                            .getOrDefault(SensorKind.Unknown),
                    )
                )
            }
        }
    }

    private fun persist(devices: List<SensorDevice>) {
        val arr = JSONArray()
        devices.forEach { d ->
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("name", d.name)
                    .put("kind", d.kind.name)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "omnigart_sensors_remembered"
        private const val KEY = "devices"
    }
}
