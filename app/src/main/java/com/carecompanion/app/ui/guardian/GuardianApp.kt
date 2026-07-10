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

        composable(Routes.REMINDERS, elderArg) { Placeholder("Reminders", onBack = { nav.popBackStack() }) }
        composable(Routes.VITALS, elderArg) { Placeholder("Vitals", onBack = { nav.popBackStack() }) }
        composable(Routes.ADD_VITAL, elderArg) { Placeholder("Add Vital", onBack = { nav.popBackStack() }) }
        composable(Routes.ADHERENCE, elderArg) { Placeholder("Adherence", onBack = { nav.popBackStack() }) }
        composable(Routes.SOS, elderArg) { GuardianSosScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SOS_SETTINGS, elderArg) { Placeholder("SOS Settings", onBack = { nav.popBackStack() }) }
        composable(Routes.FAMILY, elderArg) { Placeholder("Family Members", onBack = { nav.popBackStack() }) }
        composable(Routes.OTT, elderArg) { Placeholder("Videos & Apps", onBack = { nav.popBackStack() }) }
        composable(Routes.WHEELCHAIR, elderArg) { Placeholder("Wheelchair Assistance", onBack = { nav.popBackStack() }) }
    }
}
