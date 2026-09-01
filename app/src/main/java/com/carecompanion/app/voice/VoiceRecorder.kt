package com.carecompanion.app.voice

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs

/**
 * Captures a short utterance as 16 kHz mono PCM, wrapped in a WAV header.
 *
 * 16 kHz mono is what the speech recogniser expects, so recording it directly avoids a
 * transcoding step and the quality loss that comes with it.
 *
 * Recording stops on a run of near-silence rather than after a fixed time. The silence
 * window is deliberately long: older speakers pause mid-sentence, and the usual ~0.7 s
 * endpointing cuts them off half way through an answer, which then gets blamed on the
 * recogniser rather than on the timing.
 */
object VoiceRecorder {

    const val SAMPLE_RATE = 16_000
    private const val SILENCE_WINDOW_MS = 2_500      // pause allowed before we stop
    private const val MAX_MS = 10_000                // hard cap, so the mic never hangs open
    private const val MIN_MS = 700                   // ignore an accidental tap
    private const val SILENCE_AMPLITUDE = 1_200      // 16-bit PCM; below this counts as quiet

    /**
     * Records until the speaker stops, then returns WAV bytes — or null if nothing
     * usable was captured. Never throws: a failed recording falls back to the buttons.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun record(): ByteArray? = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) return@withContext null
        val bufSize = maxOf(minBuf, SAMPLE_RATE / 5 * 2)   // ~200 ms of audio

        val recorder = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,   // applies noise suppression
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufSize,
            )
        }.getOrNull() ?: return@withContext null

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { recorder.release() }
            return@withContext null
        }

        val pcm = ByteArrayOutputStream()
        val buf = ShortArray(bufSize / 2)
        var quietMs = 0
        var totalMs = 0
        var heardAnything = false

        try {
            recorder.startRecording()
            while (totalMs < MAX_MS) {
                val n = recorder.read(buf, 0, buf.size)
                if (n <= 0) break
                val chunkMs = n * 1000 / SAMPLE_RATE
                totalMs += chunkMs

                var peak = 0
                for (i in 0 until n) {
                    val a = abs(buf[i].toInt())
                    if (a > peak) peak = a
                    pcm.write(buf[i].toInt() and 0xFF)
                    pcm.write((buf[i].toInt() shr 8) and 0xFF)
                }

                if (peak > SILENCE_AMPLITUDE) {
                    heardAnything = true
                    quietMs = 0
                } else if (heardAnything) {
                    quietMs += chunkMs
                    if (quietMs >= SILENCE_WINDOW_MS) break
                }
            }
        } catch (_: Exception) {
            return@withContext null
        } finally {
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
        }

        if (!heardAnything || totalMs < MIN_MS) return@withContext null
        wrapAsWav(pcm.toByteArray())
    }

    /** Prepend the 44-byte RIFF/WAVE header the recogniser expects. */
    private fun wrapAsWav(pcm: ByteArray): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = SAMPLE_RATE * channels * bitsPerSample / 8
        val out = ByteArrayOutputStream(44 + pcm.size)

        fun ascii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun le32(v: Int) {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
        }
        fun le16(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }

        ascii("RIFF"); le32(36 + pcm.size); ascii("WAVE")
        ascii("fmt "); le32(16); le16(1); le16(channels)
        le32(SAMPLE_RATE); le32(byteRate)
        le16(channels * bitsPerSample / 8); le16(bitsPerSample)
        ascii("data"); le32(pcm.size)
        out.write(pcm)
        return out.toByteArray()
    }
}
