package com.carecompanion.app.ui.elder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.repo.VoiceRepository
import com.carecompanion.app.voice.Speaker
import com.carecompanion.app.voice.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the spoken exchange has got to. */
enum class VoicePhase { IDLE, SPEAKING, LISTENING, THINKING, UNCLEAR, UNAVAILABLE }

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.IDLE,
    val transcript: String = "",
)

/**
 * Runs one spoken exchange per dose: say the medicine, listen, decide.
 *
 * The buttons on the screen stay live throughout. Voice is an additional way to answer,
 * never the only one — recognition is least reliable for exactly the people this screen
 * is built for, and an unanswered dose is reported to the guardian as missed.
 *
 * Anything short of a confident answer ends in [VoicePhase.UNCLEAR], which asks rather
 * than assumes. A medication record is never written from a guess.
 */
@HiltViewModel
class VoiceDoseViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val voice: VoiceRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(VoiceUiState())
    val ui: StateFlow<VoiceUiState> = _ui.asStateFlow()

    private val speaker by lazy { Speaker(appContext) }
    private var job: Job? = null

    /** True when this device can speak the elder's language at all. */
    fun canSpeak(lang: String): Boolean = speaker.isSupported(lang)

    private fun hasMic(): Boolean = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Speak the prompt, listen for a reply, and report what was understood.
     *
     * [onAnswer] receives true for taken and false for not taken, and is called only on
     * a confident classification.
     */
    fun ask(prompt: String, medicine: String, lang: String, onAnswer: (Boolean) -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            _ui.value = VoiceUiState(VoicePhase.SPEAKING)
            val spoke = speaker.speak(prompt, lang)
            if (!spoke) {
                // No voice for this language on this device; stay silent rather than
                // read a Marathi sentence in an English voice.
                _ui.value = VoiceUiState(VoicePhase.UNAVAILABLE)
                return@launch
            }

            // Checked here as well as in the UI: this view model must never reach the
            // recorder without the permission, whatever calls it.
            if (!hasMic()) {
                _ui.value = VoiceUiState(VoicePhase.UNAVAILABLE)
                return@launch
            }

            _ui.value = VoiceUiState(VoicePhase.LISTENING)
            // SecurityException is caught explicitly rather than through runCatching:
            // the permission can be revoked between the check above and this call, and
            // static analysis cannot see a check made in a helper.
            val wav = try {
                VoiceRecorder.record()
            } catch (_: SecurityException) {
                _ui.value = VoiceUiState(VoicePhase.UNAVAILABLE)
                return@launch
            } catch (_: Exception) {
                null
            }
            if (wav == null) {
                _ui.value = VoiceUiState(VoicePhase.UNCLEAR)
                return@launch
            }

            _ui.value = VoiceUiState(VoicePhase.THINKING)
            val result = voice.classify(wav, lang, medicine)

            if (!result.configured) {
                _ui.value = VoiceUiState(VoicePhase.UNAVAILABLE)
                return@launch
            }
            when (result.intent) {
                "taken", "not_taken" -> {
                    _ui.value = VoiceUiState(VoicePhase.IDLE, result.transcript)
                    speaker.speak(result.reply, lang)
                    onAnswer(result.intent == "taken")
                }
                "repeat" -> ask(prompt, medicine, lang, onAnswer)
                else -> {
                    _ui.value = VoiceUiState(VoicePhase.UNCLEAR, result.transcript)
                    speaker.speak(result.reply, lang)
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        speaker.stop()
        _ui.value = VoiceUiState(VoicePhase.IDLE)
    }

    override fun onCleared() {
        job?.cancel()
        speaker.release()
    }
}
