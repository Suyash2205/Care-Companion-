package com.carecompanion.app.fixes

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.data.model.ReminderDto
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
 * Covers general reminders (water, walk, vitals) reaching the elder's phone.
 *
 * The guardian could create these, they saved to the database and listed on the guardian's
 * Reminders screen — and then nothing delivered them. No alarm was armed on the elder's
 * device, and the only server cron job scans missed medicine doses. A "drink water at
 * 11:00" reminder never arrived, which is exactly what was reported.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class GeneralRemindersArmedTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    private val today = "2026-08-10"
    private val allDays = 127

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(AlarmManager::class.java)
        while (shadowOf(alarmManager).nextScheduledAlarm != null) { /* drain */ }
        context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun armedCount() = shadowOf(alarmManager).scheduledAlarms.size

    private fun water(times: List<String>, days: Int = allDays, enabled: Boolean = true) =
        ReminderDto(
            id = "rem-1", elderId = "e1", title = "Drink water",
            times = times, days = days, enabled = enabled,
        )

    /** A time far enough ahead that scheduleDoses() won't discard it as past. */
    private fun laterToday(): String {
        val c = java.util.Calendar.getInstance()
        c.add(java.util.Calendar.MINUTE, 30)
        return "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun `an enabled reminder for today produces one alarm per time`() {
        val alarms = ReminderScheduler.remindersToAlarms(
            listOf(water(listOf("09:00", "11:00", "16:00"))), 1 shl 0, today,
        )

        assertEquals(3, alarms.size)
        assertTrue(alarms.all { it.title == "Drink water" })
    }

    @Test
    fun `reminder keys cannot collide with dose keys`() {
        // Both are hashed into the PendingIntent request code and the notification id,
        // so a collision would mean one silently replacing the other.
        val reminder = ReminderScheduler.remindersToAlarms(listOf(water(listOf("08:00"))), 1 shl 0, today).single()
        val doseKey = "sched-1-$today"

        assertTrue("reminder keys must be namespaced", reminder.key.startsWith("rem-"))
        assertTrue(reminder.key != doseKey)
        assertTrue("the time must be part of the key", reminder.key.contains("08:00"))
    }

    @Test
    fun `a disabled reminder is not armed`() {
        val alarms = ReminderScheduler.remindersToAlarms(
            listOf(water(listOf("09:00"), enabled = false)), 1 shl 0, today,
        )
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun `a reminder not scheduled for today is not armed`() {
        // Reminder runs Mondays only (bit 0); today is Wednesday (bit 2).
        val alarms = ReminderScheduler.remindersToAlarms(
            listOf(water(listOf("09:00"), days = 1 shl 0)), 1 shl 2, today,
        )
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun `doses and reminders armed together both survive - neither wipes the other`() {
        // scheduleDoses() replaces the persisted set, so both must go in one call.
        val dose = DoseAlarm("sched-1-$today", System.currentTimeMillis() + 30 * 60_000L, "Time for Thyroid", "Breakfast")
        val reminders = ReminderScheduler.remindersToAlarms(listOf(water(listOf(laterToday()))), 1 shl 0, today)

        ReminderScheduler.scheduleDoses(context, listOf(dose) + reminders)

        assertEquals("the medicine dose and the water reminder must both be armed", 2, armedCount())

        // And both must come back after a reboot.
        while (shadowOf(alarmManager).nextScheduledAlarm != null) { /* drain */ }
        ReminderScheduler.rescheduleAll(context)
        assertEquals(2, armedCount())
    }

    @Test
    fun `a reminder with no id is skipped rather than crashing`() {
        val alarms = ReminderScheduler.remindersToAlarms(
            listOf(water(listOf("09:00")).copy(id = null)), 1 shl 0, today,
        )
        assertTrue(alarms.isEmpty())
    }
}
