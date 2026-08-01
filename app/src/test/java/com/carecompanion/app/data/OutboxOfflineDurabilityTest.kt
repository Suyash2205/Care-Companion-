package com.carecompanion.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.carecompanion.app.data.model.AdherenceLogDto
import com.carecompanion.app.data.repo.AdherenceRepository
import com.carecompanion.app.data.repo.OutboxStore
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * THE headline property: "an elder's dose is never lost offline."
 *
 * OutboxStore is the durable queue behind adherence writes (a "Taken" tap). This test
 * proves the full offline lifecycle: persist while offline -> survive an app restart ->
 * stay queued while the server keeps failing -> drain once the server recovers ->
 * multiple queued items all drain without loss or duplication.
 *
 * Robolectric is used ONLY because OutboxStore needs a real android.content.Context for
 * SharedPreferences; everything else is a plain JVM MockWebServer-backed repository.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutboxOfflineDurabilityTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        context = ApplicationProvider.getApplicationContext()
        // Start every test from a clean slate; OutboxStore always uses the "cc_outbox" prefs file.
        context.getSharedPreferences("cc_outbox", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun newOutbox(): OutboxStore {
        val api = TestSupport.buildService(server)
        val repo = AdherenceRepository(api)
        return OutboxStore(context, repo)
    }

    private fun sampleLog(sourceId: String = "sched-1") = AdherenceLogDto(
        elderId = "elder-1",
        source = "schedule",
        sourceId = sourceId,
        occurrenceDate = "2026-08-01",
        dueAt = "2026-08-01T09:00:00Z",
        status = "taken",
    )

    private fun successBodyFor(log: AdherenceLogDto): String =
        "[" + TestSupport.json.encodeToString(AdherenceLogDto.serializer(), log) + "]"

    // ── 5a: enqueue while offline persists the item ─────────────────────────

    @Test
    fun `5a - enqueue while offline persists the item`() {
        val outbox = newOutbox()
        assertEquals(0, outbox.pendingCount())

        outbox.enqueue(sampleLog())

        assertEquals("the dose must be persisted immediately, not held only in memory", 1, outbox.pendingCount())
    }

    // ── 5b: survives a fresh OutboxStore instance (simulated app restart) ──

    @Test
    fun `5b - enqueued item survives a new OutboxStore instance (restart)`() {
        newOutbox().enqueue(sampleLog())

        val afterRestart = newOutbox()

        assertEquals(
            "a restarted app must still see the queued dose — it lives in SharedPreferences, not in the object",
            1,
            afterRestart.pendingCount(),
        )
    }

    // ── 5c: flush while server still errors retains the item ───────────────

    @Test
    fun `5c - flush while server errors retains the item (never silently dropped)`() = runTest {
        val outbox = newOutbox()
        outbox.enqueue(sampleLog())
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"message":"still down"}"""))

        outbox.flush()

        assertEquals("a failed flush must NOT lose the dose", 1, outbox.pendingCount())
        // and it's durable, not just held in this instance's memory:
        assertEquals(1, newOutbox().pendingCount())
    }

    // ── 5d: flush with server success sends the item and empties the queue ─

    @Test
    fun `5d - flush with server success sends the item and empties the queue`() = runTest {
        val outbox = newOutbox()
        val log = sampleLog()
        outbox.enqueue(log)
        server.enqueue(MockResponse().setResponseCode(201).setBody(successBodyFor(log)))

        outbox.flush()

        assertEquals("a successful flush must drain the queue", 0, outbox.pendingCount())
        val recorded = server.takeRequest()
        assertTrue(recorded.path!!.startsWith("/rest/v1/adherence_logs"))
    }

    // ── 5e: multiple queued items all flush, no duplicates remain ──────────

    @Test
    fun `5e - multiple queued items all flush and no duplicates remain`() = runTest {
        val outbox = newOutbox()
        val logs = listOf(sampleLog("sched-1"), sampleLog("sched-2"), sampleLog("sched-3"))
        logs.forEach { outbox.enqueue(it) }
        assertEquals(3, outbox.pendingCount())

        logs.forEach { server.enqueue(MockResponse().setResponseCode(201).setBody(successBodyFor(it))) }

        outbox.flush()

        assertEquals("every queued dose must be sent with none lost or left behind", 0, outbox.pendingCount())
        assertEquals(3, server.requestCount)
    }

    // ── mixed: one item recovers, one keeps failing -> only the failing one remains ─

    @Test
    fun `mixed flush - only the still-failing item remains queued, the recovered one is gone`() = runTest {
        val outbox = newOutbox()
        val okLog = sampleLog("sched-ok")
        val badLog = sampleLog("sched-bad")
        outbox.enqueue(okLog)
        outbox.enqueue(badLog)

        server.enqueue(MockResponse().setResponseCode(201).setBody(successBodyFor(okLog)))
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"message":"nope"}"""))

        outbox.flush()

        assertEquals("exactly one item (the one that still failed) must remain", 1, outbox.pendingCount())
    }
}
