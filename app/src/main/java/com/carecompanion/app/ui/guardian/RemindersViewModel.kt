package com.carecompanion.app.ui.guardian

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.ReminderCategoryDto
import com.carecompanion.app.data.model.ReminderDto
import com.carecompanion.app.data.repo.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemindersUiState(
    val loading: Boolean = true,
    val categories: List<ReminderCategoryDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val care: CareRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(RemindersUiState())
    val ui: StateFlow<RemindersUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            try {
                val cats = care.reminderCategories(elderId)
                val rems = care.reminders(elderId)
                _ui.value = RemindersUiState(loading = false, categories = cats, reminders = rems)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun addReminder(title: String, categoryId: String?, times: List<String>, days: Int) {
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                care.addReminder(
                    ReminderDto(
                        elderId = elderId,
                        categoryId = categoryId,
                        title = title.trim(),
                        times = times,
                        repeatKind = if (days == 127) "daily" else "days",
                        days = days,
                        enabled = true,
                    )
                )
            }.onSuccess {
                _ui.value = _ui.value.copy(saving = false)
                load()
            }.onFailure {
                _ui.value = _ui.value.copy(saving = false, error = it.message ?: "Failed to save")
            }
        }
    }

    fun toggleReminder(r: ReminderDto) = viewModelScope.launch {
        val id = r.id ?: return@launch
        runCatching { care.updateReminder(id, r.copy(enabled = !r.enabled)) }.onSuccess { load() }
    }

    fun deleteReminder(id: String) = viewModelScope.launch {
        runCatching { care.deleteReminder(id) }.onSuccess { load() }
    }

    fun addCustomCategory(name: String) {
        if (name.isBlank()) return
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                care.addReminderCategory(
                    ReminderCategoryDto(
                        elderId = elderId,
                        name = name.trim(),
                        isDefault = false,
                        isVirtual = false,
                    )
                )
            }.onSuccess {
                _ui.value = _ui.value.copy(saving = false)
                load()
            }.onFailure {
                _ui.value = _ui.value.copy(saving = false, error = it.message ?: "Failed to save")
            }
        }
    }
}
