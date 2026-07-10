package com.carecompanion.app.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carecompanion.app.*
import com.carecompanion.app.data.model.AdherenceLogDto
import com.carecompanion.app.data.repo.AdherenceRepository
import com.carecompanion.app.ui.theme.CareGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val TakenColor = CareGreen
private val MissedColor = Color(0xFFDC2626)
private val SkippedColor = Color(0xFFF59E0B)
private val EmptyColor = Color(0xFFE2E8F0)

@Composable
fun GuardianAdherenceScreen(onBack: () -> Unit, vm: AdherenceViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    // Last 7 days (today-6 .. today) as (yyyy-MM-dd, short weekday label).
    val days = remember {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        (0 until 7).map {
            val entry = fmt.format(cal.time) to dayFmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            entry
        }
    }

    Scaffold(containerColor = GuardianBg) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            GuardianHeaderBar("Adherence", onBack)
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CareGreen) }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { SummaryCard(ui.stats) }
                    item { WeekStrip(days, ui.weekLogs) }
                    item {
                        Text(
                            "BY MEDICINE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GuardianTextSub,
                            letterSpacing = 1.sp
                        )
                    }
                    if (ui.perMedicine.isEmpty()) {
                        item { Text("No medicines yet.", color = GuardianTextSub, fontSize = 13.sp) }
                    } else {
                        items(ui.perMedicine) { row -> MedicineRow(row) }
                    }
                    ui.error?.let { err -> item { Text(err, color = MissedColor, fontSize = 13.sp) } }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(stats: AdherenceRepository.Stats) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                Modifier.size(128.dp).clip(CircleShape)
                    .background(Color(0xFFF1F8F2))
                    .border(10.dp, CareGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.adherencePct}%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
                    Text("adherence", fontSize = 11.sp, color = GuardianTextSub)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MiniStat("Taken", stats.taken, TakenColor)
                MiniStat("Missed", stats.missed, MissedColor)
                MiniStat("Skipped", stats.skipped, SkippedColor)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Text("$value", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GuardianTextPrimary)
        }
        Text(label, fontSize = 12.sp, color = GuardianTextSub)
    }
}

@Composable
private fun WeekStrip(days: List<Pair<String, String>>, logs: List<AdherenceLogDto>) {
    val byDate = logs.groupBy { it.occurrenceDate }
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("LAST 7 DAYS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GuardianTextSub, letterSpacing = 1.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { (date, label) ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(dayColor(byDate[date].orEmpty()))
                        )
                        Text(label, fontSize = 11.sp, color = GuardianTextSub)
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineRow(row: MedAdherence) {
    val dot = when {
        row.total == 0 -> EmptyColor
        row.taken == row.total -> TakenColor
        row.taken == 0 -> MissedColor
        else -> SkippedColor
    }
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            Text(row.medicine.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = GuardianTextPrimary)
            Text("${row.taken}/${row.total} doses", fontSize = 13.sp, color = GuardianTextSub)
        }
    }
}

private fun dayColor(logs: List<AdherenceLogDto>): Color = when {
    logs.isEmpty() -> EmptyColor
    logs.any { it.status == "missed" } -> MissedColor
    logs.any { it.status == "skipped" } -> SkippedColor
    logs.any { it.status == "taken" } -> TakenColor
    else -> EmptyColor
}
