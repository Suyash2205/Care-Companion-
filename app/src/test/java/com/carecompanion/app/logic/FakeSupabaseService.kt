package com.carecompanion.app.logic

import com.carecompanion.app.data.model.*
import com.carecompanion.app.data.remote.*

/**
 * Bare-bones fake implementing every [SupabaseService] endpoint with a hard failure.
 * Only [getThresholds] is meant to be overridden per-test via [thresholds].
 * Every other method is unreachable from the logic under test (VitalsRepository.severity()
 * only ever calls getThresholds()), so they throw if accidentally invoked.
 */
open class FakeSupabaseService(
    private val thresholds: List<VitalThresholdDto> = emptyList(),
) : SupabaseService {

    override suspend fun getThresholds(): List<VitalThresholdDto> = thresholds

    private fun notUsed(): Nothing = throw UnsupportedOperationException("not used by logic tests")

    override suspend fun upsertUser(body: UserDto): List<UserDto> = notUsed()
    override suspend fun getUsers(firebaseUid: String?, phone: String?, select: String): List<UserDto> = notUsed()
    override suspend fun patchUser(firebaseUid: String, body: Map<String, String?>) = notUsed()
    override suspend fun getElders(id: String?, order: String, select: String): List<ElderDto> = notUsed()
    override suspend fun patchElder(id: String, body: Map<String, String?>): List<ElderDto> = notUsed()
    override suspend fun getContacts(elderId: String, order: String): List<ContactDto> = notUsed()
    override suspend fun insertContact(body: ContactDto): List<ContactDto> = notUsed()
    override suspend fun patchContact(id: String, body: ContactDto): List<ContactDto> = notUsed()
    override suspend fun deleteContact(id: String) = notUsed()
    override suspend fun getOtt(elderId: String, order: String): List<OttShortcutDto> = notUsed()
    override suspend fun insertOtt(body: OttShortcutDto): List<OttShortcutDto> = notUsed()
    override suspend fun deleteOtt(id: String) = notUsed()
    override suspend fun getMedicines(elderId: String, order: String): List<MedicineDto> = notUsed()
    override suspend fun insertMedicine(body: MedicineDto): List<MedicineDto> = notUsed()
    override suspend fun patchMedicine(id: String, body: MedicineDto): List<MedicineDto> = notUsed()
    override suspend fun deleteMedicine(id: String) = notUsed()
    override suspend fun getSchedules(elderId: String): List<MedicineScheduleDto> = notUsed()
    override suspend fun getSchedulesForMedicine(medicineId: String): List<MedicineScheduleDto> = notUsed()
    override suspend fun insertSchedule(body: MedicineScheduleDto): List<MedicineScheduleDto> = notUsed()
    override suspend fun patchSchedule(id: String, body: MedicineScheduleDto): List<MedicineScheduleDto> = notUsed()
    override suspend fun deleteSchedule(id: String) = notUsed()
    override suspend fun getReminderCategories(or: String): List<ReminderCategoryDto> = notUsed()
    override suspend fun insertReminderCategory(body: ReminderCategoryDto): List<ReminderCategoryDto> = notUsed()
    override suspend fun getReminders(elderId: String): List<ReminderDto> = notUsed()
    override suspend fun insertReminder(body: ReminderDto): List<ReminderDto> = notUsed()
    override suspend fun patchReminder(id: String, body: ReminderDto): List<ReminderDto> = notUsed()
    override suspend fun deleteReminder(id: String) = notUsed()
    override suspend fun getLinks(elderId: String?): List<GuardianLinkDto> = notUsed()
    override suspend fun getAdherence(elderId: String, date: String?, order: String): List<AdherenceLogDto> = notUsed()
    override suspend fun upsertAdherence(body: AdherenceLogDto): List<AdherenceLogDto> = notUsed()
    override suspend fun getVitals(elderId: String, type: String?, order: String): List<VitalDto> = notUsed()
    override suspend fun insertVital(body: VitalDto): List<VitalDto> = notUsed()
    override suspend fun insertSos(body: SosEventDto): List<SosEventDto> = notUsed()
    override suspend fun getSos(elderId: String, order: String): List<SosEventDto> = notUsed()
    override suspend fun patchSos(id: String, body: Map<String, String>): List<SosEventDto> = notUsed()
    override suspend fun getAlerts(order: String, limit: Int): List<AlertDto> = notUsed()
    override suspend fun markAlertRead(id: String, body: Map<String, Boolean>) = notUsed()
    override suspend fun getWheelchairPlaces(order: String): List<WheelchairPlaceDto> = notUsed()
    override suspend fun rpcCreateElder(body: CreateElderRequest): ElderDto = notUsed()
    override suspend fun rpcVerifyElderPhone(body: VerifyElderPhoneRequest): ElderDto = notUsed()
    override suspend fun rpcChangeElderPhone(body: ChangeElderPhoneRequest): ElderDto = notUsed()
    override suspend fun rpcSetElderActive(body: SetElderActiveRequest): ElderDto = notUsed()
    override suspend fun rpcInviteGuardian(body: InviteGuardianRequest): GuardianLinkDto = notUsed()
    override suspend fun rpcSetMemberAccess(body: SetMemberAccessRequest) = notUsed()
    override suspend fun rpcRemoveMember(body: RemoveMemberRequest) = notUsed()
    override suspend fun rpcResolvePendingLinks(body: Map<String, String>): Int = notUsed()
    override suspend fun rpcLinkElderByPhone(body: Map<String, String>): ElderDto? = notUsed()
}
