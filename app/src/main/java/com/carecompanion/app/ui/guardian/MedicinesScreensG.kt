package com.carecompanion.app.ui.guardian

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.carecompanion.app.*
import com.carecompanion.app.ui.theme.CareGreen

private val forms = listOf("Tablet", "Capsule", "Syrup", "Drops", "Injection")

@Composable
fun MedicinesScreenG(onBack: () -> Unit, onAdd: () -> Unit, onSchedule: (String) -> Unit, vm: MedicinesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    Scaffold(
        containerColor = GuardianBg,
        floatingActionButton = { FloatingActionButton(onClick = onAdd, containerColor = CareGreen, contentColor = Color.White) { Text("+", fontSize = 24.sp) } }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Medicines", onBack)
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                ui.medicines.isEmpty() -> Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) { Text("No medicines yet. Tap + to add one.", color = GuardianTextSub) }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.medicines) { m ->
                        val scheds = ui.schedules[m.id].orEmpty()
                        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEDE9FE)), contentAlignment = Alignment.Center) {
                                        if (m.pillUrl != null) AsyncImage(model = m.pillUrl, contentDescription = m.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                        else Icon(Icons.Outlined.Medication, contentDescription = null, tint = CareGreen)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(m.name, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                                        val detail = listOfNotNull(m.dosage.ifBlank { null }, m.form).joinToString(" · ")
                                        Text(detail, fontSize = 13.sp, color = GuardianTextSub)
                                    }
                                    Switch(checked = m.isActive, onCheckedChange = { vm.toggleActive(m) }, colors = SwitchDefaults.colors(checkedTrackColor = CareGreen))
                                }
                                if (scheds.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    scheds.take(3).forEach { s ->
                                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F8F2)) {
                                            Text("${s.label} · ${s.time}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = CareGreen)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { m.id?.let(onSchedule) }) {
                                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Schedule")
                                    }
                                    TextButton(onClick = { m.id?.let(vm::deleteMedicine) }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddMedicineScreenG(onDone: () -> Unit, onBack: () -> Unit, vm: MedicinesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var form by remember { mutableStateOf("Tablet") }
    var withLiquid by remember { mutableStateOf<String?>("water") }
    var meal by remember { mutableStateOf<String?>("after") }
    var pill by remember { mutableStateOf<Uri?>(null) }
    var pf by remember { mutableStateOf<Uri?>(null) }
    var pb by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(ui.done) { if (ui.done) onDone() }

    Scaffold(containerColor = GuardianBg, bottomBar = {
        Surface(color = Color.White, shadowElevation = 12.dp) {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                GradientButton(text = if (ui.saving) "Saving…" else "Save Medicine",
                    onClick = { if (name.isNotBlank()) vm.addMedicine(name, dosage, form, withLiquid, meal, pill, pf, pb) },
                    enabled = !ui.saving && name.isNotBlank(), modifier = Modifier.fillMaxWidth())
            }
        }
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Add Medicine", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GuardianTextField(value = name, onValueChange = { name = it }, label = "Medicine Name")
                GuardianTextField(value = dosage, onValueChange = { dosage = it }, label = "Dosage (e.g. 500mg)")
                Text("Form", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    forms.forEach { f -> FilterChip(selected = form == f, onClick = { form = f }, label = { Text(f) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White)) }
                }
                Text("Take with", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("water" to "Water", "milk" to "Milk").forEach { (v, l) ->
                        FilterChip(selected = withLiquid == v, onClick = { withLiquid = v }, label = { Text(l) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White))
                    }
                }
                Text("Meal timing", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("before" to "Before meal", "after" to "After meal").forEach { (v, l) ->
                        FilterChip(selected = meal == v, onClick = { meal = v }, label = { Text(l) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White))
                    }
                }
                Text("Photos (help the elder identify it)", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MedImagePick("Pill", pill, Modifier.weight(1f)) { pill = it }
                    MedImagePick("Front", pf, Modifier.weight(1f)) { pf = it }
                    MedImagePick("Back", pb, Modifier.weight(1f)) { pb = it }
                }
                ui.error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MedImagePick(label: String, uri: Uri?, modifier: Modifier, onPick: (Uri?) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { onPick(it) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.85f).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF1F8F2))
            .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
            if (uri != null) UriBitmapImage(uri, label, Modifier.fillMaxSize(), ContentScale.Crop)
            else Text("+", fontSize = 28.sp, color = GuardianTextSub)
        }
        Text(label, fontSize = 11.sp, color = GuardianTextSub)
    }
}
