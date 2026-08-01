package com.carecompanion.app.notify

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.reminder.ReminderReceiver
import com.carecompanion.app.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Proves that when a medicine-reminder alarm actually fires (ReminderReceiver.onReceive),
 * the elder gets a real, high-priority notification — not just a scheduled-but-silent alarm.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ReminderNotificationTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(NotificationManager::class.java)
        // POST_NOTIFICATIONS (API 33+) must be granted or Notifications.safeNotify swallows
        // the SecurityException and no notification is posted at all — grant it explicitly so
        // this test proves the notification-construction path, not the permission-request path.
        shadowOf(context as Application).grantPermissions("android.permission.POST_NOTIFICATIONS")
    }

    @Test
    fun `firing the alarm receiver posts a notification with correct title and non-empty body`() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_TITLE, "Time for Metformin")
            putExtra(ReminderScheduler.EXTRA_BODY, "500mg · with water")
        }

        ReminderReceiver().onReceive(context, intent)

        val posted = shadowOf(notificationManager).allNotifications
        assertEquals("expected exactly one notification to be posted", 1, posted.size)
        val n = posted.single()
        assertEquals("Time for Metformin", n.extras.getString("android.title"))
        val body = n.extras.getString("android.text")
        assertNotNull(body)
        assertFalse("notification body must not be empty", body!!.isBlank())
        assertEquals("500mg · with water", body)
    }

    @Test
    fun `reminder falls back to a safe default title-body when extras are missing`() {
        val intent = Intent(context, ReminderReceiver::class.java) // no extras at all

        ReminderReceiver().onReceive(context, intent)

        val posted = shadowOf(notificationManager).allNotifications
        assertEquals(1, posted.size)
        val n = posted.single()
        assertEquals("Medicine reminder", n.extras.getString("android.title"))
        assertFalse(n.extras.getString("android.text")!!.isBlank())
    }

    @Test
    fun `reminders channel exists and is IMPORTANCE_HIGH so it actually alerts the elder`() {
        Notifications.ensureChannels(context)
        val channel = notificationManager.getNotificationChannel(Notifications.CHANNEL_REMINDERS)
        assertNotNull("cc_reminders channel must exist", channel)
        assertEquals(
            "a LOW/DEFAULT importance channel would post silently (no heads-up, no sound) " +
                "and an elder would never notice a medicine reminder",
            NotificationManager.IMPORTANCE_HIGH,
            channel!!.importance,
        )
    }

    @Test
    fun `alerts channel (SOS + missed-dose) also exists and is IMPORTANCE_HIGH`() {
        Notifications.ensureChannels(context)
        val channel = notificationManager.getNotificationChannel(Notifications.CHANNEL_ALERTS)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel!!.importance)
    }

    @Test
    fun `posting a reminder notification implicitly creates the channels if missing`() {
        // showReminder() calls ensureChannels() itself; simulate firing before any explicit setup.
        assertEquals(null, notificationManager.getNotificationChannel(Notifications.CHANNEL_REMINDERS))

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_TITLE, "Time for Aspirin")
            putExtra(ReminderScheduler.EXTRA_BODY, "one tablet")
        }
        ReminderReceiver().onReceive(context, intent)

        assertNotNull(notificationManager.getNotificationChannel(Notifications.CHANNEL_REMINDERS))
    }
}
