package com.carecompanion.app.data.repo

import android.app.Activity
import com.carecompanion.app.auth.FirebaseAuthManager
import com.carecompanion.app.auth.OtpRequest
import com.carecompanion.app.data.model.UserDto
import com.carecompanion.app.data.remote.SupabaseService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class SessionState {
    data object Loading : SessionState()
    data object LoggedOut : SessionState()
    /** Signed in with Firebase but no users row yet — must pick a role. */
    data object NeedsRole : SessionState()
    data class Ready(val user: UserDto) : SessionState()
}

@Singleton
class AuthRepository @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val api: SupabaseService,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val currentUid: String? get() = authManager.currentUid

    /** Decide the initial route on app launch. */
    suspend fun refresh() {
        val uid = authManager.currentUid
        if (uid == null) {
            _state.value = SessionState.LoggedOut
            return
        }
        loadUser(uid)
    }

    private suspend fun loadUser(uid: String) {
        _state.value = try {
            val rows = api.getUsers(firebaseUid = "eq.$uid")
            val user = rows.firstOrNull()
            if (user == null) SessionState.NeedsRole else SessionState.Ready(user)
        } catch (e: Exception) {
            SessionState.NeedsRole
        }
    }

    // ── OTP ──────────────────────────────────────────────────────────────────
    suspend fun requestOtp(activity: Activity, phoneE164: String): OtpRequest =
        authManager.requestOtp(activity, phoneE164)

    /** Complete sign-in, then resolve session (existing user or needs-role). */
    suspend fun signInWithCode(verificationId: String, code: String) {
        authManager.signInWithCode(verificationId, code)
        authManager.currentUid?.let { loadUser(it) }
    }

    suspend fun completeAutoVerified(credential: com.google.firebase.auth.PhoneAuthCredential) {
        authManager.signInWithCredential(credential)
        authManager.currentUid?.let { loadUser(it) }
    }

    /** First-time users choose a role; creates the users row and resolves pending links. */
    suspend fun provisionUser(role: String, name: String, phone: String) {
        val uid = authManager.currentUid ?: error("not signed in")
        val fcm = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        val saved = api.upsertUser(
            UserDto(firebaseUid = uid, phone = phone, role = role, name = name.ifBlank { null }, fcmToken = fcm)
        ).firstOrNull() ?: UserDto(firebaseUid = uid, phone = phone, role = role, name = name)
        if (role == "guardian") runCatching { api.rpcResolvePendingLinks() }
        _state.value = SessionState.Ready(saved)
    }

    /** Push the FCM token to the current users row (called post-login). */
    suspend fun syncFcmToken() {
        val uid = authManager.currentUid ?: return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        runCatching { api.patchUser("eq.$uid", mapOf("fcm_token" to token)) }
    }

    fun signOut() {
        authManager.signOut()
        _state.value = SessionState.LoggedOut
    }
}
