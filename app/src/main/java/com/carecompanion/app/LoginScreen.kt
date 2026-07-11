package com.carecompanion.app

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.carecompanion.app.ui.auth.AuthUiState
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
private val FieldBorder = Color(0xFFE8E8E8)
private val TextPrimary = Color(0xFF1C1C1C)
private val TextHint = Color(0xFFAAAAAA)
private val GreenLight = Color(0xFFEAF5EA)

@Composable
fun LoginScreen(vm: AuthViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    var otp by remember { mutableStateOf("") }
    // Start every visit to the login screen fresh — the ViewModel is Activity-scoped and
    // would otherwise retain the previous session's OTP step after a logout.
    LaunchedEffect(Unit) { vm.reset(); otp = "" }

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
                    Text("Login", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

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

                    AnimatedContent(
                        targetState = ui.phase,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "step"
                    ) { phase ->
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (phase == AuthUiState.Phase.OTP) {
                                StepField(otp, { if (it.length <= 6) otp = it }, "Enter OTP", KeyboardType.NumberPassword)
                                ActionButton("Login", enabled = otp.length >= 4 && !ui.loading, loading = ui.loading) {
                                    vm.verifyOtp(otp)
                                }
                            } else {
                                StepField(ui.phone, vm::setPhone, "Enter Phone Number", KeyboardType.Phone)
                                ActionButton("Get OTP", enabled = ui.phone.length >= 10 && !ui.loading, loading = ui.loading) {
                                    activity?.let { vm.requestOtp(it) }
                                }
                            }
                            ui.error?.let {
                                Text(it, color = Color(0xFFB42318), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
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
private fun StepField(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType) {
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = TextPrimary),
        cursorBrush = SolidColor(CareGreen),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(1.dp, FieldBorder, RoundedCornerShape(12.dp)).background(FieldBg)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = TextHint, fontSize = 15.sp)
            inner()
        }
    )
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CareGreen, disabledContainerColor = Color(0xFFB2DFDB)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
    ) {
        if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
