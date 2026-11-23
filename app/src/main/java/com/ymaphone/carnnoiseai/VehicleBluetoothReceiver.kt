package com.ymaphone.carnnoiseai

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.os.Build

class VehicleBluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("carnoise", Context.MODE_PRIVATE)
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val address = try { device?.address } catch (_: SecurityException) { null }
                if (address != null && address == prefs.getString("vehicle_address", null)) {
                    prefs.edit().putBoolean("vehicle_connected", true).apply()
                    DrivingProfile.apply(context)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val address = try { device?.address } catch (_: SecurityException) { null }
                if (address != null && address == prefs.getString("vehicle_address", null)) {
                    prefs.edit().putBoolean("vehicle_connected", false).apply()
                    DrivingProfile.restore(context)
                }
            }
        }
    }
}

object DrivingProfile {
    fun apply(context: Context) {
        val prefs = context.getSharedPreferences("carnoise", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("profile_enabled", true)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return
        val policy = NotificationManager.Policy(
            if (prefs.getBoolean("allow_alarms", true)) NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS else 0,
            0,
            0,
            NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
        )
        nm.notificationPolicy = policy
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences("carnoise", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("restore_dnd", true)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }
}
