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
        val bearer = firebaseToken ?: BuildConfig.SUPABASE_ANON_KEY
        val request = chain.request().newBuilder()
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $bearer")
            .build()
        return chain.proceed(request)
    }
}
