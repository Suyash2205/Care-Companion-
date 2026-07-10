package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
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
import com.carecompanion.app.ui.theme.CareGreen

private data class PresetApp(val key: String, val title: String, val packageName: String, val color: Color)

private val catalog = listOf(
    PresetApp("youtube", "YouTube", "com.google.android.youtube", Color(0xFFEF4444)),
    PresetApp("hotstar", "Hotstar", "in.startv.hotstar", Color(0xFF2563EB)),
    PresetApp("whatsapp", "WhatsApp", "com.whatsapp", Color(0xFF25D366)),
    PresetApp("prime", "Prime", "com.amazon.avod.thirdpartyclient", Color(0xFF14B8A6)),
)

@Composable
fun GuardianOttScreen(onBack: () -> Unit, vm: OttViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    var customTitle by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }

    Scaffold(containerColor = GuardianBg) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Video & Apps", onBack)
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                else -> Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── Configured shortcuts ─────────────────────────────────────
                    Text("Configured shortcuts", fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                    if (ui.shortcuts.isEmpty()) {
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "No shortcuts yet. Add one from the catalog or paste a custom link.",
                                Modifier.padding(20.dp), fontSize = 13.sp, color = GuardianTextSub
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ui.shortcuts.forEach { s ->
                                val tint = catalog.firstOrNull { it.key == s.presetKey }?.color ?: CareGreen
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(tint), contentAlignment = Alignment.Center) {
                                            Text(s.title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(s.title, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF1F8F2)) {
                                                Text(
                                                    if (s.kind == "custom") "Custom link" else "Preset",
                                                    Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                    fontSize = 10.sp, color = CareGreen, fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        IconButton(onClick = { s.id?.let(vm::delete) }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Add from catalog ─────────────────────────────────────────
                    Text("ADD FROM CATALOG", fontWeight = FontWeight.Bold, color = GuardianTextSub, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        catalog.forEach { app ->
                            PresetTile(app, enabled = !ui.saving, modifier = Modifier.weight(1f)) {
                                vm.addPreset(app.key, app.title, app.packageName)
                            }
                        }
                    }

                    // ── Custom link ──────────────────────────────────────────────
                    Text("Custom link", fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                    GuardianTextField(value = customTitle, onValueChange = { customTitle = it }, label = "Title (e.g. Bhajans)")
                    GuardianTextField(
                        value = customUrl, onValueChange = { customUrl = it }, label = "URL (https://…)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    GradientButton(
                        text = if (ui.saving) "Saving…" else "Save custom link",
                        onClick = {
                            if (customTitle.isNotBlank() && customUrl.isNotBlank()) {
                                vm.addCustom(customTitle, customUrl)
                                customTitle = ""; customUrl = ""
                            }
                        },
                        enabled = !ui.saving && customTitle.isNotBlank() && customUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Custom links show a letter tile on the elder's home.",
                        fontSize = 12.sp, color = GuardianTextSub
                    )
                    ui.error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PresetTile(app: PresetApp, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                .background(app.color.copy(alpha = if (enabled) 1f else 0.5f))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(app.title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
        Text(app.title, fontSize = 12.sp, color = GuardianTextPrimary, fontWeight = FontWeight.Medium)
    }
}
