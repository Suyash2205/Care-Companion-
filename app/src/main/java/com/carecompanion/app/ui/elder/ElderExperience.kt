package com.carecompanion.app.ui.elder

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.carecompanion.app.data.model.ContactDto
import com.carecompanion.app.data.model.OttShortcutDto
import com.carecompanion.app.data.model.VitalDto
import com.carecompanion.app.data.repo.Severity
import com.carecompanion.app.ui.sos.ElderSosViewModel
import com.carecompanion.app.ui.theme.CareGreen
import kotlinx.coroutines.delay
import java.util.Locale

private enum class ElderDest { HOME, MEDICINES, CONTACTS, SOS, VITALS, VIDEOS, SETTINGS }

@Composable
fun ElderExperience(
    onLogout: () -> Unit,
    /** Screen a notification tap asked for, e.g. Notifications.DEST_MEDICINES. */
    openDest: String? = null,
    onDestConsumed: () -> Unit = {},
    vm: ElderHomeViewModel = hiltViewModel(),
    sosVm: ElderSosViewModel = hiltViewModel(),
    settingsVm: ElderSettingsViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val lang by settingsVm.lang.collectAsStateWithLifecycle()
    val fontScale by settingsVm.fontScale.collectAsStateWithLifecycle()
    val contrast by settingsVm.contrast.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    var dest by remember { mutableStateOf(ElderDest.HOME) }

    // Tapping a medicine reminder lands straight on the take-medicine flow. Consumed
    // once, so returning home and re-opening the app doesn't jump back here.
    LaunchedEffect(openDest) {
        if (openDest == com.carecompanion.app.notify.Notifications.DEST_MEDICINES) {
            dest = ElderDest.MEDICINES
            onDestConsumed()
        }
    }

    // Elder-friendly system-back: from any sub-screen, go home instead of
    // exiting the app (an accidental back-swipe should never kick an elder out).
    BackHandler(enabled = dest != ElderDest.HOME) { dest = ElderDest.HOME }

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalElderLang provides lang,
        LocalFontScale provides fontScale,
        LocalHighContrast provides contrast,
        // Combine the OS accessibility font scale with the elder's in-app step
        // so a system-wide enlargement is preserved, not discarded.
        LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * fontScale),
    ) {
        Scaffold(containerColor = Color(0xFFF4F6F4)) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                // Before anything else: this Google account isn't connected to a care
                // profile yet. Everything else is meaningless until it is.
                if (ui.needsInviteCode) {
                    InviteCodeEntry(
                        redeeming = ui.redeeming,
                        error = ui.redeemError,
                        onSubmit = { vm.redeemInviteCode(it) },
                        onClearError = { vm.clearRedeemError() },
                        onLogout = onLogout,
                    )
                    return@Box
                }
                when (dest) {
                    ElderDest.HOME -> ElderHome(ui, onOpen = { dest = it }, onLogout = onLogout)
                    ElderDest.MEDICINES -> MedicineFlow(vm, onBack = { dest = ElderDest.HOME })
                    ElderDest.CONTACTS -> ElderContacts(ui.contacts, onBack = { dest = ElderDest.HOME })
                    ElderDest.SOS -> SosFlow(ui, sosVm, onBack = { dest = ElderDest.HOME })
                    ElderDest.VITALS -> ElderVitals(onBack = { dest = ElderDest.HOME })
                    ElderDest.VIDEOS -> ElderVideos(ui.ott, onBack = { dest = ElderDest.HOME })
                    ElderDest.SETTINGS -> ElderSettings(settingsVm, onLogout = onLogout, onBack = { dest = ElderDest.HOME })
                }
            }
        }
    }
}

private fun ink(contrast: Boolean, normal: Color) = if (contrast) Color(0xFF000000) else normal

/**
 * High-contrast foreground helper. When the elder's High-contrast mode is on, muted
 * grey text/icons are pushed to pure black for maximum legibility; otherwise the
 * colour passes through unchanged. Only wrap muted greys — never accent/semantic
 * colours (greens, reds, blues), so they keep their meaning.
 */
@Composable
private fun hc(color: Color): Color = if (LocalHighContrast.current) Color(0xFF000000) else color

// ── Home ─────────────────────────────────────────────────────────────────────
@Composable
private fun ElderHome(ui: ElderUiState, onOpen: (ElderDest) -> Unit, onLogout: () -> Unit) {
    val lang = LocalElderLang.current
    val contrast = LocalHighContrast.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${tr(lang, "namaste")} 🙏", fontSize = 18.sp, color = hc(Color(0xFF666666)))
                Text(ui.elder?.name ?: "…", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ink(contrast, Color(0xFF1C1C1C)))
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF1F8F2)) {
                Text(tr(lang, "at_home"), Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFF3F5C45), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            IconButton(onClick = { onOpen(ElderDest.SETTINGS) }) { Icon(Icons.Outlined.Settings, contentDescription = tr(lang, "settings"), tint = hc(Color(0xFF555555))) }
            IconButton(onClick = onLogout) { Icon(Icons.Outlined.Logout, contentDescription = tr(lang, "logout"), tint = Color(0xFFB42318)) }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFF24141), Color(0xFFD62323))))
                .clickable { onOpen(ElderDest.SOS) }, contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(tr(lang, "sos"), fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 4.sp)
                    Text(tr(lang, "sos_hint"), fontSize = 15.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ElderTile(tr(lang, "medicines"), Icons.Outlined.Medication, Color(0xFFE8F5E9), Color(0xFF2E7D32),
                badge = if (ui.dueCount > 0) String.format(Locale.getDefault(), tr(lang, "due_today"), ui.dueCount) else null, modifier = Modifier.weight(1f)) { onOpen(ElderDest.MEDICINES) }
            ElderTile(tr(lang, "contacts"), Icons.Outlined.Call, Color(0xFFE3F2FD), Color(0xFF1565C0), modifier = Modifier.weight(1f)) { onOpen(ElderDest.CONTACTS) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ElderTile(tr(lang, "vitals"), Icons.Outlined.FavoriteBorder, Color(0xFFFFEBEE), Color(0xFFC62828), modifier = Modifier.weight(1f)) { onOpen(ElderDest.VITALS) }
            ElderTile(tr(lang, "videos"), Icons.Outlined.PlayCircle, Color(0xFFF3E5F5), Color(0xFF6A1B9A), modifier = Modifier.weight(1f)) { onOpen(ElderDest.VIDEOS) }
        }
        ui.error?.let { Text(it, color = Color(0xFFB42318), fontSize = 14.sp) }
    }
}

@Composable
private fun ElderTile(label: String, icon: ImageVector, bg: Color, fg: Color, modifier: Modifier = Modifier, badge: String? = null, onClick: () -> Unit) {
    val contrast = LocalHighContrast.current
    Box(
        modifier = modifier.height(150.dp).clip(RoundedCornerShape(20.dp)).background(Color.White)
            .then(if (contrast) Modifier.border(2.dp, Color(0xFF222222), RoundedCornerShape(20.dp)) else Modifier)
            .clickable(onClick = onClick).padding(16.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(34.dp))
            }
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = ink(contrast, Color(0xFF2F2F2F)))
        }
        if (badge != null) Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFF8E1), modifier = Modifier.align(Alignment.TopEnd)) {
            Text(badge, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = Color(0xFF6B4D00), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ElderHeader(title: String, onBack: () -> Unit) {
    val contrast = LocalHighContrast.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr(LocalElderLang.current, "back"), tint = Color(0xFF404040))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ink(contrast, Color(0xFF1C1C1C)))
    }
}

// ── Medicine step-through ────────────────────────────────────────────────────
@Composable
private fun MedicineFlow(vm: ElderHomeViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val lang = LocalElderLang.current
    var step by remember { mutableStateOf(0) }
    var index by remember { mutableStateOf(0) }
    val doses = ui.doses

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElderHeader(tr(lang, "take_medicine"), onBack)
        // Only show the "nothing due" state before the flow starts. recordDose() updates
        // the dose list optimistically, so once the elder answers the LAST dose the list
        // can empty out — without this `step == 0` guard the completion screen would be
        // replaced by "No medicines due today", losing the "Great job!" confirmation.
        if (doses.isEmpty() && step == 0) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("${tr(lang, "no_medicines_today")} 🎉", fontSize = 20.sp, color = hc(Color(0xFF666666))) }
            return@Column
        }
        when (step) {
            0 -> Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr(lang, "todays_medicines"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    doses.forEachIndexed { i, d ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8F8F8)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}. ${d.medicine.name}", fontSize = 18.sp, modifier = Modifier.weight(1f))
                            Text(d.schedule.time, fontSize = 16.sp, color = hc(Color(0xFF666666)))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { index = 0; step = 1 }, modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = CareGreen, contentColor = Color.White)) {
                        Text(tr(lang, "start_taking"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            1 -> {
                // recordDose() updates state optimistically, so answering the last dose
                // empties this list. Advance to the completion screen rather than trying
                // to render a dose that no longer exists (which also crashed on doses[-1]).
                LaunchedEffect(doses.isEmpty()) { if (doses.isEmpty()) step = 2 }
                if (doses.isEmpty()) return@Column
                AnimatedContent(targetState = index.coerceIn(0, doses.lastIndex), transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "dose") { idx ->
                val dose = doses[idx.coerceIn(0, doses.lastIndex)]
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(String.format(Locale.getDefault(), tr(lang, "medicine_of"), idx + 1, doses.size), fontSize = 18.sp, color = hc(Color(0xFF666666)))
                    Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF8E1)), contentAlignment = Alignment.Center) {
                        if (dose.medicine.pillUrl != null) AsyncImage(model = dose.medicine.pillUrl, contentDescription = dose.medicine.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Outlined.Medication, contentDescription = null, tint = Color(0xFF6B4D00), modifier = Modifier.size(96.dp))
                    }
                    Text(dose.medicine.name, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F))
                    val chips = listOfNotNull(dose.medicine.dosage.ifBlank { null }, dose.schedule.label,
                        dose.medicine.withLiquid?.let { "with $it" }).joinToString(" · ")
                    Text(chips, fontSize = 18.sp, color = hc(Color(0xFF555555)))
                    Spacer(Modifier.weight(1f))
                    Text(tr(lang, "did_you_take"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { vm.recordDose(dose, false); if (idx < doses.lastIndex) index = idx + 1 else step = 2 },
                            modifier = Modifier.weight(1f).height(80.dp), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDECEC), contentColor = Color(0xFFD32F2F))) {
                            Text("✕ ${tr(lang, "not_taken")}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { vm.recordDose(dose, true); if (idx < doses.lastIndex) index = idx + 1 else step = 2 },
                            modifier = Modifier.weight(1f).height(80.dp), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                            Text("✓ ${tr(lang, "taken")}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                Text(tr(lang, "great_job"), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(tr(lang, "finished_medicines"), fontSize = 18.sp, color = hc(Color(0xFF555555)), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.height(56.dp)) { Text(tr(lang, "back_to_home"), fontSize = 18.sp) }
            }
        }
    }
}

// ── Contacts ─────────────────────────────────────────────────────────────────
@Composable
private fun ElderContacts(contacts: List<ContactDto>, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lang = LocalElderLang.current
    // The contact the elder tapped — a confirmation is shown before any dial, so an
    // accidental tap while scrolling never actually places a call.
    var pending by remember { mutableStateOf<ContactDto?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElderHeader(tr(lang, "contacts"), onBack)
        if (contacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text(tr(lang, "no_contacts"), fontSize = 20.sp, color = hc(Color(0xFF666666))) }
        } else {
            Text(tr(lang, "tap_to_call"), fontSize = 16.sp, color = hc(Color(0xFF888888)))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(contacts.chunked(2)) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { c -> ContactTile(c, Modifier.weight(1f)) { pending = c } }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // ── Confirm-before-dial (prevents accidental mis-dials) ──
    pending?.let { c ->
        CallConfirmDialog(
            contact = c,
            onDismiss = { pending = null },
            onCall = {
                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}")))
                pending = null
            },
        )
    }
}

/** A big photo tile — the photo IS the button. The whole card taps to call. */
@Composable
private fun ContactTile(c: ContactDto, modifier: Modifier, onTap: () -> Unit) {
    Column(
        modifier = modifier.height(230.dp).clip(RoundedCornerShape(20.dp)).background(Color.White)
            .clickable(onClick = onTap).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Photo dominates the tile
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
            if (c.photoUrl != null) {
                AsyncImage(model = c.photoUrl, contentDescription = c.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                // No text fallback — a face silhouette (a non-reading elder can't use a letter)
                Icon(Icons.Outlined.Person, contentDescription = c.name, tint = Color(0xFF7BA7D0), modifier = Modifier.fillMaxSize(0.6f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Call, contentDescription = null, tint = CareGreen, modifier = Modifier.size(18.dp))
            }
            Text(c.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** Full-screen-feel confirmation: shows the face big again + "Call [Name]?" + huge Yes/No. */
@Composable
private fun CallConfirmDialog(contact: ContactDto, onDismiss: () -> Unit, onCall: () -> Unit) {
    val lang = LocalElderLang.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(Modifier.size(150.dp).clip(CircleShape).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                    if (contact.photoUrl != null) AsyncImage(model = contact.photoUrl, contentDescription = contact.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF7BA7D0), modifier = Modifier.fillMaxSize(0.6f))
                }
                Text(String.format(Locale.getDefault(), tr(lang, "call_who"), contact.name),
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Button(onClick = onCall, modifier = Modifier.fillMaxWidth().height(84.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CareGreen)) {
                    Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp))
                    Text(tr(lang, "call_now_btn"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(68.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = hc(Color(0xFF555555)))) {
                    Text(tr(lang, "cancel_btn"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Vitals ───────────────────────────────────────────────────────────────────
@Composable
private fun ElderVitals(onBack: () -> Unit, vm: ElderVitalsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val lang = LocalElderLang.current
    LaunchedEffect(Unit) { vm.load() }
    var type by remember { mutableStateOf("bp") }
    var v1 by remember { mutableStateOf("") }
    var v2 by remember { mutableStateOf("") }
    var ctxSel by remember { mutableStateOf("fasting") }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ElderHeader(tr(lang, "add_reading"), onBack)
        // type picker
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("bp" to tr(lang, "blood_pressure"), "sugar" to tr(lang, "sugar")).forEach { (t, l) -> VitalTypeCard(t, l, type == t, Modifier.weight(1f)) { type = t; v1 = ""; v2 = "" } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("temp" to tr(lang, "temperature"), "pulse" to tr(lang, "pulse")).forEach { (t, l) -> VitalTypeCard(t, l, type == t, Modifier.weight(1f)) { type = t; v1 = ""; v2 = "" } }
        }
        // fields
        if (type == "bp") Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigNumberField(v1, { v1 = it }, tr(lang, "systolic"), Modifier.weight(1f))
            BigNumberField(v2, { v2 = it }, tr(lang, "diastolic"), Modifier.weight(1f))
        } else BigNumberField(v1, { v1 = it }, tr(lang, when (type) { "sugar" -> "sugar"; "temp" -> "temperature"; else -> "pulse" }), Modifier.fillMaxWidth())
        if (type == "sugar") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("fasting" to tr(lang, "ctx_fasting"), "post_meal" to tr(lang, "ctx_post_meal")).forEach { (v, l) ->
                FilterChip(selected = ctxSel == v, onClick = { ctxSel = v }, label = { Text(l) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = hc(Color(0xFF999999)), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp)); Text(tr(lang, "saved_auto_time"), fontSize = 14.sp, color = hc(Color(0xFF888888)))
        }
        Button(onClick = {
            val a = v1.toDoubleOrNull() ?: return@Button
            vm.add(type, a, if (type == "bp") v2.toDoubleOrNull() else null, if (type == "sugar") ctxSel else null); v1 = ""; v2 = ""
        }, enabled = v1.toDoubleOrNull() != null && !ui.saving, modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = CareGreen)) {
            Text(tr(lang, "save_reading"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider()
        Text(tr(lang, "my_readings"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        if (ui.readings.isEmpty()) Text(tr(lang, "no_readings"), fontSize = 16.sp, color = hc(Color(0xFF888888)))
        ui.readings.take(10).forEach { r -> VitalRow(r, ui.severities[r.id]) }
    }
}

@Composable
private fun VitalTypeCard(type: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.height(80.dp).clip(RoundedCornerShape(14.dp))
        .background(if (selected) Color(0xFFEAF6EC) else Color.White)
        .border(if (selected) 2.5.dp else 1.dp, if (selected) CareGreen else Color(0xFFE0E4E0), RoundedCornerShape(14.dp))
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = if (selected) CareGreen else hc(Color(0xFF444444)), textAlign = TextAlign.Center)
    }
}

@Composable
private fun BigNumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    Column(modifier) {
        OutlinedTextField(value = value, onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CareGreen))
        Text(label, fontSize = 15.sp, color = hc(Color(0xFF666666)), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
private fun VitalRow(r: VitalDto, sev: Severity?) {
    val lang = LocalElderLang.current
    val value = when (r.type) { "bp" -> "${r.value1.toInt()}/${r.value2?.toInt() ?: 0}"; "temp" -> "${r.value1}°F"; else -> "${r.value1.toInt()}" }
    val (pillBg, pillFg, pillText) = when (sev) {
        Severity.HIGH -> Triple(Color(0xFFFDECEC), Color(0xFFDC2626), tr(lang, "high"))
        Severity.WARNING -> Triple(Color(0xFFFFF8E1), Color(0xFF6B4D00), tr(lang, "warning"))
        else -> Triple(Color(0xFFEAF6EC), Color(0xFF2E7D32), tr(lang, "normal"))
    }
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(r.takenAt?.take(16)?.replace("T", " ") ?: "", fontSize = 13.sp, color = hc(Color(0xFF888888)))
            }
            Surface(shape = RoundedCornerShape(20.dp), color = pillBg) {
                Text(pillText, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pillFg)
            }
        }
    }
}

// ── Videos (OTT) ─────────────────────────────────────────────────────────────
@Composable
private fun ElderVideos(ott: List<OttShortcutDto>, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lang = LocalElderLang.current
    var notInstalled by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ElderHeader(tr(lang, "videos"), onBack)
        if (notInstalled) Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF8E1)) {
            Text(tr(lang, "not_installed"), Modifier.padding(12.dp), fontSize = 16.sp, color = Color(0xFF6B4D00))
        }
        if (ott.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text(tr(lang, "no_videos"), fontSize = 18.sp, color = hc(Color(0xFF888888))) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(ott.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { s ->
                        Box(Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)
                            .clickable { if (!launchOtt(ctx, s)) notInstalled = true }.padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3E5F5)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.PlayCircle, contentDescription = s.title, tint = Color(0xFF6A1B9A), modifier = Modifier.size(40.dp))
                                }
                                Text(s.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun launchOtt(ctx: android.content.Context, s: OttShortcutDto): Boolean = try {
    val target = s.packageOrUrl
    if (s.kind == "custom" || target.startsWith("http")) {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } else {
        val intent = ctx.packageManager.getLaunchIntentForPackage(target)
        if (intent != null) { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } else false
    }
} catch (_: Exception) { false }

// ── Settings ─────────────────────────────────────────────────────────────────
@Composable
private fun ElderSettings(vm: ElderSettingsViewModel, onLogout: () -> Unit, onBack: () -> Unit) {
    val lang by vm.lang.collectAsStateWithLifecycle()
    val fontScale by vm.fontScale.collectAsStateWithLifecycle()
    val contrast by vm.contrast.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ElderHeader(tr(lang, "settings"), onBack)
        // Text size
        SettingsCard(tr(lang, "text_size")) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(1.0f to tr(lang, "small"), 1.15f to tr(lang, "medium"), 1.3f to tr(lang, "large")).forEach { (f, l) ->
                    val sel = kotlin.math.abs(fontScale - f) < 0.01f
                    Box(Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) Color(0xFFEAF6EC) else Color(0xFFF8F8F8))
                        .border(if (sel) 2.5.dp else 1.dp, if (sel) CareGreen else Color(0xFFE0E4E0), RoundedCornerShape(14.dp))
                        .clickable { vm.setFontScale(f) }, contentAlignment = Alignment.Center) {
                        // Divide out the globally-applied fontScale so each swatch shows its own
                        // absolute target size (18*f px), not f × currentScale (double-scaling).
                        Text("A", fontSize = (18 * f / fontScale).sp, fontWeight = FontWeight.Bold, color = if (sel) CareGreen else hc(Color(0xFF444444)))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${tr(lang, "preview_text")}", fontSize = 18.sp, color = hc(Color(0xFF333333)))
        }
        // Language
        SettingsCard(tr(lang, "language")) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ElderLang.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { l ->
                            val sel = lang == l
                            Box(Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) Color(0xFFEAF6EC) else Color(0xFFF8F8F8))
                                .border(if (sel) 2.5.dp else 1.dp, if (sel) CareGreen else Color(0xFFE0E4E0), RoundedCornerShape(14.dp))
                                .clickable { vm.setLang(l) }, contentAlignment = Alignment.Center) {
                                Text(l.native, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = if (sel) CareGreen else hc(Color(0xFF333333)))
                            }
                        }
                    }
                }
            }
        }
        // Contrast
        SettingsCard(tr(lang, "high_contrast")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(lang, "high_contrast"), Modifier.weight(1f), fontSize = 20.sp)
                Switch(checked = contrast, onCheckedChange = { vm.setContrast(it) }, colors = SwitchDefaults.colors(checkedTrackColor = CareGreen))
            }
        }
        // Logout
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth().clickable(onClick = onLogout)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Logout, contentDescription = null, tint = Color(0xFFB42318))
                Spacer(Modifier.width(12.dp))
                Text(tr(lang, "logout"), fontSize = 20.sp, color = Color(0xFFB42318), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1C))
            content()
        }
    }
}

// ── SOS confirm → sent ───────────────────────────────────────────────────────
@Composable
private fun SosFlow(ui: ElderUiState, sosVm: ElderSosViewModel, onBack: () -> Unit) {
    val result by sosVm.result.collectAsStateWithLifecycle()
    val lang = LocalElderLang.current
    var countdown by remember { mutableStateOf(5) }
    var fired by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    // Contacts (not just phones) — the offline fallback needs their names on the buttons.
    val emergencyContacts = ui.contacts.filter { it.isEmergency }.ifEmpty { ui.contacts }
    val primaryContact = emergencyContacts.firstOrNull()
    val primaryPhone = primaryContact?.phone

    LaunchedEffect(Unit) {
        sosVm.reset()   // clear any prior "sent" result so this SOS starts fresh (visible countdown + CANCEL)
        while (countdown > 0 && !fired) { delay(1000); countdown-- }
        if (!fired) { fired = true; ui.elder?.id?.let { sosVm.fire(it, ui.elder.name, ui.elder.sosMessage) } }
    }

    if (!result.sent) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(tr(lang, "emergency_alert"), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB42318))
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(180.dp).clip(CircleShape).background(Color(0xFFFDECEC)), contentAlignment = Alignment.Center) {
                Text(if (result.sending) "…" else "$countdown", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
            }
            Spacer(Modifier.height(20.dp))
            Text(if (result.sending) tr(lang, "sending") else String.format(Locale.getDefault(), tr(lang, "sending_in"), countdown), fontSize = 20.sp, color = hc(Color(0xFF555555)), textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onBack, enabled = !result.sending, modifier = Modifier.fillMaxWidth().height(96.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFD32F2F))) {
                Text(tr(lang, "cancel_im_ok"), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Text(tr(lang, "location_shared_note"), fontSize = 14.sp, color = hc(Color(0xFF888888)), modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
        }
    } else {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // If nothing actually reached anyone, say so plainly and tell the elder to
            // call directly — never show a reassuring green tick over a failed alert.
            val failed = result.nooneReached
            Box(
                Modifier.size(140.dp).clip(CircleShape)
                    .background(if (failed) Color(0xFFFDECEC) else Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (failed) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (failed) Color(0xFFD32F2F) else CareGreen,
                    modifier = Modifier.size(80.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                tr(lang, if (failed) "alert_failed" else "alert_sent"),
                fontSize = 38.sp, fontWeight = FontWeight.Bold,
                color = if (failed) Color(0xFFD32F2F) else Color.Unspecified,
                textAlign = TextAlign.Center,
            )
            Text(
                tr(lang, if (failed) "call_family_now" else "family_notified"),
                fontSize = 20.sp, color = hc(Color(0xFF555555)), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!failed) SentRow(tr(lang, "guardians_pushed"))
                    SentRow(result.locationText ?: tr(lang, "location_shared"))
                    SentRow(tr(lang, if (result.serverAlertFailed) "alert_not_logged" else "alert_logged"))
                }
            }
            Spacer(Modifier.height(20.dp))

            // The alert never left this phone (almost always: no internet). Offer the two
            // things that still work offline, pre-filled, one tap each. We deliberately
            // do NOT send the SMS ourselves — that needs a Play-restricted permission.
            if (failed && primaryContact != null) {
                Text(tr(lang, "fallback_help"), fontSize = 16.sp, color = hc(Color(0xFF555555)),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp))
                Button(
                    onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${primaryContact.phone}"))
                                    .putExtra("sms_body", result.fallbackMessage)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(96.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                ) {
                    Icon(Icons.Outlined.Message, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(String.format(Locale.getDefault(), tr(lang, "send_text_to"), primaryContact.name),
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
            }

            if (primaryPhone != null) Button(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$primaryPhone"))) },
                modifier = Modifier.fillMaxWidth().height(96.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = CareGreen)) {
                Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp))
                Text(String.format(Locale.getDefault(), tr(lang, "call_family"), primaryContact?.name ?: ""), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))) {
                Text(tr(lang, "im_safe"), fontSize = 18.sp)
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

// ── Invite code entry (one time, on the elder's device) ──────────────────────
/**
 * Shown when a signed-in Google account isn't connected to a care profile yet.
 * Deliberately oversized and single-purpose: this is often filled in by the guardian
 * sitting next to the elder during setup, and never seen again afterwards.
 */
@Composable
private fun InviteCodeEntry(
    redeeming: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    onClearError: () -> Unit,
    onLogout: () -> Unit,
) {
    val lang = LocalElderLang.current
    var code by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(110.dp).clip(CircleShape).background(Color(0xFFEAF6EC)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = CareGreen, modifier = Modifier.size(56.dp))
        }

        Text(
            tr(lang, "enter_code_title"),
            fontSize = 30.sp, fontWeight = FontWeight.Bold,
            color = hc(Color(0xFF1C1C1C)), textAlign = TextAlign.Center,
        )
        Text(
            tr(lang, "enter_code_sub"),
            fontSize = 18.sp, color = hc(Color(0xFF555555)), textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = code,
            onValueChange = { new ->
                if (new.length <= 6 && new.all { it.isDigit() }) { code = new; if (error != null) onClearError() }
            },
            singleLine = true,
            enabled = !redeeming,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 40.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, letterSpacing = 10.sp,
            ),
            placeholder = {
                Text("------", fontSize = 40.sp, letterSpacing = 10.sp,
                    color = Color(0xFFBBBBBB), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(96.dp),
        )

        error?.let {
            Text(it, color = Color(0xFFD32F2F), fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }

        Button(
            onClick = { onSubmit(code) },
            enabled = code.length == 6 && !redeeming,
            modifier = Modifier.fillMaxWidth().height(76.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CareGreen),
        ) {
            if (redeeming) CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            else Text(tr(lang, "connect_btn"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Text(
            tr(lang, "enter_code_help"),
            fontSize = 15.sp, color = hc(Color(0xFF888888)), textAlign = TextAlign.Center,
        )

        TextButton(onClick = onLogout) {
            Text(tr(lang, "logout"), fontSize = 16.sp, color = Color(0xFFB42318))
        }
    }
}
