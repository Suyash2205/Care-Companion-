package com.carecompanion.app.ui.guardian

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.auth.FirebaseAuthManager
import com.carecompanion.app.data.model.ElderDto
import com.carecompanion.app.data.repo.ElderRepository
import com.carecompanion.app.data.repo.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ElderVerify { NONE, SENDING, CODE_SENT, VERIFIED, FAILED }

data class AddEditElderUiState(
    val elderId: String? = null,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val age: String = "",
    val photoUri: Uri? = null,
    val existingPhotoUrl: String? = null,
    val verify: ElderVerify = ElderVerify.NONE,
    val verifiedUid: String? = null,
    val verificationId: String? = null,
    val phoneVerified: Boolean = false,
    val isActive: Boolean = true,
    val saving: Boolean = false,
    val saved: ElderDto? = null,
    val error: String? = null,
    /** One-time code the elder enters on their own phone to connect to this profile. */
    val inviteCode: String? = null,
    val inviteLoading: Boolean = false,
)

@HiltViewModel
class AddEditElderViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedState: SavedStateHandle,
    private val elderRepo: ElderRepository,
    private val storage: StorageRepository,
    private val authManager: FirebaseAuthManager,
) : ViewModel() {

    private val editingId: String? = savedState["elderId"]
    private val _ui = MutableStateFlow(AddEditElderUiState(elderId = editingId))
    val ui: StateFlow<AddEditElderUiState> = _ui.asStateFlow()

    init { editingId?.let { load(it) } }

    /** Fetch (or create) this elder's connect code. Idempotent server-side. */
    fun loadInviteCode() {
        val id = _ui.value.elderId ?: return
        if (_ui.value.inviteCode != null || _ui.value.inviteLoading) return
        _ui.value = _ui.value.copy(inviteLoading = true)
        viewModelScope.launch {
            val code = runCatching { elderRepo.inviteCode(id)?.code }.getOrNull()
            _ui.value = _ui.value.copy(inviteLoading = false, inviteCode = code)
        }
    }

    private fun load(id: String) = viewModelScope.launch {
        elderRepo.getElder(id)?.let { e ->
            _ui.value = _ui.value.copy(
                elderId = e.id, name = e.name, phone = e.phone.orEmpty(),
                address = e.address.orEmpty(), age = e.age?.toString().orEmpty(),
                existingPhotoUrl = e.photoUrl, phoneVerified = e.phoneVerified, isActive = e.isActive,
                verify = if (e.phoneVerified) ElderVerify.VERIFIED else ElderVerify.NONE,
            )
        }
    }

    fun setName(v: String) { _ui.value = _ui.value.copy(name = v) }
    fun setPhone(v: String) { _ui.value = _ui.value.copy(phone = v, verify = ElderVerify.NONE, verifiedUid = null) }
    fun setAddress(v: String) { _ui.value = _ui.value.copy(address = v) }
    fun setAge(v: String) { _ui.value = _ui.value.copy(age = v.filter { it.isDigit() }) }
    fun setPhoto(uri: Uri?) { _ui.value = _ui.value.copy(photoUri = uri) }
    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    private fun normalize(raw: String): String {
        val d = raw.filter { it.isDigit() || it == '+' }
        return if (d.startsWith("+")) d else "+91$d"
    }

    fun save() {
        val s = _ui.value
        if (s.name.isBlank()) { _ui.value = s.copy(error = "Enter a name"); return }
        // The elder's phone is no longer used for login (Google Sign-In + invite code),
        // but it IS used for SOS SMS and for the guardian to call them, so a malformed
        // value still silently breaks those. Reject anything that isn't a real number.
        if (s.phone.isNotBlank()) {
            val digits = s.phone.filter { it.isDigit() }
            if (digits.length < 10) {
                _ui.value = s.copy(error = "Enter a valid phone number (at least 10 digits)")
                return
            }
        }
        _ui.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val elder = if (s.elderId == null) {
                    elderRepo.createElder(
                        name = s.name.trim(),
                        phone = s.phone.trim().ifBlank { null }?.let { normalize(it) },
                        photoUrl = null, avatarKey = null,
                        age = s.age.toIntOrNull(), address = s.address.trim().ifBlank { null },
                        verifiedUid = s.verifiedUid,
                    )
                } else {
                    elderRepo.updateElderFields(s.elderId, buildMap {
                        put("name", s.name.trim())
                        put("address", s.address.trim().ifBlank { null })
                        put("age", s.age.toIntOrNull()?.toString())
                    }) ?: error("update failed")
                }
                val elderId = elder.id!!
                // upload photo if a new one was picked
                val finalElder = s.photoUri?.let { uri ->
                    val bytes = com.carecompanion.app.data.ImageUtil.readScaledJpeg(appContext, uri)
                    if (bytes != null) {
                        val url = storage.upload(StorageRepository.BUCKET_PHOTOS, elderId, bytes)
                        elderRepo.updateElderFields(elderId, mapOf("photo_url" to url)) ?: elder
                    } else elder
                } ?: elder
                _ui.value = _ui.value.copy(saving = false, saved = finalElder)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(saving = false, error = e.message ?: "Save failed")
            }
        }
    }

    fun deactivate(active: Boolean) = viewModelScope.launch {
        val id = _ui.value.elderId ?: return@launch
        runCatching { elderRepo.setActive(id, active) }
            .onSuccess { _ui.value = _ui.value.copy(isActive = active) }   // in-place toggle, don't exit
            .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "Couldn't save. Please check your connection and try again.") }
    }
}
