package com.carecompanion.app.data.remote

import com.carecompanion.app.data.model.*
import com.carecompanion.app.data.model.GuardianLinkDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * PostgREST + RPC surface for Supabase. Filters are passed as PostgREST operator
 * strings, e.g. elderId = "eq.<uuid>". apikey + Firebase bearer are added by the
 * OkHttp [AuthInterceptor]; per-call Prefer headers set representation/upsert.
 */
interface SupabaseService {

    // ── users ────────────────────────────────────────────────────────────────
    @Headers("Prefer: return=representation,resolution=merge-duplicates")
    @POST("rest/v1/users?on_conflict=firebase_uid")
    suspend fun upsertUser(@Body body: UserDto): List<UserDto>

    @GET("rest/v1/users")
    suspend fun getUsers(
        @Query("firebase_uid") firebaseUid: String? = null,
        @Query("phone") phone: String? = null,
        @Query("select") select: String = "*",
    ): List<UserDto>

    @PATCH("rest/v1/users")
    suspend fun patchUser(
        @Query("firebase_uid") firebaseUid: String,
        @Body body: Map<String, String?>,
    )

    // ── elders ───────────────────────────────────────────────────────────────
    @GET("rest/v1/elders")
    suspend fun getElders(
        @Query("id") id: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Query("select") select: String = "*",
    ): List<ElderDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/elders")
    suspend fun patchElder(
        @Query("id") id: String,
        @Body body: Map<String, String?>,
    ): List<ElderDto>

    // ── generic care tables (contacts, ott, medicines, schedules, reminders) ──
    @GET("rest/v1/contacts")
    suspend fun getContacts(
        @Query("elder_id") elderId: String,
        @Query("order") order: String = "sort.asc",
    ): List<ContactDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/contacts")
    suspend fun insertContact(@Body body: ContactDto): List<ContactDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/contacts")
    suspend fun patchContact(@Query("id") id: String, @Body body: ContactDto): List<ContactDto>

    @DELETE("rest/v1/contacts")
    suspend fun deleteContact(@Query("id") id: String)

    @GET("rest/v1/ott_shortcuts")
    suspend fun getOtt(@Query("elder_id") elderId: String, @Query("order") order: String = "sort.asc"): List<OttShortcutDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/ott_shortcuts")
    suspend fun insertOtt(@Body body: OttShortcutDto): List<OttShortcutDto>

    @DELETE("rest/v1/ott_shortcuts")
    suspend fun deleteOtt(@Query("id") id: String)

    @GET("rest/v1/medicines")
    suspend fun getMedicines(@Query("elder_id") elderId: String, @Query("order") order: String = "created_at.asc"): List<MedicineDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/medicines")
    suspend fun insertMedicine(@Body body: MedicineDto): List<MedicineDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/medicines")
    suspend fun patchMedicine(@Query("id") id: String, @Body body: MedicineDto): List<MedicineDto>

    @DELETE("rest/v1/medicines")
    suspend fun deleteMedicine(@Query("id") id: String)

    @GET("rest/v1/medicine_schedules")
    suspend fun getSchedules(@Query("elder_id") elderId: String): List<MedicineScheduleDto>

    @GET("rest/v1/medicine_schedules")
    suspend fun getSchedulesForMedicine(@Query("medicine_id") medicineId: String): List<MedicineScheduleDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/medicine_schedules")
    suspend fun insertSchedule(@Body body: MedicineScheduleDto): List<MedicineScheduleDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/medicine_schedules")
    suspend fun patchSchedule(@Query("id") id: String, @Body body: MedicineScheduleDto): List<MedicineScheduleDto>

    @DELETE("rest/v1/medicine_schedules")
    suspend fun deleteSchedule(@Query("id") id: String)

    @GET("rest/v1/reminder_categories")
    suspend fun getReminderCategories(@Query("or") or: String): List<ReminderCategoryDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/reminder_categories")
    suspend fun insertReminderCategory(@Body body: ReminderCategoryDto): List<ReminderCategoryDto>

    @GET("rest/v1/reminders")
    suspend fun getReminders(@Query("elder_id") elderId: String): List<ReminderDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/reminders")
    suspend fun insertReminder(@Body body: ReminderDto): List<ReminderDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/reminders")
    suspend fun patchReminder(@Query("id") id: String, @Body body: ReminderDto): List<ReminderDto>

    @DELETE("rest/v1/reminders")
    suspend fun deleteReminder(@Query("id") id: String)

    // ── guardian links ───────────────────────────────────────────────────────
    @GET("rest/v1/guardian_elder_links")
    suspend fun getLinks(@Query("elder_id") elderId: String? = null): List<GuardianLinkDto>

    // ── adherence ────────────────────────────────────────────────────────────
    @GET("rest/v1/adherence_logs")
    suspend fun getAdherence(
        @Query("elder_id") elderId: String,
        @Query("occurrence_date") date: String? = null,
        @Query("order") order: String = "due_at.desc",
    ): List<AdherenceLogDto>

    @Headers("Prefer: return=representation,resolution=merge-duplicates")
    @POST("rest/v1/adherence_logs?on_conflict=source,source_id,due_at")
    suspend fun upsertAdherence(@Body body: AdherenceLogDto): List<AdherenceLogDto>

    // ── vitals ───────────────────────────────────────────────────────────────
    @GET("rest/v1/vitals")
    suspend fun getVitals(
        @Query("elder_id") elderId: String,
        @Query("type") type: String? = null,
        @Query("order") order: String = "taken_at.desc",
    ): List<VitalDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/vitals")
    suspend fun insertVital(@Body body: VitalDto): List<VitalDto>

    @GET("rest/v1/vital_thresholds")
    suspend fun getThresholds(): List<VitalThresholdDto>

    // ── SOS ──────────────────────────────────────────────────────────────────
    @Headers("Prefer: return=representation")
    @POST("rest/v1/sos_events")
    suspend fun insertSos(@Body body: SosEventDto): List<SosEventDto>

    @GET("rest/v1/sos_events")
    suspend fun getSos(@Query("elder_id") elderId: String, @Query("order") order: String = "triggered_at.desc"): List<SosEventDto>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/sos_events")
    suspend fun patchSos(@Query("id") id: String, @Body body: Map<String, String>): List<SosEventDto>

    // ── alerts ───────────────────────────────────────────────────────────────
    @GET("rest/v1/alerts")
    suspend fun getAlerts(@Query("order") order: String = "created_at.desc", @Query("limit") limit: Int = 50): List<AlertDto>

    @PATCH("rest/v1/alerts")
    suspend fun markAlertRead(@Query("id") id: String, @Body body: Map<String, Boolean>)

    // ── wheelchair ───────────────────────────────────────────────────────────
    @GET("rest/v1/wheelchair_places")
    suspend fun getWheelchairPlaces(@Query("order") order: String = "sort.asc"): List<WheelchairPlaceDto>

    // ── RPCs ─────────────────────────────────────────────────────────────────
    @POST("rest/v1/rpc/rpc_create_elder")
    suspend fun rpcCreateElder(@Body body: CreateElderRequest): ElderDto

    @POST("rest/v1/rpc/rpc_verify_elder_phone")
    suspend fun rpcVerifyElderPhone(@Body body: VerifyElderPhoneRequest): ElderDto

    @POST("rest/v1/rpc/rpc_change_elder_phone")
    suspend fun rpcChangeElderPhone(@Body body: ChangeElderPhoneRequest): ElderDto

    @POST("rest/v1/rpc/rpc_set_elder_active")
    suspend fun rpcSetElderActive(@Body body: SetElderActiveRequest): ElderDto

    @POST("rest/v1/rpc/rpc_invite_guardian")
    suspend fun rpcInviteGuardian(@Body body: InviteGuardianRequest): GuardianLinkDto

    @POST("rest/v1/rpc/rpc_set_member_access")
    suspend fun rpcSetMemberAccess(@Body body: SetMemberAccessRequest)

    @POST("rest/v1/rpc/rpc_remove_member")
    suspend fun rpcRemoveMember(@Body body: RemoveMemberRequest)

    @POST("rest/v1/rpc/rpc_resolve_pending_links")
    suspend fun rpcResolvePendingLinks(@Body body: Map<String, String> = emptyMap()): Int

    @POST("rest/v1/rpc/rpc_link_elder_by_phone")
    suspend fun rpcLinkElderByPhone(@Body body: Map<String, String> = emptyMap()): ElderDto?

    // ── Invite codes (elder device ↔ profile linking, replaces phone matching) ──
    @POST("rest/v1/rpc/rpc_create_invite_code")
    suspend fun rpcCreateInviteCode(@Body body: Map<String, String>): InviteCodeDto?

    @POST("rest/v1/rpc/rpc_redeem_invite_code")
    suspend fun rpcRedeemInviteCode(@Body body: Map<String, String>): ElderDto?
}
