package com.carecompanion.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.repo.AuthRepository
import com.carecompanion.app.data.repo.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {
    val session: StateFlow<SessionState> = authRepo.state

    init { viewModelScope.launch { authRepo.refresh() } }

    fun signOut() = authRepo.signOut()
}
