package com.carecompanion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianManageElderScreen(
    profile: GuardianProfile,
    onBack: () -> Unit,
    onSwitchProfiles: () -> Unit,
    onLogout: () -> Unit,
    onOpenContacts: () -> Unit = {},
    onOpenMedicines: () -> Unit = {},
    onOpenDailySchedule: () -> Unit = {},
    onOpenWellnessSos: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GuardianBg,
        bottomBar = {
            GuardianBottomBar(
                activeTab = BottomTab.Home,
                onHome = onBack,
                onAlerts = onOpenWellnessSos
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Hero header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Top row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Manage Care", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                        TextButton(onClick = onSwitchProfiles, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF93C5FD))) {
                            Icon(Icons.Outlined.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Switch", fontSize = 13.sp)
                        }
                        TextButton(onClick = onLogout, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))) {
                            Text("Logout", fontSize = 13.sp)
                        }
                    }

                    // Profile card in hero
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.photoUri != null) {
                                UriBitmapImage(
                                    uri = profile.photoUri,
                                    contentDescription = profile.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(profile.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(7.dp).background(Color(0xFF22C55E), CircleShape))
                                Text("At Home · 15 mins ago", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                            }
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = CareGreen) {
                            Text(
                                "SAFE",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Section label ──────────────────────────────────────────────────
            Text(
                "QUICK ACTIONS",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GuardianTextSub,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))

            // ── 2×2 action grid ────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionTile(
                        title = "Contacts",
                        subtitle = "Emergency list & calls",
                        icon = Icons.Outlined.Call,
                        gradient = ContactsGrad,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenContacts
                    )
                    ActionTile(
                        title = "Medicines",
                        subtitle = "Add & manage meds",
                        icon = Icons.Outlined.Medication,
                        gradient = MedicinesGrad,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMedicines
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionTile(
                        title = "Schedule",
                        subtitle = "Timings & reminders",
                        icon = Icons.Outlined.Schedule,
                        gradient = ScheduleGrad,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDailySchedule
                    )
                    ActionTile(
                        title = "Wellness",
                        subtitle = "SOS & tracking",
                        icon = Icons.Outlined.FavoriteBorder,
                        gradient = SosGrad,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenWellnessSos
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Alerts card ────────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Text("Recent Alerts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GuardianTextPrimary)
                        }
                        Surface(shape = CircleShape, color = Color(0xFFDC2626)) {
                            Text("2", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    AlertSummaryRow(
                        text = "${profile.name} missed 8:00 AM medicine",
                        time = "1h ago",
                        color = Color(0xFFF97316)
                    )
                    AlertSummaryRow(
                        text = "SOS triggered · Mira Road, Mumbai",
                        time = "2h ago",
                        color = Color(0xFFDC2626)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(18.dp)
        )
    }
}

@Composable
private fun AlertSummaryRow(text: String, time: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(text, modifier = Modifier.weight(1f), fontSize = 13.sp, color = GuardianTextPrimary)
        Text(time, fontSize = 11.sp, color = GuardianTextSub)
    }
}
