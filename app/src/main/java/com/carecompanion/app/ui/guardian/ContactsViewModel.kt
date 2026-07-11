package com.carecompanion.app.ui.guardian

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.ContactDto
import com.carecompanion.app.data.repo.CareRepository
import com.carecompanion.app.data.repo.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val loading: Boolean = true,
    val contacts: List<ContactDto> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedState: SavedStateHandle,
    private val care: CareRepository,
    private val storage: StorageRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(ContactsUiState())
    val ui: StateFlow<ContactsUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { care.contacts(elderId) }
                .onSuccess { _ui.value = ContactsUiState(false, it) }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message) }
        }
    }

    fun addContact(name: String, phone: String, relation: String?, isEmergency: Boolean, isService: Boolean, photo: Uri?) {
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val photoUrl = photo?.let { uploadPhoto(it) }
                care.addContact(
                    ContactDto(
                        elderId = elderId, name = name.trim(), phone = phone.trim(),
                        relation = relation?.ifBlank { null }, isEmergency = isEmergency,
                        isService = isService, photoUrl = photoUrl,
                    )
                )
                _ui.value = _ui.value.copy(saving = false, done = true)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(saving = false, error = e.message ?: "Failed to save")
            }
        }
    }

    fun editContact(id: String, name: String, phone: String, relation: String?, isEmergency: Boolean) {
        val existing = _ui.value.contacts.find { it.id == id } ?: return
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                care.updateContact(id, existing.copy(name = name.trim(), phone = phone.trim(), relation = relation?.ifBlank { null }, isEmergency = isEmergency))
            }.onSuccess { _ui.value = _ui.value.copy(saving = false, done = true) }
                .onFailure { _ui.value = _ui.value.copy(saving = false, error = it.message ?: "Failed to save") }
        }
    }

    fun deleteContact(id: String) = viewModelScope.launch {
        runCatching { care.deleteContact(id) }.onSuccess { load() }
    }

    private suspend fun uploadPhoto(uri: Uri): String? {
        val bytes = com.carecompanion.app.data.ImageUtil.readScaledJpeg(appContext, uri) ?: return null
        return storage.upload(StorageRepository.BUCKET_PHOTOS, elderId, bytes)
    }
}
