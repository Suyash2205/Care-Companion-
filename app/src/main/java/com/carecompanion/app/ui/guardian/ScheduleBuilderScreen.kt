package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayLabels.forEachIndexed { i, d ->
                            val bit = 1 shl i
                            val on = (days and bit) != 0
                            Box(Modifier.size(40.dp).clip(CircleShape).background(if (on) CareGreen else Color(0xFFF1F8F2))
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
                    GuardianTextField(value = time, onValueChange = { time = it }, label = "Time (HH:MM 24-hour)")
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
