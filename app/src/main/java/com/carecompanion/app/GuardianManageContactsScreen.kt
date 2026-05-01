package com.carecompanion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianManageContactsScreen(
    profile: GuardianProfile,
    initialContacts: List<ManagedContact>,
    onBack: () -> Unit,
    onSaveContacts: (List<ManagedContact>) -> Unit,
    onAddContact: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val contacts = remember(initialContacts) {
        mutableStateListOf<ManagedContact>().apply { addAll(initialContacts) }
    }
    var confirmDeleteIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GuardianBg,
        bottomBar = {
            GuardianBottomBar(
                activeTab = BottomTab.Home,
                onHome = onBack,
                onAlerts = {}
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ContactsGrad)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Contacts", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(profile.name, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Outlined.Logout, contentDescription = "Logout", tint = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // ── Stats bar ────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip(
                        count = contacts.size.toString(),
                        label = "Contacts",
                        color = Color(0xFF0369A1),
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        count = "2",
                        label = "Alerts",
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Contact list ─────────────────────────────────────────────────
            if (contacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(52.dp))
                                Text("No contacts yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GuardianTextPrimary)
                                Text("Tap the button below to add emergency contacts.", fontSize = 13.sp, color = GuardianTextSub, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(contacts) { index, contact ->
                    ContactCard(
                        contact = contact,
                        onDelete = {
                            confirmDeleteIndex = index
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Add button ───────────────────────────────────────────────────
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    GradientButton(
                        text = "+ Add New Contact",
                        onClick = onAddContact,
                        gradient = ContactsGrad,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Alerts section ────────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
                            Text("Alerts", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                        }
                        AlertRow(
                            icon = Icons.Outlined.LocalPharmacy,
                            text = "${profile.name} missed her 8:00 am medicine",
                            iconBg = Color(0xFFFFF7ED),
                            iconTint = Color(0xFFF97316)
                        )
                        AlertRow(
                            icon = Icons.Outlined.Warning,
                            text = "SOS Triggered · 10:32 AM · Khar",
                            iconBg = Color(0xFFFEF2F2),
                            iconTint = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }

    // ── Delete confirm dialog ────────────────────────────────────────────────
    confirmDeleteIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { confirmDeleteIndex = null },
            title = { Text("Remove contact?") },
            text = { Text("Remove ${contacts.getOrNull(idx)?.name} from the list?") },
            confirmButton = {
                Button(
                    onClick = {
                        contacts.removeAt(idx)
                        onSaveContacts(contacts.toList())
                        confirmDeleteIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteIndex = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ContactCard(
    contact: ManagedContact,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF38BDF8), Color(0xFF0369A1)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (contact.photoUri != null) {
                    UriBitmapImage(
                        uri = contact.photoUri,
                        contentDescription = contact.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = GuardianTextPrimary)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = GuardianTextSub, modifier = Modifier.size(12.dp))
                    Text(contact.phone, fontSize = 13.sp, color = GuardianTextSub)
                }
            }
            Row {
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Call, contentDescription = "Call", tint = CareGreen, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun StatChip(count: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = GuardianTextSub)
        }
    }
}

@Composable
private fun AlertRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconBg: Color,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFAFAFA))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(34.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Text(text, modifier = Modifier.weight(1f), fontSize = 13.sp, color = GuardianTextPrimary)
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = GuardianTextSub, modifier = Modifier.size(18.dp))
    }
}
