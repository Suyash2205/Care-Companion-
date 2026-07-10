package com.carecompanion.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when an exact alarm triggers for a due medicine schedule or reminder.
 *  Fleshed out in the reminder-engine phase. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.handleFire(context, intent)
    }
}
