package com.carecompanion.app.notify

import android.app.AlarmManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.reminder.BootReceiver
import com.carecompanion.app.reminder.DoseAlarm
import com.carecompanion.app.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Proves that medicine reminders survive a device reboot: BootReceiver is declared in the
 * manifest for ACTION_BOOT_COMPLETED, and delivering that intent re-arms the persisted,
 * still-future doses without needing the elder to reopen the app.
 *
 * Times are expressed relative to the real "now" — Robolectric cannot move the wall clock
 * that ReminderScheduler reads, so pinning a fake date would only produce false results.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class BootPersistenceTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        drainAlarms()
        context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE).edit().clear().commit()
    }

    /**
     * Robolectric's `getScheduledAlarms()` returns a COPY, so calling `.clear()` on it does
     * nothing. `getNextScheduledAlarm()` removes-and-returns, so draining in a loop is the
     * only way to genuinely empty the shadow's alarm table between phases of a test.
     */
    private fun drainAlarms() {
        while (shadowOf(alarmManager).getNextScheduledAlarm() != null) { /* drain */ }
    }

    private fun dose(key: String, minutesFromNow: Long) = DoseAlarm(
        key = key,
        timeMillis = System.currentTimeMillis() + minutesFromNow * 60_000L,
        title = "Time for Aspirin",
        body = "500mg",
    )

    @Test
    fun `BootReceiver is declared in the manifest for ACTION_BOOT_COMPLETED`() {
        val componentName = ComponentName(context, BootReceiver::class.java)
        val info = context.packageManager.getReceiverInfo(componentName, PackageManager.GET_META_DATA)
        assertTrue("BootReceiver must be registered or reminders die at reboot", info != null)
    }

    @Test
    fun `delivering ACTION_BOOT_COMPLETED re-arms the persisted future doses`() {
        // Arrange: arm today's doses, then simulate the device powering off by clearing
        // the in-memory alarm table (persisted prefs survive, exactly like a real reboot).
        ReminderScheduler.scheduleDoses(context, listOf(dose("s1", 60), dose("s2", 180)))
        assertEquals(2, shadowOf(alarmManager).scheduledAlarms.size)
        drainAlarms()
        assertEquals(0, shadowOf(alarmManager).scheduledAlarms.size)

        // Act: the device boots.
        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        // Assert: reminders are back without the elder opening the app.
        assertEquals(
            "both still-future doses must be re-armed after a reboot",
            2,
            shadowOf(alarmManager).scheduledAlarms.size,
        )
    }

    @Test
    fun `boot re-arm ignores a non-boot intent`() {
        ReminderScheduler.scheduleDoses(context, listOf(dose("s1", 60)))
        drainAlarms()

        BootReceiver().onReceive(context, Intent(Intent.ACTION_SCREEN_ON))

        assertEquals(
            "only ACTION_BOOT_COMPLETED should trigger a re-arm",
            0,
            shadowOf(alarmManager).scheduledAlarms.size,
        )
    }

    @Test
    fun `boot re-arm does not resurrect doses whose time has already passed`() {
        // A dose that elapsed while the phone was off must not fire late on boot.
        ReminderScheduler.scheduleDoses(context, listOf(dose("past", -30), dose("future", 90)))
        drainAlarms()

        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val armed = shadowOf(alarmManager).scheduledAlarms
        assertEquals("only the still-future dose is re-armed", 1, armed.size)
        assertTrue(armed[0].triggerAtTime > System.currentTimeMillis())
    }

    @Test
    fun `persisted doses are day-stamped so a stale day is not restored`() {
        ReminderScheduler.scheduleDoses(context, listOf(dose("s1", 60)))

        val prefs = context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE)
        assertTrue("a saved_day stamp guards against restoring yesterday's doses",
            prefs.contains("saved_day"))

        // Simulate the day having rolled over while the device was off.
        prefs.edit().putLong("saved_day", -1L).commit()
        drainAlarms()

        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertEquals(
            "a stale (previous-day) set must not be re-armed; DailyArmWorker rebuilds the new day",
            0,
            shadowOf(alarmManager).scheduledAlarms.size,
        )
    }
}
