package com.carecompanion.app.notify

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
import java.util.Calendar

/**
 * Proves the medicine-reminder alarm-scheduling path (ReminderScheduler) behaves correctly
 * under Robolectric's ShadowAlarmManager — no real-time waiting involved.
 *
 * Time handling: Robolectric does not let us move the wall clock that
 * `System.currentTimeMillis()` (and therefore ReminderScheduler) reads, so every assertion
 * here is expressed RELATIVE to the real "now". That is deterministic, and avoids asserting
 * a fixed calendar date which would break depending on the machine's timezone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ReminderSchedulerTest {

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

    private fun dose(key: String, minutesFromNow: Long, title: String = "Time for Aspirin") = DoseAlarm(
        key = key,
        timeMillis = System.currentTimeMillis() + minutesFromNow * 60_000L,
        title = title,
        body = "500mg",
    )

    // ── 1. Alarms get scheduled ──────────────────────────────────────────────

    @Test
    fun `each future dose registers one alarm at its exact trigger time`() {
        val doses = listOf(dose("s1", 30), dose("s2", 90), dose("s3", 150))

        ReminderScheduler.scheduleDoses(context, doses)

        val armed = shadowOf(alarmManager).scheduledAlarms
        assertEquals("one alarm per future dose", 3, armed.size)
        assertEquals(
            "alarms must fire at exactly the dose times",
            doses.map { it.timeMillis }.sorted(),
            armed.map { it.triggerAtTime }.sorted(),
        )
    }

    @Test
    fun `a dose whose time has already passed today is not armed`() {
        // A past-time dose must be dropped, not fired immediately (which would nag an
        // elder with a stale reminder the moment they open the app).
        ReminderScheduler.scheduleDoses(context, listOf(dose("past", -60), dose("future", 60)))

        val armed = shadowOf(alarmManager).scheduledAlarms
        assertEquals("only the future dose is armed", 1, armed.size)
        assertTrue("the armed alarm is in the future", armed[0].triggerAtTime > System.currentTimeMillis())
    }

    @Test
    fun `an empty dose list arms nothing and does not crash`() {
        ReminderScheduler.scheduleDoses(context, emptyList())
        assertEquals(0, shadowOf(alarmManager).scheduledAlarms.size)
    }

    // ── 2. timeMillisToday ───────────────────────────────────────────────────

    @Test
    fun `timeMillisToday maps HH-MM onto today at that local hour and minute`() {
        val result = ReminderScheduler.timeMillisToday("14:30")

        val cal = Calendar.getInstance().apply { timeInMillis = result }
        val today = Calendar.getInstance()
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals("must land on today's date", today.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `timeMillisToday falls back to 08-00 for malformed input instead of throwing`() {
        // Guards against a corrupt/empty schedule string crashing the elder's home load.
        listOf("", "abc", ":", "not-a-time").forEach { bad ->
            val cal = Calendar.getInstance().apply { timeInMillis = ReminderScheduler.timeMillisToday(bad) }
            assertEquals("malformed '$bad' should default to hour 8", 8, cal.get(Calendar.HOUR_OF_DAY))
        }
    }

    // ── 3. No duplicate alarms ───────────────────────────────────────────────

    @Test
    fun `arming the same dose set twice does not stack duplicate alarms`() {
        val doses = listOf(dose("s1", 30), dose("s2", 90))

        ReminderScheduler.scheduleDoses(context, doses)
        ReminderScheduler.scheduleDoses(context, doses)

        // Same key -> same PendingIntent request code -> the second arm REPLACES the first.
        assertEquals(
            "re-arming must replace, not stack (an elder must not get the same reminder twice)",
            2,
            shadowOf(alarmManager).scheduledAlarms.size,
        )
    }

    // ── 4. Persistence for boot re-arm ───────────────────────────────────────

    @Test
    fun `armed doses are persisted so they can be re-armed after a reboot`() {
        ReminderScheduler.scheduleDoses(context, listOf(dose("s1", 45), dose("s2", 120)))

        val raw = context.getSharedPreferences("cc_reminders", Context.MODE_PRIVATE)
            .getString("today_doses", null)
        assertTrue("today's doses must be persisted for BootReceiver", raw != null && raw.contains("s1"))
    }

    // ── 5. Doze-readiness ────────────────────────────────────────────────────

    @Test
    fun `alarms are armed as wakeup alarms so they fire with the screen off`() {
        ReminderScheduler.scheduleDoses(context, listOf(dose("s1", 60)))

        val armed = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, armed.size)
        assertEquals(
            "must be RTC_WAKEUP — a non-wakeup alarm would not rouse the device for a dose",
            AlarmManager.RTC_WAKEUP,
            armed[0].type,
        )
    }
}
