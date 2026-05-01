package com.carecompanion.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carecompanion.app.ui.theme.CareGreen
import java.util.UUID

private val FormOptions = listOf("Tablet", "Capsule", "Syrup", "Drops", "Injection")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuardianAddMedicineScreen(
    profile: GuardianProfile,
    onBack: () -> Unit,
    onSave: (Medicine) -> Unit
) {
    var medicineName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedForm by remember { mutableStateOf("Tablet") }
    var pillUri by remember { mutableStateOf<Uri?>(null) }
    var packetFrontUri by remember { mutableStateOf<Uri?>(null) }
    var packetBackUri by remember { mutableStateOf<Uri?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    var activePickerTarget by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        when (activePickerTarget) {
            "pill"         -> pillUri = uri
            "packetFront"  -> packetFrontUri = uri
            "packetBack"   -> packetBackUri = uri
        }
    }

    fun launchPicker(target: String) {
        activePickerTarget = target
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
            // ── Gradient header ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MedicinesGrad)
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
                        Text("Add Medicine", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("for ${profile.name}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // ── Name & dosage card ───────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Medicine Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                        GuardianTextField(
                            value = medicineName,
                            onValueChange = { medicineName = it },
                            label = "Medicine Name",
                            leadingIcon = {
                                Icon(Icons.Outlined.Medication, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        )
                        GuardianTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = "Dosage (e.g. 500mg)",
                            leadingIcon = {
                                Icon(Icons.Outlined.Scale, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                // ── Form selector ────────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Form", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormOptions.forEach { form ->
                                FilterChip(
                                    selected = selectedForm == form,
                                    onClick = { selectedForm = form },
                                    label = { Text(form, fontWeight = if (selectedForm == form) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CareGreen,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF1F8F2),
                                        labelColor = GuardianTextSub
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                // ── Image pickers ────────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = CareGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Medicine Images", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                        }
                        Text(
                            "Add photos of the pill and medicine packet to help identify the medicine.",
                            fontSize = 12.sp,
                            color = GuardianTextSub,
                            lineHeight = 18.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ImagePickerCard(
                                label = "Pill\nImage",
                                imageUri = pillUri,
                                onClick = { launchPicker("pill") },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.Medication
                            )
                            ImagePickerCard(
                                label = "Packet\nFront",
                                imageUri = packetFrontUri,
                                onClick = { launchPicker("packetFront") },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.FlipToFront
                            )
                            ImagePickerCard(
                                label = "Packet\nBack",
                                imageUri = packetBackUri,
                                onClick = { launchPicker("packetBack") },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.FlipToBack
                            )
                        }
                        Text(
                            "Tap any slot to pick an image from your gallery.",
                            fontSize = 11.sp,
                            color = GuardianTextSub
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
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
                    text = "Save Medicine",
                    onClick = {
                        if (medicineName.isNotBlank()) {
                            onSave(
                                Medicine(
                                    id = UUID.randomUUID().toString(),
                                    name = medicineName.trim(),
                                    dosage = dosage.trim(),
                                    form = selectedForm,
                                    pillImageUri = pillUri,
                                    packetFrontUri = packetFrontUri,
                                    packetBackUri = packetBackUri
                                )
                            )
                        } else {
                            snackMsg = "Please enter medicine name"
                        }
                    },
                    gradient = MedicinesGrad,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = medicineName.isNotBlank()
                )
            }
        }
    }
}
