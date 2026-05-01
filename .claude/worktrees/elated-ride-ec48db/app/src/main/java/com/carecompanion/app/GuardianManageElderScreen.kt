package com.carecompanion.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianManageElderScreen(
    profile: GuardianProfile,
    onBack: () -> Unit,
    onSwitchProfiles: () -> Unit,
    onLogout: () -> Unit,
    onOpenContacts: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        containerColor = Color(0xFFF5F6F4),
        bottomBar = {
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE8EBE9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("Back") }
                    TextButton(onClick = onSwitchProfiles) { Text("Switch profile", color = Color(0xFF2D6DCF)) }
                    TextButton(onClick = onLogout) { Text("Logout", color = Color(0xFFB42318)) }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Manage care",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1C1C1C)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(profile.bg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.photoUri != null) {
                            UriBitmapImage(
                                uri = profile.photoUri,
                                contentDescription = profile.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = profile.icon,
                                contentDescription = null,
                                tint = Color(0xFF424242),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F1F1F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Elder profile",
                            fontSize = 14.sp,
                            color = Color(0xFF6B6B6B)
                        )
                    }
                }
            }

            Text(
                text = "Quick actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E2E2E)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManageActionTile(
                        title = "Contacts",
                        subtitle = "Call list & SMS",
                        icon = Icons.Outlined.Call,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenContacts
                    )
                    ManageActionTile(
                        title = "Medicines",
                        subtitle = "Coming soon",
                        icon = Icons.Outlined.Medication,
                        modifier = Modifier.weight(1f),
                        onClick = { }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManageActionTile(
                        title = "Vitals",
                        subtitle = "Coming soon",
                        icon = Icons.Outlined.MonitorHeart,
                        modifier = Modifier.weight(1f),
                        onClick = { }
                    )
                    ManageActionTile(
                        title = "Wellness",
                        subtitle = "Tips & goals",
                        icon = Icons.Outlined.FavoriteBorder,
                        modifier = Modifier.weight(1f),
                        onClick = { }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E6)),
                border = BorderStroke(1.dp, Color(0xFFFFE0A3))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = Color(0xFFB54708)
                    )
                    Column {
                        Text(
                            text = "Alerts",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7A3E00)
                        )
                        Text(
                            text = "No urgent alerts right now.",
                            fontSize = 13.sp,
                            color = Color(0xFF8A5A2A)
                        )
                    }
                }
            }

            Button(
                onClick = onOpenContacts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CareGreen)
            ) {
                Text("Open contacts", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ManageActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8EBE9))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = CareGreen, modifier = Modifier.size(26.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1F1F1F))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF777777))
        }
    }
}
