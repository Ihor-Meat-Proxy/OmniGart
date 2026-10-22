package com.omnigart.sensors.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.omnigart.sensors.ble.BleHeartRateScanner

/**
 * Foreground service with `connectedDevice` type — keeps the BLE HR GATT link
 * alive while the app is backgrounded during a workout.
 *
 * Host app must:
 * 1. Request `FOREGROUND_SERVICE_CONNECTED_DEVICE` + notification permission.
 * 2. Start via [start] / stop via [stop].
 */
class BleHeartRateForegroundService : Service() {

    private var scanner: BleHeartRateScanner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                ensureChannel()
                val notification = buildNotification(
                    intent?.getStringExtra(EXTRA_TITLE) ?: "Heart rate sensor connected",
                )
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    } else {
                        0
                    },
                )
                if (scanner == null) {
                    scanner = BleHeartRateScanner(this)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scanner?.disconnect()
        scanner?.stopScan()
        scanner = null
        super.onDestroy()
    }

    private fun ensureChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sensor connection",
            NotificationManager.IMPORTANCE_LOW,
        )
        mgr.createNotificationChannel(channel)
    }

    private fun buildNotification(title: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("OmniGart is receiving heart rate from your strap")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

    companion object {
        const val CHANNEL_ID = "omnigart.sensors.connected_device"
        const val NOTIFICATION_ID = 18_013
        const val ACTION_STOP = "com.omnigart.sensors.STOP"
        const val EXTRA_TITLE = "title"

        fun start(context: Context, title: String? = null) {
            val intent = Intent(context, BleHeartRateForegroundService::class.java).apply {
                if (title != null) putExtra(EXTRA_TITLE, title)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BleHeartRateForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
