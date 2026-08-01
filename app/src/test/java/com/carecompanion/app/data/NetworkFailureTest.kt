package com.carecompanion.app.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Timeouts and dropped connections (real-world "elder's phone lost signal" scenarios)
 * must surface as a plain IOException the caller can catch — not hang forever, not crash.
 */
class NetworkFailureTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `no response before client timeout fails gracefully as IOException`() = runTest {
        val api = TestSupport.buildService(server, timeoutSeconds = 1)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val result = runCatching { api.getElders() }

        assertTrue("a hung connection must time out into a failure, not crash or hang forever", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `connection dropped at start fails gracefully as IOException`() = runTest {
        val api = TestSupport.buildService(server)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = runCatching { api.getElders() }

        assertTrue("a dropped connection must surface as a failure, not crash", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `connection dropped mid-response body fails gracefully as IOException`() = runTest {
        val api = TestSupport.buildService(server)
        server.enqueue(
            MockResponse()
                .setBody("[{\"name\":\"Truncated Elder\"")
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
        )

        val result = runCatching { api.getElders() }

        assertTrue("a body that cuts off mid-stream must fail, not crash", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `unresolvable host fails gracefully as IOException (offline simulation)`() = runTest {
        // Simulates "truly offline": no server reachable at all, not even a MockWebServer.
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(1, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val contentType = "application/json".toMediaType()
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("http://this-host-does-not-resolve.invalid/")
            .client(client)
            .addConverterFactory(TestSupport.json.asConverterFactory(contentType))
            .build()
        val api = retrofit.create(com.carecompanion.app.data.remote.SupabaseService::class.java)

        val result = runCatching { api.getElders() }

        assertTrue("an unreachable host must fail, not crash", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
