package com.carecompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.data.repo.SessionState
import com.carecompanion.app.notify.Notifications
import com.carecompanion.app.ui.RootViewModel
import com.carecompanion.app.ui.elder.ElderApp
import com.carecompanion.app.ui.guardian.GuardianApp
import com.carecompanion.app.ui.theme.CareCompanionTheme
import com.carecompanion.app.ui.theme.CareGreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.ensureChannels(this)
        setContent {
            CareCompanionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val root: RootViewModel = hiltViewModel()
                    val session by root.session.collectAsStateWithLifecycle()
                    when (val s = session) {
                        is SessionState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                        is SessionState.LoggedOut, is SessionState.NeedsRole -> LoginScreen()
                        is SessionState.Ready ->
                            if (s.user.role.equals("elder", ignoreCase = true))
                                ElderApp(onLogout = { root.signOut() })
                            else
                                GuardianApp(onLogout = { root.signOut() })
                    }
                }
            }
        }
    }
}
