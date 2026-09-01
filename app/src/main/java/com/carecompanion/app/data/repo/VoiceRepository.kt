package com.carecompanion.app.data.repo

import android.util.Base64
import com.carecompanion.app.data.model.VoiceIntentRequest
import com.carecompanion.app.data.model.VoiceIntentResponse
import com.carecompanion.app.data.remote.SupabaseService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepository @Inject constructor(
    private val api: SupabaseService,
) {
    /**
     * Send a recorded utterance for recognition and classification.
     *
     * Any failure returns "unclear" rather than propagating, because the caller's only
     * sensible response to a failure is the same as to an unclear answer: ask again, or
     * fall back to the buttons. A medication record is never written from a guess.
     */
    suspend fun classify(wav: ByteArray, lang: String, medicine: String): VoiceIntentResponse =
        runCatching {
            api.voiceIntent(
                VoiceIntentRequest(
                    audio = Base64.encodeToString(wav, Base64.NO_WRAP),
                    lang = lang,
                    medicine = medicine,
                )
            )
        }.getOrElse { VoiceIntentResponse(intent = "unclear", error = it.message) }
}
