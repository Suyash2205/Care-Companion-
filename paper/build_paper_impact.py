"""
build_paper_impact.py — the paper in the IMPACT conference template.

That template is single-column A4 Word, not IEEE two-column PDF, so this is a
separate builder rather than a tweak to build_paper.py. Every rule below comes from
Paper-Template-IMPACT.docx and the contributor guidelines:

  single column, A4, 2.54 cm margins, nothing in header or footer
  title      Times New Roman 16 pt bold, centered
  abstract   Times New Roman 10 pt justified, 200 words maximum
  headings   Times New Roman 11 pt bold, left aligned, numbered 1, 2, 3
  body       Times New Roman 11 pt justified
  references Times New Roman 10 pt justified, IEEE numeric style
  spacing    single throughout
  US spelling; no bold, italic or underline used for emphasis

    ./.venv/bin/python build_paper_impact.py   ->  CareCompanion-IMPACT.docx
"""
import os
import re

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Cm, Inches, Pt

HERE = os.path.dirname(os.path.abspath(__file__))
FIGS = os.path.join(HERE, "figs")
OUT = os.path.join(HERE, "CareCompanion-IMPACT.docx")

FONT = "Times New Roman"
RUNNING_HEAD = "CareCompanion: assistive app for elders"   # 41 chars, limit is 45


# ---------------------------------------------------------------- helpers
def style_doc(doc):
    s = doc.sections[0]
    s.page_width, s.page_height = Inches(8.27), Inches(11.69)      # A4
    s.top_margin = s.bottom_margin = Cm(2.54)
    s.left_margin = s.right_margin = Cm(2.54)
    # The template says to put nothing in either, and that the publisher paginates.
    s.header_distance = s.footer_distance = Cm(1.25)
    normal = doc.styles["Normal"]
    normal.font.name = FONT
    normal.font.size = Pt(11)
    pf = normal.paragraph_format
    pf.line_spacing = 1
    pf.space_before = Pt(0)
    pf.space_after = Pt(6)


def para(doc, text, size=11, bold=False, align="justify", space_after=6, space_before=0):
    p = doc.add_paragraph()
    p.alignment = {
        "justify": WD_ALIGN_PARAGRAPH.JUSTIFY,
        "center": WD_ALIGN_PARAGRAPH.CENTER,
        "left": WD_ALIGN_PARAGRAPH.LEFT,
    }[align]
    pf = p.paragraph_format
    pf.line_spacing = 1
    pf.space_after = Pt(space_after)
    pf.space_before = Pt(space_before)
    run = p.add_run(text)
    run.font.name = FONT
    run.font.size = Pt(size)
    run.bold = bold
    return p


def heading(doc, text):
    return para(doc, text, size=11, bold=True, align="left",
                space_before=10, space_after=4)


def figure(doc, name, number, caption):
    path = os.path.join(FIGS, name + ".png")
    doc.add_picture(path, width=Cm(14))
    doc.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER
    para(doc, f"Figure {number}: {caption}", size=10, align="center", space_after=10)


def table(doc, number, caption, rows, widths_cm):
    para(doc, f"Table {number}: {caption}", size=10, align="center", space_after=4)
    t = doc.add_table(rows=len(rows), cols=len(rows[0]))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for r, row in enumerate(rows):
        for c, val in enumerate(row):
            cell = t.cell(r, c)
            cell.width = Cm(widths_cm[c])
            cell.text = ""
            p = cell.paragraphs[0]
            p.paragraph_format.line_spacing = 1
            p.paragraph_format.space_after = Pt(2)
            run = p.add_run(str(val))
            run.font.name = FONT
            run.font.size = Pt(10)
            run.bold = (r == 0)
    para(doc, "", size=6, space_after=4)
    return t


# ---------------------------------------------------------------- content
ABSTRACT = (
    "Mobile applications that help older adults manage medication and summon help are "
    "usually judged on how legible the older adult's screen is. A second property "
    "matters as much and is rarely examined: whether the system tells the truth when it "
    "fails. We present CareCompanion, an implemented Android system for "
    "caregiver-mediated elder care built on three commitments. An asymmetric dual-surface "
    "architecture places all configuration on the caregiver's device, leaving the older "
    "adult a zero-configuration surface; measured from source, that surface has a median "
    "declared text size of 20 sp against 13 sp on the caregiver surface, and every "
    "primary control is at least 64 dp against a 48 dp platform minimum. Out-of-band "
    "device binding closes an identity gap created by federated sign-in, using a "
    "single-use short-lived code in place of phone-number matching. Fail-honest alerting "
    "requires that the interface never assert an outcome it has not observed, so an "
    "undelivered emergency alert says so and offers a user-mediated fallback. A "
    "structured audit found 13 defects, of which 8, or 62 percent, would have failed "
    "with no signal to the user, including two features stored correctly and delivered "
    "to nobody. We report the taxonomy and what remains unevaluated."
)

KEYWORDS = ("elder care, mobile health, medication adherence, caregiver-mediated systems, "
            "accessibility, dependability, Android, human-centered design")

INTRO = [
    "India is aging quickly, and the support infrastructure is not keeping pace. The "
    "United Nations Population Fund projects that the population aged 60 and over will "
    "grow by 134 percent between 2022 and 2050, and the population aged 80 and over by "
    "279 percent [1]. Digital access, however, remains uneven across age groups. Mobile "
    "phone use is widespread nationally, but internet use declines sharply with age [2], "
    "and among older adults in rural India, particularly women, shared handsets and "
    "limited digital literacy are the norm rather than the exception [3], [4].",

    "Medication non-adherence is a significant concern among older adults managing "
    "chronic conditions and polypharmacy. Randomized evidence suggests that mobile "
    "reminder tools help without solving the problem: a trial of a drug-management "
    "application among older adults with polypharmacy reported improved adherence and "
    "reduced hospital readmission [5], while a systematic review of reminder technologies "
    "for home-dwelling older citizens reports beneficial effects for some outcomes but "
    "emphasizes that the overall evidence remains heterogeneous [6].",

    "The usability literature is clear about what makes an interface hard for an older "
    "adult. Small text is repeatedly the most-cited barrier; touch targets are too small "
    "for reduced dexterity; icons are misread; and navigation depth exceeds working "
    "memory [7], [8], [9]. Design guideline syntheses converge on enlarged type, enlarged "
    "targets, shallow navigation and error tolerance [10], [11].",

    "What this literature largely assumes is that the older adult is the person "
    "configuring the system. In practice, care is dyadic. Quinn et al. found in a dyad "
    "usability study that most interaction occurred between caregiver and older adult "
    "rather than the older adult alone [12], and dyadic digital health modules are now "
    "designed explicitly around shared care [13]. If the caregiver is doing the "
    "configuring, the older adult's surface does not need to be a simplified version of a "
    "management application. It can be a different application altogether.",

    "A second assumption is more consequential and, we argue, under-examined: that when "
    "the system reports success, it has succeeded. Emergency and reminder paths on mobile "
    "platforms are unreliable in ways the application can observe but frequently does not "
    "check. The Android Doze and App Standby mechanisms defer alarms and network access "
    "during idle [14], and manufacturer power management is more aggressive still. Push "
    "delivery depends on connectivity that an older adult at home may not have. An "
    "interface that shows a green confirmation on an emergency alert it never delivered "
    "is not merely unhelpful; in the one situation the system exists for, it is harmful.",

    "This paper makes four contributions. The first is an asymmetric dual-surface "
    "architecture for caregiver-mediated elder care, in which configuration authority and "
    "interface complexity are deliberately unequal across two clients over one care "
    "record, with the asymmetry measured from the implementation rather than asserted. "
    "The second is out-of-band device binding, a single-use short-lived code that binds a "
    "device to a care profile; we show that migrating to federated identity silently "
    "breaks phone-number-based linking, and that an out-of-band code is a simple and "
    "auditable repair. The third is fail-honest alerting, a discipline in which the "
    "interface never asserts an unobserved outcome, together with a permission-minimal "
    "degradation path that keeps the application distributable under current application "
    "store policy. The fourth is an empirical defect taxonomy from a structured audit of "
    "the feature-complete system, showing that silent failures dominate.",

    "We are explicit about scope. This is a systems and design-contribution paper. We "
    "report conformance, interaction cost and defect data measured from the artifact. No "
    "user study has yet been conducted, and we make no usability or clinical claims.",
]

RELATED = [
    ("Age-friendly mobile interface design", [
        "Systematic reviews of mobile design for older adults consistently identify text "
        "size, target size, icon interpretability and navigation complexity as the "
        "dominant barriers. Awan et al. applied an analytical hierarchy process to "
        "prioritize usability barriers and ranked small font and screen size highest [7]. "
        "Elguera Paez and Zapata Del Rio catalog challenges across the literature [8]. "
        "Gomez-Hernandez et al. derive design guidelines through thematic synthesis [10], "
        "and Amouzadeh et al. provide a recent consolidation of age-friendly design "
        "recommendations [11]. Liu et al. review interface and persuasive feature design "
        "in mobile health applications for older adults [9]. Our elder surface is an "
        "implementation of this consensus; the contribution here is not the guidelines "
        "but the demonstration that offloading configuration to a second surface is what "
        "makes conformance affordable.",
    ]),
    ("Medication reminder and adherence systems", [
        "Poorcheraghi et al. report a randomized controlled trial of a mobile drug "
        "management application among older adults with polypharmacy, finding improved "
        "adherence and reduced hospital readmission [5]. Salmensuu et al. systematically "
        "review reminder technologies for home-dwelling older citizens [6]. Wiecek et al. "
        "report real-world engagement and adherence evidence for older adults [15]. These "
        "studies evaluate outcomes; they do not generally report whether reminders were "
        "delivered by the operating system as scheduled, which Section 3.4 treats as a "
        "first-class engineering problem.",
    ]),
    ("Dyadic and caregiver-mediated health systems", [
        "Quinn et al. conducted a dyad usability study of a mobile application used by "
        "older adults together with informal caregivers, observing that interaction was "
        "predominantly caregiver-mediated [12]. Benmessaoud et al. designed a dyadic "
        "digital health module for chronic disease shared care, identifying "
        "disease-invariant features for patient and caregiver dyads [13]. Nimmanterdwong "
        "et al. review human-centered design practice in mobile health for older adults "
        "[16]. This work establishes the dyad as the unit of design. We extend it "
        "architecturally: rather than one application used by two people, we build two "
        "surfaces of deliberately unequal complexity over one access-controlled record.",
    ]),
    ("Personal emergency response systems", [
        "Personal emergency response systems have a long history and a well-documented "
        "failure mode: Hamill et al. report call-center estimates attributing as much as "
        "85 percent of calls to false alarms, largely accidental activations [17]. This "
        "motivates the cancellable grace period described in Section 3.3. The "
        "complementary failure, an alarm the user believes was raised but which never "
        "reached anyone, is less discussed in that literature, and is what fail-honest "
        "alerting addresses.",
        "Across these strands, interface legibility for the older adult and outcome "
        "efficacy are well covered; delivery honesty and deployability under platform "
        "policy are not. Few studies in the reviewed elder-care literature explicitly "
        "report a taxonomy of silent implementation failures, or treat application store "
        "permission policy as an architectural design constraint on the emergency path.",
    ]),
]


METHOD = [
    ("3.1 Asymmetric dual-surface architecture", [
        "The system presents two Android surfaces backed by one record. The caregiver "
        "surface carries the full management vocabulary across seventeen navigation "
        "destinations: profiles, medicines, schedules, reminders, contacts, vital signs, "
        "adherence history, media shortcuts, shared access and an emergency monitor. The "
        "elder surface carries seven: a home screen, an alarm control, today's medicines, "
        "contacts, vital-sign entry, media shortcuts and a settings screen containing "
        "three controls, plus a one-time binding screen seen only at setup.",

        "The asymmetry is the design. Because no configuration occurs on the elder "
        "surface, that surface has no forms to validate, no destructive actions to guard, "
        "no navigation deeper than two levels and no state the older adult can corrupt. "
        "This is what makes generous type and target sizing affordable: a screen that "
        "must accommodate a scheduling form cannot also give every control 96 dp of "
        "height. Section 4.1 quantifies the resulting difference. Contacts on the elder "
        "surface are photograph-first, so recognition does not depend on reading a name.",

        "Both surfaces authenticate with a federated identity provider and address the "
        "same record through a per-row access boundary. Thirty-three row-level policies "
        "across seventeen tables derive the caller's identity from the verified token "
        "rather than from any client-supplied parameter, so a modified client cannot "
        "widen its own visibility. All privileged transitions, such as creating a "
        "profile, issuing a code, redeeming one or changing another member's access, are "
        "server-side routines rather than client writes.",
    ]),
    ("3.2 Out-of-band device binding", [
        "Elder-care systems must answer a question ordinary applications do not: which "
        "physical device belongs to the person being cared for? The caregiver creates the "
        "profile, so the profile cannot identify its subject by who created it.",

        "Our first implementation matched on phone number. The caregiver entered the "
        "older adult's number; when a device signed in with that number, the two were "
        "linked. This works under telephone-number-based authentication and fails "
        "completely under federated sign-in, which returns an email address and a "
        "provider subject identifier and no telephone number at all. After migrating to "
        "federated sign-in, the stored telephone number for every account was the empty "
        "string, and the matching predicate could never be satisfied. The failure was "
        "silent: links were recorded in a pending state, and the resolution routine "
        "compared an empty string against a real number indefinitely.",

        "The repair is an out-of-band binding primitive. The caregiver requests a "
        "six-digit code bound to the profile; the code is single-use and expires after "
        "seven days. On the older adult's device, after federated sign-in, entering the "
        "code claims the profile for that account. The channel by which the code travels "
        "is deliberately unspecified, because the caregiver is usually present during "
        "setup [12]. Three properties matter. The code is single-use, so a shared code "
        "cannot be replayed. It is short-lived, bounding the window in which a guessed "
        "code is useful. Responses are indistinguishable between an unknown code and an "
        "expired one, so the endpoint cannot be used to enumerate which codes exist. A "
        "profile already bound to one account cannot be silently rebound to another. The "
        "same primitive, parameterized by an access level, later replaced the equally "
        "broken caregiver-invitation path, which depended on the same assumption.",
    ]),
    ("3.3 Fail-honest alerting", [
        "We adopt a single rule: the interface never asserts an outcome it has not "
        "observed. Applied to the emergency path, this produces the state machine in "
        "Figure 2.",

        "Pressing the alarm control begins a five-second cancellable grace period. This "
        "is a direct response to the dominant failure mode of personal emergency response "
        "systems, where Hamill et al. report estimates of up to 85 percent of call-center "
        "traffic arising from accidental activation [17], and it costs a genuine "
        "emergency five seconds.",

        "If the period expires, the client records the alarm on the server, which is what "
        "reaches caregivers, using bounded retry with an increasing delay between "
        "attempts. The terminal state is then selected by whether that record was "
        "acknowledged, not by whether it was attempted. On acknowledgment the interface "
        "confirms delivery. On failure it states plainly that the alarm was not delivered "
        "and presents two large controls: one opens the device's own messaging "
        "application with the emergency text and a location link already composed, and "
        "one dials the primary contact.",

        "An earlier revision of this screen displayed a success confirmation "
        "unconditionally. It was possible for an older adult to be shown a green "
        "confirmation and the words your family has been notified when the emergency "
        "event had not in fact reached the caregiver-facing system. We regard this class "
        "of defect as the most serious a care system can contain, because it converts a "
        "recoverable failure into an unrecoverable one: a user who believes help is "
        "coming stops seeking it.",
    ]),
    ("3.4 Redundant reminder arming", [
        "Medication reminders must fire on a device the application may not be running "
        "on, under an operating system actively suppressing background work. Android Doze "
        "and App Standby defer alarms, jobs and network access during idle, and "
        "manufacturer power management layers are typically more aggressive [14].",

        "An early revision armed the day's alarms only when the older adult opened the "
        "application. The consequence is easy to state and was easy to miss: an older "
        "adult who did not open the application received no reminders that day, precisely "
        "the user for whom the reminders exist. The system now arms through three "
        "independent paths: on application open; from a periodic background worker that "
        "re-arms the current day every four hours and skips doses already answered; and "
        "from a boot-completed receiver that re-arms from a persisted set, since alarms "
        "do not survive a restart. Exact alarms are requested where the platform grants "
        "them for medication reminders, with a windowed fallback when the permission is "
        "unavailable.",

        "Two correctness properties proved necessary in implementation. Alarm keys must "
        "be namespaced by source, because the hash of the key is used as both the pending "
        "intent request code and the notification identifier; an un-namespaced key "
        "allowed a general reminder and a medication dose to silently replace one "
        "another. Answering a dose must cancel its pending alarm, or an older adult who "
        "takes a tablet early is told to take it again at the scheduled time, a prompt "
        "toward double-dosing generated by an adherence tool.",
    ]),
    ("3.5 Permission-minimal degradation", [
        "Emergency features in the research literature routinely assume the application "
        "may send a short message directly. Current application store policy "
        "substantially restricts this: messaging and call-log permissions are limited to "
        "applications whose primary function is messaging or telephony, and a care "
        "application declaring them is likely to face review or rejection [18], [19].",

        "We therefore removed direct message sending and direct dialing entirely. The "
        "emergency path reaches caregivers over the network; where a message is "
        "warranted, the application hands a pre-composed message to the device's own "
        "messaging application through a standard intent, and the user presses send. This "
        "costs one additional interaction in the degraded path and removes two restricted "
        "permissions. We report it because deployability is a research-relevant property: "
        "a prototype that cannot be distributed cannot be evaluated at scale, and this "
        "constraint is rarely stated in the elder-care literature.",
    ]),
    ("3.6 Implementation", [
        "Both clients are native Android applications written in Kotlin using Jetpack "
        "Compose, targeting application programming interface level 36 with a minimum of "
        "level 24. Persistence and access control are provided by Supabase-hosted "
        "PostgreSQL with row-level security, reached over its generated representational "
        "state transfer interface; privileged operations are database routines executing "
        "with definer rights. Push delivery uses Firebase Cloud Messaging with data-only "
        "messages, because notification-payload messages bypass the application's own "
        "handler when the application is backgrounded and would prevent the client from "
        "applying its own delivery rules, including the rule that emergency alerts are "
        "exempt from the application's general notification preference.",

        "The elder surface is localized into English, Hindi, Marathi and Gujarati, offers "
        "three type scales, and provides a high-contrast mode that composes with the "
        "operating system's own accessibility scaling rather than overriding it. Language "
        "matters here beyond convenience: interfaces presented only in English are a "
        "documented barrier for older adults in India, particularly older women with "
        "lower English literacy [3], [4].",

        "Table 1 reports the size of the implementation, measured from the repository. "
        "The test suite comprises 147 automated tests executing on the Java virtual "
        "machine with a shadowed Android runtime, requiring no device or emulator. "
        "Coverage is concentrated where silent failure is most likely: alarm scheduling "
        "and cancellation, offline durability of adherence records, delivery-contract "
        "behavior of the push path, translation-table integrity across the four "
        "languages, and a regression test for each defect in Section 4.3. Static analysis "
        "reports no errors at release configuration.",
    ]),
]

SIZE_TABLE = [
    ["Component", "Files", "Lines"],
    ["Caregiver surface", "27", "3,942"],
    ["Elder surface", "6", "1,271"],
    ["Data access and models", "12", "1,082"],
    ["Reminder and alarm engine", "4", "329"],
    ["Notification and push", "2", "147"],
    ["Authentication", "1", "79"],
    ["Application total (Kotlin)", "83", "13,473"],
    ["Automated tests (Kotlin)", "26", "3,010"],
    ["Database migrations (SQL)", "15", "1,121"],
]


RESULTS_INTRO = (
    "We evaluate three properties measurable from the artifact: conformance of the elder "
    "surface to age-friendly sizing guidance, interaction cost for core tasks, and the "
    "defect profile of the feature-complete system. Every quantitative figure in this "
    "section is generated by a script that parses the application source directly, so the "
    "reported measurements cannot drift from the implementation."
)

CONFORMANCE = [
    "Figure 3 shows every declared text size on each surface. The elder surface has a "
    "median of 20 sp against 13 sp for the caregiver surface, and 16.4 percent of its "
    "declarations fall below 16 sp against 79.7 percent on the caregiver surface. The "
    "residual small sizes on the elder surface are supporting captions rather than "
    "primary content. Because the elder surface additionally composes a user-selected "
    "scale of 1.0, 1.15 or 1.3 with the operating system's own font scaling, the "
    "effective floor rises further when either is increased.",

    "Figure 4 reports the height of the primary control on each elder screen. The "
    "smallest is 64 dp, one third above the 48 dp platform minimum [20], and comfortably "
    "above the widely cited 44 by 44 target-size recommendation associated with the Web "
    "Content Accessibility Guidelines [21]. Note that those guidelines are specified in "
    "cascading style sheet pixels rather than Android density-independent pixels, so the "
    "comparison is indicative rather than exact. The median is 76 dp, and the emergency "
    "control is 130 dp, roughly 2.7 times the platform minimum, reflecting its priority. "
    "We report this as conformance evidence, not as a usability result: meeting a sizing "
    "guideline is necessary, not sufficient, and only a study with older adults can "
    "establish sufficiency.",
]

INTERACTION = [
    "Figure 5 counts interactions to completion for the elder surface's core tasks. "
    "Raising an alarm costs one tap. Confirming a dose from its reminder costs three, "
    "following a change that routes the notification directly to the medication screen "
    "rather than to the home screen; previously it cost four. The saving is one tap, "
    "which sounds trivial and is not: each intermediate screen is an opportunity for an "
    "older adult to lose the thread, and an unconfirmed dose is recorded as missed and "
    "shown to the caregiver as a possible non-adherence event. Interaction cost on this "
    "surface is therefore coupled to data quality, not only to convenience.",
]

DEFECTS = [
    "After the system was feature-complete and had been exercised on physical devices, we "
    "conducted a structured audit: a line-by-line review of all 83 source files together "
    "with targeted inspection of the database schema and access policies. We recorded "
    "every defect found and classified each by whether a user could have detected it from "
    "the interface alone. Every defect reported here was subsequently corrected, and each "
    "correction is traceable to a commit in the project's version-control history "
    "together with the regression test added for it.",

    "Thirteen distinct defects were found, distributed as shown in Figure 6. The dominant "
    "finding is that 8 of 13, or 62 percent, were silent: the operation failed, or never "
    "occurred, while the interface reported success or reported nothing. Three classes "
    "deserve comment.",

    "Unreachable features accounted for three. Two complete features, general reminders "
    "for hydration and activity and the caregiver invitation path, were written, "
    "exercised by the caregiver, stored correctly in the database and delivered to "
    "nobody. General reminders were never armed as alarms on the older adult's device and "
    "no server job scanned for them. The invitation path depended on the telephone-number "
    "assumption broken by federated sign-in, described in Section 3.2. In both cases the "
    "caregiver's interface behaved as though the feature worked. Both were subsequently "
    "repaired.",

    "Lost state accounted for three. Three view models rebuilt their state object when a "
    "background refresh completed, discarding a transient completion flag that the screen "
    "used to navigate. A save that had already succeeded on the server therefore appeared "
    "to do nothing. The defect is timing-dependent, which is why it survived manual "
    "testing: it reproduces only when the refresh completes after the save, most likely "
    "on a slow connection.",

    "Null failure messages accounted for one class spanning seventeen call sites. Every "
    "repository write terminates in taking the first element of the server's response. "
    "When access policy hides the returned row, that list is empty and the resulting "
    "exception carries no message. Seventeen call sites propagated that null message "
    "directly to the interface, rendering an empty error and presenting as an "
    "unresponsive button.",

    "Two observations generalize beyond this system. First, identity-model migration "
    "produces latent dead features. Moving from telephone-number authentication to "
    "federated sign-in silently invalidated every code path that used a telephone number "
    "as a join key. Nothing failed loudly; two features simply stopped being reachable. "
    "Any system migrating to federated identity should audit its non-authentication uses "
    "of the prior identifier. Second, test suites do not detect unreachable features. The "
    "suite passed throughout the period in which two features delivered nothing, because "
    "each tested component behaved correctly in isolation. The defect lay in the absence "
    "of a caller. Detection required tracing each data type from producer to consumer, a "
    "check we recommend as routine practice and for which we found no substitute.",
]

DISCUSSION = [
    "The audit's central result is that most defects in a feature-complete care "
    "application were invisible from the interface. This has a specific consequence for "
    "elder care that it does not have for, say, a photo application. The older adult "
    "often cannot diagnose a silent failure, and the caregiver, the person who could, is "
    "looking at a different device that is also reporting success. A missing reminder "
    "produces no artifact at all; its absence is indistinguishable from a day with no "
    "medication due. The system's own adherence record, which the caregiver trusts, will "
    "attribute the gap to the older adult. We therefore propose that care systems adopt "
    "an explicit honesty discipline: acknowledge only observed outcomes; give every "
    "failure path a message a lay reader can act on; and, for any feature whose value is "
    "delivery, test the delivery rather than the storage.",

    "The measured gap between the two surfaces is not the result of applying guidelines "
    "harder on one screen. It is a consequence of removing work from that screen. Type "
    "size and target size compete with information density; a surface that must present a "
    "scheduling form cannot also give every control 96 dp. By moving all configuration to "
    "the caregiver, the elder surface gains an accessibility budget it can spend on "
    "sizing and shallow navigation. We suggest this reframing, asymmetry as a budget "
    "rather than as simplification, is the transferable idea.",

    "Application store permission policy determined the architecture of our emergency "
    "path. This is unusual to state in a research paper and, we think, ought not to be. A "
    "design that cannot be distributed cannot accumulate the longitudinal evidence the "
    "field needs. Treating policy as a constraint at design time, rather than discovering "
    "it at submission time, is a practical contribution of this work.",

    "On scalability, the architecture is designed to support independent care circles: "
    "circles are disjoint, queries are scoped to a profile, and no routine operation "
    "requires a cross-tenant scan, so growth in the number of circles should be "
    "horizontal. Reminder scheduling is performed on the device, so the server is not a "
    "timing authority and carries no per-dose timer. Adherence writes are small, "
    "append-only and queue locally when offline. Large-scale performance has not been "
    "benchmarked; this is architectural analysis rather than measurement.",

    "Several limitations bound these results. No user study has been conducted: no older "
    "adult or caregiver outside the development team has used the system under "
    "observation, so we make no usability, adherence or clinical claim, and every "
    "conformance number above is a static property of the implementation. Reminder "
    "delivery has been verified on physical devices anecdotally and in a shadowed runtime "
    "exhaustively, but we have no distribution of delivery latency across manufacturers "
    "and power-management regimes. The defect taxonomy comes from one application audited "
    "by its own developers, with a single observer and no inter-rater agreement; it is a "
    "case study, not a population estimate. The four language tables are complete and "
    "machine-checked for structural integrity but have not been reviewed by native "
    "speakers for register or clarity for older readers. Finally, the system is not a "
    "medical device and provides no diagnostic or dosing advice; vital-sign banding is "
    "presented as general reference information only.",
]

CONCLUSIONS = [
    "We presented CareCompanion, a caregiver-mediated elder-care system built on three "
    "commitments. An asymmetric dual-surface architecture buys the older adult an "
    "accessibility budget by removing configuration from their device entirely, so that "
    "generous type and large targets become affordable rather than aspirational. "
    "Out-of-band device binding repairs an identity gap that federated sign-in "
    "introduces, using a single-use short-lived code in place of the telephone number "
    "that federated identity does not supply. Fail-honest alerting requires that the "
    "interface never assert an outcome it has not observed, so that an undelivered "
    "emergency alert says so and degrades to a user-mediated fallback.",

    "Our measurements show the asymmetry is real. Parsed directly from the source, the "
    "elder surface has a median declared text size of 20 sp against 13 sp on the "
    "caregiver surface, and every primary control is at least 64 dp against a 48 dp "
    "platform minimum, with the emergency control at 130 dp. Interaction cost for the "
    "most frequent task, confirming a dose from its reminder, is three taps.",

    "The audit result is the one we consider most transferable. Of thirteen defects found "
    "in a feature-complete system, eight would have failed with no user-visible signal, "
    "including two features that were stored faithfully and delivered to nobody, and a "
    "passing test suite did not detect either. For software whose purpose is to be relied "
    "upon in an emergency, we argue that honesty about failure is not a secondary quality "
    "attribute but a primary design requirement, and that it should be evaluated "
    "explicitly rather than assumed.",

    "These results suggest that elder-care systems should be assessed not only for "
    "accessibility and functionality, but also for whether users receive truthful "
    "feedback when critical operations fail. Future work falls into three parts. The "
    "immediate priority is a dyad field deployment with older adults and their "
    "caregivers, measuring the delivery-level outcomes the literature usually assumes: "
    "the proportion of scheduled reminders actually presented by the device, the time "
    "from alarm activation to caregiver acknowledgment, and the frequency of the "
    "degraded emergency path. These are precisely the quantities the fail-honest "
    "instrumentation can record truthfully, which is the point of building it that way. "
    "Second, a spoken interaction layer in the four supported languages has since been "
    "implemented, allowing an older adult to confirm a dose by speaking rather than "
    "tapping; initial verification against synthesized speech is encouraging, but it "
    "remains untested with speakers of the target age. Third, medication stock tracking "
    "would let the system warn a caregiver before a medicine runs out, which is a failure "
    "mode as consequential as forgetting a dose and one the present system does not "
    "address.",
]

REFERENCES = [
    "United Nations Population Fund, India Ageing Report 2023: Caring for Our Elders, "
    "Institutional Responses. New Delhi, India: UNFPA India, 2023.",
    "Data for India, “Access to phones and the internet,” 2025. [Online]. "
    "Available: https://www.dataforindia.com/comm-tech/. Accessed: Oct. 1, 2026.",
    "D. Chakraborty and C. Garg, “Navigating technology: Mobile media usage and "
    "reticence among older adults in rural India,” Mobile Media and Communication, "
    "vol. 14, pp. 31–49, 2025, doi: 10.1177/20501579251378548.",
    "A. Khare, R. Mukherjee, and M. Bedarkar, “Beyond digital literacy: A structural "
    "model analysis of technology acceptance among elderly persons,” Humanities and "
    "Social Sciences Communications, vol. 13, art. 794, 2026, doi: "
    "10.1057/s41599-026-07146-7.",
    "H. Poorcheraghi, R. Negarandeh, S. Pashaeypoor, and J. Jorian, “Effect of using "
    "a mobile drug management application on medication adherence and hospital "
    "readmission among elderly patients with polypharmacy: A randomized controlled "
    "trial,” BMC Health Services Research, vol. 23, art. 1192, 2023, doi: "
    "10.1186/s12913-023-10177-4.",
    "O. Salmensuu, J. Isotalo, M. Rijken, V. Hyttinen-Huotari, M. Kaarakainen, and I. "
    "Linnosmaa, “Effects of using medication reminder technologies by home-dwelling "
    "older citizens: A systematic review,” Age and Ageing, vol. 55, no. 2, art. "
    "afag007, 2026, doi: 10.1093/ageing/afag007.",
    "M. Awan, S. Ali, M. Ali, M. F. Abrar, H. Ullah, and D. Khan, “Usability barriers "
    "for elderly users in smartphone app usage: An analytical hierarchical process-based "
    "prioritization,” Scientific Programming, vol. 2021, art. 2780257, 2021, doi: "
    "10.1155/2021/2780257.",
    "L. Elguera Paez and C. Zapata Del Rio, “Elderly users and their main challenges "
    "usability with mobile applications: A systematic review,” in Design, User "
    "Experience, and Usability, Lecture Notes in Computer Science. Cham, Switzerland: "
    "Springer, 2019, pp. 423–438, doi: 10.1007/978-3-030-23570-3_31.",
    "N. Liu, J. Yin, S. S.-L. Tan, K. Y. Ngiam, and H. H. Teo, “Mobile health "
    "applications for older adults: A systematic review of interface and persuasive "
    "feature design,” Journal of the American Medical Informatics Association, vol. "
    "28, pp. 2483–2501, 2021, doi: 10.1093/jamia/ocab151.",
    "M. Gomez-Hernandez, X. Ferre, C. Moral, and E. Villalba-Mora, “Design guidelines "
    "of mobile apps for older adults: Systematic review and thematic analysis,” JMIR "
    "mHealth and uHealth, vol. 11, art. e43186, 2023, doi: 10.2196/43186.",
    "E. Amouzadeh, I. Dianat, J. Faradmal, and M. Babamiri, “Optimizing mobile app "
    "design for older adults: Systematic review of age-friendly design,” Aging "
    "Clinical and Experimental Research, vol. 37, art. 248, 2025, doi: "
    "10.1007/s40520-025-03157-7.",
    "C. C. Quinn, S. Staub, E. Barr, and A. Gruber-Baldini, “Mobile support for older "
    "adults and their caregivers: Dyad usability study,” JMIR Aging, vol. 2, no. 1, "
    "art. e12276, 2019, doi: 10.2196/12276.",
    "C. Benmessaoud, K. J. Pfisterer, A. De Leon, A. Saragadam, N. El-Dassouki, K. G. M. "
    "Young, R. Lohani, and T. Xiong, “Design of a dyadic digital health module for "
    "chronic disease shared care: Development study,” JMIR Human Factors, vol. 10, "
    "art. e45035, 2023, doi: 10.2196/45035.",
    "Android Developers, “Optimize for Doze and App Standby,” Android "
    "Documentation. [Online]. Available: "
    "https://developer.android.com/training/monitoring-device-state/doze-standby. "
    "Accessed: Oct. 1, 2026.",
    "E. Wiecek, S. Taylor, H. Rourke, F. Hammond, and N. Amador-Fernandez, “Mobile "
    "health apps for older adults: Real-world evidence on engagement and medication "
    "adherence,” Frontiers in Digital Health, vol. 8, art. 1716880, 2026, doi: "
    "10.3389/fdgth.2026.1716880.",
    "Z. Nimmanterdwong, S. Boonviriya, and P. Tangkijvanich, “Human-centered design "
    "of mobile health apps for older adults: Systematic review and narrative "
    "synthesis,” JMIR mHealth and uHealth, vol. 10, no. 1, art. e29512, 2022, doi: "
    "10.2196/29512.",
    "M. Hamill, V. Young, J. Boger, and A. Mihailidis, “Development of an automated "
    "speech recognition interface for personal emergency response systems,” Journal "
    "of NeuroEngineering and Rehabilitation, vol. 6, art. 26, 2009, doi: "
    "10.1186/1743-0003-6-26.",
    "Google Play Console Help, “Use of SMS or Call Log permission groups,” "
    "Google Play. [Online]. Available: "
    "https://support.google.com/googleplay/android-developer/answer/10208820. Accessed: "
    "Oct. 1, 2026.",
    "Android Developers Blog, “Reminder: SMS/Call Log policy changes,” Jan. "
    "2019. [Online]. Available: "
    "https://android-developers.googleblog.com/2019/01/reminder-smscall-log-policy-changes.html. "
    "Accessed: Oct. 1, 2026.",
    "Material Design 3, “Accessibility and touch target size,” Google. "
    "[Online]. Available: https://m3.material.io/foundations/accessible-design/patterns. "
    "Accessed: Oct. 1, 2026.",
    "Web Content Accessibility Guidelines (WCAG) 2.2, W3C Recommendation, World Wide Web "
    "Consortium, 2023. [Online]. Available: https://www.w3.org/TR/WCAG22/. Accessed: Oct. "
    "1, 2026.",
]


# ---------------------------------------------------------------- assembly
def build():
    doc = Document()
    style_doc(doc)

    para(doc, "CareCompanion: Assistive Companion Mobile Application for Elderly and "
              "Disabled Users", size=16, bold=True, align="center", space_after=10)

    para(doc, "Devanshi Pandey¹, Suyash Humne²*, Deon Menezes², "
              "Sonali Wankhede³, Balwant Kumar Singh¹",
         size=11, align="center", space_after=4)
    para(doc, "¹ Department of Electronics and Computer Engineering, K. J. Somaiya "
              "School of Engineering, Somaiya Vidyavihar University, Mumbai, India",
         size=10, align="center", space_after=2)
    para(doc, "² Department of Artificial Intelligence and Data Science, K. J. "
              "Somaiya School of Engineering, Somaiya Vidyavihar University, Mumbai, India",
         size=10, align="center", space_after=2)
    para(doc, "³ Department of Information Technology, K. J. Somaiya School of "
              "Engineering, Somaiya Vidyavihar University, Mumbai, India",
         size=10, align="center", space_after=2)
    para(doc, "devanshi.pandey@somaiya.edu, suyash.humne@somaiya.edu, "
              "deon.m@somaiya.edu, sonali.w@somaiya.edu, balwantkumar@somaiya.edu",
         size=10, align="center", space_after=2)
    para(doc, "*Corresponding author: Suyash Humne (suyash.humne@somaiya.edu)",
         size=10, align="center", space_after=2)
    para(doc, f"Running head: {RUNNING_HEAD}", size=10, align="center", space_after=10)

    heading(doc, "ABSTRACT")
    para(doc, ABSTRACT, size=10, space_after=8)
    para(doc, f"Keywords: {KEYWORDS}", size=10, space_after=12)

    heading(doc, "1. Introduction")
    for p in INTRO:
        para(doc, p)

    heading(doc, "2. Related Work")
    for n, (sub, paras) in enumerate(RELATED, start=1):
        heading(doc, f"2.{n} {sub}")
        for p in paras:
            para(doc, p)

    heading(doc, "3. Research Methodology")
    para(doc, "This section describes the system as built, in enough detail to let the "
              "design be reproduced. Figure 1 shows the overall structure.")
    figure(doc, "fig_arch", 1,
           "System structure. Two clients of deliberately unequal complexity share one "
           "care record behind a per-row access boundary that derives identity from the "
           "caller's token. The out-of-band binding step establishes which physical "
           "device belongs to which older adult.")
    for sub, paras in METHOD:
        heading(doc, sub)
        for p in paras:
            para(doc, p)
        if sub.startswith("3.3"):
            figure(doc, "fig_failhonest", 2,
                   "The emergency path. A cancellable grace period addresses accidental "
                   "activation, which dominates personal emergency response system "
                   "traffic. The terminal states are distinguished by observed delivery, "
                   "not by having attempted it.")
        if sub.startswith("3.6"):
            table(doc, 1, "Implementation size, measured from the repository.",
                  SIZE_TABLE, [7.0, 3.0, 3.5])

    heading(doc, "4. Results and Discussion")
    para(doc, RESULTS_INTRO)

    heading(doc, "4.1 Conformance of the elder surface")
    figure(doc, "fig_typescale", 3,
           "Distribution of declared text sizes across the two surfaces, parsed from "
           "source. The caregiver surface concentrates at 12 to 13 sp; the elder surface "
           "is shifted upward with a long tail to 72 sp for the alarm control.")
    para(doc, CONFORMANCE[0])
    figure(doc, "fig_targets", 4,
           "Height of the primary control on each elder screen, parsed from source. "
           "Every primary control exceeds the 48 dp platform minimum; the median is 76 "
           "dp and the emergency control is 130 dp.")
    para(doc, CONFORMANCE[1])

    heading(doc, "4.2 Task interaction cost")
    figure(doc, "fig_taps", 5,
           "Interactions to completion for core tasks on the elder surface, counted from "
           "the notification or from the home screen.")
    for p in INTERACTION:
        para(doc, p)

    heading(doc, "4.3 Defect study")
    para(doc, DEFECTS[0])
    figure(doc, "fig_defects", 6,
           "Defects found in the audit window, by class. Eight of thirteen would have "
           "failed with no user-visible signal.")
    for p in DEFECTS[1:]:
        para(doc, p)

    heading(doc, "4.4 Discussion")
    for p in DISCUSSION:
        para(doc, p)

    heading(doc, "5. Conclusions")
    for p in CONCLUSIONS:
        para(doc, p)

    heading(doc, "Acknowledgments")
    para(doc, "The authors thank the Department of Artificial Intelligence and Data "
              "Science and the Department of Electronics and Computer Engineering at K. "
              "J. Somaiya School of Engineering for their guidance and support "
              "throughout this project.")

    heading(doc, "Funding source")
    para(doc, "This research did not receive any specific grant from funding agencies in "
              "the public, commercial, or not-for-profit sectors.")

    heading(doc, "Conflict of Interest")
    para(doc, "The authors declare that they have no known competing financial interests "
              "or personal relationships that could have appeared to influence the work "
              "reported in this paper.")

    heading(doc, "References")
    for i, r in enumerate(REFERENCES, start=1):
        p = para(doc, f"[{i}] {r}", size=10, space_after=4)
        p.paragraph_format.left_indent = Cm(0.8)
        p.paragraph_format.first_line_indent = Cm(-0.8)

    doc.save(OUT)
    return doc


def check(name, text, lo, hi):
    n = len(re.findall(r"\b[\w'-]+\b", text))
    flag = "OK " if lo <= n <= hi else "!! "
    print(f"  {flag}{name}: {n} words (limit {lo}-{hi})")


if __name__ == "__main__":
    build()
    print("Wrote:", OUT)
    check("abstract", ABSTRACT, 0, 200)
    check("conclusions", " ".join(CONCLUSIONS), 250, 500)
    print(f"  {'OK ' if len(RUNNING_HEAD) <= 45 else '!! '}running head: "
          f"{len(RUNNING_HEAD)} chars (limit 45)")
