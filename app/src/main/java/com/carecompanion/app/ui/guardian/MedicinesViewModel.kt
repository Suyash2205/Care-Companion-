package com.carecompanion.app.ui.guardian

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.MedicineDto
import com.carecompanion.app.data.model.MedicineScheduleDto
import com.carecompanion.app.data.repo.CareRepository
import com.carecompanion.app.data.repo.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicinesUiState(
    val loading: Boolean = true,
    val medicines: List<MedicineDto> = emptyList(),
    val schedules: Map<String, List<MedicineScheduleDto>> = emptyMap(),  // medicineId -> schedules
    val saving: Boolean = false,
    val done: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MedicinesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedState: SavedStateHandle,
    private val care: CareRepository,
    private val storage: StorageRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(MedicinesUiState())
    val ui: StateFlow<MedicinesUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            try {
                val meds = care.medicines(elderId)
                val scheds = care.schedules(elderId).groupBy { it.medicineId }
                // copy(), never a fresh MedicinesUiState: building a new state here reset
                // `done` back to false. A load finishing just after a save therefore ate
                // the done=true that the screens navigate on, and the save looked ignored.
                _ui.value = _ui.value.copy(
                    loading = false, medicines = meds, schedules = scheds, error = null,
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun addMedicine(
        name: String, dosage: String, form: String, withLiquid: String?, meal: String?,
        pill: Uri?, packetFront: Uri?, packetBack: Uri?,
    ) {
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                care.addMedicine(
                    MedicineDto(
                        elderId = elderId, name = name.trim(), dosage = dosage.trim(), form = form,
                        withLiquid = withLiquid, meal = meal,
                        pillUrl = pill?.let { uploadMed(it) },
                        packetFrontUrl = packetFront?.let { uploadMed(it) },
                        packetBackUrl = packetBack?.let { uploadMed(it) },
                    )
                )
                _ui.value = _ui.value.copy(saving = false, done = true)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(saving = false, error = e.message ?: "Failed to save")
            }
        }
    }

    fun editMedicine(id: String, name: String, dosage: String, form: String, withLiquid: String?, meal: String?) {
        val existing = _ui.value.medicines.find { it.id == id } ?: return
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                care.updateMedicine(id, existing.copy(name = name.trim(), dosage = dosage.trim(), form = form, withLiquid = withLiquid, meal = meal))
            }.onSuccess { _ui.value = _ui.value.copy(saving = false, done = true) }
                .onFailure { _ui.value = _ui.value.copy(saving = false, error = it.message ?: "Failed to save") }
        }
    }

    fun toggleActive(m: MedicineDto) = viewModelScope.launch {
        val id = m.id ?: return@launch
        runCatching { care.setMedicineActive(id, !m.isActive, m) }.onSuccess { load() }
            .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "Couldn't update the medicine") }
    }

    fun deleteMedicine(id: String) = viewModelScope.launch {
        runCatching { care.deleteMedicine(id) }.onSuccess { load() }
            .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "Couldn't delete the medicine") }
    }

    // ── Schedule builder ────────────────────────────────────────────────────
    fun addSchedules(medicineId: String, labels: List<String>, time: String, days: Int, withWater: Boolean, meal: String?) {
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                labels.forEach { label ->
                    care.addSchedule(
                        MedicineScheduleDto(
                            medicineId = medicineId, elderId = elderId,
                            label = label, time = time, days = days, enabled = true,
                        )
                    )
                }
                // medicine_schedules has no water/meal columns - those live on the
                // medicine. Previously both arguments were accepted and then dropped on
                // the floor, so the "Take with water" switch and the Before/After meal
                // chips silently did nothing. Best-effort: a failure here must not lose
                // the schedules that already saved.
                _ui.value.medicines.find { it.id == medicineId }?.let { med ->
                    val updated = med.copy(withLiquid = if (withWater) "water" else null, meal = meal)
                    if (updated != med) runCatching { care.updateMedicine(medicineId, updated) }
                }
                _ui.value = _ui.value.copy(saving = false, done = true)
            } catch (e: Exception) {
                // Never leave this null: NoSuchElementException and friends carry no
                // message, which showed the guardian nothing at all on failure.
                _ui.value = _ui.value.copy(
                    saving = false, error = e.message ?: "Couldn't save the schedule. Please try again.",
                )
            }
        }
    }

    fun toggleSchedule(s: MedicineScheduleDto) = viewModelScope.launch {
        val id = s.id ?: return@launch
        runCatching { care.updateSchedule(id, s.copy(enabled = !s.enabled)) }.onSuccess { load() }
            .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "Couldn't update the schedule") }
    }

    fun deleteSchedule(id: String) = viewModelScope.launch {
        runCatching { care.deleteSchedule(id) }.onSuccess { load() }
            .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "Couldn't delete the schedule") }
    }

    private suspend fun uploadMed(uri: Uri): String? {
        val bytes = com.carecompanion.app.data.ImageUtil.readScaledJpeg(appContext, uri) ?: return null
        return storage.upload(StorageRepository.BUCKET_MEDICINE, elderId, bytes)
    }
}
