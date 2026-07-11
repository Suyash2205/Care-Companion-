package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.*
import com.carecompanion.app.data.model.ReminderCategoryDto
import com.carecompanion.app.data.model.ReminderDto
import com.carecompanion.app.ui.theme.CareGreen

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")           // Mon..Sun
private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun repeatText(r: ReminderDto): String = when {
    r.repeatKind == "interval" && r.intervalMinutes != null -> "Every ${r.intervalMinutes} min"
    r.days == 127 -> "Every day"
    r.days == 31 -> "Mon–Fri"
    r.days == 0 -> "No days"
    else -> (0..6).filter { (r.days and (1 shl it)) != 0 }.joinToString(", ") { dayNames[it] }
}

@Composable
fun GuardianRemindersScreen(onBack: () -> Unit, vm: RemindersViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    var showAdd by remember { mutableStateOf(false) }
    var filterCatId by remember { mutableStateOf<String?>(null) }   // null = All

    val catById = ui.categories.associateBy { it.id }
    val shown = ui.reminders.filter { filterCatId == null || it.categoryId == filterCatId }

    Scaffold(
        containerColor = GuardianBg,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = CareGreen, contentColor = Color.White) {
                Text("+", fontSize = 24.sp)
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Reminders", onBack)

            // ── Category filter chips ─────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = filterCatId == null,
                    onClick = { filterCatId = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White),
                )
                ui.categories.forEach { c ->
                    FilterChip(
                        selected = filterCatId == c.id,
                        onClick = { filterCatId = c.id },
                        label = { Text(listOfNotNull(c.icon?.takeIf { it.length <= 2 }, c.name).joinToString(" ")) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White),
                    )
                }
            }
            Text(
                "Medicine reminders come from schedules, not here.",
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp, color = GuardianTextSub,
            )

            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                shown.isEmpty() -> Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
                    Text("No reminders yet. Tap + to add one.", color = GuardianTextSub)
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown) { r ->
                        val cat = catById[r.categoryId]
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFEAF6EC)), contentAlignment = Alignment.Center) {
                                    val ic = cat?.icon
                                    if (ic != null && ic.length <= 2) Text(ic, fontSize = 20.sp)
                                    else Icon(Icons.Outlined.Notifications, contentDescription = null, tint = CareGreen)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(r.title, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                                    val subtitle = listOfNotNull(
                                        r.times.joinToString(", ").ifBlank { null },
                                        repeatText(r),
                                    ).joinToString(" · ")
                                    Text(subtitle, fontSize = 13.sp, color = GuardianTextSub)
                                }
                                Switch(checked = r.enabled, onCheckedChange = { vm.toggleReminder(r) }, colors = SwitchDefaults.colors(checkedTrackColor = CareGreen))
                                IconButton(onClick = { r.id?.let(vm::deleteReminder) }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddReminderDialog(
            categories = ui.categories,
            saving = ui.saving,
            error = ui.error,
            onAddCategory = vm::addCustomCategory,
            onDismiss = { showAdd = false },
            onSave = { title, catId, times, days ->
                vm.addReminder(title, catId, times, days)
                showAdd = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddReminderDialog(
    categories: List<ReminderCategoryDto>,
    saving: Boolean,
    error: String?,
    onAddCategory: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (title: String, categoryId: String?, times: List<String>, days: Int) -> Unit,
) {
    // Only non-virtual categories are selectable for adding (Medicine is virtual).
    val selectable = categories.filter { !it.isVirtual }

    var title by remember { mutableStateOf("") }
    var selectedCatId by remember(selectable) { mutableStateOf(selectable.firstOrNull()?.id) }
    var timeInput by remember { mutableStateOf("08:00") }
    var times by remember { mutableStateOf(listOf<String>()) }
    var days by remember { mutableStateOf(127) }
    var newCategory by remember { mutableStateOf("") }

    val canSave = title.isNotBlank() && times.isNotEmpty() && days != 0 && !saving

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(title, selectedCatId, times, days) },
            ) { Text(if (saving) "Saving…" else "Save", color = if (canSave) CareGreen else GuardianTextSub) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GuardianTextSub) } },
        title = { Text("New Reminder", fontWeight = FontWeight.Bold, color = GuardianTextPrimary) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GuardianTextField(value = title, onValueChange = { title = it }, label = "Title (e.g. Drink water)")

                // Category chips
                Text("Category", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary, fontSize = 14.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectable.forEach { c ->
                        FilterChip(
                            selected = selectedCatId == c.id,
                            onClick = { selectedCatId = c.id },
                            label = { Text(listOfNotNull(c.icon?.takeIf { it.length <= 2 }, c.name).joinToString(" ")) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GuardianTextField(value = newCategory, onValueChange = { newCategory = it }, label = "New category", modifier = Modifier.weight(1f))
                    TextButton(
                        enabled = newCategory.isNotBlank() && !saving,
                        onClick = { onAddCategory(newCategory); newCategory = "" },
                    ) { Text("Add", color = CareGreen) }
                }

                // Times
                Text("Times", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GuardianTextField(
                        value = timeInput,
                        onValueChange = { timeInput = it },
                        label = "Time e.g. 14:30",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                    OutlinedButton(onClick = {
                        val t = timeInput.trim()
                        if (t.isNotBlank() && t !in times) times = times + t
                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add time")
                    }
                }
                if (times.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    times.forEach { t ->
                        AssistChip(
                            onClick = { times = times - t },
                            label = { Text(t) },
                            trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
                        )
                    }
                }

                // Days (bitmask Mon=1 .. Sun=64)
                Text("Days", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayLabels.forEachIndexed { i, d ->
                        val bit = 1 shl i
                        val on = (days and bit) != 0
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(if (on) CareGreen else Color(0xFFF1F8F2))
                                .clickable { days = days xor bit },
                            contentAlignment = Alignment.Center,
                        ) { Text(d, color = if (on) Color.White else GuardianTextSub, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    }
                }

                error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
            }
        },
        containerColor = Color.White,
    )
}
