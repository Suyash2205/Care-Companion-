package com.carecompanion.app.ui.elder

import com.carecompanion.app.data.model.*
import com.carecompanion.app.data.repo.AdherenceRepository
import com.carecompanion.app.data.repo.CareRepository
import com.carecompanion.app.data.repo.ElderRepository
import com.carecompanion.app.data.repo.OutboxStore
import com.carecompanion.app.data.repo.SosRepository
import com.carecompanion.app.logic.FakeSupabaseService
import com.carecompanion.app.ui.sos.ElderSosViewModel

/**
 * A [FakeSupabaseService] wired to return a fixed elder + care graph, so
 * [ElderHomeViewModel.load] runs its real code path end-to-end against in-memory
 * data instead of a network. Every field defaults to "nothing" so a test only
 * has to override what it actually needs.
 */
class ScriptedSupabaseService(
    private val elder: ElderDto? = null,
    private val contacts: List<ContactDto> = emptyList(),
    private val ott: List<OttShortcutDto> = emptyList(),
    private val medicines: List<MedicineDto> = emptyList(),
    private val schedules: List<MedicineScheduleDto> = emptyList(),
    private val adherence: List<AdherenceLogDto> = emptyList(),
) : FakeSupabaseService() {
    override suspend fun getElders(id: String?, order: String, select: String): List<ElderDto> =
        listOfNotNull(elder)

    override suspend fun rpcLinkElderByPhone(body: Map<String, String>): ElderDto? = null

    override suspend fun getContacts(elderId: String, order: String): List<ContactDto> = contacts
    override suspend fun getOtt(elderId: String, order: String): List<OttShortcutDto> = ott
    override suspend fun getMedicines(elderId: String, order: String): List<MedicineDto> = medicines
    override suspend fun getSchedules(elderId: String): List<MedicineScheduleDto> = schedules
    override suspend fun getAdherence(elderId: String, date: String?, order: String): List<AdherenceLogDto> = adherence
    override suspend fun upsertAdherence(body: AdherenceLogDto): List<AdherenceLogDto> = listOf(body)
}

/** A default elder + one contact + one due medicine, for tests that need "something real". */
object ElderFixtures {
    val elder = ElderDto(id = "e1", name = "Asha Devi", isActive = true)

    fun contact(name: String, photo: String? = null, emergency: Boolean = false) =
        ContactDto(elderId = "e1", name = name, phone = "9990000000", photoUrl = photo, isEmergency = emergency)

    val medicine = MedicineDto(id = "m1", elderId = "e1", name = "Metformin", dosage = "500mg")
    val schedule = MedicineScheduleDto(id = "s1", medicineId = "m1", elderId = "e1", label = "Morning", time = "08:00", days = 127)

    /** Builds a real [ElderHomeViewModel] backed by [ScriptedSupabaseService], using a
     *  Robolectric application context for the pieces that need SharedPreferences. */
    fun homeViewModel(
        context: android.content.Context,
        contacts: List<ContactDto> = emptyList(),
        ott: List<OttShortcutDto> = emptyList(),
        medicines: List<MedicineDto> = emptyList(),
        schedules: List<MedicineScheduleDto> = emptyList(),
        elder: ElderDto? = ElderFixtures.elder,
    ): ElderHomeViewModel {
        val api = ScriptedSupabaseService(
            elder = elder, contacts = contacts, ott = ott, medicines = medicines, schedules = schedules,
        )
        val elderRepo = ElderRepository(api)
        val care = CareRepository(api)
        val adherenceRepo = AdherenceRepository(api)
        val outbox = OutboxStore(context, adherenceRepo)
        return ElderHomeViewModel(context, elderRepo, care, adherenceRepo, outbox)
    }

    fun sosViewModel(context: android.content.Context, elder: ElderDto? = ElderFixtures.elder): ElderSosViewModel {
        val api = ScriptedSupabaseService(elder = elder)
        return ElderSosViewModel(context, SosRepository(api))
    }

    fun settingsViewModel(context: android.content.Context, store: ElderSettingsStore = ElderSettingsStore(context)): ElderSettingsViewModel {
        val api = ScriptedSupabaseService()
        val auth = com.carecompanion.app.auth.FirebaseAuthManager(context)
        return ElderSettingsViewModel(store, api, auth)
    }

    /** Voice controller backed by the same scripted service the other fixtures use. */
    fun voiceViewModel(context: android.content.Context): VoiceDoseViewModel =
        VoiceDoseViewModel(
            context,
            com.carecompanion.app.data.repo.VoiceRepository(ScriptedSupabaseService()),
        )
}
