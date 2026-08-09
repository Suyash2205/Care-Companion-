package com.carecompanion.app.fixes

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
 * Covers [ReminderScheduler.cancelDose] — the fix for an answered dose still ringing.
 *
 * Taking an 08:00 tablet at 07:45 left the 08:00 alarm armed, so the elder was told to
 * take a medicine they had already taken. For someone forgetful, that invites a second
 * dose, which is the failure mode this app exists to prevent.
 *
 * Also pins that the dose is dropped from the persisted set, since BootReceiver re-arms
 * from that set and would otherwise resurrect the cancelled alarm after a restart.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class CancelDoseAlarmTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        drainAlarms()
        context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE).edit().clear().commit()
    }

    /** ShadowAlarmManager.getScheduledAlarms() returns a COPY, so clear() on it is a no-op. */
    private fun drainAlarms() {
        val shadow = shadowOf(alarmManager)
        while (shadow.nextScheduledAlarm != null) { /* pops one each call */ }
    }

    private fun armedCount(): Int = shadowOf(alarmManager).scheduledAlarms.size

    private fun future(minutes: Int) = System.currentTimeMillis() + minutes * 60_000L

    @Test
    fun `cancelling one dose leaves the others armed`() {
        ReminderScheduler.scheduleDoses(
            context,
            listOf(
                DoseAlarm("sched-a-2026-08-09", future(30), "Time for Thyroid Medicine", "Breakfast"),
                DoseAlarm("sched-b-2026-08-09", future(60), "Time for Metformin", "Lunch"),
            ),
        )
        assertEquals(2, armedCount())

        ReminderScheduler.cancelDose(context, "sched-a-2026-08-09")

        assertEquals("only the answered dose should be cancelled", 1, armedCount())
    }

    @Test
    fun `a cancelled dose is not resurrected by a reboot`() {
        ReminderScheduler.scheduleDoses(
            context,
            listOf(
                DoseAlarm("sched-a-2026-08-09", future(30), "Time for Thyroid Medicine", "Breakfast"),
                DoseAlarm("sched-b-2026-08-09", future(60), "Time for Metformin", "Lunch"),
            ),
        )
        ReminderScheduler.cancelDose(context, "sched-a-2026-08-09")

        // BootReceiver re-arms from the persisted set.
        drainAlarms()
        ReminderScheduler.rescheduleAll(context)

        assertEquals("the cancelled dose must not come back after a restart", 1, armedCount())
    }

    @Test
    fun `cancelling an unknown key is a harmless no-op`() {
        ReminderScheduler.scheduleDoses(
            context,
            listOf(DoseAlarm("sched-a-2026-08-09", future(30), "Time for Thyroid Medicine", "Breakfast")),
        )

        ReminderScheduler.cancelDose(context, "sched-does-not-exist-2026-08-09")

        assertEquals(1, armedCount())
    }

    @Test
    fun `cancelling every dose empties the persisted set`() {
        ReminderScheduler.scheduleDoses(
            context,
            listOf(DoseAlarm("only-2026-08-09", future(45), "Time for Thyroid Medicine", "Breakfast")),
        )

        ReminderScheduler.cancelDose(context, "only-2026-08-09")
        drainAlarms()
        ReminderScheduler.rescheduleAll(context)

        assertTrue("nothing should be re-armed", armedCount() == 0)
    }
}
