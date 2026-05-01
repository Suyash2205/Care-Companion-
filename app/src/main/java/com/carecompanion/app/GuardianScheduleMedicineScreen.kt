package com.carecompanion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

private val MealOptions = listOf("Breakfast", "Lunch", "Dinner")
private val TimePresets = listOf("7:00 AM", "8:00 AM", "12:00 PM", "2:00 PM", "7:00 PM", "9:00 PM")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuardianScheduleMedicineScreen(
    medicines: List<Medicine>,
    onBack: () -> Unit,
    onSave: (List<Medicine>) -> Unit
) {
    var selectedMedicineId by remember { mutableStateOf<String?>(null) }
    var selectedMeals by remember { mutableStateOf(setOf<String>()) }
    var mealTiming by remember { mutableStateOf(MealTiming.Before) }
    var withWater by remember { mutableStateOf(false) }
    var customTime by remember { mutableStateOf("8:00 AM") }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GuardianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ── Gradient header ─────────────────────────────────────────────
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
                    Column {
                        Text("Schedule Medicine", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Set timing and preferences", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // ── Select medicine ──────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Medication, contentDescription = null, tint = CareGreen, modifier = Modifier.size(20.dp))
                            Text("Select Medicine", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                        }
                        if (medicines.isEmpty()) {
                            Text(
                                "No medicines found. Add a medicine first.",
                                fontSize = 13.sp,
                                color = GuardianTextSub
                            )
                        } else {
                            medicines.forEach { med ->
                                val isSelected = selectedMedicineId == med.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFFF1F8F2) else Color(0xFFF8FAF8)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) CareGreen else Color(0xFFE0E4E0),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedMedicineId = med.id }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (isSelected) CareGreen else Color(0xFFE0E4E0),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Medication,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else GuardianTextSub,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            med.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = if (isSelected) CareGreen else GuardianTextPrimary
                                        )
                                        val detail = listOfNotNull(
                                            med.dosage.takeIf { it.isNotBlank() },
                                            med.form.takeIf { it.isNotBlank() }
                                        ).joinToString(", ")
                                        if (detail.isNotBlank()) {
                                            Text(detail, fontSize = 12.sp, color = GuardianTextSub)
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = CareGreen, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── When to take (meal) ──────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = CareGreen, modifier = Modifier.size(20.dp))
                            Text("When to Take", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                        }
                        Text("Select one or more meals:", fontSize = 12.sp, color = GuardianTextSub)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MealOptions.forEach { meal ->
                                val selected = meal in selectedMeals
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedMeals = if (selected) selectedMeals - meal else selectedMeals + meal
                                    },
                                    label = { Text(meal, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CareGreen,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF1F8F2),
                                        labelColor = GuardianTextSub
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Text("Before or After meal:", fontSize = 12.sp, color = GuardianTextSub)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(MealTiming.Before to "Before Meal", MealTiming.After to "After Meal").forEach { (timing, label) ->
                                val selected = mealTiming == timing
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) CareGreen else Color(0xFFF1F8F2))
                                        .clickable { mealTiming = timing }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        label,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else GuardianTextSub
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Water preference ─────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(if (withWater) Color(0xFFEAF6EC) else Color(0xFFF1F8F2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.LocalDrink,
                                contentDescription = null,
                                tint = if (withWater) CareGreen else GuardianTextSub,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Take With Water", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = GuardianTextPrimary)
                            Text(if (withWater) "Yes, with a full glass of water" else "No water required", fontSize = 12.sp, color = GuardianTextSub)
                        }
                        Switch(
                            checked = withWater,
                            onCheckedChange = { withWater = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CareGreen)
                        )
                    }
                }

                // ── Time picker ──────────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = CareGreen, modifier = Modifier.size(20.dp))
                            Text("Time", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                        }
                        Text("Quick select:", fontSize = 12.sp, color = GuardianTextSub)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimePresets.forEach { preset ->
                                val selected = customTime == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) CareGreen else Color(0xFFF1F8F2))
                                        .clickable { customTime = preset }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        preset,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else GuardianTextSub
                                    )
                                }
                            }
                        }
                        GuardianTextField(
                            value = customTime,
                            onValueChange = { customTime = it },
                            label = "Custom time (e.g. 6:30 AM)",
                            leadingIcon = {
                                Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // ── Save button ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                GradientButton(
                    text = "Add to Schedule",
                    onClick = {
                        val medId = selectedMedicineId
                        if (medId == null) {
                            snackMsg = "Please select a medicine"
                            return@GradientButton
                        }
                        if (selectedMeals.isEmpty()) {
                            snackMsg = "Please select at least one meal"
                            return@GradientButton
                        }
                        val updatedMedicines = medicines.map { med ->
                            if (med.id != medId) med
                            else {
                                val newSlots = selectedMeals.map { meal ->
                                    MedicineSchedule(
                                        label = meal,
                                        time = customTime,
                                        enabled = true,
                                        withWater = withWater,
                                        mealTiming = mealTiming
                                    )
                                }
                                val existingLabels = med.schedules.map { it.label }.toSet()
                                val merged = med.schedules.toMutableList()
                                newSlots.forEach { slot ->
                                    if (slot.label !in existingLabels) merged.add(slot)
                                    else {
                                        val idx = merged.indexOfFirst { it.label == slot.label }
                                        if (idx >= 0) merged[idx] = slot
                                    }
                                }
                                med.copy(schedules = merged)
                            }
                        }
                        onSave(updatedMedicines)
                    },
                    gradient = ScheduleGrad,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
