package com.carecompanion.app.ui.guardian

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.ContactDto
import com.carecompanion.app.data.model.WheelchairPlaceDto
import com.carecompanion.app.data.repo.CareRepository
import com.carecompanion.app.data.repo.WheelchairRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WheelchairUiState(
    val loading: Boolean = true,
    val places: List<WheelchairPlaceDto> = emptyList(),
    val serviceContacts: List<ContactDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class WheelchairViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wheelchair: WheelchairRepository,
    private val care: CareRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(WheelchairUiState())
    val ui: StateFlow<WheelchairUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val places = wheelchair.places()
                val serviceContacts = care.contacts(elderId).filter { it.isService }
                places to serviceContacts
            }
                .onSuccess { (places, serviceContacts) ->
                    _ui.value = WheelchairUiState(
                        loading = false,
                        places = places,
                        serviceContacts = serviceContacts,
                    )
                }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message) }
        }
    }
}
