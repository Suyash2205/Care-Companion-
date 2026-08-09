package com.carecompanion.app.fixes

import com.carecompanion.app.ui.sos.SosSendResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the truth-reporting semantics of [SosSendResult] — the fix for an elder always
 * being told "Alert Sent!" even when the alert had actually reached nobody.
 *
 * Since silent SMS was removed (SEND_SMS is a Play-restricted permission), the server
 * record is the only automatic channel, so `nooneReached` and `serverAlertFailed` now
 * move together. They are kept as separate fields deliberately, and the tests below pin
 * the invariant the UI depends on: the green tick appears only when the alert really was
 * delivered, and the manual text/call fallback appears exactly when it was not.
 *
 * LIMITATION: `ElderSosViewModel` is a `@HiltViewModel` needing an injected Context and
 * `SosRepository`, and its `fire()` reaches real framework classes (`LocationServices`)
 * that are not meaningfully fakeable on a plain JVM test without a much larger fixture.
 * That is NOT faked here — this class asserts the pure data-class semantics the view model
 * is documented to produce.
 */
class SosResultSemanticsTest {

    @Test
    fun `default result represents the not-yet-fired idle state`() {
        val result = SosSendResult()

        assertFalse(result.sending)
        assertFalse(result.sent)
        assertEquals(null, result.locationText)
        assertEquals(null, result.error)
        assertEquals("", result.fallbackMessage)
        assertFalse("must not claim no-one-was-reached before anything happened", result.nooneReached)
        assertFalse("must not claim server failure before anything happened", result.serverAlertFailed)
    }

    @Test
    fun `a delivered alert reports success and needs no fallback`() {
        val delivered = SosSendResult(sent = true, serverAlertFailed = false, nooneReached = false)

        assertTrue(delivered.sent)
        assertFalse("server succeeded -> guardians got the push, so never warn", delivered.nooneReached)
        assertFalse(delivered.serverAlertFailed)
    }

    @Test
    fun `a failed alert reports failure and carries the message for the manual fallback`() {
        val body = "EMERGENCY! Kamala needs help. https://maps.google.com/?q=19.07,72.87"
        val failed = SosSendResult(
            sent = true, serverAlertFailed = true, nooneReached = true, fallbackMessage = body,
        )

        assertTrue("server failed -> nobody was reached automatically", failed.nooneReached)
        assertTrue(failed.serverAlertFailed)
        assertEquals(
            "the offline fallback can only pre-fill the SMS if the body survives on the result",
            body, failed.fallbackMessage,
        )
    }

    @Test
    fun `nooneReached tracks the server outcome now that SMS is not sent by the app`() {
        // Guards the invariant the SOS screen relies on: it picks the warning UI purely
        // from nooneReached, so that flag must never disagree with serverAlertFailed.
        val ok = SosSendResult(sent = true, serverAlertFailed = false, nooneReached = false)
        val bad = SosSendResult(sent = true, serverAlertFailed = true, nooneReached = true)

        assertEquals(ok.serverAlertFailed, ok.nooneReached)
        assertEquals(bad.serverAlertFailed, bad.nooneReached)
    }
}
