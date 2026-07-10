package com.carecompanion.app.data.repo

import com.carecompanion.app.data.model.VitalDto
import com.carecompanion.app.data.model.VitalThresholdDto
import com.carecompanion.app.data.remote.SupabaseService
import javax.inject.Inject
import javax.inject.Singleton

enum class Severity { NORMAL, WARNING, HIGH }

@Singleton
class VitalsRepository @Inject constructor(
    private val api: SupabaseService,
) {
    @Volatile private var thresholds: List<VitalThresholdDto>? = null

    suspend fun list(elderId: String, type: String? = null): List<VitalDto> =
        api.getVitals("eq.$elderId", type?.let { "eq.$it" })

    suspend fun add(v: VitalDto): VitalDto = api.insertVital(v).first()

    suspend fun loadThresholds(): List<VitalThresholdDto> =
        thresholds ?: api.getThresholds().also { thresholds = it }

    /** Worst-of severity for a reading. BP uses systolic (v1) + diastolic (v2). */
    suspend fun severity(type: String, context: String?, v1: Double, v2: Double?): Severity {
        val t = loadThresholds()
        return when (type) {
            "bp" -> {
                val sys = severityFor(t, "bp", "systolic", v1)
                val dia = v2?.let { severityFor(t, "bp", "diastolic", it) } ?: Severity.NORMAL
                maxOf(sys, dia)
            }
            "sugar" -> severityFor(t, "sugar", context ?: "default", v1)
            else -> severityFor(t, type, "default", v1)
        }
    }

    private fun severityFor(all: List<VitalThresholdDto>, type: String, context: String, v: Double): Severity {
        val th = all.firstOrNull { it.type == type && it.context == context }
            ?: all.firstOrNull { it.type == type }
            ?: return Severity.NORMAL
        val nLo = th.normalLo; val nHi = th.normalHi
        val wLo = th.warnLo; val wHi = th.warnHi
        return when {
            nLo != null && nHi != null && v in nLo..nHi -> Severity.NORMAL
            wLo != null && wHi != null && v in wLo..wHi -> Severity.WARNING
            else -> Severity.HIGH
        }
    }
}
