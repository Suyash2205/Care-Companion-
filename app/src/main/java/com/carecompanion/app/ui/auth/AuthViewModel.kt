package com.carecompanion.app.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.repo.AuthRepository
import com.carecompanion.app.data.repo.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val role: String = "guardian",   // chosen on the login card before signing in
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    val session: StateFlow<SessionState> = authRepo.state

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    /** Fresh state on each entry so a previous session's error never lingers. */
    fun reset() { _ui.value = AuthUiState() }

    fun setRole(role: String) { _ui.value = _ui.value.copy(role = role, error = null) }
    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    /**
     * Show the Google account picker and sign in. [activityContext] must be an Activity
     * context — Credential Manager renders a system dialog.
     */
    fun signInWithGoogle(activityContext: Context) {
        if (_ui.value.loading) return          // guard against a double-tap opening two pickers
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val result = authRepo.signInWithGoogle(activityContext)
                ensureProvisioned(result.displayName)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = friendlyError(e))
            }
        }
    }

    /** After sign-in: if this is a brand-new user, create the users row with the chosen role. */
    private suspend fun ensureProvisioned(displayName: String?) {
        when (authRepo.state.value) {
            is SessionState.NeedsRole -> {
                // Phone is no longer collected at login; guardians add the elder's number
                // on the profile itself (it is still used for SOS/dialling).
                authRepo.provisionUser(_ui.value.role, name = displayName.orEmpty(), phone = "")
                authRepo.syncFcmToken()
            }
            is SessionState.Ready -> authRepo.syncFcmToken()
            else -> {}
        }
        _ui.value = _ui.value.copy(loading = false)
    }

    /**
     * Credential Manager throws for ordinary situations too (user dismissed the sheet,
     * no Google account on the device), so translate rather than dumping a raw message
     * that would read as a crash to an elder.
     */
    private fun friendlyError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("cancel", true) || msg.contains("dismiss", true) ->
                "Sign-in was cancelled."
            msg.contains("No credential", true) || msg.contains("no matching", true) ->
                "No Google account found on this phone. Add one in Settings, then try again."
            msg.contains("network", true) || msg.contains("Unable to resolve host", true) ->
                "No internet connection. Connect and try again."
            else -> "Could not sign in. Please try again."
        }
    }
}
