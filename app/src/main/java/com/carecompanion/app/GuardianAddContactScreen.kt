package com.carecompanion.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun GuardianAddContactScreen(
    profile: GuardianProfile,
    onBack: () -> Unit,
    onSave: (ManagedContact) -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { photoUri = it } }

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val (n, p) = ContactImport.readNameAndPhone(ctx, uri)
        if (n != null) name = n
        if (p != null) phone = p
        snackMsg = "Contact imported"
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPicker.launch(null)
        else snackMsg = "Contacts permission required"
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GuardianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ── Gradient header ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0369A1), Color(0xFF38BDF8))))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Add Contact", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("for ${profile.name}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                // ── Photo picker ───────────────────────────────────────────────
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                if (photoUri == null)
                                    Brush.radialGradient(listOf(Color(0xFFBAE6FD), Color(0xFF0EA5E9)))
                                else Brush.radialGradient(listOf(Color.LightGray, Color.Gray))
                            )
                            .clickable {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            UriBitmapImage(
                                uri = photoUri!!,
                                contentDescription = "Contact photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                                Text("Tap to add", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GuardianPrimary)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Camera", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Contact Photo", fontSize = 13.sp, color = GuardianTextSub)

                Spacer(Modifier.height(28.dp))

                // ── Input card ─────────────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Contact Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                        GuardianTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full Name",
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        )
                        GuardianTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "Phone Number",
                            leadingIcon = {
                                Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Import from contacts ───────────────────────────────────────
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            contactPicker.launch(null)
                        } else {
                            permLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GuardianPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GuardianPrimary)
                ) {
                    Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import from Contacts", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(32.dp))
            }

            // ── Save button ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                GradientButton(
                    text = "Save Contact",
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onSave(ManagedContact(name.trim(), phone.trim(), photoUri))
                        } else {
                            snackMsg = "Please enter name and phone"
                        }
                    },
                    gradient = ContactsGrad,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && phone.isNotBlank()
                )
            }
        }
    }
}
