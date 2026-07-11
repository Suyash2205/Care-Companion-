package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.GuardianBg
import com.carecompanion.app.GuardianTextPrimary
import com.carecompanion.app.GuardianTextSub
import com.carecompanion.app.data.model.SosEventDto
import com.carecompanion.app.ui.sos.GuardianSosViewModel
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianSosScreen(onBack: () -> Unit, onSettings: () -> Unit = {}, vm: GuardianSosViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(containerColor = GuardianBg) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("SOS Alerts", onBack)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSettings) { Text("Settings", color = CareGreen, fontWeight = FontWeight.Bold) }
            }
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                ui.events.isEmpty() -> AllClear()
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.events) { e -> SosEventCard(e, onResolve = { e.id?.let { vm.resolve(it, "resolved") } }) }
                }
            }
        }
    }
}

@Composable
private fun AllClear() {
    Column(Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = CareGreen, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("All Clear", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
        Text("No SOS alerts.", fontSize = 14.sp, color = GuardianTextSub)
    }
}

@Composable
private fun SosEventCard(e: SosEventDto, onResolve: () -> Unit) {
    val active = e.status == "active"
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(44.dp).background(if (active) Color(0xFFFEE2E2) else Color(0xFFF3F4F6), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = if (active) Color(0xFFDC2626) else GuardianTextSub, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(if (active) "Emergency Triggered" else "SOS · ${e.status}", fontWeight = FontWeight.Bold, color = if (active) Color(0xFFDC2626) else GuardianTextPrimary)
                    Text(e.triggeredAt?.take(16)?.replace("T", " ") ?: "", fontSize = 12.sp, color = GuardianTextSub)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = if (active) Color(0xFFDC2626) else CareGreen) {
                    Text(e.status.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (e.address != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                Text(e.address, fontSize = 12.sp, color = GuardianTextPrimary)
            }
            if (active) Button(onClick = onResolve, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CareGreen)) {
                Text("Mark Resolved")
            }
        }
    }
}
