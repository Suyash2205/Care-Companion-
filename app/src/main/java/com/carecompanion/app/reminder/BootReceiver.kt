package com.carecompanion.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-registers exact alarms after device reboot. Fleshed out in the reminder-engine phase. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.rescheduleAll(context)
        }
    }
}
