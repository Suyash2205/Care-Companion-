package com.carecompanion.app.data.repo

import com.carecompanion.app.data.model.ElderDto
import com.carecompanion.app.data.model.GuardianLinkDto
import com.carecompanion.app.data.model.UserDto
import com.carecompanion.app.data.remote.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElderRepository @Inject constructor(
    private val api: SupabaseService,
) {
    /** Elders this guardian can see (RLS-scoped). Active first. */
    suspend fun listElders(): List<ElderDto> =
        api.getElders().sortedByDescending { it.isActive }

    suspend fun getElder(id: String): ElderDto? =
        api.getElders(id = "eq.$id").firstOrNull()

    suspend fun createElder(
        name: String, phone: String?, photoUrl: String?, avatarKey: String?,
        age: Int?, address: String?, verifiedUid: String?,
    ): ElderDto = api.rpcCreateElder(
        CreateElderRequest(
            name = name, phone = phone, photoUrl = photoUrl, avatarKey = avatarKey,
            age = age, address = address, verifiedUid = verifiedUid
        )
    )

    suspend fun updateElderFields(id: String, fields: Map<String, String?>): ElderDto? =
        api.patchElder("eq.$id", fields).firstOrNull()

    suspend fun verifyPhone(elderId: String, verifiedUid: String): ElderDto =
        api.rpcVerifyElderPhone(VerifyElderPhoneRequest(elderId, verifiedUid))

    suspend fun changePhone(elderId: String, newPhone: String, verifiedUid: String?): ElderDto =
        api.rpcChangeElderPhone(ChangeElderPhoneRequest(elderId, newPhone, verifiedUid))

    suspend fun setActive(elderId: String, active: Boolean): ElderDto =
        api.rpcSetElderActive(SetElderActiveRequest(elderId, active))

    // ── Family members / links ────────────────────────────────────────────────
    suspend fun links(elderId: String): List<GuardianLinkDto> =
        api.getLinks("eq.$elderId")

    /** Guardians linked to an elder, joined with their user rows for display. */
    suspend fun members(elderId: String): List<Pair<GuardianLinkDto, UserDto?>> {
        val links = api.getLinks("eq.$elderId")
        val guardianIds = links.mapNotNull { it.guardianId }
        val users = if (guardianIds.isEmpty()) emptyList()
        else api.getUsers().filter { it.id in guardianIds }
        return links.map { link -> link to users.firstOrNull { it.id == link.guardianId } }
    }

    suspend fun inviteGuardian(elderId: String, phone: String, access: String): GuardianLinkDto =
        api.rpcInviteGuardian(InviteGuardianRequest(elderId, phone, access))

    suspend fun setMemberAccess(elderId: String, guardianId: String, access: String) =
        api.rpcSetMemberAccess(SetMemberAccessRequest(elderId, guardianId, access))

    suspend fun removeMember(elderId: String, guardianId: String) =
        api.rpcRemoveMember(RemoveMemberRequest(elderId, guardianId))
}
