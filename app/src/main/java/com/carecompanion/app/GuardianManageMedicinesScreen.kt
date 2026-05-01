package com.carecompanion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianManageMedicinesScreen(
    profile: GuardianProfile,
    medicines: List<Medicine>,
    onBack: () -> Unit,
    onSaveMedicines: (List<Medicine>) -> Unit,
    onAddMedicine: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateSos: () -> Unit = {}
) {
    val localMeds = remember(medicines) { mutableStateListOf<Medicine>().apply { addAll(medicines) } }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GuardianBg,
        bottomBar = {
            GuardianBottomBar(
                activeTab = BottomTab.Home,
                onHome = onNavigateHome,
                onAlerts = onNavigateSos
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MedicinesGrad)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Manage Medicines", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(profile.name, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // ── Elder status card ─────────────────────────────────────────────
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    ElderStatusCard(profile = profile)
                }
            }

            // ── Summary row ───────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MedStatChip(
                        count = localMeds.size.toString(),
                        label = "Total",
                        color = CareGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MedStatChip(
                        count = localMeds.count { it.isActive }.toString(),
                        label = "Active",
                        color = CareGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MedStatChip(
                        count = localMeds.count { !it.isActive }.toString(),
                        label = "Inactive",
                        color = Color(0xFF999999),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Medicine cards ────────────────────────────────────────────────
            if (localMeds.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Medication, contentDescription = null, tint = Color(0xFFC2CCC2), modifier = Modifier.size(52.dp))
                            Text("No medicines added", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GuardianTextPrimary)
                            Text("Tap the button below to add medicines.", fontSize = 13.sp, color = GuardianTextSub, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                itemsIndexed(localMeds) { index, med ->
                    MedicineCard(
                        medicine = med,
                        onToggleActive = {
                            localMeds[index] = med.copy(isActive = !med.isActive)
                            onSaveMedicines(localMeds.toList())
                        },
                        onDelete = { confirmDeleteId = med.id },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Add button ────────────────────────────────────────────────────
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    GradientButton(
                        text = "+ Add New Medicine",
                        onClick = onAddMedicine,
                        gradient = MedicinesGrad,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    confirmDeleteId?.let { targetId ->
        val med = localMeds.find { it.id == targetId }
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Remove medicine?") },
            text = { Text("Remove ${med?.name} from the list?") },
            confirmButton = {
                Button(
                    onClick = {
                        localMeds.removeAll { it.id == targetId }
                        onSaveMedicines(localMeds.toList())
                        confirmDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pill image or icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (medicine.pillImageUri == null)
                                Brush.linearGradient(listOf(Color(0xFFEDE9FE), Color(0xFFC4B5FD)))
                            
                            else Brush.linearGradient(listOf(Color.LightGray, Color.Gray))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (medicine.pillImageUri != null) {
                        UriBitmapImage(
                            uri = medicine.pillImageUri,
                            contentDescription = "Pill",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Outlined.Medication, contentDescription = null, tint = CareGreen, modifier = Modifier.size(28.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(medicine.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GuardianTextPrimary, modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (medicine.isActive) Color(0xFFEAF6EC) else Color(0xFFF2F2F2)
                        ) {
                            Text(
                                if (medicine.isActive) "ACTIVE" else "INACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (medicine.isActive) CareGreen else GuardianTextSub
                            )
                        }
                    }
                    val detail = listOfNotNull(
                        medicine.dosage.takeIf { it.isNotBlank() },
                        medicine.form.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (detail.isNotBlank()) {
                        Text(detail, fontSize = 13.sp, color = GuardianTextSub)
                    }
                }

                Switch(
                    checked = medicine.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CareGreen)
                )
            }

            // Packet images row (if available)
            if (medicine.packetFrontUri != null || medicine.packetBackUri != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    medicine.packetFrontUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .size(56.dp, 40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F8F2))
                        ) {
                            UriBitmapImage(uri = uri, contentDescription = "Front", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    medicine.packetBackUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .size(56.dp, 40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F8F2))
                        ) {
                            UriBitmapImage(uri = uri, contentDescription = "Back", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    Text("Packet images", fontSize = 11.sp, color = GuardianTextSub, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            // Schedule summary
            if (medicine.schedules.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    medicine.schedules.filter { it.enabled }.take(3).forEach { sched ->
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F8F2)) {
                            Text(
                                "${sched.label} · ${sched.time}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = CareGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Delete row
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MedStatChip(count: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = GuardianTextSub)
        }
    }
}
