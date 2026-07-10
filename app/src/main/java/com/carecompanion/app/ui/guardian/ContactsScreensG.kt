package com.carecompanion.app.ui.guardian

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.carecompanion.app.*
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun ContactsScreenG(onBack: () -> Unit, onAdd: () -> Unit, vm: ContactsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = GuardianBg,
        floatingActionButton = { FloatingActionButton(onClick = onAdd, containerColor = CareGreen, contentColor = Color.White) { Text("+", fontSize = 24.sp) } }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Contacts", onBack)
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                ui.contacts.isEmpty() -> Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
                    Text("No contacts yet. Tap + to add one.", color = GuardianTextSub)
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ui.contacts) { c ->
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEAF6EC)), contentAlignment = Alignment.Center) {
                                    if (c.photoUrl != null) AsyncImage(model = c.photoUrl, contentDescription = c.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                    else Text(c.name.take(1).uppercase(), color = CareGreen, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(c.name, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                                        if (c.isEmergency) Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFDECEC)) {
                                            Text("SOS", Modifier.padding(horizontal = 5.dp, vertical = 1.dp), fontSize = 9.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(c.phone, fontSize = 13.sp, color = GuardianTextSub)
                                }
                                IconButton(onClick = { c.id?.let(vm::deleteContact) }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddContactScreenG(onDone: () -> Unit, onBack: () -> Unit, vm: ContactsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var emergency by remember { mutableStateOf(true) }
    var photo by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(ui.done) { if (ui.done) onDone() }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { photo = it } }
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val (n, p) = ContactImport.readNameAndPhone(ctx, uri)
        if (n != null) name = n; if (p != null) phone = p
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) pickContact.launch(null) }

    Scaffold(
        containerColor = GuardianBg,
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 12.dp) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                    GradientButton(text = if (ui.saving) "Saving…" else "Save Contact",
                        onClick = { if (name.isNotBlank() && phone.isNotBlank()) vm.addContact(name, phone, relation, emergency, false, photo) },
                        enabled = !ui.saving && name.isNotBlank() && phone.isNotBlank(), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Add Contact", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(110.dp).clip(CircleShape).background(Color(0xFFEAF6EC)).clickable { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                    if (photo != null) UriBitmapImage(photo!!, "photo", Modifier.fillMaxSize().clip(CircleShape), ContentScale.Crop)
                    else Icon(Icons.Outlined.Person, contentDescription = null, tint = CareGreen, modifier = Modifier.size(44.dp))
                }
                GuardianTextField(value = name, onValueChange = { name = it }, label = "Full Name")
                GuardianTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                GuardianTextField(value = relation, onValueChange = { relation = it }, label = "Relation (e.g. Son)")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Emergency contact (receives SOS)", Modifier.weight(1f), fontSize = 14.sp, color = GuardianTextPrimary)
                    Switch(checked = emergency, onCheckedChange = { emergency = it }, colors = SwitchDefaults.colors(checkedTrackColor = CareGreen))
                }
                OutlinedButton(onClick = {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) pickContact.launch(null)
                    else permLauncher.launch(Manifest.permission.READ_CONTACTS)
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Import from Contacts")
                }
                ui.error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
            }
        }
    }
}

@Composable
fun GuardianHeaderBar(title: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(GuardianBg).statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
        }
    }
}
