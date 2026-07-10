package com.carecompanion.app.data.repo

import android.app.Activity
import android.content.Context
import com.carecompanion.app.auth.FirebaseAuthManager
import com.carecompanion.app.auth.OtpRequest
import com.carecompanion.app.data.model.UserDto
import com.carecompanion.app.data.remote.SupabaseService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
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
    @ApplicationContext private val context: Context,
    private val authManager: FirebaseAuthManager,
    private val api: SupabaseService,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()
    private val prefs = context.getSharedPreferences("cc_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false }

    val currentUid: String? get() = authManager.currentUid

    suspend fun refresh() {
        val uid = authManager.currentUid
        if (uid == null) { _state.value = SessionState.LoggedOut; return }
        loadUser(uid)
    }

    /**
     * Only a SUCCESSFUL empty query means NeedsRole. A network/parse failure must NOT
     * be treated as "no user" — otherwise a transient blip would re-provision and
     * overwrite an existing account. On failure we fall back to the cached user.
     */
    private suspend fun loadUser(uid: String) {
        val result = runCatching { api.getUsers(firebaseUid = "eq.$uid") }
        _state.value = when {
            result.isSuccess -> {
                val user = result.getOrNull()?.firstOrNull()
                if (user == null) {
                    // genuinely no row for this uid → needs role (unless we have a stale cache for a different uid)
                    if (cachedUser()?.firebaseUid == uid) SessionState.Ready(cachedUser()!!) else SessionState.NeedsRole
                } else { cacheUser(user); SessionState.Ready(user) }
            }
            // network/parse error: use cache if it belongs to this uid, else keep them logged in but retryable
            cachedUser()?.firebaseUid == uid -> SessionState.Ready(cachedUser()!!)
            else -> SessionState.LoggedOut
        }
    }

    // ── OTP ──────────────────────────────────────────────────────────────────
    suspend fun requestOtp(activity: Activity, phoneE164: String): OtpRequest =
        authManager.requestOtp(activity, phoneE164)

    suspend fun signInWithCode(verificationId: String, code: String) {
        authManager.signInWithCode(verificationId, code)
        authManager.currentUid?.let { loadUser(it) }
    }

    suspend fun completeAutoVerified(credential: com.google.firebase.auth.PhoneAuthCredential) {
        authManager.signInWithCredential(credential)
        authManager.currentUid?.let { loadUser(it) }
    }

    /**
     * First-time provisioning. Re-checks for an existing row first so an erroneous
     * NeedsRole (e.g. earlier network blip) can NEVER overwrite an existing user's
     * role/identity.
     */
    suspend fun provisionUser(role: String, name: String, phone: String) {
        val uid = authManager.currentUid ?: error("not signed in")
        val existing = runCatching { api.getUsers(firebaseUid = "eq.$uid").firstOrNull() }.getOrNull()
        if (existing != null) {
            cacheUser(existing); _state.value = SessionState.Ready(existing); syncFcmToken(); return
        }
        val fcm = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        val saved = api.upsertUser(
            UserDto(firebaseUid = uid, phone = phone, role = role, name = name.ifBlank { null }, fcmToken = fcm)
        ).firstOrNull() ?: UserDto(firebaseUid = uid, phone = phone, role = role, name = name)
        if (role == "guardian") runCatching { api.rpcResolvePendingLinks() }
        cacheUser(saved)
        _state.value = SessionState.Ready(saved)
    }

    suspend fun syncFcmToken() {
        val uid = authManager.currentUid ?: return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        runCatching { api.patchUser("eq.$uid", mapOf("fcm_token" to token)) }
    }

    fun signOut() {
        authManager.signOut()
        prefs.edit().remove("user").apply()
        _state.value = SessionState.LoggedOut
    }

    private fun cacheUser(u: UserDto) = prefs.edit().putString("user", json.encodeToString(UserDto.serializer(), u)).apply()
    private fun cachedUser(): UserDto? =
        prefs.getString("user", null)?.let { runCatching { json.decodeFromString(UserDto.serializer(), it) }.getOrNull() }
}
