package com.ymaphone.carnoiseai

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
        if (!prefs.getBoolean("dnd_was_applied", false)) {
            prefs.edit().putInt("previous_filter", nm.currentInterruptionFilter).putBoolean("dnd_was_applied", true).apply()
        }
        val categories =
            (if (prefs.getBoolean("allow_calls", false)) NotificationManager.Policy.PRIORITY_CATEGORY_CALLS else 0) or
            (if (prefs.getBoolean("allow_messages", false)) NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES else 0) or
            (if (prefs.getBoolean("allow_media", true)) NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA else 0) or
            (if (prefs.getBoolean("allow_alarms", true)) NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS else 0)
        val policy = NotificationManager.Policy(
            categories,
            NotificationManager.Policy.PRIORITY_SENDERS_NONE,
            NotificationManager.Policy.PRIORITY_SENDERS_NONE,
            NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
        )
        nm.notificationPolicy = policy
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences("carnoise", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("restore_dnd", true)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted && prefs.getBoolean("dnd_was_applied", false)) {
            nm.setInterruptionFilter(prefs.getInt("previous_filter", NotificationManager.INTERRUPTION_FILTER_ALL))
            prefs.edit().putBoolean("dnd_was_applied", false).apply()
        }
    }
}
