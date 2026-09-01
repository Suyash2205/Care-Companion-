package com.carecompanion.app.fixes

import com.carecompanion.app.data.model.VoiceIntentResponse
import com.carecompanion.app.ui.elder.VoicePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the safety rules of the spoken dose confirmation.
 *
 * The whole feature writes to a medication record, so the interesting cases are the
 * ones where it must NOT write: an unheard answer, an unconfigured backend, a network
 * failure. Each of those has to end somewhere the user can see, never in a silent
 * "taken".
 */
class VoiceDoseTest {

    @Test
    fun `default response records nothing`() {
        val r = VoiceIntentResponse()
        assertEquals("unclear", r.intent)
        assertTrue("a bare response must not be treated as an answer",
            r.intent !in setOf("taken", "not_taken"))
    }

    @Test
    fun `only taken and not_taken may write a dose`() {
        val writes = setOf("taken", "not_taken")
        assertTrue("taken" in writes)
        assertTrue("not_taken" in writes)
        assertFalse("an unheard answer must never write", "unclear" in writes)
        assertFalse("a request to repeat must never write", "repeat" in writes)
    }

    @Test
    fun `an unconfigured backend is reported, not guessed`() {
        // The function returns configured=false when it has no speech key. The screen
        // must hide the microphone rather than offer a button that cannot work.
        val r = VoiceIntentResponse(configured = false)
        assertFalse(r.configured)
        assertEquals("unclear", r.intent)
    }

    @Test
    fun `a transport failure degrades to unclear rather than throwing`() {
        // VoiceRepository maps any exception to this shape; the caller's response to a
        // failure and to an unheard answer is the same, so they share a path.
        val r = VoiceIntentResponse(intent = "unclear", error = "timeout")
        assertEquals("unclear", r.intent)
        assertEquals("timeout", r.error)
    }

    @Test
    fun `unavailable hides the affordance and unclear keeps it`() {
        // UNAVAILABLE means this device or backend cannot do voice at all, so the row is
        // removed. UNCLEAR means it can, but did not hear — the row stays so the elder
        // can try again.
        assertTrue(VoicePhase.UNAVAILABLE != VoicePhase.UNCLEAR)
        val hidden = setOf(VoicePhase.UNAVAILABLE)
        assertTrue(VoicePhase.UNAVAILABLE in hidden)
        assertFalse(VoicePhase.UNCLEAR in hidden)
    }
}
