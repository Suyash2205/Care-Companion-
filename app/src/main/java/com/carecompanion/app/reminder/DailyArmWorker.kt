package com.carecompanion.app.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carecompanion.app.data.repo.AdherenceRepository
import com.carecompanion.app.data.repo.CareRepository
import com.carecompanion.app.data.repo.ElderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Re-arms today's medicine alarms **without requiring the elder to open the app**.
 *
 * Why this exists: [ReminderScheduler] only persists *today's* doses, and alarms were
 * previously armed solely from the elder home screen. An elder who did not open the app
 * on a given day (the common case — they rely on the reminder to prompt them) would get
 * no reminders at all that day, and a reboot after midnight would re-arm nothing because
 * the persisted set is day-stamped. This worker closes that hole: it runs periodically,
 * fetches today's schedule, and arms the alarms itself.
 *
 * It is best-effort and offline-safe: on any failure it retries rather than losing the day.
 */
@HiltWorker
class DailyArmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val elderRepo: ElderRepository,
    private val care: CareRepository,
    private val adherence: AdherenceRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val elder = elderRepo.listElders().firstOrNull { it.isActive } ?: return@runCatching Result.success()
        val eid = elder.id ?: return@runCatching Result.success()

        val meds = care.medicines(eid).filter { it.isActive }.associateBy { it.id }
        val scheds = care.schedules(eid).filter { it.enabled }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val logs = runCatching { adherence.forDate(eid, today) }.getOrDefault(emptyList())

        // Mon=bit0 .. Sun=bit6, matching ElderHomeViewModel.todayIndexMonday0()
        val dowIdx = ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7)
        val todayBit = 1 shl dowIdx

        val alarms = scheds
            .filter { (it.days and todayBit) != 0 && meds.containsKey(it.medicineId) }
            .filter { s ->
                // skip doses the elder has already responded to today
                logs.none { it.source == "schedule" && it.sourceId == s.id && it.occurrenceDate == today }
            }
            .mapNotNull { s ->
                val med = meds[s.medicineId] ?: return@mapNotNull null
                val chips = listOfNotNull(
                    med.dosage.ifBlank { null }, s.label, med.withLiquid?.let { "with $it" },
                ).joinToString(" · ")
                DoseAlarm(
                    key = "${s.id}-$today",
                    timeMillis = ReminderScheduler.timeMillisToday(s.time),
                    title = "Time for ${med.name}",
                    body = chips.ifBlank { "It's time to take your medicine." },
                )
            }

        ReminderScheduler.scheduleDoses(applicationContext, alarms)
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val NAME = "cc_daily_arm"

        /** Schedule the recurring re-arm. Safe to call repeatedly (KEEP policy). */
        fun ensureScheduled(context: Context) {
            val req = PeriodicWorkRequestBuilder<DailyArmWorker>(4, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            runCatching {
                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
            }
        }
    }
}
