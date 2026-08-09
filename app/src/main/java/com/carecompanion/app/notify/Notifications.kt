package com.carecompanion.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.carecompanion.app.MainActivity

object Notifications {
    const val CHANNEL_ALERTS = "cc_alerts"        // guardian: SOS + missed-dose
    const val CHANNEL_REMINDERS = "cc_reminders"  // elder: medicine + activity reminders

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Emergency and missed-dose alerts"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Medicine and activity reminders"
            }
        )
    }

    /** Intent extra naming the screen a notification tap should land on. */
    const val EXTRA_DEST = "cc_dest"
    const val DEST_MEDICINES = "medicines"

    /**
     * Tapping a medicine reminder should land on the medicine flow, not the home screen.
     *
     * The elder previously had to find "Medicines", then "Start taking", then answer —
     * three more taps, each one a chance to get lost, and every unconfirmed dose shows up
     * to the guardian as missed.
     *
     * The request code MUST differ from [openAppIntent]'s: with FLAG_UPDATE_CURRENT, two
     * PendingIntents sharing a request code are the same object, so the second would
     * rewrite the first and every notification would go to the same place.
     */
    fun openMedicinesIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_DEST, DEST_MEDICINES)
        return PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showAlert(context: Context, title: String, body: String) {
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        safeNotify(context, System.currentTimeMillis().toInt(), n)
    }

    fun showReminder(context: Context, id: Int, title: String, body: String, contentIntent: PendingIntent? = null) {
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent ?: openAppIntent(context))
            .build()
        safeNotify(context, id, n)
    }

    private fun safeNotify(context: Context, id: Int, n: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet; ignore.
        }
    }
}
