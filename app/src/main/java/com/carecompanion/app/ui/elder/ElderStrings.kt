package com.carecompanion.app.ui.elder

import androidx.compose.runtime.compositionLocalOf

enum class ElderLang(val code: String, val label: String, val native: String) {
    EN("en", "English", "English"),
    HI("hi", "Hindi", "हिन्दी"),
    MR("mr", "Marathi", "मराठी"),
    GU("gu", "Gujarati", "ગુજરાતી");

    companion object {
        fun from(code: String?) = entries.firstOrNull { it.code == code } ?: EN
    }
}

/** Elder-side settings shared through composition. */
val LocalElderLang = compositionLocalOf { ElderLang.EN }
val LocalFontScale = compositionLocalOf { 1.0f }
val LocalHighContrast = compositionLocalOf { false }

/** Translate [key] for [lang]. Falls back to English, then the key itself. */
fun tr(lang: ElderLang, key: String): String = STRINGS[key]?.get(lang) ?: STRINGS[key]?.get(ElderLang.EN) ?: key

private fun m(en: String, hi: String, mr: String, gu: String) =
    mapOf(ElderLang.EN to en, ElderLang.HI to hi, ElderLang.MR to mr, ElderLang.GU to gu)

private val STRINGS: Map<String, Map<ElderLang, String>> = mapOf(
    "namaste" to m("Namaste", "नमस्ते", "नमस्कार", "નમસ્તે"),
    "at_home" to m("At home", "घर पर", "घरी", "ઘરે"),
    "sos" to m("SOS", "SOS", "SOS", "SOS"),
    "sos_hint" to m("Tap here for emergency", "आपातकाल के लिए दबाएँ", "आपत्कालीनसाठी दाबा", "કટોકટી માટે દબાવો"),
    "medicines" to m("Medicines", "दवाइयाँ", "औषधे", "દવાઓ"),
    "contacts" to m("Contacts", "संपर्क", "संपर्क", "સંપર્કો"),
    "vitals" to m("Vitals", "वाइटल्स", "वाइटल्स", "વાઇટલ્સ"),
    "videos" to m("Videos", "वीडियो", "व्हिडिओ", "વિડિઓ"),
    "due_today" to m("%d due today", "आज %d बाकी", "आज %d बाकी", "આજે %d બાકી"),
    "settings" to m("Settings", "सेटिंग्स", "सेटिंग्ज", "સેટિંગ્સ"),
    // Invite-code entry (one-time device linking, replaces phone-number matching)
    "enter_code_title" to m("Enter your code", "अपना कोड डालें", "तुमचा कोड टाका", "તમારો કોડ દાખલ કરો"),
    "enter_code_sub" to m("Type the 6 numbers your family gave you", "परिवार ने दिए 6 अंक टाइप करें", "कुटुंबाने दिलेले 6 अंक टाइप करा", "પરિવારે આપેલા 6 અંક ટાઈપ કરો"),
    "connect_btn" to m("Connect", "जोड़ें", "जोडा", "જોડો"),
    "enter_code_help" to m("Ask your family member for the code from their app", "कोड के लिए अपने परिवार से पूछें", "कोडसाठी तुमच्या कुटुंबाला विचारा", "કોડ માટે તમારા પરિવારને પૂછો"),
    "logout" to m("Log out", "लॉग आउट", "लॉग आउट", "લૉગ આઉટ"),
    "back" to m("Back", "पीछे", "मागे", "પાછળ"),
    // medicine flow
    "take_medicine" to m("Take medicine", "दवा लें", "औषध घ्या", "દવા લો"),
    "todays_medicines" to m("Today's medicines", "आज की दवाइयाँ", "आजची औषधे", "આજની દવાઓ"),
    "start_taking" to m("Start taking", "लेना शुरू करें", "सुरू करा", "શરૂ કરો"),
    "medicine_of" to m("Medicine %1\$d of %2\$d", "दवा %1\$d / %2\$d", "औषध %1\$d / %2\$d", "દવા %1\$d / %2\$d"),
    "did_you_take" to m("Did you take this medicine?", "क्या आपने यह दवा ली?", "तुम्ही हे औषध घेतले?", "શું તમે આ દવા લીધી?"),
    "not_taken" to m("Not taken", "नहीं ली", "नाही घेतले", "નથી લીધી"),
    "taken" to m("Taken", "ली", "घेतले", "લીધી"),
    "no_medicines_today" to m("No medicines due today", "आज कोई दवा नहीं", "आज औषध नाही", "આજે દવા નથી"),
    "great_job" to m("Great job!", "बहुत अच्छा!", "छान!", "શાબાશ!"),
    "finished_medicines" to m("You finished today's medicines.", "आज की सभी दवाइयाँ पूरी हुईं।", "आजची सर्व औषधे पूर्ण झाली.", "આજની બધી દવાઓ પૂરી થઈ."),
    "back_to_home" to m("Back to home", "होम पर जाएँ", "होमवर जा", "હોમ પર જાઓ"),
    // sos
    "emergency_alert" to m("Emergency Alert", "आपातकालीन अलर्ट", "आपत्कालीन इशारा", "કટોકટી ચેતવણી"),
    "sending_in" to m("Sending alert in %d seconds…", "%d सेकंड में भेजा जाएगा…", "%d सेकंदात पाठवले जाईल…", "%d સેકંડમાં મોકલાશે…"),
    "sending" to m("Sending alert…", "भेजा जा रहा है…", "पाठवत आहे…", "મોકલી રહ્યું છે…"),
    "cancel_im_ok" to m("CANCEL — I'm OK", "रद्द करें — मैं ठीक हूँ", "रद्द करा — मी ठीक आहे", "રદ કરો — હું બરાબર છું"),
    "location_shared_note" to m("Your location will be shared with your family", "आपका स्थान परिवार को भेजा जाएगा", "तुमचे स्थान कुटुंबाला पाठवले जाईल", "તમારું સ્થાન પરિવારને મોકલાશે"),
    "alert_sent" to m("Alert Sent!", "अलर्ट भेजा गया!", "इशारा पाठवला!", "ચેતવણી મોકલાઈ!"),
    "family_notified" to m("Your family has been notified", "आपके परिवार को सूचित किया गया", "तुमच्या कुटुंबाला कळवले", "તમારા પરિવારને જાણ કરાઈ"),
    "guardians_pushed" to m("Sent to your family's phones", "परिवार के फ़ोन पर भेजा गया", "कुटुंबाच्या फोनवर पाठवले", "પરિવારના ફોન પર મોકલાયું"),
    // Manual fallback, shown only when the alert could not be delivered online.
    "send_text_to" to m("Send text to %s", "%s को मैसेज भेजें", "%s ला मेसेज पाठवा", "%s ને મેસેજ મોકલો"),
    "fallback_help" to m("Tap below — then press send in the message app", "नीचे दबाएँ — फिर मैसेज ऐप में भेजें दबाएँ", "खाली दाबा — मग मेसेज अ‍ॅपमध्ये पाठवा दाबा", "નીચે દબાવો — પછી મેસેજ એપમાં મોકલો દબાવો"),
    "location_shared" to m("Location shared", "स्थान भेजा गया", "स्थान पाठवले", "સ્થાન મોકલ્યું"),
    "alert_logged" to m("Alert logged with your family", "परिवार के पास अलर्ट दर्ज", "कुटुंबाकडे इशारा नोंदवला", "પરિવાર પાસે ચેતવણી નોંધાઈ"),
    // Shown when the alert genuinely reached no one (no SMS sent AND the server record failed).
    "alert_failed" to m("Alert NOT sent", "अलर्ट नहीं भेजा गया", "इशारा पाठवला गेला नाही", "ચેતવણી મોકલાઈ નથી"),
    "call_family_now" to m("Please call your family now", "कृपया अभी परिवार को कॉल करें", "कृपया आता कुटुंबाला कॉल करा", "કૃપા કરીને હમણાં પરિવારને કૉલ કરો"),
    "alert_not_logged" to m("Could not reach your family online", "परिवार तक ऑनलाइन नहीं पहुँच सका", "कुटुंबापर्यंत ऑनलाइन पोहोचू शकलो नाही", "પરિવાર સુધી ઓનલાઈન પહોંચી શકાયું નહીં"),
    "call_family" to m("Call %s", "%s को कॉल करें", "%s ला कॉल करा", "%s ને કૉલ કરો"),
    "im_safe" to m("I am safe — go back", "मैं सुरक्षित हूँ — वापस जाएँ", "मी सुरक्षित आहे — परत जा", "હું સુરક્ષિત છું — પાછા જાઓ"),
    "no_contacts" to m("No contacts yet", "कोई संपर्क नहीं", "संपर्क नाहीत", "કોઈ સંપર્ક નથી"),
    "call_who" to m("Call %s?", "%s को कॉल करें?", "%s ला कॉल करायचे?", "%s ને કૉલ કરવો?"),
    "call_now_btn" to m("Yes, Call", "हाँ, कॉल करें", "होय, कॉल करा", "હા, કૉલ કરો"),
    "cancel_btn" to m("No", "नहीं", "नाही", "ના"),
    "tap_to_call" to m("Tap a photo to call", "कॉल करने के लिए फ़ोटो दबाएँ", "कॉल करण्यासाठी फोटो दाबा", "કૉલ કરવા ફોટો દબાવો"),
    "no_videos" to m("No videos added yet", "अभी कोई वीडियो नहीं", "अजून व्हिडिओ नाहीत", "હજી કોઈ વિડિઓ નથી"),
    // vitals
    "add_reading" to m("Add reading", "रीडिंग जोड़ें", "रीडिंग जोडा", "રીડિંગ ઉમેરો"),
    "my_readings" to m("My readings", "मेरी रीडिंग", "माझ्या रीडिंग", "મારી રીડિંગ"),
    "blood_pressure" to m("Blood Pressure", "रक्तचाप", "रक्तदाब", "બ્લડ પ્રેશર"),
    "sugar" to m("Sugar", "शुगर", "साखर", "સુગર"),
    "temperature" to m("Temperature", "तापमान", "तापमान", "તાપમાન"),
    "pulse" to m("Pulse", "नाड़ी", "नाडी", "પલ્સ"),
    "ctx_fasting" to m("Fasting", "खाली पेट", "उपाशी", "ઉપવાસ"),
    "ctx_post_meal" to m("Post-meal", "खाने के बाद", "जेवणानंतर", "જમ્યા પછી"),
    "systolic" to m("Systolic (upper)", "सिस्टोलिक (ऊपर)", "सिस्टोलिक (वर)", "સિસ્ટોલિક (ઉપર)"),
    "diastolic" to m("Diastolic (lower)", "डायस्टोलिक (नीचे)", "डायस्टोलिक (खाली)", "ડાયાસ્ટોલિક (નીચે)"),
    "save_reading" to m("Save reading", "सहेजें", "जतन करा", "સાચવો"),
    "saved_auto_time" to m("Date & time saved automatically", "दिनांक व समय स्वतः सहेजे गए", "दिनांक व वेळ आपोआप जतन", "તારીખ અને સમય આપમેળે સચવાયા"),
    "normal" to m("Normal", "सामान्य", "सामान्य", "સામાન્ય"),
    "warning" to m("Borderline", "सीमारेखा", "सीमारेषा", "સીમારેખા"),
    "high" to m("High", "उच्च", "उच्च", "ઉચ્ચ"),
    "no_readings" to m("No readings yet", "कोई रीडिंग नहीं", "रीडिंग नाहीत", "કોઈ રીડિંગ નથી"),
    // settings
    "text_size" to m("Text size", "टेक्स्ट का आकार", "मजकुराचा आकार", "ટેક્સ્ટ કદ"),
    "language" to m("Language", "भाषा", "भाषा", "ભાષા"),
    "high_contrast" to m("High contrast", "उच्च कंट्रास्ट", "उच्च कॉन्ट्रास्ट", "ઉચ્ચ કોન્ટ્રાસ્ટ"),
    "small" to m("Small", "छोटा", "लहान", "નાનું"),
    "medium" to m("Medium", "मध्यम", "मध्यम", "મધ્યમ"),
    "large" to m("Large", "बड़ा", "मोठा", "મોટું"),
    "preview_text" to m("This is how text will look", "टेक्स्ट ऐसा दिखेगा", "मजकूर असा दिसेल", "ટેક્સ્ટ આ રીતે દેખાશે"),
    "open" to m("Open", "खोलें", "उघडा", "ખોલો"),
    "not_installed" to m("Ask your family to install this app", "परिवार से यह ऐप इंस्टॉल करने को कहें", "कुटुंबाला हे अ‍ॅप इंस्टॉल करण्यास सांगा", "પરિવારને આ એપ ઇન્સ્ટોલ કરવા કહો"),
)
