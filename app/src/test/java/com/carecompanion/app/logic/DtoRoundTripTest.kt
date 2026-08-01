package com.carecompanion.app.logic

import com.carecompanion.app.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JSON round-trip tests for the wire DTOs in data/model/Models.kt. Supabase (PostgREST)
 * can add new columns at any time, so decoding must ignore unknown keys instead of crashing;
 * optional fields must tolerate explicit JSON nulls too.
 */
class DtoRoundTripTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `UserDto round-trips and ignores unknown keys`() {
        val u = UserDto(id = "u1", firebaseUid = "fb1", phone = "+919876500002", role = "guardian", name = "Asha")
        val encoded = json.encodeToString(u)
        val decoded = json.decodeFromString<UserDto>(encoded)
        assertEquals(u, decoded)

        val withExtra = """{"id":"u1","firebase_uid":"fb1","phone":"+919876500002","role":"guardian","name":"Asha","brand_new_column":"future value"}"""
        val decodedExtra = json.decodeFromString<UserDto>(withExtra)
        assertEquals(u, decodedExtra)
    }

    @Test fun `ElderDto tolerates nulls in optional fields and unknown keys`() {
        val jsonStr = """
            {"id":"e1","name":"Grandma","photo_url":null,"avatar_key":null,"dob":null,"age":null,
             "address":null,"phone":null,"phone_verified":false,"verified_elder_uid":null,
             "is_active":true,"created_by":null,"sos_message":null,"some_future_field":42}
        """.trimIndent()
        val decoded = json.decodeFromString<ElderDto>(jsonStr)
        assertEquals("e1", decoded.id)
        assertEquals("Grandma", decoded.name)
        assertNull(decoded.photoUrl)
        assertNull(decoded.age)
        assertEquals(true, decoded.isActive)

        val reEncoded = json.decodeFromString<ElderDto>(json.encodeToString(decoded))
        assertEquals(decoded, reEncoded)
    }

    @Test fun `MedicineDto round-trips with defaults for absent optional fields`() {
        val minimal = """{"elder_id":"e1","name":"Paracetamol"}"""
        val decoded = json.decodeFromString<MedicineDto>(minimal)
        assertEquals("Tablet", decoded.form)
        assertEquals("", decoded.dosage)
        assertEquals(true, decoded.isActive)
        assertNull(decoded.withLiquid)
    }

    @Test fun `MedicineScheduleDto round-trips days bitmask and defaults`() {
        val s = MedicineScheduleDto(id = "s1", medicineId = "m1", elderId = "e1", label = "Morning", time = "08:00", days = 31)
        val decoded = json.decodeFromString<MedicineScheduleDto>(json.encodeToString(s))
        assertEquals(s, decoded)

        val noDays = """{"id":"s2","medicine_id":"m1","elder_id":"e1","label":"Night","time":"21:00"}"""
        assertEquals(127, json.decodeFromString<MedicineScheduleDto>(noDays).days)
    }

    @Test fun `AdherenceLogDto ignores unknown keys and tolerates null responded_at`() {
        val jsonStr = """{"elder_id":"e1","source":"schedule","source_id":"s1","occurrence_date":"2026-08-01",
            "due_at":"2026-08-01T08:00:00+05:30","status":"pending","responded_at":null,"server_computed_field":true}"""
        val decoded = json.decodeFromString<AdherenceLogDto>(jsonStr)
        assertNull(decoded.respondedAt)
        assertEquals("pending", decoded.status)
    }

    @Test fun `VitalDto round-trips and value2 (diastolic) can be absent`() {
        val v = VitalDto(elderId = "e1", type = "bp", value1 = 120.0, value2 = 80.0, context = null)
        assertEquals(v, json.decodeFromString<VitalDto>(json.encodeToString(v)))

        val sugarOnly = """{"elder_id":"e1","type":"sugar","value_1":95.0,"context":"fasting","extra_col":"x"}"""
        val decoded = json.decodeFromString<VitalDto>(sugarOnly)
        assertNull(decoded.value2)
        assertEquals("fasting", decoded.context)
    }

    @Test fun `VitalThresholdDto round-trips and tolerates missing warn bounds`() {
        val jsonStr = """{"type":"pulse","context":"default","normal_lo":60.0,"normal_hi":100.0,"unrelated":123}"""
        val decoded = json.decodeFromString<VitalThresholdDto>(jsonStr)
        assertEquals(60.0, decoded.normalLo)
        assertNull(decoded.warnLo)
        assertNull(decoded.unit)
    }

    @Test fun `ContactDto and OttShortcutDto round-trip with defaults`() {
        val c = ContactDto(elderId = "e1", name = "Son", phone = "+919000000000")
        assertEquals(c, json.decodeFromString<ContactDto>(json.encodeToString(c)))
        assertEquals(false, c.isEmergency)

        val o = OttShortcutDto(elderId = "e1", title = "YouTube", packageOrUrl = "com.google.android.youtube")
        assertEquals(o, json.decodeFromString<OttShortcutDto>(json.encodeToString(o)))
        assertEquals("preset", o.kind)
    }

    @Test fun `SosEventDto and AlertDto tolerate null optional geolocation and read fields`() {
        val sosJson = """{"elder_id":"e1","lat":null,"lng":null,"accuracy":null,"address":null,"status":"active","unexpected":1}"""
        val sos = json.decodeFromString<SosEventDto>(sosJson)
        assertNull(sos.lat)
        assertEquals("active", sos.status)

        val alertJson = """{"elder_id":"e1","kind":"sos","title":"SOS triggered","body":null,"read":false,"unused_field":true}"""
        val alert = json.decodeFromString<AlertDto>(alertJson)
        assertNull(alert.body)
        assertEquals(false, alert.read)
    }

    @Test fun `ReminderDto round-trips list-typed times field and interval fields`() {
        val r = ReminderDto(elderId = "e1", title = "Walk", times = listOf("07:00", "18:00"), repeatKind = "days", days = 65, intervalMinutes = null)
        assertEquals(r, json.decodeFromString<ReminderDto>(json.encodeToString(r)))

        val noTimes = """{"elder_id":"e1","title":"Hydrate","interval_minutes":90,"extra":"x"}"""
        val decoded = json.decodeFromString<ReminderDto>(noTimes)
        assertEquals(emptyList<String>(), decoded.times)
        assertEquals(90, decoded.intervalMinutes)
    }
}
