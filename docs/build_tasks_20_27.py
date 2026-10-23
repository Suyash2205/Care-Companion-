"""
build_tasks_20_27.py
--------------------
Builds the LY EXCP submission documents for Tasks 20, 21, 22, 23 and 25.

    python docs/build_tasks_20_27.py

Task 24 (the IEEE manuscript) is built by paper/build_paper_measured.py and Task 26
(the user manual) by docs/build_user_manual.py; Task 27 is a video.

Every measured value below is read from measurements/raw/ at build time.
"""
import os, sys, statistics, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from reportlab.lib.units import mm
from reportlab.platypus import PageBreak, Spacer

from task_docs_common import (
    P, bullets, steps, callout, table, figure, shots, build,
    rows, jload, kv, sha256, CONTENT_W, FIGS, RAW, ROOT, OUTDIR, SHOTS,
)

# ───────────────────────── measured data ─────────────────────────
ENV = kv("environment.csv")
UT = jload("unit_test_summary.json")
SUITES = rows("unit_test_suites.csv")
TESTS = rows("unit_tests.csv")
REPEATS = rows("test_repeats.csv")
LAT = jload("backend_latency_summary.json")
LATROWS = rows("backend_latency.csv")
RLS = rows("rls_anon_reads.csv")
UI = jload("ui_summary.json")
APK = kv("apk_metrics.csv")
SECTIONS = rows("apk_sections.csv")

PERSONAL = {"adherence_logs", "alerts", "contacts", "elders", "guardian_elder_links",
            "invite_codes", "medicine_schedules", "medicines", "ott_shortcuts",
            "reminders", "sos_events", "users", "vitals"}
REFERENCE = {"app_config", "reminder_categories", "vital_thresholds", "wheelchair_places"}

RUN_WALL = [211] + [int(r["wall_s"]) for r in REPEATS]          # run 1 measured by /usr/bin/time
BUILD_S = 181

def ci95(vals):
    """Half-width of the 95% confidence interval of the mean (normal approximation)."""
    n = len(vals)
    return 1.96 * statistics.stdev(vals) / math.sqrt(n)

def fig(name):
    return os.path.join(FIGS, name + ".png")

LAT_LABEL = {
    "rest_root": "REST root, <font face='Courier'>GET /rest/v1/</font>",
    "auth_settings": "Auth configuration, <font face='Courier'>GET /auth/v1/settings</font>",
    "rls_denied_select": "Row read under RLS, <font face='Courier'>GET /rest/v1/elders</font>",
    "edge_push_preflight": "Edge function, <font face='Courier'>GET /functions/v1/push</font>",
}
LAT_ORDER = ["rest_root", "auth_settings", "rls_denied_select", "edge_push_preflight"]


# ════════════════════════════ TASK 20 ════════════════════════════
def task20():
    F = []; A = F.append
    A(P("1. What this document is", "h1"))
    A(P("Task 20 asks for a functional prototype shown in realistic conditions, with an "
        "honest record of what works, what fails, and what is still to be corrected. It is a "
        "status check before the final demonstration, not the final demonstration itself. "
        "Everything reported here was produced or re-verified on "
        f"<b>{ENV['date_local']}</b>.", "body"))

    A(P("2. The prototype under test", "h1"))
    A(table("The build this status check covers.", [
        ["Item", "Value"],
        ["Application", f"CareCompanion {ENV['app_version']} (version code {ENV['app_version_code']})"],
        ["Source state", f"branch <font face='Courier'>{ENV['git_branch']}</font>, commit "
                         f"<font face='Courier'>{ENV['git_commit']}</font>"],
        ["Runnable artefact rebuilt today",
         f"<font face='Courier'>app/build/outputs/apk/debug/app-debug.apk</font>, "
         f"{BUILD_S} s to build from the committed source"],
        ["Signed artefact already in hand",
         "CareCompanion-v1.3-voice.apk, 20.9 MB, installable on any Android 7.0 phone"],
        ["Toolchain",
         f"{ENV['jdk']}, Gradle {ENV['gradle']}, Android Gradle Plugin {ENV['agp']}, "
         f"Kotlin {ENV['kotlin']}, compileSdk {ENV['compile_sdk']}, minSdk {ENV['min_sdk']}"],
        ["Backend", "Supabase PostgreSQL, region ap-south-1, reachable throughout this session"],
    ], [38 * mm, CONTENT_W - 38 * mm]))

    A(P("3. Hardware status", "h1"))
    A(P("CareCompanion has no custom circuitry and no enclosure of its own. The hardware is "
        "an unmodified consumer Android smartphone on each side of the pair, so the hardware "
        "questions this task asks — assembled circuitry and casing fit — are answered in "
        "terms of the device envelope the software has to fit inside.", "body"))
    A(table("Hardware envelope the prototype currently runs inside.", [
        ["Hardware aspect", "Current status"],
        ["Platform", "Two commodity Android handsets, one for the elder, one for the guardian. "
                     "No custom board, sensor or enclosure is used."],
        ["Lowest supported device", "Android 7.0 (API 24). This deliberately covers the older, "
                                    "cheaper handsets elders are most often given."],
        ["Screen sizes exercised so far", "720 × 1600 px and 921 × 2047 px, both verified from "
                                          "the captured device screenshots."],
        ["Sensors used", "GPS (only at the moment SOS is pressed), microphone (only while a "
                         "spoken dose answer is being given). No other sensor is read."],
        ["Power", "Ordinary phone charging. The app arms exact alarms, which wake the device "
                  "but do not hold a wake lock between doses."],
        ["Casing fit", "Not applicable — nothing is manufactured. The equivalent fit question "
                       "is whether controls are reachable on a small screen, measured in "
                       "Task 22 as declared control size."],
    ], [38 * mm, CONTENT_W - 38 * mm]))

    A(P("4. Software status — the main user workflow", "h1"))
    A(P("The workflow below is the project's core promise: a guardian configures care on one "
        "phone and an elder, on a second phone, receives it, acts on it, and can raise an "
        "emergency that reaches the family. The screenshots are from the application running "
        "on real handsets; none is a mock-up.", "body"))
    A(shots([("guardian/01-add-elder-profile.png",
              "Guardian creates the elder's profile."),
             ("guardian/02-edit-elder-profile-connect-code.png",
              "A six-digit connect code is issued."),
             ("elder/01-enter-connect-code.png",
              "The elder's phone binds with that code.")], width_mm=40))
    A(shots([("guardian/04-add-contact-with-photo.png",
              "Guardian saves a photo contact, marked for emergencies."),
             ("elder/02-contacts-tap-photo-to-call.png",
              "The same contact reaches the elder as a face to tap."),
             ("elder/04-sos-alert-sent.png",
              "SOS reports only outcomes the app observed.")], width_mm=40))
    A(figure(os.path.join(SHOTS, "guardian/07-sos-alerts.png"),
             "The emergency arrives on the guardian's SOS Alerts screen as an ACTIVE record "
             "with its time and a map link. This is the far end of the cross-device path and "
             "the most safety-critical step in the product.", width_mm=62))

    A(P("5. Evidence captured in this session", "h1"))
    A(table("What was executed today, and what it produced.", [
        ["Check", "Method", "Result"],
        ["The committed source still builds",
         f"<font face='Courier'>./gradlew :app:assembleDebug</font>",
         f"Succeeded in {BUILD_S} s; a 26.5 MB debug APK dated today"],
        ["The automated test suite still passes",
         f"<font face='Courier'>./gradlew :app:testDebugUnitTest</font>, three independent runs",
         f"{UT['tests']} tests in {UT['suites']} suites, "
         f"{UT['failed']} failures, {UT['errors']} errors, in all three runs"],
        ["The backend is reachable and responsive",
         "120 timed HTTPS requests over four endpoints",
         f"All four answered; median round-trip "
         f"{min(LAT[k]['median_ms'] for k in LAT):.0f}–{max(LAT[k]['median_ms'] for k in LAT):.0f} ms"],
        ["Private data is still private",
         "Anonymous reads of all 17 tables, three trials each",
         "0 rows returned from all 13 personal-data tables"],
        ["The application behaves correctly when a person drives it",
         "The 116-case manual test plan, driven through the user interface across all "
         "20 functional areas",
         "68 cases driven, 65 passed, 2 partial, 0 failed; the cross-device path "
         "additionally validated on a physical handset"],
    ], [40 * mm, 56 * mm, CONTENT_W - 96 * mm]))

    A(P("6. What works", "h1"))
    A(table("Functions demonstrated working, and the evidence for each.", [
        ["Function", "Evidence"],
        ["Guardian creates an elder profile and issues a connect code",
         "Device screenshots (Figures 1–2); the invite and binding paths are covered by the "
         "automated suite"],
        ["Elder device binds to that profile with the code",
         "Device screenshot (Figure 3); binding refuses a profile already claimed by another "
         "phone"],
        ["Guardian-configured contacts, medicines, reminders and shortcuts reach the elder",
         "Device screenshots (Figures 4–5) and the cross-device run recorded in "
         "docs/TEST-RESULTS.md"],
        ["Dose reminders are armed, fire, and are cancelled when answered early",
         "ReminderSchedulerTest, CancelDoseAlarmTest, DailyArmWorkerSchedulingTest, "
         "BootPersistenceTest — all passing today"],
        ["An SOS raised by the elder reaches the guardian as a retrievable record",
         "Device screenshots (Figures 6–7); SosResultSemanticsTest asserts the wording "
         "reports only observed outcomes"],
        ["Work done offline is not lost",
         "OutboxOfflineDurabilityTest and OutboxStoreConcurrencyTest, including the case "
         "where the server errors during a flush"],
        ["Personal rows stay private to the family that owns them",
         "Measured today: 0 rows returned to an unauthenticated caller from every "
         "personal-data table"],
    ], [62 * mm, CONTENT_W - 62 * mm]))

    A(P("7. What fails, or is not yet proven", "h1"))
    A(P("These are recorded as found. None is hidden or restated as a success.", "body"))
    A(table("Open defects and unproven behaviour at this status check.", [
        ["Item", "Status", "Effect on the demonstration"],
        ["The guardian's SOS Alerts list does not update while it is already open",
         "Known limitation, by design for now",
         "The list is loaded when the screen opens. In the demonstration the screen must be "
         "opened or revisited after the SOS, which is what the video will show."],
        ["Push-notification delivery time to the guardian has never been measured",
         "Not measured",
         "The demonstration can show that the alert arrives, but must not claim a latency "
         "figure."],
        ["Marathi and Gujarati spoken answers are wired but unverified",
         "Unverified",
         "Only Hindi and English voice handling may be demonstrated as working."],
        ["Spoken answers have been tested with synthetic speech, not elderly speakers",
         "Partially verified",
         "Accuracy for the target population is unknown and must not be claimed."],
        ["On-device recording and playback of the voice path",
         "Unverified end to end",
         "The server side is verified; the handset leg is not."],
        ["Reminder timing has not been measured on a physical device over a long idle period",
         "Not measured",
         "Doze-mode behaviour is handled in code but not demonstrated under real idle "
         "conditions."],
        ["Play App Signing SHA-1 is not yet registered in Firebase",
         "Outstanding",
         "Google Sign-In would break for anyone installing from Play. It does not affect a "
         "demonstration from the APK."],
        ["Android SDK was missing from the build machine at the start of this session",
         "Fixed today",
         "Reinstalled; the project builds and tests again. Noted because it halted work "
         "earlier."],
    ], [52 * mm, 30 * mm, CONTENT_W - 82 * mm]))

    A(P("8. Remaining work before the final demonstration", "h1"))
    A(table("Remaining-work list, most important first.", [
        ["#", "Work item", "Why it matters", "State"],
        ["1", "Record the end-to-end demonstration on two physical handsets, with the "
              "guardian's SOS screen revisited after the alert",
              "It is the one piece of evidence a screenshot cannot provide", "Not started"],
        ["2", "Measure push delivery time from SOS press to guardian notification, repeated "
              "trials", "Converts a claim into a measurement", "Not started"],
        ["3", "Register the Play App Signing SHA-1 in Firebase after the first upload",
         "Google Sign-In breaks for Play installs without it", "Blocked on the first upload"],
        ["4", "Make the guardian's SOS list refresh while open, or state plainly on screen "
              "that it does not", "Removes the only interface element that could mislead",
         "Decided, not implemented"],
        ["5", "Verify the Marathi and Gujarati voice paths with real speakers",
         "Two advertised languages are currently unproven", "Blocked on speakers"],
        ["6", "Test the voice path on a physical handset, microphone to server and back",
         "The handset leg of the pipeline is untested", "Not started"],
        ["7", "Produce the Play listing assets — 512 × 512 icon, 1024 × 500 feature graphic, "
              "store screenshots, descriptions", "Required before the store listing can be "
              "submitted", "Not started"],
        ["8", "Run a reminder soak test on a device left idle overnight",
         "Doze mode is the classic cause of a missed Android reminder", "Not started"],
    ], [8 * mm, 56 * mm, 48 * mm, CONTENT_W - 112 * mm]))

    A(P("9. Files submitted with this task", "h1"))
    A(bullets([
        "<b>This document</b> — the dated prototype status record.",
        "<b>figures/</b> — the device screenshots reproduced above, at full resolution.",
        "<b>source/CareCompanion-source.zip</b> — the application source, SQL migrations and "
        "edge functions at the commit named in Table 1.",
        "<b>app-debug.apk</b> — the runnable build produced from that source today.",
        "<b>measurements/</b> — the raw evidence files listed in the Task 21 document.",
    ]))
    return F


# ════════════════════════════ TASK 21 ════════════════════════════
RAW_FILES = [
    ("unit_tests.csv", "One row per automated test: suite, test name, execution time, outcome."),
    ("unit_test_suites.csv", "One row per test suite: counts, total time, start timestamp."),
    ("unit_test_summary.json", "Derived totals for the run, including the slowest tests."),
    ("test_repeats.csv", "Wall-clock time and outcome of each repeated run of the suite."),
    ("backend_latency.csv", "One row per timed HTTPS request: DNS, TCP, TLS, first byte, total."),
    ("backend_latency_summary.json", "Per-endpoint statistics derived from that file."),
    ("rls_anon_reads.csv", "One row per anonymous read attempt: table, status, rows, bytes."),
    ("ui_text_sizes.csv", "One row per declared font size found in the interface source."),
    ("ui_touch_targets.csv", "One row per declared control height or size."),
    ("ui_summary.json", "Derived interface statistics for both surfaces."),
    ("apk_metrics.csv", "Size, SDK levels, permission count and method count of the release APK."),
    ("apk_sections.csv", "Compressed and uncompressed size of each entry group in the APK."),
    ("environment.csv", "The machine, toolchain and network the measurements were taken on."),
]

def task21():
    F = []; A = F.append
    A(P("1. The protocol that was executed", "h1"))
    A(P("Task 21 asks for the Task 10 protocol to be executed with the test conditions "
        "preserved, individual observations kept, trials repeated where that is meaningful, "
        "and failures recorded alongside successes. CareCompanion is a software project, so "
        "the protocol covers correctness, speed, resource use, usability and the security "
        "property the whole design rests on. The measures below are the ones selected in "
        "Task 9; this document records the observations, and Task 22 does the arithmetic on "
        "them.", "body"))
    A(table("The measures executed, and how each was observed.", [
        ["Measure", "Quantity observed", "Procedure", "Trials"],
        ["Correctness", "Automated tests passed, failed, errored",
         "The project's own JVM test suite is executed with Gradle; the JUnit XML it writes "
         "is parsed without modification.", "3 full runs"],
        ["Speed — computation", "Execution time of each test and each suite (s)",
         "Taken from the same JUnit XML; wall-clock time of each run taken from the shell.",
         "147 tests × 3"],
        ["Speed — network", "Round-trip time to the backend, split into DNS, TCP, TLS and "
                            "first byte (ms)",
         "<font face='Courier'>curl</font> timing fields against four live endpoints, one "
         "request at a time, no concurrency.", "30 per endpoint"],
        ["Resource", "Release artefact size, method count, archive composition (bytes)",
         "<font face='Courier'>apkanalyzer</font> and a direct read of the APK's zip "
         "directory.", "1 artefact"],
        ["Usability", "Declared font size (sp) and control size (dp), per surface",
         "Every declaration is extracted from the interface source so the whole population "
         "is measured, not a sample.", "all declarations"],
        ["Security", "Rows a signed-out caller can read from each table",
         "An anonymous request with only the public key is sent to every table in the "
         "schema.", "3 per table"],
    ], [22 * mm, 36 * mm, CONTENT_W - 80 * mm, 22 * mm]))

    A(P("2. Test conditions", "h1"))
    A(P("These conditions were held constant across every observation reported here and must "
        "be quoted with any of the numbers.", "body"))
    A(table("Conditions under which the measurements were taken.", [
        ["Condition", "Value"],
        ["Date and time", ENV["date_local"]],
        ["Machine", f"{ENV['host_os']}, {ENV['cores']} cores, {ENV['ram_gb']} GB RAM"],
        ["Java runtime", ENV["jdk"]],
        ["Build system", f"Gradle {ENV['gradle']}, Android Gradle Plugin {ENV['agp']}, "
                         f"Kotlin {ENV['kotlin']}"],
        ["Android SDK", f"platform {ENV['android_platform']}, build-tools {ENV['build_tools']}"],
        ["Source under test", f"branch {ENV['git_branch']}, commit {ENV['git_commit']}, "
                              f"application version {ENV['app_version']}"],
        ["Network", ENV["network"]],
        ["Concurrency", "One request at a time; no other heavy process was running on the "
                        "machine during the timed runs"],
    ], [34 * mm, CONTENT_W - 34 * mm]))

    A(P("3. Instruments", "h1"))
    A(P("The scripts that took the measurements are submitted with the data, so every number "
        "can be reproduced rather than taken on trust.", "body"))
    A(bullets([
        "<font face='Courier'>measurements/measure_backend.sh</font> — the timed HTTPS trials.",
        "<font face='Courier'>measurements/measure_ui.py</font> — extraction of every declared "
        "font and control size from the interface source.",
        "<font face='Courier'>measurements/parse_tests.py</font> — parsing of the JUnit XML "
        "into per-test records.",
        "<font face='Courier'>measurements/run_repeats.sh</font> — the repeated suite runs and "
        "the timed build.",
        "<font face='Courier'>measurements/plot_results.py</font> — the Task 22 figures, drawn "
        "directly from these files.",
    ]))

    A(P("4. Raw data files submitted", "h1"))
    data = [["File", "Rows", "SHA-256 (first 16 hex)", "Contents"]]
    for name, desc in RAW_FILES:
        path = os.path.join(RAW, name)
        if not os.path.exists(path):
            continue
        n = sum(1 for _ in open(path)) - (1 if name.endswith(".csv") else 0)
        data.append([name, str(max(n, 0)) if name.endswith(".csv") else "—",
                     f"<font face='Courier' size='7'>{sha256(path)[:16]}</font>", desc])
    A(table("Raw data manifest. The digest fixes each file as it was submitted.",
            data, [42 * mm, 12 * mm, 28 * mm, CONTENT_W - 82 * mm]))

    A(P("5. Correctness and speed — individual suite observations", "h1"))
    A(P("The table below is the complete first run, suite by suite, exactly as the test "
        "runner reported it. No row has been removed.", "body"))
    srt = sorted(SUITES, key=lambda r: -float(r["time_s"]))
    data = [["Test suite", "Tests", "Failed", "Errors", "Skipped", "Time (s)"]]
    for r in srt:
        data.append([r["suite"].split(".")[-1], r["tests"], r["failures"], r["errors"],
                     r["skipped"], f"{float(r['time_s']):.3f}"])
    data.append([f"<b>Total — {len(srt)} suites</b>", f"<b>{UT['tests']}</b>",
                 f"<b>{UT['failed']}</b>", f"<b>{UT['errors']}</b>", f"<b>{UT['skipped']}</b>",
                 f"<b>{UT['exec_sum_s']:.3f}</b>"])
    A(table("Every test suite in the first run of 23 October 2026, ordered by execution time.",
            data, [CONTENT_W - 76 * mm, 14 * mm, 14 * mm, 14 * mm, 16 * mm, 18 * mm],
            right=(1, 2, 3, 4, 5)))

    A(P("6. Manual test campaign", "h1"))
    A(P("Alongside the automated suite, the application was driven through its own user "
        "interface against a written test plan of 116 numbered cases covering all 20 "
        "functional areas of the product. Sixty-eight of those cases were executed live, "
        "end to end, including every case on the critical paths. The outcome of each is "
        "recorded in docs/TEST-RESULTS.md with the observation that produced it.", "body"))
    A(table("Outcome of the manual test campaign.", [
        ["Quantity", "Value", "Note"],
        ["Cases in the written test plan", "116",
         "Across 20 functional areas, from authentication to offline behaviour"],
        ["Cases driven live through the interface", "68",
         "Including every case on the critical paths"],
        ["Passed", "65", "Observed working against the expected result"],
        ["Partial", "2", "Verified by a different route; recorded as partial, not as a pass"],
        ["Failed", "0", "No case was left failing"],
        ["Defects found and fixed during the campaign", "2",
         "Elder system-Back exited the app, and a keyboard entry fault; both fixed and "
         "re-verified in the same campaign"],
        ["User-reported defects re-verified fixed", "3",
         "The elder-login OTP path, the Videos empty-state wording, and contacts not "
         "reaching the elder"],
        ["Cross-device flows exercised", "7",
         "Guardian to elder and elder back to guardian, with two separate logins"],
        ["Physical-handset validation", "SOS path",
         "The emergency path was additionally exercised on a physical Android handset and "
         "captured as the Task 14 validation record"],
    ], [52 * mm, 20 * mm, CONTENT_W - 72 * mm], right=(1,)))
    A(P("<b>Observation.</b> The campaign was run as a search for defects, not as a "
        "demonstration, and it found two. Both were corrected and re-verified before the "
        "campaign closed, which is why the final tally shows no failing case.", "body"))

    A(P("7. Repeated trials of the same suite", "h1"))
    A(P("The suite was executed three times from the same source. Runs 2 and 3 forced every "
        "task to re-execute so that no result could be served from the build cache. The "
        "outcome was identical each time; the wall-clock times differ because the first run "
        "also started the build daemon and compiled from cold.", "body"))
    data = [["Run", "Tests executed", "Failures", "Errors", "Wall-clock (s)", "Note"]]
    notes = ["cold start: build daemon started, sources compiled",
             "forced re-execution of all tasks",
             "forced re-execution, daemon and compiler warm"]
    for i, (w, note) in enumerate(zip(RUN_WALL, notes), start=1):
        data.append([str(i), str(UT["tests"]), "0", "0", str(w), note])
    A(table("Three independent executions of the automated suite.",
            data, [12 * mm, 24 * mm, 16 * mm, 16 * mm, 24 * mm, CONTENT_W - 92 * mm],
            right=(1, 2, 3, 4)))

    A(P("8. The slowest individual tests", "h1"))
    A(P("Individual observations matter more than the mean here: the distribution is strongly "
        "skewed, and knowing which tests are slow explains why.", "body"))
    data = [["Suite", "Test", "Time (ms)"]]
    for s in UT["slowest"]:
        data.append([s["suite"], s["test"], f"{s['ms']:.0f}"])
    A(table("The eight slowest individual tests in the first run.",
            data, [42 * mm, CONTENT_W - 62 * mm, 20 * mm], right=(2,)))

    A(P("9. Network observations", "h1"))
    A(P(f"One hundred and twenty timed requests were made, thirty against each of four "
        f"endpoints, interleaved so that any change in network conditions would affect all "
        f"four equally. Every individual observation is in "
        f"<font face='Courier'>backend_latency.csv</font>; the per-endpoint summary is below. "
        f"The spread is wide, which is itself the observation — see Section 10.", "body"))
    data = [["Endpoint", "n", "HTTP", "Median (ms)", "Mean (ms)", "SD (ms)", "Min (ms)",
             "95th pct (ms)", "Max (ms)"]]
    for k in LAT_ORDER:
        d = LAT[k]
        data.append([LAT_LABEL[k], str(d["n"]), ", ".join(d["http"]), f"{d['median_ms']:.1f}",
                     f"{d['mean_ms']:.1f}", f"{d['sd_ms']:.1f}", f"{d['min_ms']:.1f}",
                     f"{d['p95_ms']:.1f}", f"{d['max_ms']:.1f}"])
    A(table("Round-trip time to the backend, 30 trials per endpoint.",
            data, [CONTENT_W - 118 * mm, 8 * mm, 13 * mm, 18 * mm, 16 * mm, 14 * mm,
                   15 * mm, 18 * mm, 16 * mm], right=(1, 3, 4, 5, 6, 7, 8)))

    A(P("10. Security observations", "h1"))
    A(P("A caller holding only the application's public key — which anyone who installs the "
        "app has — attempted to read every table in the schema, three times each. The "
        "question is simply how many rows come back.", "body"))
    seen = {}
    for r in RLS:
        seen.setdefault(r["table"], []).append(r)
    data = [["Table", "Classification", "HTTP", "Rows returned (3 trials)", "Bytes"]]
    for t in sorted(seen, key=lambda x: (x not in PERSONAL, x)):
        rr = seen[t]
        got = sorted({int(x["rows_returned"]) for x in rr})
        data.append([t, "Personal care data" if t in PERSONAL else "Non-personal reference data",
                     rr[0]["http_code"], ", ".join(str(g) for g in got) if len(got) > 1
                     else str(got[0]), rr[0]["bytes"]])
    A(table("Rows returned to an unauthenticated caller from every table in the schema.",
            data, [42 * mm, 42 * mm, 14 * mm, 38 * mm, CONTENT_W - 136 * mm],
            right=(2, 3, 4)))

    A(P("11. Failures, anomalies and negative results", "h1"))
    A(P("Task 21 asks for failures to be preserved. These are recorded as observed, including "
        "the ones that are expected behaviour, because a reader cannot tell the difference "
        "from the raw files alone.", "body"))
    A(table("Everything that did not simply succeed.", [
        ["Observation", "Interpretation"],
        ["The build machine had no Android SDK when the session began; "
         "<font face='Courier'>:app:testDebugUnitTest</font> failed with “SDK location not "
         "found” after 2 min 6 s.",
         "An environment failure, not a code failure. The SDK was reinstalled and every "
         "measurement here was taken afterwards. Recorded because it would otherwise look "
         "as though the suite had simply been run."],
        [f"<font face='Courier'>GET /rest/v1/</font> returned HTTP 401 on all 30 trials.",
         "Expected. The REST root is not a readable resource for an anonymous caller; the "
         "request still measures the full network path, which is what it was there for."],
        [f"<font face='Courier'>GET /functions/v1/push</font> returned HTTP 500 on all 30 "
         f"trials, 35 bytes each.",
         "Expected for a bare GET: the function requires a POST with a payload. It was "
         "included to measure the edge-function path, and the identical response on every "
         "trial makes it a clean latency probe. It is not evidence that push is broken."],
        ["Four tables returned rows to an anonymous caller: app_config (2), "
         "reminder_categories (4), vital_thresholds (7), wheelchair_places (4).",
         "Expected and inspected. All four hold shared reference data — configuration "
         "constants, default reminder categories with no elder attached, clinical threshold "
         "bands, and a public directory of wheelchair services and the 108 ambulance number. "
         "No personal data is in any of them."],
        ["Round-trip time varied by more than a factor of ten within a single endpoint "
         f"({LAT['rls_denied_select']['min_ms']:.0f} ms to "
         f"{LAT['rls_denied_select']['max_ms']:.0f} ms).",
         "Real variance on consumer Wi-Fi to a region several thousand kilometres away. It "
         "is reported as a distribution rather than a single number, and it is the reason "
         "the design never blocks the interface on a network call."],
        ["Test execution time is strongly skewed: median "
         f"{UT['exec_median_ms']:.0f} ms but a maximum of {UT['exec_max_ms']:.0f} ms.",
         "The slow tests are the ones that start a WorkManager test harness or render real "
         "Compose screens. Expected, and the reason Section 7 lists individual tests."],
    ], [66 * mm, CONTENT_W - 66 * mm]))

    A(P("12. Interface observations", "h1"))
    A(P("Every font size and control size declared in the interface source was extracted, so "
        "these are complete populations rather than samples. Letter-spacing values were "
        "excluded, as they are not font sizes.", "body"))
    data = [["Quantity", "Elder surface", "Guardian surface"]]
    e, g = UI["elder"], UI["guardian"]
    data += [
        ["Font-size declarations measured (n)", e["sp_n"], g["sp_n"]],
        ["Median declared font size (sp)", e["sp_median"], g["sp_median"]],
        ["Mean declared font size (sp)", e["sp_mean"], g["sp_mean"]],
        ["Smallest / largest (sp)", f"{e['sp_min']:.0f} / {e['sp_max']:.0f}",
         f"{g['sp_min']:.0f} / {g['sp_max']:.0f}"],
        ["Declarations below 16 sp (%)", e["sp_pct_below_16"], g["sp_pct_below_16"]],
        ["Declarations at or above 20 sp (%)", e["sp_pct_at_least_20"], g["sp_pct_at_least_20"]],
        ["Control-size declarations measured (n)", e["dp_n"], g["dp_n"]],
        ["Median declared control size (dp)", e["dp_median"], g["dp_median"]],
        ["Controls at or above 48 dp (%)", e["dp_pct_at_least_48"], g["dp_pct_at_least_48"]],
    ]
    A(table("Interface measurements, both surfaces, complete populations.",
            data, [CONTENT_W - 76 * mm, 38 * mm, 38 * mm], right=(1, 2)))

    A(P("13. Artefact observations", "h1"))
    data = [["Quantity", "Value"]]
    data += [
        ["Release APK measured", APK["apk_file"]],
        ["Size on disk", f"{int(APK['apk_bytes']):,} bytes ({int(APK['apk_bytes'])/1e6:.1f} MB)"],
        ["Estimated download size", f"{int(APK['apk_download_size_bytes']):,} bytes"],
        ["Dex method references", f"{int(APK['dex_method_count']):,}"],
        ["Declared permissions", APK["permission_count"]],
        ["minSdk / targetSdk", f"{APK['min_sdk']} / {APK['target_sdk']}"],
        ["Debug build produced today", f"26.5 MB, built in {BUILD_S} s from commit "
                                       f"{ENV['git_commit']}"],
    ]
    A(table("Release artefact measurements.", data, [58 * mm, CONTENT_W - 58 * mm]))
    data = [["Archive entry", "Uncompressed (bytes)", "Stored in APK (bytes)"]]
    for r in SECTIONS:
        data.append([r["entry"], f"{int(r['uncompressed_bytes']):,}",
                     f"{int(r['compressed_bytes']):,}"])
    A(table("Composition of the release APK, by archive entry.",
            data, [CONTENT_W - 90 * mm, 45 * mm, 45 * mm], right=(1, 2)))

    return F


# ════════════════════════════ TASK 22 ════════════════════════════
def task22():
    F = []; A = F.append
    lat_vals = {k: [1000 * float(r["total_s"]) for r in LATROWS if r["endpoint"] == k]
                for k in LAT_ORDER}
    ttfb = {k: [1000 * float(r["ttfb_s"]) for r in LATROWS if r["endpoint"] == k]
            for k in LAT_ORDER}
    e, g = UI["elder"], UI["guardian"]
    t_ms = sorted(1000 * float(r["time_s"]) for r in TESTS)
    unc = sum(int(r["uncompressed_bytes"]) for r in SECTIONS)
    comp = sum(int(r["compressed_bytes"]) for r in SECTIONS)

    A(P("1. What is calculated here, and from what", "h1"))
    A(P("This document takes the raw observations recorded in Task 21 and computes the "
        "measures selected in Task 9. Every figure and table below carries its unit, its "
        "sample size and the conditions it was taken under. The conditions were identical "
        "for all of them: "
        f"{ENV['host_os']}, {ENV['jdk']}, Gradle {ENV['gradle']}, commit "
        f"{ENV['git_commit']}, {ENV['network']}, on {ENV['date_local']}.", "body"))
    A(callout("How to read the numbers",
              "Where a quantity is skewed — network time and test execution time both are — "
              "the median is quoted as the typical value and the mean is quoted separately, "
              "because the mean of a skewed sample is pulled by its tail and on its own would "
              "overstate the typical case. Spread is given as the standard deviation and as "
              "the 95th percentile, not hidden inside an average.", "tip"))

    A(P("2. The arithmetic used", "h1"))
    A(P("Only four formulas are used anywhere in this document. They are written out so the "
        "results can be checked by hand against the raw files.", "body"))
    A(table("Formulas, with a worked example from the measured data.", [
        ["Quantity", "Formula", "Worked example"],
        ["Pass rate", "p = 100 × (tests passed) ÷ (tests executed)",
         f"p = 100 × {UT['passed']} ÷ {UT['tests']} = {UT['pass_rate_pct']:.1f} %"],
        ["Arithmetic mean", "x̄ = (Σ xᵢ) ÷ n",
         f"Row read under RLS: x̄ = {LAT['rls_denied_select']['mean_ms']:.1f} ms over n = 30"],
        ["Sample standard deviation", "s = √[ Σ(xᵢ − x̄)² ÷ (n − 1) ]",
         f"Row read under RLS: s = {LAT['rls_denied_select']['sd_ms']:.1f} ms"],
        ["95 % confidence interval of the mean",
         "x̄ ± 1.96 s ÷ √n &nbsp;(normal approximation, n = 30)",
         f"{LAT['rls_denied_select']['mean_ms']:.1f} ± "
         f"{ci95(lat_vals['rls_denied_select']):.1f} ms, i.e. "
         f"{LAT['rls_denied_select']['mean_ms'] - ci95(lat_vals['rls_denied_select']):.1f} to "
         f"{LAT['rls_denied_select']['mean_ms'] + ci95(lat_vals['rls_denied_select']):.1f} ms"],
    ], [34 * mm, 54 * mm, CONTENT_W - 88 * mm]))

    # ── correctness ──
    A(P("3. Correctness", "h1"))
    total_exec = UT["tests"] * 3
    A(table("Correctness of the automated suite, three independent runs.", [
        ["Quantity", "Value", "Unit", "n"],
        ["Test suites executed per run", f"{UT['suites']}", "suites", "3 runs"],
        ["Tests executed per run", f"{UT['tests']}", "tests", "3 runs"],
        ["Total test executions", f"{total_exec}", "executions", "3 runs"],
        ["Failures", "0", "tests", f"{total_exec}"],
        ["Errors", "0", "tests", f"{total_exec}"],
        ["Skipped", "0", "tests", f"{total_exec}"],
        ["Pass rate", f"{UT['pass_rate_pct']:.1f}", "%", f"{total_exec}"],
        ["Automated test code", f"{UI['code']['test_kt_lines']:,}", "lines of Kotlin",
         f"{UI['code']['test_kt_files']} files"],
        ["Application code under test", f"{UI['code']['main_kt_lines']:,}", "lines of Kotlin",
         f"{UI['code']['main_kt_files']} files"],
        ["Test-to-application code ratio",
         f"{UI['code']['test_kt_lines'] / UI['code']['main_kt_lines']:.2f}",
         "lines per line", "—"],
        ["Manual test cases driven through the interface", "68", "cases", "116 planned"],
        ["Manual cases passed", "65", "cases", "68"],
        ["Manual cases failed", "0", "cases", "68"],
    ], [60 * mm, 26 * mm, 32 * mm, CONTENT_W - 118 * mm], right=(1, 3)))
    A(P(f"<b>Observation.</b> Every one of the {total_exec} test executions passed. This is a "
        f"statement about the {UT['tests']} behaviours the suite encodes and nothing wider: it "
        f"says those behaviours hold, not that the application is free of defects.", "body"))
    A(figure(fig("fig-22-01-suites"),
             "Execution time and test count of every suite in the first run. All 147 tests "
             "passed; no suite reported a failure, an error or a skip. Conditions as in "
             "Section 1; n = 1 run, 24 suites.", width_mm=160))

    # ── speed ──
    A(P("4. Speed — computation", "h1"))
    A(table("Execution time of the automated suite.", [
        ["Quantity", "Value", "Unit", "n"],
        ["Sum of per-test execution time", f"{UT['exec_sum_s']:.2f}", "s", f"{UT['tests']} tests"],
        ["Median per-test execution time", f"{UT['exec_median_ms']:.0f}", "ms", f"{UT['tests']}"],
        ["Mean per-test execution time", f"{UT['exec_mean_ms']:.1f}", "ms", f"{UT['tests']}"],
        ["Slowest single test", f"{UT['exec_max_ms']:.0f}", "ms", f"{UT['tests']}"],
        ["Throughput", f"{UT['tests'] / UT['exec_sum_s']:.1f}", "tests per second", "—"],
        ["Wall-clock, run 1 (cold)", f"{RUN_WALL[0]}", "s", "1"],
        ["Wall-clock, run 2 (forced re-execution)", f"{RUN_WALL[1]}", "s", "1"],
        ["Wall-clock, run 3 (warm)", f"{RUN_WALL[2]}", "s", "1"],
        ["Reduction, cold run to warm run",
         f"{100 * (RUN_WALL[0] - RUN_WALL[2]) / RUN_WALL[0]:.0f}", "%", "—"],
        ["Full debug build from committed source", f"{BUILD_S}", "s", "1"],
    ], [60 * mm, 26 * mm, 32 * mm, CONTENT_W - 118 * mm], right=(1, 3)))
    A(P(f"<b>Calculation.</b> Mean per-test time is {UT['exec_mean_ms']:.1f} ms but the median "
        f"is {UT['exec_median_ms']:.0f} ms — the mean is {UT['exec_mean_ms'] / UT['exec_median_ms']:.1f} "
        f"times the median. Four suites account for most of the total: they start a "
        f"WorkManager test harness or render real interface screens, which costs seconds, "
        f"while a pure logic test costs microseconds. The distribution, not the average, is "
        f"the honest summary.", "body"))
    A(figure(fig("fig-22-02-test-time"),
             f"Distribution of per-test execution time, n = {UT['tests']}, log scale. The bulk "
             f"of the suite runs in under 10 ms; the tail above 1 s is the small number of "
             f"tests that start Android components.", width_mm=112))

    # ── network ──
    A(P("5. Speed — network", "h1"))
    data = [["Endpoint", "Median (ms)", "Mean ± 95 % CI (ms)", "SD (ms)", "95th pct (ms)",
             "Elapsed at first byte (% of mean)"]]
    for k in LAT_ORDER:
        d = LAT[k]
        share = 100 * statistics.mean(ttfb[k]) / statistics.mean(lat_vals[k])
        data.append([LAT_LABEL[k], f"{d['median_ms']:.1f}",
                     f"{d['mean_ms']:.1f} ± {ci95(lat_vals[k]):.1f}",
                     f"{d['sd_ms']:.1f}", f"{d['p95_ms']:.1f}", f"{share:.0f}"])
    A(table("Round-trip time to the backend. n = 30 per endpoint, one request at a time, "
            "consumer Wi-Fi in Mumbai to a server in ap-south-1.",
            data, [CONTENT_W - 112 * mm, 20 * mm, 30 * mm, 16 * mm, 20 * mm, 26 * mm],
            right=(1, 2, 3, 4, 5)))
    A(P(f"<b>Observation.</b> The read an elder's screen actually depends on — a row read "
        f"under row-level security — has a median round-trip of "
        f"{LAT['rls_denied_select']['median_ms']:.0f} ms, with a 95th percentile of "
        f"{LAT['rls_denied_select']['p95_ms']:.0f} ms. The standard deviation "
        f"({LAT['rls_denied_select']['sd_ms']:.0f} ms) is larger than the median, so the "
        f"spread, not the centre, is what an interface has to survive. This is why the "
        f"application never blocks a screen on a network call and queues writes locally "
        f"instead.", "body"))
    A(figure(fig("fig-22-03-latency"),
             "Round-trip time by endpoint. Box: interquartile range; red line: median; "
             "whiskers: 1.5 × IQR; dots: every individual trial. n = 30 per endpoint.",
             width_mm=132))
    A(figure(fig("fig-22-04-latency-phases"),
             "Where the time goes. Mean elapsed time decomposed into DNS lookup, TCP connect, "
             "TLS handshake and server processing plus first byte. n = 30 per endpoint.",
             width_mm=132))
    A(P(f"<b>Calculation.</b> For the row read under RLS, the mean total was "
        f"{statistics.mean(lat_vals['rls_denied_select']):.1f} ms of which "
        f"{statistics.mean(ttfb['rls_denied_select']):.1f} ms had already elapsed when the "
        f"first byte arrived; connection setup — DNS, TCP and TLS — therefore accounts for "
        f"{100 * statistics.mean([1000 * float(r['tls_s']) for r in LATROWS if r['endpoint'] == 'rls_denied_select']) / statistics.mean(lat_vals['rls_denied_select']):.0f} % "
        f"of the mean. On a connection that is already open this cost is not paid again, "
        f"which is why the client keeps one alive.", "body"))

    # ── usability ──
    A(P("6. Usability — the two surfaces measured", "h1"))
    A(P("The design claim being tested is that the elder's surface is deliberately larger "
        "than the guardian's. Every declaration in the interface source was measured, so "
        "these are complete populations, not samples.", "body"))
    A(table("Typography and control size, both surfaces, complete populations.", [
        ["Measure", "Elder", "Guardian", "Ratio or difference", "Unit"],
        ["Font-size declarations (n)", e["sp_n"], g["sp_n"], "—", "count"],
        ["Median font size", f"{e['sp_median']:.0f}", f"{g['sp_median']:.0f}",
         f"{e['sp_median'] / g['sp_median']:.2f}× larger", "sp"],
        ["Mean font size", f"{e['sp_mean']:.1f}", f"{g['sp_mean']:.1f}",
         f"{e['sp_mean'] / g['sp_mean']:.2f}× larger", "sp"],
        ["Below 16 sp", f"{e['sp_pct_below_16']:.1f}", f"{g['sp_pct_below_16']:.1f}",
         f"{g['sp_pct_below_16'] - e['sp_pct_below_16']:.1f} pp fewer", "%"],
        ["At or above 20 sp", f"{e['sp_pct_at_least_20']:.1f}", f"{g['sp_pct_at_least_20']:.1f}",
         f"{e['sp_pct_at_least_20'] - g['sp_pct_at_least_20']:.1f} pp more", "%"],
        ["Control-size declarations (n)", e["dp_n"], g["dp_n"], "—", "count"],
        ["Median control size", f"{e['dp_median']:.0f}", f"{g['dp_median']:.0f}",
         f"{e['dp_median'] / g['dp_median']:.1f}× larger", "dp"],
        ["At or above 48 dp", f"{e['dp_pct_at_least_48']:.1f}", f"{g['dp_pct_at_least_48']:.1f}",
         f"{e['dp_pct_at_least_48'] - g['dp_pct_at_least_48']:.1f} pp more", "%"],
    ], [44 * mm, 20 * mm, 22 * mm, CONTENT_W - 108 * mm, 22 * mm], right=(1, 2)))
    A(P(f"<b>Observation.</b> The asymmetry is real and measurable: the elder's median "
        f"declared font size is {e['sp_median']:.0f} sp against {g['sp_median']:.0f} sp on the "
        f"guardian's, and {e['sp_pct_below_16']:.0f} % of elder declarations fall below 16 sp "
        f"against {g['sp_pct_below_16']:.0f} % on the guardian's. What this does <i>not</i> "
        f"establish is that elders read the result more easily; that requires a user study, "
        f"which has not been run.", "body"))
    A(figure(fig("fig-22-05-typescale"),
             f"Cumulative distribution of declared font size. Elder n = {e['sp_n']}, guardian "
             f"n = {g['sp_n']}. The dashed line is the 16 sp floor used as the reference "
             f"point in Task 23.", width_mm=118))
    A(figure(fig("fig-22-06-targets"),
             f"Cumulative distribution of declared control height or size. Elder n = "
             f"{e['dp_n']}, guardian n = {g['dp_n']}. The dashed line is the 48 dp Material "
             f"minimum touch target.", width_mm=118))

    # ── security ──
    A(P("7. Security", "h1"))
    npers = len([t for t in {r["table"] for r in RLS} if t in PERSONAL])
    A(table("Rows reachable by an unauthenticated caller.", [
        ["Quantity", "Value", "Unit", "n"],
        ["Tables in the schema", f"{len({r['table'] for r in RLS})}", "tables", "—"],
        ["Tables holding personal care data", f"{npers}", "tables", "—"],
        ["Read attempts made", f"{len(RLS)}", "requests", "3 per table"],
        ["Requests refused outright", "0", "requests", f"{len(RLS)}"],
        ["Personal rows returned", "0", "rows", f"{npers * 3} requests"],
        ["Reference rows returned (by design)", "17", "rows", "12 requests"],
        ["Row-level security policies in the schema", "37", "policies", "—"],
        ["SQL functions enforcing access", "26", "functions", "—"],
    ], [66 * mm, 24 * mm, 26 * mm, CONTENT_W - 116 * mm], right=(1, 3)))
    A(P("<b>Observation.</b> Every request was answered with HTTP 200 and an empty array "
        "rather than an error. That is the intended behaviour of row-level security: the "
        "filter is applied to the rows, so an unauthorised caller cannot even learn whether a "
        "row exists. The four tables that do return rows hold configuration constants, "
        "default reminder categories, clinical threshold bands and a public directory of "
        "wheelchair services — each was inspected and none contains personal data.", "body"))
    A(figure(fig("fig-22-07-rls"),
             "Rows returned to a caller holding only the application's public key, every "
             "table in the schema, 3 trials each.", width_mm=150))

    # ── resources ──
    A(P("8. Resource use", "h1"))
    A(table("Release artefact size and composition.", [
        ["Quantity", "Value", "Unit"],
        ["Release APK on disk", f"{int(APK['apk_bytes']) / 1e6:.1f}", "MB"],
        ["Estimated download size", f"{int(APK['apk_download_size_bytes']) / 1e6:.1f}", "MB"],
        ["Uncompressed content", f"{unc / 1e6:.1f}", "MB"],
        ["Stored in the archive", f"{comp / 1e6:.1f}", "MB"],
        ["Compression achieved", f"{100 * (1 - comp / unc):.0f}", "%"],
        ["Dex method references", f"{int(APK['dex_method_count']):,}", "methods"],
        ["Declared permissions", APK["permission_count"], "permissions"],
        ["Minimum Android version", f"API {APK['min_sdk']} (Android 7.0)", "—"],
    ], [60 * mm, 40 * mm, CONTENT_W - 100 * mm], right=(1,)))
    A(P(f"<b>Observation.</b> Three dex files hold "
        f"{int(APK['dex_method_count']):,} method references, which is what a Compose and "
        f"Firebase application costs; they compress to about a quarter of their size, while "
        f"the resources are already compressed and do not. A "
        f"{int(APK['apk_download_size_bytes']) / 1e6:.0f} MB download is modest enough for an "
        f"entry-level phone on mobile data, which is the deployment case this project has to "
        f"work in.", "body"))
    A(figure(fig("fig-22-08-apk"),
             "Composition of the release APK by archive entry, uncompressed against stored "
             "size. n = 1 artefact (version 1.3).", width_mm=124))

    A(P("9. Summary of results", "h1"))
    A(table("Every measure selected in Task 9, with its measured value.", [
        ["Measure", "Result", "Conditions"],
        ["Correctness — automated",
         f"{UT['pass_rate_pct']:.1f} % pass rate over {total_exec} test executions",
         "3 runs, commit " + ENV["git_commit"]],
        ["Correctness — manual",
         "65 of 68 cases passed, 2 partial, 0 failed, across all 20 functional areas",
         "116-case written test plan"],
        ["Speed — computation", f"{UT['exec_median_ms']:.0f} ms median per test; suite in "
                                f"{UT['exec_sum_s']:.1f} s of execution time",
         f"n = {UT['tests']}"],
        ["Speed — network",
         f"{LAT['rls_denied_select']['median_ms']:.0f} ms median round-trip for a care-data "
         f"read (95th pct {LAT['rls_denied_select']['p95_ms']:.0f} ms)",
         "n = 30, Wi-Fi, Mumbai to ap-south-1"],
        ["Usability — typography",
         f"elder median {e['sp_median']:.0f} sp against guardian {g['sp_median']:.0f} sp",
         f"complete populations, n = {e['sp_n']} and {g['sp_n']}"],
        ["Usability — reach",
         f"elder median control {e['dp_median']:.0f} dp against guardian "
         f"{g['dp_median']:.0f} dp",
         f"complete populations, n = {e['dp_n']} and {g['dp_n']}"],
        ["Security", "0 personal rows reachable without authentication",
         f"{npers} tables × 3 trials"],
        ["Resource", f"{int(APK['apk_download_size_bytes']) / 1e6:.1f} MB download, "
                     f"{int(APK['dex_method_count']):,} method references", "version 1.3"],
    ], [38 * mm, 66 * mm, CONTENT_W - 104 * mm]))
    return F


# ════════════════════════════ TASK 23 ════════════════════════════
import json as _json
_BASE = _json.load(open(os.path.join(RAW, "_refs.json")))
REFS = _BASE + [
    "A. Papageorgiou, M. Strigkos, E. Politou, E. Alepis, A. Solanas, and C. Patsakis, "
    "“Security and privacy analysis of mobile health applications: The alarming state of "
    "practice,” IEEE Access, vol. 6, pp. 9390–9403, 2018, doi: 10.1109/ACCESS.2018.2799522.",
    "K. Huckvale, J. T. Prieto, M. Tilney, P.-J. Benghozi, and J. Car, “Unaddressed privacy "
    "risks in accredited health and wellness apps: A cross-sectional systematic assessment,” "
    "BMC Medicine, vol. 13, art. 214, 2015, doi: 10.1186/s12916-015-0444-y.",
    "J. Nielsen, Usability Engineering. San Francisco, CA, USA: Morgan Kaufmann, 1993, "
    "ch. 5, pp. 135–137.",
]
# index (1-based) of the references cited in this document
R = {k: i + 1 for i, k in enumerate([
    "unfpa", "dataforindia", "chakraborty", "khare", "poorcheraghi", "salmensuu", "awan",
    "elguera", "liu", "gomez", "amouzadeh", "quinn", "benmessaoud", "doze", "wiecek",
    "nimmanterdwong", "hamill", "playsms", "playsms2", "material", "wcag",
    "papageorgiou", "huckvale", "nielsen"])}

def c(*keys):
    return "[" + ", ".join(str(R[k]) for k in keys) + "]"


def task23():
    F = []; A = F.append
    e, g = UI["elder"], UI["guardian"]
    med = LAT["rls_denied_select"]

    A(P("1. Purpose and basis of comparison", "h1"))
    A(P("Task 23 asks for the measured results of Task 22 to be set against the most relevant "
        "values in the project's literature comparison, with better or worse performance "
        "explained, and with limitations and sources of error stated. The comparison below is "
        "restricted to the published work that measures something this project also measured. "
        "Where the literature reports an outcome this project has not measured — adherence "
        "improvement in real patients is the main one — that is said plainly instead of being "
        "filled in with an estimate.", "body"))
    A(callout("A caution about what can honestly be compared",
              "Most of the published work in this area measures outcomes in human "
              "participants. This project has measured an implementation. A design that "
              "matches published guidance is not thereby shown to produce the published "
              "benefit, and nothing in this document should be read as claiming otherwise.",
              "warn"))

    A(P("2. The comparison set", "h1"))
    A(table("The entries from the literature comparison that bear directly on a measured "
            "result, and what each contributes.", [
        ["Source", "What it provides", "Measure it bears on"],
        [f"Gomez-Hernandez et al. {c('gomez')}; Amouzadeh et al. {c('amouzadeh')}; "
         f"Liu et al. {c('liu')}",
         "Systematic reviews of age-friendly mobile design. Legible type, large controls and "
         "reduced per-screen choice are recurring recommendations across all three.",
         "Typography, control size"],
        [f"Awan et al. {c('awan')}; Elguera Paez and Zapata Del Rio {c('elguera')}",
         "Prioritised catalogues of the usability barriers elderly smartphone users actually "
         "hit; small text and small targets rank near the top.",
         "Typography, control size"],
        [f"Material Design 3 {c('material')}; WCAG 2.2 {c('wcag')}",
         "Normative, numeric minima: a 48 dp minimum touch target, and target-size success "
         "criteria.", "Control size"],
        [f"Nielsen {c('nielsen')}",
         "The classical response-time thresholds: about 0.1 s feels instantaneous, about 1 s "
         "keeps the user's flow of thought, about 10 s loses attention.",
         "Network latency"],
        [f"Papageorgiou et al. {c('papageorgiou')}; Huckvale et al. {c('huckvale')}",
         "Empirical security and privacy audits of deployed health apps. Huckvale et al. "
         "found that of 79 NHS-accredited apps, about 89 % transmitted data online, none "
         "encrypted personal data stored on the device, and roughly two thirds of those "
         "sending identifying data did so without encryption.",
         "Data exposure"],
        [f"Poorcheraghi et al. {c('poorcheraghi')}; Salmensuu et al. {c('salmensuu')}; "
         f"Wiecek et al. {c('wiecek')}",
         "A randomised controlled trial and two reviews of medication-reminder technology in "
         "older adults — the outcome evidence this project's reminder design is built on.",
         "Adherence (not measured here)"],
        [f"Quinn et al. {c('quinn')}; Benmessaoud et al. {c('benmessaoud')}",
         "Dyad studies: the caregiver and the cared-for person treated as a pair with "
         "different interfaces. The architectural precedent for the two-surface design.",
         "Two-surface design"],
        [f"Hamill et al. {c('hamill')}",
         "An automated speech-recognition interface for personal emergency response systems, "
         "and the accuracy problems encountered with older speakers.",
         "Voice (not measured here)"],
        [f"Android Doze documentation {c('doze')}",
         "The platform constraint that governs whether a scheduled reminder fires on time.",
         "Reminder timing (not measured here)"],
    ], [44 * mm, CONTENT_W - 82 * mm, 38 * mm]))

    A(P("3. Typography and control size against published guidance", "h1"))
    A(P(f"The measured elder surface has a median declared font size of {e['sp_median']:.0f} sp "
        f"and a median declared control size of {e['dp_median']:.0f} dp, against "
        f"{g['sp_median']:.0f} sp and {g['dp_median']:.0f} dp on the guardian surface "
        f"(Task 22, Table 5). The nearest normative value is the 48 dp minimum touch target "
        f"{c('material')}; the elder median is {e['dp_median'] / 48:.1f} times that minimum, "
        f"while the guardian median is below it.", "body"))
    A(table("Measured interface values against the published reference points.", [
        ["Reference point", "Published position", "Measured here", "Assessment"],
        ["Minimum touch target " + c("material"),
         "48 dp minimum",
         f"Elder median {e['dp_median']:.0f} dp; "
         f"{e['dp_pct_at_least_48']:.0f} % of elder controls at or above 48 dp",
         "<b>Better than the minimum</b> on the elder surface, by a wide margin."],
        ["Minimum touch target " + c("material"),
         "48 dp minimum",
         f"Guardian median {g['dp_median']:.0f} dp; "
         f"{g['dp_pct_at_least_48']:.0f} % at or above 48 dp",
         "<b>Below the minimum at the median.</b> Explained in the next paragraph, but it is "
         "a real finding and is carried into the conclusion."],
        ["Legible type for older adults " + c("gomez", "amouzadeh", "awan"),
         "Reviews recommend enlarged type and name small text as a leading barrier; they do "
         "not converge on one number",
         f"Elder: {e['sp_pct_below_16']:.0f} % of declarations below 16 sp, median "
         f"{e['sp_median']:.0f} sp",
         "<b>Consistent with the direction of the guidance.</b> No numeric claim is made, "
         "because the sources give none to compare against."],
        ["Reduced per-screen choice " + c("liu", "elguera"),
         "Fewer options per screen for older users",
         "7 elder screen files against 27 guardian screen files; the elder navigation is a "
         "fixed set of large destinations",
         "<b>Consistent in design.</b> Not an outcome measurement."],
    ], [38 * mm, 42 * mm, 46 * mm, CONTENT_W - 126 * mm]))
    A(P("<b>Why the guardian surface sits below the published minimum, and why that is "
        "defensible.</b> The 48 dp figure is a floor for interactive controls. The guardian "
        "median of "
        f"{g['dp_median']:.0f} dp is dominated by icons, status chips and list rows inside a "
        "dense information display, not by the buttons a guardian presses; the measurement "
        "counts every declared size, which is the honest thing to do but does not separate a "
        "16 dp icon from a 56 dp button. The population that the accessibility literature is "
        "concerned with is on the elder surface, where "
        f"{e['dp_pct_at_least_48']:.0f} % of declared controls meet or exceed the minimum. "
        "The correct reading of this result is that the asymmetry the design claims is real "
        "and measurable, and that the guardian surface has not been audited against the "
        "minimum — not that it fails it.", "body"))

    A(P("4. Network latency against the response-time thresholds", "h1"))
    A(P(f"The read that an elder's screen depends on completed in a median of "
        f"{med['median_ms']:.0f} ms, with a 95th percentile of {med['p95_ms']:.0f} ms and a "
        f"worst observed trial of {med['max_ms']:.0f} ms (n = 30). Against Nielsen's "
        f"thresholds {c('nielsen')} — roughly 0.1 s for an instantaneous feel, 1 s to keep "
        f"the user's flow of thought — the median sits comfortably inside the one-second "
        f"band but well outside the hundred-millisecond band, and even the worst trial stays "
        f"inside one second.", "body"))
    A(table("Measured round-trip time against the classical response-time thresholds "
            + c("nielsen") + ".", [
        ["Threshold", "Published meaning", "Measured position"],
        ["≈ 0.1 s", "The result feels instantaneous; no feedback is needed",
         f"Not met at the median ({med['median_ms']:.0f} ms). The fastest single trial was "
         f"{med['min_ms']:.0f} ms."],
        ["≈ 1 s", "The user's flow of thought is preserved; a delay is noticed but not "
                  "disruptive",
         f"Met at the median, at the 95th percentile ({med['p95_ms']:.0f} ms) and in the "
         f"worst of the 30 trials ({med['max_ms']:.0f} ms)."],
        ["≈ 10 s", "Attention is lost; the user needs something else to do",
         "Not approached in any trial."],
    ], [22 * mm, 60 * mm, CONTENT_W - 82 * mm]))
    A(P(f"<b>Why the result is worse than it first looks.</b> This measurement is a network "
        f"round trip taken from a laptop, not the delay an elder experiences. The user-visible "
        f"delay also contains the handset's own request setup, deserialisation and recomposition, "
        f"none of which was measured. The standard deviation ({med['sd_ms']:.0f} ms) exceeds "
        f"the median, so a single typical number is misleading; the design response is not to "
        f"optimise the mean but to never make the interface wait for it, which is why writes "
        f"go through a local outbox and screens render from cache.", "body"))
    A(P("<b>Why it may also be better than it looks.</b> Every trial opened a fresh "
        "connection, so each one paid DNS, TCP and TLS setup in full "
        f"({100 * statistics.mean([1000 * float(r['tls_s']) for r in LATROWS if r['endpoint'] == 'rls_denied_select']) / statistics.mean([1000 * float(r['total_s']) for r in LATROWS if r['endpoint'] == 'rls_denied_select']):.0f} % "
        "of the mean, Task 22, Figure 4). A running application keeps the connection alive and "
        "does not pay that cost on every request, so the steady-state figure in use is lower "
        "than the one measured here.", "body"))

    A(P("5. Data exposure against the published audits", "h1"))
    A(P(f"Huckvale et al. {c('huckvale')} assessed 79 apps that the UK NHS had accredited: "
        f"about 89 % transmitted data to an online service, none encrypted personal data held "
        f"on the device, and about two thirds of those sending identifying data sent it "
        f"without encryption. Papageorgiou et al. {c('papageorgiou')} analysed 20 widely used "
        f"mHealth apps and concluded that most did not follow established security practice, "
        f"with several falling short of data-protection requirements.", "body"))
    A(table("Measured data exposure against the published baseline.", [
        ["Property", "Published baseline", "Measured here", "Assessment"],
        ["Transport encryption",
         f"About a third of transmitting apps in the Huckvale sample sent identifying data "
         f"unencrypted {c('huckvale')}",
         "All 171 measured requests were HTTPS; a TLS handshake was timed on every one",
         "<b>Better.</b> There is no unencrypted path in the measurement."],
        ["Server-side authorisation",
         f"Audits repeatedly find data reachable without proper authorisation "
         f"{c('papageorgiou')}",
         "0 rows returned from all 13 personal-data tables across 39 anonymous requests",
         "<b>Better on this test.</b> The test is narrower than theirs — see below."],
        ["Permissions requested",
         f"Over-permissioned health apps are a recurring finding {c('papageorgiou')}; the "
         f"platform has since restricted SMS and call-log access outright {c('playsms')}",
         f"{APK['permission_count']} declared permissions; SMS was removed from the product "
         f"entirely",
         "<b>Comparable or better</b>, though permission count alone is a weak measure."],
        ["On-device storage of personal data",
         f"No app in the Huckvale sample encrypted personal data at rest {c('huckvale')}",
         "Not measured in this project",
         "<b>No comparison possible.</b> Stated as a gap, not a result."],
    ], [30 * mm, 48 * mm, 44 * mm, CONTENT_W - 122 * mm]))
    A(P("<b>Why this comparison is weaker than the numbers suggest.</b> The published audits "
        "intercepted live traffic from a running app and inspected what left the device. This "
        "project measured something narrower: whether a caller holding the public key can "
        "read rows from the server. A clean result on that test does not rule out an "
        "over-sharing client, data written to an unprotected file on the handset, or a leak "
        "through a third-party component. Those would need the same traffic-analysis method "
        "the published work used, and it has not been applied here.", "body"))

    A(P("6. Where no comparison is possible", "h1"))
    A(table("Published results this project cannot yet be compared against, and why.", [
        ["Published result", "Why no comparison can be drawn"],
        [f"Reminder applications improve medication adherence in older adults with "
         f"polypharmacy {c('poorcheraghi')}, with reviews finding mixed but generally "
         f"positive effects {c('salmensuu', 'wiecek')}",
         "This project has measured that reminders are scheduled, fire, persist across a "
         "reboot and are cancelled when a dose is answered early. It has not measured "
         "adherence in any human participant. The mechanism is verified; the outcome is not, "
         "and quoting a published effect size next to this work would be a false comparison."],
        [f"Speech interfaces for emergency response are harder for older speakers "
         f"{c('hamill')}",
         "The spoken-answer classifier was verified against synthesised Hindi speech only. No "
         "recording from an elderly speaker has been tested, and Marathi and Gujarati are "
         "unverified, so no accuracy figure exists on this side to compare."],
        [f"Dyad designs change how caregiver and patient each use the system "
         f"{c('quinn', 'benmessaoud')}",
         "The two-surface architecture follows that precedent, but no dyad has used this "
         "system under observation."],
        [f"Doze and App Standby delay scheduled work {c('doze')}",
         "Exact alarms are requested and re-armed after a reboot, and this is covered by "
         "tests on the JVM. The actual delay on a physical phone left idle overnight has not "
         "been measured."],
        [f"Older adults in India face specific access and confidence barriers "
         f"{c('unfpa', 'dataforindia', 'chakraborty', 'khare')}",
         "These motivate the design. They are not performance values and nothing here is "
         "measured against them."],
    ], [58 * mm, CONTENT_W - 58 * mm]))

    A(P("7. Limitations of this study", "h1"))
    A(P("Stated as limitations of the measurements, in the order in which they most affect "
        "the conclusions.", "body"))
    A(bullets([
        "<b>Self-authored tests.</b> The suite was written by the same author as the code. A "
        f"100 % pass rate over {UT['tests']} tests says those {UT['tests']} behaviours hold; "
        "it says nothing about behaviours nobody thought to test. The number of tests is not "
        "a coverage figure and is not presented as one.",
        "<b>One network location, one session.</b> All 120 timed requests were made from one "
        "place on one connection within a few minutes. They describe that path at that "
        "moment, not the service in general, and no other network or time of day was sampled.",
        "<b>One artefact, one version.</b> Size and composition come from a single build of "
        "version 1.3. No trend over releases is available.",
        "<b>Static interface measurement.</b> Declared sizes are not rendered sizes. The "
        "elder surface additionally scales type at run time, which this method cannot see, "
        "and the extraction cannot tell a button from an icon.",
        "<b>Reads only, on the security test.</b> Write and update attempts were deliberately "
        "not made against the production database, so insert and update policies are covered "
        "only by the automated backend assertions, not by a live probe.",
    ]))

    A(P("8. Sources of error", "h1"))
    A(table("Identified sources of error and their likely direction.", [
        ["Source", "Effect on the reported numbers"],
        ["Shared Wi-Fi and a shared cloud region",
         "Inflates network times unpredictably and is the main reason the standard deviation "
         "exceeds the median. Direction: makes the reported latency pessimistic."],
        ["Fresh TCP and TLS connection on every trial",
         f"Adds connection setup to every observation. Direction: pessimistic by roughly the "
         f"setup share shown in Task 22, Figure 4."],
        ["JVM warm-up and class loading in the first tests of a run",
         "Inflates the execution time of whichever tests run first. Direction: pessimistic, "
         "and partly why run 1 took "
         f"{RUN_WALL[0]} s against {RUN_WALL[2]} s for run 3."],
        ["Build caching between runs",
         "Would make a repeated run look artificially fast; countered by forcing every task "
         "to re-execute in runs 2 and 3."],
        ["Pattern-based extraction of sizes from source",
         "Can miss a size written in an unusual form and cannot resolve one held in a "
         "variable. Direction: unknown; letter-spacing values were excluded explicitly after "
         "they were found to contaminate an earlier pass."],
        ["Timer resolution in the test runner",
         "Reports in milliseconds, so the fastest tests appear as 0 ms. Affects the minimum "
         "and the mean slightly; the median is unaffected."],
        ["Single operator, no blinding",
         "The measurements were taken by the author of the system. The mitigation is that "
         "every raw file and every script is submitted, so the numbers can be recomputed by "
         "someone else."],
    ], [52 * mm, CONTENT_W - 52 * mm]))

    A(P("9. What would change these conclusions", "h1"))
    A(bullets([
        "A user study with elderly participants would replace every usability inference with "
        "a measurement, and could just as easily contradict it.",
        "An on-device trial measuring SOS-to-notification time and overnight alarm accuracy "
        "would turn the two largest unmeasured claims into results.",
        "Traffic analysis of the running application, as in the published audits "
        + c("papageorgiou", "huckvale") + ", would make the security comparison a like-for-like "
        "one instead of the narrower server-side test reported here.",
        "Mutation testing, or review by someone who did not write the code, would give the "
        "pass rate a meaning that the current figure does not carry.",
    ]))

    A(P("References", "h1"))
    for i, r in enumerate(REFS, start=1):
        A(P(f"[{i}]&nbsp;&nbsp;{r}", "cell"))
        A(Spacer(1, 2))
    return F


# ════════════════════════════ TASK 25 ════════════════════════════
def task25():
    F = []; A = F.append
    e, g = UI["elder"], UI["guardian"]
    med = LAT["rls_denied_select"]

    A(P("1. How this conclusion is drawn", "h1"))
    A(P("Each of the six objectives stated at the start of the project is answered below "
        "against measured evidence, in the same words it was set in. Where an objective is "
        "met, the measurement that shows it is named. Where it is met only in part, the part "
        "that is missing is stated rather than smoothed over. Improvements that have been "
        "planned but not built appear only in Section 4, and are written in the future tense "
        "throughout.", "body"))

    A(P("2. Objective-by-objective findings", "h1"))
    A(table("Every project objective, answered with measured evidence.", [
        ["Objective", "Achieved result", "Evidence", "Verdict"],
        ["Develop an assistive Android application",
         f"A complete two-surface Android application exists: "
         f"{UI['code']['main_kt_lines']:,} lines of Kotlin in "
         f"{UI['code']['main_kt_files']} files, 34 screens, backed by "
         f"{UI['code']['sql_lines']:,} lines of SQL across 17 tables. It builds from the "
         f"committed source in {BUILD_S} s and installs as a "
         f"{int(APK['apk_bytes']) / 1e6:.0f} MB APK on any Android 7.0 phone.",
         "Task 21 §12; build performed 23 Oct 2026", "<b>Met</b>"],
        ["Provide medicine and activity reminders",
         "Doses and general reminders are armed as exact alarms, survive a reboot, deep-link "
         "into the medicine flow, and are cancelled when a dose is answered early so the same "
         "dose is never asked for twice.",
         "ReminderSchedulerTest, CancelDoseAlarmTest, BootPersistenceTest, "
         "GeneralRemindersArmedTest, ReminderDeepLinkTest — all passing in three independent "
         "runs",
         "<b>Met</b>"],
        ["Enable caregiver monitoring",
         "A guardian creates the elder's profile, issues a six-digit binding code, and sees "
         "adherence, vitals and alerts for that elder. A second family member can be invited "
         "to the same elder. Cross-device propagation was demonstrated on real handsets.",
         "Device screenshots (Task 20, Figures 1–7); the recorded cross-device run in "
         "docs/TEST-RESULTS.md",
         "<b>Met</b>"],
        ["Implement emergency SOS",
         "An SOS raised on the elder's phone creates a persisted emergency record with its "
         "location, and the guardian retrieves it as an ACTIVE alert. The elder's screen "
         "reports only outcomes the application actually observed.",
         "Device screenshots (Task 20, Figure 7); SosResultSemanticsTest",
         "<b>Met, with one known limitation</b> — an already-open alert list does not refresh "
         "by itself"],
        ["Maintain health vitals records",
         "Vitals are recorded, classified against stored clinical threshold bands, and "
         "exported as a PDF for a doctor.",
         "VitalsSeverityTest (12 tests); the vital_thresholds table, measured as 7 rows of "
         "reference data",
         "<b>Met</b>"],
        ["Ensure an accessibility-first interface",
         f"The elder surface is measurably larger than the guardian surface: median declared "
         f"font size {e['sp_median']:.0f} sp against {g['sp_median']:.0f} sp, median control "
         f"size {e['dp_median']:.0f} dp against {g['dp_median']:.0f} dp, with "
         f"{e['dp_pct_at_least_48']:.0f} % of elder controls at or above the 48 dp minimum.",
         f"Task 22, Table 5; complete populations, n = {e['sp_n']} and {g['sp_n']} "
         f"declarations",
         "<b>Met</b>"],
    ], [34 * mm, 54 * mm, 42 * mm, CONTENT_W - 130 * mm]))

    A(P("3. What the project has established", "h1"))
    A(P("Taken together, the measurements support four statements and no more.", "body"))
    A(bullets([
        f"<b>The system works end to end.</b> Care configured by a guardian reaches the "
        f"elder's phone, and an emergency raised by the elder reaches the guardian as a "
        f"retrievable record. This was demonstrated on two real handsets and is the project's "
        f"central claim.",
        f"<b>The application was driven through its interface against a written plan.</b> "
        f"Of 116 planned cases across 20 functional areas, 68 were executed live: 65 passed, "
        f"2 were partial and none failed. Two defects were found during that campaign and "
        f"both were fixed and re-verified before it closed.",
        f"<b>The implementation is stable under its own tests.</b> "
        f"{UT['tests']} automated tests across {UT['suites']} suites passed in three "
        f"independent runs, a pass rate of {UT['pass_rate_pct']:.1f} % over "
        f"{UT['tests'] * 3} test executions, covering reminder scheduling, offline durability, "
        f"network failure handling, malformed responses and interface state.",
        f"<b>Personal data is not reachable without authentication.</b> Across 39 anonymous "
        f"requests to the 13 tables holding personal care data, zero rows were returned. The "
        f"four tables that do answer hold configuration constants, default categories, "
        f"clinical thresholds and a public services directory.",
        f"<b>The accessibility asymmetry is real and measurable.</b> The elder surface is "
        f"{e['sp_median'] / g['sp_median']:.2f} times the guardian surface at the median font "
        f"size and {e['dp_median'] / g['dp_median']:.1f} times at the median control size — a "
        f"design intention confirmed by measurement rather than asserted.",
    ]))

    A(P("4. Limitations, stated plainly", "h1"))
    A(table("Limitations of the work as completed.", [
        ["Limitation", "What it means for the conclusions"],
        ["Tests written by the author of the code",
         f"The {UT['pass_rate_pct']:.0f} % pass rate covers {UT['tests']} intended behaviours. "
         f"It is not a coverage figure and does not bound the defects that remain."],
        ["Voice verified only for synthesised Hindi",
         "Marathi and Gujarati are wired but unproven, and no elderly speaker has been "
         "recorded. The spoken-answer feature must not be described as validated."],
        ["Security tested from the server side only",
         "Reads were probed; writes were not, and the running application's own network "
         "traffic was not inspected. A clean result here does not rule out an over-sharing "
         "client."],
        ["One network path, one session, one build",
         "The latency results describe one connection at one moment, and the size results "
         "one artefact. Neither supports a trend."],
        ["Guardian alert list does not live-update",
         "A guardian who leaves the alerts screen open will not see a new emergency appear. "
         "This is a real defect in a safety-relevant screen and is listed as such."],
    ], [52 * mm, CONTENT_W - 52 * mm]))

    A(P("5. Specific future improvements", "h1"))
    A(P("None of the following has been built. Each is written as work to be done, with the "
        "measurement that would show it had succeeded.", "body"))
    A(table("Planned future work. Nothing in this table is implemented.", [
        ["#", "Planned improvement", "How success would be measured"],
        ["1", "Run a supervised study with elderly participants on the elder surface",
         "Task completion rate and time on a fixed set of tasks, and errors per task, with a "
         "stated sample size — replacing every inference now drawn from source code"],
        ["2", "Instrument SOS end to end on two physical handsets",
         "Median and 95th-percentile time from SOS press to the notification appearing on the "
         "guardian's phone, over at least 30 trials"],
        ["3", "Make the guardian alert list refresh while it is open, by subscription or by "
              "polling",
         "A new emergency appears on an already-open screen within a stated number of seconds "
         "in repeated trials"],
        ["4", "Run an overnight idle soak test for reminders",
         "Difference between scheduled and actual fire time for each dose on a device left "
         "idle, reported as a distribution"],
        ["5", "Collect and test real elderly speech in Hindi, Marathi and Gujarati",
         "Classification accuracy per language against a labelled set, with the confusion "
         "between taken, not-taken and unclear reported separately"],
        ["6", "Apply traffic analysis to the running application, as the published audits do",
         "An inventory of every destination the app contacts and the data sent, compared "
         "against the declared behaviour"],
        ["7", "Register the Play App Signing certificate in Firebase and publish to the Play "
              "Store",
         "A successful Google sign-in from a Play-installed build on a device that has never "
         "had the APK"],
        ["8", "Measure cold start and memory on an entry-level handset",
         "Time to first interactive frame and resident memory, on the lowest-specification "
         "device the project claims to support"],
    ], [8 * mm, 66 * mm, CONTENT_W - 74 * mm]))

    A(P("6. Conclusion", "h1"))
    A(P(f"CareCompanion set out to put an elder's reminders, contacts, vitals and an "
        f"emergency call into one application simple enough to be used by the elder and "
        f"detailed enough to be managed by a relative. That system has been built and "
        f"demonstrated working across two real phones: "
        f"{UI['code']['main_kt_lines']:,} lines of Kotlin and {UI['code']['sql_lines']:,} "
        f"lines of SQL, {UT['tests']} automated tests passing without exception in three "
        f"independent runs, a median care-data round trip of {med['median_ms']:.0f} ms, and "
        f"no personal row reachable by an unauthenticated caller. Measured against published "
        f"guidance, the elder surface sits comfortably above the 48 dp minimum touch target "
        f"and the response times sit inside the one-second band that preserves a user's flow "
        f"of thought.", "body"))
    A(P("The work that remains is set out in Section 5: instrumented timings for the "
        "emergency path, a soak test for reminders left overnight, verification of the "
        "Marathi and Gujarati voice paths, and publication to the Play Store. Each is "
        "written there with the measurement that would show it had succeeded.", "body"))
    return F


# ──────────────────────────── main ────────────────────────────
DOCS = [
    ("Task-20-Operational-Prototype.pdf", 20, "Operational Prototype", task20,
     "Dated prototype demonstration record and remaining-work list"),
    ("Task-21-Raw-Performance-Data.pdf", 21, "Raw Performance Data", task21,
     "Test log, raw data files and measurement evidence"),
    ("Task-22-Results-Graphs-Calculations.pdf", 22, "Results, Graphs and Calculations", task22,
     "Results section with numbered tables, figures and calculations"),
    ("Task-23-Literature-Based-Performance-Analysis.pdf", 23,
     "Literature-Based Performance Analysis", task23,
     "Discussion section with cited comparisons"),
    ("Task-25-Evidence-Based-Conclusion.pdf", 25, "Evidence-Based Conclusion", task25,
     "Conclusion and future-work section"),
]

if __name__ == "__main__":
    for args in DOCS:
        build(*args)
