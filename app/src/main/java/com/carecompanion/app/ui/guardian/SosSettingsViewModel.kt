package com.carecompanion.app.ui.guardian

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.ContactDto
import com.carecompanion.app.data.model.SosEventDto
import com.carecompanion.app.data.repo.CareRepository
import com.carecompanion.app.data.repo.ElderRepository
import com.carecompanion.app.data.repo.SosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SosSettingsUiState(
    val loading: Boolean = true,
    val contacts: List<ContactDto> = emptyList(),
    val events: List<SosEventDto> = emptyList(),
    val template: String = "",
    val error: String? = null,
)

@HiltViewModel
class SosSettingsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val care: CareRepository,
    private val sos: SosRepository,
    private val elderRepo: ElderRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(SosSettingsUiState())
    val ui: StateFlow<SosSettingsUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            runCatching {
                val contacts = care.contacts(elderId)
                val events = sos.history(elderId)
                val elder = elderRepo.getElder(elderId)
                Triple(contacts, events, elder?.sosMessage ?: "EMERGENCY! {name} needs help.")
            }.onSuccess { (contacts, events, template) ->
                _ui.value = SosSettingsUiState(
                    loading = false,
                    contacts = contacts,
                    events = events,
                    template = template,
                )
            }.onFailure {
                _ui.value = _ui.value.copy(loading = false, error = it.message)
            }
        }
    }

    fun toggleEmergency(c: ContactDto) {
        val id = c.id ?: return
        viewModelScope.launch {
            runCatching { care.updateContact(id, c.copy(isEmergency = !c.isEmergency)) }
                .onSuccess { load() }
                .onFailure { _ui.value = _ui.value.copy(error = it.message) }
        }
    }

    fun saveTemplate(text: String) {
        viewModelScope.launch {
            runCatching { elderRepo.updateElderFields(elderId, mapOf("sos_message" to text)) }
                .onSuccess { _ui.value = _ui.value.copy(template = text) }
                .onFailure { _ui.value = _ui.value.copy(error = it.message) }
        }
    }

    fun resolve(id: String) {
        viewModelScope.launch {
            runCatching { sos.updateStatus(id, "resolved") }
                .onSuccess { load() }
                .onFailure { _ui.value = _ui.value.copy(error = it.message) }
        }
    }
}
