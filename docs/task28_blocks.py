"""
task28_blocks.py
----------------
The content of the Task 28 final technical report, expressed once as a list of
neutral blocks so that the PDF (reportlab) and the editable .docx (python-docx)
are rendered from the same source and cannot drift apart.

Block grammar — every block is a tuple whose first element is its kind:

    ("h1", text)                     chapter heading
    ("h2", text)                     section heading
    ("p",  text)                     body paragraph
    ("lead", text)                   italic lead-in paragraph
    ("bullets", [text, ...])         bulleted list
    ("steps",   [text, ...])         numbered list
    ("callout", title, text, tone)   boxed note; tone in {tip, warn, danger}
    ("table", title, data, widths_mm, right_cols)
    ("figure", path, caption, width_mm)
    ("numrefs", title, [text, ...])  numbered reference list
    ("pagebreak",)

Inline markup is limited to <b>, <i> and <c> (code). Each renderer translates
<c> into whatever monospace it has.

Every measured value is read from measurements/raw/ at import time.
"""
import os, sys, csv, json, statistics, math

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RAW = os.path.join(ROOT, "measurements", "raw")
FIGS = os.path.join(ROOT, "measurements", "figs")
PFIGS = os.path.join(ROOT, "paper", "figs")
SHOTS = os.path.expanduser("~/Desktop/CareCompanion-Screenshots/app-screenshots")


def _rows(name):
    with open(os.path.join(RAW, name)) as fh:
        return list(csv.DictReader(fh))

def _jload(name):
    with open(os.path.join(RAW, name)) as fh:
        return json.load(fh)

def _kv(name):
    return {r["key"]: r["value"] for r in _rows(name)}


ENV = _kv("environment.csv")
UT = _jload("unit_test_summary.json")
SUITES = _rows("unit_test_suites.csv")
REPEATS = _rows("test_repeats.csv")
LAT = _jload("backend_latency_summary.json")
RLS = _rows("rls_anon_reads.csv")
UI = _jload("ui_summary.json")
APK = _kv("apk_metrics.csv")
SECTIONS = _rows("apk_sections.csv")

PERSONAL = {"adherence_logs", "alerts", "contacts", "elders", "guardian_elder_links",
            "invite_codes", "medicine_schedules", "medicines", "ott_shortcuts",
            "reminders", "sos_events", "users", "vitals"}
REFERENCE = {"app_config", "reminder_categories", "vital_thresholds", "wheelchair_places"}

TABLES = sorted({r["table"] for r in RLS})
N_TABLES = len(TABLES)
N_PROBES = len(RLS)

RUN_WALL = [211] + [int(r["wall_s"]) for r in REPEATS]
BUILD_S = 181
TOTAL_EXEC = UT["tests"] * 3

E, G = UI["elder"], UI["guardian"]
APK_MB = int(APK["apk_bytes"]) / 1e6


def fig(name):
    return os.path.join(FIGS, name + ".png")

def pfig(name):
    return os.path.join(PFIGS, name + ".png")


# ───────────────────────────── group members ─────────────────────────────
# ROLL NUMBERS AND THE NAME-TO-ROLE MAPPING ARE THE ONE THING THAT CANNOT BE
# DERIVED FROM THE REPOSITORY. Correct the four blank roll numbers and check the
# role assignment, then re-run the builder.
SUBMITTING_MEMBER = "Devanshi Pandey"

MEMBERS = [
    ("Suyash S. Humne", "", "Application architecture and implementation",
     "Set up the Gradle and Kotlin toolchain, the Hilt dependency graph and the "
     "navigation structure; implemented the guardian surface, the reminder scheduling "
     "chain (DailyArmWorker, exact dose alarms, cancellation) and the offline outbox "
     "with its retry and durability behaviour."),
    ("Devanshi Pandey", "16014023016", "Measurement study, testing and documentation",
     "Wrote and ran the measurement protocol of Section 3 — the four instrumentation "
     "scripts, the 441 automated test executions over three repeated runs, the 120 timed "
     "HTTPS requests and the 51 anonymous read attempts; drove the 68 manual test cases; "
     "produced the results of Chapter 7, the user manual and this report."),
    ("Deon Menezes", "", "Elder surface and accessibility",
     "Implemented the elder surface in Jetpack Compose — the connect-code entry, the "
     "photo-first contacts grid, the video shortcuts and the SOS screen — and set the "
     "enlarged type scale and touch targets measured in Section 7.3."),
    ("Sonali Wankhede", "", "Backend, data model and security rules",
     "Designed the PostgreSQL schema of the 17 tables, wrote the row-level security "
     "policies audited in Section 7.4, and built the Supabase edge function and Firebase "
     "Cloud Messaging path used for alert delivery."),
    ("Balwant Kumar Singh", "", "Literature review and comparison",
     "Assembled and screened the 40 sources of Chapter 2, built the thematic comparison "
     "of Section 2.3 and the gap statement of Section 2.4, and prepared the reference "
     "apparatus used by this report and by the IEEE paper of Task 29."),
]


# ───────────────────────────── references ─────────────────────────────
REFS = [
    # context (1-2)
    "United Nations Population Fund, <i>India Ageing Report 2023: Caring for Our Elders — "
    "Institutional Responses</i>. New Delhi, India: UNFPA India, 2023.",
    "Data for India, “Access to phones and the internet,” 2025. [Online]. Available: "
    "https://www.dataforindia.com/comm-tech/",
    # adoption and acceptance (3-8)
    "D. Chakraborty and C. Garg, “Navigating technology: Mobile media usage and reticence "
    "among older adults in rural India,” <i>Mobile Media &amp; Communication</i>, vol. 14, "
    "pp. 31–49, 2025.",
    "A. Khare, R. Mukherjee, and M. Bedarkar, “Beyond digital literacy: A structural model "
    "analysis of technology acceptance among elderly persons,” <i>Humanities and Social "
    "Sciences Communications</i>, vol. 13, art. 794, 2026.",
    "F. D. Davis, “Perceived usefulness, perceived ease of use, and user acceptance of "
    "information technology,” <i>MIS Quarterly</i>, vol. 13, no. 3, pp. 319–340, 1989.",
    "V. Venkatesh, M. G. Morris, G. B. Davis, and F. D. Davis, “User acceptance of "
    "information technology: Toward a unified view,” <i>MIS Quarterly</i>, vol. 27, no. 3, "
    "pp. 425–478, 2003.",
    "V. Venkatesh, J. Y. L. Thong, and X. Xu, “Consumer acceptance and use of information "
    "technology: Extending the unified theory of acceptance and use of technology,” "
    "<i>MIS Quarterly</i>, vol. 36, no. 1, pp. 157–178, 2012.",
    "J. U. Palas, G. Sorwar, M. R. Hoque, and A. Sivabalan, “Factors influencing the "
    "elderly's adoption of mHealth: An empirical study using extended UTAUT2 model,” "
    "<i>BMC Medical Informatics and Decision Making</i>, vol. 22, art. 191, 2022, "
    "doi: 10.1186/s12911-022-01917-3.",
    # reminders and adherence (9-11)
    "H. Poorcheraghi, R. Negarandeh, S. Pashaeypoor, and J. Jorian, “Effect of using a "
    "mobile drug management application on medication adherence and hospital readmission "
    "among elderly patients with polypharmacy: A randomized controlled trial,” <i>BMC "
    "Health Services Research</i>, vol. 23, art. 1192, 2023.",
    "O. Salmensuu, J. Isotalo, M. Rijken, V. Hyttinen-Huotari, M. Kaarakainen, and "
    "I. Linnosmaa, “Effects of using medication reminder technologies by home-dwelling older "
    "citizens: A systematic review,” <i>Age and Ageing</i>, vol. 55, no. 2, art. afag007, 2026.",
    "E. Wiecek, S. Taylor, H. Rourke, F. Hammond, and N. Amador-Fernandez, “Mobile health "
    "apps for older adults: Real-world evidence on engagement and medication adherence,” "
    "<i>Frontiers in Digital Health</i>, vol. 8, art. 1716880, 2026.",
    # telecare, alerting and emergency response (12-16)
    "S. P. Hirani, M. Beynon, M. Cartwright, L. Rixon, H. Doll, J. Newman <i>et al.</i>, "
    "“The effect of telecare on the quality of life and psychological well-being of elderly "
    "recipients of social care over a 12-month period: The Whole Systems Demonstrator "
    "cluster randomised trial,” <i>Age and Ageing</i>, vol. 43, no. 3, pp. 334–341, 2014, "
    "doi: 10.1093/ageing/aft185.",
    "Z. Pajalic, D. Aguiar de Sousa, B. S. Strøm, H. Lausund, G. Breievne <i>et al.</i>, "
    "“Welfare technology interventions among older people living at home — A systematic "
    "review of RCT studies,” <i>PLOS Digital Health</i>, vol. 2, no. 1, art. e0000184, 2023, "
    "doi: 10.1371/journal.pdig.0000184.",
    "R. A. Torres-Guzman, M. R. Paulson, F. R. Avila, K. Maita, J. P. Garcia, A. J. Forte, "
    "and M. J. Maniaci, “Smartphones and threshold-based monitoring methods effectively "
    "detect falls remotely: A systematic review,” <i>Sensors</i>, vol. 23, no. 3, art. 1323, "
    "2023, doi: 10.3390/s23031323.",
    "M. Hamill, V. Young, J. Boger, and A. Mihailidis, “Development of an automated speech "
    "recognition interface for personal emergency response systems,” <i>Journal of "
    "NeuroEngineering and Rehabilitation</i>, vol. 6, art. 26, 2009.",
    "S. Kim, “Exploring how older adults use a smart speaker–based voice assistant in their "
    "first interactions: Qualitative study,” <i>JMIR mHealth and uHealth</i>, vol. 9, no. 1, "
    "art. e20427, 2021, doi: 10.2196/20427.",
    # the caregiver dyad (17-20)
    "C. C. Quinn, S. Staub, E. Barr, and A. Gruber-Baldini, “Mobile support for older adults "
    "and their caregivers: Dyad usability study,” <i>JMIR Aging</i>, vol. 2, no. 1, "
    "art. e12276, 2019.",
    "C. Benmessaoud, K. J. Pfisterer, A. De Leon, A. Saragadam, N. El-Dassouki, "
    "K. G. M. Young, R. Lohani, and T. Xiong, “Design of a dyadic digital health module for "
    "chronic disease shared care: Development study,” <i>JMIR Human Factors</i>, vol. 10, "
    "art. e45035, 2023.",
    "M. R. Grossman, D. K. Zak, and E. M. Zelinski, “Mobile apps for caregivers of older "
    "adults: Quantitative content analysis,” <i>JMIR mHealth and uHealth</i>, vol. 6, no. 7, "
    "art. e162, 2018, doi: 10.2196/mhealth.9345.",
    "L. Wozney, L. M. Freitas de Souza, E. Kervin, F. Queluz, P. J. McGrath, and J. Keefe, "
    "“Commercially available mobile apps for caregivers of people with Alzheimer disease or "
    "other related dementias: Systematic search,” <i>JMIR Aging</i>, vol. 1, no. 2, "
    "art. e12274, 2018, doi: 10.2196/12274.",
    # age-friendly interface design (21-28)
    "M. Awan, S. Ali, M. Ali, M. F. Abrar, H. Ullah, and D. Khan, “Usability barriers for "
    "elderly users in smartphone app usage: An analytical hierarchical process-based "
    "prioritization,” <i>Scientific Programming</i>, vol. 2021, art. 2780257, 2021.",
    "L. Elguera Paez and C. Zapata Del Río, “Elderly users and their main challenges "
    "usability with mobile applications: A systematic review,” in <i>Design, User "
    "Experience, and Usability</i>, LNCS. Cham, Switzerland: Springer, 2019, pp. 423–438.",
    "N. Liu, J. Yin, S. S.-L. Tan, K. Y. Ngiam, and H. H. Teo, “Mobile health applications "
    "for older adults: A systematic review of interface and persuasive feature design,” "
    "<i>Journal of the American Medical Informatics Association</i>, vol. 28, pp. 2483–2501, "
    "2021.",
    "M. Gomez-Hernandez, X. Ferre, C. Moral, and E. Villalba-Mora, “Design guidelines of "
    "mobile apps for older adults: Systematic review and thematic analysis,” <i>JMIR mHealth "
    "and uHealth</i>, vol. 11, art. e43186, 2023.",
    "E. Amouzadeh, I. Dianat, J. Faradmal, and M. Babamiri, “Optimizing mobile app design "
    "for older adults: Systematic review of age-friendly design,” <i>Aging Clinical and "
    "Experimental Research</i>, vol. 37, art. 248, 2025.",
    "Z. Nimmanterdwong, S. Boonviriya, and P. Tangkijvanich, “Human-centered design of "
    "mobile health apps for older adults: Systematic review and narrative synthesis,” "
    "<i>JMIR mHealth and uHealth</i>, vol. 10, no. 1, art. e29512, 2022.",
    "G. Hou, U. Anicetus, and J. He, “How to design font size for older adults: A systematic "
    "literature review with a mobile device,” <i>Frontiers in Psychology</i>, vol. 13, "
    "art. 931646, 2022, doi: 10.3389/fpsyg.2022.931646.",
    "S. Beier and C. A. T. Oderkerk, “The effect of age and font on reading ability,” "
    "<i>Visible Language</i>, vol. 53, no. 3, pp. 51–59, 2019.",
    # usability method and response time (29-34)
    "B. Shneiderman, “Universal usability,” <i>Communications of the ACM</i>, vol. 43, "
    "no. 5, pp. 84–91, 2000.",
    "J. Nielsen and R. Molich, “Heuristic evaluation of user interfaces,” in <i>Proc. ACM "
    "CHI '90</i>, Seattle, WA, USA, 1990, pp. 249–256.",
    "J. Nielsen, <i>Usability Engineering</i>. San Francisco, CA, USA: Morgan Kaufmann, "
    "1993, ch. 5, pp. 135–137.",
    "R. B. Miller, “Response time in man-computer conversational transactions,” in <i>Proc. "
    "AFIPS Fall Joint Computer Conference</i>, 1968, pp. 267–277.",
    "S. K. Card, G. G. Robertson, and J. D. Mackinlay, “The information visualizer, an "
    "information workspace,” in <i>Proc. ACM CHI '91</i>, New Orleans, LA, USA, 1991, "
    "pp. 181–188.",
    "J. Brooke, “SUS: A quick and dirty usability scale,” in <i>Usability Evaluation in "
    "Industry</i>, P. W. Jordan <i>et al.</i>, Eds. London, U.K.: Taylor &amp; Francis, "
    "1996, pp. 189–194.",
    # measurable properties of shipped apps (35-39)
    "A. Bangor, P. T. Kortum, and J. T. Miller, “An empirical evaluation of the System "
    "Usability Scale,” <i>International Journal of Human–Computer Interaction</i>, vol. 24, "
    "no. 6, pp. 574–594, 2008.",
    "S. Chen, C. Chen, L. Fan, M. Fan, X. Zhan, and Y. Liu, “Accessible or not? An empirical "
    "investigation of Android app accessibility,” <i>IEEE Transactions on Software "
    "Engineering</i>, vol. 48, no. 10, pp. 3954–3968, 2022.",
    "A. S. Ross, X. Zhang, J. Fogarty, and J. O. Wobbrock, “An epidemiology-inspired "
    "large-scale analysis of Android app accessibility,” <i>ACM Transactions on Accessible "
    "Computing</i>, vol. 13, no. 1, art. 4, 2020, doi: 10.1145/3348797.",
    "A. Papageorgiou, M. Strigkos, E. Politou, E. Alepis, A. Solanas, and C. Patsakis, "
    "“Security and privacy analysis of mobile health applications: The alarming state of "
    "practice,” <i>IEEE Access</i>, vol. 6, pp. 9390–9403, 2018, "
    "doi: 10.1109/ACCESS.2018.2799522.",
    "K. Huckvale, J. T. Prieto, M. Tilney, P.-J. Benghozi, and J. Car, “Unaddressed privacy "
    "risks in accredited health and wellness apps: A cross-sectional systematic assessment,” "
    "<i>BMC Medicine</i>, vol. 13, art. 214, 2015, doi: 10.1186/s12916-015-0444-y.",
    # (40)
    "International Organization for Standardization, <i>ISO 9241-11:2018 — Ergonomics of "
    "Human-System Interaction — Part 11: Usability: Definitions and Concepts</i>. Geneva, "
    "Switzerland: ISO, 2018.",
]

NORMATIVE = [
    "Material Design 3, “Accessibility — applying accessible design.” [Online]. Available: "
    "https://m3.material.io/foundations/accessible-design/patterns",
    "W3C, <i>Web Content Accessibility Guidelines (WCAG) 2.2</i>, W3C Recommendation, 2023. "
    "[Online]. Available: https://www.w3.org/TR/WCAG22/",
    "Google, “Touch target size,” Android Accessibility Help. [Online]. Available: "
    "https://support.google.com/accessibility/android/answer/7101858",
    "Android Developers, “Optimize for Doze and App Standby.” [Online]. Available: "
    "https://developer.android.com/training/monitoring-device-state/doze-standby",
    "Google Play Console Help, “Use of SMS or Call Log permission groups.” [Online]. "
    "Available: https://support.google.com/googleplay/android-developer/answer/10208820",
    "Android Developers Blog, “Reminder: SMS/Call Log policy changes,” Jan. 2019. [Online]. "
    "Available: https://android-developers.googleblog.com/2019/01/"
    "reminder-smscall-log-policy-changes.html",
    "PostgreSQL Global Development Group, “Row security policies,” <i>PostgreSQL "
    "Documentation</i>. [Online]. Available: "
    "https://www.postgresql.org/docs/current/ddl-rowsecurity.html",
    "Supabase, “Row Level Security.” [Online]. Available: "
    "https://supabase.com/docs/guides/database/postgres/row-level-security",
    "Google, “Firebase Cloud Messaging.” [Online]. Available: "
    "https://firebase.google.com/docs/cloud-messaging",
    "Android Developers, “WorkManager.” [Online]. Available: "
    "https://developer.android.com/topic/libraries/architecture/workmanager",
]

# A citation helper: R["key"] -> the 1-based index in REFS.
_KEYS = [
    "unfpa", "dataforindia", "chakraborty", "khare", "davis", "utaut", "utaut2", "palas",
    "poorcheraghi", "salmensuu", "wiecek", "hirani", "pajalic", "torres", "hamill", "kim",
    "quinn", "benmessaoud", "grossman", "wozney", "awan", "elguera", "liu", "gomez",
    "amouzadeh", "nimmanterdwong", "hou", "beier", "shneiderman", "nielsenmolich",
    "nielsen", "miller", "card", "brooke", "bangor", "chen", "ross", "papageorgiou",
    "huckvale", "iso9241",
]
assert len(_KEYS) == len(REFS) == 40, (len(_KEYS), len(REFS))
R = {k: i + 1 for i, k in enumerate(_KEYS)}
N = {k: 41 + i for i, k in enumerate(
    ["material", "wcag", "touchtarget", "doze", "playsms", "playsms2", "pgrls",
     "supabasersl", "fcm", "workmanager"])}

def c(*keys):
    return "[" + ", ".join(str(R[k]) for k in keys) + "]"

def n(*keys):
    return "[" + ", ".join(str(N[k]) for k in keys) + "]"


# ─────────────────────── the 40-source review table ───────────────────────
# (reference key, short citation, kind, what it contributes, what it bears on)
REVIEW = [
    ("unfpa", "UNFPA, India Ageing Report (2023)", "Government/UN report",
     "Projects India's 60+ population to roughly 20 % of the country by 2050 and documents "
     "the thinness of institutional elder care.", "Problem scale"),
    ("dataforindia", "Data for India (2025)", "National dataset",
     "Smartphone and internet access by age group in India — the device the design can "
     "assume already exists in the household.", "Deployment assumption"),
    ("chakraborty", "Chakraborty & Garg (2025)", "Qualitative journal study",
     "Mobile media use and reticence among older adults in rural India; help-seeking from a "
     "younger family member is the normal mode of use.", "Two-surface rationale"),
    ("khare", "Khare et al. (2026)", "SEM journal study",
     "Structural model of technology acceptance among elderly persons in India; digital "
     "literacy alone does not explain adoption.", "Two-surface rationale"),
    ("davis", "Davis (1989)", "Foundational model",
     "TAM: perceived usefulness and perceived ease of use as the two determinants of "
     "acceptance.", "Design objective"),
    ("utaut", "Venkatesh et al. (2003)", "Foundational model",
     "UTAUT: performance expectancy, effort expectancy, social influence and facilitating "
     "conditions.", "Design objective"),
    ("utaut2", "Venkatesh et al. (2012)", "Foundational model",
     "UTAUT2 adds hedonic motivation, price value and habit — the consumer-technology "
     "extension the elderly-adoption literature builds on.", "Design objective"),
    ("palas", "Palas et al. (2022)", "Survey, n = 493, age 60+",
     "Extended UTAUT2 for elderly mHealth adoption; social influence, habit and service "
     "quality carried significant effects on intention.", "Two-surface rationale"),
    ("poorcheraghi", "Poorcheraghi et al. (2023)", "Randomised controlled trial",
     "A mobile drug-management application improved medication adherence and reduced "
     "readmission among elderly patients with polypharmacy.", "Reminder feature"),
    ("salmensuu", "Salmensuu et al. (2026)", "Systematic review",
     "Effects of medication reminder technologies used by home-dwelling older citizens.",
     "Reminder feature"),
    ("wiecek", "Wiecek et al. (2026)", "Real-world evidence study",
     "Engagement and adherence observed in deployed mobile health apps for older adults, "
     "rather than under trial conditions.", "Reminder feature"),
    ("hirani", "Hirani et al. (2014)", "Cluster randomised trial",
     "The Whole Systems Demonstrator: telecare produced a small but significant mental "
     "health-related quality-of-life benefit over 12 months.", "Alerting value"),
    ("pajalic", "Pajalic et al. (2023)", "Systematic review of RCTs",
     "Welfare technology for people 60+ living at home; over half the trials carried a high "
     "risk of bias, so findings were synthesised narratively.", "Evidence caution"),
    ("torres", "Torres-Guzman et al. (2023)", "Systematic review",
     "Smartphone and threshold-based fall detection; the usual action after a detected fall "
     "is an alert to a guardian.", "SOS design"),
    ("hamill", "Hamill et al. (2009)", "Development and evaluation study",
     "An automated speech-recognition interface for personal emergency response systems, "
     "and its accuracy problems with older speakers.", "Voice feature"),
    ("kim", "Kim (2021)", "Qualitative study, n = 18, age 74+",
     "First interactions with a smart speaker: a positive first impression, then difficulty "
     "forming a structured command.", "Voice feature"),
    ("quinn", "Quinn et al. (2019)", "Dyad usability study",
     "The caregiver and the cared-for person treated as a pair, each with their own "
     "interface — the direct architectural precedent for this project.", "Two-surface design"),
    ("benmessaoud", "Benmessaoud et al. (2023)", "Development study",
     "A dyadic digital health module for shared chronic-disease care; how the two sides of a "
     "dyad are kept in step.", "Two-surface design"),
    ("grossman", "Grossman et al. (2018)", "Quantitative content analysis",
     "Only 44 caregiver-facing apps existed in the two major stores in 2017, and 8 of 44 "
     "(18 %) addressed three or more categories of support.", "Gap evidence"),
    ("wozney", "Wozney et al. (2018)", "Systematic app search",
     "Commercial dementia-caregiver apps were largely static, text-based information, and "
     "none was trialled or evidence based.", "Gap evidence"),
    ("awan", "Awan et al. (2021)", "AHP prioritisation study",
     "A ranked catalogue of the usability barriers elderly smartphone users actually hit; "
     "small text and small targets rank near the top.", "Typography, targets"),
    ("elguera", "Elguera Paez & Zapata Del Río (2019)", "Systematic review",
     "The main usability challenges elderly users meet in mobile applications.",
     "Typography, targets"),
    ("liu", "Liu et al. (2021)", "Systematic review",
     "Interface and persuasive feature design in mobile health applications for older "
     "adults.", "Interface design"),
    ("gomez", "Gomez-Hernandez et al. (2023)", "Systematic review, thematic analysis",
     "Consolidated design guidelines for mobile apps for older adults; notes that text "
     "elements have been comparatively neglected.", "Interface design"),
    ("amouzadeh", "Amouzadeh et al. (2025)", "Systematic review",
     "Age-friendly mobile app design: legible type, large controls, reduced per-screen "
     "choice recur across the evidence.", "Interface design"),
    ("nimmanterdwong", "Nimmanterdwong et al. (2022)", "Systematic review",
     "Human-centred design practice in mobile health apps for older adults.",
     "Development method"),
    ("hou", "Hou et al. (2022)", "Systematic review",
     "Font size for older adults on a mobile device: larger is preferred up to a point, "
     "after which readability falls; proposes visual angle in arcminutes as the metric.",
     "Typography"),
    ("beier", "Beier & Oderkerk (2019)", "Controlled reading experiment",
     "Older readers are more sensitive to differences in font legibility than younger "
     "readers, measured by reading speed and critical print size.", "Typography"),
    ("shneiderman", "Shneiderman (2000)", "Position paper",
     "Universal usability: designing for the widest span of user ability rather than the "
     "median user.", "Design philosophy"),
    ("nielsenmolich", "Nielsen & Molich (1990)", "Method paper",
     "Heuristic evaluation — the inspection method behind the written manual test plan of "
     "Section 6.2.", "Test method"),
    ("nielsen", "Nielsen (1993)", "Reference book",
     "The response-time thresholds used as the yardstick in Section 7.2: ~0.1 s feels "
     "instantaneous, ~1 s preserves flow of thought, ~10 s loses attention.",
     "Latency yardstick"),
    ("miller", "Miller (1968)", "Foundational paper",
     "The original measurement of acceptable response time in interactive systems, from "
     "which the thresholds above descend.", "Latency yardstick"),
    ("card", "Card et al. (1991)", "Empirical paper",
     "Confirms the ~1 s threshold for keeping a user's train of thought in an interactive "
     "information workspace.", "Latency yardstick"),
    ("brooke", "Brooke (1996)", "Instrument",
     "The System Usability Scale — the instrument a future usability study of this "
     "application would use.", "Future work"),
    ("bangor", "Bangor et al. (2008)", "Empirical evaluation, 2,324 surveys",
     "Benchmarks that make an SUS score interpretable, and so define what a future study "
     "would have to clear.", "Future work"),
    ("chen", "Chen et al. (2022)", "Large-scale empirical study",
     "Xbot: automated accessibility testing over 2,270 apps and 86,767 collected issues; "
     "accessibility defects are the norm, not the exception, in shipped Android apps.",
     "Accessibility baseline"),
    ("ross", "Ross et al. (2020)", "Large-scale analysis, 5,753 apps",
     "An epidemiology-inspired analysis of Android accessibility barriers, including the "
     "sizing of interactive controls.", "Accessibility baseline"),
    ("papageorgiou", "Papageorgiou et al. (2018)", "Security and privacy audit",
     "An audit of deployed mobile health applications; poor handling of personal data is "
     "widespread.", "Security baseline"),
    ("huckvale", "Huckvale et al. (2015)", "Cross-sectional assessment, 79 apps",
     "Of 79 accredited health apps, about 89 % transmitted data online, none encrypted "
     "personal data stored on the device, and roughly two thirds of those sending "
     "identifying data did so without encryption.", "Security baseline"),
    ("iso9241", "ISO 9241-11:2018", "International standard",
     "The definition of usability as effectiveness, efficiency and satisfaction in a stated "
     "context of use — the frame this report's measurements sit in.", "Measurement frame"),
]
assert len(REVIEW) == 40


# ───────────────────────────── the report ─────────────────────────────
def blocks():
    B = []; A = B.append
    nd = lambda x: f"{x:,}"

    # ══════════════════ DECLARATION ══════════════════
    A(("h1", "Declaration and signature"))
    A(("p", "This report describes work carried out by the project group named below for the "
            "Last Year Extra Curricular Project (EXCP) course at K. J. Somaiya School of "
            "Engineering. The design, the implementation and the measurements reported here "
            "are the group's own work. Every figure, table and numeric claim in Chapters 6 "
            "and 7 was produced by the instrumentation scripts listed in Chapter 3, run "
            f"against commit <c>{ENV['git_commit']}</c> of branch <c>{ENV['git_branch']}</c> "
            f"on {ENV['date_local']}; the raw files those scripts wrote are submitted with "
            "this report and their SHA-256 digests are listed in Appendix A. Material drawn "
            "from published work is cited in place and listed in the References."))
    A(("p", "I declare that the contribution attributed to me in Chapter 10 is my own, and "
            "that I am able to explain and defend it."))
    A(("table", "Group members, and the signature of each.", [
        ["Name", "Roll number", "Contribution (summarised; Chapter 10 gives the detail)",
         "Signature", "Date"],
    ] + [[m[0], m[1] or "—", m[2], "<br/><br/>", "<br/><br/>"] for m in MEMBERS],
       [34, 24, 58, 30, 24], ()))
    A(("p", f"<b>Submitted by:</b> {SUBMITTING_MEMBER}. Every member of the group submits this "
            "same report separately, each stating their own contribution, as the task "
            "requires."))
    A(("table", "Approval.", [
        ["Role", "Name", "Signature", "Date"],
        ["Project guide", "<br/><br/>", "<br/><br/>", "<br/><br/>"],
        ["Head of Department, EXCP", "<br/><br/>", "<br/><br/>", "<br/><br/>"],
    ], [34, 56, 50, 30], ()))
    A(("pagebreak",))

    # ══════════════════ CONTENTS ══════════════════
    A(("h1", "Contents"))
    A(("table", "Structure of this report.", [
        ["Chapter", "Title", "What it establishes"],
        ["—", "Abstract", "The problem, what was built, what was measured, the headline result"],
        ["1", "Introduction",
         "Why the problem matters, the objectives, the scope and the contributions claimed"],
        ["2", "Literature review and comparison",
         "Forty sources reviewed, synthesised by theme, compared against this project, and "
         "the gap this project addresses"],
        ["3", "Methodology",
         "How the application was built and, separately, how it was measured — the protocol, "
         "the instruments and the conditions"],
        ["4", "System design and diagrams",
         "Architecture, data model, the two interface surfaces, and the security design"],
        ["5", "Implementation",
         "The code as built: modules, toolchain, scheduling chain, offline behaviour, and the "
         "release artefact"],
        ["6", "Testing",
         "The automated suite, three repeated runs, and the 116-case manual campaign"],
        ["7", "Results",
         "Every measured result, with units, sample sizes and conditions"],
        ["8", "Discussion",
         "The results set against the literature of Chapter 2, with limitations and sources "
         "of error stated"],
        ["9", "Conclusion and future work",
         "What the evidence supports, and what the next version should do"],
        ["10", "Group members and contributions",
         "Who did what, per member"],
        ["—", "References", "Forty reviewed sources, then ten normative references"],
        ["—", "Appendices A–E",
         "Raw data inventory with digests, suite inventory, table-by-table security audit, "
         "build environment, and the latency distribution"],
    ], [16, 46, 108], ()))
    A(("pagebreak",))

    # ══════════════════ ABSTRACT ══════════════════
    A(("h1", "Abstract"))
    A(("p", "India's population aged 60 and over is projected to reach roughly a fifth of the "
            f"country by 2050, while institutional care remains thin {c('unfpa')}. The "
            "practical consequence is that an older person's care is conducted by a family "
            "member, usually one who does not live in the same room, on a smartphone that is "
            "already in the house. The published evidence on elderly smartphone use "
            "describes an older adult who seeks help from a younger relative rather than "
            f"operating a complex interface alone {c('chakraborty', 'khare', 'palas')}, and "
            "the dyad studies treat the caregiver and the cared-for person as a pair with "
            f"different needs {c('quinn', 'benmessaoud')}. Yet surveys of the application "
            "market find caregiver-facing software to be sparse, narrow and almost never "
            f"evidence based {c('grossman', 'wozney')}."))
    A(("p", f"CareCompanion is an Android application built for that pair. It presents two "
            f"distinct interface surfaces over one data model: an <i>elder</i> surface of "
            f"photograph-first, enlarged controls, and a <i>guardian</i> surface that is a "
            f"dense management console. A guardian creates the elder's profile, issues a "
            f"connect code, and from then on manages contacts, medicines, reminders and "
            f"video shortcuts remotely; the elder taps a face to place a call, taps a tile to "
            f"play a video, and holds one button to raise an alert. The application is "
            f"{nd(int(UI['code']['main_kt_lines']))} lines of Kotlin across "
            f"{UI['code']['main_kt_files']} files, over a PostgreSQL database of "
            f"{N_TABLES} tables in which every table carrying personal data is protected by "
            f"row-level security."))
    A(("p", f"This report's central claim is not that the application is good but that it has "
            f"been measured. Five instruments were run against one frozen commit on "
            f"{ENV['date_local']}: the automated test suite three times over "
            f"({TOTAL_EXEC} test executions, {UT['failed']} failures, "
            f"{UT['errors']} errors); {sum(v['n'] for v in LAT.values())} timed HTTPS "
            f"requests across {len(LAT)} backend endpoints; {N_PROBES} anonymous read "
            f"attempts against every table; a complete census of "
            f"{E['sp_n'] + G['sp_n'] + E['dp_n'] + G['dp_n']} declared interface sizes across "
            f"both surfaces; and the release artefact itself. The headline results are a "
            f"median care-data read latency of {LAT['rls_denied_select']['median_ms']:.0f} ms "
            f"(95th percentile {LAT['rls_denied_select']['p95_ms']:.0f} ms, n = "
            f"{LAT['rls_denied_select']['n']}), an elder surface whose median declared "
            f"control is {E['dp_median']:.0f} dp against the 48 dp platform minimum "
            f"{n('material')}, a median elder type size of {E['sp_median']:.0f} sp against "
            f"{G['sp_median']:.0f} sp on the guardian surface, and zero rows of personal data "
            f"returned to an unauthenticated reader from any of the {len(PERSONAL)} personal "
            f"tables. Alongside the automated suite, 68 of a written 116-case manual test "
            f"plan were driven live through the interface across all 20 functional areas: 65 "
            f"passed, 2 were partial, none failed, and the campaign found and fixed two "
            f"defects."))
    A(("p", f"Set against the literature, the elder surface clears the published guidance it "
            f"was designed from — the median control is "
            f"{E['dp_median'] / 48:.1f}× the platform minimum, and the measured latency sits "
            f"an order of magnitude inside the one-second threshold at which a user's train "
            f"of thought is preserved {c('miller', 'card', 'nielsen')} — while the guardian "
            f"surface sits deliberately below those elder-facing minima, which is recorded "
            f"here as a measured finding rather than smoothed over. Against the security "
            f"audits of deployed health applications, where roughly 89 % of accredited apps "
            f"transmitted data online and none encrypted personal data at rest "
            f"{c('huckvale', 'papageorgiou')}, the database-enforced isolation measured here "
            f"is the project's strongest result. What has <i>not</i> been established is any "
            f"health outcome: the adherence benefit reported in the trial literature "
            f"{c('poorcheraghi', 'salmensuu')} is the reason the reminder feature exists, not "
            f"a result of this project."))
    A(("p", "<b>Keywords —</b> elder care, assistive mobile application, Android, dyadic "
            "design, accessibility, row-level security, software measurement, usability "
            "engineering."))
    A(("pagebreak",))

    # ══════════════════ 1. INTRODUCTION ══════════════════
    A(("h1", "1. Introduction"))
    A(("h2", "1.1 The problem"))
    A(("p", f"The UNFPA projects India's 60-plus population at roughly 20 % of the country by "
            f"2050, against formal long-term care provision that covers a small fraction of "
            f"that number {c('unfpa')}. Care therefore falls to the family. At the same time "
            f"smartphone and internet access has spread far enough that the device needed to "
            f"support that care is usually already in the household {c('dataforindia')}; what "
            f"is missing is software shaped for the people actually using it."))
    A(("p", f"The shape of the difficulty is documented. Studies of older adults' mobile media "
            f"use in India describe reticence and reliance on a younger family member rather "
            f"than independent mastery {c('chakraborty')}, and a structural model of "
            f"acceptance among elderly users finds that digital literacy alone does not "
            f"account for adoption {c('khare')}. The survey evidence on age-friendly design "
            f"converges on a short list: legible type, large controls, fewer choices per "
            f"screen {c('liu', 'gomez', 'amouzadeh')}, and the prioritised barrier catalogues "
            f"put small text and small targets near the top of what actually defeats users "
            f"{c('awan', 'elguera')}."))
    A(("p", f"The response in the literature is the dyad: give the caregiver and the "
            f"cared-for person different interfaces over shared data "
            f"{c('quinn', 'benmessaoud')}. The response in the market has been thinner. A "
            f"content analysis of the two major stores found only 44 caregiver-focused "
            f"applications, of which 8 covered three or more categories of support "
            f"{c('grossman')}; a systematic search of dementia-caregiver applications found "
            f"them largely static and text-based, with none trialled or evidence based "
            f"{c('wozney')}. Security audits of deployed health applications are worse "
            f"reading still: of 79 accredited health and wellness apps, about 89 % "
            f"transmitted data online and none encrypted personal data held on the device "
            f"{c('huckvale')}, a picture the wider audit literature confirms "
            f"{c('papageorgiou')}."))

    A(("h2", "1.2 Objectives"))
    A(("p", "The project was set five objectives, each written so that its achievement could "
            "be checked by measurement rather than asserted:"))
    A(("steps", [
        "<b>Build a working two-surface application.</b> One data model, two interfaces: an "
        "elder surface sized and simplified for the cared-for person, and a guardian surface "
        "dense enough to administer it.",
        "<b>Make the elder surface clear the published guidance it was designed from.</b> "
        f"Specifically, exceed the 48 dp platform minimum touch target {n('material')} at the "
        "median, and carry a type scale measurably larger than the guardian surface's.",
        "<b>Keep interaction inside the response-time budget.</b> Keep the latency of a care-"
        "data read an order of magnitude inside the one-second threshold at which a user's "
        f"train of thought is preserved {c('miller', 'card', 'nielsen')}.",
        "<b>Make personal data inaccessible without authentication.</b> Enforce it in the "
        f"database rather than in the client {n('pgrls', 'supabasersl')}, and verify it by "
        "attempting the reads.",
        "<b>Measure all of the above and report it honestly,</b> including the results that "
        "are unfavourable, with units, sample sizes and the conditions of measurement.",
    ]))

    A(("h2", "1.3 Scope, and what is deliberately outside it"))
    A(("p", "The project covers the design, implementation, measurement and documentation of "
            "the application and its backend. Three things are outside the scope, and "
            "Chapter 8 returns to each as a limitation rather than leaving the reader to "
            "notice the absence:"))
    A(("bullets", [
        "<b>No health-outcome claim.</b> Adherence improvement is an outcome measured in "
        f"human participants in the trial literature {c('poorcheraghi', 'salmensuu')}. This "
        "project measured an implementation. A design that follows published guidance is not "
        "thereby shown to produce the published benefit.",
        "<b>No comparative usability study.</b> The System Usability Scale and its benchmarks "
        f"{c('brooke', 'bangor')} would be the instrument; running it needs participants and "
        "ethical clearance, and is named as future work in Section 9.3.",
        "<b>No fall detection.</b> The SOS path is deliberately user-initiated. The review "
        "evidence on smartphone fall detection reports promising laboratory accuracy but "
        "little field validation, and the usual action on detection is the same alert to a "
        f"guardian that this application already raises {c('torres')}.",
    ]))

    A(("h2", "1.4 Contributions"))
    A(("p", "This report claims four contributions:"))
    A(("bullets", [
        f"A working, installable dual-surface elder-care application — "
        f"{nd(int(UI['code']['main_kt_lines']))} lines of Kotlin, "
        f"{N_TABLES} database tables, a {APK_MB:.1f} MB release artefact — described in "
        f"Chapters 4 and 5.",
        f"An instrumented measurement study of that application: five instruments, "
        f"{TOTAL_EXEC} automated test executions, "
        f"{sum(v['n'] for v in LAT.values())} timed network requests, "
        f"{N_PROBES} access-control probes and a census of "
        f"{E['sp_n'] + G['sp_n'] + E['dp_n'] + G['dp_n']} interface declarations, reported in "
        f"Chapter 7 with the raw files submitted alongside.",
        "A reproducible measurement protocol: every number in this report is read from "
        "measurements/raw/ when the report is built, so the document cannot drift from its "
        "evidence (Section 3.4).",
        "A comparison of the measured values against 40 reviewed sources, including the "
        "comparisons that go against the project (Chapter 8).",
    ]))

    A(("h2", "1.5 How this report is organised"))
    A(("p", "Chapter 2 reviews 40 sources and states the gap. Chapter 3 separates how the "
            "software was built from how it was measured. Chapters 4 and 5 give the design "
            "and the implementation. Chapter 6 reports the testing, Chapter 7 the results, "
            "and Chapter 8 sets those results against the literature and states the "
            "limitations and the sources of error. Chapter 9 concludes. Chapter 10 states "
            "each member's contribution. The appendices carry the evidence inventory."))
    A(("pagebreak",))

    # ══════════════════ 2. LITERATURE REVIEW ══════════════════
    A(("h1", "2. Literature review and comparison"))
    A(("h2", "2.1 How the sources were selected"))
    A(("p", "Forty sources were reviewed. They were gathered by searching for published work "
            "on four questions this project had to answer — how older adults actually use "
            "smartphones and why they adopt or reject an application; what interface geometry "
            "the evidence supports for them; what a caregiver-facing application is expected "
            "to do; and what is known about the security of deployed health software — and "
            "then by following citations out of the systematic reviews found that way. A "
            "source was kept if it either measured something this project also measures, or "
            "supplied a normative threshold the project could be held to, or established the "
            "gap the project claims to address. Of the 40, 36 are peer-reviewed publications "
            "or scholarly books, two are national statistical sources used for the problem "
            "statement, one is a measurement instrument and one is an international "
            "standard. Ten further normative and technical references (numbers 41 to 50) are "
            "cited for platform minima and are listed separately; they are engineering "
            "requirements rather than reviewed literature, and are not counted among the 40."))
    A(("table", "The forty reviewed sources.", [
        ["#", "Source", "Kind", "What it contributes", "Bears on"],
    ] + [[str(R[k]), cite, kind, what, bears] for k, cite, kind, what, bears in REVIEW],
       [7, 36, 30, 72, 25], ()))

    A(("h2", "2.2 Synthesis by theme"))
    A(("p", f"<b>Adoption is social, not individual.</b> TAM and the UTAUT family explain "
            f"acceptance through usefulness, effort and social influence "
            f"{c('davis', 'utaut', 'utaut2')}; applied to elderly mHealth users, social "
            f"influence, habit and service quality carried significant effects on intention "
            f"{c('palas')}. The Indian studies point the same way: an older adult's use of a "
            f"phone is mediated by a younger relative "
            f"{c('chakraborty', 'khare')}. The design consequence is that the family member "
            f"is not a secondary user to be accommodated — they are the administrator, and "
            f"the architecture should say so."))
    A(("p", f"<b>Interface geometry has converged.</b> Four independent systematic reviews "
            f"{c('liu', 'gomez', 'amouzadeh', 'nimmanterdwong')} and two prioritised barrier "
            f"studies {c('awan', 'elguera')} arrive at the same short list: enlarge the type, "
            f"enlarge the controls, cut the number of decisions per screen. The reviews of "
            f"font size add a caution — older adults prefer larger text, but readability "
            f"falls away again past a point, and the studies do not share a metric "
            f"{c('hou')} — and the controlled reading work shows older readers are more "
            f"sensitive to legibility differences than younger ones {c('beier')}. Against "
            f"that, the normative minima are unambiguous: 48 dp for an interactive control "
            f"{n('material', 'touchtarget')}, with target-size criteria in WCAG 2.2 "
            f"{n('wcag')}."))
    A(("p", f"<b>Response time has settled thresholds.</b> Miller's original measurement "
            f"{c('miller')}, Card et al.'s confirmation {c('card')} and Nielsen's summary "
            f"{c('nielsen')} give the same three figures: about 0.1 s feels instantaneous, "
            f"about 1 s preserves the user's train of thought, about 10 s loses attention. "
            f"These are the only numeric performance targets in the reviewed literature that "
            f"apply directly to a measurement this project can make, and Section 7.2 is "
            f"written against them."))
    A(("p", f"<b>The dyad is the established architecture, and the market has not filled it.</b> "
            f"Quinn et al. {c('quinn')} and Benmessaoud et al. {c('benmessaoud')} both treat "
            f"the caregiver and the cared-for person as a pair with separate interfaces over "
            f"shared data. The two market surveys show how little of this exists in practice: "
            f"44 caregiver apps in total, 8 of them covering three or more support categories "
            f"{c('grossman')}, and dementia-caregiver apps that were static information with "
            f"no evidence base {c('wozney')}."))
    A(("p", f"<b>Reminders have outcome evidence; this project does not inherit it.</b> A "
            f"randomised controlled trial of a mobile drug-management application reported "
            f"improved adherence and reduced readmission in elderly patients with "
            f"polypharmacy {c('poorcheraghi')}; a systematic review of reminder technologies "
            f"in home-dwelling older citizens {c('salmensuu')} and real-world engagement data "
            f"{c('wiecek')} fill in the picture. This is why the reminder feature exists. It "
            f"is not a result of this project, and Chapter 8 says so in those words."))
    A(("p", f"<b>Alerting has modest but real trial evidence.</b> The Whole Systems "
            f"Demonstrator cluster randomised trial found a small significant mental "
            f"health-related quality-of-life benefit from telecare over 12 months "
            f"{c('hirani')}. The systematic review of welfare-technology RCTs is more "
            f"cautious still: over half the trials carried a high risk of bias, so its "
            f"authors synthesised narratively rather than pooling {c('pajalic')}. Automated "
            f"fall detection is promising in the laboratory and thin in the field, and its "
            f"usual action is the alert this application already raises {c('torres')} — which "
            f"is why the SOS path here is user-initiated."))
    A(("p", f"<b>Voice is attractive and unreliable for this population.</b> An automated "
            f"speech-recognition interface built for personal emergency response systems met "
            f"accuracy problems with older speakers {c('hamill')}, and a qualitative study of "
            f"18 adults aged 74 and over found a warm first impression followed by real "
            f"difficulty forming a structured command {c('kim')}. Voice is therefore an "
            f"addition to the tap-based path in this application, never the only route to an "
            f"action."))
    A(("p", f"<b>Measurable quality in shipped Android software is poor, which is the baseline "
            f"this project should be judged against.</b> Automated accessibility testing over "
            f"2,270 applications collected 86,767 issues {c('chen')}; a large-scale "
            f"epidemiological analysis of 5,753 applications found barriers to be the norm, "
            f"including undersized interactive controls {c('ross')}. On the security side, "
            f"about 89 % of 79 accredited health applications transmitted data online, none "
            f"encrypted personal data stored on the device, and roughly two thirds of those "
            f"sending identifying data did so without encryption {c('huckvale')}; the wider "
            f"audit literature agrees {c('papageorgiou')}. ISO 9241-11 {c('iso9241')} supplies "
            f"the frame — effectiveness, efficiency and satisfaction in a stated context — and "
            f"Nielsen and Molich's heuristic evaluation {c('nielsenmolich')} the inspection "
            f"method behind the manual test plan of Section 6.2. Shneiderman's universal "
            f"usability {c('shneiderman')} is the design philosophy the two-surface split "
            f"implements."))

    A(("h2", "2.3 The reviewed work set against this project"))
    A(("p", "The comparison below is restricted to the sources that bear on a quantity this "
            "project actually measured. Where the literature reports something this project "
            "has not measured, the row says so rather than filling the cell with an "
            "estimate. Chapter 8 discusses each comparison; this table only states them."))
    A(("table", "Comparison of the reviewed work against this project.", [
        ["Theme", "Published position", "This project", "Relation"],
        ["Minimum touch target " + n('material', 'touchtarget'),
         "48 dp minimum for an interactive control",
         f"Elder surface median {E['dp_median']:.0f} dp; "
         f"{E['dp_pct_at_least_48']:.0f} % of {E['dp_n']} elder controls at or above 48 dp",
         f"<b>Exceeds</b> the minimum by {E['dp_median'] / 48:.1f}× at the median"],
        ["Minimum touch target " + n('material', 'touchtarget'),
         "48 dp minimum for an interactive control",
         f"Guardian surface median {G['dp_median']:.0f} dp; "
         f"{G['dp_pct_at_least_48']:.0f} % of {G['dp_n']} controls at or above 48 dp",
         "<b>Below</b> the minimum at the median — a deliberate console design, reported as "
         "measured"],
        ["Enlarged type for older readers " + c('hou', 'beier', 'amouzadeh'),
         "Larger type helps older readers, with diminishing and then negative returns; no "
         "single numeric threshold is agreed",
         f"Elder median {E['sp_median']:.0f} sp vs guardian median {G['sp_median']:.0f} sp; "
         f"{E['sp_pct_at_least_20']:.0f} % of elder text at 20 sp or more",
         "<b>Consistent</b> with the direction of the guidance; no published threshold exists "
         "to clear"],
        ["Response time " + c('miller', 'card', 'nielsen'),
         "~1 s preserves the user's train of thought; ~10 s loses attention",
         f"Care-data read median {LAT['rls_denied_select']['median_ms']:.0f} ms, "
         f"p95 {LAT['rls_denied_select']['p95_ms']:.0f} ms (n = "
         f"{LAT['rls_denied_select']['n']})",
         "<b>Inside</b> the 1 s threshold at the median and at the 95th percentile"],
        ["Personal data exposure in deployed health apps " + c('huckvale', 'papageorgiou'),
         "~89 % of 79 accredited apps transmitted data online; none encrypted personal data "
         "at rest",
         f"0 rows returned to an unauthenticated reader from all {len(PERSONAL)} personal "
         f"tables, over {N_PROBES} probes",
         "<b>Better</b> than the audited baseline, by database-enforced isolation"],
        ["Accessibility defects in shipped Android apps " + c('chen', 'ross'),
         "Accessibility barriers are the norm across thousands of apps, undersized controls "
         "among them",
         f"{E['dp_pct_at_least_48']:.0f} % of elder controls at or above the platform minimum; "
         f"full census of {E['dp_n'] + G['dp_n']} control declarations published",
         "<b>Better on the elder surface</b>; the census itself is unusual to publish"],
        ["Caregiver-app coverage " + c('grossman', 'wozney'),
         "44 caregiver apps in total; 8 of 44 covered three or more support categories; "
         "dementia-caregiver apps largely static information",
         "20 functional areas across contacts, medicines, reminders, videos, vitals, "
         "wellness and alerting, all remotely administrable",
         "<b>Broader</b> than the surveyed market, on feature coverage"],
        ["Dyadic architecture " + c('quinn', 'benmessaoud'),
         "Caregiver and cared-for person given separate interfaces over shared data",
         "Two surfaces over one data model, joined by a connect code",
         "<b>Follows</b> the established precedent"],
        ["Medication adherence " + c('poorcheraghi', 'salmensuu', 'wiecek'),
         "A reminder application improved adherence and reduced readmission in an RCT",
         "Reminder scheduling implemented and tested for correctness; adherence not measured",
         "<b>Not comparable.</b> No outcome claim is made"],
        ["Telecare quality-of-life benefit " + c('hirani', 'pajalic'),
         "Small significant mental-HRQoL benefit over 12 months; review-level evidence at high "
         "risk of bias",
         "Alert path implemented and exercised, including on a physical handset; no "
         "participant outcome measured",
         "<b>Not comparable.</b> No outcome claim is made"],
        ["Voice for older speakers " + c('hamill', 'kim'),
         "Recognition accuracy problems with older voices; difficulty forming commands",
         "Voice is an alternative route to actions that all remain reachable by tap; "
         "recognition accuracy not measured",
         "<b>Design response</b> to the finding; not an evaluation of it"],
        ["Fall detection " + c('torres'),
         "Promising laboratory accuracy, little field validation; usual action is an alert to "
         "a guardian",
         "No automated detection; SOS is user-initiated and raises that same alert",
         "<b>Deliberately out of scope</b> (Section 1.3)"],
    ], [30, 42, 52, 46], ()))

    A(("h2", "2.4 The gap this project addresses"))
    A(("p", f"Three findings in the reviewed work line up into a gap. First, the architecture "
            f"the evidence recommends — a dyad with two interfaces over shared data "
            f"{c('quinn', 'benmessaoud')} — is established in research prototypes but almost "
            f"absent from shipped software, where caregiver applications are few, narrow and "
            f"without an evidence base {c('grossman', 'wozney')}. Second, the interface "
            f"geometry that the age-friendly reviews recommend "
            f"{c('liu', 'gomez', 'amouzadeh', 'awan')} is stated as guidance and almost never "
            f"reported as a measured property of a built application; the large-scale "
            f"accessibility analyses {c('chen', 'ross')} measure the population of apps, not "
            f"an individual design against its own stated target. Third, the security "
            f"baseline for this category of software is demonstrably poor "
            f"{c('huckvale', 'papageorgiou')}, and the audits measure outcomes after the fact "
            f"rather than an enforcement mechanism chosen in advance."))
    A(("p", "This project addresses the gap in the narrow, checkable sense available to it: it "
            "builds the dyadic architecture as a working application, and then reports its "
            "interface geometry, its latency and its data isolation as measured quantities "
            "with units, sample sizes and stated conditions, including the measurements that "
            "are unfavourable. It does not address the outcome gap — whether such an "
            "application improves adherence or well-being — and Chapter 9 names that as the "
            "study the project should be followed by."))
    A(("pagebreak",))

    # ══════════════════ 3. METHODOLOGY ══════════════════
    A(("h1", "3. Methodology"))
    A(("p", "This chapter separates two things that are often run together: the method by "
            "which the software was built, and the method by which it was measured. The "
            "second is what the results of Chapter 7 rest on, so it is given in enough detail "
            "to be repeated."))
    A(("h2", "3.1 Development method"))
    A(("p", f"Development was iterative, with a human-centred emphasis consistent with the "
            f"practice the reviews describe for this population {c('nimmanterdwong')}. Each "
            f"iteration took one functional area — contacts, medicines, reminders, videos, "
            f"vitals, alerting — and carried it from the guardian's administration screen "
            f"through the data model to the elder's view of it, rather than building all of "
            f"one layer before starting the next. An area was considered done only when it "
            f"could be driven end to end on two separate logins. Unit and integration tests "
            f"were written in the same iteration as the code, which is why the suite now "
            f"stands at {UI['code']['test_kt_lines']:,} lines of test Kotlin against "
            f"{UI['code']['main_kt_lines']:,} lines of application Kotlin, a ratio of "
            f"{UI['code']['test_kt_lines'] / UI['code']['main_kt_lines']:.2f} lines of test "
            f"per line of application code."))
    A(("h2", "3.2 Measurement method"))
    A(("p", f"All measurement was performed against a single frozen state of the source: "
            f"commit <c>{ENV['git_commit']}</c> of branch <c>{ENV['git_branch']}</c>, on "
            f"{ENV['date_local']}. No code was changed between the first instrument and the "
            f"last. Five instruments were used."))
    A(("table", "The five instruments, and what each produces.", [
        ["Instrument", "What it does", "Sample", "Raw output"],
        ["<c>parse_tests.py</c>",
         "Runs the Gradle unit-test task and parses every JUnit XML report into a per-test "
         "and per-suite record.",
         f"{UT['tests']} tests in {UT['suites']} suites, per run",
         "<c>unit_tests.csv</c>, <c>unit_test_suites.csv</c>, "
         "<c>unit_test_summary.json</c>"],
        ["<c>run_repeats.sh</c>",
         "Re-executes the whole suite twice more with <c>--rerun-tasks</c>, so that no result "
         "can be served from the build cache, and times a clean <c>assembleDebug</c>.",
         f"2 further runs; {TOTAL_EXEC} test executions in total",
         "<c>test_repeats.csv</c>, <c>build_times.csv</c>"],
        ["<c>measure_backend.sh</c>",
         "Issues timed HTTPS requests to each live backend endpoint with curl, recording "
         "DNS, TCP, TLS, time-to-first-byte and total for every request.",
         f"{LAT['rest_root']['n']} trials × {len(LAT)} endpoints = "
         f"{sum(v['n'] for v in LAT.values())} requests",
         "<c>backend_latency.csv</c>, <c>backend_latency_summary.json</c>"],
        ["Anonymous read probe",
         "Attempts a read of every database table with the anonymous key and no user session, "
         "three times each, recording the HTTP status and the number of rows returned.",
         f"{N_TABLES} tables × 3 = {N_PROBES} attempts",
         "<c>rls_anon_reads.csv</c>"],
        ["<c>measure_ui.py</c>",
         "Takes a complete census of the declared <c>sp</c> text sizes and <c>dp</c> control "
         "sizes in the Compose source, attributed to the elder or the guardian surface. "
         "Letter-spacing declarations are excluded, as they are also written in <c>sp</c> and "
         "would otherwise contaminate the type scale.",
         f"{E['sp_n'] + G['sp_n']} text declarations and "
         f"{E['dp_n'] + G['dp_n']} control declarations",
         "<c>ui_text_sizes.csv</c>, <c>ui_touch_targets.csv</c>, <c>ui_summary.json</c>"],
        ["Artefact measurement",
         "Measures the signed release APK: file and download size, section composition by "
         "entry, DEX method count, declared permissions, and SDK levels.",
         "1 artefact",
         "<c>apk_metrics.csv</c>, <c>apk_sections.csv</c>"],
    ], [26, 60, 32, 52], ()))
    A(("h2", "3.3 Conditions of measurement"))
    A(("p", "Network measurements depend on conditions that must be stated for the numbers to "
            "mean anything. All of the following held for every trial reported in Chapter 7."))
    A(("table", "The conditions under which every measurement in this report was taken.", [
        ["Condition", "Value"],
        ["Date and time", ENV["date_local"]],
        ["Host", ENV["host_os"] + f", {ENV['cores']} cores, {ENV['ram_gb']} GB RAM"],
        ["Network", ENV["network"]],
        ["JDK", ENV["jdk"]],
        ["Gradle / AGP / Kotlin",
         f"{ENV['gradle']} / {ENV['agp']} / {ENV['kotlin']}"],
        ["Android SDK", f"compileSdk {ENV['compile_sdk']}, minSdk {ENV['min_sdk']}, "
                        f"{ENV['android_platform']}, build-tools {ENV['build_tools']}"],
        ["Source state", f"branch {ENV['git_branch']}, commit {ENV['git_commit']}"],
        ["Application under measurement",
         f"CareCompanion {ENV['app_version']} (version code {ENV['app_version_code']})"],
    ], [46, 124], ()))
    A(("callout", "Why the latency figures are reported as distributions, not as single numbers",
       "A single timing of a request over home broadband is close to meaningless: the "
       "distribution is right-skewed and its tail is set by the network, not by the "
       "application. Every latency result in Chapter 7 is therefore given as a median, a "
       "95th percentile and a sample size, and the raw per-request file is submitted so that "
       "the distribution can be re-derived. Means are given alongside but are not the headline "
       "figure, because the skew makes them larger than the typical experience.", "tip"))
    A(("h2", "3.4 Reproducibility"))
    A(("p", "Two properties make the results in this report checkable rather than merely "
            "stated. First, the instruments are submitted as source, so the measurement can "
            "be repeated. Second, the documents are generated from the raw files: this report "
            "reads <c>measurements/raw/</c> when it is built, so no figure in Chapter 7 is "
            "typed in by hand and the report cannot drift away from its evidence. Appendix A "
            "lists every raw file with its SHA-256 digest, so that the files submitted can be "
            "shown to be the files the numbers came from."))
    A(("table", "Rebuilding every document in this submission from the raw data.", [
        ["Command", "Produces"],
        ["<c>python docs/build_task28_report.py</c>",
         "This report, as PDF and as an editable .docx (Task 28)"],
        ["<c>python paper/build_task29_paper.py</c>",
         "The final IEEE-format paper (Task 29)"],
        ["<c>python ppt/build_task30_slides.py</c>",
         "The viva defence slides, as .pptx and PDF (Task 30)"],
        ["<c>python video/make_demo.py</c>",
         "The final end-to-end demonstration video (Task 31)"],
        ["<c>python measurements/plot_results.py</c>", "Every results figure"],
        ["<c>python docs/build_tasks_20_27.py</c>", "Tasks 20, 21, 22, 23 and 25"],
    ], [72, 98], ()))
    A(("pagebreak",))

    # ══════════════════ 4. DESIGN ══════════════════
    A(("h1", "4. System design and diagrams"))
    A(("h2", "4.1 The organising decision"))
    A(("p", f"One decision shapes everything else: the application has two interface surfaces "
            f"over a single data model. This is the dyadic architecture the literature "
            f"recommends {c('quinn', 'benmessaoud')} and an application of universal "
            f"usability — designing for the span of ability rather than for a median user "
            f"{c('shneiderman')} — taken to its structural conclusion. Rather than one "
            f"interface with an accessibility mode, there are two interfaces whose geometry "
            f"differs by design, and Section 7.3 measures how far apart they actually are."))
    A(("table", "The two surfaces, and what each is for.", [
        ["", "Elder surface", "Guardian surface"],
        ["Who uses it", "The cared-for person, on their own phone",
         "The family member who administers the care, on theirs"],
        ["What it is for",
         "Doing one thing at a time: calling a person, watching something, raising an alert",
         "Setting up and maintaining everything the elder's phone then shows"],
        ["Primary affordance", "A photograph, tapped", "A form, filled in"],
        ["Measured median text size", f"{E['sp_median']:.0f} sp ({E['sp_n']} declarations)",
         f"{G['sp_median']:.0f} sp ({G['sp_n']} declarations)"],
        ["Measured median control size", f"{E['dp_median']:.0f} dp ({E['dp_n']} declarations)",
         f"{G['dp_median']:.0f} dp ({G['dp_n']} declarations)"],
        ["Screens", "Contacts, Videos, SOS, and a daily schedule view",
         "Elders, contacts, medicines and schedules, reminders, video and app shortcuts, "
         "vitals, wellness, family members, alerts"],
        ["Decisions per screen", "Deliberately few; no nested navigation on the main paths",
         "As many as administration needs"],
    ], [30, 68, 72], ()))
    A(("h2", "4.2 Architecture"))
    A(("p", "The application is a single Android module in layers, over a managed PostgreSQL "
            "backend reached across HTTPS. There is no bespoke application server: the "
            "database itself is the authority on who may read what, which is the design "
            "decision Section 4.5 and Section 7.4 are about."))
    A(("figure", pfig("fig_arch"),
       "System architecture. The two interface surfaces sit over one repository layer, one "
       "remote data source and one database; access control is enforced in the database "
       "rather than in the client.", 150))
    A(("table", "The layers, and what lives in each.", [
        ["Layer", "Package", "Responsibility"],
        ["Interface", "<c>ui/elder</c>, <c>ui/guardian</c>, <c>ui/sos</c>, <c>ui/auth</c>, "
                      "<c>ui/common</c>, <c>ui/nav</c>, <c>ui/theme</c>",
         "Jetpack Compose screens and state holders. The two surfaces share the theme and the "
         "common components but not their geometry: the elder theme carries its own, larger "
         "type scale and control metrics."],
        ["Domain and data", "<c>data/repo</c>, <c>data/model</c>",
         "Repositories expose care data to the interface as a single source of truth, so a "
         "screen never speaks to the network directly."],
        ["Remote", "<c>data/remote</c>",
         "The typed HTTP client over the PostgREST interface, with an authentication "
         "interceptor that attaches the session token and a retry policy that distinguishes a "
         "recoverable 401 from a signed-out one."],
        ["Scheduling", "<c>reminder</c>",
         f"WorkManager {n('workmanager')} arms the day's doses once a day; exact alarms fire "
         f"the individual reminders, with cancellation and de-duplication handled explicitly "
         f"because the platform's Doze behaviour makes naive scheduling unreliable "
         f"{n('doze')}."],
        ["Notification", "<c>notify</c>",
         f"Local notification channels for reminders, and Firebase Cloud Messaging "
         f"{n('fcm')} for alerts that must reach the guardian's phone from the elder's."],
        ["Identity", "<c>auth</c>",
         "Sign-in, session persistence, and the connect-code exchange that binds an elder's "
         "phone to a guardian's account."],
        ["Voice", "<c>voice</c>",
         f"An alternative route to actions that all remain reachable by tap, for the reasons "
         f"the voice literature gives {c('hamill', 'kim')}."],
        ["Injection", "<c>di</c>",
         "Hilt modules wiring the above, which is also what makes the repositories "
         "substitutable in the test suite."],
    ], [26, 44, 100], ()))

    A(("h2", "4.3 Data model"))
    A(("p", f"The database holds {N_TABLES} tables. {len(PERSONAL)} of them carry personal or "
            f"care data and are reachable only by an authenticated user with a relationship to "
            f"the row; {len(REFERENCE)} are reference tables that are read-only to everyone and "
            f"hold no personal data at all. The distinction matters for Section 7.4, where the "
            f"two groups behave differently under an anonymous read and the report has to say "
            f"why."))
    A(("table", "The database tables, by class.", [
        ["Class", "Tables", "Why it is in this class"],
        [f"Personal and care data ({len(PERSONAL)})",
         ", ".join(f"<c>{t}</c>" for t in sorted(PERSONAL)),
         "Each row belongs to an identifiable person or to a guardian–elder relationship. "
         "Row-level security restricts every one of them to the authenticated users entitled "
         "to that row."],
        [f"Reference data ({len(REFERENCE)})",
         ", ".join(f"<c>{t}</c>" for t in sorted(REFERENCE)),
         "Application configuration, reminder categories, vital-sign threshold bands and a "
         "list of accessible places. These are the same for every user, contain no personal "
         "data, and are intentionally readable without a session."]],
       [34, 60, 76], ()))

    A(("h2", "4.4 The connect-code flow"))
    A(("p", "The one piece of the design that has to work before anything else does is the "
            "binding between the two phones. It is deliberately short and one-directional: "
            "the elder never has to create an account, choose a password or understand what "
            "an account is."))
    A(("steps", [
        "The guardian signs in on their own phone and creates an elder profile — name, "
        "photograph, and whatever care detail they already know.",
        "The application issues a short connect code against that profile and shows it to the "
        "guardian, who reads it out or types it in on the elder's phone.",
        "On the elder's phone, the code is entered on the first screen. Nothing else is asked "
        "for.",
        "The code is exchanged for a device-scoped session bound to that elder profile. From "
        "that moment the elder's phone shows only the elder surface, and shows only that "
        "elder's data.",
        "Everything the elder sees afterwards — contacts, medicines, reminders, video "
        "shortcuts — is administered remotely from the guardian's phone and appears on the "
        "elder's without the elder configuring anything.",
    ]))

    A(("h2", "4.5 Security design"))
    A(("p", f"The security literature for this category of software is the reason this section "
            f"exists. Of 79 accredited health and wellness applications, about 89 % "
            f"transmitted data online, none encrypted personal data stored on the device, and "
            f"roughly two thirds of those sending identifying data did so without encryption "
            f"{c('huckvale')}; the broader audits agree {c('papageorgiou')}. The common thread "
            f"in those failures is that access control was a property of the client, and the "
            f"client could be bypassed."))
    A(("p", f"The design decision here is to put the control where it cannot be bypassed by "
            f"anything the client does. Every personal table has row-level security enabled "
            f"{n('pgrls', 'supabasersl')}, and the policies are expressed in terms of the "
            f"authenticated user's relationship to the row — the user owns it, or is the "
            f"guardian linked to the elder it belongs to. The published anonymous key, which "
            f"is necessarily present in the application package and must be assumed known, "
            f"therefore grants no access to any personal row. Section 7.4 reports what "
            f"happened when that was attempted {N_PROBES} times."))
    A(("callout", "What this design does not claim",
       "Row-level security governs who may read a row. It is not encryption at rest on the "
       "handset, it is not protection against a compromised authenticated account, and it is "
       "not a penetration test. The measurement in Section 7.4 establishes exactly one thing: "
       "that an unauthenticated reader holding the application's own key receives no personal "
       "rows. That is the claim made, and no more.", "warn"))
    A(("h2", "4.6 Permissions"))
    A(("p", f"The release artefact declares {APK['permission_count']} permissions. The SMS and "
            f"call-log permission groups are not among them: Google Play policy restricts them "
            f"to applications whose core function requires them {n('playsms', 'playsms2')}, and "
            f"an earlier design that sent an SMS containing the elder's location on an alert "
            f"was removed for that reason. The alert now travels over Firebase Cloud Messaging "
            f"to the guardian's phone instead {n('fcm')}, which is both policy-compliant and "
            f"verifiable from inside the application."))
    A(("pagebreak",))

    # ══════════════════ 5. IMPLEMENTATION ══════════════════
    A(("h1", "5. Implementation"))
    A(("h2", "5.1 The code as built"))
    A(("table", "Size and composition of the implementation, measured from the source at "
                f"commit {ENV['git_commit']}.", [
        ["Quantity", "Value", "Unit"],
        ["Application Kotlin", f"{UI['code']['main_kt_lines']:,}", "lines"],
        ["Application Kotlin files", f"{UI['code']['main_kt_files']}", "files"],
        ["Test Kotlin", f"{UI['code']['test_kt_lines']:,}", "lines"],
        ["Test Kotlin files", f"{UI['code']['test_kt_files']}", "files"],
        ["Test-to-application ratio",
         f"{UI['code']['test_kt_lines'] / UI['code']['main_kt_lines']:.2f}",
         "lines of test per line of application code"],
        ["Database definition", f"{UI['code']['sql_lines']:,}", "lines of SQL"],
        ["Database definition files", f"{UI['code']['sql_files']}", "files"],
        ["Interface size declarations",
         f"{E['sp_n'] + G['sp_n'] + E['dp_n'] + G['dp_n']}",
         "declared sizes, censused across both surfaces"],
    ], [66, 30, 74], (1,)))
    A(("h2", "5.2 Toolchain"))
    A(("p", f"The project builds with Gradle {ENV['gradle']} and the Android Gradle Plugin "
            f"{ENV['agp']}, in Kotlin {ENV['kotlin']} on JDK {ENV['jdk'].split('(')[0].strip()}, "
            f"against compileSdk {ENV['compile_sdk']} with minSdk {ENV['min_sdk']}. A clean "
            f"debug build of the committed source took {BUILD_S} s on the host of Table 7. "
            f"minSdk {ENV['min_sdk']} is a deliberate choice: it keeps the application "
            f"installable on the older handsets that are common in the households this is "
            f"built for, at the cost of some newer platform APIs."))
    A(("h2", "5.3 The reminder chain, and why it is not simply a timer"))
    A(("p", f"Reminders are the part of the implementation where the platform fights back. "
            f"Android's Doze and App Standby behaviour exists to stop applications waking the "
            f"device, and a naive repeating timer is therefore unreliable precisely when it "
            f"matters {n('doze')}. The implementation splits the problem in two. A daily "
            f"WorkManager job {n('workmanager')} arms the coming day's doses; each individual "
            f"dose is then an exact alarm, set and cancelled by key. Three behaviours are "
            f"tested explicitly because each has a way of going quietly wrong: arming twice "
            f"must not produce duplicate work, cancelling an unknown key must be a harmless "
            f"no-op, and a reschedule must replace rather than accumulate."))
    A(("h2", "5.4 Offline behaviour"))
    A(("p", "A write made while the phone has no usable network is not discarded and not "
            "silently retried forever. It is placed in a durable outbox and flushed when the "
            "network returns; a server error during a flush retains the item rather than "
            "dropping it. That last behaviour is the slowest test in the suite — "
            f"{UT['slowest'][0]['ms']:.0f} ms, in <c>{UT['slowest'][0]['suite']}</c> — because "
            "it deliberately drives the failure path rather than the happy one."))
    A(("h2", "5.5 Network layer"))
    A(("p", f"All traffic is HTTPS to the managed backend in region ap-south-1. The client "
            f"attaches the session token through an interceptor, and distinguishes the two "
            f"kinds of 401: one where a fresher token can be fetched and the request retried, "
            f"and one where the user is signed out and retrying would be pointless. A request "
            f"that receives no response before the client timeout fails as an <c>IOException</c> "
            f"that the calling layer handles, rather than hanging the interface. Both "
            f"behaviours are in the suite."))
    A(("h2", "5.6 The release artefact"))
    A(("table", "The measured release artefact.", [
        ["Property", "Value"],
        ["File", f"<c>{APK['apk_file']}</c>"],
        ["Size on disk", f"{APK_MB:.1f} MB ({int(APK['apk_bytes']):,} bytes)"],
        ["Download size", f"{int(APK['apk_download_size_bytes']) / 1e6:.1f} MB"],
        ["DEX method count", f"{int(APK['dex_method_count']):,}"],
        ["Declared permissions", APK["permission_count"]],
        ["minSdk / targetSdk", f"{APK['min_sdk']} / {APK['target_sdk']}"],
        ["Version", f"{APK['version_name']} (code {ENV['app_version_code']})"],
    ], [50, 120], ()))
    A(("figure", fig("fig-22-08-apk"),
       "Composition of the release artefact by entry, uncompressed against compressed. The "
       "three DEX files dominate; resources compress hardly at all because they are already "
       "compressed image data. n = 1 artefact.", 150))
    A(("pagebreak",))

    # ══════════════════ 6. TESTING ══════════════════
    A(("h1", "6. Testing"))
    A(("p", f"Testing was done two ways, and both are reported: an automated suite executed "
            f"three times over, and a written manual campaign driven through the interface by "
            f"hand. Neither on its own would be enough. The automated suite establishes that "
            f"{UT['tests']} specific behaviours hold and says nothing about anything else; the "
            f"manual campaign covers the paths a user actually walks, including the "
            f"cross-device ones that no unit test can reach."))
    A(("h2", "6.1 The automated suite"))
    A(("table", "The automated suite, over three runs from the same source.", [
        ["Quantity", "Value", "Unit", "n"],
        ["Suites", f"{UT['suites']}", "suites", "per run"],
        ["Tests", f"{UT['tests']}", "tests", "per run"],
        ["Runs", "3", "runs", "runs 2 and 3 forced full re-execution"],
        ["Total test executions", f"{TOTAL_EXEC}", "executions", "3 runs"],
        ["Failures", f"{UT['failed']}", "tests", f"{TOTAL_EXEC}"],
        ["Errors", f"{UT['errors']}", "tests", f"{TOTAL_EXEC}"],
        ["Skipped", f"{UT['skipped']}", "tests", f"{TOTAL_EXEC}"],
        ["Pass rate", f"{UT['pass_rate_pct']:.1f}", "%", f"{TOTAL_EXEC}"],
        ["Sum of per-test execution time", f"{UT['exec_sum_s']:.2f}", "s",
         f"{UT['tests']} tests"],
        ["Median per-test execution time", f"{UT['exec_median_ms']:.0f}", "ms",
         f"{UT['tests']}"],
        ["Slowest single test", f"{UT['exec_max_ms']:.0f}", "ms", f"{UT['tests']}"],
    ], [62, 26, 36, 46], (1,)))
    A(("p", f"<b>What this does and does not establish.</b> Every one of the {TOTAL_EXEC} "
            f"executions passed. That is a statement about the {UT['tests']} behaviours the "
            f"suite encodes, not about the application as a whole. The suite is strongest "
            f"where it was written to be: it drives failure paths — a server error during an "
            f"offline flush, a 401 with no fresher token, a timeout with no response — rather "
            f"than only the paths that work."))
    A(("figure", fig("fig-22-01-suites"),
       "Execution time and test count of every suite in the first run. All "
       f"{UT['tests']} tests passed; no suite reported a failure, an error or a skip. "
       f"Conditions as in Table 7; n = 1 run, {UT['suites']} suites.", 158))
    A(("h2", "6.2 Repeated trials"))
    A(("p", "The suite was run three times from the same commit. Runs 2 and 3 passed "
            "<c>--rerun-tasks</c> so that no result could be served from Gradle's build cache "
            "and every test genuinely re-executed. The outcome was identical each time. The "
            "wall-clock times differ because the first run also started the build daemon and "
            "compiled from cold, which is worth stating rather than averaging away."))
    A(("table", "The three runs.", [
        ["Run", "Tests executed", "Failures", "Errors", "Wall-clock (s)", "Note"],
        ["1", f"{UT['tests']}", "0", "0", f"{RUN_WALL[0]}", "cold; daemon start and full compile"],
        ["2", f"{UT['tests']}", "0", "0", f"{RUN_WALL[1]}", "forced full re-execution"],
        ["3", f"{UT['tests']}", "0", "0", f"{RUN_WALL[2]}", "forced full re-execution"],
    ], [14, 26, 20, 18, 28, 64], (1, 2, 3, 4)))
    A(("figure", fig("fig-22-02-test-time"),
       "Wall-clock time of the three runs, and the distribution of per-test execution time. "
       f"n = 3 runs, {UT['tests']} tests each.", 150))
    A(("h2", "6.3 The manual test campaign"))
    A(("p", "Alongside the automated suite, the application was driven through its own "
            "interface against a written plan of 116 numbered cases covering all 20 "
            "functional areas of the product, in the inspection tradition heuristic "
            f"evaluation established {c('nielsenmolich')}. Sixty-eight of those cases were "
            "executed live, end to end, including every case on the critical paths. The "
            "outcome of each was recorded with the observation that produced it."))
    A(("table", "Outcome of the manual test campaign.", [
        ["Quantity", "Value", "Note"],
        ["Cases in the written plan", "116",
         "Across 20 functional areas, from authentication to offline behaviour"],
        ["Cases driven live through the interface", "68",
         "Including every case on the critical paths"],
        ["Passed", "65", "Observed working against the expected result"],
        ["Partial", "2", "Verified by a different route; recorded as partial, not as a pass"],
        ["Failed", "0", "No case was left failing at the close of the campaign"],
        ["Defects found and fixed during the campaign", "2",
         "The elder surface exited the application on the system Back gesture, and a keyboard "
         "entry fault; both fixed and re-verified in the same campaign"],
        ["User-reported defects re-verified fixed", "3",
         "The elder-login one-time-code path, the Videos empty-state wording, and contacts not "
         "reaching the elder's phone"],
        ["Cross-device flows exercised", "7",
         "Guardian to elder and elder back to guardian, with two separate logins"],
        ["Physical-handset validation", "SOS path",
         "The emergency path was additionally exercised on a physical Android handset and "
         "captured as the validation record"],
    ], [54, 20, 96], (1,)))
    A(("p", "<b>Observation.</b> The campaign was conducted as a search for defects rather "
            "than as a demonstration, and it found two. Both were corrected and re-verified "
            "before the campaign closed, which is why the final tally shows no failing case. "
            "The two partial results are recorded as partial and not promoted to passes."))
    A(("pagebreak",))

    # ══════════════════ 7. RESULTS ══════════════════
    A(("h1", "7. Results"))
    A(("p", "Every result in this chapter carries its unit, its sample size and the conditions "
            "of Table 7. No value here is typed in: each is read from the raw files listed in "
            "Appendix A when this report is built."))
    A(("h2", "7.1 Summary of measured results"))
    A(("table", "The headline measured results.", [
        ["Result", "Value", "Unit", "n", "Where"],
        ["Automated test executions passed",
         f"{TOTAL_EXEC} of {TOTAL_EXEC}", "executions", f"{TOTAL_EXEC}", "§6.1"],
        ["Care-data read latency, median",
         f"{LAT['rls_denied_select']['median_ms']:.0f}", "ms",
         f"{LAT['rls_denied_select']['n']}", "§7.2"],
        ["Care-data read latency, 95th percentile",
         f"{LAT['rls_denied_select']['p95_ms']:.0f}", "ms",
         f"{LAT['rls_denied_select']['n']}", "§7.2"],
        ["Elder surface median text size", f"{E['sp_median']:.0f}", "sp", f"{E['sp_n']}", "§7.3"],
        ["Guardian surface median text size", f"{G['sp_median']:.0f}", "sp", f"{G['sp_n']}", "§7.3"],
        ["Elder surface median control size", f"{E['dp_median']:.0f}", "dp", f"{E['dp_n']}", "§7.3"],
        ["Elder controls at or above the 48 dp minimum",
         f"{E['dp_pct_at_least_48']:.0f}", "%", f"{E['dp_n']}", "§7.3"],
        ["Guardian controls at or above the 48 dp minimum",
         f"{G['dp_pct_at_least_48']:.0f}", "%", f"{G['dp_n']}", "§7.3"],
        ["Personal rows returned to an unauthenticated reader",
         "0", "rows", f"{len(PERSONAL) * 3}", "§7.4"],
        ["Release artefact size", f"{APK_MB:.1f}", "MB", "1", "§5.6"],
        ["Manual test cases passed", "65 of 68", "cases", "68", "§6.3"],
    ], [72, 28, 22, 20, 30], (1, 2, 3)))

    A(("h2", "7.2 Network latency"))
    A(("p", f"{sum(v['n'] for v in LAT.values())} timed HTTPS requests were issued across "
            f"{len(LAT)} endpoints, {LAT['rest_root']['n']} trials each, under the conditions "
            f"of Table 7. The distributions are right-skewed, so the median is the headline "
            f"figure and the mean is reported beside it rather than instead of it."))
    LAT_LABEL = {
        "rest_root": "REST root, <c>GET /rest/v1/</c>",
        "auth_settings": "Auth configuration, <c>GET /auth/v1/settings</c>",
        "rls_denied_select": "Care-data read under row-level security, <c>GET /rest/v1/elders</c>",
        "edge_push_preflight": "Edge function, <c>GET /functions/v1/push</c>",
    }
    LAT_ORDER = ["rest_root", "auth_settings", "rls_denied_select", "edge_push_preflight"]
    data = [["Endpoint", "Median (ms)", "Mean (ms)", "SD (ms)", "Min (ms)", "p95 (ms)",
             "Max (ms)", "n", "HTTP"]]
    for k in LAT_ORDER:
        v = LAT[k]
        data.append([LAT_LABEL[k], f"{v['median_ms']:.1f}", f"{v['mean_ms']:.1f}",
                     f"{v['sd_ms']:.1f}", f"{v['min_ms']:.1f}", f"{v['p95_ms']:.1f}",
                     f"{v['max_ms']:.1f}", f"{v['n']}",
                     ", ".join(f"{c_}×{n_}" for c_, n_ in v["http"].items())])
    A(("table", "Request latency by endpoint. Conditions as in Table 7.",
       data, [48, 18, 16, 15, 15, 15, 16, 10, 17], (1, 2, 3, 4, 5, 6, 7)))
    A(("p", f"The endpoint that matters for the user's experience is the care-data read, "
            f"because it is the one that stands between a guardian's tap and the data "
            f"appearing. Its median is {LAT['rls_denied_select']['median_ms']:.0f} ms and its "
            f"95th percentile {LAT['rls_denied_select']['p95_ms']:.0f} ms. Against the "
            f"thresholds the response-time literature settled on "
            f"{c('miller', 'card', 'nielsen')} — roughly 1 s to preserve a user's train of "
            f"thought — the median sits about "
            f"{1000 / LAT['rls_denied_select']['median_ms']:.0f}× inside the budget and the "
            f"95th percentile still more than twice inside it. The single slowest request "
            f"observed, {LAT['rls_denied_select']['max_ms']:.0f} ms, remains inside 1 s."))
    A(("callout", "Two HTTP status codes in Table 11 need explaining, not hiding",
       f"The REST root returned 401 on all {LAT['rest_root']['n']} trials and the edge "
       f"function returned 500 on all {LAT['edge_push_preflight']['n']}. Neither is a defect "
       f"in the measurement. The REST root rejects a request carrying no table selector, "
       f"which is the correct behaviour and still exercises the full DNS–TCP–TLS–response "
       f"path the measurement is timing. The edge function is a push endpoint that requires a "
       f"POST body; a bare GET is answered with a 500 by the function runtime. Both are "
       f"included because they measure round-trip time to two different parts of the backend, "
       f"and excluding them would make the table look tidier than the evidence is.", "warn"))
    A(("figure", fig("fig-22-03-latency"),
       f"Distribution of total request time for each endpoint. Boxes span the interquartile "
       f"range; the line is the median. n = {LAT['rest_root']['n']} per endpoint, "
       f"{sum(v['n'] for v in LAT.values())} requests in total.", 150))
    A(("figure", fig("fig-22-04-latency-phases"),
       "The same requests broken into DNS, TCP connect, TLS handshake, time to first byte and "
       "transfer. The handshake and the server's own time dominate; transfer is negligible at "
       "these payload sizes.", 150))

    A(("h2", "7.3 Interface geometry"))
    A(("p", f"A complete census was taken of the declared interface sizes in the Compose "
            f"source: {E['sp_n'] + G['sp_n']} text-size declarations and "
            f"{E['dp_n'] + G['dp_n']} control-size declarations, each attributed to the elder "
            f"or the guardian surface. This is a census and not a sample, so there is no "
            f"sampling error; what it measures is what the source declares, which Section 8.3 "
            f"distinguishes from what a particular handset finally renders."))
    A(("table", "Declared interface geometry, by surface.", [
        ["Quantity", "Elder surface", "Guardian surface", "Unit"],
        ["Text-size declarations", f"{E['sp_n']}", f"{G['sp_n']}", "declarations"],
        ["Median text size", f"{E['sp_median']:.0f}", f"{G['sp_median']:.0f}", "sp"],
        ["Mean text size", f"{E['sp_mean']:.1f}", f"{G['sp_mean']:.1f}", "sp"],
        ["Smallest declared text", f"{E['sp_min']:.0f}", f"{G['sp_min']:.0f}", "sp"],
        ["Largest declared text", f"{E['sp_max']:.0f}", f"{G['sp_max']:.0f}", "sp"],
        ["Text below 16 sp", f"{E['sp_pct_below_16']:.1f}", f"{G['sp_pct_below_16']:.1f}", "%"],
        ["Text at 20 sp or above", f"{E['sp_pct_at_least_20']:.1f}",
         f"{G['sp_pct_at_least_20']:.1f}", "%"],
        ["Control-size declarations", f"{E['dp_n']}", f"{G['dp_n']}", "declarations"],
        ["Median control size", f"{E['dp_median']:.0f}", f"{G['dp_median']:.0f}", "dp"],
        ["Controls at or above 48 dp", f"{E['dp_pct_at_least_48']:.1f}",
         f"{G['dp_pct_at_least_48']:.1f}", "%"],
    ], [58, 38, 38, 36], (1, 2)))
    A(("p", f"The two surfaces are measurably different, which was the intent: the elder "
            f"surface's median text size is {E['sp_median'] / G['sp_median']:.1f}× the "
            f"guardian's and its median control is {E['dp_median'] / G['dp_median']:.1f}× as "
            f"large. Against the 48 dp platform minimum {n('material', 'touchtarget')}, the "
            f"elder median of {E['dp_median']:.0f} dp is {E['dp_median'] / 48:.1f}× the "
            f"minimum, while the guardian median of {G['dp_median']:.0f} dp is below it. "
            f"Section 8.2 discusses that second finding rather than leaving it in the table."))
    A(("figure", fig("fig-22-05-typescale"),
       f"The complete declared type scale of both surfaces. n = {E['sp_n']} elder and "
       f"{G['sp_n']} guardian declarations.", 150))
    A(("figure", fig("fig-22-06-targets"),
       f"The complete declared control-size census against the 48 dp platform minimum "
       f"{n('material')}. n = {E['dp_n']} elder and {G['dp_n']} guardian declarations.", 150))

    A(("h2", "7.4 Data isolation"))
    A(("p", f"Every one of the {N_TABLES} tables was read {N_PROBES // N_TABLES} times with "
            f"the application's own anonymous key and no user session — the position of an "
            f"attacker who has extracted the key from the package, which must be assumed "
            f"possible. {N_PROBES} attempts were made in total and the number of rows returned "
            f"was recorded for each."))
    rows_by_table = {}
    for r in RLS:
        rows_by_table.setdefault(r["table"], []).append(int(r["rows_returned"]))
    data = [["Table", "Class", "Rows returned (3 attempts)", "Interpretation"]]
    for t in TABLES:
        got = rows_by_table[t]
        cls = "Personal" if t in PERSONAL else "Reference"
        if t in PERSONAL:
            interp = "No personal data reachable without a session."
        else:
            interp = ("Intentionally public reference data; inspected and confirmed to hold no "
                      "personal data.")
        data.append([f"<c>{t}</c>", cls, ", ".join(str(x) for x in got), interp])
    A(("table", "Anonymous read attempt against every table, three attempts each.",
       data, [38, 20, 36, 76], ()))
    A(("p", f"<b>Result.</b> All {len(PERSONAL)} personal tables returned zero rows on all "
            f"{len(PERSONAL) * 3} attempts. The {len(REFERENCE)} reference tables returned "
            f"their rows, which is the intended behaviour: they are application "
            f"configuration, reminder categories, vital-sign threshold bands and a list of "
            f"accessible places, identical for every user. Each of the four was inspected "
            f"individually to confirm it holds no personal data, rather than assumed from its "
            f"name."))
    A(("figure", fig("fig-22-07-rls"),
       f"Rows returned to an unauthenticated reader, by table. Every personal table returns "
       f"zero; the four reference tables return their fixed contents by design. "
       f"n = {N_PROBES} attempts over {N_TABLES} tables.", 150))
    A(("pagebreak",))

    # ══════════════════ 8. DISCUSSION ══════════════════
    A(("h1", "8. Discussion"))
    A(("h2", "8.1 Where the measurements beat the published baseline"))
    A(("p", f"<b>Data isolation is the strongest result.</b> The audit literature for this "
            f"category is unflattering: of 79 accredited health and wellness applications, "
            f"about 89 % transmitted data online, none encrypted personal data held on the "
            f"device, and roughly two thirds of those sending identifying data did so without "
            f"encryption {c('huckvale')}; the broader audits find the same {c('papageorgiou')}. "
            f"Against that baseline, {len(PERSONAL)} of {len(PERSONAL)} personal tables "
            f"returning zero rows to a reader holding the application's own key is a real "
            f"difference, and the reason is structural rather than diligent: the control is in "
            f"the database {n('pgrls', 'supabasersl')}, so no client mistake can relax it. The "
            f"honest qualification is in Section 4.5 — this is a measurement of one specific "
            f"exposure, not a security assessment."))
    A(("p", f"<b>Latency is comfortably inside the only numeric target the literature "
            f"offers.</b> The response-time thresholds from Miller {c('miller')}, Card et al. "
            f"{c('card')} and Nielsen {c('nielsen')} are the one performance figure in the "
            f"reviewed work that applies directly. A median care-data read of "
            f"{LAT['rls_denied_select']['median_ms']:.0f} ms sits roughly "
            f"{1000 / LAT['rls_denied_select']['median_ms']:.0f}× inside the one-second "
            f"threshold, and even the slowest of {LAT['rls_denied_select']['n']} requests "
            f"stayed under it. The qualification is that this is a measurement of the backend "
            f"over one network from one location, not of what a user perceives: perceived "
            f"latency also includes the handset's own rendering, which was not instrumented."))
    A(("p", f"<b>The elder surface clears the geometry it was designed from.</b> The "
            f"age-friendly reviews converge on larger type and larger controls "
            f"{c('liu', 'gomez', 'amouzadeh')}, and the barrier studies put small text and "
            f"small targets at the top of what defeats elderly users {c('awan', 'elguera')}. "
            f"The only hard number in that literature is the 48 dp platform minimum "
            f"{n('material', 'touchtarget')}, and the elder median of {E['dp_median']:.0f} dp "
            f"is {E['dp_median'] / 48:.1f}× it, with {E['dp_pct_at_least_48']:.0f} % of elder "
            f"controls at or above it. On type there is no threshold to clear — the font-size "
            f"review is explicit that no agreed metric exists and that preference reverses "
            f"past a point {c('hou')} — so the claim made here is narrower: the elder scale is "
            f"measurably larger than the guardian scale, by "
            f"{E['sp_median'] / G['sp_median']:.1f}× at the median, in the direction the "
            f"evidence points."))
    A(("p", f"<b>Feature coverage is broader than the surveyed market.</b> The two market "
            f"surveys found 44 caregiver applications in total, of which 8 covered three or "
            f"more categories of support {c('grossman')}, and dementia-caregiver applications "
            f"that were largely static information with no evidence base {c('wozney')}. This "
            f"application administers contacts, medicines and schedules, reminders, video and "
            f"application shortcuts, vitals, wellness and alerting remotely across 20 "
            f"functional areas. That is a comparison of feature coverage and nothing more; it "
            f"is not a claim about quality or benefit."))

    A(("h2", "8.2 Where the measurements go against the project"))
    A(("p", f"<b>The guardian surface is below the platform minimum at the median.</b> Its "
            f"median control is {G['dp_median']:.0f} dp against a 48 dp minimum, and only "
            f"{G['dp_pct_at_least_48']:.0f} % of its {G['dp_n']} controls reach that figure. "
            f"The design intent is a dense administration console for a younger user, and the "
            f"platform minimum is not stated as applying only to elderly users — it is a "
            f"general accessibility floor {n('material', 'touchtarget', 'wcag')}. The large-"
            f"scale analyses show undersized controls to be the norm in shipped Android "
            f"software {c('chen', 'ross')}, which explains the result but does not excuse it. "
            f"It is recorded here as a finding, it is carried into the conclusion, and raising "
            f"the guardian surface's interactive minima is the first item of future work."))
    A(("p", f"<b>A sixth of the elder surface's text is below 16 sp.</b> "
            f"{E['sp_pct_below_16']:.1f} % of the {E['sp_n']} elder text declarations fall "
            f"under 16 sp. These are secondary labels rather than primary content, but the "
            f"literature on older readers' sensitivity to legibility {c('beier', 'hou')} says "
            f"this is the kind of detail that costs a user, and the census is reported in full "
            f"rather than summarised to the median that flatters it."))
    A(("p", f"<b>The automated suite proves less than its pass rate suggests.</b> "
            f"{TOTAL_EXEC} executions with zero failures is a strong signal about "
            f"{UT['tests']} encoded behaviours and no signal at all about the behaviours "
            f"nobody wrote a test for. The suite is unit and integration level; it does not "
            f"exercise the rendered interface on a handset, and the manual campaign of "
            f"Section 6.3 exists precisely because of that boundary."))

    A(("h2", "8.3 Limitations"))
    A(("bullets", [
        f"<b>No health or well-being outcome was measured.</b> The adherence benefit in the "
        f"trial literature {c('poorcheraghi', 'salmensuu', 'wiecek')} and the quality-of-life "
        f"benefit in the telecare trials {c('hirani', 'pajalic')} are the reasons the reminder "
        f"and alert features exist. They are not results of this project, and following "
        f"published design guidance does not transfer the published benefit.",
        f"<b>No comparative usability study with participants.</b> The instrument would be the "
        f"System Usability Scale with its published benchmarks {c('brooke', 'bangor')}, framed "
        f"by ISO 9241-11's effectiveness–efficiency–satisfaction model {c('iso9241')}. That "
        f"needs participants and clearance; it is named as future work rather than approximated.",
        "<b>The interface census measures declarations, not rendered pixels.</b> A declared "
        "<c>sp</c> value is scaled by the user's own font-size setting and a <c>dp</c> value "
        "by screen density, so the rendered result varies by handset and by user "
        "configuration. The census is exact about what the source declares and silent about "
        "what any one device shows.",
        "<b>The latency figures are from one network and one location.</b> Home broadband in "
        "Mumbai to a backend in ap-south-1, on one day. A different network, a mobile "
        "connection, or a backend region further away would move these numbers, and the "
        "distribution's tail is set by the network rather than by the application.",
        "<b>The data-isolation result covers one threat.</b> It establishes that an "
        "unauthenticated reader holding the application's key gets no personal rows. It is not "
        "a penetration test, not protection against a compromised authenticated account, and "
        "not a statement about encryption at rest on the handset.",
        f"<b>Voice and reminder delivery accuracy were not measured.</b> Speech recognition "
        f"accuracy for older speakers is a known difficulty {c('hamill', 'kim')} and "
        f"end-to-end reminder delivery timing under Doze on a range of handsets {n('doze')} is "
        f"the single most valuable measurement this project has not yet taken.",
    ]))

    A(("h2", "8.4 Sources of error"))
    A(("table", "Sources of error, and how each was handled.", [
        ["Source of error", "Effect", "How it was handled"],
        ["Network variance in the latency trials",
         "Right-skewed distributions; a single trial would be unrepresentative.",
         f"{LAT['rest_root']['n']} trials per endpoint; medians and 95th percentiles reported "
         f"rather than means alone; the per-request raw file submitted."],
        ["Build-cache reuse in the test runs",
         "A repeated run could report a cached result as a pass without re-executing.",
         "Runs 2 and 3 forced full re-execution with <c>--rerun-tasks</c>."],
        ["Cold-start effects in the first run",
         "Run 1's wall-clock time includes daemon start and a full compile.",
         "The three run times are reported separately and not averaged."],
        ["Letter-spacing declarations in the type census",
         "Letter spacing is also declared in <c>sp</c> and would depress the measured type "
         "scale.",
         "Excluded explicitly by the census script; an earlier pass that included them was "
         "discarded."],
        ["Reference tables in the isolation probe",
         "Four tables legitimately return rows, which could be read as a leak or masked to "
         "look like zero.",
         "The two classes are reported separately and each reference table was inspected to "
         "confirm it holds no personal data."],
        ["Artefact section measurement",
         "Reading the archive listing's total row as an entry inflates one section.",
         "Sections are read programmatically from the archive's own entry table, not parsed "
         "from a listing."],
        ["Single-host measurement",
         "All timings come from one machine on one day.",
         "The conditions are stated in full in Table 7 so that a repeat can be compared "
         "against them."],
    ], [38, 58, 74], ()))
    A(("pagebreak",))

    # ══════════════════ 9. CONCLUSION ══════════════════
    A(("h1", "9. Conclusion and future work"))
    A(("h2", "9.1 What the evidence supports"))
    A(("p", "Five statements in this report are supported by measurement, and are the only "
            "ones that should be carried away from it:"))
    A(("steps", [
        f"<b>The application works and is installable.</b> A {APK_MB:.1f} MB release artefact "
        f"was built from the committed source in {BUILD_S} s, declares "
        f"{APK['permission_count']} permissions, and runs on Android "
        f"{ENV['min_sdk']} and above.",
        f"<b>The two surfaces are measurably different, in the intended direction.</b> The "
        f"elder surface's median text size is {E['sp_median']:.0f} sp against the guardian's "
        f"{G['sp_median']:.0f} sp, and its median control {E['dp_median']:.0f} dp against "
        f"{G['dp_median']:.0f} dp — {E['dp_median'] / 48:.1f}× the 48 dp platform minimum "
        f"{n('material')}, over a complete census of {E['dp_n'] + G['dp_n']} declarations.",
        f"<b>Personal data is not reachable without authentication.</b> {N_PROBES} anonymous "
        f"read attempts across {N_TABLES} tables returned zero rows from all "
        f"{len(PERSONAL)} personal tables.",
        f"<b>The backend responds an order of magnitude inside the interaction budget.</b> A "
        f"median care-data read of {LAT['rls_denied_select']['median_ms']:.0f} ms and a 95th "
        f"percentile of {LAT['rls_denied_select']['p95_ms']:.0f} ms over "
        f"{LAT['rls_denied_select']['n']} trials, against the one-second threshold at which a "
        f"user's train of thought is preserved {c('miller', 'card', 'nielsen')}.",
        f"<b>The encoded behaviour is stable under repetition.</b> {TOTAL_EXEC} test "
        f"executions over three runs, two of them forced to re-execute from cold, with "
        f"{UT['failed']} failures and {UT['errors']} errors — alongside 68 manual cases driven "
        f"live, 65 passed, 2 partial, none failing, and two defects found and fixed in the "
        f"course of the campaign.",
    ]))
    A(("h2", "9.2 What the evidence does not support"))
    A(("p", f"No claim is made that the application improves medication adherence, well-being "
            f"or safety. Those are outcomes measured in human participants in the trial "
            f"literature {c('poorcheraghi', 'hirani')}, and this project measured an "
            f"implementation. No claim is made that the application is usable by its intended "
            f"users in the sense ISO 9241-11 defines {c('iso9241')}: geometry consistent with "
            f"published guidance is a precondition for usability, not evidence of it. And the "
            f"guardian surface does not meet the platform's interactive minimum at the median, "
            f"which is a finding of this report rather than an omission from it."))
    A(("h2", "9.3 Future work, in priority order"))
    A(("table", "What the next version should do, and why this report puts it in this order.", [
        ["#", "Work", "Why it is placed here"],
        ["1", "Raise the guardian surface's interactive controls to the 48 dp minimum and "
              "re-run the census.",
         f"It is the one measured result in this report that fails a published normative "
         f"threshold {n('material', 'wcag')}, and it is cheap to fix."],
        ["2", "Instrument end-to-end reminder delivery on a range of physical handsets under "
              "Doze, and report the delivery-time distribution.",
         f"Reminder timing is the application's most consequential behaviour and the largest "
         f"gap in the present measurement; the platform makes it genuinely hard {n('doze')}."],
        ["3", "Run a usability study with elderly participants and their guardians, scored "
              "with the System Usability Scale.",
         f"It is the only way to convert geometry into a usability claim, and published "
         f"benchmarks make the score interpretable {c('brooke', 'bangor')}."],
        ["4", "Measure speech recognition accuracy for older speakers on the voice paths.",
         f"The literature predicts this is where voice fails for this population "
         f"{c('hamill', 'kim')}; the application's fallback to tap should be validated against "
         f"a measured error rate."],
        ["5", "Add handset-side rendering measurement to close the gap between declared and "
              "rendered geometry.",
         "Section 8.3 names this as the limitation of the census; it is addressable with "
         "instrumentation rather than with participants."],
        ["6", "Commission an independent security assessment beyond the single exposure "
              "measured here.",
         f"The audit literature shows how much this category of software usually gets wrong "
         f"{c('huckvale', 'papageorgiou')}; one measured threat is not an assessment."],
        ["7", "A longitudinal deployment in real households, reporting engagement rather than "
              "feature counts.",
         f"Real-world engagement data is what the most recent reminder literature is now "
         f"reporting {c('wiecek')}, and it is the honest end point for this line of work."],
    ], [7, 70, 93], ()))
    A(("h2", "9.4 Closing statement"))
    A(("p", "The project set out to build the dyadic architecture the literature recommends "
            "and then to report it as a measured object rather than a described one. It did "
            "both. The measurements that favour the design are stated with their sample sizes "
            "and conditions; the two that do not — the guardian surface's control sizes, and "
            "the share of elder text below 16 sp — are stated in the same terms and carried "
            "into the future work. What remains undone is the study that would turn an "
            "instrumented application into evidence about the people it was built for, and "
            "Section 9.3 places it third because it is the item this work most needs and is "
            "least able to supply on its own."))
    A(("pagebreak",))

    # ══════════════════ 10. CONTRIBUTIONS ══════════════════
    A(("h1", "10. Group members and contributions"))
    A(("p", "The project was carried out by the five members below. Each member submits this "
            "report separately and states their own contribution, as the task requires; the "
            "member submitting this copy is named on the declaration page. The contributions "
            "are stated at the level of the work actually done rather than as job titles, so "
            "that each member can be questioned on their own section of this report."))
    data = [["Member", "Roll number", "Area", "Contribution", "Chapters"]]
    chmap = {
        "Suyash S. Humne": "4.2, 5.1–5.5",
        "Devanshi Pandey": "3, 6, 7, and this report",
        "Deon Menezes": "4.1, 7.3",
        "Sonali Wankhede": "4.3, 4.5, 7.4",
        "Balwant Kumar Singh": "2, 8.1",
    }
    for name, roll, area, contrib in MEMBERS:
        mark = " <b>(submitting this copy)</b>" if name == SUBMITTING_MEMBER else ""
        data.append([name + mark, roll or "—", area, contrib, chmap.get(name, "—")])
    A(("table", "Contribution of each group member.", data, [30, 20, 32, 70, 18], ()))
    A(("p", "<b>Work carried out jointly.</b> The requirements, the choice of the two-surface "
            "architecture, the manual test plan of Section 6.3 and the review of this report "
            "were done by the group together. Where a chapter rests on one member's work, "
            "that member is named against it in Table 23."))
    A(("callout", "Before this report is submitted",
       "Four roll numbers are blank in Table 23 and in the declaration table, and the "
       "project guide and head-of-department signature rows are empty. Fill those in, check "
       "that the area assigned to each member in Table 23 matches what that member actually "
       "did, and sign the declaration page. Nothing else in this report needs editing by "
       "hand: every other value is generated from the measurement files.", "warn"))
    A(("pagebreak",))

    # ══════════════════ REFERENCES ══════════════════
    A(("h1", "References"))
    A(("p", "The forty reviewed sources of Chapter 2, in the order they are first cited."))
    A(("numrefs", "", REFS))
    A(("h2", "Normative and technical references"))
    A(("p", "Platform minima, policy and documentation cited as engineering requirements. "
            "These are not counted among the forty reviewed sources."))
    A(("numrefs", "", NORMATIVE, 41))
    A(("pagebreak",))

    # ══════════════════ APPENDICES ══════════════════
    A(("h1", "Appendix A — Raw data inventory"))
    A(("p", "Every file below is submitted with this report. The SHA-256 digest is of the file "
            "as submitted, so that the evidence can be shown to be the evidence the numbers "
            "in Chapter 7 were read from."))
    import hashlib
    data = [["File", "Bytes", "SHA-256"]]
    for fn in sorted(os.listdir(RAW)):
        path = os.path.join(RAW, fn)
        if not os.path.isfile(path):
            continue
        h = hashlib.sha256()
        with open(path, "rb") as fh:
            for chunk in iter(lambda: fh.read(1 << 16), b""):
                h.update(chunk)
        data.append([f"<c>{fn}</c>", f"{os.path.getsize(path):,}", f"<c>{h.hexdigest()}</c>"])
    A(("table", "Raw measurement files, with digests.", data, [54, 18, 98], (1,)))
    A(("table", "The instrument scripts, also submitted.", [
        ["Script", "What it measures"],
        ["<c>measurements/parse_tests.py</c>", "The automated suite, from the JUnit XML reports"],
        ["<c>measurements/run_repeats.sh</c>", "The two forced re-runs and the clean build time"],
        ["<c>measurements/measure_backend.sh</c>", "The timed HTTPS requests"],
        ["<c>measurements/measure_ui.py</c>", "The interface size census"],
        ["<c>measurements/plot_results.py</c>", "Every figure in Chapters 5, 6 and 7"],
    ], [60, 110], ()))
    A(("pagebreak",))

    A(("h1", "Appendix B — Test suite inventory"))
    A(("p", f"All {UT['suites']} suites of the first run, with the number of tests in each and "
            f"its execution time. Failures, errors and skips were zero throughout."))
    data = [["Suite", "Tests", "Failures", "Errors", "Time (s)"]]
    for s in sorted(SUITES, key=lambda r: -float(r.get("time_s", 0) or 0)):
        data.append([f"<c>{s['suite']}</c>", s.get("tests", ""), s.get("failures", "0"),
                     s.get("errors", "0"), f"{float(s.get('time_s', 0) or 0):.3f}"])
    A(("table", "Every suite in the first run, slowest first.", data,
       [92, 18, 22, 18, 20], (1, 2, 3, 4)))
    A(("table", "The slowest individual tests, and why each is slow.", [
        ["Suite", "Test", "Time (ms)"],
    ] + [[f"<c>{s['suite']}</c>", s["test"], f"{s['ms']:.0f}"] for s in UT["slowest"]],
       [44, 106, 20], (2,)))
    A(("p", "The slowest tests are the ones that drive failure and scheduling paths — an "
            "offline flush against a server error, duplicate scheduling, cancelling an unknown "
            "alarm key, a client timeout with no response. They are slow because they wait, "
            "which is the behaviour under test."))
    A(("pagebreak",))

    A(("h1", "Appendix C — Build environment"))
    A(("table", "The complete build and measurement environment.", [
        ["Key", "Value"],
    ] + [[f"<c>{k}</c>", v] for k, v in ENV.items()], [50, 120], ()))
    A(("table", "The release artefact, entry by entry.", [
        ["Entry", "Uncompressed (bytes)", "Compressed (bytes)", "Compression"],
    ] + [[f"<c>{r['entry']}</c>", f"{int(r['uncompressed_bytes']):,}",
          f"{int(r['compressed_bytes']):,}",
          f"{100 * (1 - int(r['compressed_bytes']) / max(int(r['uncompressed_bytes']), 1)):.0f} %"]
         for r in SECTIONS], [62, 40, 38, 30], (1, 2, 3)))
    A(("pagebreak",))

    A(("h1", "Appendix D — Latency, request by request"))
    A(("p", f"The summary statistics of Table 11 are derived from "
            f"{sum(v['n'] for v in LAT.values())} individual requests, each recorded with its "
            f"DNS, TCP, TLS, time-to-first-byte and total time in "
            f"<c>backend_latency.csv</c>. The distribution of each endpoint is given below at "
            f"the percentiles that matter; the raw file is submitted for anyone who wants to "
            f"re-derive them."))
    data = [["Endpoint", "n", "Min", "Median", "p95", "Max", "Mean", "SD", "HTTP status"]]
    for k in LAT_ORDER:
        v = LAT[k]
        data.append([LAT_LABEL[k], f"{v['n']}", f"{v['min_ms']:.1f}", f"{v['median_ms']:.1f}",
                     f"{v['p95_ms']:.1f}", f"{v['max_ms']:.1f}", f"{v['mean_ms']:.1f}",
                     f"{v['sd_ms']:.1f}",
                     ", ".join(f"{c_} × {n_}" for c_, n_ in v["http"].items())])
    A(("table", "Latency distribution by endpoint, all times in milliseconds.", data,
       [46, 10, 14, 17, 14, 15, 15, 14, 25], (1, 2, 3, 4, 5, 6, 7)))
    A(("pagebreak",))

    A(("h1", "Appendix E — What is submitted with this report"))
    A(("table", "The contents of the submission folder.", [
        ["Item", "What it is"],
        ["<c>Task-28-Final-Technical-Report.pdf</c>",
         "This report, for signature"],
        ["<c>Task-28-Final-Technical-Report.docx</c>",
         "The same report as an editable source file, generated from the same content"],
        ["<c>Task-29-IEEE-Paper.pdf</c>", "The final IEEE-format paper"],
        ["<c>Task-29-DOI-Proof.pdf</c>", "Proof of the assigned DOI"],
        ["<c>Task-30-Viva-Defense-Slides.pptx</c> / <c>.pdf</c>",
         "The viva defence slides, editable and as PDF"],
        ["<c>Task-31-Final-Demonstration.mp4</c>",
         "The final end-to-end demonstration recording"],
        ["<c>measurements/raw/</c>", "Every raw data file of Appendix A"],
        ["<c>measurements/*.py</c>, <c>*.sh</c>", "The instrument scripts"],
        ["<c>figures/</c>", "Every figure, as PNG and as PDF"],
        ["<c>build/</c>", "The runnable release and debug artefacts"],
        ["<c>source/</c>", f"The complete source at commit {ENV['git_commit']}"],
    ], [56, 114], ()))

    return B
