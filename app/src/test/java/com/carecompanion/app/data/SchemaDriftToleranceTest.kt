package com.carecompanion.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The #1 production-robustness property: the server (Supabase/PostgREST) can add new
 * columns or omit optional ones at any time (schema drift), and the app must not crash
 * or fail to parse. Every payload below carries an UNKNOWN extra field (simulating a
 * future migration) and OMITS every optional/defaulted field.
 */
class SchemaDriftToleranceTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun serve(body: String) {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
    }

    @Test
    fun `UserDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "firebase_uid": "fb-1",
              "phone": "+911234567890",
              "role": "guardian",
              "brand_new_column_from_future_migration": {"nested": ["x", 1, null]}
            }]
            """.trimIndent()
        )
        val result = api.getUsers()
        assertEquals(1, result.size)
        assertEquals("fb-1", result[0].firebaseUid)
        assertEquals("en", result[0].language) // default applied since field was omitted
    }

    @Test
    fun `ElderDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "name": "Grandma Rao",
              "totally_unforeseen_field": 42
            }]
            """.trimIndent()
        )
        val result = api.getElders()
        assertEquals(1, result.size)
        assertEquals("Grandma Rao", result[0].name)
        assertTrue(result[0].isActive) // default
        assertEquals(false, result[0].phoneVerified) // default
    }

    @Test
    fun `ContactDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "elder_id": "elder-1",
              "name": "Dr. Mehta",
              "phone": "+911111111111",
              "future_field": [1,2,3]
            }]
            """.trimIndent()
        )
        val result = api.getContacts("eq.elder-1")
        assertEquals(1, result.size)
        assertEquals(0, result[0].sort) // default
        assertEquals(false, result[0].isEmergency) // default
    }

    @Test
    fun `MedicineDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "elder_id": "elder-1",
              "name": "Metformin",
              "manufacturer_batch_code": "XZ-9912"
            }]
            """.trimIndent()
        )
        val result = api.getMedicines("eq.elder-1")
        assertEquals(1, result.size)
        assertEquals("Tablet", result[0].form) // default
        assertTrue(result[0].isActive) // default
    }

    @Test
    fun `MedicineScheduleDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "medicine_id": "med-1",
              "elder_id": "elder-1",
              "label": "Morning",
              "time": "08:00",
              "server_side_debug_info": null
            }]
            """.trimIndent()
        )
        val result = api.getSchedules("eq.elder-1")
        assertEquals(1, result.size)
        assertEquals(127, result[0].days) // default
        assertTrue(result[0].enabled) // default
    }

    @Test
    fun `ReminderDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "elder_id": "elder-1",
              "title": "Drink water",
              "extra_analytics_blob": {"a": 1}
            }]
            """.trimIndent()
        )
        val result = api.getReminders("eq.elder-1")
        assertEquals(1, result.size)
        assertEquals("daily", result[0].repeatKind) // default
        assertTrue(result[0].times.isEmpty()) // default
    }

    @Test
    fun `AdherenceLogDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "elder_id": "elder-1",
              "source": "schedule",
              "source_id": "sched-1",
              "occurrence_date": "2026-08-01",
              "due_at": "2026-08-01T09:00:00Z",
              "unexpected_audit_column": "surprise"
            }]
            """.trimIndent()
        )
        val result = api.getAdherence("eq.elder-1")
        assertEquals(1, result.size)
        assertEquals("pending", result[0].status) // default
    }

    @Test
    fun `VitalDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "elder_id": "elder-1",
              "type": "bp",
              "value_1": 120.0,
              "device_firmware_version": "1.2.3"
            }]
            """.trimIndent()
        )
        val result = api.getVitals("eq.elder-1")
        assertEquals(1, result.size)
        assertEquals(null, result[0].value2)
        assertEquals(null, result[0].context)
    }

    @Test
    fun `SosEventDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "elder_id": "elder-1",
              "internal_routing_hint": "region-3"
            }]
            """.trimIndent()
        )
        val result = api.getSos("eq.elder-1")
        assertEquals(1, result.size)
        assertEquals("active", result[0].status) // default
        assertEquals(null, result[0].lat)
    }

    @Test
    fun `AlertDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "elder_id": "elder-1",
              "kind": "missed_dose",
              "title": "Missed morning dose",
              "push_channel_debug": {"sent": true, "provider": "fcm"}
            }]
            """.trimIndent()
        )
        val result = api.getAlerts()
        assertEquals(1, result.size)
        assertEquals(false, result[0].read) // default
    }

    @Test
    fun `WheelchairPlaceDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "name": "Accessible Cab Co",
              "kind": "service",
              "geo_precision_meta": [0.1, 0.2]
            }]
            """.trimIndent()
        )
        val result = api.getWheelchairPlaces()
        assertEquals(1, result.size)
        assertEquals("Mumbai", result[0].city) // default
        assertEquals(0, result[0].sort) // default
    }

    @Test
    fun `GuardianLinkDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "elder_id": "elder-1",
              "access": "owner",
              "audit_trace_id": "trace-999"
            }]
            """.trimIndent()
        )
        val result = api.getLinks("eq.elder-1")
        assertEquals(1, result.size)
        assertEquals("active", result[0].status) // default
    }

    @Test
    fun `VitalThresholdDto tolerates extra fields and missing optionals`() = runTest {
        val api = TestSupport.buildService(server)
        serve(
            """
            [{
              "type": "bp",
              "context": "fasting",
              "future_calibration_notes": "n/a"
            }]
            """.trimIndent()
        )
        val result = api.getThresholds()
        assertEquals(1, result.size)
        assertEquals(null, result[0].normalLo)
        assertEquals(null, result[0].unit)
    }

    @Test
    fun `coerceInputValues coerces a null explicit-default field instead of throwing`() = runTest {
        val api = TestSupport.buildService(server)
        // is_active is non-nullable Boolean with a default; server sends it as null.
        serve(
            """
            [{
              "name": "Coerced Elder",
              "is_active": null
            }]
            """.trimIndent()
        )
        val result = api.getElders()
        assertEquals(1, result.size)
        assertTrue(result[0].isActive) // coerced back to the default rather than throwing
    }
}
