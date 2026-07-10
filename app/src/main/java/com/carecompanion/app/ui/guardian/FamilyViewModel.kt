package com.carecompanion.app.ui.guardian

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.GuardianLinkDto
import com.carecompanion.app.data.model.UserDto
import com.carecompanion.app.data.repo.ElderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val loading: Boolean = true,
    val members: List<Pair<GuardianLinkDto, UserDto?>> = emptyList(),
    val inviting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: ElderRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(FamilyUiState())
    val ui: StateFlow<FamilyUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { repo.members(elderId) }
                .onSuccess { _ui.value = FamilyUiState(false, it) }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message) }
        }
    }

    fun invite(phone: String, access: String) {
        _ui.value = _ui.value.copy(inviting = true, error = null)
        viewModelScope.launch {
            runCatching { repo.inviteGuardian(elderId, phone.trim(), access) }
                .onSuccess { _ui.value = _ui.value.copy(inviting = false); load() }
                .onFailure { _ui.value = _ui.value.copy(inviting = false, error = it.message ?: "Failed to send invite") }
        }
    }

    fun changeAccess(guardianId: String, access: String) = viewModelScope.launch {
        runCatching { repo.setMemberAccess(elderId, guardianId, access) }.onSuccess { load() }
    }

    fun remove(guardianId: String) = viewModelScope.launch {
        runCatching { repo.removeMember(elderId, guardianId) }.onSuccess { load() }
    }
}
