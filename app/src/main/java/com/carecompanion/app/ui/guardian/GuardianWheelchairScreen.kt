package com.carecompanion.app.ui.guardian

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.*
import com.carecompanion.app.data.model.ContactDto
import com.carecompanion.app.data.model.WheelchairPlaceDto
import com.carecompanion.app.ui.theme.CareGreen

@Composable
fun GuardianWheelchairScreen(onBack: () -> Unit, vm: WheelchairViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(containerColor = GuardianBg) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Wheelchair & Accessibility", onBack)
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                ui.error != null -> Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
                    Text(ui.error ?: "Something went wrong", color = Color(0xFFDC2626), fontSize = 13.sp)
                }
                ui.places.isEmpty() && ui.serviceContacts.isEmpty() -> Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
                    Text("No wheelchair services or accessible places listed yet.", color = GuardianTextSub)
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (ui.serviceContacts.isNotEmpty()) {
                        item { SectionTitle("Service contacts") }
                        items(ui.serviceContacts) { c ->
                            WheelchairContactCard(
                                contact = c,
                                onCall = { phone -> ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) },
                            )
                        }
                    }
                    if (ui.places.isNotEmpty()) {
                        item { SectionTitle("Wheelchair services & accessible places") }
                        items(ui.places) { p ->
                            WheelchairPlaceCard(
                                place = p,
                                onCall = { phone -> ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) },
                                onDirections = { url -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun WheelchairPlaceCard(
    place: WheelchairPlaceDto,
    onCall: (String) -> Unit,
    onDirections: (String) -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(place.name, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary, modifier = Modifier.weight(1f))
                KindBadge(place.kind)
            }
            place.city.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 13.sp, color = GuardianTextSub) }
            val phone = place.phone
            val mapsUrl = place.mapsUrl
            if (!phone.isNullOrBlank() || !mapsUrl.isNullOrBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!phone.isNullOrBlank()) {
                        FilledTonalButton(onClick = { onCall(phone) }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEAF6EC), contentColor = CareGreen)) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Call")
                        }
                    }
                    if (!mapsUrl.isNullOrBlank()) {
                        FilledTonalButton(onClick = { onDirections(mapsUrl) }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEAF6EC), contentColor = CareGreen)) {
                            Icon(Icons.Outlined.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Directions")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelchairContactCard(
    contact: ContactDto,
    onCall: (String) -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(contact.name, fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
                    KindBadge("service")
                }
                val detail = listOfNotNull(contact.relation?.ifBlank { null }, contact.phone).joinToString(" · ")
                Text(detail, fontSize = 13.sp, color = GuardianTextSub)
            }
            if (contact.phone.isNotBlank()) {
                FilledTonalButton(onClick = { onCall(contact.phone) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEAF6EC), contentColor = CareGreen)) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Call")
                }
            }
        }
    }
}

@Composable
private fun KindBadge(kind: String) {
    val isService = kind.equals("service", ignoreCase = true)
    val bg = if (isService) Color(0xFFEDE9FE) else Color(0xFFEAF6EC)
    val fg = if (isService) Color(0xFF6D28D9) else CareGreen
    val label = if (isService) "Service" else "Place"
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}
