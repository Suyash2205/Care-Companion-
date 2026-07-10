package com.carecompanion.app.ui.guardian

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.carecompanion.app.ui.common.Placeholder
import com.carecompanion.app.ui.nav.Routes

@Composable
fun GuardianApp(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val elderArg = listOf(navArgument("elderId") { type = NavType.StringType })

    NavHost(navController = nav, startDestination = Routes.GUARDIAN_HOME) {

        composable(Routes.GUARDIAN_HOME) {
            GuardianDashboardScreen(
                onAddElder = { nav.navigate(Routes.ADD_ELDER) },
                onOpen = { route -> nav.navigate(route) },
                onLogout = onLogout,
            )
        }

        composable(Routes.ADD_ELDER) {
            AddEditElderScreen(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }

        composable(Routes.ELDER_PROFILE, elderArg) {
            AddEditElderScreen(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }

        composable(Routes.CONTACTS, elderArg) { back ->
            val elderId = back.arguments?.getString("elderId")!!
            ContactsScreenG(
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate(Routes.elder(Routes.ADD_CONTACT, elderId)) },
            )
        }
        composable(Routes.ADD_CONTACT, elderArg) {
            AddContactScreenG(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }

        composable(Routes.MEDICINES, elderArg) { back ->
            val elderId = back.arguments?.getString("elderId")!!
            MedicinesScreenG(
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate(Routes.elder(Routes.ADD_MEDICINE, elderId)) },
                onSchedule = { medId -> nav.navigate(Routes.scheduleBuilder(elderId, medId)) },
            )
        }
        composable(Routes.ADD_MEDICINE, elderArg) {
            AddMedicineScreenG(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
        composable(Routes.SCHEDULE, elderArg) {
            MedicinesScreenG(onBack = { nav.popBackStack() }, onAdd = {}, onSchedule = {})
        }
        composable(
            Routes.SCHEDULE_BUILDER,
            listOf(navArgument("elderId") { type = NavType.StringType }, navArgument("medicineId") { type = NavType.StringType })
        ) { back ->
            ScheduleBuilderScreen(
                medicineId = back.arguments?.getString("medicineId")!!,
                onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.REMINDERS, elderArg) { GuardianRemindersScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VITALS, elderArg) { GuardianVitalsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ADHERENCE, elderArg) { GuardianAdherenceScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SOS, elderArg) { GuardianSosScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SOS_SETTINGS, elderArg) { Placeholder("SOS Settings", onBack = { nav.popBackStack() }) }
        composable(Routes.FAMILY, elderArg) { GuardianFamilyScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.OTT, elderArg) { GuardianOttScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.WHEELCHAIR, elderArg) { GuardianWheelchairScreen(onBack = { nav.popBackStack() }) }
    }
}
