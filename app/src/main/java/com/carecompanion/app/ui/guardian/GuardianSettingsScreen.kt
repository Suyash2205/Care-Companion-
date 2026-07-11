package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.*
import com.carecompanion.app.ui.theme.CareGreen

private val LogoutRed = Color(0xFFDC2626)

@Composable
fun GuardianSettingsScreen(onBack: () -> Unit, vm: GuardianSettingsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(containerColor = GuardianBg) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Settings", onBack)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ── ACCOUNT ─────────────────────────────────────────────
                SectionHeader("ACCOUNT")
                SettingsCard {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFEAF6EC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initialsOf(ui.user?.name),
                                color = CareGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                ui.user?.name?.takeIf { it.isNotBlank() } ?: "CareCompanion User",
                                fontWeight = FontWeight.SemiBold,
                                color = GuardianTextPrimary,
                                fontSize = 16.sp
                            )
                            ui.user?.phone?.takeIf { it.isNotBlank() }?.let {
                                Text(it, fontSize = 13.sp, color = GuardianTextSub)
                            }
                        }
                        ui.user?.role?.takeIf { it.isNotBlank() }?.let { role ->
                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFEAF6EC)) {
                                Text(
                                    role.replaceFirstChar { it.uppercase() },
                                    Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CareGreen
                                )
                            }
                        }
                    }
                }

                // ── NOTIFICATIONS ───────────────────────────────────────
                SectionHeader("NOTIFICATIONS")
                SettingsCard {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = CareGreen)
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Push notifications",
                                fontWeight = FontWeight.SemiBold,
                                color = GuardianTextPrimary,
                                fontSize = 15.sp
                            )
                            Text("Alerts, SOS and reminders", fontSize = 12.sp, color = GuardianTextSub)
                        }
                        Switch(
                            checked = ui.notificationsEnabled,
                            onCheckedChange = vm::setNotifications,
                            colors = SwitchDefaults.colors(checkedTrackColor = CareGreen)
                        )
                    }
                }

                // ── HELP & SUPPORT ──────────────────────────────────────
                SectionHeader("HELP & SUPPORT")
                val ctx = androidx.compose.ui.platform.LocalContext.current
                SettingsCard {
                    Column {
                        HelpRow(Icons.Outlined.Email, "Contact support", "care@example.com", onClick = {
                            runCatching {
                                ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO,
                                    android.net.Uri.parse("mailto:care@example.com?subject=Care%20Companion%20Support")))
                            }
                        })
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        HelpRow(Icons.Outlined.Info, "App version", "1.0")
                    }
                }

                // ── LOG OUT ─────────────────────────────────────────────
                SettingsCard {
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.logout() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Outlined.Logout, contentDescription = null, tint = LogoutRed)
                        Text(
                            "Log Out",
                            fontWeight = FontWeight.Bold,
                            color = LogoutRed,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun HelpRow(icon: ImageVector, title: String, value: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = GuardianTextSub, modifier = Modifier.size(20.dp))
        Text(title, Modifier.weight(1f), fontSize = 14.sp, color = GuardianTextPrimary)
        if (value != null) Text(value, fontSize = 13.sp, color = GuardianTextSub)
    }
}

private fun initialsOf(name: String?): String {
    val parts = name?.trim()?.split(" ")?.filter { it.isNotBlank() }.orEmpty()
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
