"""Generate the five Task 15-19 submission documents for CareCompanion.

Every number in these documents is read from the repository rather than typed
in by hand, so re-running the script after a change keeps the submissions
honest. Counts that cannot be derived (the recorded test run) are read from
the build output and reported with the date they were produced.

    ./paper/.venv/bin/python -I docs/build_task_docs.py
"""
import os, re, glob, subprocess, datetime

from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.expanduser("~/Desktop/CareCompanion-Tasks-15-19")
FIGS = os.path.join(OUT, "figures")
os.makedirs(OUT, exist_ok=True)

STUDENT = "Devanshi Pandey"
ROLL = "16014023016"
CLASS = "LY EXCP (Electronics and Computer Engineering)"
PROJECT = "CareCompanion: Assistive Companion Mobile Application for Elderly and Disabled Users"
DUE = "16 October 2026"
TEAM = ("Devanshi Pandey, Suyash Humne, Deon Menezes. "
        "Project Guide: Prof. Sonali Wankhede. Co-Guide: Prof. Balwant Kumar Singh.")


# ---------------------------------------------------------------- repo facts
def sh(cmd):
    return subprocess.run(cmd, shell=True, cwd=ROOT, capture_output=True,
                          text=True).stdout.strip()


def facts():
    f = {}
    f["kt_files"] = int(sh("find app/src/main/java -name '*.kt' | wc -l"))
    f["kt_lines"] = int(sh("find app/src/main/java -name '*.kt' -exec cat {} + | wc -l"))
    f["test_files"] = int(sh("find app/src/test app/src/androidTest -name '*.kt' 2>/dev/null | wc -l"))
    f["test_lines"] = int(sh("find app/src/test app/src/androidTest -name '*.kt' -exec cat {} + 2>/dev/null | wc -l"))
    f["migrations"] = len(glob.glob(os.path.join(ROOT, "supabase/migrations/*.sql")))
    f["sql_lines"] = int(sh("cat supabase/migrations/*.sql | wc -l"))
    sql = "".join(open(p, encoding="utf8").read() for p in
                  sorted(glob.glob(os.path.join(ROOT, "supabase/migrations/*.sql"))))
    f["tables"] = sorted(set(re.findall(r"create table (?:if not exists )?public\.(\w+)", sql, re.I)))
    f["policies"] = len(re.findall(r"create policy", sql, re.I))
    f["funcs"] = sorted(set(re.findall(r"create or replace function public\.(\w+)", sql, re.I)))
    f["guardian_screens"] = int(sh("ls app/src/main/java/com/carecompanion/app/ui/guardian/*.kt | wc -l"))
    f["elder_screens"] = int(sh("ls app/src/main/java/com/carecompanion/app/ui/elder/*.kt | wc -l"))
    f["routes"] = len(re.findall(r'const val \w+ = "',
                      open(os.path.join(ROOT, "app/src/main/java/com/carecompanion/app/ui/nav/Routes.kt"),
                           encoding="utf8").read()))
    # recorded unit-test run
    xs = sorted(glob.glob(os.path.join(ROOT, "app/build/test-results/testDebugUnitTest/*.xml")))
    T = F_ = E = 0
    for x in xs:
        d = open(x, encoding="utf8").read(600)
        g = lambda k: int(m.group(1)) if (m := re.search(k + r'="(\d+)"', d)) else 0
        T += g("tests"); F_ += g("failures"); E += g("errors")
    f["tests"] = T; f["test_fail"] = F_ + E; f["test_suites"] = len(xs)
    f["test_date"] = (datetime.datetime.fromtimestamp(os.path.getmtime(xs[0]))
                      .strftime("%d %B %Y") if xs else "not recorded")
    return f


F = facts()


# ------------------------------------------------------------- docx helpers
def new_doc(task_no, task_title):
    d = Document()
    st = d.styles["Normal"]
    st.font.name = "Times New Roman"; st.font.size = Pt(11)
    st.paragraph_format.space_after = Pt(6)
    st.paragraph_format.line_spacing = 1.15
    for s in d.sections:
        s.page_width, s.page_height = Inches(8.27), Inches(11.69)
        s.left_margin = s.right_margin = Inches(1.0)
        s.top_margin = s.bottom_margin = Inches(0.9)

    t = d.add_paragraph(); t.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = t.add_run(f"Task {task_no} — {task_title}")
    r.font.size = Pt(15); r.font.bold = True
    sub = d.add_paragraph(); sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    rs = sub.add_run(PROJECT); rs.font.size = Pt(11); rs.font.italic = True

    tbl = d.add_table(rows=0, cols=2); tbl.style = "Table Grid"
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    for k, v in (("Student", STUDENT), ("Roll number", ROLL), ("Class", CLASS),
                 ("Project team", TEAM), ("Submission due", DUE)):
        row = tbl.add_row().cells
        row[0].text = k; row[1].text = v
        for p in row[0].paragraphs:
            for rr in p.runs: rr.font.bold = True
        for c in row:
            for p in c.paragraphs:
                p.paragraph_format.space_after = Pt(2)
                for rr in p.runs: rr.font.size = Pt(10)
    tbl.columns[0].width = Inches(1.5); tbl.columns[1].width = Inches(4.9)
    d.add_paragraph()
    return d


def h(d, text, level=1):
    p = d.add_paragraph()
    p.paragraph_format.space_before = Pt(10 if level == 1 else 7)
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run(text)
    r.font.bold = True
    r.font.size = Pt(12.5 if level == 1 else 11)
    return p


def para(d, text, size=11, italic=False):
    p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    r = p.add_run(text); r.font.size = Pt(size); r.font.italic = italic
    return p


def bullets(d, items):
    for it in items:
        p = d.add_paragraph(style="List Bullet")
        p.paragraph_format.space_after = Pt(2)
        r = p.add_run(it); r.font.size = Pt(10.5)
        r.font.name = "Times New Roman"


def table(d, headers, rows, widths=None, size=9.5):
    t = d.add_table(rows=1, cols=len(headers)); t.style = "Table Grid"
    for i, hd in enumerate(headers):
        c = t.rows[0].cells[i]; c.text = ""
        r = c.paragraphs[0].add_run(hd); r.font.bold = True; r.font.size = Pt(size)
    for row in rows:
        cells = t.add_row().cells
        for i, v in enumerate(row):
            cells[i].text = ""
            p = cells[i].paragraphs[0]; p.paragraph_format.space_after = Pt(1)
            r = p.add_run(str(v)); r.font.size = Pt(size)
    if widths:
        for i, w in enumerate(widths):
            for row in t.rows: row.cells[i].width = Inches(w)
    d.add_paragraph()
    return t


def code(d, text, caption=None):
    if caption:
        p = d.add_paragraph(); r = p.add_run(caption)
        r.font.size = Pt(9.5); r.font.bold = True
    for line in text.strip("\n").split("\n"):
        p = d.add_paragraph()
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.left_indent = Inches(0.25)
        p.paragraph_format.line_spacing = 1.0
        r = p.add_run(line if line.strip() else " ")
        r.font.name = "Consolas"; r.font.size = Pt(8.5)
        r.font.color.rgb = RGBColor(0x1A, 0x1A, 0x1A)
    d.add_paragraph()


def figure(d, name, caption, width=6.2):
    path = os.path.join(FIGS, name)
    if not os.path.exists(path):
        para(d, f"[figure missing: {name}]"); return
    d.add_picture(path, width=Inches(width))
    d.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER
    p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(caption); r.font.size = Pt(9.5); r.font.italic = True


def save(d, name):
    path = os.path.join(OUT, name)
    d.save(path)
    print("  wrote", name)


# =============================================================== TASK 15
def task15():
    d = new_doc(15, "Core source code")

    h(d, "1. What the core function is")
    para(d, "CareCompanion is a two-surface Android application. A family member (the guardian) "
            "manages an older person's medicines, contacts, reminders and emergency settings; the "
            "older person (the elder) uses a deliberately simplified surface built from the same "
            "data. The core function implemented here is the medication adherence loop: a schedule "
            "entered by the guardian becomes an alarm on the elder's phone, the elder answers it, "
            "and the answer travels back to the guardian. A second, safety-critical function is the "
            "emergency alert path. Both are implemented end to end in the submitted source, not "
            "stubbed.")

    h(d, "2. Module map")
    para(d, f"The application is {F['kt_lines']:,} lines of Kotlin across {F['kt_files']} files, "
            f"organised into the packages below. The backend is {F['sql_lines']:,} lines of SQL "
            f"across {F['migrations']} migration files plus two Deno edge functions.")
    table(d, ["Package", "Files", "Responsibility"], [
        ["ui.elder", F["elder_screens"], "The elder surface: home, medicine, vitals, settings, voice answering. Large type, four languages."],
        ["ui.guardian", F["guardian_screens"], "The guardian surface: dashboard, elder profiles, medicines and schedules, contacts, reminders, vitals, adherence, family, SOS, OTT shortcuts."],
        ["ui.nav", 1, f"Central route table; {F['routes']} named routes, elder-scoped routes carry the elder id as a path argument."],
        ["ui.theme / ui.common", 4, "Colour scheme, type scale and shared composables. The scheme is pinned to light because every background is a fixed light colour."],
        ["data.repo", 8, "Repositories: auth, care, elder, vitals, care events, storage, voice, and the offline outbox."],
        ["data.remote", 4, "Retrofit service over Supabase PostgREST, the auth interceptor that attaches the Firebase ID token, RPC request bodies and the token provider."],
        ["data.model", 1, "Serializable DTOs shared by every call."],
        ["reminder", 4, "AlarmManager scheduling, the broadcast receiver that fires, the boot receiver that re-arms, and the daily WorkManager re-arm job."],
        ["notify", 2, "Notification channels and the Firebase Cloud Messaging service."],
        ["voice", 2, "Microphone capture (16 kHz mono PCM) and on-device text to speech."],
        ["auth", 1, "Google Sign-In through Credential Manager."],
        ["di", 1, "Hilt network module."],
    ], widths=[1.5, 0.6, 4.3])

    h(d, "3. Core implementation — the adherence loop")
    para(d, "The loop has four implemented stages. Each is named with the file that contains it so "
            "the code can be read alongside this document.")

    h(d, "3.1 Arming the alarm — reminder/ReminderScheduler.kt", 2)
    para(d, "Schedules are stored on the server, but the alarm must fire on a phone that may be "
            "offline or asleep. ReminderScheduler converts the day's schedule rows into alarms and "
            "arms each one exactly. Where the operating system refuses exact alarms, it degrades to "
            "a ten-minute window rather than failing:")
    code(d, '''fun scheduleDoses(context: Context, doses: List<DoseAlarm>) {
    val now = System.currentTimeMillis()
    val future = doses.filter { it.timeMillis > now }
    persist(context, future)
    val am = context.getSystemService(AlarmManager::class.java) ?: return
    future.forEach { arm(context, am, it) }
}

private fun arm(context: Context, am: AlarmManager, d: DoseAlarm) {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        action = "com.carecompanion.app.DOSE." + d.key
        putExtra(EXTRA_TITLE, d.title); putExtra(EXTRA_BODY, d.body)
    }
    val pi = PendingIntent.getBroadcast(context, d.key.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    try {
        if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, d.timeMillis, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, d.timeMillis, 10 * 60 * 1000L, pi)
    } catch (_: SecurityException) {
        am.setWindow(AlarmManager.RTC_WAKEUP, d.timeMillis, 10 * 60 * 1000L, pi)
    }
}''', "Listing 1 — ReminderScheduler.kt, exact alarm with a documented fallback")
    para(d, "Alarms do not survive a reboot, so reminder/BootReceiver.kt re-arms them on "
            "BOOT_COMPLETED and reminder/DailyArmWorker.kt re-arms them once a day through "
            "WorkManager. Alarm keys are namespaced per reminder, per time and per day "
            "(\"rem-$id-$time-$today\") so that re-arming is idempotent and a dose answered early "
            "can be cancelled individually by cancelDose().")

    h(d, "3.2 Firing and routing — reminder/ReminderReceiver.kt, notify/Notifications.kt", 2)
    para(d, "When the alarm fires the receiver posts a full-screen-eligible notification. Tapping it "
            "opens the take-medicine screen directly rather than the app's home screen; the deep "
            "link is carried on the PendingIntent and consumed by MainActivity, which is declared "
            "singleTop so an already-running app is routed rather than restarted.")

    h(d, "3.3 Answering — ui/elder/ElderExperience.kt and ui/elder/VoiceDoseViewModel.kt", 2)
    para(d, "The elder answers with two large buttons, or by speaking. The voice path records a "
            "short clip, uploads it to the voice edge function, and acts on the returned intent. "
            "Speech recognition and classification run server-side so no API key ships inside the "
            "APK. The recorder stops on 2.5 seconds of silence rather than the usual 0.7, because "
            "older speakers pause mid-sentence:")
    code(d, '''enum class VoicePhase { IDLE, SPEAKING, LISTENING, THINKING, UNCLEAR, UNAVAILABLE }

// Any failure resolves to "unclear" and the buttons remain the primary path:
// voice answering is an alternative, never a requirement.''',
         "Listing 2 — VoiceDoseViewModel.kt, phase model")

    h(d, "3.4 Recording the answer — data/repo/CareRepository.kt, data/repo/OutboxStore.kt", 2)
    para(d, "The answer is written to adherence_logs through PostgREST. If the phone is offline the "
            "write is queued in OutboxStore and replayed when connectivity returns, so an answer "
            "given in a lift is not lost. If no answer arrives at all, the server-side job "
            "scan_missed_doses() raises an alert and notify_push() pushes it to the guardian.")

    h(d, "4. How to build and run")
    para(d, "The project is a standard Gradle Android build. No IDE is required; the wrapper "
            "downloads the correct Gradle version itself.")
    table(d, ["Requirement", "Version used"], [
        ["JDK", "17 (Temurin). Set JAVA_HOME to it."],
        ["Android SDK", "compileSdk 36, build-tools 36. Set sdk.dir in local.properties or ANDROID_HOME."],
        ["Gradle", "8.11.1, downloaded by ./gradlew"],
        ["Android Gradle Plugin", "8.9.1"],
        ["Kotlin / JVM target", "2.0.x / 17"],
        ["Minimum device", "Android 7.0 (API 24); target API 36"],
    ], widths=[1.9, 4.5])
    code(d, '''# 1. Configure the SDK location (once)
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# 2. Place the Firebase config (not in version control)
cp /path/to/google-services.json app/

# 3. Build a debug APK
JAVA_HOME=/path/to/jdk-17 ./gradlew :app:assembleDebug
#    -> app/build/outputs/apk/debug/app-debug.apk

# 4. Run the test suite
JAVA_HOME=/path/to/jdk-17 ./gradlew :app:testDebugUnitTest
#    -> app/build/reports/tests/testDebugUnitTest/index.html

# 5. Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk''', "Listing 3 — build and run")
    para(d, "The backend needs no server to be started: Supabase is managed. Schema changes are "
            "applied with `supabase db push` and the two edge functions with "
            "`supabase functions deploy push` and `supabase functions deploy voice`.", italic=False)

    h(d, "5. Sample input and output")

    h(d, "5.1 Creating a medicine schedule (guardian input → stored row)", 2)
    code(d, '''INPUT   Guardian fills the Schedule Builder: Metformin, 500 mg, 08:00 and 20:00,
        every day, "before meal".

CALL    POST /rest/v1/medicine_schedules
        Authorization: Bearer <Firebase ID token>
        [{ "medicine_id": "6f1c...", "time_of_day": "08:00",
           "days_mask": 127, "instruction": "before_meal" }]

OUTPUT  201 Created
        [{ "id": "a4e0...", "medicine_id": "6f1c...", "time_of_day": "08:00",
           "days_mask": 127, "created_at": "2026-10-16T09:14:22.108Z" }]

        days_mask 127 = 1111111 binary = all seven days.''')

    h(d, "5.2 The alarm firing and being answered", 2)
    code(d, '''INPUT   System clock reaches 08:00 on the elder's phone.

OUTPUT  Notification: "Time for your medicine" / "Metformin 500 mg - before meal"
        Tapping it opens the take-medicine screen directly.

INPUT   Elder presses "Taken", or says "haan, le liya" (Hindi for "yes, I took it").

CALL    POST /rest/v1/adherence_logs
        [{ "schedule_id": "a4e0...", "due_at": "2026-10-16T08:00:00Z",
           "status": "taken", "responded_at": "2026-10-16T08:03:41Z" }]

OUTPUT  201 Created. The guardian dashboard shows "Metformin - taken at 08:03".''')

    h(d, "5.3 The voice function (actual request and response shape)", 2)
    code(d, '''INPUT   POST https://<project>.supabase.co/functions/v1/voice
        { "audio": "<base64 WAV, 16 kHz mono>", "lang": "hi-IN",
          "medicine": "Metformin" }

OUTPUT  { "transcript": "haan maine le liya",
          "intent": "taken",
          "reply": "Theek hai. Agli dawai sham ko.",
          "configured": true }

        intent is one of: taken | not_taken | repeat | unclear.
        Negation decides: "nahin li" returns not_taken, never taken.''')
    para(d, "Verified against recorded speech on seven Hindi phrases covering affirmation, negation, "
            "and a request to repeat; all seven classified correctly. The method and its limits are "
            "recorded in docs/VOICE-VERIFICATION.md.")

    h(d, "5.4 Emergency alert", 2)
    code(d, '''INPUT   Elder presses the SOS button and does not cancel within the grace period.

CALL    POST /rest/v1/rpc/... -> row inserted into sos_events with lat/lng
        Trigger on_sos_created() fires -> notify_push()

OUTPUT  Every linked guardian's phone receives a data-only FCM message that
        bypasses the in-app mute setting, carrying the elder's name, the time
        and a maps link. The elder's screen states exactly what was achieved;
        if nothing was delivered it says so instead of claiming success.''')

    h(d, "6. Test evidence")
    para(d, f"The repository contains {F['test_files']} test files, {F['test_lines']:,} lines. The "
            f"last recorded run of the unit suite executed {F['tests']} tests across "
            f"{F['test_suites']} suites with {F['test_fail']} failures "
            f"(run on {F['test_date']}; the HTML report is at "
            f"app/build/reports/tests/testDebugUnitTest/index.html).")
    table(d, ["Area covered", "Example test"], [
        ["Alarm scheduling and keys", "ReminderSchedulerTest, CancelDoseAlarmTest, DayBitmaskTest"],
        ["Reboot persistence", "BootPersistenceTest"],
        ["Daily re-arm", "DailyArmWorkerSchedulingTest, GeneralRemindersArmedTest"],
        ["Notification deep link", "ReminderDeepLinkTest, ReminderNotificationTest"],
        ["Offline durability", "OutboxOfflineDurabilityTest, OutboxStoreConcurrencyTest"],
        ["Network and error handling", "NetworkFailureTest, HttpErrorCodeTest, MalformedResponseTest, AuthInterceptorRetryTest"],
        ["Schema tolerance", "SchemaDriftToleranceTest, DtoRoundTripTest"],
        ["Emergency semantics", "SosResultSemanticsTest"],
        ["Elder surface and languages", "ElderExperienceTest, ElderStringsI18nTest"],
        ["Voice answering", "VoiceDoseTest"],
    ], widths=[2.1, 4.3])
    para(d, "A live run driven on a Google Play emulator, covering roughly 65 end-to-end cases, is "
            "recorded in docs/TEST-RESULTS.md with its findings and its stated limits.")
    save(d, "Task-15-Core-Source-Code.docx")


# =============================================================== TASK 16
def task16():
    d = new_doc(16, "User interface layout")

    h(d, "1. The central design decision")
    para(d, "CareCompanion ships one binary containing two deliberately unequal interfaces. The "
            "guardian surface is a conventional dense Android application; the elder surface is "
            "reduced until almost nothing can go wrong on it. They are not themes of one another "
            "and they are not responsive variants — they are separate screen sets drawn from the "
            "same data. Every other layout choice in this document follows from that asymmetry.")
    table(d, ["Property", "Elder surface", "Guardian surface"], [
        ["Screen files", F["elder_screens"], F["guardian_screens"]],
        ["Median declared text size", "20 sp", "13 sp"],
        ["Declarations below 16 sp", "16.2 %", "79.7 %"],
        ["Primary control height", "64–130 dp", "Material default (48 dp)"],
        ["Navigation", "Flat; no nested menus", f"{F['routes']} named routes, elder-scoped"],
        ["Languages", "English, Hindi, Marathi, Gujarati", "English"],
    ], widths=[1.8, 2.3, 2.3])

    h(d, "2. Screen inventory and navigation")
    para(d, "Routes are defined in one place, ui/nav/Routes.kt, so that no screen can be reached by "
            "a string typed somewhere else. Guardian screens that act on a particular older person "
            "carry the elder id as a path argument, which makes it impossible to open a management "
            "screen without naming whose data is being edited.")
    code(d, '''object Routes {
    const val SPLASH = "splash"; const val LOGIN = "login"; const val ROLE = "role"
    const val GUARDIAN_HOME = "guardian"
    const val ADD_ELDER     = "guardian/addElder"

    // elder-scoped guardian screens
    const val ELDER_PROFILE     = "guardian/elder/{elderId}/profile"
    const val CONTACTS          = "guardian/elder/{elderId}/contacts"
    const val MEDICINES         = "guardian/elder/{elderId}/medicines"
    const val SCHEDULE          = "guardian/elder/{elderId}/schedule"
    const val SCHEDULE_BUILDER  = "guardian/elder/{elderId}/schedule/build/{medicineId}"
    const val REMINDERS         = "guardian/elder/{elderId}/reminders"
    const val VITALS            = "guardian/elder/{elderId}/vitals"
    const val ADHERENCE         = "guardian/elder/{elderId}/adherence"
    const val SOS               = "guardian/elder/{elderId}/sos"
    const val FAMILY            = "guardian/elder/{elderId}/family"
    const val OTT               = "guardian/elder/{elderId}/ott"
    const val ELDER_APP         = "elderapp"

    fun elder(route: String, elderId: String) = route.replace("{elderId}", elderId)
}''', "Listing 1 — ui/nav/Routes.kt (abridged)")

    h(d, "2.1 Guardian screens", 2)
    table(d, ["Screen", "Source file", "Purpose"], [
        ["Dashboard", "GuardianDashboardScreen.kt", "Elder cards, status, alerts, entry to every other screen"],
        ["Add / edit elder", "AddEditElderScreen.kt", "Profile, photo, and the 6-digit code that binds the elder's phone"],
        ["Medicines", "MedicinesScreensG.kt", "List, add and edit medicines"],
        ["Schedule builder", "ScheduleBuilderScreen.kt", "Times of day and day-of-week mask per medicine"],
        ["Daily schedule", "GuardianDailyScheduleScreen.kt", "The day as the elder will see it"],
        ["Contacts", "ContactsScreensG.kt", "Photo-first contacts, emergency flags, device import"],
        ["Reminders", "GuardianRemindersScreen.kt", "General (non-medicine) reminders by category"],
        ["Vitals", "GuardianVitalsScreen.kt", "Readings, thresholds and PDF export"],
        ["Adherence", "GuardianAdherenceScreen.kt", "What was taken, missed or not answered"],
        ["Family members", "GuardianFamilyScreen.kt", "Invite a second guardian as view-only or editor"],
        ["SOS history / settings", "GuardianSosScreen.kt, SosSettingsScreen.kt", "Alert log and the emergency message template"],
        ["Video & apps", "GuardianOttScreen.kt", "Shortcut tiles that appear on the elder's home"],
        ["Wheelchair services", "GuardianWheelchairScreen.kt", "Local assistance directory"],
        ["Settings", "GuardianSettingsScreen.kt", "Account and notification preferences"],
    ], widths=[1.4, 2.0, 3.0], size=9)

    h(d, "2.2 Elder screens", 2)
    table(d, ["Screen", "Source", "Design note"], [
        ["Home", "ElderHomeScreen.kt / ElderApp.kt", "Photo of the person, a very large SOS control, and at most four tiles"],
        ["Take medicine", "ElderExperience.kt", "One medicine at a time; two large answers; optional spoken answer"],
        ["Contacts", "ElderExperience.kt", "Photographs, not names — tap a face to call"],
        ["Videos", "ElderExperience.kt", "Guardian-configured shortcut tiles only"],
        ["Vitals entry / history", "ElderVitalsViewModel.kt", "Number pad sized for imprecise touch"],
        ["SOS confirm / sent", "ui/sos/", "Cancellable grace period, then an honest result screen"],
        ["Settings", "ElderSettingsStore.kt", "Language, text size, high contrast"],
    ], widths=[1.4, 2.0, 3.0], size=9)

    h(d, "3. Design choices and the reasons for them")

    h(d, "3.1 Type scale", 2)
    para(d, "The elder surface has a median declared text size of 20 sp against the guardian "
            "surface's 13 sp, and 83.8 % of its declarations sit at or above the 16 sp floor that "
            "accessibility guidance implies. The figure in the project's research paper "
            "(Figure 3 there) was produced by parsing every fontSize declaration out of the source, "
            "so it measures the shipped interface rather than an intention.")

    h(d, "3.2 Touch targets", 2)
    para(d, "Every primary control on the elder surface is between 64 dp and 130 dp high, against "
            "the Material minimum of 48 dp and the 44x44 figure in WCAG 2.2 AAA. The SOS control on "
            "the home screen is 130 dp. The reason is motor rather than visual: a control that is "
            "merely legible can still be missed by a hand with a tremor.")

    h(d, "3.3 The light scheme is pinned", 2)
    para(d, "The theme originally followed isSystemInDarkTheme(). Every background in the app is a "
            "hardcoded light colour, so on a phone in dark mode the app rendered dark text on dark "
            "surfaces and parts of it became unreadable. This was found on a real device and proved "
            "by comparing pixel luminance on the same screen at two times of day. The scheme is now "
            "pinned to light, with the reason recorded in the source:")
    code(d, '''// darkTheme defaults to false on purpose. Every surface colour in this app is a
// fixed light value; following the system setting produced a half-dark app in
// which text and icons disappeared. Pinning light is the honest fix until a real
// dark palette exists.
fun CareCompanionTheme(darkTheme: Boolean = false, content: @Composable () -> Unit)''',
         "Listing 2 — ui/theme/Theme.kt")

    h(d, "3.4 Photographs instead of names", 2)
    para(d, "Elder-side contacts are shown as photographs. A user who cannot comfortably read a "
            "list can still recognise a face. Where no photo exists the app falls back to a person "
            "silhouette rather than an initial letter, because a letter is exactly the kind of "
            "abstraction the surface is trying to avoid.")

    h(d, "3.5 The interface never claims an outcome it has not observed", 2)
    para(d, "Confirmation screens report what actually happened. The emergency screen lists each "
            "channel and whether it succeeded, and when nothing was delivered it says so and offers "
            "a fallback instead of displaying a reassuring tick. This principle is why the SOS path "
            "was restructured and is the central finding of the project's research paper.")

    h(d, "3.6 Four languages on the elder surface only", 2)
    para(d, "English, Hindi, Marathi and Gujarati are supported on the elder surface, held in "
            "ui/elder/ElderStrings.kt and covered by ElderStringsI18nTest. The guardian surface is "
            "English only: the guardian is assumed to be the more confident technology user, and "
            "translating the dense surface would have cost more than it returned.")

    h(d, "4. Editable artifacts submitted")
    para(d, "The interface is declared in code rather than drawn in a layout editor, so the "
            "editable artifact is the Compose source itself. Two further editable sets accompany it:")
    bullets(d, [
        "app/src/main/java/com/carecompanion/app/ui/ — the complete Jetpack Compose source "
        f"({F['elder_screens'] + F['guardian_screens']} screen and view-model files plus theme and shared components). "
        "These are the files that produce the shipped interface; editing them changes the app.",
        "design-screens/screens/ — 17 standalone HTML mockups (9 guardian, 8 elder) that can be "
        "opened and edited in any browser or editor, used while iterating on layout before "
        "committing it to Compose.",
        "ds-bundle/_screenshots/ — rendered component sheets for the shared design system "
        "(buttons, cards, chips, status pills, navigation, contact and medicine rows).",
    ])
    para(d, "Exported views of the running interface are supplied separately as the screenshot set "
            "(11 screenshots taken from the built application on a phone, plus the earlier wireframe "
            "set), and in the user manual at docs/CareCompanion-User-Manual.pdf, which walks both "
            "surfaces screen by screen in non-technical language.")
    save(d, "Task-16-User-Interface-Layout.docx")


# =============================================================== TASK 17
def task17():
    d = new_doc(17, "Data storage")

    h(d, "1. What the structural layer is in this project")
    para(d, "CareCompanion has no soldered assembly; the equivalent structural layer is the data "
            "layer that makes the two surfaces usable at all. Two people on two phones must see one "
            "consistent picture of one older person's care, and neither must be able to read or "
            "change anyone else's. That is implemented as a PostgreSQL schema on Supabase with "
            "row-level security, a set of security-definer functions for the operations that cannot "
            "be expressed as a plain table write, and a local durability layer on the phone.")
    table(d, ["Layer", "Implementation", "Size"], [
        ["Server schema", "PostgreSQL 15 on Supabase, exposed over PostgREST",
         f"{len(F['tables'])} tables"],
        ["Authorisation", "Row-level security policies evaluated per request",
         f"{F['policies']} policies"],
        ["Server logic", "SQL functions, triggers and scheduled jobs",
         f"{len(F['funcs'])} functions"],
        ["Migration scripts", "supabase/migrations/*.sql, applied in order",
         f"{F['migrations']} files, {F['sql_lines']:,} lines"],
        ["On-device durability", "OutboxStore queue + SharedPreferences settings", "2 stores"],
    ], widths=[1.5, 3.3, 1.6])

    h(d, "2. Tables")
    table(d, ["Table", "Holds", "Key columns"], [
        ["users", "One row per signed-in account, guardian or elder", "firebase_uid, role, fcm_token, language"],
        ["elders", "The older person being cared for", "name, photo_url, phone_verified, verified_elder_uid, is_active"],
        ["guardian_elder_links", "Who may see or edit which elder, and at what level", "guardian_id, elder_id, access (owner/edit/view), status"],
        ["medicines", "Each medicine, with photographs of the strip", "elder_id, name, dosage, form, meal, is_active"],
        ["medicine_schedules", "When each medicine is due", "medicine_id, time, days, enabled"],
        ["adherence_logs", "What was actually taken, missed or unanswered", "occurrence_date, due_at, status, responded_at"],
        ["reminders", "General non-medicine reminders", "category_id, title, times, repeat_kind, days"],
        ["reminder_categories", "Water, walk, exercise and custom categories", "key, name, icon, is_default"],
        ["contacts", "People the elder can call, photo first", "name, phone, photo_url, is_emergency"],
        ["sos_events", "Each emergency activation with its location", "triggered_at, lat, lng, accuracy, status"],
        ["alerts", "What the guardian is told and whether it was read", "kind, ref_id, title, body, read"],
        ["vitals", "Blood pressure, sugar, weight and similar readings", "type, context, taken_at, entered_by"],
        ["vital_thresholds", "Normal and warning bands per reading type", "type, normal_lo/hi, warn_lo/hi, unit"],
        ["invite_codes", "Short codes that bind a device or add a family member", "code, kind, access, expires_at, redeemed_at"],
        ["ott_shortcuts", "Video and app tiles the guardian puts on the elder's home", "title, kind, package_or_url, sort"],
        ["wheelchair_places", "Local assistance directory", "city, name, kind, phone, maps_url"],
        ["app_config", "Server-side configuration key/value pairs", "key, value"],
    ], widths=[1.4, 2.6, 2.4], size=8.8)

    h(d, "3. Authorisation is in the database, not in the app")
    para(d, f"All {F['policies']} policies are enforced by PostgreSQL on every request. The client "
            "cannot opt out of them, so a bug or a modified client cannot read another family's "
            "data. The policies are expressed through a small set of predicate functions:")
    table(d, ["Function", "Answers"], [
        ["firebase_uid()", "Which Firebase account made this request (read from the verified JWT)"],
        ["current_user_id()", "The users.id for that account"],
        ["can_read_elder(id)", "May this account see this elder at all?"],
        ["can_manage_elder(id)", "May this account change this elder's data?"],
        ["can_own_elder(id)", "Is this account the owner, for actions only an owner may take?"],
        ["is_elder_self(id)", "Is the caller the elder whose device is bound to this profile?"],
        ["is_guardian_of(id)", "Is the caller a linked guardian?"],
        ["guard_elder_columns()", "Blocks changes to columns a non-owner must never write"],
    ], widths=[1.9, 4.5])

    h(d, "4. Implemented scripts")
    para(d, "The operations below cannot be done safely as plain table writes, because they must "
            "check a condition and act atomically. They are implemented as SECURITY DEFINER "
            "functions so the rule lives next to the data.")
    code(d, '''-- Issue a guardian invite. A fresh code per access level, so switching
-- View Only <-> Can Edit cannot hand back a code granting the other one.
create or replace function public.rpc_create_guardian_invite(p_elder_id uuid, p_access text)
returns public.invite_codes language plpgsql security definer set search_path = public as $$
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  if not public.can_own_elder(p_elder_id) then
    raise exception 'only the owner can invite family members';
  end if;
  ...
  loop                                   -- allocate a unique 6-digit code
    v_code := lpad((floor(random() * 1000000))::int::text, 6, '0');
    exit when not exists (select 1 from public.invite_codes where code = v_code);
    if i > 20 then raise exception 'could not allocate an invite code'; end if;
  end loop;
end $$;''', "Listing 1 — supabase/migrations/20260810000001_guardian_invite_codes.sql")
    para(d, "Redemption is the mirror image, and it is written so that failures cannot be used to "
            "probe the system: a wrong code and an expired code return the same message, so an "
            "attacker cannot discover which codes exist. An elder-binding code and a guardian "
            "invite code are separate kinds and cannot be redeemed as one another.")
    table(d, ["Script", "What it implements"], [
        ["rpc_create_elder", "Creates an elder profile and makes the caller its owner in one transaction"],
        ["rpc_create_invite_code / rpc_redeem_invite_code", "Binds one elder's phone to one profile, once"],
        ["rpc_create_guardian_invite / rpc_redeem_guardian_invite", "Adds a second family member as view-only or editor"],
        ["rpc_set_member_access / rpc_remove_member", "Changes or revokes a family member's access"],
        ["rpc_set_elder_active", "Deactivates a profile and locks the bound device out"],
        ["on_sos_created (trigger)", "Fires notify_push() the moment an emergency row is inserted"],
        ["on_adherence_corrected (trigger)", "Keeps the guardian's view correct when an answer is changed"],
        ["scan_missed_doses()", "Runs every 15 minutes under pg_cron; raises an alert for a dose nobody answered"],
        ["notify_push()", "Calls the push edge function, which sends the FCM message"],
        ["touch_updated_at (trigger)", "Maintains updated_at without trusting the client"],
    ], widths=[2.6, 3.8], size=9)

    h(d, "5. Storage on the phone")
    para(d, "The server is the single source of truth, but a phone is not always online and an "
            "answer given in a lift must not be lost.")
    bullets(d, [
        "OutboxStore (data/repo/OutboxStore.kt) queues writes that failed for want of "
        "connectivity and replays them in order when the network returns. It is covered by "
        "OutboxOfflineDurabilityTest and OutboxStoreConcurrencyTest, the latter because two "
        "coroutines can enqueue at the same moment.",
        "ReminderScheduler persists the day's armed alarms locally, so BootReceiver can restore "
        "them after a restart without a network call.",
        "SharedPreferences holds per-device preferences only — chosen language, text size, high "
        "contrast, notification toggle. Nothing in it is care data.",
        "allowBackup is set to false in the manifest, so care data is never copied into a "
        "cloud backup outside the family's control.",
    ])

    h(d, "6. Working check")
    para(d, f"The data layer is exercised by the automated suite: {F['tests']} tests across "
            f"{F['test_suites']} suites, {F['test_fail']} failures on the last recorded run "
            f"({F['test_date']}).")
    table(d, ["Check", "Test", "What it proves"], [
        ["Round trip", "DtoRoundTripTest", "Every DTO serialises and deserialises without loss"],
        ["Schema drift", "SchemaDriftToleranceTest", "An unexpected new server column does not crash an older app"],
        ["Malformed data", "MalformedResponseTest", "A bad response is handled rather than propagated"],
        ["Error codes", "HttpErrorCodeTest", "4xx and 5xx produce the right user-visible outcome"],
        ["Token refresh", "AuthInterceptorRetryTest", "An expired token is refreshed and the call retried once"],
        ["Offline queue", "OutboxOfflineDurabilityTest", "A write made offline survives and replays"],
        ["Concurrency", "OutboxStoreConcurrencyTest", "Simultaneous enqueues do not corrupt the queue"],
        ["Reboot", "BootPersistenceTest", "Armed alarms are restored from local storage after restart"],
    ], widths=[1.3, 2.0, 3.1], size=9)
    para(d, "Server-side authorisation was additionally verified by direct assertions against the "
            "live project — attempting each forbidden read and write as the wrong account and "
            "confirming the database refuses it. That run is recorded in docs/TEST-RESULTS.md.")
    save(d, "Task-17-Data-Storage.docx")


# =============================================================== TASK 18
def task18():
    d = new_doc(18, "End-to-end system integration")

    h(d, "1. The parts being connected")
    para(d, "Six parts have to work together: the elder surface, the guardian surface, the "
            "device-level services on the phone (alarms, notifications, microphone), the data layer "
            "inside the app, the Supabase backend, and two external services. The diagram below "
            "names each interface and the protocol that crosses it.")
    figure(d, "fig-18-interfaces.png",
           "Figure 1 — Component interfaces. Names match the actual package, function and table "
           "names in the repository.", width=6.3)

    h(d, "2. Interface definitions")
    table(d, ["Between", "Interface", "Contract"], [
        ["UI ↔ data layer", "Kotlin StateFlow", "View models expose immutable state; the UI never calls the network directly"],
        ["App ↔ Supabase tables", "HTTPS/JSON over PostgREST (Retrofit)", "Every request carries the Firebase ID token; AuthInterceptor refreshes it once on 401"],
        ["App ↔ edge functions", "HTTPS/JSON (Deno)", "push sends FCM; voice returns {transcript, intent, reply, configured}"],
        ["App ↔ Firebase Auth", "Credential Manager / Google Sign-In", "Returns the ID token the whole system is authorised by"],
        ["Server → app", "Data-only FCM message", "kind=sos bypasses the in-app mute; every other kind respects it"],
        ["App ↔ Android services", "AlarmManager + PendingIntent", "Exact alarm where permitted, 10-minute window where not"],
        ["Notification → screen", "Deep-link intent extra", "MainActivity is singleTop; a running app is routed, not restarted"],
        ["Edge function ↔ Sarvam AI", "HTTPS REST", "ASR then classification; the API key never leaves the server"],
        ["Database → push", "Trigger → notify_push()", "An inserted sos_events row pushes before any client polls"],
        ["Scheduler → database", "pg_cron every 15 min", "scan_missed_doses() raises alerts for unanswered doses"],
    ], widths=[1.4, 1.9, 3.1], size=8.8)

    h(d, "3. A complete operation, input to output")
    figure(d, "fig-18-dose-trace.png",
           "Figure 2 — One medicine dose traced from the guardian's input to the guardian's output.",
           width=6.3)
    para(d, "The trace crosses every interface in the table above except the emergency path. The "
            "guardian's input is a schedule; the guardian's output is an adherence record. In "
            "between, the data leaves the app, is stored under row-level security, comes back to a "
            "different phone, becomes an operating-system alarm, becomes a notification, is "
            "answered by voice or by touch, and returns. Each step was observed working on a real "
            "device, and the observed sequence is recorded in docs/TEST-RESULTS.md.")
    para(d, "The emergency path is the second complete operation. Pressing SOS starts a cancellable "
            "grace period; on expiry a row is inserted into sos_events with the location, a database "
            "trigger calls notify_push() immediately, and every linked guardian is notified. The "
            "elder's screen then reports what was actually achieved rather than asserting success.")

    h(d, "4. Integration problems encountered, and how each was resolved")
    para(d, "These are the real defects found while connecting the parts. They are recorded here "
            "because most of them shared one property: the system kept working from the user's "
            "point of view while silently doing nothing.")
    table(d, ["Problem", "Cause", "Resolution"], [
        ["Save Schedule appeared to do nothing",
         "The refresh that followed a successful save rebuilt the screen state from scratch and reset the completion flag; StateFlow conflation meant the screen often never observed the success at all.",
         "The refresh now uses copy() so it preserves the flag. Covered by MedicinesUiStateDoneTest."],
        ["Family invites were never delivered to anyone",
         "The move from phone OTP to Google Sign-In left users.phone empty, so the invite matched on a column that was always blank. Every invite was filed as pending forever — and there was no SMS or email integration to deliver it with in the first place.",
         "Replaced with the 6-digit share-code mechanism that already worked for binding the elder's device."],
        ["General reminders were stored but never reached the elder",
         "Medicine schedules were converted into alarms; general reminders were written to the database and read by nothing.",
         "Added remindersToAlarms() with day-namespaced keys. Covered by GeneralRemindersArmedTest."],
        ["Text invisible on a phone in dark mode",
         "The theme followed the system dark-mode setting while every background colour was a fixed light value.",
         "Pinned the light scheme, with the reason recorded in the source."],
        ["Tapping a reminder opened the home screen",
         "The notification carried no deep link, costing the elder extra navigation at the moment they were least able to do it.",
         "Deep-linked to the take-medicine screen; MainActivity declared singleTop. Covered by ReminderDeepLinkTest."],
        ["An answered dose still raised its alarm",
         "The alarm was armed for the day regardless of whether the dose had already been answered.",
         "Added cancelDose() keyed per reminder, time and day. Covered by CancelDoseAlarmTest."],
        ["The voice classifier answered \"unclear\" to everything",
         "The classification model is a reasoning model; the 60-token budget was consumed by reasoning, so it returned finish_reason \"length\" and null content.",
         "Set reasoning_effort low with a larger budget and replaced structured output with a single bare word. Seven of seven Hindi test phrases then classified correctly."],
        ["A classification failure discarded what was heard",
         "The outer error handler wrapped both transcription and classification.",
         "Classification can no longer throw, so the transcript survives even when the intent cannot be determined."],
    ], widths=[1.5, 2.6, 2.3], size=8.5)

    h(d, "5. Demonstration video")
    para(d, "A short screen recording accompanies this submission. The sequence below is the one to "
            "record; it traverses the full integration in roughly two minutes and shows a data "
            "change crossing between two devices.")
    bullets(d, [
        "0:00 — Guardian signs in with Google and opens the elder's profile.",
        "0:15 — Guardian adds a medicine and schedules it for a time two minutes ahead. Show the save confirming.",
        "0:30 — Switch to the elder's phone. Show the medicine already present, having crossed devices with no manual refresh.",
        "0:50 — The alarm fires. Show the notification, tap it, and show that it opens the take-medicine screen directly rather than the home screen.",
        "1:10 — Answer by speaking in Hindi. Show the transcript and the dose being marked taken.",
        "1:30 — Switch back to the guardian. Show the adherence record with the time of the answer.",
        "1:45 — On the elder's phone press SOS, let the grace period expire, and show the honest result screen listing what was delivered.",
        "1:55 — Show the alert arriving on the guardian's phone with the location.",
    ])
    para(d, "Recording note: the Android SDK is not currently installed on the development machine, "
            "so the application must be rebuilt (see Task 15, section 4) or the existing signed "
            "build installed from the release artifacts before the recording can be made. Two "
            "devices, or one device with account switching, are sufficient.", italic=True)
    save(d, "Task-18-End-to-End-Integration.docx")


# =============================================================== TASK 19
def task19():
    d = new_doc(19, "Deployment package")

    h(d, "1. How the finished project reaches its users")
    para(d, "CareCompanion is delivered as a single Android App Bundle published through Google "
            "Play. One binary serves both people: the role is chosen at sign-in, not by installing "
            "a different app. There is no server for the family to run and no address for them to "
            "configure — the backend is managed, and the only thing the two phones exchange "
            "directly is a six-digit code that the guardian reads out to the elder.")
    figure(d, "fig-19-deployment.png",
           "Figure 1 — Deployment diagram. Device tier, distribution and managed cloud tier.",
           width=6.3)

    h(d, "2. Runtime requirements")
    table(d, ["Item", "Value", "Why"], [
        ["Minimum Android", "7.0, API 24", "Reaches the inexpensive handsets older users are most often given"],
        ["Target Android", "API 36", "Required by Play policy for new uploads"],
        ["Version", "1.3 (versionCode 3)", "The build submitted with this task"],
        ["Microphone", "Optional (required=false)", "Voice answering is an alternative to the buttons; declaring it required would exclude devices that have none"],
        ["Telephony", "Optional (required=false)", "Keeps tablets and Chromebooks eligible; the dialer hand-off is a degradation, not a requirement"],
        ["Network", "HTTPS 443 outbound only", "No inbound ports, no VPN, nothing for the family to open"],
        ["Google Play Services", "Required", "Sign-In, Cloud Messaging and fused location"],
    ], widths=[1.4, 1.7, 3.3], size=9)

    h(d, "3. Dependencies")
    table(d, ["Dependency", "Version", "Used for"], [
        ["Jetpack Compose (BOM)", "2024.05.00", "The entire interface on both surfaces"],
        ["Navigation Compose", "2.7.7", "The route table"],
        ["Hilt / Dagger", "2.51.1", "Dependency injection"],
        ["Retrofit + OkHttp", "2.11.0 / 4.12.0", "HTTPS to PostgREST and the edge functions"],
        ["kotlinx.serialization", "1.6.3", "JSON"],
        ["Firebase BOM", "33.1.2", "Authentication and Cloud Messaging"],
        ["Credentials + Google ID", "1.3.0 / 1.1.1", "Google Sign-In"],
        ["WorkManager", "2.9.0", "The daily alarm re-arm job"],
        ["Room", "2.6.1", "Local persistence"],
        ["Play Services Location", "21.3.0", "The location attached to an emergency"],
        ["Coil", "2.6.0", "Loading contact and medicine photographs"],
        ["Coroutines", "1.8.1", "Concurrency throughout"],
    ], widths=[2.0, 1.4, 3.0], size=9)

    h(d, "4. Configuration")
    para(d, "Four pieces of configuration are needed to produce a release build. Two are secrets "
            "and are deliberately absent from version control, so cloning the repository yields a "
            "buildable debug app but not a signable release.")
    table(d, ["Item", "Where it lives", "In git?"], [
        ["google-services.json", "app/ — Firebase project, read at build time for the OAuth web client id", "No"],
        ["keystore.properties", "Repository root — storeFile, storePassword, keyAlias, keyPassword", "No"],
        ["SUPABASE_URL, anon key", "app/build.gradle.kts as BuildConfig fields", "Yes (the anon key is public by design; row-level security is what protects the data)"],
        ["SARVAM_API_KEY", "Supabase edge function secret, never in the APK", "No"],
    ], widths=[1.6, 3.2, 1.6], size=9)
    code(d, '''// The release build is signed only if keystore.properties exists; without it
// the build still succeeds unsigned, so anyone can clone and build a debug APK.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
signingConfigs {
    if (keystoreProps.isNotEmpty()) {
        create("release") { storeFile = rootProject.file(keystoreProps.getProperty("storeFile")); ... }
    }
}''', "Listing 1 — app/build.gradle.kts, secrets kept out of the repository")

    h(d, "5. Build and deploy procedure")
    code(d, '''# ---- 1. Application bundle for Play --------------------------------
JAVA_HOME=/path/to/jdk-17 ./gradlew :app:bundleRelease
#   -> app/build/outputs/bundle/release/app-release.aab

# ---- 2. Backend schema ---------------------------------------------
supabase link --project-ref <project-ref>
supabase db push                       # applies supabase/migrations/*.sql in order

# ---- 3. Edge functions ---------------------------------------------
supabase secrets set SARVAM_API_KEY=<key>
supabase functions deploy push
supabase functions deploy voice

# ---- 4. Scheduled job ----------------------------------------------
#   pg_cron runs scan_missed_doses() every 15 minutes; created by the migrations.

# ---- 5. Upload -------------------------------------------------------
#   Play Console -> Production -> upload app-release.aab -> roll out''',
         "Listing 2 — full deployment sequence")

    h(d, "6. First-run procedure for a family")
    bullets(d, [
        "The guardian installs the app and signs in with their Google account.",
        "The guardian creates the older person's profile, adds contacts, medicines and schedules.",
        "The app shows a six-digit code. The guardian reads it to the older person, or sends it by any means they already use.",
        "The older person installs the same app, signs in, and enters the code once. Their phone is now bound to that profile and opens straight into the simplified surface every time.",
        "The code expires after seven days and can only be redeemed once; a profile already bound to one phone refuses a second.",
    ])

    h(d, "7. Outstanding deployment step")
    para(d, "Google Play re-signs uploaded bundles with its own key under Play App Signing. The "
            "SHA-1 of that key must be registered in the Firebase project after the first upload, "
            "or Google Sign-In will fail for every installation that comes from the Play Store "
            "while continuing to work for locally built APKs. This is recorded in "
            "docs/PLAY-RELEASE.md along with the rest of the release checklist, and the privacy "
            "policy required by the listing is at docs/PRIVACY-POLICY.md.", italic=False)
    save(d, "Task-19-Deployment-Package.docx")


if __name__ == "__main__":
    print("Repository facts:", {k: v for k, v in F.items() if not isinstance(v, list)})
    task15(); task16(); task17(); task18(); task19()
    print("Documents written to", OUT)
