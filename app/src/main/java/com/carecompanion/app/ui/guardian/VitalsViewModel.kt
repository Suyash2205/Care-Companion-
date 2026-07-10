package com.carecompanion.app.ui.guardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carecompanion.app.data.model.VitalDto
import com.carecompanion.app.data.repo.Severity
import com.carecompanion.app.data.repo.VitalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class VitalsUiState(
    val loading: Boolean = true,
    val all: List<VitalDto> = emptyList(),
    val severities: Map<String, Severity> = emptyMap(),   // vital id -> severity
    val tab: String = "bp",                                // bp | sugar | temp
    val saving: Boolean = false,
    val error: String? = null,
) {
    /** Latest reading of a type (list is kept sorted newest-first). */
    fun latest(type: String): VitalDto? = all.firstOrNull { it.type == type }
}

@HiltViewModel
class VitalsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: VitalsRepository,
) : ViewModel() {

    private val elderId: String = checkNotNull(savedState["elderId"])
    private val _ui = MutableStateFlow(VitalsUiState())
    val ui: StateFlow<VitalsUiState> = _ui.asStateFlow()

    fun load() {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val all = repo.list(elderId).sortedByDescending { it.takenAt ?: "" }
                val sev = mutableMapOf<String, Severity>()
                all.forEach { v ->
                    val id = v.id ?: return@forEach
                    sev[id] = runCatching { repo.severity(v.type, v.context, v.value1, v.value2) }
                        .getOrDefault(Severity.NORMAL)
                }
                all to sev
            }.onSuccess { (all, sev) ->
                _ui.value = _ui.value.copy(loading = false, all = all, severities = sev, error = null)
            }.onFailure {
                _ui.value = _ui.value.copy(loading = false, error = it.message)
            }
        }
    }

    fun setTab(type: String) {
        _ui.value = _ui.value.copy(tab = type)
    }

    fun addReading(type: String, v1: Double, v2: Double?, context: String?, note: String?) {
        _ui.value = _ui.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                repo.add(
                    VitalDto(
                        elderId = elderId,
                        type = type,
                        value1 = v1,
                        value2 = v2,
                        context = context?.ifBlank { null },
                        note = note?.ifBlank { null },
                        takenAt = nowIso(),
                    )
                )
            }.onSuccess {
                _ui.value = _ui.value.copy(saving = false)
                load()
            }.onFailure {
                _ui.value = _ui.value.copy(saving = false, error = it.message ?: "Failed to save")
            }
        }
    }

    /** Build a simple tabular PDF of the loaded vitals in the app cache dir. */
    fun buildPdf(context: Context): File? = runCatching {
        val vitals = _ui.value.all
        val severities = _ui.value.severities
        val doc = PdfDocument()

        val pageW = 595   // A4 @ 72dpi
        val pageH = 842
        val left = 40f
        val rightEdge = pageW - 40f
        val colDate = 40f
        val colType = 210f
        val colValue = 360f
        val colStatus = 490f

        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true; isAntiAlias = true }
        val subPaint = Paint().apply { color = Color.GRAY; textSize = 11f; isAntiAlias = true }
        val headerPaint = Paint().apply { color = Color.rgb(46, 133, 64); textSize = 12f; isFakeBoldText = true; isAntiAlias = true }
        val cellPaint = Paint().apply { color = Color.DKGRAY; textSize = 12f; isAntiAlias = true }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
        var canvas: Canvas = page.canvas
        var y: Float

        fun drawColumnHeaders(startY: Float): Float {
            var yy = startY
            canvas.drawText("Date", colDate, yy, headerPaint)
            canvas.drawText("Type", colType, yy, headerPaint)
            canvas.drawText("Value", colValue, yy, headerPaint)
            canvas.drawText("Status", colStatus, yy, headerPaint)
            yy += 8f
            canvas.drawLine(left, yy, rightEdge, yy, linePaint)
            return yy + 18f
        }

        y = 62f
        canvas.drawText("Vitals Report", left, y, titlePaint)
        y += 20f
        canvas.drawText("Generated ${SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US).format(Date())}", left, y, subPaint)
        y += 28f
        y = drawColumnHeaders(y)

        vitals.forEach { v ->
            if (y > pageH - 50f) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                canvas = page.canvas
                y = drawColumnHeaders(62f)
            }
            canvas.drawText(vitalDateLabel(v.takenAt), colDate, y, cellPaint)
            canvas.drawText(vitalTypeLabel(v.type), colType, y, cellPaint)
            canvas.drawText(vitalValueText(v), colValue, y, cellPaint)
            canvas.drawText(v.id?.let { severities[it] }?.name ?: "-", colStatus, y, cellPaint)
            y += 20f
        }

        doc.finishPage(page)
        val file = File(context.cacheDir, "vitals_report.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        file
    }.getOrNull()

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}

// ── Shared display helpers (reused by the screen) ───────────────────────────────
fun vitalTypeLabel(type: String): String = when (type) {
    "bp" -> "Blood Pressure"
    "sugar" -> "Blood Sugar"
    "temp" -> "Temperature"
    "pulse" -> "Pulse"
    else -> type.replaceFirstChar { it.uppercase() }
}

fun vitalValueText(v: VitalDto): String = when (v.type) {
    "bp" -> "${v.value1.toInt()}/${v.value2?.toInt() ?: 0} mmHg"
    "sugar" -> "${v.value1.toInt()} mg/dL"
    "temp" -> "${v.value1} °F"
    "pulse" -> "${v.value1.toInt()} bpm"
    else -> v.value1.toString()
}

fun vitalDateLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
        val d = parser.parse(iso.take(19))
        if (d != null) SimpleDateFormat("MMM d, h:mm a", Locale.US).format(d) else iso.take(16)
    } catch (e: Exception) {
        iso.take(16)
    }
}
