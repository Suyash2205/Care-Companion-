package com.carecompanion.app.logic

import com.carecompanion.app.data.model.VitalThresholdDto
import com.carecompanion.app.data.repo.Severity
import com.carecompanion.app.data.repo.VitalsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Boundary tests for VitalsRepository.severity(). Thresholds are supplied by the test
 * (they normally live in the `vital_thresholds` table, fetched over the network), using
 * clinically representative cutoffs: BP normal <140/90, warning 140-159/90-99, high >=160/100.
 */
class VitalsSeverityTest {

    private val bpSystolic = VitalThresholdDto(
        type = "bp", context = "systolic", normalLo = 90.0, normalHi = 139.0, warnLo = 140.0, warnHi = 159.0,
    )
    private val bpDiastolic = VitalThresholdDto(
        type = "bp", context = "diastolic", normalLo = 60.0, normalHi = 89.0, warnLo = 90.0, warnHi = 99.0,
    )
    private val sugarFasting = VitalThresholdDto(
        type = "sugar", context = "fasting", normalLo = 70.0, normalHi = 99.0, warnLo = 100.0, warnHi = 125.0,
    )
    private val sugarPostMeal = VitalThresholdDto(
        type = "sugar", context = "post_meal", normalLo = 70.0, normalHi = 139.0, warnLo = 140.0, warnHi = 199.0,
    )
    private val temp = VitalThresholdDto(
        type = "temp", context = "default", normalLo = 97.0, normalHi = 99.0, warnLo = 99.1, warnHi = 100.4,
    )
    private val pulse = VitalThresholdDto(
        type = "pulse", context = "default", normalLo = 60.0, normalHi = 100.0, warnLo = 50.0, warnHi = 120.0,
    )

    private fun repo(vararg t: VitalThresholdDto) =
        VitalsRepository(FakeSupabaseService(t.toList()))

    // ── BP ──────────────────────────────────────────────────────────────────

    @Test fun `bp 139 over 89 is normal (both within normal range)`() = runTest {
        val r = repo(bpSystolic, bpDiastolic)
        assertEquals(Severity.NORMAL, r.severity("bp", null, 139.0, 89.0))
    }

    @Test fun `bp 140 over 90 crosses into warning`() = runTest {
        val r = repo(bpSystolic, bpDiastolic)
        assertEquals(Severity.WARNING, r.severity("bp", null, 140.0, 90.0))
    }

    @Test fun `bp severity is worst-of systolic and diastolic`() = runTest {
        val r = repo(bpSystolic, bpDiastolic)
        // systolic normal, diastolic high -> overall HIGH
        assertEquals(Severity.HIGH, r.severity("bp", null, 120.0, 105.0))
        // systolic high, diastolic normal -> overall HIGH
        assertEquals(Severity.HIGH, r.severity("bp", null, 165.0, 80.0))
    }

    @Test fun `bp 159 over 99 is still warning, 160 over 100 is high`() = runTest {
        val r = repo(bpSystolic, bpDiastolic)
        assertEquals(Severity.WARNING, r.severity("bp", null, 159.0, 99.0))
        assertEquals(Severity.HIGH, r.severity("bp", null, 160.0, 100.0))
    }

    @Test fun `bp with null diastolic treats diastolic as normal`() = runTest {
        val r = repo(bpSystolic, bpDiastolic)
        // systolic high, diastolic absent -> overall driven by systolic alone
        assertEquals(Severity.HIGH, r.severity("bp", null, 165.0, null))
        assertEquals(Severity.NORMAL, r.severity("bp", null, 120.0, null))
    }

    // ── Sugar (context-dependent) ────────────────────────────────────────────

    @Test fun `sugar fasting boundaries`() = runTest {
        val r = repo(sugarFasting, sugarPostMeal)
        assertEquals(Severity.NORMAL, r.severity("sugar", "fasting", 99.0, null))
        assertEquals(Severity.WARNING, r.severity("sugar", "fasting", 100.0, null))
        assertEquals(Severity.WARNING, r.severity("sugar", "fasting", 125.0, null))
        assertEquals(Severity.HIGH, r.severity("sugar", "fasting", 126.0, null))
    }

    @Test fun `sugar post-meal uses a different (higher) threshold than fasting`() = runTest {
        val r = repo(sugarFasting, sugarPostMeal)
        // 135 is HIGH under fasting thresholds (warnHi=125) but NORMAL under post-meal (normalHi=139)
        assertEquals(Severity.NORMAL, r.severity("sugar", "post_meal", 135.0, null))
        assertEquals(Severity.HIGH, r.severity("sugar", "fasting", 135.0, null))
    }

    @Test fun `sugar with unknown context falls back to any threshold for that type`() = runTest {
        val r = repo(sugarFasting)
        // context "random" doesn't match any row's context, falls back to first row of type "sugar"
        assertEquals(Severity.NORMAL, r.severity("sugar", "random_unmapped_context", 90.0, null))
    }

    // ── Temperature / Pulse (single-context types) ──────────────────────────

    @Test fun `temperature boundaries`() = runTest {
        val r = repo(temp)
        assertEquals(Severity.NORMAL, r.severity("temp", null, 99.0, null))
        assertEquals(Severity.WARNING, r.severity("temp", null, 99.1, null))
        assertEquals(Severity.WARNING, r.severity("temp", null, 100.4, null))
        assertEquals(Severity.HIGH, r.severity("temp", null, 100.5, null))
    }

    @Test fun `pulse boundaries`() = runTest {
        val r = repo(pulse)
        assertEquals(Severity.NORMAL, r.severity("pulse", null, 60.0, null))
        assertEquals(Severity.NORMAL, r.severity("pulse", null, 100.0, null))
        assertEquals(Severity.WARNING, r.severity("pulse", null, 50.0, null))
        assertEquals(Severity.WARNING, r.severity("pulse", null, 120.0, null))
        assertEquals(Severity.HIGH, r.severity("pulse", null, 200.0, null))
        assertEquals(Severity.HIGH, r.severity("pulse", null, 10.0, null))
    }

    // ── Missing threshold data ───────────────────────────────────────────────

    @Test fun `unknown vital type with no threshold row at all defaults to NORMAL`() = runTest {
        val r = repo() // no thresholds loaded
        assertEquals(Severity.NORMAL, r.severity("bp", null, 250.0, 180.0))
        assertEquals(Severity.NORMAL, r.severity("sugar", "fasting", 500.0, null))
    }

    @Test fun `thresholds are cached after first load (network hit only once)`() = runTest {
        var calls = 0
        val fake = object : FakeSupabaseService(listOf(bpSystolic, bpDiastolic)) {
            override suspend fun getThresholds(): List<VitalThresholdDto> {
                calls++
                return super.getThresholds()
            }
        }
        val r = VitalsRepository(fake)
        r.severity("bp", null, 120.0, 80.0)
        r.severity("bp", null, 150.0, 95.0)
        r.loadThresholds()
        assertEquals(1, calls)
    }
}
