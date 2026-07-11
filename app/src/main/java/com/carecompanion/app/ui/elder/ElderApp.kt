package com.carecompanion.app.ui.elder

import androidx.compose.runtime.Composable

/** Elder experience entry point, backed by real data (see [ElderExperience]).
 *  Permissions are requested by the one-time PermissionsOnboarding gate. */
@Composable
fun ElderApp(onLogout: () -> Unit) {
    ElderExperience(onLogout = onLogout)
}
