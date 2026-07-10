package com.carecompanion.app.data.repo

import com.carecompanion.app.data.model.*
import com.carecompanion.app.data.remote.SupabaseService
import javax.inject.Inject
import javax.inject.Singleton

/** Guardian-managed care data for one elder: contacts, OTT, medicines, schedules, reminders. */
@Singleton
class CareRepository @Inject constructor(
    private val api: SupabaseService,
) {
    // ── Contacts ───────────────────────────────────────────────────────────────
    suspend fun contacts(elderId: String) = api.getContacts("eq.$elderId")
    suspend fun addContact(c: ContactDto) = api.insertContact(c).first()
    suspend fun updateContact(id: String, c: ContactDto) = api.patchContact("eq.$id", c).first()
    suspend fun deleteContact(id: String) = api.deleteContact("eq.$id")

    // ── OTT shortcuts ──────────────────────────────────────────────────────────
    suspend fun ott(elderId: String) = api.getOtt("eq.$elderId")
    suspend fun addOtt(o: OttShortcutDto) = api.insertOtt(o).first()
    suspend fun deleteOtt(id: String) = api.deleteOtt("eq.$id")

    // ── Medicines ──────────────────────────────────────────────────────────────
    suspend fun medicines(elderId: String) = api.getMedicines("eq.$elderId")
    suspend fun addMedicine(m: MedicineDto) = api.insertMedicine(m).first()
    suspend fun updateMedicine(id: String, m: MedicineDto) = api.patchMedicine("eq.$id", m).first()
    suspend fun setMedicineActive(id: String, active: Boolean, base: MedicineDto) =
        api.patchMedicine("eq.$id", base.copy(isActive = active)).first()
    suspend fun deleteMedicine(id: String) = api.deleteMedicine("eq.$id")

    // ── Schedules ──────────────────────────────────────────────────────────────
    suspend fun schedules(elderId: String) = api.getSchedules("eq.$elderId")
    suspend fun schedulesForMedicine(medicineId: String) = api.getSchedulesForMedicine("eq.$medicineId")
    suspend fun addSchedule(s: MedicineScheduleDto) = api.insertSchedule(s).first()
    suspend fun updateSchedule(id: String, s: MedicineScheduleDto) = api.patchSchedule("eq.$id", s).first()
    suspend fun deleteSchedule(id: String) = api.deleteSchedule("eq.$id")

    // ── Reminder categories + reminders ─────────────────────────────────────────
    suspend fun reminderCategories(elderId: String): List<ReminderCategoryDto> =
        api.getReminderCategories(or = "(elder_id.is.null,elder_id.eq.$elderId)")
    suspend fun addReminderCategory(c: ReminderCategoryDto) = api.insertReminderCategory(c).first()
    suspend fun reminders(elderId: String) = api.getReminders("eq.$elderId")
    suspend fun addReminder(r: ReminderDto) = api.insertReminder(r).first()
    suspend fun updateReminder(id: String, r: ReminderDto) = api.patchReminder("eq.$id", r).first()
    suspend fun deleteReminder(id: String) = api.deleteReminder("eq.$id")
}
