package com.carecompanion.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.HttpException

/**
 * Every PostgREST/Supabase HTTP error status must surface as a normal, catchable
 * [HttpException] carrying the real status code — never an unhandled crash, and never
 * silently swallowed into a false "success".
 */
@RunWith(Parameterized::class)
class HttpErrorCodeTest(private val statusCode: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "status={0}")
        fun codes() = listOf(401, 403, 404, 409, 500, 503)
    }

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
    fun `error status is surfaced as a catchable HttpException with the right code`() = runTest {
        val api = TestSupport.buildService(server)
        server.enqueue(
            MockResponse()
                .setResponseCode(statusCode)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"message":"synthetic $statusCode error","code":"PGRST000"}""")
        )

        val result = runCatching { api.getElders() }

        assertTrue("status $statusCode must be a failure, not a crash", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("expected HttpException for status $statusCode but got $ex", ex is HttpException)
        assertEquals(statusCode, (ex as HttpException).code())
    }
}
