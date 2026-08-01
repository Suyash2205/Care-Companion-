package com.carecompanion.app.data

import com.carecompanion.app.data.remote.CreateElderRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Garbage / malformed bodies must surface as a catchable failure (a normal Kotlin
 * exception the caller can runCatching/try-catch around) — never an unhandled crash.
 *
 * Two response shapes are exercised: a List<T>-returning endpoint (getElders) and a
 * single-object-returning endpoint (rpcCreateElder), since a body that is *valid* JSON
 * for one shape can be nonsense for the other (e.g. "[]" is a legitimate empty list,
 * but nonsense where a single object is expected).
 */
class MalformedResponseTest {

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

    private fun serve(body: String, code: Int = 200, contentType: String = "application/json") {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", contentType)
                .setBody(body)
        )
    }

    // ── List<T>-returning endpoint ──────────────────────────────────────────

    @Test
    fun `empty object body for a list endpoint fails gracefully, not crashes`() = runTest {
        val api = TestSupport.buildService(server)
        serve("{}")
        val result = runCatching { api.getElders() }
        assertTrue("expected failure for {} on a List-returning endpoint", result.isFailure)
        assertTrue(result.exceptionOrNull() is SerializationException)
    }

    @Test
    fun `empty array body for a list endpoint is a valid degenerate case, not a crash`() = runTest {
        val api = TestSupport.buildService(server)
        serve("[]")
        val result = runCatching { api.getElders() }
        assertTrue("an empty array is legitimately an empty list, must not throw", result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrNull())
    }

    @Test
    fun `null body for a list endpoint fails gracefully, not crashes`() = runTest {
        val api = TestSupport.buildService(server)
        serve("null")
        val result = runCatching { api.getElders() }
        assertTrue("expected failure for null body on a non-nullable List return type", result.isFailure)
    }

    @Test
    fun `invalid json for a list endpoint fails gracefully, not crashes`() = runTest {
        val api = TestSupport.buildService(server)
        serve("{ this is not : valid json at all [[[")
        val result = runCatching { api.getElders() }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SerializationException)
    }

    @Test
    fun `html error page for a list endpoint fails gracefully, not crashes`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            body = "<html><head><title>502 Bad Gateway</title></head><body>Bad Gateway</body></html>",
            code = 200, // some proxies return 200 with an HTML body on failure
            contentType = "text/html",
        )
        val result = runCatching { api.getElders() }
        assertTrue("HTML where JSON was expected must not crash the app", result.isFailure)
    }

    // ── single-object-returning endpoint (RPC) ──────────────────────────────

    @Test
    fun `empty object body for a single-object endpoint fails gracefully (missing required field)`() = runTest {
        val api = TestSupport.buildService(server)
        serve("{}")
        val result = runCatching {
            api.rpcCreateElder(
                CreateElderRequest(name = "New Elder")
            )
        }
        assertTrue("ElderDto.name is required with no default; {} must fail, not crash", result.isFailure)
    }

    @Test
    fun `array body for a single-object endpoint fails gracefully, not crashes`() = runTest {
        val api = TestSupport.buildService(server)
        serve("[]")
        val result = runCatching {
            api.rpcCreateElder(
                CreateElderRequest(name = "New Elder")
            )
        }
        assertTrue("an array where a single object is expected must fail, not crash", result.isFailure)
    }

    @Test
    fun `null body for a single-object endpoint fails gracefully, not crashes`() = runTest {
        val api = TestSupport.buildService(server)
        serve("null")
        val result = runCatching {
            api.rpcCreateElder(
                CreateElderRequest(name = "New Elder")
            )
        }
        assertTrue("null where a non-nullable ElderDto is expected must fail, not crash", result.isFailure)
    }
}
