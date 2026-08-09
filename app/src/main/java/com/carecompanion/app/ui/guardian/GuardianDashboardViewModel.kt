package com.carecompanion.app.ui.guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.AlertDto
import com.carecompanion.app.data.model.ElderDto
import com.carecompanion.app.data.repo.AlertsRepository
import com.carecompanion.app.data.repo.ElderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val elders: List<ElderDto> = emptyList(),
    val selectedElderId: String? = null,
    val alerts: List<AlertDto> = emptyList(),
    val error: String? = null,
    val joining: Boolean = false,
    val joinError: String? = null,
) {
    val selectedElder: ElderDto? get() = elders.firstOrNull { it.id == selectedElderId }
    val unreadAlerts: Int get() = alerts.count { !it.read }
}

@HiltViewModel
class GuardianDashboardViewModel @Inject constructor(
    private val elderRepo: ElderRepository,
    private val alertsRepo: AlertsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(DashboardUiState())
    val ui: StateFlow<DashboardUiState> = _ui.asStateFlow()

    /**
     * Join an elder's circle with a code another guardian shared.
     *
     * This is the other half of the guardian invite. The previous "invite by mobile"
     * matched on users.phone, which Google Sign-In leaves empty for every account, so an
     * invited person had no way in at all.
     */
    fun joinWithCode(code: String) {
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) {
            _ui.value = _ui.value.copy(joinError = "Please enter all 6 numbers.")
            return
        }
        _ui.value = _ui.value.copy(joining = true, joinError = null)
        viewModelScope.launch {
            runCatching { elderRepo.redeemGuardianInvite(digits) }
                .onSuccess { _ui.value = _ui.value.copy(joining = false); load() }
                .onFailure {
                    // The RPC raises messages written to be shown as-is.
                    val msg = it.message.orEmpty()
                    _ui.value = _ui.value.copy(
                        joining = false,
                        joinError = when {
                            msg.contains("already been used", true) ->
                                "That code has already been used. Ask them for a new one."
                            msg.contains("already own", true) ->
                                "You already own this profile."
                            msg.contains("not valid", true) ->
                                "That code is not valid. Ask them for a new one."
                            msg.contains("Unable to resolve host", true) || msg.contains("timeout", true) ->
                                "No internet connection. Connect and try again."
                            else -> "That code did not work. Please check and try again."
                        },
                    )
                }
        }
    }

    fun clearJoinError() { _ui.value = _ui.value.copy(joinError = null) }

    fun load() {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val elders = elderRepo.listElders()
                val alerts = runCatching { alertsRepo.list() }.getOrDefault(emptyList())
                val selected = _ui.value.selectedElderId?.takeIf { id -> elders.any { it.id == id } }
                    ?: elders.firstOrNull()?.id
                _ui.value = _ui.value.copy(
                    loading = false, elders = elders, selectedElderId = selected,
                    alerts = alerts, error = null,
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun selectElder(id: String) { _ui.value = _ui.value.copy(selectedElderId = id) }

    /** Lightweight refresh of just the alert feed (used for live polling). */
    fun refreshAlerts() {
        viewModelScope.launch {
            runCatching { alertsRepo.list() }.onSuccess { _ui.value = _ui.value.copy(alerts = it) }
        }
    }
}
