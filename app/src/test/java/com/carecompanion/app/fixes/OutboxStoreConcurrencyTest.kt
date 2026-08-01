package com.carecompanion.app.fixes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.data.TestSupport
import com.carecompanion.app.data.model.AdherenceLogDto
import com.carecompanion.app.data.repo.AdherenceRepository
import com.carecompanion.app.data.repo.OutboxStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Targets the concurrency fix described in OutboxStore's class doc: previously `flush()`
 * snapshotted the queue, awaited the network call, then wrote back only the snapshot —
 * silently erasing anything enqueued *during* the flush (an elder's dose, lost with no
 * error). The fix re-reads the queue after the network calls and removes only the exact
 * items that were actually sent.
 *
 * These tests use a real MockWebServer whose dispatcher blocks on a latch so we can
 * enqueue a second item from the test thread while flush()'s single in-flight request is
 * still pending on a background thread — no reliance on wall-clock sleeps.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutboxStoreConcurrencyTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("cc_outbox", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun newOutbox(): OutboxStore {
        val api = TestSupport.buildService(server, timeoutSeconds = 10)
        return OutboxStore(context, AdherenceRepository(api))
    }

    private fun log(sourceId: String) = AdherenceLogDto(
        elderId = "elder-1",
        source = "schedule",
        sourceId = sourceId,
        occurrenceDate = "2026-08-01",
        dueAt = "2026-08-01T09:00:00Z",
        status = "taken",
    )

    private fun successBody(l: AdherenceLogDto) =
        "[" + TestSupport.json.encodeToString(AdherenceLogDto.serializer(), l) + "]"

    // ── CRITICAL: item enqueued mid-flush must survive ──────────────────────

    @Test
    fun `item enqueued while flush is in-flight is not erased`() = runBlocking {
        val inFlight = log("in-flight")
        val midFlush = log("mid-flush")

        val requestReceived = CountDownLatch(1)
        val releaseResponse = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestReceived.countDown()
                // Block the server thread (not the test thread) until the test says go.
                releaseResponse.await(10, TimeUnit.SECONDS)
                return MockResponse().setResponseCode(201).setBody(successBody(inFlight))
            }
        }

        val outbox = newOutbox()
        outbox.enqueue(inFlight)

        val flushJob = launch(Dispatchers.IO) { outbox.flush() }

        assertTrue("flush must have issued its network request", requestReceived.await(10, TimeUnit.SECONDS))
        // Now, WHILE the single in-flight request is still pending, enqueue a second item.
        outbox.enqueue(midFlush)
        releaseResponse.countDown()
        flushJob.join()

        assertEquals(
            "the item sent successfully is gone, but the one enqueued mid-flush must survive",
            1,
            outbox.pendingCount(),
        )
        val remainingRaw = context.getSharedPreferences("cc_outbox", Context.MODE_PRIVATE)
            .getString("adherence", "[]")!!
        assertTrue(
            "the surviving entry must be the one enqueued mid-flush, not the sent one",
            remainingRaw.contains("mid-flush") && !remainingRaw.contains("in-flight"),
        )
    }

    // ── flush retains on failure, then drains once the server recovers ─────

    @Test
    fun `flush retains on server failure then fully drains once the server recovers`() = runBlocking {
        val item = log("retry-me")
        val outbox = newOutbox()
        outbox.enqueue(item)

        server.enqueue(MockResponse().setResponseCode(503).setBody("down"))
        outbox.flush()
        assertEquals("a failed flush must retain the item", 1, outbox.pendingCount())

        server.enqueue(MockResponse().setResponseCode(201).setBody(successBody(item)))
        outbox.flush()
        assertEquals("once the server recovers, flush must fully drain the queue", 0, outbox.pendingCount())
    }

    // ── undecodable entry is dropped, not retried forever ───────────────────

    @Test
    fun `a corrupt undecodable entry is dropped rather than retried forever`() = runBlocking {
        val outbox = newOutbox()
        outbox.enqueue(log("valid-1"))

        // Inject a corrupt raw entry directly into the backing prefs (bypassing enqueue,
        // which always writes valid JSON) to simulate a previously-corrupted queue entry.
        val prefs = context.getSharedPreferences("cc_outbox", Context.MODE_PRIVATE)
        val raw = JSONArray(prefs.getString("adherence", "[]"))
        raw.put("{ not valid json at all")
        prefs.edit().putString("adherence", raw.toString()).commit()
        assertEquals(2, outbox.pendingCount())

        server.enqueue(MockResponse().setResponseCode(201).setBody(successBody(log("valid-1"))))
        outbox.flush()

        assertEquals(
            "the corrupt entry must be dropped (not left queued to be retried forever)",
            0,
            outbox.pendingCount(),
        )
    }

    // ── clear() empties the queue ────────────────────────────────────────────

    @Test
    fun `clear empties the queue`() {
        val outbox = newOutbox()
        outbox.enqueue(log("a"))
        outbox.enqueue(log("b"))
        assertEquals(2, outbox.pendingCount())

        outbox.clear()

        assertEquals(0, outbox.pendingCount())
    }

    // ── pendingCount() accuracy ──────────────────────────────────────────────

    @Test
    fun `pendingCount reflects exactly the number of queued items`() {
        val outbox = newOutbox()
        assertEquals(0, outbox.pendingCount())
        outbox.enqueue(log("1"))
        assertEquals(1, outbox.pendingCount())
        outbox.enqueue(log("2"))
        outbox.enqueue(log("3"))
        assertEquals(3, outbox.pendingCount())
    }
}
