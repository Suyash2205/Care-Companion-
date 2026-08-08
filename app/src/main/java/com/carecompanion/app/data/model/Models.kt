package com.carecompanion.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire + domain models. Field names match Supabase columns (snake_case). */

@Serializable
data class UserDto(
    val id: String? = null,
    @SerialName("firebase_uid") val firebaseUid: String,
    val phone: String,
    val role: String,                       // guardian | elder
    val name: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
    val language: String = "en",
)

@Serializable
data class ElderDto(
    val id: String? = null,
    val name: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("avatar_key") val avatarKey: String? = null,
    val dob: String? = null,
    val age: Int? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("phone_verified") val phoneVerified: Boolean = false,
    @SerialName("verified_elder_uid") val verifiedElderUid: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("sos_message") val sosMessage: String? = null,
)

@Serializable
data class GuardianLinkDto(
    val id: String? = null,
    @SerialName("guardian_id") val guardianId: String? = null,
    @SerialName("elder_id") val elderId: String,
    val access: String,                     // owner | edit | view
    val status: String = "active",          // pending | active
    @SerialName("invited_phone") val invitedPhone: String? = null,
)

@Serializable
data class ContactDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String,
    val name: String,
    val phone: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    val relation: String? = null,
    @SerialName("is_emergency") val isEmergency: Boolean = false,
    @SerialName("is_service") val isService: Boolean = false,
    val sort: Int = 0,
)

@Serializable
data class OttShortcutDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String,
    val title: String,
    val kind: String = "preset",            // preset | custom
    @SerialName("preset_key") val presetKey: String? = null,
    @SerialName("package_or_url") val packageOrUrl: String,
    val sort: Int = 0,
)

@Serializable
data class MedicineDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String,
    val name: String,
    val dosage: String = "",
    val form: String = "Tablet",
    val instructions: String = "",
    @SerialName("with_liquid") val withLiquid: String? = null,   // water | milk
    val meal: String? = null,                                    // before | after
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("pill_url") val pillUrl: String? = null,
    @SerialName("packet_front_url") val packetFrontUrl: String? = null,
    @SerialName("packet_back_url") val packetBackUrl: String? = null,
)

@Serializable
data class MedicineScheduleDto(
    val id: String? = null,
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("elder_id") val elderId: String,
    val label: String,
    val time: String,                       // HH:MM 24h
    val days: Int = 127,                    // Mon..Sun bitmask
    val enabled: Boolean = true,
)

@Serializable
data class ReminderCategoryDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String? = null,
    val key: String? = null,
    val name: String,
    val icon: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("is_virtual") val isVirtual: Boolean = false,
)

@Serializable
data class ReminderDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String,
    @SerialName("category_id") val categoryId: String? = null,
    val title: String,
    val times: List<String> = emptyList(),
    @SerialName("repeat_kind") val repeatKind: String = "daily",  // daily | days | interval
    val days: Int = 127,
    @SerialName("interval_minutes") val intervalMinutes: Int? = null,
    val enabled: Boolean = true,
)

@Serializable
data class AdherenceLogDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String,
    val source: String,                     // schedule | reminder
    @SerialName("source_id") val sourceId: String,
    @SerialName("occurrence_date") val occurrenceDate: String,
    @SerialName("due_at") val dueAt: String,
    val status: String = "pending",         // pending | taken | skipped | missed
    @SerialName("responded_at") val respondedAt: String? = null,
)

@Serializable
data class VitalDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String,
    val type: String,                       // bp | sugar | temp | pulse
    @SerialName("value_1") val value1: Double,
    @SerialName("value_2") val value2: Double? = null,
    val context: String? = null,            // fasting | post_meal
    val note: String? = null,
    @SerialName("taken_at") val takenAt: String? = null,
    @SerialName("entered_by") val enteredBy: String? = null,
)

@Serializable
data class VitalThresholdDto(
    val type: String,
    val context: String,
    @SerialName("normal_lo") val normalLo: Double? = null,
    @SerialName("normal_hi") val normalHi: Double? = null,
    @SerialName("warn_lo") val warnLo: Double? = null,
    @SerialName("warn_hi") val warnHi: Double? = null,
    val unit: String? = null,
)

@Serializable
data class SosEventDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String,
    @SerialName("triggered_at") val triggeredAt: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Double? = null,
    val address: String? = null,
    val status: String = "active",          // active | cancelled | dismissed | resolved
)

@Serializable
data class AlertDto(
    val id: String? = null,
    @SerialName("elder_id") val elderId: String,
    @SerialName("guardian_id") val guardianId: String? = null,
    val kind: String,
    @SerialName("ref_id") val refId: String? = null,
    val title: String,
    val body: String? = null,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class WheelchairPlaceDto(
    val id: String? = null,
    val city: String = "Mumbai",
    val name: String,
    val kind: String,                       // service | place
    val phone: String? = null,
    @SerialName("maps_url") val mapsUrl: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val sort: Int = 0,
)

/**
 * A one-time code that links an elder's device to their care profile. Replaces the
 * old phone-number matching now that sign-in is via Google.
 */
@Serializable
data class InviteCodeDto(
    val code: String,
    @SerialName("elder_id") val elderId: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("redeemed_at") val redeemedAt: String? = null,
)
