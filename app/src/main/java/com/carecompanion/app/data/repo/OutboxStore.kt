package com.carecompanion.app.data.repo

import android.content.Context
import com.carecompanion.app.data.model.AdherenceLogDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline outbox for elder-originated adherence writes. If a Supabase write fails
 * (no network), the log is queued locally and flushed on the next successful load —
 * so a "Taken" tap is never lost when offline (decision #7: elder is offline-first).
 */
@Singleton
class OutboxStore @Inject constructor(
    @ApplicationContext context: Context,
    private val adherence: AdherenceRepository,
) {
    private val prefs = context.getSharedPreferences("cc_outbox", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false }
    private val KEY = "adherence"

    @Synchronized
    fun enqueue(log: AdherenceLogDto) {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        arr.put(json.encodeToString(AdherenceLogDto.serializer(), log))
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    /** Retry all queued writes; keep only the ones that still fail. */
    suspend fun flush() {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return
        if (arr.length() == 0) return
        val remaining = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.getString(i)
            val log = runCatching { json.decodeFromString(AdherenceLogDto.serializer(), item) }.getOrNull()
            if (log == null) continue
            val ok = runCatching { adherence.record(log) }.isSuccess
            if (!ok) remaining.put(item)
        }
        prefs.edit().putString(KEY, remaining.toString()).apply()
    }

    fun pendingCount(): Int = runCatching { JSONArray(prefs.getString(KEY, "[]")).length() }.getOrDefault(0)
}
