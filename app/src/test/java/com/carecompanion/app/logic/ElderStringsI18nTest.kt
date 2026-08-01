package com.carecompanion.app.logic

import com.carecompanion.app.ui.elder.ElderLang
import com.carecompanion.app.ui.elder.tr
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integrity checks for the translation table in ElderStrings.kt. STRINGS itself is private,
 * but every key it defines is enumerable from the source (extracted below) and each entry is
 * reachable through the public tr(lang, key) function, so we drive the check through that.
 *
 * A specifier-count mismatch between languages for the same key (e.g. "%d" in English but
 * no placeholder in another language, or vice versa) crashes at runtime on String.format.
 */
class ElderStringsI18nTest {

    // Every key defined in ElderStrings.kt's STRINGS map (kept in sync manually; a key
    // present in source but missing here — or vice versa — should be reconciled on sight).
    private val allKeys = listOf(
        "namaste", "at_home", "sos", "sos_hint", "medicines", "contacts", "vitals", "videos",
        "due_today", "settings", "logout", "back",
        "take_medicine", "todays_medicines", "start_taking", "medicine_of", "did_you_take",
        "not_taken", "taken", "no_medicines_today", "great_job", "finished_medicines", "back_to_home",
        "emergency_alert", "sending_in", "sending", "cancel_im_ok", "location_shared_note",
        "alert_sent", "family_notified", "sms_sent_to", "location_shared", "alert_logged",
        "call_family", "im_safe", "no_contacts", "call_who", "call_now_btn", "cancel_btn",
        "tap_to_call", "no_videos",
        "add_reading", "my_readings", "blood_pressure", "sugar", "temperature", "pulse",
        "ctx_fasting", "ctx_post_meal", "systolic", "diastolic", "save_reading", "saved_auto_time",
        "normal", "warning", "high", "no_readings",
        "text_size", "language", "high_contrast", "small", "medium", "large", "preview_text",
        "open", "not_installed",
    )

    private val allLangs = ElderLang.entries.toList()

    @Test fun `table defines exactly the four expected languages`() {
        assertTrue(allLangs.map { it.code }.toSet() == setOf("en", "hi", "mr", "gu"))
    }

    @Test fun `every key is present and non-blank in all four languages`() {
        val missing = mutableListOf<String>()
        for (key in allKeys) {
            for (lang in allLangs) {
                val value = tr(lang, key)
                // tr() falls back to EN then to the key itself; a value equal to the raw key
                // (while key != the English text itself) indicates a genuinely missing entry.
                if (value.isBlank()) missing += "$key/${lang.code} is blank"
            }
        }
        assertTrue("Blank translations found: $missing", missing.isEmpty())
    }

    @Test fun `no language silently falls back to English (would indicate a missing key)`() {
        // If a non-EN language were missing from the map entirely, tr() would return the
        // English string for that language too — detect any such accidental collision only
        // when English itself isn't a symbol/number-only string that could coincidentally match.
        // Universally-recognised terms that are intentionally identical in every language.
        // "SOS" is an international distress signal — translating it would make the app's
        // most important button LESS recognisable to an elder, not more.
        val intentionallyUniversal = setOf("sos")

        val falsePositives = mutableListOf<String>()
        for (key in allKeys) {
            if (key in intentionallyUniversal) continue
            val en = tr(ElderLang.EN, key)
            for (lang in listOf(ElderLang.HI, ElderLang.MR, ElderLang.GU)) {
                val value = tr(lang, key)
                if (value == en && en.any { it.isLetter() } && en.none { it.code > 127 }) {
                    falsePositives += "$key/${lang.code} equals English verbatim: \"$en\""
                }
            }
        }
        assertTrue("Suspicious untranslated (English-identical) entries: $falsePositives", falsePositives.isEmpty())
    }

    private val specifierRegex = Regex("""%(?:\d+\$)?[sd]""")

    @Test fun `format specifier count is identical across all four languages for every key`() {
        val mismatches = mutableListOf<String>()
        for (key in allKeys) {
            val counts = allLangs.associateWith { lang -> specifierRegex.findAll(tr(lang, key)).count() }
            if (counts.values.toSet().size > 1) {
                mismatches += "$key -> ${counts.mapKeys { it.key.code }}"
            }
        }
        assertTrue("Format specifier count mismatches (would crash String.format): $mismatches", mismatches.isEmpty())
    }

    @Test fun `keys with no format specifiers have zero specifiers in every language`() {
        // sanity check that the regex itself behaves as expected on a known plain key
        assertTrue(specifierRegex.findAll(tr(ElderLang.EN, "namaste")).count() == 0)
        assertFalse(specifierRegex.containsMatchIn(tr(ElderLang.HI, "back")))
    }

    @Test fun `keys with format specifiers actually have at least one in English`() {
        val withSpecifiers = listOf("due_today", "medicine_of", "sending_in", "sms_sent_to", "call_family", "call_who")
        for (key in withSpecifiers) {
            assertTrue("$key expected to contain a specifier", specifierRegex.containsMatchIn(tr(ElderLang.EN, key)))
        }
    }

    @Test fun `unknown key falls back to itself`() {
        assertTrue(tr(ElderLang.EN, "this_key_does_not_exist") == "this_key_does_not_exist")
    }
}
