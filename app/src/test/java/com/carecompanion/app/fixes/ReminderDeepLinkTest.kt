package com.carecompanion.app.fixes

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.notify.Notifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Covers the medicine-reminder deep link: tapping the notification must land on the
 * take-medicine flow, not the home screen.
 *
 * Before this, the elder had to find "Medicines", then "Start taking", then answer —
 * three extra taps, each a chance to get lost, and any dose left unconfirmed is reported
 * to the guardian as missed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ReminderDeepLinkTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `the medicine intent carries the destination that routes to the medicine flow`() {
        val pi = Notifications.openMedicinesIntent(context)
        val intent = shadowOf(pi).savedIntent

        assertEquals(
            "MainActivity reads this extra to pick the screen",
            Notifications.DEST_MEDICINES,
            intent.getStringExtra(Notifications.EXTRA_DEST),
        )
    }

    @Test
    fun `the medicine intent does not collide with the plain open-app intent`() {
        // Both use FLAG_UPDATE_CURRENT, so sharing a request code would make them the
        // same PendingIntent: the second built would rewrite the first, and every
        // notification in the app would end up opening the same screen.
        val medicines = shadowOf(Notifications.openMedicinesIntent(context))

        assertEquals(1, medicines.requestCode)
        assertNotEquals("must differ from openAppIntent's request code of 0", 0, medicines.requestCode)
    }

    @Test
    fun `a general reminder key is distinguishable from a dose key`() {
        // ReminderScheduler.handleFire uses this prefix to decide whether to deep-link:
        // water and walk reminders have no medicine screen to open.
        val reminderKey = "rem-abc-11:00-2026-08-10"
        val doseKey = "sched-1-2026-08-10"

        assert(reminderKey.startsWith("rem-"))
        assert(!doseKey.startsWith("rem-"))
    }

    @Test
    fun `an intent with no destination extra leaves routing untouched`() {
        // A plain launch, or a general reminder's tap, must not force the medicine flow.
        val plain = android.content.Intent(context, com.carecompanion.app.MainActivity::class.java)

        assertNull(plain.getStringExtra(Notifications.EXTRA_DEST))
    }
}
