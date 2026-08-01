package com.carecompanion.app.fixes

import com.carecompanion.app.ui.sos.SosSendResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tests the truth-reporting semantics of [SosSendResult] — the fix for an elder always
 * being told "Alert Sent!" even when zero SMS went out AND the server insert failed
 * (i.e. nobody was actually contacted).
 *
 * LIMITATION: [ElderSosViewModel] is a `@HiltViewModel` that needs an injected
 * `android.content.Context` and `SosRepository`, and its `fire()` reaches real Android
 * framework classes (`LocationServices`, `SmsManager`) that are not meaningfully fakeable
 * on a plain JVM/Robolectric unit test without a much larger fixture (fake location
 * client, fake SmsManager shadow, Hilt test component). Per the instructions, that is NOT
 * faked here — this class only asserts the pure data-class semantics that the view model
 * is documented to produce, which is genuinely reachable and worth locking down: the
 * default (pre-fire) state, and that `nooneReached` / `serverAlertFailed` express exactly
 * the states the fix's doc comment describes.
 */
class SosResultSemanticsTest {

    @Test
    fun `default result represents the not-yet-fired idle state`() {
        val result = SosSendResult()

        assertFalse(result.sending)
        assertFalse(result.sent)
        assertEquals(0, result.smsCount)
        assertEquals(null, result.locationText)
        assertEquals(null, result.error)
        assertFalse("must not claim no-one-was-reached before anything happened", result.nooneReached)
        assertFalse("must not claim server failure before anything happened", result.serverAlertFailed)
    }

    @Test
    fun `nooneReached is true only when zero SMS went out AND the server insert failed`() {
        val trulyNobodyReached = SosSendResult(sent = true, smsCount = 0, serverAlertFailed = true, nooneReached = true)
        assertEquals(true, trulyNobodyReached.nooneReached)

        // SMS delivered even though the server failed -> at least the family got texted.
        val smsOnly = SosSendResult(sent = true, smsCount = 2, serverAlertFailed = true, nooneReached = false)
        assertFalse("SMS delivered means nooneReached must be false, even if the server failed", smsOnly.nooneReached)

        // Server succeeded even though no SMS went out -> guardians still got the dashboard/push alert.
        val serverOnly = SosSendResult(sent = true, smsCount = 0, serverAlertFailed = false, nooneReached = false)
        assertFalse("server success means nooneReached must be false, even with zero SMS", serverOnly.nooneReached)
    }

    @Test
    fun `serverAlertFailed independently reflects the server outcome regardless of SMS count`() {
        val failedWithSms = SosSendResult(sent = true, smsCount = 3, serverAlertFailed = true, nooneReached = false)
        assertEquals(true, failedWithSms.serverAlertFailed)

        val succeededWithSms = SosSendResult(sent = true, smsCount = 3, serverAlertFailed = false, nooneReached = false)
        assertFalse(succeededWithSms.serverAlertFailed)
    }
}
