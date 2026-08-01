package com.carecompanion.app.data.remote

import com.carecompanion.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the Supabase anon apikey to every request and a Bearer token that is the
 * current Firebase ID token (so Supabase RLS sees the Firebase UID). Falls back to
 * the anon key as the bearer when signed out, matching anonymous RLS behavior.
 */
class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val firebaseToken = tokenProvider.currentIdTokenBlocking()
        val response = chain.proceed(signed(chain, firebaseToken))

        // A 401 means the cached ID token was rejected (revoked, clock skew, or a refresh
        // race). Without this retry the session is permanently stuck: every subsequent
        // request fails and the user just sees empty data with no way back short of a
        // manual re-login. Force-refresh once and replay the request.
        if (response.code == 401 && firebaseToken != null) {
            val fresh = tokenProvider.forceRefreshedIdTokenBlocking()
            if (fresh != null && fresh != firebaseToken) {
                response.close()
                return chain.proceed(signed(chain, fresh))
            }
        }
        return response
    }

    private fun signed(chain: Interceptor.Chain, token: String?) =
        chain.request().newBuilder()
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer ${token ?: BuildConfig.SUPABASE_ANON_KEY}")
            .build()
}
