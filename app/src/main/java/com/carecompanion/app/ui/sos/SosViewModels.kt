package com.carecompanion.app.ui.sos

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.SosEventDto
import com.carecompanion.app.data.repo.SosRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class SosSendResult(
    val sending: Boolean = false,
    val sent: Boolean = false,
    val locationText: String? = null,
    val error: String? = null,
    /**
     * The alert reached NOBODY. Identical to [serverAlertFailed] today because the server
     * record is the only automatic channel; kept separate so adding a channel later can
     * change one without silently changing the other.
     */
    val nooneReached: Boolean = false,
    /** True when the server record failed, so guardians get no push/dashboard entry. */
    val serverAlertFailed: Boolean = false,
    /**
     * The ready-to-send emergency text, including the maps link. Only used for the manual
     * fallback the elder is offered when [serverAlertFailed] — we never send it silently.
     */
    val fallbackMessage: String = "",
)

/**
 * Elder side: fire the alert by recording it on the server, which pushes to every linked
 * guardian.
 *
 * Deliberately does NOT send SMS itself. Silent background SMS needs the SEND_SMS
 * permission, which Play restricts to apps whose core purpose is messaging — declaring it
 * here risks the whole app being blocked from publishing. When the server call fails (the
 * offline case SMS used to cover) the UI offers a one-tap "send text" that hands a
 * pre-filled message to the phone's own messaging app instead.
 */
@HiltViewModel
class ElderSosViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sosRepo: SosRepository,
) : ViewModel() {

    private val _result = MutableStateFlow(SosSendResult())
    val result: StateFlow<SosSendResult> = _result.asStateFlow()

    /** Reset before each new SOS entry so a prior "sent" result never leaks into the next. */
    fun reset() { _result.value = SosSendResult() }

    @SuppressLint("MissingPermission")
    fun fire(elderId: String, elderName: String, template: String? = null) {
        // Re-entrancy guard: a double-tap must not fire two alerts (duplicate pushes to
        // every guardian and a duplicate SOS row, in an already-stressful moment).
        if (_result.value.sending) return
        _result.value = SosSendResult(sending = true)
        viewModelScope.launch {
            // 1) location (best-effort, ~5s cap)
            val loc = withTimeoutOrNull(5000) {
                runCatching {
                    LocationServices.getFusedLocationProviderClient(appContext)
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                }.getOrNull()
            }
            val mapsLink = loc?.let { "https://maps.google.com/?q=${it.latitude},${it.longitude}" }
            val locText = loc?.let { "Location: %.5f, %.5f".format(it.latitude, it.longitude) }

            // 2) Compose the message once. Not sent from here — it is handed to the
            //    messaging app only if the elder taps the manual fallback below.
            val base = template?.takeIf { it.isNotBlank() }?.replace("{name}", elderName)
                ?: "EMERGENCY! $elderName needs help."
            val body = if (mapsLink != null) "$base $mapsLink" else base

            // 3) Server record — this is what reaches guardians (dashboard + FCM push),
            //    so a failure here is NOT cosmetic: retry briefly before giving up.
            var serverOk = false
            repeat(3) { attempt ->
                if (serverOk) return@repeat
                serverOk = runCatching {
                    sosRepo.trigger(elderId, loc?.latitude, loc?.longitude, loc?.accuracy?.toDouble(), mapsLink)
                }.isSuccess
                if (!serverOk && attempt < 2) kotlinx.coroutines.delay(1500L * (attempt + 1))
            }

            // Tell the elder the TRUTH — never show a reassuring tick over a failed alert.
            // When this fails the UI switches to the manual text/call fallback.
            _result.value = SosSendResult(
                sending = false,
                sent = true,
                locationText = locText,
                serverAlertFailed = !serverOk,
                nooneReached = !serverOk,
                fallbackMessage = body,
            )
        }
    }
}

data class GuardianSosUiState(
    val loading: Boolean = true,
    val events: List<SosEventDto> = emptyList(),
    val error: String? = null,
)

/** Guardian side: monitor + resolve SOS events for one elder. */
@HiltViewModel
class GuardianSosViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val sosRepo: SosRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(GuardianSosUiState())
    val ui: StateFlow<GuardianSosUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { sosRepo.history(elderId) }
                .onSuccess { _ui.value = GuardianSosUiState(false, it) }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message ?: "Couldn't load. Please check your connection and try again.") }
        }
    }

    fun resolve(id: String, status: String) = viewModelScope.launch {
        runCatching { sosRepo.updateStatus(id, status) }.onSuccess { load() }
            .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "Couldn't update the alert") }
    }
}
