package com.carecompanion.app.reminder

import android.content.Context
import android.content.Intent

/**
 * Schedules and handles exact alarms for medicine schedules + activity reminders.
 * Real scheduling logic lands in the reminder-engine phase; the entry points exist
 * now so the manifest receivers resolve and callers can be wired incrementally.
 */
object ReminderScheduler {

    const val EXTRA_KIND = "kind"          // schedule | reminder
    const val EXTRA_SOURCE_ID = "source_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"

    fun handleFire(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val id = (intent.getStringExtra(EXTRA_SOURCE_ID) ?: title).hashCode()
        com.carecompanion.app.notify.Notifications.showReminder(context, id, title, body)
    }

    fun rescheduleAll(context: Context) {
        // TODO(reminder-phase): reload today's schedules/reminders from Room and re-arm alarms.
    }
}
