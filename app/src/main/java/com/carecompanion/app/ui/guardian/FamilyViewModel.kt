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
    /** The share code for the currently chosen access level, once generated. */
    val inviteCode: String? = null,
    val inviteAccess: String = "view",
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
                // copy(), not a fresh state: rebuilding would drop a code the guardian
                // is in the middle of reading out.
                .onSuccess { _ui.value = _ui.value.copy(loading = false, members = it, error = null) }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message ?: "Couldn't load. Please check your connection and try again.") }
        }
    }

    /**
     * Produce a share code granting [access].
     *
     * Replaces the old phone-number invite, which was stranded by the move to Google
     * Sign-In: it matched the invitee on users.phone, which is now empty for every
     * account, so every invite sat 'pending' forever — and nothing was ever sent, since
     * the project has no SMS or email integration at all.
     */
    fun createInviteCode(access: String) {
        _ui.value = _ui.value.copy(inviting = true, error = null, inviteAccess = access, inviteCode = null)
        viewModelScope.launch {
            runCatching { repo.guardianInviteCode(elderId, access) }
                .onSuccess { _ui.value = _ui.value.copy(inviting = false, inviteCode = it?.code) }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        inviting = false,
                        error = it.message ?: "Couldn't create a code. Please check your connection and try again.",
                    )
                }
        }
    }

    fun clearInviteCode() { _ui.value = _ui.value.copy(inviteCode = null) }

    fun changeAccess(guardianId: String, access: String) = viewModelScope.launch {
        runCatching { repo.setMemberAccess(elderId, guardianId, access) }.onSuccess { load() }
    }

    fun remove(guardianId: String) = viewModelScope.launch {
        runCatching { repo.removeMember(elderId, guardianId) }.onSuccess { load() }
    }
}
