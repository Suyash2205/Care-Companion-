package com.carecompanion.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** One armed reminder occurrence. */
data class DoseAlarm(
    val key: String,        // stable per schedule occurrence
    val timeMillis: Long,   // when to fire (today)
    val title: String,
    val body: String,
)

/**
 * Arms exact alarms for today's medicine doses. Alarms are (re)armed whenever the
 * elder opens the app; the set is persisted to SharedPreferences so [rescheduleAll]
 * can re-arm the remainder after a reboot — no Room/WorkManager needed for v1.
 */
object ReminderScheduler {

    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"
    private const val PREFS = "cc_reminders"
    private const val KEY_DOSES = "today_doses"

    /**
     * Arm alarms for the future doses in [doses] (past times are skipped).
     *
     * Any key that was armed before but is absent from [doses] is cancelled first.
     * Without that, replacing the persisted set orphaned the old alarm: it stayed armed
     * with AlarmManager while no longer being tracked, so [cancelAll] could not reach it
     * and it still fired. A guardian deleting or re-timing a medicine during the day left
     * the elder being told to take it anyway — a double-dose prompt from the tool meant
     * to prevent one, and invisible until the moment it rang.
     *
     * Found by on-device fault injection, not by reading the code: the orphaned alarm
     * survived every later cancelAll() in the test run and showed up in `dumpsys alarm`.
     *
     * [disarmDropped] MUST be false when any read feeding [doses] failed. An empty list
     * from a failed fetch is not evidence that nothing is scheduled, and disarming on it
     * would let one timed-out request silently switch off a day of reminders.
     */
    fun scheduleDoses(
        context: Context,
        doses: List<DoseAlarm>,
        disarmDropped: Boolean = true,
    ) {
        val now = System.currentTimeMillis()
        val future = doses.filter { it.timeMillis > now }

        // Keep every key the caller supplied, not just the future ones. A dose whose
        // time has passed may still be waiting to ring: setExactAndAllowWhileIdle can be
        // held back under Doze, and retiring it here would turn "late" into "never"
        // without telling anyone.
        val keeping = doses.map { it.key }.toSet()

        val am = context.getSystemService(AlarmManager::class.java)
        if (disarmDropped) {
            load(context).filterNot { it.key in keeping }.forEach { stale ->
                cancelArmed(context, am, stale.key)
            }
        }
        // distinctBy keeps the FIRST occurrence, so `future` must come first: a re-timed
        // dose keeps its key, and persisting the stale entry would make a reboot re-arm
        // the old time and lose the new one.
        persist(context, if (disarmDropped) future else (future + load(context)).distinctBy { it.key })
        if (am == null) return
        future.forEach { arm(context, am, it) }
    }

    /**
     * Drop one armed alarm, token and all.
     *
     * `AlarmManager.cancel(pi)` retires the alarm but leaves the PendingIntent token
     * alive, so a FLAG_NO_CREATE lookup still finds it. Cancelling the token too keeps
     * "is this armed?" answerable, which is what makes the on-device tests meaningful.
     */
    private fun cancelArmed(context: Context, am: AlarmManager?, key: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.carecompanion.app.DOSE.$key"
        }
        val pi = PendingIntent.getBroadcast(
            context, key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching { am?.cancel(pi) }
        runCatching { pi.cancel() }
    }

    private fun arm(context: Context, am: AlarmManager, d: DoseAlarm) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.carecompanion.app.DOSE." + d.key
            putExtra(EXTRA_TITLE, d.title)
            putExtra(EXTRA_BODY, d.body)
        }
        val pi = PendingIntent.getBroadcast(
            context, d.key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, d.timeMillis, pi)
            else am.setWindow(AlarmManager.RTC_WAKEUP, d.timeMillis, 10 * 60 * 1000L, pi)
        } catch (_: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, d.timeMillis, 10 * 60 * 1000L, pi)
        }
    }

    fun handleFire(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Medicine reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "It's time to take your medicine."
        // Use the per-occurrence key (carried on the intent action) as the notification id.
        // Keying on the title alone collided whenever two doses shared a title, so one
        // reminder silently REPLACED the other instead of both being shown.
        val key = intent.action?.removePrefix("com.carecompanion.app.DOSE.").orEmpty()
        val notifId = (if (key.isNotBlank()) key else title).hashCode()
        // Medicine doses deep-link into the take-medicine flow; general reminders (water,
        // walk) have no such screen, so those still just open the app.
        val contentIntent = if (key.startsWith("rem-")) null
        else com.carecompanion.app.notify.Notifications.openMedicinesIntent(context)
        com.carecompanion.app.notify.Notifications.showReminder(context, notifId, title, body, contentIntent)
    }

    /** Re-arm the persisted, still-future doses (called on boot). */
    fun rescheduleAll(context: Context) {
        val doses = load(context)
        scheduleDoses(context, doses)
    }

    /**
     * Cancel every armed dose alarm and forget the persisted set. Used on sign-out so a
     * shared device never keeps firing the previous elder's medicine reminders.
     */
    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        load(context).forEach { d -> cancelArmed(context, am, d.key) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * Turn the guardian's general reminders (water, walk, vitals…) into today's alarms.
     *
     * These were being saved by the guardian and then delivered by nothing at all: no
     * alarm was armed on the elder's device and no server job scanned the table, so a
     * "drink water at 11:00" reminder simply never arrived.
     *
     * A reminder can carry several times, so the key includes the time as well as the id.
     * The "rem-" prefix keeps these from colliding with dose keys, which are
     * "<scheduleId>-<date>" — a collision would mean one silently replacing the other,
     * because the key's hash is used as both the PendingIntent request code and the
     * notification id.
     */
    fun remindersToAlarms(
        reminders: List<com.carecompanion.app.data.model.ReminderDto>,
        todayBit: Int,
        today: String,
    ): List<DoseAlarm> = reminders
        .filter { it.enabled && (it.days and todayBit) != 0 }
        .flatMap { r ->
            r.times.mapNotNull { t ->
                val id = r.id ?: return@mapNotNull null
                DoseAlarm(
                    key = "rem-$id-$t-$today",
                    timeMillis = timeMillisToday(t),
                    title = r.title,
                    body = "It's time.",
                )
            }
        }

    /**
     * Cancel one dose's alarm and drop it from the persisted set.
     *
     * Called when the elder answers a dose. Without this, taking a medicine early left
     * its alarm armed: an elder who took their 08:00 tablet at 07:45 was still told to
     * take it at 08:00, which for a confused or forgetful user invites a double dose.
     * Removing it from the persisted set matters too, or a reboot would re-arm it.
     */
    fun cancelDose(context: Context, key: String) {
        cancelArmed(context, context.getSystemService(AlarmManager::class.java), key)
        persist(context, load(context).filterNot { it.key == key })
    }

    // ── persistence ──────────────────────────────────────────────────────────
    private fun persist(context: Context, doses: List<DoseAlarm>) {
        val arr = JSONArray()
        doses.forEach {
            arr.put(JSONObject().put("key", it.key).put("t", it.timeMillis).put("title", it.title).put("body", it.body))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_DOSES, arr.toString()).putLong("saved_day", dayStamp()).apply()
    }

    private fun load(context: Context): List<DoseAlarm> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong("saved_day", -1) != dayStamp()) return emptyList()  // stale (previous day)
        val raw = prefs.getString(KEY_DOSES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                DoseAlarm(o.getString("key"), o.getLong("t"), o.getString("title"), o.getString("body"))
            }
        }.getOrDefault(emptyList())
    }

    private fun dayStamp(): Long {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR)
    }

    /** Build the fire-time (millis today) for an "HH:MM" schedule. */
    fun timeMillisToday(hhmm: String): Long {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
