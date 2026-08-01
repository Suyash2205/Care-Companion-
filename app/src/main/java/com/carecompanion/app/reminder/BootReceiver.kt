package com.carecompanion.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-registers exact alarms after device reboot. Fleshed out in the reminder-engine phase. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-arm today's already-persisted doses…
            ReminderScheduler.rescheduleAll(context)
            // …and make sure the daily refresh keeps running, so a reboot after
            // midnight (when the persisted set is stale) still rebuilds the day.
            DailyArmWorker.ensureScheduled(context)
        }
    }
}
