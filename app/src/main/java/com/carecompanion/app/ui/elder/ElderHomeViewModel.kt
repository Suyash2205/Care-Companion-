package com.carecompanion.app.ui.elder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.*
import com.carecompanion.app.data.repo.AdherenceRepository
import com.carecompanion.app.data.repo.CareRepository
import com.carecompanion.app.data.repo.ElderRepository
import com.carecompanion.app.reminder.DoseAlarm
import com.carecompanion.app.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** One medicine dose due today, with its current adherence status. */
data class Dose(
    val medicine: MedicineDto,
    val schedule: MedicineScheduleDto,
    val dueAt: String,
    val occurrenceDate: String,
    val status: String,     // pending | taken | skipped | missed
)

data class ElderUiState(
    val loading: Boolean = true,
    val elder: ElderDto? = null,
    val doses: List<Dose> = emptyList(),
    val contacts: List<ContactDto> = emptyList(),
    val ott: List<OttShortcutDto> = emptyList(),
    val error: String? = null,
) {
    val dueCount: Int get() = doses.count { it.status == "pending" || it.status == "missed" }
}

@HiltViewModel
class ElderHomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val elderRepo: ElderRepository,
    private val care: CareRepository,
    private val adherence: AdherenceRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ElderUiState())
    val ui: StateFlow<ElderUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            try {
                val elder = elderRepo.listElders().firstOrNull { it.isActive }
                if (elder?.id == null) { _ui.value = ElderUiState(false, error = "No profile linked yet"); return@launch }
                val eid = elder.id
                val meds = care.medicines(eid).filter { it.isActive }
                val scheds = care.schedules(eid).filter { it.enabled }
                val contacts = care.contacts(eid)
                val ott = care.ott(eid)
                val today = todayDate()
                val logs = runCatching { adherence.forDate(eid, today) }.getOrDefault(emptyList())
                val doses = buildDoses(meds, scheds, today, logs)
                _ui.value = ElderUiState(false, elder, doses, contacts, ott)
                armAlarms(doses)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
        }
    }

    private fun buildDoses(
        meds: List<MedicineDto>, scheds: List<MedicineScheduleDto>, today: String, logs: List<AdherenceLogDto>,
    ): List<Dose> {
        val todayBit = 1 shl todayIndexMonday0()
        val medById = meds.associateBy { it.id }
        return scheds.filter { (it.days and todayBit) != 0 && medById.containsKey(it.medicineId) }
            .sortedBy { it.time }
            .map { sched ->
                val due = "${today}T${sched.time}:00+00"
                val log = logs.firstOrNull { it.source == "schedule" && it.sourceId == sched.id && it.dueAt.startsWith("${today}T${sched.time}") }
                Dose(medById[sched.medicineId]!!, sched, due, today, log?.status ?: "pending")
            }
    }

    /** Arm on-device notifications for today's still-pending doses. */
    private fun armAlarms(doses: List<Dose>) {
        val alarms = doses.filter { it.status == "pending" }.map { d ->
            val chips = listOfNotNull(
                d.medicine.dosage.ifBlank { null }, d.schedule.label,
                d.medicine.withLiquid?.let { "with $it" }
            ).joinToString(" · ")
            DoseAlarm(
                key = "${d.schedule.id}-${d.dueAt}",
                timeMillis = ReminderScheduler.timeMillisToday(d.schedule.time),
                title = "Time for ${d.medicine.name}",
                body = chips.ifBlank { "It's time to take your medicine." },
            )
        }
        runCatching { ReminderScheduler.scheduleDoses(appContext, alarms) }
    }

    fun recordDose(dose: Dose, taken: Boolean) {
        viewModelScope.launch {
            val elderId = _ui.value.elder?.id ?: return@launch
            runCatching {
                adherence.record(
                    AdherenceLogDto(
                        elderId = elderId, source = "schedule", sourceId = dose.schedule.id!!,
                        occurrenceDate = dose.occurrenceDate, dueAt = dose.dueAt,
                        status = if (taken) "taken" else "skipped",
                        respondedAt = nowIso(),
                    )
                )
            }.onSuccess {
                _ui.value = _ui.value.copy(doses = _ui.value.doses.map {
                    if (it.schedule.id == dose.schedule.id && it.dueAt == dose.dueAt)
                        it.copy(status = if (taken) "taken" else "skipped") else it
                })
            }
        }
    }

    private fun todayDate(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    private fun nowIso(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02dT%02d:%02d:%02d+00".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND)
        )
    }

    /** Monday=0 … Sunday=6 for bitmask. */
    private fun todayIndexMonday0(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
            else -> 6
        }
    }
}
