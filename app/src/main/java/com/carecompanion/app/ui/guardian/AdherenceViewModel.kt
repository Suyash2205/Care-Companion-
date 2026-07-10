package com.carecompanion.app.ui.guardian

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.AdherenceLogDto
import com.carecompanion.app.data.model.MedicineDto
import com.carecompanion.app.data.model.MedicineScheduleDto
import com.carecompanion.app.data.repo.AdherenceRepository
import com.carecompanion.app.data.repo.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/** Weekly taken/total for one medicine. */
data class MedAdherence(
    val medicine: MedicineDto,
    val taken: Int,
    val total: Int,
)

data class AdherenceUiState(
    val loading: Boolean = true,
    val stats: AdherenceRepository.Stats = AdherenceRepository.Stats(0, 0, 0, 0),
    val weekLogs: List<AdherenceLogDto> = emptyList(),
    val medicines: List<MedicineDto> = emptyList(),
    val perMedicine: List<MedAdherence> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class AdherenceViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val adherence: AdherenceRepository,
    private val care: CareRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(AdherenceUiState())
    val ui: StateFlow<AdherenceUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val fromDate = sevenDaysAgo()
                val logs = adherence.range(elderId, fromDate)
                val meds = care.medicines(elderId)
                val scheds = care.schedules(elderId)
                AdherenceUiState(
                    loading = false,
                    stats = adherence.stats(logs),
                    weekLogs = logs,
                    medicines = meds,
                    perMedicine = perMedicine(logs, meds, scheds),
                )
            }.onSuccess { _ui.value = it }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message) }
        }
    }

    /** Join schedule-sourced logs to their medicine via schedules→medicineId. */
    private fun perMedicine(
        logs: List<AdherenceLogDto>,
        meds: List<MedicineDto>,
        scheds: List<MedicineScheduleDto>,
    ): List<MedAdherence> {
        val schedToMed = scheds.mapNotNull { s -> s.id?.let { it to s.medicineId } }.toMap()
        val byMed = logs.asSequence()
            .filter { it.source == "schedule" }
            .mapNotNull { log -> schedToMed[log.sourceId]?.let { medId -> medId to log } }
            .groupBy({ it.first }, { it.second })
        return meds.map { m ->
            val ml = byMed[m.id].orEmpty()
            MedAdherence(m, taken = ml.count { it.status == "taken" }, total = ml.size)
        }
    }

    private fun sevenDaysAgo(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
