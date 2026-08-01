package com.carecompanion.app.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Supplies the current Firebase ID token for Supabase third-party auth. */
@Singleton
class TokenProvider @Inject constructor() {

    /** Blocking fetch for use inside the OkHttp interceptor (runs on an OkHttp thread). */
    fun currentIdTokenBlocking(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            Tasks.await(user.getIdToken(false)).token
        } catch (e: Exception) {
            null
        }
    }

    /** Blocking force-refresh, for the interceptor's one-shot retry after a 401. */
    fun forceRefreshedIdTokenBlocking(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            Tasks.await(user.getIdToken(true)).token
        } catch (e: Exception) {
            null
        }
    }

    suspend fun currentIdToken(forceRefresh: Boolean = false): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            user.getIdToken(forceRefresh).await().token
        } catch (e: Exception) {
            null
        }
    }
}
