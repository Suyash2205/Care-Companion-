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
val GuardianBg          = Color(0xFFF4F6F4)
val GuardianPrimary     = CareGreen
val GuardianTextPrimary = Color(0xFF1C1C1C)
val GuardianTextSub     = Color(0xFF666666)

val ContactsGrad   = Brush.linearGradient(listOf(Color(0xFF4B8B62), CareGreen))
val MedicinesGrad  = Brush.linearGradient(listOf(Color(0xFF558F6A), Color(0xFF3F7E58)))
val ScheduleGrad   = Brush.linearGradient(listOf(Color(0xFF5A9670), Color(0xFF4B8B62)))
val SosGrad        = Brush.linearGradient(listOf(Color(0xFFF24141), Color(0xFFD62323)))

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
    val tint = if (active) GuardianPrimary else Color(0xFF8A8A8A)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) CareGreen.copy(alpha = 0.10f) else Color.Transparent)
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
    gradient: Brush = Brush.linearGradient(listOf(Color(0xFF4B8B62), GuardianPrimary)),
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
            .background(Brush.linearGradient(listOf(Color(0xFF5A9670), Color(0xFF4B8B62))))
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
            focusedBorderColor = CareGreen.copy(alpha = 0.55f),
            focusedLabelColor = GuardianPrimary,
            focusedLeadingIconColor = GuardianPrimary,
            unfocusedBorderColor = Color(0xFFE0E4E0),
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
    gradient: Brush = Brush.linearGradient(listOf(Color(0xFF4B8B62), GuardianPrimary)),
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) gradient else Brush.linearGradient(listOf(Color(0xFFCDD5CD), Color(0xFFCDD5CD))))
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
            .background(if (imageUri == null) Color(0xFFF1F8F2) else Color.Transparent)
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
                        if (imageUri == null) Color(0xFFE3EEE6) else Color.White.copy(alpha = 0.25f),
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
