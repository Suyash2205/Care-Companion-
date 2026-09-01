package com.carecompanion.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Speaks short prompts with the device's own text-to-speech.
 *
 * On-device rather than a cloud voice, for three reasons: it costs nothing, it works
 * with no connection, and the prompts repeat every single day so there is nothing to
 * gain from synthesising them fresh each time.
 *
 * Marathi and Gujarati voices are not installed on every device. [isSupported] reports
 * that honestly so the caller can stay silent rather than fall back to reading an
 * Indian-language sentence in an English voice, which is worse than saying nothing.
 */
class Speaker(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private val counter = AtomicInteger(0)

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
    }

    private fun localeFor(lang: String): Locale = when (lang) {
        "hi" -> Locale("hi", "IN")
        "mr" -> Locale("mr", "IN")
        "gu" -> Locale("gu", "IN")
        else -> Locale("en", "IN")
    }

    /** True when this device can actually speak [lang]. */
    fun isSupported(lang: String): Boolean {
        val engine = tts ?: return false
        if (!ready) return false
        val result = runCatching { engine.isLanguageAvailable(localeFor(lang)) }.getOrNull()
        return result == TextToSpeech.LANG_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    /** Speaks [text] and suspends until it finishes. Returns false if it could not. */
    suspend fun speak(text: String, lang: String): Boolean {
        val engine = tts ?: return false
        if (!ready || text.isBlank() || !isSupported(lang)) return false
        runCatching { engine.language = localeFor(lang) }.getOrNull() ?: return false
        // Slower than default: the listener is elderly and the text is instructions.
        runCatching { engine.setSpeechRate(0.85f) }

        val id = "cc-${counter.incrementAndGet()}"
        return suspendCancellableCoroutine { cont ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(true)
                }
                @Deprecated("Framework requires the old signature")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(false)
                }
            })
            cont.invokeOnCancellation { runCatching { engine.stop() } }
            val queued = runCatching {
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            }.getOrDefault(TextToSpeech.ERROR)
            if (queued != TextToSpeech.SUCCESS && cont.isActive) cont.resume(false)
        }
    }

    fun stop() { runCatching { tts?.stop() } }

    fun release() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
