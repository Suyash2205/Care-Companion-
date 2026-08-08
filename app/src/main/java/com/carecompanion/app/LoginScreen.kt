package com.carecompanion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.carecompanion.app.ui.auth.AuthViewModel
import com.carecompanion.app.ui.theme.CareGreen

private data class RoleChoice(val label: String, val value: String, val icon: ImageVector)

private val roleChoices = listOf(
    RoleChoice("Elder User", "elder", Icons.Outlined.Person),
    RoleChoice("Guardian User", "guardian", Icons.Outlined.Security),
)

private val PageBg = Color(0xFFF4F6F4)
private val CardBg = Color(0xFFFFFFFF)
private val FieldBg = Color(0xFFF8F8F8)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextSub = Color(0xFF6B6B6B)
private val GreenLight = Color(0xFFEAF5EA)
private val GoogleBlue = Color(0xFF4285F4)

@Composable
fun LoginScreen(vm: AuthViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    // Credential Manager renders a system dialog, so it needs the ACTIVITY context —
    // the application context throws at runtime.
    val activityContext = LocalContext.current
    // Start each visit fresh; the ViewModel is Activity-scoped and would otherwise keep
    // a previous session's error after a logout.
    LaunchedEffect(Unit) { vm.reset() }

    Box(modifier = Modifier.fillMaxSize().background(PageBg)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp).align(Alignment.Center),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Care\nCompanion",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = TextPrimary, lineHeight = 54.sp,
                    fontWeight = FontWeight.ExtraLight, letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Box(
                modifier = Modifier.fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.08f))
                    .clip(RoundedCornerShape(24.dp)).background(CardBg)
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("Who is using this phone?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        roleChoices.forEach { role ->
                            RoleCard(
                                role = role,
                                selected = ui.role == role.value,
                                modifier = Modifier.weight(1f),
                                onClick = { vm.setRole(role.value) }
                            )
                        }
                    }

                    GoogleSignInButton(loading = ui.loading) { vm.signInWithGoogle(activityContext) }

                    Text(
                        "You'll pick your Google account next.",
                        fontSize = 12.sp, color = TextSub,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )

                    ui.error?.let {
                        Text(it, color = Color(0xFFB42318), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleCard(role: RoleChoice, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor = if (selected) GreenLight else FieldBg
    val iconColor = if (selected) CareGreen else Color(0xFF777777)
    val borderMod = if (selected)
        Modifier.border(1.5.dp, CareGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp)) else Modifier
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).then(borderMod).background(bgColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (selected) CareGreen.copy(alpha = 0.12f) else Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(role.icon, contentDescription = role.label, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Text(role.label, fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) CareGreen else TextPrimary)
        }
    }
}

@Composable
private fun GoogleSignInButton(loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            disabledContainerColor = Color(0xFFF2F2F2),
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = CareGreen, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            GoogleGlyph()
            Spacer(Modifier.width(12.dp))
            Text("Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C4043))
        }
    }
}

/** Google's "G" drawn inline so no extra asset is needed. */
@Composable
private fun GoogleGlyph() {
    Box(
        Modifier.size(22.dp).clip(CircleShape).background(GoogleBlue),
        contentAlignment = Alignment.Center,
    ) {
        Text("G", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
