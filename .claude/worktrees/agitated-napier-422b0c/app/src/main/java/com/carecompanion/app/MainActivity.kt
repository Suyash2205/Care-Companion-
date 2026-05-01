package com.carecompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.carecompanion.app.ui.theme.CareCompanionTheme

sealed class AppScreen {
    object Login          : AppScreen()
    data class ElderHome(val name: String) : AppScreen()
    object GuardianHome : AppScreen()
    object GuardianAddProfile : AppScreen()
    data class GuardianManageElder(val profile: GuardianProfile) : AppScreen()
    data class GuardianManageContacts(val profile: GuardianProfile) : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CareCompanionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
                    val guardianProfiles = remember { mutableStateListOf<GuardianProfile>() }
                    val contactsByProfile = remember { mutableStateMapOf<String, List<ManagedContact>>() }

                    when (val s = screen) {
                        is AppScreen.Login -> {
                            LoginScreen { role, phone ->
                                screen = when {
                                    role.contains("Elder", ignoreCase = true) ->
                                        AppScreen.ElderHome(name = "Sunita")
                                    role.contains("Guardian", ignoreCase = true) ->
                                        AppScreen.GuardianHome
                                    else -> AppScreen.Login
                                }
                            }
                        }
                        is AppScreen.ElderHome -> {
                            ElderHomeScreen(
                                elderName = s.name,
                                onSosPressed = { /* trigger emergency flow */ },
                                onLogout = { screen = AppScreen.Login },
                                elderContacts = contactsByProfile[s.name].orEmpty()
                            )
                        }
                        is AppScreen.GuardianHome -> {
                            GuardianHomeScreen(
                                profiles = guardianProfiles,
                                onAddProfile = {
                                    screen = AppScreen.GuardianAddProfile
                                },
                                onManageProfile = { profile ->
                                    screen = AppScreen.GuardianManageElder(profile)
                                },
                                onLogout = { screen = AppScreen.Login }
                            )
                        }
                        is AppScreen.GuardianManageElder -> {
                            GuardianManageElderScreen(
                                profile = s.profile,
                                onBack = { screen = AppScreen.GuardianHome },
                                onSwitchProfiles = { screen = AppScreen.GuardianHome },
                                onLogout = { screen = AppScreen.Login },
                                onOpenContacts = {
                                    screen = AppScreen.GuardianManageContacts(s.profile)
                                }
                            )
                        }
                        is AppScreen.GuardianManageContacts -> {
                            GuardianManageContactsScreen(
                                profile = s.profile,
                                initialContacts = contactsByProfile[s.profile.name].orEmpty(),
                                onBack = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onSaveContacts = { updated ->
                                    contactsByProfile[s.profile.name] = updated
                                },
                                onLogout = { screen = AppScreen.Login }
                            )
                        }
                        is AppScreen.GuardianAddProfile -> {
                            GuardianAddProfileScreen(
                                onBack = { screen = AppScreen.GuardianHome },
                                onSaveNext = { profile ->
                                    guardianProfiles.add(profile)
                                    screen = AppScreen.GuardianHome
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
