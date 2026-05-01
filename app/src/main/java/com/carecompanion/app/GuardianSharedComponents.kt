package com.carecompanion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

// ── Design tokens ──────────────────────────────────────────────────────────────
val GuardianBg          = Color(0xFFF0F4FF)
val GuardianPrimary     = Color(0xFF1D4ED8)
val GuardianTextPrimary = Color(0xFF0F172A)
val GuardianTextSub     = Color(0xFF64748B)

val ContactsGrad   = Brush.linearGradient(listOf(Color(0xFF0369A1), Color(0xFF38BDF8)))
val MedicinesGrad  = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF818CF8)))
val ScheduleGrad   = Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFFA78BFA)))
val SosGrad        = Brush.linearGradient(listOf(Color(0xFFB91C1C), Color(0xFFF87171)))

enum class BottomTab { Home, Alerts, Settings }

// ── Bottom navigation bar ─────────────────────────────────────────────────────
@Composable
fun GuardianBottomBar(
    activeTab: BottomTab = BottomTab.Home,
    onHome: () -> Unit = {},
    onAlerts: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    Surface(color = Color.White, shadowElevation = 16.dp, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Outlined.Home, "Home", activeTab == BottomTab.Home, onHome)
            BottomNavItem(Icons.Outlined.Notifications, "Alerts", activeTab == BottomTab.Alerts, onAlerts)
            BottomNavItem(Icons.Outlined.Settings, "Settings", activeTab == BottomTab.Settings, onSettings)
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint = if (active) GuardianPrimary else Color(0xFF94A3B8)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Color(0xFFEFF6FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            Text(
                label, fontSize = 10.sp, color = tint,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ── Gradient page header ──────────────────────────────────────────────────────
@Composable
fun GradientPageHeader(
    title: String,
    subtitle: String = "",
    gradient: Brush = Brush.linearGradient(listOf(GuardianPrimary, Color(0xFF3B82F6))),
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
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
                androidx.compose.material3.Icon(
                    Icons.Outlined.Home, // placeholder, caller replaces
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (subtitle.isNotBlank())
                    Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            }
            actions()
        }
    }
}

// ── Elder status profile card ─────────────────────────────────────────────────
@Composable
fun ElderStatusCard(
    profile: GuardianProfile,
    statusText: String = "At Home | 15 mins ago",
    isSafe: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF0288D1))))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
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
                    androidx.compose.material3.Icon(
                        imageVector = profile.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(statusText, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
            if (isSafe) {
                Surface(shape = RoundedCornerShape(20.dp), color = CareGreen) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Text("SAFE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = GuardianTextSub,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

// ── Styled input field ─────────────────────────────────────────────────────────
@Composable
fun GuardianTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GuardianPrimary,
            focusedLabelColor = GuardianPrimary,
            focusedLeadingIconColor = GuardianPrimary,
            unfocusedBorderColor = Color(0xFFCBD5E1),
            unfocusedLabelColor = GuardianTextSub,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White
        )
    )
}

// ── Gradient primary button ───────────────────────────────────────────────────
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.linearGradient(listOf(GuardianPrimary, Color(0xFF3B82F6))),
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) gradient else Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFFCBD5E1))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
    }
}

// ── Image picker card ─────────────────────────────────────────────────────────
@Composable
fun ImagePickerCard(
    label: String,
    imageUri: android.net.Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Home
) {
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (imageUri == null) Color(0xFFF1F5F9) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            UriBitmapImage(
                uri = imageUri,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // dark overlay + label
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (imageUri == null) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.25f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    icon,
                    contentDescription = null,
                    tint = if (imageUri == null) GuardianTextSub else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (imageUri == null) GuardianTextSub else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        // dashed border when empty
        if (imageUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }
}
