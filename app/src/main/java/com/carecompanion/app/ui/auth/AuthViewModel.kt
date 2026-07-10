package com.carecompanion.app.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.auth.OtpRequest
import com.carecompanion.app.data.repo.AuthRepository
import com.carecompanion.app.data.repo.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val phase: Phase = Phase.PHONE,
    val role: String = "guardian",         // chosen on the login card before OTP
    val phone: String = "",
    val verificationId: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
) {
    enum class Phase { PHONE, OTP }
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    val session: StateFlow<SessionState> = authRepo.state

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    fun setRole(role: String) { _ui.value = _ui.value.copy(role = role) }
    fun setPhone(p: String) { _ui.value = _ui.value.copy(phone = p) }
    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    fun requestOtp(activity: Activity) {
        val phone = normalize(_ui.value.phone)
        if (phone.length < 10) { _ui.value = _ui.value.copy(error = "Enter a valid phone number"); return }
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                when (val r = authRepo.requestOtp(activity, phone)) {
                    is OtpRequest.AutoVerified -> {
                        authRepo.completeAutoVerified(r.credential)
                        ensureProvisioned()
                    }
                    is OtpRequest.CodeSent ->
                        _ui.value = _ui.value.copy(loading = false, phase = AuthUiState.Phase.OTP, verificationId = r.verificationId)
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Failed to send OTP")
            }
        }
    }

    fun verifyOtp(code: String) {
        val vid = _ui.value.verificationId ?: return
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                authRepo.signInWithCode(vid, code)
                ensureProvisioned()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = "Incorrect code, try again")
            }
        }
    }

    /** After sign-in: if this is a brand-new user, create the users row with the chosen role. */
    private suspend fun ensureProvisioned() {
        when (authRepo.state.value) {
            is SessionState.NeedsRole -> {
                authRepo.provisionUser(_ui.value.role, name = "", phone = normalize(_ui.value.phone))
                authRepo.syncFcmToken()
            }
            is SessionState.Ready -> authRepo.syncFcmToken()
            else -> {}
        }
        _ui.value = _ui.value.copy(loading = false)
    }

    private fun normalize(raw: String): String {
        val digits = raw.filter { it.isDigit() || it == '+' }
        return if (digits.startsWith("+")) digits else "+91$digits"   // default India country code
    }
}
