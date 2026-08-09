package com.carecompanion.app.ui.guardian

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.OttShortcutDto
import com.carecompanion.app.data.repo.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OttUiState(
    val loading: Boolean = true,
    val shortcuts: List<OttShortcutDto> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OttViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val care: CareRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(OttUiState())
    val ui: StateFlow<OttUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { care.ott(elderId) }
                .onSuccess { _ui.value = OttUiState(loading = false, shortcuts = it) }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message ?: "Couldn't load. Please check your connection and try again.") }
        }
    }

    fun addPreset(presetKey: String, title: String, packageName: String) {
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                care.addOtt(
                    OttShortcutDto(
                        elderId = elderId,
                        title = title.trim(),
                        kind = "preset",
                        presetKey = presetKey,
                        packageOrUrl = packageName,
                        sort = _ui.value.shortcuts.size,
                    )
                )
            }
                .onSuccess { _ui.value = _ui.value.copy(saving = false); load() }
                .onFailure { _ui.value = _ui.value.copy(saving = false, error = it.message ?: "Failed to save") }
        }
    }

    fun addCustom(title: String, url: String) {
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                care.addOtt(
                    OttShortcutDto(
                        elderId = elderId,
                        title = title.trim(),
                        kind = "custom",
                        presetKey = null,
                        packageOrUrl = url.trim(),
                        sort = _ui.value.shortcuts.size,
                    )
                )
            }
                .onSuccess { _ui.value = _ui.value.copy(saving = false); load() }
                .onFailure { _ui.value = _ui.value.copy(saving = false, error = it.message ?: "Failed to save") }
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        runCatching { care.deleteOtt(id) }.onSuccess { load() }
            .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "Couldn't remove the shortcut") }
    }
}
