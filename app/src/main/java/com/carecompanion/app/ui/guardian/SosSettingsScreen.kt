package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
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
import com.carecompanion.app.data.model.ContactDto
import com.carecompanion.app.data.model.SosEventDto
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun SosSettingsScreen(onBack: () -> Unit, vm: SosSettingsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(containerColor = GuardianBg) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("SOS Settings", onBack)
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RecipientsCard(ui.contacts, vm::toggleEmergency)
                    TemplateCard(ui.template, vm::saveTemplate)
                    HistoryCard(ui.events, vm::resolve)
                    ui.error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GuardianTextSub, letterSpacing = 0.5.sp)
            content()
        }
    }
}

@Composable
private fun RecipientsCard(contacts: List<ContactDto>, onToggle: (ContactDto) -> Unit) {
    SettingsCard("WHO GETS ALERTED") {
        if (contacts.isEmpty()) {
            Text("No contacts yet.", fontSize = 13.sp, color = GuardianTextSub)
        } else {
            contacts.forEach { c ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFEAF6EC)), contentAlignment = Alignment.Center) {
                        Text(c.name.take(1).uppercase(), color = CareGreen, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                        Text(c.phone, fontSize = 13.sp, color = GuardianTextSub)
                    }
                    Switch(
                        checked = c.isEmergency,
                        onCheckedChange = { onToggle(c) },
                        colors = SwitchDefaults.colors(checkedTrackColor = CareGreen)
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: String, onSave: (String) -> Unit) {
    var text by remember(template) { mutableStateOf(template) }
    SettingsCard("ALERT MESSAGE") {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("SOS message") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CareGreen.copy(alpha = 0.55f),
                focusedLabelColor = CareGreen,
                unfocusedBorderColor = Color(0xFFE0E4E0),
                unfocusedLabelColor = GuardianTextSub,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            )
        )
        Text(
            "Sent to all emergency contacts when SOS fires. Use {name} for the elder's name.",
            fontSize = 12.sp,
            color = GuardianTextSub
        )
        GradientButton(
            text = "Save",
            onClick = { onSave(text.trim()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank()
        )
    }
}

@Composable
private fun HistoryCard(events: List<SosEventDto>, onResolve: (String) -> Unit) {
    SettingsCard("SOS HISTORY") {
        if (events.isEmpty()) {
            Text("No SOS events yet.", fontSize = 13.sp, color = GuardianTextSub)
        } else {
            events.forEach { e -> SosHistoryRow(e, onResolve) }
        }
    }
}

@Composable
private fun SosHistoryRow(e: SosEventDto, onResolve: (String) -> Unit) {
    val active = e.status == "active"
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    e.triggeredAt?.take(16)?.replace("T", " ") ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GuardianTextPrimary
                )
                if (e.address != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                    Text(e.address, fontSize = 12.sp, color = GuardianTextSub)
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = if (active) Color(0xFFDC2626) else CareGreen) {
                Text(e.status.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        if (active) e.id?.let { id ->
            Button(onClick = { onResolve(id) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CareGreen)) {
                Text("Mark Resolved")
            }
        }
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}
