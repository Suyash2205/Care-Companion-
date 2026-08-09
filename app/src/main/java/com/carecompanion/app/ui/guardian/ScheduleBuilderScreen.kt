package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.*
import com.carecompanion.app.ui.theme.CareGreen

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
private val timePresets = listOf("07:00", "08:00", "12:00", "14:00", "19:00", "21:00")

@Composable
fun ScheduleBuilderScreen(medicineId: String, onDone: () -> Unit, onBack: () -> Unit, vm: MedicinesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(ui.done) { if (ui.done) onDone() }

    var days by remember { mutableStateOf(127) } // all days
    var labels by remember { mutableStateOf(setOf("Breakfast")) }
    var time by remember { mutableStateOf("08:00") }
    var withWater by remember { mutableStateOf(true) }
    var meal by remember { mutableStateOf("after") }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        TimePickerDialogCC(
            initial = time,
            onDismiss = { showTimePicker = false },
            onConfirm = { picked -> time = picked; showTimePicker = false },
        )
    }

    Scaffold(containerColor = GuardianBg, bottomBar = {
        Surface(color = Color.White, shadowElevation = 12.dp) {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                GradientButton(text = if (ui.saving) "Saving…" else "Save Schedule",
                    onClick = { if (labels.isNotEmpty()) vm.addSchedules(medicineId, labels.toList(), time, days, withWater, meal) },
                    enabled = !ui.saving && labels.isNotEmpty() && days != 0, modifier = Modifier.fillMaxWidth())
            }
        }
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Schedule Medicine", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                // Days
                CardSection("Days") {
                    // weight(), not a fixed 40.dp: seven fixed circles plus gaps and the
                    // card padding came to exactly the width of a 360dp phone, so Sunday
                    // was clipped off the edge and could never be switched off.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEachIndexed { i, d ->
                            val bit = 1 shl i
                            val on = (days and bit) != 0
                            Box(Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(if (on) CareGreen else Color(0xFFF1F8F2))
                                .clickable { days = days xor bit }, contentAlignment = Alignment.Center) {
                                Text(d, color = if (on) Color.White else GuardianTextSub, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { days = 127 }) { Text("Every day") }
                        TextButton(onClick = { days = 31 }) { Text("Mon–Fri") }
                    }
                }
                // Meal slots
                CardSection("Meal") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Breakfast", "Lunch", "Dinner").forEach { m ->
                            val on = m in labels
                            FilterChip(selected = on, onClick = { labels = if (on) labels - m else labels + m }, label = { Text(m) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("before" to "Before meal", "after" to "After meal").forEach { (v, l) ->
                            FilterChip(selected = meal == v, onClick = { meal = v }, label = { Text(l) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White))
                        }
                    }
                }
                // Time
                CardSection("Time") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScrollWrap()) {
                        timePresets.forEach { t ->
                            val on = time == t
                            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (on) CareGreen else Color(0xFFF1F8F2))
                                .clickable { time = t }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text(t, color = if (on) Color.White else GuardianTextSub, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pick time — $time", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                CardSection("Water") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Take with water", Modifier.weight(1f), color = GuardianTextPrimary)
                        Switch(checked = withWater, onCheckedChange = { withWater = it }, colors = SwitchDefaults.colors(checkedTrackColor = CareGreen))
                    }
                }
                ui.error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun CardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
            content()
        }
    }
}

private fun Modifier.horizontalScrollWrap(): Modifier = this

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogCC(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val h = initial.split(":").getOrNull(0)?.toIntOrNull() ?: 8
    val m = initial.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) {
                Text("OK", color = CareGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) } },
    )
}
