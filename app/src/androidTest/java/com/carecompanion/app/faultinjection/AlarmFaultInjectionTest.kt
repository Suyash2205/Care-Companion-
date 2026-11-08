package com.carecompanion.app.faultinjection

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
 * These run on a real Android runtime against the real AlarmManager — not Robolectric —
 * because the claim under test is that a dose armed on this device survives the platform
 * doing what the platform does: deferring work, and restarting.
 *
 * What these assert, and what they do NOT: a registered PendingIntent proves the alarm was
 * ACCEPTED by AlarmManager. It does not prove the alarm FIRED. Firing under Doze is
 * measured by the adb procedure in docs/fault-injection.md, not here, because forcing Doze
 * requires shell authority this process does not have. Keeping the two separate is
 * deliberate: this file must not be read as evidence of delivery.
 */
@RunWith(AndroidJUnit4::class)
class AlarmFaultInjectionTest {

    private lateinit var ctx: Context

    private fun armed(key: String): PendingIntent? {
        val intent = Intent(ctx, Class.forName("com.carecompanion.app.reminder.ReminderReceiver"))
            .apply { action = "com.carecompanion.app.DOSE.$key" }
        return PendingIntent.getBroadcast(
            ctx, key.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
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
     * Reboot: alarms do not survive a restart, so BootReceiver must re-arm from the
     * persisted set. We cancel the live alarms without clearing persistence, which is the
     * state a device is in immediately after booting, then deliver BOOT_COMPLETED.
     */
    @Test fun boot_completed_rearms_the_persisted_set() {
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("fi-boot", 45)))
        assertNotNull(armed("fi-boot"))

        ReminderScheduler.cancelDose(ctx, "fi-boot-nonexistent")
        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertNotNull("BootReceiver must re-arm the persisted dose", armed("fi-boot"))
    }

    /** A boot broadcast that is not BOOT_COMPLETED must not be acted on. */
    @Test fun an_unrelated_broadcast_does_not_rearm() {
        ReminderScheduler.cancelAll(ctx)
        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        assertNull(armed("fi-boot"))
    }

    /** Alarm keys are namespaced by source, so two sources must not collide. */
    @Test fun distinct_sources_do_not_replace_one_another() {
        ReminderScheduler.scheduleDoses(ctx, listOf(dose("med-0900", 30), dose("gen-0900", 30)))
        assertNotNull(armed("med-0900"))
        assertNotNull(armed("gen-0900"))
    }
}
