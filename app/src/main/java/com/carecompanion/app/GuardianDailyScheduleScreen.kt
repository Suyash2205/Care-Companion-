package com.carecompanion.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

private data class GenericReminder(
    val label: String, val subtitle: String, val time: String,
    val icon: ImageVector, val enabled: Boolean = true
)

private data class ActivityReminder(
    val label: String, val time: String,
    val icon: ImageVector, val enabled: Boolean = true
)

@Composable
fun GuardianDailyScheduleScreen(
    profile: GuardianProfile,
    medicines: List<Medicine>,
    onBack: () -> Unit,
    onSaveMedicines: (List<Medicine>) -> Unit,
    onAddSchedule: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateSos: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val localMeds = remember(medicines) { mutableStateListOf<Medicine>().apply { addAll(medicines) } }

    val genericReminders = remember {
        mutableStateListOf(
            GenericReminder("Vitals Check", "Blood pressure · Pulse", "9:00 AM", Icons.Outlined.MonitorHeart, true),
            GenericReminder("Blood Sugar", "Fasting check", "7:00 AM", Icons.Outlined.Favorite, false)
        )
    }
    val activityReminders = remember {
        mutableStateListOf(
            ActivityReminder("Morning Walk", "6:30 AM", Icons.Outlined.DirectionsWalk, true),
            ActivityReminder("Hydration Reminder", "Every 2 hrs", Icons.Outlined.LocalDrink, true),
            ActivityReminder("Evening Exercise", "5:00 PM", Icons.Outlined.FitnessCenter, false),
            ActivityReminder("Meditation", "7:00 PM", Icons.Outlined.Spa, false)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GuardianBg,
        bottomBar = {
            GuardianBottomBar(
                activeTab = BottomTab.Home,
                onHome = onNavigateHome,
                onAlerts = onNavigateSos
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSchedule,
                containerColor = CareGreen,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add schedule")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScheduleGrad)
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
                            Text("Daily Schedule", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(profile.name, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // ── Tab selector ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    listOf("Medicines", "Activities").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ScheduleGrad else Brush.linearGradient(listOf(Color.White, Color.White)))
                                .clickable { selectedTab = index }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White else GuardianTextSub
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // ── Generic reminders ──────────────────────────────────────
                item {
                    Text(
                        "GENERIC REMINDERS",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GuardianTextSub,
                        letterSpacing = 1.sp
                    )
                }
                itemsIndexed(genericReminders) { index, reminder ->
                    GenericReminderCard(
                        reminder = reminder,
                        onToggle = { genericReminders[index] = reminder.copy(enabled = !reminder.enabled) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // ── Scheduled medications ──────────────────────────────────
                item {
                    Text(
                        "SCHEDULED MEDICATIONS",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GuardianTextSub,
                        letterSpacing = 1.sp
                    )
                }

                if (localMeds.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Medication, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(40.dp))
                                Text("No medicines scheduled", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                                Text("Tap the + button to schedule a medicine.", fontSize = 13.sp, color = GuardianTextSub, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    itemsIndexed(localMeds) { medIndex, med ->
                        if (med.schedules.isNotEmpty()) {
                            ScheduledMedicineCard(
                                medicine = med,
                                onToggleSchedule = { schedIndex, enabled ->
                                    val updatedSchedules = med.schedules.toMutableList()
                                    updatedSchedules[schedIndex] = updatedSchedules[schedIndex].copy(enabled = enabled)
                                    localMeds[medIndex] = med.copy(schedules = updatedSchedules)
                                    onSaveMedicines(localMeds.toList())
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (localMeds.none { it.schedules.isNotEmpty() }) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White
                            ) {
                                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                    Text("Tap + to schedule medicines", fontSize = 13.sp, color = GuardianTextSub, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

            } else {
                // ── Activities tab ─────────────────────────────────────────
                item {
                    Text(
                        "DAILY ACTIVITIES",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GuardianTextSub,
                        letterSpacing = 1.sp
                    )
                }
                itemsIndexed(activityReminders) { index, activity ->
                    ActivityReminderCard(
                        activity = activity,
                        onToggle = { activityReminders[index] = activity.copy(enabled = !activity.enabled) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GenericReminderCard(
    reminder: GenericReminder,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (reminder.enabled) Color(0xFFEAF6EC) else Color(0xFFF2F2F2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(reminder.icon, contentDescription = null, tint = if (reminder.enabled) CareGreen else GuardianTextSub, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = GuardianTextPrimary)
                Text("${reminder.subtitle} · ${reminder.time}", fontSize = 12.sp, color = GuardianTextSub)
            }
            Switch(
                checked = reminder.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CareGreen)
            )
        }
    }
}

@Composable
private fun ScheduledMedicineCard(
    medicine: Medicine,
    onToggleSchedule: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFFEAF6EC), Color(0xFFCAE7D2))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Medication, contentDescription = null, tint = CareGreen, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(medicine.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GuardianTextPrimary)
                    val detail = listOfNotNull(medicine.dosage.takeIf { it.isNotBlank() }, medicine.form.takeIf { it.isNotBlank() }).joinToString(" · ")
                    if (detail.isNotBlank()) Text(detail, fontSize = 12.sp, color = GuardianTextSub)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (medicine.isActive) Color(0xFFEAF6EC) else Color(0xFFF2F2F2)
                ) {
                    Text(
                        if (medicine.isActive) "ACTIVE" else "PAUSED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (medicine.isActive) CareGreen else GuardianTextSub
                    )
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null, tint = GuardianTextSub
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    medicine.schedules.forEachIndexed { index, sched ->
                        ScheduleSlotRow(
                            schedule = sched,
                            onToggle = { onToggleSchedule(index, !sched.enabled) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleSlotRow(schedule: MedicineSchedule, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (schedule.enabled) Color(0xFFF1F8F2) else Color(0xFFFAFAFA))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(schedule.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GuardianTextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(schedule.time, fontSize = 12.sp, color = GuardianTextSub)
                Text("·", fontSize = 12.sp, color = GuardianTextSub)
                Text(
                    "${if (schedule.mealTiming == MealTiming.Before) "Before" else "After"} meal",
                    fontSize = 12.sp, color = GuardianTextSub
                )
                if (schedule.withWater) {
                    Text("·", fontSize = 12.sp, color = GuardianTextSub)
                    Text("With water", fontSize = 12.sp, color = CareGreen)
                }
            }
        }
        Switch(
            checked = schedule.enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CareGreen)
        )
    }
}

@Composable
private fun ActivityReminderCard(
    activity: ActivityReminder,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (activity.enabled) Color(0xFFEAF6EC) else Color(0xFFF2F2F2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(activity.icon, contentDescription = null, tint = if (activity.enabled) CareGreen else GuardianTextSub, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = GuardianTextPrimary)
                Text(activity.time, fontSize = 12.sp, color = GuardianTextSub)
            }
            Switch(
                checked = activity.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CareGreen)
            )
        }
    }
}
