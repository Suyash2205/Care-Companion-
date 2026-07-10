package com.carecompanion.app.ui.elder

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.carecompanion.app.data.model.ContactDto
import com.carecompanion.app.ui.sos.ElderSosViewModel
import com.carecompanion.app.ui.theme.CareGreen
import kotlinx.coroutines.delay

private enum class ElderDest { HOME, MEDICINES, CONTACTS, SOS }

@Composable
fun ElderExperience(
    onLogout: () -> Unit,
    vm: ElderHomeViewModel = hiltViewModel(),
    sosVm: ElderSosViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    var dest by remember { mutableStateOf(ElderDest.HOME) }

    Scaffold(containerColor = Color(0xFFF4F6F4)) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (dest) {
                ElderDest.HOME -> ElderHome(ui, onOpen = { dest = it }, onLogout = onLogout)
                ElderDest.MEDICINES -> MedicineFlow(vm, onBack = { dest = ElderDest.HOME })
                ElderDest.CONTACTS -> ElderContacts(ui.contacts, onBack = { dest = ElderDest.HOME })
                ElderDest.SOS -> SosFlow(ui, sosVm, onBack = { dest = ElderDest.HOME })
            }
        }
    }
}

// ── Home ─────────────────────────────────────────────────────────────────────
@Composable
private fun ElderHome(ui: ElderUiState, onOpen: (ElderDest) -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Namaste 🙏", fontSize = 18.sp, color = Color(0xFF666666))
                Text(ui.elder?.name ?: "…", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1C))
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF1F8F2)) {
                Text("At home", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFF3F5C45), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            IconButton(onClick = onLogout) { Icon(Icons.Outlined.Logout, contentDescription = "Logout", tint = Color(0xFFB42318)) }
        }

        // Giant SOS
        Box(
            modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFF24141), Color(0xFFD62323))))
                .clickable { onOpen(ElderDest.SOS) }, contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("SOS", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 4.sp)
                    Text("Tap here for emergency", fontSize = 15.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        // Tiles
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ElderTile("Medicines", Icons.Outlined.Medication, Color(0xFFE8F5E9), Color(0xFF2E7D32),
                badge = if (ui.dueCount > 0) "${ui.dueCount} due today" else null, modifier = Modifier.weight(1f)) { onOpen(ElderDest.MEDICINES) }
            ElderTile("Contacts", Icons.Outlined.Call, Color(0xFFE3F2FD), Color(0xFF1565C0), modifier = Modifier.weight(1f)) { onOpen(ElderDest.CONTACTS) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ElderTile("Vitals", Icons.Outlined.FavoriteBorder, Color(0xFFFFEBEE), Color(0xFFC62828), modifier = Modifier.weight(1f)) { }
            ElderTile("Videos", Icons.Outlined.PlayCircle, Color(0xFFF3E5F5), Color(0xFF6A1B9A), modifier = Modifier.weight(1f)) { }
        }
        ui.error?.let { Text(it, color = Color(0xFFB42318), fontSize = 14.sp) }
    }
}

@Composable
private fun ElderTile(label: String, icon: ImageVector, bg: Color, fg: Color, modifier: Modifier = Modifier, badge: String? = null, onClick: () -> Unit) {
    Box(modifier = modifier.height(150.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).clickable(onClick = onClick).padding(16.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(34.dp))
            }
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2F2F2F))
        }
        if (badge != null) Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFF8E1), modifier = Modifier.align(Alignment.TopEnd)) {
            Text(badge, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = Color(0xFF6B4D00), fontWeight = FontWeight.Bold)
        }
    }
}

// ── Medicine step-through ────────────────────────────────────────────────────
@Composable
private fun MedicineFlow(vm: ElderHomeViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf(0) } // 0=list, 1=stepping, 2=done
    var index by remember { mutableStateOf(0) }
    val doses = ui.doses

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color(0xFF404040))
            }
            Spacer(Modifier.width(12.dp))
            Text("Take medicine", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1C))
        }

        if (doses.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No medicines due today 🎉", fontSize = 20.sp, color = Color(0xFF666666)) }
            return@Column
        }

        when (step) {
            0 -> Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Today's medicines", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    doses.forEachIndexed { i, d ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8F8F8)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}. ${d.medicine.name}", fontSize = 18.sp, modifier = Modifier.weight(1f))
                            Text(d.schedule.time, fontSize = 16.sp, color = Color(0xFF666666))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { index = 0; step = 1 }, modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C8BD9))) {
                        Text("Start taking", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            1 -> {
                val d = doses[index.coerceIn(0, doses.lastIndex)]
                AnimatedContent(targetState = index, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "dose") { idx ->
                    val dose = doses[idx.coerceIn(0, doses.lastIndex)]
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Medicine ${idx + 1} of ${doses.size}", fontSize = 18.sp, color = Color(0xFF666666))
                        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF8E1)), contentAlignment = Alignment.Center) {
                            if (dose.medicine.pillUrl != null) AsyncImage(model = dose.medicine.pillUrl, contentDescription = dose.medicine.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Outlined.Medication, contentDescription = null, tint = Color(0xFF6B4D00), modifier = Modifier.size(96.dp))
                        }
                        Text(dose.medicine.name, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                        val chips = listOfNotNull(dose.medicine.dosage.ifBlank { null }, dose.schedule.label,
                            dose.medicine.meal?.let { "${it.replaceFirstChar { c -> c.uppercase() }} meal" },
                            dose.medicine.withLiquid?.let { "With $it" })
                        Text(chips.joinToString(" · "), fontSize = 18.sp, color = Color(0xFF555555))
                        Spacer(Modifier.weight(1f))
                        Text("Did you take this medicine?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { vm.recordDose(dose, false); advance(doses, idx, { index = it }, { step = 2 }) },
                                modifier = Modifier.weight(1f).height(80.dp), shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDECEC), contentColor = Color(0xFFD32F2F))) {
                                Text("✕ Not taken", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = { vm.recordDose(dose, true); advance(doses, idx, { index = it }, { step = 2 }) },
                                modifier = Modifier.weight(1f).height(80.dp), shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                                Text("✓ Taken", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
            else -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFDCFCE7)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.SentimentVerySatisfied, contentDescription = null, tint = CareGreen, modifier = Modifier.size(76.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Great job!", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("You finished today's medicines.", fontSize = 18.sp, color = Color(0xFF555555))
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.height(56.dp)) { Text("Back to home", fontSize = 18.sp) }
            }
        }
    }
}

private fun advance(doses: List<Dose>, idx: Int, setIndex: (Int) -> Unit, finish: () -> Unit) {
    if (idx < doses.lastIndex) setIndex(idx + 1) else finish()
}

// ── Contacts ─────────────────────────────────────────────────────────────────
@Composable
private fun ElderContacts(contacts: List<ContactDto>, onBack: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color(0xFF404040))
            }
            Spacer(Modifier.width(12.dp))
            Text("Contacts", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        if (contacts.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No contacts yet", fontSize = 20.sp, color = Color(0xFF666666)) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(contacts.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { c ->
                        Box(Modifier.weight(1f).height(180.dp).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(12.dp)) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                                Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                                    if (c.photoUrl != null) AsyncImage(model = c.photoUrl, contentDescription = c.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Text(c.name.take(1).uppercase(), fontSize = 40.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}"))) },
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8F5E9))) {
                                        Icon(Icons.Outlined.Call, contentDescription = "Call", tint = CareGreen)
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(c.name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── SOS confirm → sent ───────────────────────────────────────────────────────
@Composable
private fun SosFlow(ui: ElderUiState, sosVm: ElderSosViewModel, onBack: () -> Unit) {
    val result by sosVm.result.collectAsStateWithLifecycle()
    var countdown by remember { mutableStateOf(5) }
    var fired by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val emergencyPhones = ui.contacts.filter { it.isEmergency }.map { it.phone }.ifEmpty { ui.contacts.map { it.phone } }
    val primaryPhone = emergencyPhones.firstOrNull()

    LaunchedEffect(Unit) {
        while (countdown > 0 && !fired) { delay(1000); countdown-- }
        if (!fired) { fired = true; ui.elder?.id?.let { sosVm.fire(it, ui.elder.name, emergencyPhones) } }
    }

    if (!result.sent) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Emergency Alert", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB42318))
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(180.dp).clip(CircleShape).background(Color(0xFFFDECEC)), contentAlignment = Alignment.Center) {
                Text(if (result.sending) "…" else "$countdown", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
            }
            Spacer(Modifier.height(20.dp))
            Text(if (result.sending) "Sending alert…" else "Sending alert in $countdown seconds…", fontSize = 20.sp, color = Color(0xFF555555), textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onBack, enabled = !result.sending, modifier = Modifier.fillMaxWidth().height(96.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFD32F2F))) {
                Text("CANCEL — I'm OK", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Text("Your location will be shared with your family", fontSize = 14.sp, color = Color(0xFF888888), modifier = Modifier.padding(top = 12.dp))
        }
    } else {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(140.dp).clip(CircleShape).background(Color(0xFFDCFCE7)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = CareGreen, modifier = Modifier.size(80.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Alert Sent!", fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text("Your family has been notified", fontSize = 20.sp, color = Color(0xFF555555))
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SentRow("SMS with location sent to ${result.smsCount} contact${if (result.smsCount == 1) "" else "s"}")
                    result.locationText?.let { SentRow(it) } ?: SentRow("Location shared")
                    SentRow("Alert logged with your family")
                }
            }
            Spacer(Modifier.height(20.dp))
            if (primaryPhone != null) Button(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$primaryPhone"))) },
                modifier = Modifier.fillMaxWidth().height(96.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = CareGreen)) {
                Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp))
                Text("Call ${ui.contacts.firstOrNull { it.phone == primaryPhone }?.name ?: "family"}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))) {
                Text("I am safe — go back", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun SentRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = CareGreen, modifier = Modifier.size(18.dp))
        Text(text, fontSize = 15.sp, color = Color(0xFF374151))
    }
}
