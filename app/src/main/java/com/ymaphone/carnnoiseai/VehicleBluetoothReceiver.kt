package com.ymaphone.carnoiseai

import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

private fun Intent.vehicleDevice(): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        legacyBluetoothDevice()
    }
}

@Suppress("DEPRECATION")
private fun Intent.legacyBluetoothDevice(): BluetoothDevice? =
    getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

class VehicleBluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("carnoise", Context.MODE_PRIVATE)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // A Bluetooth connection may be restored before this app can observe it.
                // Never leave a stale driving DND policy active after a reboot.
                DrivingProfile.restore(context, force = true)
                prefs.edit().putBoolean("vehicle_connected", false).apply()
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val address = try {
                    intent.vehicleDevice()?.address
                } catch (_: SecurityException) {
                    null
                }

                if (address != null && address == prefs.getString("vehicle_address", null)) {
                    prefs.edit().putBoolean("vehicle_connected", true).apply()
                    DrivingProfile.apply(context)
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val address = try {
                    intent.vehicleDevice()?.address
                } catch (_: SecurityException) {
                    null
                }

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
        if (!prefs.getBoolean("profile_enabled", true)) {
            restore(context, force = true)
            return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return

        if (!prefs.getBoolean("dnd_was_applied", false)) {
            val oldPolicy = nm.notificationPolicy
            prefs.edit()
                .putInt("previous_filter", nm.currentInterruptionFilter)
                .putInt("previous_categories", oldPolicy.priorityCategories)
                .putInt("previous_call_senders", oldPolicy.priorityCallSenders)
                .putInt("previous_message_senders", oldPolicy.priorityMessageSenders)
                .putInt("previous_suppressed_effects", oldPolicy.suppressedVisualEffects)
                .putBoolean("dnd_was_applied", true)
                .apply()
        }

        var categories =
            (if (prefs.getBoolean("allow_calls", false)) NotificationManager.Policy.PRIORITY_CATEGORY_CALLS else 0) or
            (if (prefs.getBoolean("allow_messages", false)) NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES else 0)

        // Alarms and media became configurable DND categories in Android 9 (API 28).
        if (Build.VERSION.SDK_INT >= 28) {
            if (prefs.getBoolean("allow_alarms", true)) {
                categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
            }
            if (prefs.getBoolean("allow_media", true)) {
                categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA
            }
        }

        val policy = NotificationManager.Policy(
            categories,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            0
        )

        nm.notificationPolicy = policy
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun restore(context: Context, force: Boolean = false) {
        val prefs = context.getSharedPreferences("carnoise", Context.MODE_PRIVATE)
        if (!force && !prefs.getBoolean("restore_dnd", true)) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted || !prefs.getBoolean("dnd_was_applied", false)) return

        val categories = prefs.getInt("previous_categories", 0)
        val callSenders = prefs.getInt(
            "previous_call_senders",
            NotificationManager.Policy.PRIORITY_SENDERS_ANY
        )
        val messageSenders = prefs.getInt(
            "previous_message_senders",
            NotificationManager.Policy.PRIORITY_SENDERS_ANY
        )

        nm.notificationPolicy = NotificationManager.Policy(
            categories,
            callSenders,
            messageSenders,
            prefs.getInt("previous_suppressed_effects", 0)
        )

        nm.setInterruptionFilter(
            prefs.getInt("previous_filter", NotificationManager.INTERRUPTION_FILTER_ALL)
        )
        prefs.edit().putBoolean("dnd_was_applied", false).apply()
    }
}
