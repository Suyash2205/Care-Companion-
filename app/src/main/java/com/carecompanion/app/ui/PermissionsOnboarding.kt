package com.carecompanion.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.GuardianBg
import com.carecompanion.app.GuardianTextPrimary
import com.carecompanion.app.GuardianTextSub
import com.carecompanion.app.ui.theme.CareGreen

/** One-time explainer + grant for the permissions the app relies on. [isElder] adds SOS perms. */
@Composable
fun PermissionsOnboarding(isElder: Boolean, onDone: () -> Unit) {
    val perms = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        if (isElder) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onDone() }

    Scaffold(containerColor = GuardianBg) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFEAF6EC)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = CareGreen, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("A few permissions", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
            Text("So Care Companion can keep everyone safe.", fontSize = 15.sp, color = GuardianTextSub)
            Spacer(Modifier.height(28.dp))
            PermRow(Icons.Outlined.Notifications, "Notifications", "Reminders and emergency alerts.")
            if (isElder) {
                PermRow(Icons.Outlined.LocationOn, "Location", "Shared with family only when you press SOS.")
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { if (perms.isEmpty()) onDone() else launcher.launch(perms.toTypedArray()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CareGreen),
            ) { Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onDone) { Text("Skip for now", color = GuardianTextSub) }
        }
    }
}

@Composable
private fun PermRow(icon: ImageVector, title: String, sub: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF1F8F2)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = CareGreen, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                Text(sub, fontSize = 13.sp, color = GuardianTextSub)
            }
        }
    }
}
