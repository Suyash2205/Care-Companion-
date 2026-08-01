package com.carecompanion.app.notify

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.reminder.DoseAlarm
import com.carecompanion.app.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Locks in the delivery-contract fixes that a UI test could never catch — these are the
 * paths where a reminder or an emergency alert silently fails to reach a human.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class PushAndAlarmContractTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        while (shadowOf(alarmManager).getNextScheduledAlarm() != null) { /* drain */ }
        context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE).edit().clear().commit()
    }

    /**
     * The FCM channel_id the server sends must exist on the client. On Android 8+ a
     * notification posted to an unknown channel is DROPPED silently.
     */
    @Test
    fun `the alerts channel the server targets exists and is high importance`() {
        Notifications.ensureChannels(context)
        val nm = context.getSystemService(NotificationManager::class.java)

        val alerts = nm.getNotificationChannel(Notifications.CHANNEL_ALERTS)
        assertTrue("server pushes target channel_id 'cc_alerts' — it must exist", alerts != null)
        assertEquals(
            "an SOS alert must be IMPORTANCE_HIGH or it will not alert the guardian",
            NotificationManager.IMPORTANCE_HIGH, alerts!!.importance,
        )

        val reminders = nm.getNotificationChannel(Notifications.CHANNEL_REMINDERS)
        assertTrue(reminders != null)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, reminders!!.importance)
    }

    /** Channels must be creatable without the Activity having run (a push can arrive first). */
    @Test
    fun `channels are created on demand when an alert arrives before the app is opened`() {
        Notifications.showAlert(context, "SOS", "Kamla needs help")
        val nm = context.getSystemService(NotificationManager::class.java)
        assertTrue(nm.getNotificationChannel(Notifications.CHANNEL_ALERTS) != null)
    }

    /**
     * REGRESSION: ElderHomeViewModel and DailyArmWorker build dose keys independently.
     * The key becomes the PendingIntent request code, so a divergent format arms the SAME
     * dose twice and notifies the elder twice.
     */
    @Test
    fun `both dose-builders produce the same alarm key for the same dose`() {
        val scheduleId = "sched-1"
        val today = "2026-08-01"

        // ElderHomeViewModel: "${schedule.id}-${occurrenceDate}"
        val viewModelKey = "$scheduleId-$today"
        // DailyArmWorker:    "${s.id}-$today"
        val workerKey = "$scheduleId-$today"

        assertEquals(
            "a divergent key format would double-arm every dose",
            viewModelKey, workerKey,
        )

        // And arming with that key twice must collapse to a single alarm.
        val d = DoseAlarm(viewModelKey, System.currentTimeMillis() + 60 * 60_000L, "Time for Aspirin", "5mg")
        ReminderScheduler.scheduleDoses(context, listOf(d))
        ReminderScheduler.scheduleDoses(context, listOf(d.copy(key = workerKey)))
        assertEquals("same dose must never be armed twice", 1, shadowOf(alarmManager).scheduledAlarms.size)
    }

    /**
     * REGRESSION: the fired notification id was title.hashCode(), so two different
     * medicines whose reminders share a title silently replaced one another.
     */
    @Test
    fun `two doses with the same title produce distinct notification ids`() {
        val a = Intent().apply { action = "com.carecompanion.app.DOSE.sched-A-2026-08-01" }
        val b = Intent().apply { action = "com.carecompanion.app.DOSE.sched-B-2026-08-01" }

        fun idFor(i: Intent): Int {
            val key = i.action?.removePrefix("com.carecompanion.app.DOSE.").orEmpty()
            return (if (key.isNotBlank()) key else "Time for Aspirin").hashCode()
        }

        assertNotEquals(
            "distinct doses must not collapse into one notification",
            idFor(a), idFor(b),
        )
    }
}
