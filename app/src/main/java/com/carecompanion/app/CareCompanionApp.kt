package com.carecompanion.app

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltAndroidApp
class CareCompanionApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        installCrashRecovery()
    }

    /**
     * Fault tolerance: instead of showing an elder the system "app keeps stopping"
     * dialog on an unexpected crash, log it and relaunch cleanly to the home screen.
     * A crash-loop guard prevents an infinite restart cycle: if the app dies within a
     * few seconds of launching (i.e. it crashed on startup), we let it stay down rather
     * than thrash. Normal in-use crashes recover invisibly.
     */
    private fun installCrashRecovery() {
        val startedAt = SystemClock.elapsedRealtime()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CareCompanion", "Uncaught exception on ${thread.name}", throwable)
            val aliveMs = SystemClock.elapsedRealtime() - startedAt
            val recentlyStarted = aliveMs < RESTART_GUARD_MS
            val loopingPrefs = getSharedPreferences("cc_crash", MODE_PRIVATE)
            val lastCrash = loopingPrefs.getLong("last", 0L)
            val nowWall = System.currentTimeMillis()
            val quickRepeat = nowWall - lastCrash < RESTART_GUARD_MS
            loopingPrefs.edit().putLong("last", nowWall).apply()

            if (!recentlyStarted && !quickRepeat) {
                runCatching { scheduleRestart() }
            }
            // Hand off to the original handler so the process terminates properly.
            if (previous != null) previous.uncaughtException(thread, throwable)
            else { Process.killProcess(Process.myPid()); exitProcess(2) }
        }
    }

    private fun scheduleRestart() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pi = PendingIntent.getActivity(
            this, RESTART_REQUEST, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val am = getSystemService(AlarmManager::class.java)
        // Inexact alarm ~400ms out; no exact-alarm permission needed.
        am?.set(AlarmManager.RTC, System.currentTimeMillis() + 400L, pi)
    }

    companion object {
        private const val RESTART_GUARD_MS = 5_000L
        private const val RESTART_REQUEST = 4711
    }
}
