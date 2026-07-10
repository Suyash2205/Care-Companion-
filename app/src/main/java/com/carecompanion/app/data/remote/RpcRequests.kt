package com.carecompanion.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateElderRequest(
    @SerialName("p_name") val name: String,
    @SerialName("p_phone") val phone: String? = null,
    @SerialName("p_photo_url") val photoUrl: String? = null,
    @SerialName("p_avatar_key") val avatarKey: String? = null,
    @SerialName("p_dob") val dob: String? = null,
    @SerialName("p_age") val age: Int? = null,
    @SerialName("p_address") val address: String? = null,
    @SerialName("p_verified_uid") val verifiedUid: String? = null,
)

@Serializable
data class VerifyElderPhoneRequest(
    @SerialName("p_elder") val elder: String,
    @SerialName("p_verified_uid") val verifiedUid: String,
)

@Serializable
data class ChangeElderPhoneRequest(
    @SerialName("p_elder") val elder: String,
    @SerialName("p_new_phone") val newPhone: String,
    @SerialName("p_verified_uid") val verifiedUid: String? = null,
)

@Serializable
data class SetElderActiveRequest(
    @SerialName("p_elder") val elder: String,
    @SerialName("p_active") val active: Boolean,
)

@Serializable
data class InviteGuardianRequest(
    @SerialName("p_elder") val elder: String,
    @SerialName("p_phone") val phone: String,
    @SerialName("p_access") val access: String,
)

@Serializable
data class SetMemberAccessRequest(
    @SerialName("p_elder") val elder: String,
    @SerialName("p_guardian") val guardian: String,
    @SerialName("p_access") val access: String,
)

@Serializable
data class RemoveMemberRequest(
    @SerialName("p_elder") val elder: String,
    @SerialName("p_guardian") val guardian: String,
)
