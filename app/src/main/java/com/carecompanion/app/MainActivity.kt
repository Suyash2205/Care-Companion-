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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.data.repo.SessionState
import com.carecompanion.app.notify.Notifications
import com.carecompanion.app.ui.PermissionsOnboarding
import com.carecompanion.app.ui.RootViewModel
import com.carecompanion.app.ui.SplashScreen
import com.carecompanion.app.ui.elder.ElderApp
import com.carecompanion.app.ui.guardian.GuardianApp
import com.carecompanion.app.ui.theme.CareCompanionTheme
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
                    val context = LocalContext.current
                    val prefs = remember { context.getSharedPreferences("cc_onboarding", MODE_PRIVATE) }
                    var onboarded by remember { mutableStateOf(prefs.getBoolean("done", false)) }

                    when (val s = session) {
                        is SessionState.Loading -> SplashScreen()
                        is SessionState.LoggedOut, is SessionState.NeedsRole -> LoginScreen()
                        is SessionState.Ready -> {
                            val isElder = s.user.role.equals("elder", ignoreCase = true)
                            if (!onboarded) {
                                PermissionsOnboarding(isElder = isElder) {
                                    prefs.edit().putBoolean("done", true).apply(); onboarded = true
                                }
                            } else if (isElder) {
                                ElderApp(onLogout = { root.signOut() })
                            } else {
                                GuardianApp(onLogout = { root.signOut() })
                            }
                        }
                    }
                }
            }
        }
    }
}
