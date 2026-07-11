package com.carecompanion.app.ui.guardian

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.carecompanion.app.*
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun AddEditElderScreen(onDone: () -> Unit, onBack: () -> Unit, vm: AddEditElderViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    var otpCode by remember { mutableStateOf("") }

    LaunchedEffect(ui.saved) { if (ui.saved != null) onDone() }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> vm.setPhoto(uri) }

    Scaffold(
        containerColor = GuardianBg,
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 12.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                    GradientButton(
                        text = if (ui.saving) "Saving…" else if (ui.elderId == null) "Save Profile" else "Save Changes",
                        onClick = { vm.save() }, enabled = !ui.saving, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box(Modifier.fillMaxWidth().background(GuardianBg).statusBarsPadding().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                    Spacer(Modifier.width(8.dp))
                    Text(if (ui.elderId == null) "Add Elder Profile" else "Edit Profile",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Photo
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier.size(110.dp).clip(CircleShape).background(Color(0xFFEAF6EC))
                            .clickable { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        val uri = ui.photoUri
                        when {
                            uri != null -> UriBitmapImage(uri, "photo", Modifier.fillMaxSize().clip(CircleShape), ContentScale.Crop)
                            ui.existingPhotoUrl != null -> AsyncImage(model = ui.existingPhotoUrl, contentDescription = "photo", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            else -> Icon(Icons.Outlined.Person, contentDescription = null, tint = CareGreen, modifier = Modifier.size(48.dp))
                        }
                    }
                }
                Text("Tap photo to change", fontSize = 12.sp, color = GuardianTextSub, modifier = Modifier.align(Alignment.CenterHorizontally))

                GuardianTextField(value = ui.name, onValueChange = vm::setName, label = "Full Name")
                GuardianTextField(value = ui.age, onValueChange = vm::setAge, label = "Age", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                GuardianTextField(value = ui.address, onValueChange = vm::setAddress, label = "Address")

                // Elder's login phone — the elder logs in with this number and auto-links.
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Elder's Phone Number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                        GuardianTextField(value = ui.phone, onValueChange = vm::setPhone, label = "Phone number",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFEAF6EC)) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = CareGreen, modifier = Modifier.size(18.dp))
                                Text("The elder logs in on their own phone with THIS number to see everything you set up — they link automatically. Use a real number they can receive an OTP on.",
                                    fontSize = 12.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }

                if (ui.elderId != null) {
                    OutlinedButton(
                        onClick = { vm.deactivate(!ui.isActive) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (ui.isActive) Color(0xFFDC2626) else CareGreen)
                    ) { Text(if (ui.isActive) "Deactivate Profile" else "Reactivate Profile") }
                }

                ui.error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
