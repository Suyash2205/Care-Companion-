package com.carecompanion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
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
fun GuardianWellnessSosScreen(
    profile: GuardianProfile,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    var alertDismissed by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = Color(0xFFF5F6F4),
        bottomBar = {
            GuardianBottomBar(
                activeTab = BottomTab.Alerts,
                onHome = onNavigateHome,
                onAlerts = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color(0xFF404040))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SOS Alerts Monitoring",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1C)
                    )
                    Text(
                        profile.name,
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOS Alerts", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                if (!alertDismissed) {
                    Surface(shape = CircleShape, color = Color(0xFFDC2626)) {
                        Text(
                            "1",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (!alertDismissed) {
                // Active SOS alert card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Alert info row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                .background(Color(0xFFEAF6EC), CircleShape),
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
                                        profile.icon,
                                        contentDescription = null,
                                        tint = Color(0xFF4B8B62),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111827))
                                Text("Emergency Triggered", fontSize = 13.sp, color = Color(0xFFDC2626))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("10:31 am", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF374151))
                                Text("TODAY", fontSize = 11.sp, color = Color(0xFF6B7280))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Location
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            Column {
                                Text("Current Location", fontSize = 12.sp, color = Color(0xFF6B7280))
                                Text("Mira Road, Mumbai Suburban", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF111827))
                            }
                        }

                        // Map placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(colors = listOf(Color(0xFFEAF6EC), Color(0xFFF1F8F2)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFDC2626), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.9f)) {
                                    Text(
                                        profile.name,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827)
                                    )
                                }
                            }
                            // Map grid lines for visual effect
                            Box(modifier = Modifier.fillMaxSize()) {
                                repeat(4) { i ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .offset(y = (40 * (i + 1)).dp)
                                            .background(Color.White.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CareGreen)
                            ) {
                                Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Call Now", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B8B62))
                            ) {
                                Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("View Location", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedButton(
                            onClick = { alertDismissed = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF404040))
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Dismiss", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Incident protocol
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5EDE7))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = CareGreen, modifier = Modifier.size(18.dp))
                            Text("Incident Protocol", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF3F5C45))
                        }
                        listOf(
                            "Primary emergency contacts have been notified via SMS.",
                            "Device GPS high-accuracy tracking enabled.",
                            "Alert has been logged with timestamp."
                        ).forEach { bullet ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = CareGreen, fontSize = 13.sp)
                                Text(bullet, fontSize = 13.sp, color = Color(0xFF4A4A4A))
                            }
                        }
                    }
                }

            } else {
                // No active alerts state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = CareGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text("All Clear", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text("No active SOS alerts for ${profile.name}.", fontSize = 14.sp, color = Color(0xFF666666), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }

                // Wellness summary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Wellness Summary", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF111827))
                        WellnessStat("Last seen", "At Home · 15 mins ago", Icons.Outlined.Home, Color(0xFF10B981))
                        WellnessStat("Medicine adherence", "Good · 4/5 today", Icons.Outlined.Medication, Color(0xFF3B82F6))
                        WellnessStat("Activity today", "Morning walk done", Icons.Outlined.DirectionsWalk, Color(0xFFF97316))
                        WellnessStat("Vitals check", "9:00 am · Normal", Icons.Outlined.MonitorHeart, CareGreen)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WellnessStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = Color(0xFF6B7280))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111827))
        }
    }
}
