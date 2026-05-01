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
    object Login                                                         : AppScreen()
    data class ElderHome(val name: String)                               : AppScreen()
    object GuardianHome                                                  : AppScreen()
    object GuardianAddProfile                                            : AppScreen()
    data class GuardianManageElder(val profile: GuardianProfile)         : AppScreen()
    data class GuardianManageContacts(val profile: GuardianProfile)      : AppScreen()
    data class GuardianAddContact(val profile: GuardianProfile)          : AppScreen()
    data class GuardianManageMedicines(val profile: GuardianProfile)     : AppScreen()
    data class GuardianAddMedicine(val profile: GuardianProfile)         : AppScreen()
    data class GuardianDailySchedule(val profile: GuardianProfile)       : AppScreen()
    data class GuardianScheduleMedicine(val profile: GuardianProfile)    : AppScreen()
    data class GuardianWellnessSos(val profile: GuardianProfile)         : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CareCompanionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
                    val guardianProfiles   = remember { mutableStateListOf<GuardianProfile>() }
                    val contactsByProfile  = remember { mutableStateMapOf<String, List<ManagedContact>>() }
                    val medicinesByProfile = remember { mutableStateMapOf<String, List<Medicine>>() }

                    when (val s = screen) {

                        is AppScreen.Login -> {
                            LoginScreen { role, _ ->
                                screen = when {
                                    role.contains("Elder", ignoreCase = true)    -> AppScreen.ElderHome("Sunita")
                                    role.contains("Guardian", ignoreCase = true) -> AppScreen.GuardianHome
                                    else                                          -> AppScreen.Login
                                }
                            }
                        }

                        is AppScreen.ElderHome -> {
                            ElderHomeScreen(
                                elderName    = s.name,
                                onSosPressed = {},
                                onLogout     = { screen = AppScreen.Login },
                                elderContacts = contactsByProfile[s.name].orEmpty()
                            )
                        }

                        is AppScreen.GuardianHome -> {
                            GuardianHomeScreen(
                                profiles        = guardianProfiles,
                                onAddProfile    = { screen = AppScreen.GuardianAddProfile },
                                onManageProfile = { profile -> screen = AppScreen.GuardianManageElder(profile) },
                                onLogout        = { screen = AppScreen.Login }
                            )
                        }

                        is AppScreen.GuardianAddProfile -> {
                            GuardianAddProfileScreen(
                                onBack     = { screen = AppScreen.GuardianHome },
                                onSaveNext = { profile ->
                                    guardianProfiles.add(profile)
                                    screen = AppScreen.GuardianHome
                                }
                            )
                        }

                        is AppScreen.GuardianManageElder -> {
                            GuardianManageElderScreen(
                                profile           = s.profile,
                                onBack            = { screen = AppScreen.GuardianHome },
                                onSwitchProfiles  = { screen = AppScreen.GuardianHome },
                                onLogout          = { screen = AppScreen.Login },
                                onOpenContacts    = { screen = AppScreen.GuardianManageContacts(s.profile) },
                                onOpenMedicines   = { screen = AppScreen.GuardianManageMedicines(s.profile) },
                                onOpenDailySchedule = { screen = AppScreen.GuardianDailySchedule(s.profile) },
                                onOpenWellnessSos = { screen = AppScreen.GuardianWellnessSos(s.profile) }
                            )
                        }

                        // ── Contacts ──────────────────────────────────────────────────
                        is AppScreen.GuardianManageContacts -> {
                            GuardianManageContactsScreen(
                                profile         = s.profile,
                                initialContacts = contactsByProfile[s.profile.name].orEmpty(),
                                onBack          = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onSaveContacts  = { updated -> contactsByProfile[s.profile.name] = updated },
                                onAddContact    = { screen = AppScreen.GuardianAddContact(s.profile) },
                                onLogout        = { screen = AppScreen.Login }
                            )
                        }

                        is AppScreen.GuardianAddContact -> {
                            GuardianAddContactScreen(
                                profile = s.profile,
                                onBack  = { screen = AppScreen.GuardianManageContacts(s.profile) },
                                onSave  = { contact ->
                                    val current = contactsByProfile[s.profile.name].orEmpty().toMutableList()
                                    current.add(contact)
                                    contactsByProfile[s.profile.name] = current
                                    screen = AppScreen.GuardianManageContacts(s.profile)
                                }
                            )
                        }

                        // ── Medicines ─────────────────────────────────────────────────
                        is AppScreen.GuardianManageMedicines -> {
                            GuardianManageMedicinesScreen(
                                profile        = s.profile,
                                medicines      = medicinesByProfile[s.profile.name].orEmpty(),
                                onBack         = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onSaveMedicines = { updated -> medicinesByProfile[s.profile.name] = updated },
                                onAddMedicine  = { screen = AppScreen.GuardianAddMedicine(s.profile) },
                                onNavigateHome = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onNavigateSos  = { screen = AppScreen.GuardianWellnessSos(s.profile) }
                            )
                        }

                        is AppScreen.GuardianAddMedicine -> {
                            GuardianAddMedicineScreen(
                                profile = s.profile,
                                onBack  = { screen = AppScreen.GuardianManageMedicines(s.profile) },
                                onSave  = { medicine ->
                                    val current = medicinesByProfile[s.profile.name].orEmpty().toMutableList()
                                    current.add(medicine)
                                    medicinesByProfile[s.profile.name] = current
                                    screen = AppScreen.GuardianManageMedicines(s.profile)
                                }
                            )
                        }

                        // ── Daily Schedule ────────────────────────────────────────────
                        is AppScreen.GuardianDailySchedule -> {
                            GuardianDailyScheduleScreen(
                                profile         = s.profile,
                                medicines       = medicinesByProfile[s.profile.name].orEmpty(),
                                onBack          = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onSaveMedicines = { updated -> medicinesByProfile[s.profile.name] = updated },
                                onAddSchedule   = { screen = AppScreen.GuardianScheduleMedicine(s.profile) },
                                onNavigateHome  = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onNavigateSos   = { screen = AppScreen.GuardianWellnessSos(s.profile) }
                            )
                        }

                        is AppScreen.GuardianScheduleMedicine -> {
                            GuardianScheduleMedicineScreen(
                                medicines = medicinesByProfile[s.profile.name].orEmpty(),
                                onBack    = { screen = AppScreen.GuardianDailySchedule(s.profile) },
                                onSave    = { updated ->
                                    medicinesByProfile[s.profile.name] = updated
                                    screen = AppScreen.GuardianDailySchedule(s.profile)
                                }
                            )
                        }

                        // ── Wellness & SOS ────────────────────────────────────────────
                        is AppScreen.GuardianWellnessSos -> {
                            GuardianWellnessSosScreen(
                                profile         = s.profile,
                                onBack          = { screen = AppScreen.GuardianManageElder(s.profile) },
                                onNavigateHome  = { screen = AppScreen.GuardianManageElder(s.profile) }
                            )
                        }
                    }
                }
            }
        }
    }
}
