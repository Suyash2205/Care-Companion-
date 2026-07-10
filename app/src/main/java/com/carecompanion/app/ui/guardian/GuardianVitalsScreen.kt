package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.*
import com.carecompanion.app.data.model.VitalDto
import com.carecompanion.app.data.repo.Severity
import com.carecompanion.app.ui.theme.CareGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianVitalsScreen(onBack: () -> Unit, vm: VitalsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(ui.error) { ui.error?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        containerColor = GuardianBg,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = CareGreen, contentColor = Color.White) {
                Text("+", fontSize = 24.sp)
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Vitals", onBack)
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                else -> {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            val file = vm.buildPdf(context)
                            if (file != null) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", file
                                )
                                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(share, "Share Vitals Report"))
                            } else scope.launch { snackbar.showSnackbar("Could not create PDF") }
                        }) { Text("Export PDF", color = CareGreen, fontWeight = FontWeight.Bold) }
                    }

                    // ── Stat cards ────────────────────────────────────────────
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("BP", ui.latest("bp"), ui.severities, Modifier.weight(1f))
                        StatCard("Sugar", ui.latest("sugar"), ui.severities, Modifier.weight(1f))
                        StatCard("Temp", ui.latest("temp"), ui.severities, Modifier.weight(1f))
                    }

                    // ── Tab bar ───────────────────────────────────────────────
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("bp" to "BP", "sugar" to "Sugar", "temp" to "Temp").forEach { (t, l) ->
                            FilterChip(
                                selected = ui.tab == t,
                                onClick = { vm.setTab(t) },
                                label = { Text(l) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White),
                            )
                        }
                    }

                    // ── History (filtered by tab) ─────────────────────────────
                    val history = ui.all.filter { it.type == ui.tab }
                    if (history.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
                            Text("No ${vitalTypeLabel(ui.tab).lowercase()} readings yet. Tap + to add one.", color = GuardianTextSub)
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(history) { v ->
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(vitalValueText(v), fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                                            Text(vitalDateLabel(v.takenAt), fontSize = 12.sp, color = GuardianTextSub)
                                        }
                                        SeverityPill(v.id?.let { ui.severities[it] })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddReadingSheet(
            type = ui.tab,
            saving = ui.saving,
            onDismiss = { showAdd = false },
            onSave = { v1, v2, ctx, note ->
                vm.addReading(ui.tab, v1, v2, ctx, note)
                showAdd = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReadingSheet(
    type: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (v1: Double, v2: Double?, context: String?, note: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }
    var sugarCtx by remember { mutableStateOf("fasting") }
    var note by remember { mutableStateOf("") }

    val v1 = value1.toDoubleOrNull()
    val v2 = value2.toDoubleOrNull()
    val valid = when (type) {
        "bp" -> v1 != null && v2 != null
        else -> v1 != null
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Add ${vitalTypeLabel(type)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)

            when (type) {
                "bp" -> {
                    GuardianTextField(value = value1, onValueChange = { value1 = it }, label = "Systolic (upper)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    GuardianTextField(value = value2, onValueChange = { value2 = it }, label = "Diastolic (lower)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                "sugar" -> {
                    GuardianTextField(value = value1, onValueChange = { value1 = it }, label = "Blood sugar (mg/dL)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Text("Context", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("fasting" to "Fasting", "post_meal" to "Post Meal").forEach { (v, l) ->
                            FilterChip(
                                selected = sugarCtx == v,
                                onClick = { sugarCtx = v },
                                label = { Text(l) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White),
                            )
                        }
                    }
                }
                else -> {
                    GuardianTextField(value = value1, onValueChange = { value1 = it }, label = "Temperature (°F)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }

            GuardianTextField(value = note, onValueChange = { note = it }, label = "Note (optional)")

            GradientButton(
                text = if (saving) "Saving…" else "Save Reading",
                onClick = {
                    if (valid) onSave(
                        v1!!,
                        if (type == "bp") v2 else null,
                        if (type == "sugar") sugarCtx else null,
                        note.ifBlank { null },
                    )
                },
                enabled = !saving && valid,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(title: String, v: VitalDto?, severities: Map<String, Severity>, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 12.sp, color = GuardianTextSub, fontWeight = FontWeight.SemiBold)
            Text(if (v != null) vitalValueText(v) else "—", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
            SeverityPill(v?.id?.let { severities[it] })
        }
    }
}

@Composable
private fun SeverityPill(sev: Severity?) {
    val (bg, fg, label) = when (sev) {
        Severity.NORMAL -> Triple(Color(0xFFEAF6EC), Color(0xFF2E8540), "NORMAL")
        Severity.WARNING -> Triple(Color(0xFFFFF8E1), Color(0xFF6B4D00), "WARNING")
        Severity.HIGH -> Triple(Color(0xFFFDECEC), Color(0xFFDC2626), "HIGH")
        null -> Triple(Color(0xFFF1F5F9), GuardianTextSub, "—")
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}
