package com.carecompanion.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF4B8B62), CareGreen))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text("❤", fontSize = 48.sp, color = Color.White) }
            Text("Care\nCompanion", fontSize = 34.sp, fontWeight = FontWeight.ExtraLight, color = Color.White, letterSpacing = (-0.5).sp)
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
}
