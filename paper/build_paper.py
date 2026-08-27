"""
build_paper.py — typesets the CareCompanion paper in IEEE conference format.

IEEE two-column geometry on US Letter: 0.625 in side margins, 3.5 in columns with a
0.25 in gutter, Times throughout. No LaTeX required.

    ./.venv/bin/python build_paper.py   ->  CareCompanion-IEEE-Paper.pdf
"""
import os
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import (BaseDocTemplate, Frame, FrameBreak, Image, KeepTogether,
                                NextPageTemplate, PageBreak, PageTemplate, Paragraph, Spacer,
                                Table, TableStyle)

HERE = os.path.dirname(os.path.abspath(__file__))
FIGS = os.path.join(HERE, "figs")
OUT = os.path.join(HERE, "CareCompanion-IEEE-Paper.pdf")

PW, PH = LETTER
ML = MR = 0.625 * inch
MT, MB = 0.75 * inch, 1.0 * inch
COLW = 3.5 * inch
GUT = 0.25 * inch
TITLE_H = 1.72 * inch

# ---------------- styles ----------------
def st(name, **kw):
    kw.setdefault("fontName", "Times-Roman")
    # A 3.5 in measure justifies very loosely without hyphenation; reportlab uses pyphen
    # when a hyphenation language is set on the style.
    kw.setdefault("hyphenationLang", "en_GB")
    kw.setdefault("embeddedHyphenation", 1)
    return ParagraphStyle(name, **kw)

TITLE   = st("title", fontName="Times-Bold", fontSize=20, leading=23, alignment=TA_CENTER, spaceAfter=9)
AUTHOR  = st("author", fontSize=10.5, leading=13, alignment=TA_CENTER)
AFFIL   = st("affil", fontName="Times-Italic", fontSize=9, leading=11, alignment=TA_CENTER)
ROLLNO  = st("rollno", fontSize=9, leading=11.5, alignment=TA_CENTER)
ABSTRACT= st("abs", fontName="Times-Bold", fontSize=8.6, leading=10.4, alignment=TA_JUSTIFY, spaceAfter=5)
IDXTERMS= st("idx", fontName="Times-Bold", fontSize=8.6, leading=10.4, alignment=TA_JUSTIFY, spaceAfter=4)
H1      = st("h1", fontSize=9.6, leading=12, alignment=TA_CENTER, spaceBefore=9, spaceAfter=4)
H2      = st("h2", fontName="Times-Italic", fontSize=9.6, leading=11.6, spaceBefore=6, spaceAfter=3)
BODY    = st("body", fontSize=9.6, leading=11.5, alignment=TA_JUSTIFY, firstLineIndent=10, spaceAfter=1.5)
BODY0   = st("body0", fontSize=9.6, leading=11.5, alignment=TA_JUSTIFY, spaceAfter=1.5)
BULLET  = st("bul", fontSize=9.6, leading=11.5, alignment=TA_JUSTIFY, leftIndent=12, bulletIndent=2, spaceAfter=1.5)
CAP     = st("cap", fontSize=8, leading=9.6, alignment=TA_JUSTIFY, spaceBefore=3, spaceAfter=7)
TCAP    = st("tcap", fontSize=8, leading=9.6, alignment=TA_CENTER, spaceBefore=2, spaceAfter=3)
CELL    = st("cell", fontSize=7.8, leading=9.4)
CELLB   = st("cellb", fontName="Times-Bold", fontSize=7.8, leading=9.4)
REF     = st("ref", fontSize=8, leading=9.4, alignment=TA_JUSTIFY, leftIndent=11, firstLineIndent=-11, spaceAfter=1.4)

def P(t, s=BODY): return Paragraph(t, s)
def H(n, t):
    # Unnumbered headings (References, Acknowledgment) take no numeral or full stop.
    label = f"{n}.&nbsp;&nbsp;" if n else ""
    return Paragraph(f"{label}{t.upper()}", H1)
def SH(l, t): return Paragraph(f"<i>{l}. {t}</i>", H2)

def bullets(items):
    return [Paragraph(f"<bullet>&bull;</bullet>{t}", BULLET) for t in items]

def fig(name, num, caption, height=None):
    path = os.path.join(FIGS, name + ".png")
    from PIL import Image as PIL
    w, h = PIL.open(path).size
    disp_w = COLW - 6
    disp_h = disp_w * h / w
    return KeepTogether([Spacer(1, 4), Image(path, width=disp_w, height=disp_h),
                         Paragraph(f"Fig. {num}.&nbsp; {caption}", CAP)])

def table(num, title, rows, widths, align_right=()):
    data = [[Paragraph(c, CELLB if i == 0 else CELL) for c in row] for i, row in enumerate(rows)]
    t = Table(data, colWidths=widths, repeatRows=1)
    style = [("VALIGN", (0, 0), (-1, -1), "TOP"),
             ("LEFTPADDING", (0, 0), (-1, -1), 3), ("RIGHTPADDING", (0, 0), (-1, -1), 3),
             ("TOPPADDING", (0, 0), (-1, -1), 2.2), ("BOTTOMPADDING", (0, 0), (-1, -1), 2.2),
             ("LINEABOVE", (0, 0), (-1, 0), 0.9, colors.black),
             ("LINEBELOW", (0, 0), (-1, 0), 0.5, colors.black),
             ("LINEBELOW", (0, -1), (-1, -1), 0.9, colors.black)]
    for c in align_right:
        style.append(("ALIGN", (c, 0), (c, -1), "RIGHT"))
    t.setStyle(TableStyle(style))
    return KeepTogether([Spacer(1, 5),
                         Paragraph(f"TABLE {num}", TCAP),
                         Paragraph(f"<i>{title}</i>", TCAP), t, Spacer(1, 8)])

# ---------------- page furniture ----------------
def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Times-Roman", 9)
    canvas.drawCentredString(PW / 2, MB - 26, str(canvas.getPageNumber()))
    canvas.restoreState()

def build(story):
    doc = BaseDocTemplate(OUT, pagesize=LETTER, leftMargin=ML, rightMargin=MR,
                          topMargin=MT, bottomMargin=MB,
                          title="CareCompanion: A Dual-Surface Elder Care System",
                          author="CareCompanion")
    body_h = PH - MT - MB
    first_col_h = body_h - TITLE_H
    banner = Frame(ML, PH - MT - TITLE_H, COLW * 2 + GUT, TITLE_H, id="banner",
                   leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    f1l = Frame(ML, MB, COLW, first_col_h, id="f1l", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    f1r = Frame(ML + COLW + GUT, MB, COLW, first_col_h, id="f1r", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    fl = Frame(ML, MB, COLW, body_h, id="fl", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    fr = Frame(ML + COLW + GUT, MB, COLW, body_h, id="fr", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    doc.addPageTemplates([PageTemplate(id="first", frames=[banner, f1l, f1r], onPage=footer),
                          PageTemplate(id="rest", frames=[fl, fr], onPage=footer)])
    doc.build(story)
    print("Wrote:", OUT)


# ================================ CONTENT ================================
def story():
    F = []; A = F.append

    # ---------- title block ----------
    A(Paragraph("CareCompanion: A Dual-Surface Elder Care System with "
                "Fail-Honest Alerting and Secure Device Binding", TITLE))
    # Names and roll numbers taken from the group's own project deck (Group ID 6).
    # The two departments are marked separately because the group spans both.
    A(Paragraph("Devanshi Pandey<super>2</super>, Suyash Humne<super>1</super>, "
                "Deon Menezes<super>1</super>", AUTHOR))
    A(Paragraph("16014023016&nbsp;&nbsp;&nbsp;16014223085&nbsp;&nbsp;&nbsp;16014223030", ROLLNO))
    A(Paragraph("<super>1</super>Department of Artificial Intelligence and Data Science&nbsp;&nbsp;"
                "<super>2</super>Department of Electronics and Computer Engineering", AFFIL))
    A(Paragraph("K. J. Somaiya School of Engineering, Somaiya Vidyavihar University, "
                "Mumbai, India", AFFIL))
    # End the full-width banner here so the abstract begins in the left column, as IEEE
    # sets it, rather than running across both columns.
    A(FrameBreak())
    A(NextPageTemplate("rest"))

    # ---------- abstract ----------
    A(Paragraph(
        "<i>Abstract</i>&mdash;Mobile applications intended to help older adults manage "
        "medication and summon help are typically evaluated on the legibility of the older "
        "adult's interface. We argue that a second property matters at least as much and is "
        "rarely examined: whether the system tells the truth when it fails. We present "
        "CareCompanion, an implemented Android system for caregiver-mediated elder care built "
        "on three "
        "design commitments. First, an <i>asymmetric dual-surface</i> architecture places all "
        "configuration on the caregiver's device and leaves the older adult a zero-configuration "
        "surface of seven screens; measured from the source, the elder surface has a median "
        "declared text size of 20&nbsp;sp against 13&nbsp;sp on the caregiver surface, and every "
        "primary control equals or exceeds 64&nbsp;dp against a 48&nbsp;dp platform minimum. "
        "Second, <i>out-of-band device binding</i> resolves an identity gap created by federated "
        "sign-in: a single-use, short-lived numeric code issued to the caregiver binds the older "
        "adult's device to a care profile, replacing phone-number matching, which federated "
        "identity does not supply. Third, <i>fail-honest alerting</i> requires the interface never "
        "to assert an outcome it has not observed; an emergency alert that was not delivered says "
        "so and degrades to a one-tap, user-mediated fallback rather than displaying success. "
        "A structured audit of the feature-complete system found 13 defects, of which 8 (62%) "
        "would have failed with no user-visible signal, including two features that were written, "
        "stored, and never delivered. We report the taxonomy, the mechanisms that address each "
        "class, and an explicit account of what remains unevaluated: no user study has yet been "
        "conducted.", ABSTRACT))
    A(Paragraph("<i>Index Terms</i>&mdash;elder care, mobile health, medication adherence, "
                "caregiver-mediated systems, accessibility, dependability, Android, "
                "human-centred design.", IDXTERMS))

    # ---------------- I. INTRODUCTION ----------------
    A(H("I", "Introduction"))
    A(P("India is experiencing rapid population ageing, creating growing demand for accessible "
        "and scalable elder-care support. The United Nations Population Fund projects that "
        "India's population aged 60 and over will grow by 134% between 2022 and 2050, and the "
        "population aged 80 and over by 279% [1]. Digital access, however, remains uneven across "
        "age groups: although mobile-phone use is widespread nationally, internet use declines "
        "substantially with age [2], and among older adults in rural India — particularly women "
        "— shared handsets and limited digital literacy are the norm rather than the exception "
        "[3], [4].", BODY0))
    A(P("Medication non-adherence is a significant concern among older adults managing chronic "
        "conditions and polypharmacy. Randomised evidence suggests mobile "
        "reminder tools help but do not solve the problem: a trial of a drug-management "
        "application among older adults with polypharmacy reported improved adherence and "
        "reduced readmission [5], while a 2026 systematic review of reminder technologies for "
        "home-dwelling older citizens reports beneficial effects for some outcomes but "
        "emphasises that the overall evidence remains heterogeneous and that further "
        "high-quality research is required [6]."))
    A(P("The usability literature is clear about what makes an interface hard for an older adult. "
        "Small text is repeatedly the single most-cited barrier; touch targets are too small for "
        "reduced dexterity; icons are misread; and navigation depth exceeds working memory [7], "
        "[8], [9]. Design guideline syntheses converge on enlarged type, enlarged targets, "
        "shallow navigation, and error tolerance [10], [11]."))
    A(P("What this literature largely assumes, however, is that the older adult is the person "
        "configuring the system. In practice, care is dyadic. Quinn <i>et al.</i> found in a dyad "
        "usability study that most interaction occurred between caregiver and older adult rather "
        "than the older adult alone [12], and dyadic digital health modules are now designed "
        "explicitly around shared care [13]. If the caregiver is doing the configuring, then the "
        "older adult's surface does not need to be a simplified version of a management "
        "application. It can be a different application altogether."))
    A(P("A second assumption is more consequential and, we argue, under-examined: that when the "
        "system reports success, it has succeeded. Emergency and reminder paths on mobile "
        "platforms are unreliable in ways the application can observe but frequently does not "
        "check. Android's Doze and App Standby defer alarms and network access during idle [14], "
        "and original-equipment-manufacturer power management is more aggressive still. Push "
        "delivery depends on connectivity that an older adult at home may not have. An interface "
        "that renders a green tick on an emergency alert it never delivered is not merely "
        "unhelpful; in the one situation the system exists for, it is actively harmful."))
    A(P("<b>Contributions.</b> This paper makes four contributions:"))
    A(Spacer(1, 1))
    for b in bullets([
        "An <i>asymmetric dual-surface</i> architecture for caregiver-mediated elder care, in "
        "which configuration authority and interface complexity are deliberately unequal across "
        "two clients over one care record, with the asymmetry measured from the implementation "
        "rather than asserted (Section III-A, Section V-A).",
        "<i>Out-of-band device binding</i>, a single-use short-lived code that binds a device to "
        "a care profile. We show that migrating to federated identity silently breaks "
        "phone-number-based linking, and that an out-of-band code is a simple, auditable repair "
        "(Section III-B).",
        "<i>Fail-honest alerting</i>: a discipline in which the interface never asserts an "
        "unobserved outcome, together with a permission-minimal degradation path that keeps the "
        "application distributable under current application-store policy (Section III-C, III-E).",
        "An empirical <i>defect taxonomy</i> from a structured audit of the feature-complete "
        "system, showing that silent failures — including two entire features that were stored "
        "but never delivered — dominate (Section V-C).",
    ]): A(b)
    A(Spacer(1, 2))
    A(P("We are explicit about scope. This is a systems and design-contribution paper. We report "
        "conformance, interaction cost, and defect data measured from the artefact. We have not "
        "yet run a user study, and we make no usability or clinical claims (Section VIII)."))

    # ---------------- II. RELATED WORK ----------------
    A(H("II", "Related Work"))
    A(SH("A", "Age-friendly mobile interface design"))
    A(P("Systematic reviews of mobile design for older adults consistently identify text size, "
        "target size, icon interpretability, and navigation complexity as the dominant barriers. "
        "Awan <i>et al.</i> applied an analytical hierarchy process to prioritise usability "
        "barriers and ranked small font and screen size highest [7]. Elguera Paez and Zapata Del "
        "Río catalogue challenges across the literature [8]. Gomez-Hernandez <i>et al.</i> derive "
        "design guidelines through thematic synthesis [10], and Amouzadeh <i>et al.</i> provide a "
        "recent consolidation of age-friendly design recommendations [11]. Liu <i>et al.</i> "
        "review interface and persuasive feature design in mobile health applications for older "
        "adults [9]. Our elder surface is an implementation of this consensus; our contribution "
        "is not the guidelines but the demonstration that offloading configuration to a second "
        "surface is what makes conformance affordable.", BODY0))
    A(SH("B", "Medication reminder and adherence systems"))
    A(P("Poorcheraghi <i>et al.</i> report a randomised controlled trial of a mobile drug "
        "management application among older adults with polypharmacy, finding improved adherence "
        "and reduced hospital readmission [5]. Salmensuu <i>et al.</i> systematically review "
        "reminder technologies for home-dwelling older citizens [6]. Wiecek <i>et al.</i> report "
        "real-world engagement and adherence evidence for older adults [15]. These studies "
        "evaluate outcomes; they do not generally report whether reminders were delivered by the "
        "operating system as scheduled, which our Section III-D treats as a first-class "
        "engineering problem.", BODY0))
    A(SH("C", "Dyadic and caregiver-mediated health systems"))
    A(P("Quinn <i>et al.</i> conducted a dyad usability study of a mobile application used by "
        "older adults together with informal caregivers, observing that interaction was "
        "predominantly caregiver-mediated [12]. Benmessaoud <i>et al.</i> designed a dyadic "
        "digital health module for chronic disease shared care, identifying disease-invariant "
        "features for patient–caregiver dyads [13]. Nimmanterdwong <i>et al.</i> review "
        "human-centred design practice in mobile health for older adults [16]. This work "
        "establishes the dyad as the unit of design. We extend it architecturally: rather than "
        "one application used by two people, we build two surfaces of deliberately unequal "
        "complexity over one access-controlled record.", BODY0))
    A(SH("D", "Personal emergency response systems"))
    A(P("Personal emergency response systems (PERS) have a long history and a well-documented "
        "failure mode: Hamill <i>et al.</i> report call-centre estimates attributing as much as "
        "85% of calls to false alarms, largely accidental activations [17]. This motivates our "
        "cancellable grace period. The complementary failure — an alarm the user believes was "
        "raised but which never reached anyone — is less discussed in that literature, and is "
        "what fail-honest alerting addresses.", BODY0))
    A(SH("E", "Gap"))
    A(P("Across these strands, interface legibility for the older adult and outcome efficacy are "
        "well covered; delivery honesty and deployability under platform policy are not. Few "
        "studies in the reviewed elder-care literature explicitly report a taxonomy of silent "
        "implementation failures, or treat application-store permission policy as an "
        "architectural design constraint on the emergency path.", BODY0))
    # ---------------- III. SYSTEM DESIGN ----------------
    A(H("III", "System Design"))
    A(SH("A", "Asymmetric dual-surface architecture"))
    A(P("The system presents two Android surfaces backed by one record. The caregiver surface "
        "carries the full management vocabulary: profiles, medicines, schedules, reminders, "
        "contacts, vitals, adherence history, shortcuts, shared access, and an emergency "
        "monitor across seventeen navigation destinations. The elder surface carries seven: an "
        "alarm control, today's medicines, contacts, vitals entry, media shortcuts, a home "
        "screen, and a settings screen containing three controls, plus a one-time binding "
        "screen seen only at set-up. Contacts on the elder surface are <i>photo-first</i>: "
        "family members are presented as photographs rather than text labels, so recognition "
        "does not depend on reading a name.", BODY0))
    A(fig("fig_arch", 1,
          "System structure. Two clients of deliberately unequal complexity share one care "
          "record behind a per-row access boundary that derives identity from the caller's "
          "token. The out-of-band binding step (bottom) establishes which physical device "
          "belongs to which older adult, a link federated sign-in does not provide."))
    A(P("The asymmetry is the design. Because no configuration occurs on the elder surface, that "
        "surface has no forms to validate, no destructive actions to guard, no navigation deeper "
        "than two levels, and no state the older adult can corrupt. This is what makes generous "
        "type and target sizing affordable: a screen that must accommodate a scheduling form "
        "cannot also give every control 96&nbsp;dp of height. Section V-A quantifies the "
        "resulting difference between the two surfaces."))
    A(P("Both surfaces authenticate with a federated identity provider and address the same "
        "record through a per-row access boundary. Thirty-three row-level policies across "
        "seventeen tables derive the caller's identity from the verified token rather than from "
        "any client-supplied parameter, so a modified client cannot widen its own visibility. "
        "All privileged transitions — creating a profile, issuing a code, redeeming one, "
        "changing another member's access — are server-side routines rather than client writes."))

    A(SH("B", "Out-of-band device binding"))
    A(P("Elder-care systems must answer a question ordinary applications do not: <i>which "
        "physical device belongs to the person being cared for?</i> The caregiver creates the "
        "profile, so the profile cannot identify its subject by who created it.", BODY0))
    A(P("Our first implementation matched on phone number. The caregiver entered the older "
        "adult's number; when a device signed in with that number, the two were linked. This "
        "works under telephone-number-based authentication and fails completely under federated "
        "sign-in, which returns an email address and a provider subject identifier and no "
        "telephone number at all. After migrating to federated sign-in, the stored telephone "
        "number for every account was the empty string, and the matching predicate could never "
        "be satisfied. The failure was silent: links were recorded in a pending state, and the "
        "resolution routine compared an empty string against a real number indefinitely. "
        "Section V-C returns to this as an instance of a general hazard."))
    A(P("The repair is an out-of-band binding primitive. The caregiver requests a six-digit code "
        "bound to the profile; the code is single-use and expires after seven days. On the older "
        "adult's device, after federated sign-in, entering the code claims the profile for that "
        "account. The channel by which the code travels is deliberately unspecified — spoken "
        "aloud in the same room, sent over any messaging application, or typed by the caregiver "
        "on the older adult's device — because the caregiver is present in the overwhelming "
        "majority of set-ups [12]."))
    A(P("Three properties matter. The code is <i>single-use</i>, so a shared code cannot be "
        "replayed. It is <i>short-lived</i>, bounding the window in which a guessed code is "
        "useful. And responses are <i>indistinguishable</i> between an unknown code and an "
        "expired one, so the endpoint cannot be used to enumerate which codes exist. A profile "
        "already bound to one account cannot be silently rebound to another. The same primitive, "
        "parameterised by an access level, later replaced the equally broken caregiver-invite "
        "path, which had depended on the same telephone-number assumption."))

    A(SH("C", "Fail-honest alerting"))
    A(P("We adopt a single rule: <i>the interface never asserts an outcome it has not "
        "observed.</i> Applied to the emergency path, this produces the machine in Fig. 2.", BODY0))
    A(fig("fig_failhonest", 2,
          "The emergency path. A cancellable grace period addresses accidental activation, which "
          "dominates personal emergency response system traffic [17]. The terminal states are "
          "distinguished by observed delivery, not by having attempted it: an undelivered alarm "
          "says so and offers a user-mediated fallback."))
    A(P("Pressing the alarm control begins a five-second cancellable grace period. This is a "
        "direct response to the dominant PERS failure mode — Hamill <i>et al.</i> report "
        "estimates of up to 85% of call-centre traffic arising from accidental activation [17] "
        "— and it costs a genuine emergency five seconds."))
    A(P("If the period expires, the client attempts to record the alarm on the server, which is "
        "what reaches caregivers, using bounded retry with an increasing delay between "
        "attempts. The terminal state "
        "is then selected by whether that record was <i>acknowledged</i>, not by whether it was "
        "attempted. On acknowledgement, the interface confirms delivery. On failure it states "
        "plainly that the alarm was not delivered and presents two large controls: one opens the "
        "device's own messaging application with the emergency text and a location link "
        "pre-composed, and one dials the primary contact."))
    A(P("An earlier revision of this screen displayed a success confirmation unconditionally. It "
        "was possible for an older adult to be shown a green tick and the words <i>your family "
        "has been notified</i> when the emergency event had not in fact reached the "
        "caregiver-facing system. "
        "We regard this class of defect as the most serious a care system can contain, because "
        "it converts a recoverable failure into an unrecoverable one: a user who believes help "
        "is coming stops seeking it."))

    A(SH("D", "Redundant reminder arming"))
    A(P("Medication reminders must fire on a device the application may not be running on, under "
        "an operating system actively suppressing background work. Android's Doze and App "
        "Standby defer alarms, jobs, and network access during idle, and manufacturer power "
        "management layers are typically more aggressive [14].", BODY0))
    A(P("An early revision armed the day's alarms only when the older adult opened the "
        "application. The consequence is easy to state and was easy to miss: an older adult who "
        "did not open the application received no reminders that day — precisely the user for "
        "whom the reminders exist. We now arm through three independent paths: on application "
        "open; from a periodic background worker that re-arms the current day every four hours "
        "and skips doses already answered; and from a boot-completed receiver that re-arms from "
        "a persisted set, since alarms do not survive a restart. Exact alarms are requested "
        "where the platform grants them for medication reminders, with a windowed fallback when "
        "the permission is unavailable."))
    A(P("Two correctness properties proved necessary in implementation. Alarm keys must be "
        "namespaced by source, because the key's hash is used as both the pending-intent request "
        "code and the notification identifier; an un-namespaced key allowed a general reminder "
        "and a medication dose to silently replace one another. And answering a dose must cancel "
        "its pending alarm, or an older adult who takes a tablet early is told to take it again "
        "at the scheduled time — a prompt toward double-dosing generated by an adherence tool."))

    A(SH("E", "Permission-minimal degradation"))
    A(P("Emergency features in the research literature routinely assume the application may send "
        "a short message directly. Current Google Play policy substantially restricts this: "
        "messaging and call-log permissions are limited to applications whose primary function "
        "is messaging or telephony, and a care application declaring them is likely to face "
        "review or rejection [18], [19].", BODY0))
    A(P("We therefore removed direct message-sending and direct dialling entirely. The emergency "
        "path reaches caregivers over the network; where a message is warranted, the application "
        "hands a pre-composed message to the device's own messaging application through a "
        "standard intent, and the user presses send. This costs one tap in the degraded path and "
        "removes two restricted permissions, reducing the declared set to eleven, none of which "
        "is restricted. We report this because deployability is a research-relevant property: a "
        "prototype that cannot be distributed cannot be evaluated at scale, and this constraint "
        "is rarely stated in the elder-care literature."))

    A(SH("F", "Localisation and perceptual settings"))
    A(P("The elder surface is localised into English, Hindi, Marathi, and Gujarati, and offers "
        "three type scales and a high-contrast mode that composes with the operating system's "
        "own accessibility scaling rather than overriding it. Language matters here beyond "
        "convenience: interfaces presented only in English are a documented barrier for older "
        "adults in India, particularly older women with lower English literacy [3], [4].", BODY0))
    # ---------------- IV. IMPLEMENTATION ----------------
    A(H("IV", "Implementation"))
    A(P("Both clients are native Android applications written in Kotlin using Jetpack Compose, "
        "targeting API level 36 with a minimum of API 24. Persistence and access control are "
        "provided by Supabase-hosted PostgreSQL with row-level security, reached over its "
        "generated REST interface; privileged operations are database routines executing with "
        "definer rights. Push delivery uses Firebase Cloud Messaging with data-only messages, "
        "since notification-payload messages bypass the application's own handler when the "
        "application is backgrounded and would prevent the client from applying its own delivery "
        "rules — including the rule that emergency alerts are exempt from the application's "
        "general notification preference.", BODY0))
    A(table("I", "Implementation size, measured from the repository",
            [["Component", "Files", "Lines"],
             ["Caregiver surface", "27", "3,942"],
             ["Elder surface", "6", "1,271"],
             ["Data access and models", "12", "1,082"],
             ["Reminder and alarm engine", "4", "329"],
             ["Notification and push", "2", "147"],
             ["Authentication", "1", "79"],
             ["Application total (Kotlin)", "83", "13,473"],
             ["Automated tests (Kotlin)", "26", "3,010"],
             ["Database migrations (SQL)", "15", "1,121"]],
            [1.62 * inch, 0.62 * inch, 0.72 * inch], align_right=(1, 2)))
    A(P("The test suite comprises 142 automated tests executing on the Java virtual machine with "
        "a shadowed Android runtime, requiring no device or emulator. Coverage is concentrated "
        "where silent failure is most likely: alarm scheduling and cancellation, offline "
        "durability of adherence records, delivery-contract behaviour of the push path, "
        "translation-table integrity across the four languages, and regression tests for each "
        "defect in Section V-C. Static analysis reports no errors at release configuration."))

    # ---------------- V. EVALUATION ----------------
    A(H("V", "Evaluation"))
    A(P("We evaluate three properties measurable from the artefact: conformance of the elder "
        "surface to age-friendly sizing guidance, interaction cost for core tasks, and the "
        "defect profile of the feature-complete system. All figures in this section are "
        "generated by a script that parses the application source directly, so they cannot drift "
        "from the implementation.", BODY0))

    A(SH("A", "Conformance of the elder surface"))
    A(fig("fig_typescale", 3,
          "Distribution of declared text sizes across the two surfaces, parsed from source. The "
          "caregiver surface concentrates at 12–13&nbsp;sp; the elder surface is shifted upward "
          "with a long tail to 72&nbsp;sp for the alarm control. Medians are 13&nbsp;sp and "
          "20&nbsp;sp respectively."))
    A(P("Fig. 3 shows every declared text size on each surface. The elder surface has a median "
        "of 20&nbsp;sp against 13&nbsp;sp for the caregiver surface, and 16.4% of its "
        "declarations fall below 16&nbsp;sp against 79.7% on the caregiver surface. The residual "
        "small sizes on the elder surface are supporting captions rather than primary content. "
        "Because the elder surface additionally composes a user-selected scale of 1.0, 1.15, or "
        "1.3 with the operating system's own font scaling, the effective floor rises further "
        "when either is increased.", BODY0))
    A(fig("fig_targets", 4,
          "Height of the primary control on each elder screen, parsed from source. Every primary "
          "control exceeds the 48&nbsp;dp platform minimum and the widely cited 44&times;44 "
          "target-size recommendation; the median is 76&nbsp;dp and the emergency control is "
          "130&nbsp;dp."))
    A(P("Fig. 4 reports the height of the primary control on each elder screen. The smallest is "
        "64&nbsp;dp, one third above the 48&nbsp;dp platform minimum [20] and comfortably above "
        "the widely cited 44&times;44 target-size recommendation associated with WCAG 2.2 AAA "
        "[21], noting that WCAG is specified in CSS pixels rather than Android density-independent "
        "pixels, so the comparison is indicative rather than exact; the median is 76&nbsp;dp. The "
        "emergency control is 130&nbsp;dp, roughly 2.7 times the platform minimum, reflecting "
        "its priority. We report this as conformance evidence, not as a usability result: "
        "meeting a sizing guideline is necessary, not sufficient, and only a study with older "
        "adults can establish sufficiency.", BODY0))

    A(SH("B", "Task interaction cost"))
    A(fig("fig_taps", 5,
          "Taps to completion for core tasks on the elder surface, counted from the notification "
          "or from the home screen. Deep-linking the medication reminder removed one tap from "
          "the most frequent task; other paths were already minimal."))
    A(P("Fig. 5 counts taps to completion for the elder surface's core tasks. Raising an alarm "
        "costs one tap. Confirming a dose from its reminder costs three, following a change that "
        "routes the notification directly to the medication screen rather than to the home "
        "screen; previously it cost four. The saving is one tap, which sounds trivial and is "
        "not: each intermediate screen is an opportunity for an older adult to lose the thread, "
        "and an unconfirmed dose is recorded as missed and shown to the caregiver as a possible "
        "non-adherence event. Interaction cost on this surface is therefore coupled to data "
        "quality, not only to convenience.", BODY0))

    A(SH("C", "Defect study"))
    A(P("After the system was feature-complete and had been exercised on physical devices, we "
        "conducted a structured audit: a line-by-line review of all 83 source files together "
        "with targeted inspection of the database schema and access policies. We recorded every "
        "defect found and classified each by whether a user could have detected it from the "
        "interface alone. Every defect reported here was subsequently corrected, and each "
        "correction is traceable to a commit in the project's version-control history "
        "together with the regression test added for it.", BODY0))
    A(fig("fig_defects", 6,
          "Defects found in the audit window, by class. Eight of thirteen (62%, red) would have "
          "failed with no user-visible signal: the interface either reported success or reported "
          "nothing at all."))
    A(P("Thirteen distinct defects were found. Fig. 6 gives the distribution. The dominant "
        "finding is that 8 of 13 (62%) were <i>silent</i>: the operation failed, or never "
        "occurred, while the interface reported success or reported nothing.", BODY0))
    A(P("Three classes deserve comment."))
    A(Spacer(1, 1))
    for b in bullets([
        "<b>Unreachable features (3).</b> Two complete features — general reminders for "
        "hydration and activity, and the caregiver invitation path — were written, exercised by "
        "the caregiver, stored correctly in the database, and delivered to nobody. General "
        "reminders were never armed as alarms on the older adult's device and no server job "
        "scanned for them. The invitation path depended on the telephone-number assumption "
        "broken by federated sign-in (Section III-B). In both cases the caregiver's interface "
        "behaved as though the feature worked. Both were subsequently repaired: general "
        "reminders are now armed through the same scheduling path as medication doses, and the "
        "invitation path was replaced by the binding primitive of Section III-B.",
        "<b>Lost state (3).</b> Three view models rebuilt their state object when a background "
        "refresh completed, discarding a transient completion flag that the screen used to "
        "navigate. A save that had already succeeded on the server therefore appeared to do "
        "nothing. The defect is timing-dependent, which is why it survived manual testing: it "
        "reproduces only when the refresh completes after the save, most likely on a slow "
        "connection.",
        "<b>Null failure messages (1 class, 17 sites).</b> Every repository write terminates in "
        "taking the first element of the server's response. When access policy hides the "
        "returned row, that list is empty and the resulting exception carries no message. "
        "Seventeen call sites propagated that null message directly to the interface, rendering "
        "an empty error and presenting as an unresponsive button.",
    ]): A(b)
    A(Spacer(1, 2))
    A(P("Two observations generalise beyond this system. First, <i>identity-model migration "
        "produces latent dead features</i>. Moving from telephone-number authentication to "
        "federated sign-in silently invalidated every code path that used a telephone number as "
        "a join key. Nothing failed loudly; two features simply stopped being reachable. Any "
        "system migrating to federated identity should audit its non-authentication uses of the "
        "prior identifier."))
    A(P("Second, <i>test suites do not detect unreachable features</i>. Our 142 tests passed "
        "throughout the period in which two features delivered nothing, because each tested "
        "component behaved correctly in isolation. The defect lay in the absence of a caller. "
        "Detection required tracing each data type from producer to consumer — a check we "
        "recommend as routine practice and for which we found no substitute."))

    A(SH("D", "Deployability"))
    A(P("The application declares eleven permissions, none of them in a restricted group. Two "
        "restricted permissions present in an earlier revision were removed in favour of the "
        "user-mediated path of Section III-E. The release artefact is 20&nbsp;MB and targets the "
        "currently required API level. The permission model was designed to remain compatible "
        "with current Google Play policy; the application has not been published to the store. "
        "We report this because a prototype that cannot be distributed cannot be studied "
        "longitudinally in the field.", BODY0))

    A(SH("E", "Threats to validity"))
    A(P("The defect study has a single observer and no inter-rater agreement, so the "
        "classification carries the authors' judgement. The audit window covers the period after "
        "feature completion, not the whole development history, so the reported count is not a "
        "defect density for the project. Conformance measurements are static properties of the "
        "source and say nothing about perception or comprehension. Tap counts are analytic, "
        "counted by the authors on the implemented flows, and are not observed task times.", BODY0))
    # ---------------- VI. DISCUSSION ----------------
    A(H("VI", "Discussion"))
    A(SH("A", "Silence is the dominant risk"))
    A(P("The audit's central result is that most defects in a feature-complete care application "
        "were invisible from the interface. This has a specific consequence for elder care that "
        "it does not have for, say, a photo application. The older adult often cannot diagnose a "
        "silent failure, and the caregiver — the person who could — is looking at a different "
        "device that is also reporting success. A missing reminder produces no artefact at all; "
        "its absence is indistinguishable from a day with no medication due. The system's own "
        "adherence record, which the caregiver trusts, will attribute the gap to the older "
        "adult.", BODY0))
    A(P("We therefore propose that care systems adopt an explicit honesty discipline: "
        "acknowledge only observed outcomes; give every failure path a message a lay reader can "
        "act on; and, for any feature whose value is delivery, test the delivery rather than the "
        "storage."))
    A(SH("B", "Asymmetry as an accessibility budget"))
    A(P("The measured gap between the two surfaces (Section V-A) is not the result of applying "
        "guidelines harder on one screen. It is a consequence of removing work from that screen. "
        "Type size and target size compete with information density; a surface that must present "
        "a scheduling form cannot also give every control 96&nbsp;dp. By moving all configuration "
        "to the caregiver, the elder surface gains an accessibility budget it can spend on "
        "sizing and shallow navigation. We suggest this reframing — asymmetry as a budget "
        "rather than as simplification — is the transferable idea.", BODY0))
    A(SH("C", "Policy as a design input"))
    A(P("Application-store permission policy determined the architecture of our emergency path. "
        "This is unusual to state in a research paper and, we think, ought not to be. A design "
        "that cannot be distributed cannot accumulate the longitudinal evidence the field needs. "
        "Treating policy as a constraint at design time, rather than discovering it at "
        "submission time, is a practical contribution of this work.", BODY0))

    # ---------------- VII. SCALABILITY ----------------
    A(H("VII", "Scalability Considerations and Future Deployment"))
    A(P("The architecture is designed to support independent care circles: circles are disjoint, "
        "queries are scoped to a profile, and no routine operation requires a cross-tenant "
        "scan, so growth in the number of circles should be horizontal. The heaviest recurring "
        "server task is a scheduled scan for unanswered doses, whose cost is proportional to "
        "active profiles with due doses rather than to total users. Large-scale performance has "
        "not been benchmarked; the following is architectural analysis rather than measurement.", BODY0))
    A(P("Three properties keep per-user cost low. Reminder scheduling is performed on the "
        "device, so the server is not a timing authority and carries no per-dose timer. "
        "Adherence writes are small, append-only, and queue locally when offline. Media is "
        "limited to a few profile and medication photographs per circle."))
    A(P("The identified scaling risks are ordinary rather than architectural: push-token churn "
        "requires re-synchronisation on session restore, which we implement; a single scheduled "
        "scan is a single point of failure and would need partitioning; and per-row policy "
        "evaluation grows with policy count, currently thirty-three across seventeen tables, "
        "which should be benchmarked before any large deployment."))
    A(P("<b>Path to evaluation.</b> The immediate next step is not a feature. It is a study. We "
        "plan a dyad field deployment with older adults and their caregivers, measuring "
        "delivery-level outcomes the literature usually assumes: proportion of scheduled "
        "reminders actually presented by the device, time from alarm activation to caregiver "
        "acknowledgement, and frequency of the degraded emergency path. These are precisely the "
        "quantities our fail-honest instrumentation can record truthfully, which is the point of "
        "building it that way."))

    # ---------------- VIII. LIMITATIONS ----------------
    A(H("VIII", "Limitations"))
    A(P("We state these plainly because the paper's credibility depends on it.", BODY0))
    A(Spacer(1, 1))
    for b in bullets([
        "<b>No user study has been conducted.</b> No older adult or caregiver outside the "
        "development team has used the system under observation. We make no usability, "
        "adherence, or clinical claim. Every conformance number in Section V is a static "
        "property of the implementation.",
        "<b>No field measurement of delivery.</b> Reminder delivery has been verified on "
        "physical devices anecdotally and in a shadowed runtime exhaustively, but we have no "
        "distribution of delivery latency across manufacturers and power-management regimes — "
        "the measurement that would substantiate Section III-D.",
        "<b>Single system, single team.</b> The defect taxonomy comes from one application "
        "audited by its own developers. It is a case study, not a population estimate.",
        "<b>Localisation is untested with native speakers.</b> The four language tables are "
        "complete and machine-checked for structural integrity, but have not been reviewed by "
        "native speakers for register or clarity for older readers.",
        "<b>The system is not a medical device</b> and provides no diagnostic or dosing advice. "
        "Vital-sign banding is presented as general reference information only.",
    ]): A(b)

    # ---------------- IX. CONCLUSION ----------------
    A(H("IX", "Conclusion"))
    A(P("We presented CareCompanion, a caregiver-mediated elder-care system built on three "
        "commitments: an asymmetric dual-surface architecture that buys the older adult an "
        "accessibility budget by removing configuration from their device; out-of-band device "
        "binding that repairs an identity gap federated sign-in introduces; and fail-honest "
        "alerting, under which the interface never asserts an outcome it has not observed.", BODY0))
    A(P("Our measurements show the asymmetry is real — a median 20&nbsp;sp against 13&nbsp;sp, "
        "every primary control at or above 64&nbsp;dp — and our audit shows why the honesty "
        "discipline matters: 62% of the defects in a feature-complete system would have failed "
        "without any user-visible signal, including two features stored faithfully and delivered "
        "to nobody. These results suggest that elder-care systems should be evaluated not only "
        "for accessibility and functionality, but also for whether users receive truthful "
        "feedback when critical operations fail. Future work will evaluate CareCompanion with "
        "older adults and caregivers in real-world use."))

    # ---------------- REFERENCES ----------------
    A(H("", "References"))
    refs = [
        "United Nations Population Fund, <i>India Ageing Report 2023: Caring for Our Elders — "
        "Institutional Responses</i>. New Delhi, India: UNFPA India, 2023.",
        "Data for India, “Access to phones and the internet,” 2025. [Online]. "
        "Available: https://www.dataforindia.com/comm-tech/",
        "D. Chakraborty and C. Garg, “Navigating technology: Mobile media usage and "
        "reticence among older adults in rural India,” <i>Mobile Media &amp; "
        "Communication</i>, vol. 14, pp. 31–49, 2025.",
        "A. Khare, R. Mukherjee, and M. Bedarkar, “Beyond digital literacy: A structural "
        "model analysis of technology acceptance among elderly persons,” <i>Humanities and "
        "Social Sciences Communications</i>, vol. 13, art. 794, 2026.",
        "H. Poorcheraghi, R. Negarandeh, S. Pashaeypoor, and J. Jorian, “Effect of using a "
        "mobile drug management application on medication adherence and hospital readmission "
        "among elderly patients with polypharmacy: A randomized controlled trial,” <i>BMC "
        "Health Services Research</i>, vol. 23, art. 1192, 2023.",
        "O. Salmensuu, J. Isotalo, M. Rijken, V. Hyttinen-Huotari, M. Kaarakainen, and I. "
        "Linnosmaa, “Effects of using medication reminder technologies by home-dwelling "
        "older citizens: A systematic review,” <i>Age and Ageing</i>, vol. 55, no. 2, art. "
        "afag007, 2026.",
        "M. Awan, S. Ali, M. Ali, M. F. Abrar, H. Ullah, and D. Khan, “Usability barriers "
        "for elderly users in smartphone app usage: An analytical hierarchical process-based "
        "prioritization,” <i>Scientific Programming</i>, vol. 2021, art. 2780257, 2021.",
        "L. Elguera Paez and C. Zapata Del Río, “Elderly users and their main "
        "challenges usability with mobile applications: A systematic review,” in <i>Design, "
        "User Experience, and Usability</i>, LNCS. Cham, Switzerland: Springer, 2019, pp. "
        "423–438.",
        "N. Liu, J. Yin, S. S.-L. Tan, K. Y. Ngiam, and H. H. Teo, “Mobile health "
        "applications for older adults: A systematic review of interface and persuasive feature "
        "design,” <i>Journal of the American Medical Informatics Association</i>, vol. 28, "
        "pp. 2483–2501, 2021.",
        "M. Gomez-Hernandez, X. Ferre, C. Moral, and E. Villalba-Mora, “Design guidelines "
        "of mobile apps for older adults: Systematic review and thematic analysis,” <i>JMIR "
        "mHealth and uHealth</i>, vol. 11, art. e43186, 2023.",
        "E. Amouzadeh, I. Dianat, J. Faradmal, and M. Babamiri, “Optimizing mobile app "
        "design for older adults: Systematic review of age-friendly design,” <i>Aging "
        "Clinical and Experimental Research</i>, vol. 37, art. 248, 2025.",
        "C. C. Quinn, S. Staub, E. Barr, and A. Gruber-Baldini, “Mobile support for older "
        "adults and their caregivers: Dyad usability study,” <i>JMIR Aging</i>, vol. 2, no. "
        "1, art. e12276, 2019.",
        "C. Benmessaoud, K. J. Pfisterer, A. De Leon, A. Saragadam, N. El-Dassouki, K. G. M. "
        "Young, R. Lohani, and T. Xiong, “Design of a dyadic digital health module for "
        "chronic disease shared care: Development study,” <i>JMIR Human Factors</i>, vol. "
        "10, art. e45035, 2023.",
        "Android Developers, “Optimize for Doze and App Standby.” [Online]. Available: "
        "https://developer.android.com/training/monitoring-device-state/doze-standby",
        "E. Wiecek, S. Taylor, H. Rourke, F. Hammond, and N. Amador-Fernandez, “Mobile "
        "health apps for older adults: Real-world evidence on engagement and medication "
        "adherence,” <i>Frontiers in Digital Health</i>, vol. 8, art. 1716880, 2026.",
        "Z. Nimmanterdwong, S. Boonviriya, and P. Tangkijvanich, “Human-centered design of "
        "mobile health apps for older adults: Systematic review and narrative synthesis,” "
        "<i>JMIR mHealth and uHealth</i>, vol. 10, no. 1, art. e29512, 2022.",
        "M. Hamill, V. Young, J. Boger, and A. Mihailidis, “Development of an automated "
        "speech recognition interface for personal emergency response systems,” <i>Journal "
        "of NeuroEngineering and Rehabilitation</i>, vol. 6, art. 26, 2009.",
        "Google Play Console Help, “Use of SMS or Call Log permission groups.” "
        "[Online]. Available: "
        "https://support.google.com/googleplay/android-developer/answer/10208820",
        "Android Developers Blog, “Reminder: SMS/Call Log policy changes,” Jan. 2019. "
        "[Online]. Available: https://android-developers.googleblog.com/2019/01/"
        "reminder-smscall-log-policy-changes.html",
        "Material Design 3, “Accessibility and touch target size.” [Online]. "
        "Available: https://m3.material.io/foundations/accessible-design/patterns",
        "W3C, <i>Web Content Accessibility Guidelines (WCAG) 2.2</i>, W3C Recommendation, 2023. "
        "[Online]. Available: https://www.w3.org/TR/WCAG22/",
    ]
    for i, r in enumerate(refs, 1):
        A(Paragraph(f"[{i}]&nbsp;&nbsp;{r}", REF))

    return F


if __name__ == "__main__":
    build(story())
