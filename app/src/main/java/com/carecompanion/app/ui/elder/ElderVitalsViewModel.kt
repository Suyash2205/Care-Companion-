package com.carecompanion.app.ui.elder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.VitalDto
import com.carecompanion.app.data.repo.ElderRepository
import com.carecompanion.app.data.repo.Severity
import com.carecompanion.app.data.repo.VitalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ElderVitalsUiState(
    val loading: Boolean = true,
    val readings: List<VitalDto> = emptyList(),
    val severities: Map<String, Severity> = emptyMap(),
    val saving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ElderVitalsViewModel @Inject constructor(
    private val elderRepo: ElderRepository,
    private val vitals: VitalsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ElderVitalsUiState())
    val ui: StateFlow<ElderVitalsUiState> = _ui.asStateFlow()
    private var elderId: String? = null

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            try {
                val id = elderId ?: elderRepo.listElders().firstOrNull { it.isActive }?.id
                elderId = id
                if (id == null) { _ui.value = ElderVitalsUiState(false, error = "No profile"); return@launch }
                val list = vitals.list(id)
                val sev = mutableMapOf<String, Severity>()
                list.forEach { v -> v.id?.let { sev[it] = runCatching { vitals.severity(v.type, v.context, v.value1, v.value2) }.getOrDefault(Severity.NORMAL) } }
                _ui.value = ElderVitalsUiState(false, list, sev)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun add(type: String, v1: Double, v2: Double?, context: String?) {
        val id = elderId ?: return
        _ui.value = _ui.value.copy(saving = true)
        viewModelScope.launch {
            runCatching { vitals.add(VitalDto(elderId = id, type = type, value1 = v1, value2 = v2, context = context)) }
                .onSuccess { _ui.value = _ui.value.copy(saving = false); load() }
                .onFailure { _ui.value = _ui.value.copy(saving = false, error = it.message) }
        }
    }
}
