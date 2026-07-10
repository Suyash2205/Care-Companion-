package com.carecompanion.app.ui.elder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.remote.SupabaseService
import com.carecompanion.app.auth.FirebaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Persists elder accessibility settings locally (offline-first) and syncs language upstream. */
@Singleton
class ElderSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("cc_elder_settings", Context.MODE_PRIVATE)

    val lang = MutableStateFlow(ElderLang.from(prefs.getString("lang", "en")))
    val fontScale = MutableStateFlow(prefs.getFloat("font_scale", 1.0f))
    val highContrast = MutableStateFlow(prefs.getBoolean("contrast", false))

    fun setLang(l: ElderLang) { lang.value = l; prefs.edit().putString("lang", l.code).apply() }
    fun setFontScale(f: Float) { fontScale.value = f; prefs.edit().putFloat("font_scale", f).apply() }
    fun setContrast(c: Boolean) { highContrast.value = c; prefs.edit().putBoolean("contrast", c).apply() }
}

@HiltViewModel
class ElderSettingsViewModel @Inject constructor(
    val store: ElderSettingsStore,
    private val api: SupabaseService,
    private val auth: FirebaseAuthManager,
) : ViewModel() {

    val lang: StateFlow<ElderLang> = store.lang.asStateFlow()
    val fontScale: StateFlow<Float> = store.fontScale.asStateFlow()
    val contrast: StateFlow<Boolean> = store.highContrast.asStateFlow()

    fun setLang(l: ElderLang) {
        store.setLang(l)
        val uid = auth.currentUid ?: return
        viewModelScope.launch { runCatching { api.patchUser("eq.$uid", mapOf("language" to l.code)) } }
    }

    fun setFontScale(f: Float) = store.setFontScale(f)
    fun setContrast(c: Boolean) = store.setContrast(c)
}
