package com.carecompanion.app.ui.guardian

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.UserDto
import com.carecompanion.app.data.repo.AuthRepository
import com.carecompanion.app.data.repo.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuardianSettingsUiState(
    val user: UserDto? = null,
    val notificationsEnabled: Boolean = true,
)

@HiltViewModel
class GuardianSettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("cc_guardian_settings", Context.MODE_PRIVATE)

    private val _ui = MutableStateFlow(
        GuardianSettingsUiState(
            user = (authRepo.state.value as? SessionState.Ready)?.user,
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        )
    )
    val ui: StateFlow<GuardianSettingsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.state.collect { state ->
                _ui.value = _ui.value.copy(user = (state as? SessionState.Ready)?.user)
            }
        }
    }

    fun load() {
        _ui.value = _ui.value.copy(
            user = (authRepo.state.value as? SessionState.Ready)?.user,
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        )
    }

    fun setNotifications(enabled: Boolean) {
        runCatching { prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply() }
        _ui.value = _ui.value.copy(notificationsEnabled = enabled)
    }

    fun logout() = authRepo.signOut()

    private companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
    }
}
