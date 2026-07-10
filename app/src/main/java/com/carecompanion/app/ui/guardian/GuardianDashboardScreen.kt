package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.carecompanion.app.*
import com.carecompanion.app.data.model.ElderDto
import com.carecompanion.app.ui.nav.Routes
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianDashboardScreen(
    onAddElder: () -> Unit,
    onOpen: (route: String) -> Unit,
    onLogout: () -> Unit,
    vm: GuardianDashboardViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = GuardianBg,
        bottomBar = {
            GuardianBottomBar(
                activeTab = BottomTab.Home,
                onHome = {},
                onAlerts = { ui.selectedElder?.id?.let { onOpen(Routes.elder(Routes.SOS, it)) } },
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF4B8B62), CareGreen)))
                    .statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Care Companion", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Manage your loved ones", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Box {
                            IconButton(onClick = {}) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Alerts", tint = Color.White)
                            }
                            if (ui.unreadAlerts > 0) {
                                Surface(shape = CircleShape, color = Color(0xFFDC2626), modifier = Modifier.align(Alignment.TopEnd)) {
                                    Text("${ui.unreadAlerts}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        TextButton(onClick = onLogout, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFBD5D5))) {
                            Text("Logout", fontSize = 13.sp)
                        }
                    }
                    // Elder selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(ui.elders) { elder ->
                            ElderChip(elder, selected = elder.id == ui.selectedElderId) { elder.id?.let(vm::selectElder) }
                        }
                        item { AddChip(onAddElder) }
                    }
                }
            }

            when {
                ui.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                ui.elders.isEmpty() -> EmptyElders(onAddElder)
                else -> {
                    val elder = ui.selectedElder
                    if (elder?.id != null) {
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.padding(horizontal = 16.dp)) { ElderStatusCard(profileName = elder.name, active = elder.isActive, photoUrl = elder.photoUrl) }
                        Spacer(Modifier.height(20.dp))
                        Text("QUICK ACTIONS", Modifier.padding(horizontal = 20.dp), fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = GuardianTextSub, letterSpacing = 1.sp)
                        Spacer(Modifier.height(10.dp))
                        QuickActions(elder.id) { route -> onOpen(route) }
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            OutlinedButton(onClick = { onOpen(Routes.elder(Routes.ELDER_PROFILE, elder.id)) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Edit ${elder.name}'s profile")
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ElderChip(elder: ElderDto, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .then(if (selected) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (elder.photoUrl != null) AsyncImage(model = elder.photoUrl, contentDescription = elder.name,
                modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            else Text(elder.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(elder.name.take(8), fontSize = 12.sp, color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun AddChip(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, contentDescription = "Add", tint = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text("Add", fontSize = 12.sp, color = Color.White)
    }
}

@Composable
private fun ElderStatusCard(profileName: String, active: Boolean, photoUrl: String?) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color.White).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFEAF6EC)), contentAlignment = Alignment.Center) {
                if (photoUrl != null) AsyncImage(model = photoUrl, contentDescription = profileName,
                    modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                else Text(profileName.take(1).uppercase(), color = CareGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(profileName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                Text(if (active) "At Home · monitored" else "Profile deactivated", fontSize = 13.sp, color = GuardianTextSub)
            }
            Surface(shape = RoundedCornerShape(20.dp), color = if (active) CareGreen else Color(0xFF9CA3AF)) {
                Text(if (active) "SAFE" else "OFF", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun QuickActions(elderId: String, onOpen: (String) -> Unit) {
    data class Action(val title: String, val sub: String, val icon: ImageVector, val grad: Brush, val route: String)
    val actions = listOf(
        Action("Contacts", "Emergency & calls", Icons.Outlined.Call, ContactsGrad, Routes.elder(Routes.CONTACTS, elderId)),
        Action("Medicines", "Add & manage meds", Icons.Outlined.Medication, MedicinesGrad, Routes.elder(Routes.MEDICINES, elderId)),
        Action("Schedule", "Timings & reminders", Icons.Outlined.Schedule, ScheduleGrad, Routes.elder(Routes.SCHEDULE, elderId)),
        Action("Reminders", "Water · walk · vitals", Icons.Outlined.NotificationsActive, ScheduleGrad, Routes.elder(Routes.REMINDERS, elderId)),
        Action("Vitals", "BP · sugar · PDF", Icons.Outlined.FavoriteBorder, MedicinesGrad, Routes.elder(Routes.VITALS, elderId)),
        Action("Adherence", "Taken & missed", Icons.Outlined.Schedule, ContactsGrad, Routes.elder(Routes.ADHERENCE, elderId)),
        Action("Videos", "OTT shortcuts", Icons.Outlined.Add, ScheduleGrad, Routes.elder(Routes.OTT, elderId)),
        Action("SOS & Family", "Alerts & members", Icons.Outlined.Warning, SosGrad, Routes.elder(Routes.SOS, elderId)),
    )
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { a ->
                    Box(
                        modifier = Modifier.weight(1f).height(104.dp).clip(RoundedCornerShape(20.dp))
                            .background(a.grad).clickable { onOpen(a.route) }.padding(14.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(a.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(a.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text(a.sub, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.TopEnd).size(16.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyElders(onAddElder: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(84.dp).background(Color(0xFFEAF5EA), CircleShape).clickable(onClick = onAddElder), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, contentDescription = "Add", tint = CareGreen, modifier = Modifier.size(44.dp))
        }
        Text("Add an elder profile", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
        Text("Tap to create the first profile you'll manage.", fontSize = 14.sp, color = GuardianTextSub)
    }
}
