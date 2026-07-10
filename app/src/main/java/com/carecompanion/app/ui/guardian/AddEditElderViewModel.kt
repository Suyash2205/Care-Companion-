package com.carecompanion.app.ui.guardian

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.auth.FirebaseAuthManager
import com.carecompanion.app.auth.OtpRequest
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

    /** Send OTP to the elder's phone from the guardian device (secondary auth). */
    fun sendElderOtp(activity: Activity) {
        val phone = normalize(_ui.value.phone)
        if (phone.length < 10) { _ui.value = _ui.value.copy(error = "Enter elder's phone"); return }
        _ui.value = _ui.value.copy(verify = ElderVerify.SENDING, error = null)
        viewModelScope.launch {
            try {
                when (val r = authManager.requestElderOtp(activity, phone)) {
                    is OtpRequest.AutoVerified -> {
                        val uid = authManager.verifyElderCredential(r.credential)
                        _ui.value = _ui.value.copy(verify = ElderVerify.VERIFIED, verifiedUid = uid)
                    }
                    is OtpRequest.CodeSent ->
                        _ui.value = _ui.value.copy(verify = ElderVerify.CODE_SENT, verificationId = r.verificationId)
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(verify = ElderVerify.FAILED, error = e.message ?: "Verification failed")
            }
        }
    }

    fun confirmElderOtp(code: String) {
        val vid = _ui.value.verificationId ?: return
        viewModelScope.launch {
            try {
                val cred = com.google.firebase.auth.PhoneAuthProvider.getCredential(vid, code)
                val uid = authManager.verifyElderCredential(cred)
                _ui.value = _ui.value.copy(verify = ElderVerify.VERIFIED, verifiedUid = uid)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(verify = ElderVerify.FAILED, error = "Wrong code")
            }
        }
    }

    fun save() {
        val s = _ui.value
        if (s.name.isBlank()) { _ui.value = s.copy(error = "Enter a name"); return }
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
                    val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
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
            .onSuccess { _ui.value = _ui.value.copy(isActive = active, saved = it) }
            .onFailure { _ui.value = _ui.value.copy(error = it.message) }
    }
}
