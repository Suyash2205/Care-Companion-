package com.carecompanion.app.data.repo

import android.content.Context
import com.carecompanion.app.data.model.AdherenceLogDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline outbox for elder-originated adherence writes. If a Supabase write fails
 * (no network), the log is queued locally and flushed on the next successful load —
 * so a "Taken" tap is never lost when offline (decision #7: elder is offline-first).
 *
 * Concurrency contract: every mutation of the stored array is performed under
 * [lock] as a read-modify-write, and writes use `commit()` rather than `apply()`.
 * Previously `flush()` snapshotted the queue, awaited network calls, then wrote back
 * only the items it had started with — so anything enqueued *during* the flush was
 * silently erased (an elder's dose, lost forever with no error). Now the flush only
 * ever removes the exact items it successfully sent.
 */
@Singleton
class OutboxStore @Inject constructor(
    @ApplicationContext context: Context,
    private val adherence: AdherenceRepository,
) {
    private val prefs = context.getSharedPreferences("cc_outbox", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false }
    private val KEY = "adherence"

    /** Guards every read-modify-write of the queue (suspend-friendly, unlike @Synchronized). */
    private val lock = Mutex()

    private fun readAll(): MutableList<String> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return mutableListOf()
        return MutableList(arr.length()) { arr.getString(it) }
    }

    private fun writeAll(items: List<String>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        // commit() (not apply()) so the queue is durable even if the process dies next.
        prefs.edit().putString(KEY, arr.toString()).commit()
    }

    @Synchronized
    fun enqueue(log: AdherenceLogDto) {
        val items = readAll()
        items.add(json.encodeToString(AdherenceLogDto.serializer(), log))
        writeAll(items)
    }

    /**
     * Retry all queued writes; keep the ones that still fail.
     *
     * Items sent successfully are removed by identity from the *current* queue at the
     * end, so a dose enqueued while this flush was in flight is preserved rather than
     * overwritten.
     */
    suspend fun flush() = lock.withLock {
        val snapshot = readAll()
        if (snapshot.isEmpty()) return@withLock

        val sent = mutableListOf<String>()
        for (item in snapshot) {
            val log = runCatching { json.decodeFromString(AdherenceLogDto.serializer(), item) }.getOrNull()
            if (log == null) {
                // Undecodable entry would otherwise be retried forever — drop it.
                sent += item
                continue
            }
            // The server upserts on (source, source_id, due_at), so re-sending an item
            // that actually succeeded is safe and cannot create a duplicate row.
            if (runCatching { adherence.record(log) }.isSuccess) sent += item
        }

        // Re-read: anything enqueued during the network calls above must survive.
        val current = readAll()
        sent.forEach { current.remove(it) }   // removes one matching entry per sent item
        writeAll(current)
    }

    fun pendingCount(): Int = readAll().size

    /** Drop every queued write — used on sign-out so one user's doses never flush under another's session. */
    @Synchronized
    fun clear() = writeAll(emptyList())
}
