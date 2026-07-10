package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonRemove
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
import com.carecompanion.app.data.model.GuardianLinkDto
import com.carecompanion.app.data.model.UserDto
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianFamilyScreen(onBack: () -> Unit, vm: FamilyViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    var phone by remember { mutableStateOf("") }
    var access by remember { mutableStateOf("view") }

    Scaffold(containerColor = GuardianBg) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Family Members", onBack)
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                else -> Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Info banner (green tint)
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEAF6EC), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Group, contentDescription = null, tint = CareGreen)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Who can see this profile", fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary, fontSize = 14.sp)
                                Text(
                                    "View Only members can see everything but can't make changes. Can Edit members can add and update medicines, contacts and reminders.",
                                    fontSize = 12.sp, color = GuardianTextSub
                                )
                            }
                        }
                    }

                    // Members
                    if (ui.members.isEmpty()) {
                        Text("No family members yet.", color = GuardianTextSub, fontSize = 13.sp)
                    } else {
                        ui.members.forEach { (link, user) ->
                            MemberCard(link = link, user = user, onRemove = { link.guardianId?.let(vm::remove) })
                        }
                    }

                    // Invite by mobile
                    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("INVITE BY MOBILE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GuardianTextSub, letterSpacing = 0.5.sp)
                            GuardianTextField(
                                value = phone, onValueChange = { phone = it }, label = "Mobile Number",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("view" to "View Only", "edit" to "Can Edit").forEach { (v, l) ->
                                    FilterChip(
                                        selected = access == v, onClick = { access = v }, label = { Text(l) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CareGreen, selectedLabelColor = Color.White)
                                    )
                                }
                            }
                            GradientButton(
                                text = if (ui.inviting) "Sending…" else "Send Invite",
                                onClick = { if (phone.isNotBlank()) vm.invite(phone, access) },
                                enabled = !ui.inviting && phone.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ui.error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MemberCard(link: GuardianLinkDto, user: UserDto?, onRemove: () -> Unit) {
    val displayName = user?.name ?: link.invitedPhone ?: "Pending"
    val subLine = user?.phone ?: link.invitedPhone
    val isPending = link.status == "pending"
    val isOwner = link.access == "owner"

    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEAF6EC)), contentAlignment = Alignment.Center) {
                Text(displayName.take(1).uppercase(), color = CareGreen, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(displayName, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                    if (isPending) Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                        Text("Pending", Modifier.padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 9.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                    }
                }
                if (subLine != null) Text(subLine, fontSize = 13.sp, color = GuardianTextSub)
                AccessChip(link.access)
            }
            if (!isOwner) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.PersonRemove, contentDescription = "Remove", tint = Color(0xFFDC2626))
                }
            }
        }
    }
}

@Composable
private fun AccessChip(access: String) {
    val (label, bg, fg) = when (access) {
        "owner" -> Triple("Owner", CareGreen, Color.White)
        "edit" -> Triple("Can Edit", Color(0xFFEAF6EC), CareGreen)
        else -> Triple("View Only", Color(0xFFF1F5F9), GuardianTextSub)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}
