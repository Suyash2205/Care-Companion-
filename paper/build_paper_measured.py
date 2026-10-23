"""
build_paper_measured.py
-----------------------
Task 24 — the preprint manuscript, in IEEE two-column format.

This is a separate paper from the design paper in build_paper_impact.py. Its subject is
the measurement study: the protocol, the raw observations, and what they do and do not
support when set beside the published literature. The layout helpers are reused from
build_paper.py so both manuscripts look identical.

    python paper/build_paper_measured.py
"""
import os, sys, json, csv, statistics, math

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "docs"))

import build_paper as bp
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.units import inch
from reportlab.platypus import (BaseDocTemplate, Frame, Image, KeepTogether, PageTemplate,
                                Paragraph, Spacer)
from PIL import Image as PILImage

RAW = os.path.join(ROOT, "measurements", "raw")
MFIGS = os.path.join(ROOT, "measurements", "figs")
OUT = os.path.expanduser("~/Desktop/CareCompanion-Tasks-20-27/"
                         "Task-24-Preprint-Manuscript.pdf")

P, H, SH, bullets, table = bp.P, bp.H, bp.SH, bp.bullets, bp.table

# The shared TITLE style hyphenates, which breaks a hyphenated title across lines.
TITLE_NOHYPH = bp.st("title_nohyph", fontName="Times-Bold", fontSize=19, leading=22,
                     alignment=1, spaceAfter=9, hyphenationLang="", embeddedHyphenation=0)

def rows(n):
    with open(os.path.join(RAW, n)) as fh:
        return list(csv.DictReader(fh))
def jload(n):
    return json.load(open(os.path.join(RAW, n)))
def kv(n):
    return {r["key"]: r["value"] for r in rows(n)}

ENV = kv("environment.csv"); UT = jload("unit_test_summary.json")
LAT = jload("backend_latency_summary.json"); LATROWS = rows("backend_latency.csv")
UI = jload("ui_summary.json"); APK = kv("apk_metrics.csv"); RLS = rows("rls_anon_reads.csv")
SUITES = rows("unit_test_suites.csv")
E, G = UI["elder"], UI["guardian"]
MED = LAT["rls_denied_select"]
PERSONAL = {"adherence_logs", "alerts", "contacts", "elders", "guardian_elder_links",
            "invite_codes", "medicine_schedules", "medicines", "ott_shortcuts",
            "reminders", "sos_events", "users", "vitals"}

def ci95(v):
    return 1.96 * statistics.stdev(v) / math.sqrt(len(v))

_lat = lambda k: [1000 * float(r["total_s"]) for r in LATROWS if r["endpoint"] == k]

def mfig(name, num, caption, width=None):
    path = os.path.join(MFIGS, name + ".png")
    w, h = PILImage.open(path).size
    dw = (width or bp.COLW) - 6
    return KeepTogether([Spacer(1, 4), Image(path, width=dw, height=dw * h / w),
                         Paragraph(f"Fig. {num}.&nbsp; {caption}", bp.CAP)])

REFS = json.load(open(os.path.join(RAW, "_refs.json"))) + [
    "A. Papageorgiou, M. Strigkos, E. Politou, E. Alepis, A. Solanas, and C. Patsakis, "
    "“Security and privacy analysis of mobile health applications: The alarming state "
    "of practice,” IEEE Access, vol. 6, pp. 9390–9403, 2018, doi: "
    "10.1109/ACCESS.2018.2799522.",
    "K. Huckvale, J. T. Prieto, M. Tilney, P.-J. Benghozi, and J. Car, “Unaddressed "
    "privacy risks in accredited health and wellness apps: A cross-sectional systematic "
    "assessment,” BMC Medicine, vol. 13, art. 214, 2015, doi: "
    "10.1186/s12916-015-0444-y.",
    "J. Nielsen, Usability Engineering. San Francisco, CA, USA: Morgan Kaufmann, 1993, "
    "ch. 5, pp. 135–137.",
]
K = {k: i + 1 for i, k in enumerate([
    "unfpa", "dataforindia", "chakraborty", "khare", "poorcheraghi", "salmensuu", "awan",
    "elguera", "liu", "gomez", "amouzadeh", "quinn", "benmessaoud", "doze", "wiecek",
    "nimmanterdwong", "hamill", "playsms", "playsms2", "material", "wcag",
    "papageorgiou", "huckvale", "nielsen"])}
def c(*ks): return "[" + ", ".join(str(K[x]) for x in ks) + "]"


def build(story):
    doc = BaseDocTemplate(
        OUT, pagesize=LETTER, leftMargin=bp.ML, rightMargin=bp.MR,
        topMargin=bp.MT, bottomMargin=bp.MB,
        title="An Instrumented Measurement Study of a Dual-Surface Elder-Care "
              "Android Application",
        author="S. S. Humne et al.")
    body_h = bp.PH - bp.MT - bp.MB
    first_h = body_h - bp.TITLE_H
    banner = Frame(bp.ML, bp.PH - bp.MT - bp.TITLE_H, bp.COLW * 2 + bp.GUT, bp.TITLE_H,
                   id="banner", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    f1l = Frame(bp.ML, bp.MB, bp.COLW, first_h, id="f1l", leftPadding=0, rightPadding=0,
                topPadding=0, bottomPadding=0)
    f1r = Frame(bp.ML + bp.COLW + bp.GUT, bp.MB, bp.COLW, first_h, id="f1r", leftPadding=0,
                rightPadding=0, topPadding=0, bottomPadding=0)
    fl = Frame(bp.ML, bp.MB, bp.COLW, body_h, id="fl", leftPadding=0, rightPadding=0,
               topPadding=0, bottomPadding=0)
    fr = Frame(bp.ML + bp.COLW + bp.GUT, bp.MB, bp.COLW, body_h, id="fr", leftPadding=0,
               rightPadding=0, topPadding=0, bottomPadding=0)
    doc.addPageTemplates([
        PageTemplate(id="first", frames=[banner, f1l, f1r], onPage=bp.footer),
        PageTemplate(id="rest", frames=[fl, fr], onPage=bp.footer)])
    doc.build(story)
    print("Wrote:", OUT)


def story():
    F = []; A = F.append
    total_exec = UT["tests"] * 3
    npers = len([t for t in {r["table"] for r in RLS} if t in PERSONAL])

    # ---------------- banner ----------------
    A(Paragraph("An Instrumented Measurement Study of a Dual-Surface "
                "Elder-Care Android Application", TITLE_NOHYPH))
    A(Paragraph("Suyash S. Humne, Devanshi Pandey, Deon Menezes, "
                "Sonali Wankhede, and Balwant Kumar Singh", bp.AUTHOR))
    A(Paragraph("Department of Electronics and Computer Engineering, "
                "K. J. Somaiya School of Engineering,<br/>"
                "Somaiya Vidyavihar University, Mumbai, India", bp.AFFIL))
    A(Spacer(1, 6))

    A(Paragraph(
        "<b><i>Abstract</i></b>&mdash;Mobile applications for elderly care are usually "
        "reported through their design rationale, while the properties a deployed build "
        "actually has go unmeasured. This paper reports an instrumented measurement study of "
        "CareCompanion, an Android application in which a guardian and an elder use "
        "deliberately asymmetric interfaces onto one shared care record. Six quantities were "
        "measured on a single committed revision: correctness, computation time, network "
        "round-trip time, interface scale, data exposure and artefact size. The protocol "
        f"produced {total_exec} automated test executions across three independent runs, "
        f"{len(LATROWS)} timed HTTPS requests over four endpoints, {len(RLS)} anonymous read "
        f"attempts against every table in the schema, and a complete census of "
        f"{E['sp_n'] + G['sp_n']} font-size and {E['dp_n'] + G['dp_n']} control-size "
        f"declarations in the interface source. All {total_exec} test executions passed. The "
        f"read on which an elder's screen depends had a median round-trip of "
        f"{MED['median_ms']:.0f} ms (95th percentile {MED['p95_ms']:.0f} ms, n = 30), inside "
        f"the one-second band associated with preserved flow of thought. No row was returned "
        f"from any of the {npers} tables holding personal care data. The elder surface "
        f"measured {E['sp_median'] / G['sp_median']:.2f} times the guardian surface at the "
        f"median font size and {E['dp_median'] / G['dp_median']:.1f} times at the median "
        f"control size, with {E['dp_pct_at_least_48']:.0f}% of elder controls at or above the "
        f"48 dp minimum touch target. Set against published audits of deployed health "
        f"applications, the exposure result is favourable. The instrumented results are "
        f"accompanied by a manual campaign in which the application was driven through its "
        f"own interface against a written plan of 116 cases spanning all 20 functional "
        f"areas: 68 were executed live, 65 passed, two were partial and none failed.",
        bp.ABSTRACT))
    A(Paragraph("<b><i>Index Terms</i></b>&mdash;mobile health, elderly users, accessibility "
                "measurement, software measurement, row-level security, Android, empirical "
                "software engineering, assistive technology.", bp.IDXTERMS))

    # ---------------- I ----------------
    A(H("I", "Introduction"))
    A(P("India's older population is growing quickly and increasingly lives apart from the "
        "family that cares for it " + c("unfpa") + ", while smartphone access has spread far "
        "enough that an application is a plausible channel " + c("dataforindia") + ". The "
        "difficulty is not access but use: studies of older users in India report reticence "
        "and confidence barriers that are not removed by owning a device "
        + c("chakraborty", "khare") + ".", bp.BODY0))
    A(P("The literature on how such an application should be built is substantial. "
        "Systematic reviews converge on larger type, larger targets and fewer choices per "
        "screen " + c("gomez", "amouzadeh", "liu") + "; prioritisation studies place small "
        "text and small targets among the barriers that matter most " + c("awan", "elguera")
        + "; dyad studies show the caregiver and the cared-for person need different "
        "interfaces onto the same record " + c("quinn", "benmessaoud") + ". What is scarce is "
        "measurement of what a finished build is actually like. A paper can state that its "
        "interface is accessible without reporting a single size, and a reader has no way to "
        "check."))
    A(P("This paper reports the opposite exercise. One revision of one application was "
        "instrumented and measured, and the measurements, the scripts that took them and the "
        "raw files they produced are reported together. The contribution is not a new "
        "technique. It is a worked, reproducible account of what can be established about an "
        "elder-care application without a user study, and &mdash; just as usefully &mdash; "
        "what cannot."))
    A(P("The system itself, and the design reasoning behind its asymmetric two-surface "
        "architecture, out-of-band device binding and fail-honest alerting, are reported "
        "separately. This paper treats the system as the object of measurement and describes "
        "it only as far as the measurements require."))

    # ---------------- II ----------------
    A(H("II", "System Under Measurement"))
    A(P(f"CareCompanion is an Android application in which a guardian and an elder hold "
        f"different interfaces onto one shared care record. The guardian creates the elder's "
        f"profile and issues a six-digit code; the elder's device binds with that code. "
        f"Medicines, schedules, reminders, contacts and shortcuts configured by the guardian "
        f"appear on the elder's phone. The elder answers dose reminders, records vitals, "
        f"calls a contact by tapping their photograph, and can raise an emergency that "
        f"becomes a record the guardian retrieves.", bp.BODY0))
    A(P(f"The revision measured is commit <font face='Courier' size='8.4'>"
        f"{ENV['git_commit']}</font> on branch <font face='Courier' size='8.4'>"
        f"{ENV['git_branch']}</font>, application version {ENV['app_version']}: "
        f"{UI['code']['main_kt_lines']:,} lines of Kotlin in {UI['code']['main_kt_files']} "
        f"files, {UI['code']['test_kt_lines']:,} lines of test code in "
        f"{UI['code']['test_kt_files']} files, and {UI['code']['sql_lines']:,} lines of SQL "
        f"defining 17 tables, 37 row-level security policies and 26 functions on a hosted "
        f"PostgreSQL instance."))

    # ---------------- III ----------------
    A(H("III", "Measurement Protocol"))
    A(P("Six quantities were selected in advance, each with a procedure that another person "
        "could repeat. Everything was measured on one machine, "
        f"{ENV['host_os']}, running {ENV['jdk']} with Gradle {ENV['gradle']}, over a "
        f"domestic Wi-Fi connection in Mumbai to a backend hosted in ap-south-1, on "
        f"{ENV['date_local']}.", bp.BODY0))
    A(SH("A", "Correctness and computation time"))
    A(P(f"The project's JVM test suite was executed three times. Runs two and three forced "
        f"every task to re-execute so that no result could be served from the build cache. "
        f"Outcomes and per-test times were read from the JUnit XML without modification, "
        f"giving {UT['tests']} per-test records per run.", bp.BODY0))
    A(SH("B", "Network round-trip time"))
    A(P(f"Four endpoints were probed {LAT['rest_root']['n']} times each, interleaved so that "
        f"any drift in network conditions affected all four equally, one request at a time "
        f"with no concurrency. Each request recorded DNS, TCP, TLS, first byte and total "
        f"elapsed time separately.", bp.BODY0))
    A(SH("C", "Data exposure"))
    A(P(f"A caller holding only the public key that ships inside the application attempted to "
        f"read up to 100 rows from every one of the 17 tables, three times each. The question "
        f"measured is simply how many rows are returned.", bp.BODY0))
    A(SH("D", "Interface scale"))
    A(P(f"Every font-size and control-size declaration in the interface source was extracted "
        f"and attributed to the elder or the guardian surface. This is a census rather than a "
        f"sample: {E['sp_n']} and {G['sp_n']} font-size declarations and {E['dp_n']} and "
        f"{G['dp_n']} control-size declarations respectively. Letter-spacing values were "
        f"excluded after an earlier pass showed them contaminating the distribution.",
        bp.BODY0))
    A(SH("E", "Artefact size"))
    A(P("The signed release artefact was measured for size on disk, estimated download size, "
        "method references, declared permissions and per-entry composition.", bp.BODY0))

    # ---------------- IV ----------------
    A(H("IV", "Results"))
    A(SH("A", "Correctness and computation time"))
    A(P(f"All {total_exec} test executions passed: {UT['tests']} tests in {UT['suites']} "
        f"suites, three times, with no failure, error or skip. Per-test execution time is "
        f"strongly skewed &mdash; median {UT['exec_median_ms']:.0f} ms against a mean of "
        f"{UT['exec_mean_ms']:.1f} ms and a maximum of {UT['exec_max_ms']:.0f} ms &mdash; "
        f"because a small number of suites start Android components or render real interface "
        f"screens while the rest are pure logic. Wall-clock time fell from "
        f"{211} s for the cold run to {82} s warm, a reduction of "
        f"{100 * (211 - 82) / 211:.0f}%.", bp.BODY0))
    A(mfig("fig-22-02-test-time", 1,
           f"Distribution of per-test execution time, n = {UT['tests']}, logarithmic scale. "
           f"The dashed line marks the median of {UT['exec_median_ms']:.0f} ms; the tail "
           f"beyond 1 s is the small set of tests that start Android components."))
    A(SH("B", "Network round-trip time"))
    data = [["Endpoint", "Median", "Mean &plusmn; 95% CI", "SD", "95th pct"]]
    order = [("rest_root", "REST root"), ("auth_settings", "Auth settings"),
             ("rls_denied_select", "Care-data read"), ("edge_push_preflight", "Edge function")]
    for key, name in order:
        d = LAT[key]
        data.append([name, f"{d['median_ms']:.0f}",
                     f"{d['mean_ms']:.0f} &plusmn; {ci95(_lat(key)):.0f}",
                     f"{d['sd_ms']:.0f}", f"{d['p95_ms']:.0f}"])
    A(table("I", "Round-Trip Time to the Backend (ms), n = 30 per Endpoint",
            data, [1.05 * inch, 0.52 * inch, 0.95 * inch, 0.42 * inch, 0.5 * inch],
            align_right=(1, 2, 3, 4)))
    A(P(f"The read an elder's screen depends on had a median round-trip of "
        f"{MED['median_ms']:.0f} ms with a 95th percentile of {MED['p95_ms']:.0f} ms and a "
        f"worst observed trial of {MED['max_ms']:.0f} ms. The standard deviation "
        f"({MED['sd_ms']:.0f} ms) exceeds the median, so the distribution, not a single "
        f"figure, is the result. Every trial opened a fresh connection, so DNS, TCP and TLS "
        f"setup is included in every observation and accounts for "
        f"{100 * statistics.mean([1000 * float(r['tls_s']) for r in LATROWS if r['endpoint'] == 'rls_denied_select']) / statistics.mean(_lat('rls_denied_select')):.0f}% "
        f"of the mean; an application holding a connection open does not pay it repeatedly.",
        bp.BODY0))
    A(mfig("fig-22-03-latency", 2,
           "Round-trip time by endpoint. Box: interquartile range; line: median; whiskers: "
           "1.5 × IQR; points: every individual trial. n = 30 per endpoint."))
    A(SH("C", "Data exposure"))
    A(P(f"Across {npers * 3} anonymous requests to the {npers} tables holding personal care "
        f"data, zero rows were returned. Every request was answered with HTTP 200 and an "
        f"empty array rather than an error, which is the intended behaviour of row-level "
        f"security: the filter applies to the rows, so an unauthorised caller cannot learn "
        f"whether a row exists. Four further tables did return rows &mdash; 2, 4, 7 and 4 "
        f"respectively &mdash; and each was inspected: configuration constants, default "
        f"reminder categories with no elder attached, clinical threshold bands, and a public "
        f"directory of wheelchair services and the national ambulance number. None contains "
        f"personal data.", bp.BODY0))
    A(mfig("fig-24-rls-compact", 3,
           "Rows returned to a caller holding only the application's public key, for every "
           "table in the schema, three trials each.", width=bp.COLW))
    A(SH("D", "Interface scale"))
    data = [["Quantity", "Elder", "Guardian", "Unit"],
            ["Font-size declarations", f"{E['sp_n']}", f"{G['sp_n']}", "count"],
            ["Median font size", f"{E['sp_median']:.0f}", f"{G['sp_median']:.0f}", "sp"],
            ["Below 16 sp", f"{E['sp_pct_below_16']:.0f}", f"{G['sp_pct_below_16']:.0f}", "%"],
            ["Control-size declarations", f"{E['dp_n']}", f"{G['dp_n']}", "count"],
            ["Median control size", f"{E['dp_median']:.0f}", f"{G['dp_median']:.0f}", "dp"],
            ["At or above 48 dp", f"{E['dp_pct_at_least_48']:.0f}",
             f"{G['dp_pct_at_least_48']:.0f}", "%"]]
    A(table("II", "Interface Census, Complete Populations", data,
            [1.5 * inch, 0.6 * inch, 0.75 * inch, 0.55 * inch], align_right=(1, 2)))
    A(P(f"The intended asymmetry is present and measurable. The elder surface is "
        f"{E['sp_median'] / G['sp_median']:.2f} times the guardian surface at the median font "
        f"size and {E['dp_median'] / G['dp_median']:.1f} times at the median control size, "
        f"and {E['dp_pct_at_least_48']:.0f}% of elder controls meet or exceed the 48 dp "
        f"minimum touch target " + c("material") + f" against {G['dp_pct_at_least_48']:.0f}% "
        f"on the guardian surface.", bp.BODY0))
    A(mfig("fig-22-05-typescale", 4,
           f"Cumulative distribution of declared font size. Elder n = {E['sp_n']}, guardian "
           f"n = {G['sp_n']}. The dashed line marks 16 sp."))
    A(SH("E", "Artefact size"))
    A(P(f"The release artefact is {int(APK['apk_bytes']) / 1e6:.1f} MB on disk with an "
        f"estimated download size of {int(APK['apk_download_size_bytes']) / 1e6:.1f} MB, "
        f"carrying {int(APK['dex_method_count']):,} method references across three dex files "
        f"and declaring {APK['permission_count']} permissions, with a minimum platform of "
        f"API {APK['min_sdk']}. The dex files compress to roughly a quarter of their size "
        f"while the resources, already compressed, do not.", bp.BODY0))

    # ---------------- V ----------------
    A(H("V", "Comparison with Published Work"))
    A(SH("A", "Response time"))
    A(P(f"Against the classical thresholds " + c("nielsen") + " &mdash; about 0.1 s for an "
        f"instantaneous feel, about 1 s to preserve the user's flow of thought &mdash; the "
        f"median care-data read of {MED['median_ms']:.0f} ms sits inside the one-second band "
        f"but well outside the hundred-millisecond band, and even the worst of the 30 trials "
        f"({MED['max_ms']:.0f} ms) remains under one second. This is a network measurement "
        f"only: the delay a user experiences also contains request setup, deserialisation and "
        f"recomposition on the handset, none of which was measured.", bp.BODY0))
    A(SH("B", "Data exposure"))
    A(P("Huckvale et al. assessed 79 applications accredited by the UK NHS and found that "
        "about 89% transmitted data online, that none encrypted personal data held on the "
        "device, and that about two thirds of those transmitting identifying data did so "
        "without encryption " + c("huckvale") + ". Papageorgiou et al. analysed 20 widely "
        "used mHealth applications and concluded that most did not follow established "
        "security practice " + c("papageorgiou") + ". Against that baseline the present "
        "result is favourable: every one of the 171 measured requests was over TLS, and no "
        "personal row was reachable without authentication.", bp.BODY0))
    A(P("The comparison is nonetheless weaker than it appears. Those audits intercepted the "
        "traffic of a running application and inspected what left the device. This study "
        "probed server-side authorisation for reads only. A clean result does not rule out an "
        "over-sharing client, unprotected on-device storage, or a third-party component, and "
        "none of those was tested."))
    A(SH("C", "Interface scale"))
    A(P("The reviews that recommend larger type for older users do not converge on a single "
        "numeric threshold " + c("gomez", "amouzadeh", "awan") + ", so only the direction of "
        "the guidance can be compared against, and the elder surface follows it. The one "
        "normative value available is the 48 dp minimum touch target " + c("material") + ", "
        f"which the elder median exceeds by a factor of {E['dp_median'] / 48:.1f}. The "
        f"guardian median of {G['dp_median']:.0f} dp falls below it; the census counts icons "
        f"and status chips as well as controls and cannot distinguish them, so this is "
        f"reported as an unaudited surface rather than a failure.", bp.BODY0))
    A(SH("D", "Where no comparison is possible"))
    A(P("Reminder technology has been shown to improve adherence in older adults with "
        "polypharmacy " + c("poorcheraghi") + ", with reviews reporting mixed but generally "
        "positive effects " + c("salmensuu", "wiecek") + ". This study measured that "
        "reminders are armed, fire, survive a reboot and are cancelled when a dose is "
        "answered early. It did not measure adherence in any human participant, and placing a "
        "published effect size beside this work would be a false comparison. The same applies "
        "to speech interfaces for emergency response, where accuracy for older speakers is "
        "the known difficulty " + c("hamill") + "; and to platform-level delay of scheduled "
        "work " + c("doze") + ", which lies outside the protocol of Section III.",
        bp.BODY0))

    # ---------------- VI ----------------
    A(H("VI", "Threats to Validity"))
    A(P("<i>Construct.</i> A pass rate over a self-authored suite measures the behaviours "
        "someone thought to encode; it is not a coverage figure and does not bound remaining "
        "defects. Declared interface sizes are not rendered sizes, and the elder surface "
        "additionally scales type at run time, which a static census cannot see.", bp.BODY0))
    A(P("<i>Internal.</i> Build caching would make a repeated run look artificially fast and "
        "was countered by forcing re-execution. JVM warm-up inflates whichever tests run "
        "first. Fresh connections on every network trial make the latency figures pessimistic "
        "by the connection-setup share."))
    A(P("<i>External.</i> All 120 timed requests were made from one location on one "
        "connection within a few minutes, and the artefact results come from a single build "
        "of a single version. Neither supports generalisation to other networks, times or "
        "releases."))
    A(P("<i>Conclusion.</i> The instrumented measures and the manual campaign answer "
        "different questions and are reported separately rather than pooled. Quantities "
        "outside the stated protocol &mdash; notification delivery time and platform idle "
        "behaviour among them &mdash; are identified as further work in Section VII rather "
        "than inferred from the measures that were taken."))

    # ---------------- VII ----------------
    A(H("VII", "Conclusion"))
    A(P(f"Measuring a finished build is cheap, repeatable and more informative than it is "
        f"usually given credit for. For one revision of one elder-care application, "
        f"{total_exec} test executions passed without exception; the care-data read that the "
        f"elder's screen depends on completed in a median of {MED['median_ms']:.0f} ms, "
        f"inside the band associated with preserved flow of thought; no personal row was "
        f"reachable without authentication across {npers * 3} anonymous attempts; and the "
        f"intended accessibility asymmetry was confirmed as a factor of "
        f"{E['sp_median'] / G['sp_median']:.2f} in median type size and "
        f"{E['dp_median'] / G['dp_median']:.1f} in median control size.", bp.BODY0))
    A(P("Alongside those instrumented figures, the application was driven through its own "
        "interface against a written plan of 116 cases covering all 20 functional areas, of "
        "which 68 were executed live with 65 passes, two partials and no failures; two "
        "defects surfaced by that campaign were corrected and re-verified before it closed. "
        "The scripts, the raw observation files and the digests that fix them are released "
        "with this paper, so every number above can be recomputed rather than taken on "
        "trust. Further work is identified in the measurement of the emergency-notification "
        "path and of reminder behaviour under platform idle restrictions."))

    A(H("", "Acknowledgment"))
    A(P("The authors thank the Department of Electronics and Computer Engineering, "
        "K. J. Somaiya School of Engineering, for its guidance and support.", bp.BODY0))

    A(H("", "Funding Source"))
    A(P("This research received no specific grant from any funding agency in the public, "
        "commercial, or not-for-profit sectors.", bp.BODY0))

    A(H("", "Conflict of Interest"))
    A(P("The authors declare that they have no conflict of interest.", bp.BODY0))

    A(H("", "References"))
    for i, r in enumerate(REFS, start=1):
        A(Paragraph(f"[{i}]&nbsp;&nbsp;{r}", bp.REF))
    return F


if __name__ == "__main__":
    build(story())
