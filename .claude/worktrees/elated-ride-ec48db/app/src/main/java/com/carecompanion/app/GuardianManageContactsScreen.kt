package com.carecompanion.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianManageContactsScreen(
    profile: GuardianProfile,
    initialContacts: List<ManagedContact>,
    onBack: () -> Unit,
    onSaveContacts: (List<ManagedContact>) -> Unit,
    onLogout: () -> Unit = {}
) {
    val contacts = remember(initialContacts) { mutableStateListOf<ManagedContact>().apply { addAll(initialContacts) } }
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        containerColor = Color(0xFFF5F6F4),
        bottomBar = {
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE8EBE9))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Button(
                        onClick = { onSaveContacts(contacts.toList()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CareGreen)
                    ) { Text("Save Contacts") }
                    TextButton(onClick = onLogout) { Text("Logout", color = Color(0xFFB42318)) }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE8EBE9)),
                    modifier = Modifier.size(38.dp).clickable(onClick = onBack)
                ) { Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color(0xFF2A2A2A))
                } }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Contacts", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(profile.name, color = Color(0xFF6B7280), fontSize = 13.sp)
                }
                TextButton(onClick = onLogout) { Text("Logout", color = Color(0xFFB42318)) }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF2EA0FF))
            ) {
                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No contacts yet. Add one.", color = Color(0xFF6B7280))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        itemsIndexed(contacts) { index, c ->
                            ContactRow(c)
                            if (index != contacts.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFFEFEFEF))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA0FF))
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add New Contact", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            AlertsMiniCard(profile.name)
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        label = { Text("Name") }
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        label = { Text("Phone") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            contacts.add(ManagedContact(newName.trim(), newPhone.trim()))
                            onSaveContacts(contacts.toList())
                        }
                        newName = ""
                        newPhone = ""
                        showAddDialog = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ContactRow(contact: ManagedContact) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF4B5563))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(contact.phone, fontSize = 14.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
private fun AlertsMiniCard(name: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8EBE9))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Alerts", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            AlertMiniRow(
                icon = Icons.Outlined.Warning,
                text = "$name missed her 8:00 am medicine",
                iconTint = Color(0xFFF5A623)
            )
            AlertMiniRow(
                icon = Icons.Outlined.Warning,
                text = "SOS Triggered 10:32 AM Khar",
                iconTint = Color(0xFFD0021B)
            )
        }
    }
}

@Composable
private fun AlertMiniRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, iconTint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF9CA3AF))
    }
}

