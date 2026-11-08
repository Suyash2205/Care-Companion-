package com.carecompanion.app.faultinjection

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carecompanion.app.reminder.BootReceiver
import com.carecompanion.app.reminder.DoseAlarm
import com.carecompanion.app.reminder.ReminderScheduler
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device fault injection for the reminder path, written for the ICISS camera-ready.
 *
 * These run on a real Android runtime against the real AlarmManager, because the claim
 * under test is that a dose armed on this device survives the platform doing what the
 * platform does: deferring work, and restarting.
 *
 * WHAT THE ORACLE MEANS. [armed] asks whether a PendingIntent token exists. That is only
 * a valid proxy for "an alarm is live" because production now cancels the token alongside
 * the alarm; `AlarmManager.cancel(pi)` on its own retires the alarm and leaves the token,
 * so an earlier version of this file reported two false failures. If that production
 * behaviour changes, this oracle silently stops meaning anything.
 *
 * WHAT THESE DO NOT SHOW. A live token proves the alarm was ACCEPTED by AlarmManager. It
 * does not prove the alarm FIRED. Firing under Doze requires shell authority this process
 * does not have and is measured by the adb procedure, not here. Do not cite this file as
 * evidence of delivery.
 */
@RunWith(AndroidJUnit4::class)
class AlarmFaultInjectionTest {

    private lateinit var ctx: Context

    private fun pendingIntentFor(key: String, create: Boolean): PendingIntent? {
        val intent = Intent(ctx, Class.forName("com.carecompanion.app.reminder.ReminderReceiver"))
            .apply { action = "com.carecompanion.app.DOSE.$key" }
        val flags = (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE) or
            PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, key.hashCode(), intent, flags)
    }

    private fun armed(key: String): PendingIntent? = pendingIntentFor(key, create = false)

    /** Clear the live alarm WITHOUT touching persistence — the state a device is in after a reboot. */
    private fun simulateRebootLosingAlarms(key: String) {
        val pi = pendingIntentFor(key, create = true)!!
        ctx.getSystemService(AlarmManager::class.java)?.cancel(pi)
        pi.cancel()
    }

    private fun dose(key: String, inMinutes: Long) = DoseAlarm(
        key = key,
        timeMillis = System.currentTimeMillis() + inMinutes * 60_000L,
        title = "Take your tablet",
        body = "Metformin, 1 tablet"
    )

    @Before fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        ReminderScheduler.cancelAll(ctx)
    }

    @Test fun a_future_dose_is_accepted_by_the_platform() {
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("fi-future", 30)))
        assertNotNull("a future dose must be armed", armed("fi-future"))
    }

    @Test fun a_dose_already_in_the_past_is_not_armed() {
        val past = DoseAlarm("fi-past", System.currentTimeMillis() - 60_000L, "t", "b")
        ReminderScheduler.scheduleDoses(ctx, listOf(past))
        assertNull("a dose whose time has passed must not be armed", armed("fi-past"))
    }

    /** The double-dosing defect: answering early must retract the pending alarm. */
    @Test fun answering_a_dose_cancels_its_pending_alarm() {
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("fi-cancel", 30)))
        assertNotNull(armed("fi-cancel"))
        ReminderScheduler.cancelDose(ctx, "fi-cancel")
        assertNull("an answered dose must not still be armed", armed("fi-cancel"))
    }

    /**
     * The orphaned-alarm defect, found by fault injection on an emulator.
     *
     * scheduleDoses() replaces the persisted set. It used not to cancel alarms for keys
     * dropped from the new list, so a medicine deleted or re-timed during the day stayed
     * armed while no longer being tracked: cancelAll() could not see it, and it still
     * rang. That is a double-dose prompt, delivered silently.
     */
    @Test fun a_dose_dropped_from_a_later_schedule_is_disarmed() {
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("fi-drop-a", 30), dose("fi-drop-b", 40)))
        assertNotNull(armed("fi-drop-a"))
        assertNotNull(armed("fi-drop-b"))

        // The guardian deletes the first medicine; the next arming pass carries only the second.
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("fi-drop-b", 40)))

        assertNull("a dose dropped from the schedule must not stay armed", armed("fi-drop-a"))
        assertNotNull("the surviving dose must stay armed", armed("fi-drop-b"))
    }

    /** And the orphan must not outlive a sign-out either. */
    @Test fun cancel_all_reaches_every_armed_dose() {
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("fi-all-a", 30), dose("fi-all-b", 40)))
        ReminderScheduler.cancelAll(ctx)
        assertNull(armed("fi-all-a"))
        assertNull(armed("fi-all-b"))
    }

    /** Reboot: alarms do not survive a restart, so BootReceiver must re-arm from persistence. */
    @Test fun boot_completed_rearms_the_persisted_set() {
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("fi-boot", 45)))
        simulateRebootLosingAlarms("fi-boot")
        assertNull("precondition: the alarm must be gone before the boot broadcast", armed("fi-boot"))

        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertNotNull("BootReceiver must re-arm the persisted dose", armed("fi-boot"))
    }

    /** A broadcast that is not BOOT_COMPLETED must not re-arm anything. */
    @Test fun an_unrelated_broadcast_does_not_rearm() {
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("fi-unrelated", 45)))
        simulateRebootLosingAlarms("fi-unrelated")

        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED))

        assertNull("only BOOT_COMPLETED may re-arm", armed("fi-unrelated"))
    }

    /**
     * Key namespacing, using production's real key shapes: doses are "<scheduleId>-<date>"
     * and general reminders are "rem-<id>-<time>-<date>". The hash of the key is both the
     * PendingIntent request code and the notification id, so a collision means one
     * silently replacing the other.
     */
    @Test fun a_dose_and_a_general_reminder_at_the_same_time_do_not_collide() {
        val doseKey = "sched-4821-2026-11-09"
        val reminderKey = "rem-4821-09:00-2026-11-09"
        ReminderScheduler.scheduleDoses(ctx, listOf(dose(doseKey, 30), dose(reminderKey, 30)))
        assertNotNull(armed(doseKey))
        assertNotNull(armed(reminderKey))
    }
}
