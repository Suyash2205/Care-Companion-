package com.carecompanion.app.ui.elder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.ElderHomeScreen
import com.carecompanion.app.ManagedContact

/** Elder experience entry point, backed by real data. The rich home per the
 *  approved mockup is layered on top of the existing elder screen. */
@Composable
fun ElderApp(onLogout: () -> Unit, vm: ElderHomeViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    val contacts = ui.contacts.map { ManagedContact(it.name, it.phone, null) }
    ElderHomeScreen(
        elderName = ui.elder?.name ?: "…",
        onSosPressed = {},
        onLogout = onLogout,
        elderContacts = contacts,
    )
}
