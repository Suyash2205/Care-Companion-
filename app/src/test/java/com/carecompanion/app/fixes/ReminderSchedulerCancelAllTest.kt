package com.carecompanion.app.fixes

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.reminder.DoseAlarm
import com.carecompanion.app.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests ReminderScheduler.cancelAll — used on sign-out so a shared device never keeps
 * firing a previous elder's medicine reminders.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSchedulerCancelAllTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        drainAlarms()
        context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE).edit().clear().commit()
    }

    /** getScheduledAlarms() returns a COPY; getNextScheduledAlarm() removes-and-returns. */
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
    fun `cancelAll removes every armed alarm`() {
        ReminderScheduler.scheduleDoses(context, listOf(dose("s1", 30), dose("s2", 90), dose("s3", 150)))
        assertEquals(3, shadowOf(alarmManager).scheduledAlarms.size)

        ReminderScheduler.cancelAll(context)

        assertEquals(
            "no alarms may remain armed after cancelAll",
            0,
            shadowOf(alarmManager).scheduledAlarms.size,
        )
    }

    @Test
    fun `cancelAll clears the persisted dose set so a reboot re-arms nothing`() {
        ReminderScheduler.scheduleDoses(context, listOf(dose("s1", 30), dose("s2", 90)))
        val before = context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE)
            .getString("today_doses", null)
        assertEquals(true, before != null && before.contains("s1"))

        ReminderScheduler.cancelAll(context)

        val after = context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE)
            .getString("today_doses", null)
        assertNull("persisted doses must be gone, otherwise rescheduleAll (boot) would re-arm them", after)
    }

    @Test
    fun `after cancelAll a subsequent rescheduleAll (boot) arms nothing`() {
        ReminderScheduler.scheduleDoses(context, listOf(dose("s1", 30)))
        ReminderScheduler.cancelAll(context)

        ReminderScheduler.rescheduleAll(context)

        assertEquals(
            "cancelAll must leave nothing for a subsequent boot re-arm to pick up",
            0,
            shadowOf(alarmManager).scheduledAlarms.size,
        )
    }
}
