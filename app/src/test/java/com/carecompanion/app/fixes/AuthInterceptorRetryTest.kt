package com.carecompanion.app.fixes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.BuildConfig
import com.carecompanion.app.data.remote.AuthInterceptor
import com.carecompanion.app.data.remote.TokenProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Tests AuthInterceptor's 401 retry.
 *
 * LIMITATION (stated explicitly, not worked around): [TokenProvider] is a concrete,
 * non-`open` Kotlin class whose methods call the real `FirebaseAuth.getInstance()` /
 * `FirebaseAuth.getInstance().currentUser`. There is no mockito/mockk dependency in this
 * module to build a test double, and the class cannot be subclassed on the JVM. So the
 * "signed-in user, 401 forces a refresh, retry succeeds with the new token" path — the
 * actual headline behavior of the fix — CANNOT be exercised here without either adding a
 * mocking library or a real signed-in FirebaseUser (which requires live/emulated auth).
 * That path is therefore NOT covered by this test class.
 *
 * What IS genuinely testable with a real TokenProvider, by initializing a FirebaseApp with
 * dummy (non-networked) options and leaving the user signed OUT (currentUser == null):
 *  - the anon-key fallback header path (apikey + Authorization headers are always present)
 *  - that a signed-out request does NOT retry on 401 (the interceptor's own guard is
 *    `response.code == 401 && firebaseToken != null` — with no signed-in user there is no
 *    fresher token to fetch, so it must make exactly one request, not loop pointlessly).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthInterceptorRetryTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val context: Context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000000000")
                    .setApiKey("fake-api-key")
                    .setProjectId("cc-test-project")
                    .build(),
            )
        }

        val tokenProvider = TokenProvider()
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun request() = Request.Builder().url(server.url("/rest/v1/whatever")).build()

    @Test
    fun `apikey and Authorization headers are present on every request`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        client.newCall(request()).execute().use { resp ->
            assertEquals(200, resp.code)
        }

        val recorded = server.takeRequest()
        assertEquals(BuildConfig.SUPABASE_ANON_KEY, recorded.getHeader("apikey"))
        assertNotNull("Authorization header must be present", recorded.getHeader("Authorization"))
        assertEquals("Bearer ${BuildConfig.SUPABASE_ANON_KEY}", recorded.getHeader("Authorization"))
    }

    @Test
    fun `a 200 response makes exactly one request (no needless retry)`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        client.newCall(request()).execute().use { resp ->
            assertEquals(200, resp.code)
        }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `signed-out 401 is not retried (no fresher token exists to fetch)`() {
        // With no signed-in FirebaseUser, currentIdTokenBlocking() returns null, so the
        // interceptor's guard (`firebaseToken != null`) must skip the retry entirely.
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"unauthorized"}"""))

        client.newCall(request()).execute().use { resp ->
            assertEquals(401, resp.code)
        }

        assertEquals("signed-out 401 must not trigger a retry", 1, server.requestCount)
    }
}
