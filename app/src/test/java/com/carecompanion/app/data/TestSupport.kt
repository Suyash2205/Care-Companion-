package com.carecompanion.app.data

import com.carecompanion.app.data.remote.SupabaseService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Shared plumbing for data-layer tests: builds a [SupabaseService] wired to a
 * [MockWebServer] using the *exact same* [Json] configuration as
 * `NetworkModule.provideJson()`, so serialization behavior in tests matches production.
 */
object TestSupport {

    /** Must stay in lockstep with com.carecompanion.app.di.NetworkModule.provideJson(). */
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
        coerceInputValues = true
    }

    fun buildService(server: MockWebServer, timeoutSeconds: Long = 2): SupabaseService {
        val client = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(SupabaseService::class.java)
    }
}
