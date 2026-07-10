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

    fun load() {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val elders = elderRepo.listElders()
                val alerts = runCatching { alertsRepo.list() }.getOrDefault(emptyList())
                val selected = _ui.value.selectedElderId?.takeIf { id -> elders.any { it.id == id } }
                    ?: elders.firstOrNull()?.id
                _ui.value = DashboardUiState(false, elders, selected, alerts)
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
