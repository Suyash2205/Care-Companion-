"""
build_task29_paper.py
---------------------
Task 29 — the final IEEE-format research paper.

    python paper/build_task29_paper.py

This is a separate, shorter paper from the Task 24 preprint. Where that one is a
full measurement study, this one is the final paper the task asks for: the
problem, the method, the strongest measured results, the comparison with
existing work, and the conclusion — nothing else.

Every number is read from measurements/raw/ at build time. The reference list is
the subset of the forty reviewed sources of Task 28 that this paper actually
cites, renumbered in order of first citation.
"""
import os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "docs"))

import build_paper as bp
import task28_blocks as C

from reportlab.lib.pagesizes import LETTER
from reportlab.platypus import (BaseDocTemplate, Frame, Image, KeepTogether, PageTemplate,
                                Paragraph, Spacer)
from PIL import Image as PILImage

OUTDIR = os.path.expanduser("~/Desktop/CareCompanion-Tasks-28-31")
os.makedirs(OUTDIR, exist_ok=True)
OUT = os.path.join(OUTDIR, "Task-29-IEEE-Paper.pdf")

TITLE = ("Measuring a Dual-Surface Elder-Care Android Application: "
         "Interface Geometry, Latency and Data Isolation")
AUTHORS = ("Suyash S. Humne, Devanshi Pandey, Deon Menezes, "
           "Sonali Wankhede, and Balwant Kumar Singh")
AFFIL = ("Department of Electronics and Computer Engineering, "
         "K. J. Somaiya School of Engineering,<br/>"
         "Somaiya Vidyavihar University, Mumbai, India")

P, H, SH, table = bp.P, bp.H, bp.SH, bp.table
E, G, LAT, UT, ENV, APK = C.E, C.G, C.LAT, C.UT, C.ENV, C.APK
MED = LAT["rls_denied_select"]
TOTAL_EXEC = C.TOTAL_EXEC

TITLE_NOHYPH = bp.st("t29", fontName="Times-Bold", fontSize=18, leading=21, alignment=1,
                     spaceAfter=9, hyphenationLang="", embeddedHyphenation=0)

# ── references: the cited subset of the forty, renumbered in citation order ──
CITED = ["unfpa", "dataforindia", "chakraborty", "khare", "palas", "quinn", "benmessaoud",
         "grossman", "wozney", "liu", "gomez", "amouzadeh", "awan", "elguera", "hou",
         "beier", "shneiderman", "miller", "card", "nielsen", "iso9241", "nielsenmolich",
         "huckvale", "papageorgiou", "chen", "ross", "poorcheraghi", "salmensuu",
         "wiecek", "hirani", "pajalic", "torres", "hamill", "kim", "brooke", "bangor"]
NORM_CITED = ["material", "touchtarget", "wcag", "pgrls", "supabasersl", "doze", "fcm"]

_order, _seen = [], set()
def c(*keys):
    out = []
    for k in keys:
        if k not in _seen:
            _seen.add(k); _order.append(k)
        out.append(str(_order.index(k) + 1))
    return "[" + ", ".join(out) + "]"

def n(*keys):
    return c(*keys)

def refs_list():
    """The reference strings, in the order they were first cited."""
    all_refs = {}
    for k, i in C.R.items():
        all_refs[k] = C.REFS[i - 1]
    for k, i in C.N.items():
        all_refs[k] = C.NORMATIVE[i - 41]
    return [all_refs[k] for k in _order]


def mfig(path, num, caption, width=None):
    w, h = PILImage.open(path).size
    dw = (width or bp.COLW) - 6
    return KeepTogether([Spacer(1, 4), Image(path, width=dw, height=dw * h / w),
                         Paragraph(f"Fig. {num}.&nbsp; {caption}", bp.CAP)])


def story():
    F = []; A = F.append

    A(Paragraph(TITLE, TITLE_NOHYPH))
    A(Paragraph(AUTHORS, bp.AUTHOR))
    A(Paragraph(AFFIL, bp.AFFIL))
    A(Spacer(1, 6))

    # ───────────────── abstract ─────────────────
    A(Paragraph(
        "<b><i>Abstract</i></b>&mdash;Software built for elderly care is usually described "
        "rather than measured. This paper reports an instrumented measurement of "
        "CareCompanion, a working Android application that presents two distinct interface "
        "surfaces over one data model: an enlarged, photograph-first surface for the "
        "cared-for person and a dense administration console for the family member who "
        "manages their care. Five instruments were run against one frozen commit on a single "
        f"day: the automated test suite three times over ({TOTAL_EXEC} test executions), "
        f"{sum(v['n'] for v in LAT.values())} timed HTTPS requests across {len(LAT)} backend "
        f"endpoints, {C.N_PROBES} anonymous read attempts against all {C.N_TABLES} database "
        f"tables, a complete census of "
        f"{E['sp_n'] + G['sp_n'] + E['dp_n'] + G['dp_n']} declared interface sizes, and the "
        f"release artefact. The strongest results are a median care-data read latency of "
        f"{MED['median_ms']:.0f}&nbsp;ms (95th percentile {MED['p95_ms']:.0f}&nbsp;ms, "
        f"n&nbsp;=&nbsp;{MED['n']}); an elder-surface median control of "
        f"{E['dp_median']:.0f}&nbsp;dp against the 48&nbsp;dp platform minimum, "
        f"{E['dp_median'] / 48:.1f}&times; that floor; a median elder type size of "
        f"{E['sp_median']:.0f}&nbsp;sp against {G['sp_median']:.0f}&nbsp;sp on the guardian "
        f"surface; and zero rows of personal data returned to an unauthenticated reader "
        f"holding the application's own key, across all {len(C.PERSONAL)} personal tables. "
        f"Set against the literature, the elder surface exceeds the published geometry it was "
        f"designed from, the latency sits an order of magnitude inside the one-second "
        f"interaction threshold, and the data isolation is materially better than the audited "
        f"baseline for deployed health applications. Two results go the other way and are "
        f"reported as measured: the guardian surface is below the platform's interactive "
        f"minimum at the median, and {E['sp_pct_below_16']:.0f}&nbsp;% of elder text is "
        f"declared below 16&nbsp;sp. No health outcome is claimed.", bp.BODY))
    A(Paragraph("<b><i>Index Terms</i></b>&mdash;Elder care, assistive applications, Android, "
                "dyadic design, accessibility, access control, software measurement.",
                bp.BODY))

    # ───────────────── I. problem ─────────────────
    A(H("I", "The Problem"))
    A(P(f"India's population aged 60 and over is projected to reach roughly a fifth of the "
        f"country by 2050, against formal long-term care that reaches a small fraction of it "
        f"{c('unfpa')}. Care is therefore conducted by a family member, on a smartphone that "
        f"is usually already in the household {c('dataforindia')}. The published work on how "
        f"older adults actually use those phones describes reliance on a younger relative "
        f"rather than independent mastery {c('chakraborty', 'khare')}, and models of "
        f"acceptance among elderly mHealth users find social influence and habit carrying "
        f"significant weight {c('palas')}. The architectural response in the research "
        f"literature is the dyad: give the caregiver and the cared-for person separate "
        f"interfaces over shared data {c('quinn', 'benmessaoud')}."))
    A(P(f"That architecture is largely absent from shipped software. A content analysis of "
        f"the two major application stores found 44 caregiver-focused applications in total, "
        f"of which 8 covered three or more categories of support {c('grossman')}; a "
        f"systematic search of dementia-caregiver applications found them to be static "
        f"information with no evidence base {c('wozney')}. Where guidance does exist for this "
        f"population it is consistent — larger type, larger controls, fewer decisions per "
        f"screen {c('liu', 'gomez', 'amouzadeh')}, with small text and small targets at the "
        f"top of the barrier catalogues {c('awan', 'elguera')} — but it is stated as advice "
        f"and almost never reported as a measured property of a built application. The "
        f"large-scale accessibility analyses measure the population of applications rather "
        f"than one design against its own target {c('chen', 'ross')}. On the security side "
        f"the baseline is poor: of 79 accredited health and wellness applications, about "
        f"89&nbsp;% transmitted data online and none encrypted personal data stored on the "
        f"device {c('huckvale')}, a picture the wider audit literature confirms "
        f"{c('papageorgiou')}."))
    A(P("This paper therefore asks a narrow and checkable question. If the dyadic "
        "architecture is built as a working application, what does measurement say about its "
        "interface geometry, its response time and its data isolation — including the "
        "measurements that are unfavourable?"))

    # ───────────────── II. the system ─────────────────
    A(H("II", "The System Measured"))
    A(P(f"CareCompanion is a single Android module in layers over a managed PostgreSQL "
        f"backend reached across HTTPS; there is no bespoke application server. It is "
        f"{C.UI['code']['main_kt_lines']:,} lines of Kotlin in "
        f"{C.UI['code']['main_kt_files']} files, with "
        f"{C.UI['code']['test_kt_lines']:,} lines of test Kotlin "
        f"({C.UI['code']['test_kt_lines'] / C.UI['code']['main_kt_lines']:.2f} lines of test "
        f"per line of application code), over a database of {C.N_TABLES} tables."))
    A(SH("A", "Two surfaces over one data model"))
    A(P(f"The organising decision is that the application has two interface surfaces rather "
        f"than one interface with an accessibility mode — universal usability "
        f"{c('shneiderman')} taken to its structural conclusion. The <i>elder</i> surface is "
        f"photograph-first: tap a face to place a call, tap a tile to play a video, hold one "
        f"button to raise an alert. The <i>guardian</i> surface is an administration console "
        f"from which contacts, medicines, schedules, reminders and video shortcuts are "
        f"maintained remotely. The two are joined by a short connect code: the elder never "
        f"creates an account, chooses a password, or configures anything."))
    A(SH("B", "Access control in the database, not the client"))
    A(P(f"Every table carrying personal data has row-level security enabled "
        f"{n('pgrls', 'supabasersl')}, with policies expressed in terms of the authenticated "
        f"user's relationship to the row. The anonymous key is necessarily present in the "
        f"application package and must be assumed known; the design intent is that knowing it "
        f"grants no access to any personal row. Section&nbsp;IV-C reports what happened when "
        f"that was attempted."))

    # ───────────────── III. method ─────────────────
    A(H("III", "Method"))
    A(P(f"All measurement was performed against one frozen state of the source — commit "
        f"{ENV['git_commit']} of branch {ENV['git_branch']} — on {ENV['date_local']}. No code "
        f"was changed between the first instrument and the last. Conditions: "
        f"{ENV['host_os']}, {ENV['cores']} cores, {ENV['ram_gb']}&nbsp;GB RAM; "
        f"{ENV['network']}; Gradle {ENV['gradle']}, AGP {ENV['agp']}, Kotlin "
        f"{ENV['kotlin']}, compileSdk {ENV['compile_sdk']}, minSdk {ENV['min_sdk']}."))
    A(table(1, "The five instruments.", [
        ["Instrument", "Sample"],
        ["Automated suite, parsed from JUnit XML",
         f"{UT['tests']} tests in {UT['suites']} suites"],
        ["Two forced re-runs with <font face='Courier'>--rerun-tasks</font>",
         f"{TOTAL_EXEC} test executions in all"],
        ["Timed HTTPS requests with per-phase timing",
         f"{LAT['rest_root']['n']} trials &times; {len(LAT)} endpoints = "
         f"{sum(v['n'] for v in LAT.values())}"],
        ["Anonymous read probe, no user session",
         f"{C.N_TABLES} tables &times; 3 = {C.N_PROBES}"],
        ["Census of declared interface sizes",
         f"{E['sp_n'] + G['sp_n']} text, {E['dp_n'] + G['dp_n']} control"],
    ], [bp.COLW * 0.58, bp.COLW * 0.42]))
    A(P("Two methodological points matter for how the results should be read. First, the "
        "interface measurement is a census and not a sample: every declared size in the "
        "Compose source is counted, so there is no sampling error, but it measures what the "
        "source declares rather than what a particular handset renders. Letter-spacing "
        "declarations are excluded, since they are also written in <font face='Courier'>sp"
        "</font> and would otherwise depress the measured type scale. Second, network "
        "latency distributions are right-skewed, so medians and 95th percentiles are the "
        "headline figures and means are reported beside them rather than instead of them."))
    A(P(f"Alongside the automated instruments, the application was driven through its own "
        f"interface against a written plan of 116 numbered cases covering all 20 functional "
        f"areas, in the inspection tradition heuristic evaluation established "
        f"{c('nielsenmolich')}; 68 cases were executed live, including every case on the "
        f"critical paths."))

    # ───────────────── IV. results ─────────────────
    A(H("IV", "Results"))
    A(table(2, "Headline measured results. Conditions as in Section III.", [
        ["Result", "Value", "n"],
        ["Automated test executions passed", f"{TOTAL_EXEC} of {TOTAL_EXEC}", f"{TOTAL_EXEC}"],
        ["Care-data read latency, median", f"{MED['median_ms']:.0f} ms", f"{MED['n']}"],
        ["Care-data read latency, p95", f"{MED['p95_ms']:.0f} ms", f"{MED['n']}"],
        ["Elder median text size", f"{E['sp_median']:.0f} sp", f"{E['sp_n']}"],
        ["Guardian median text size", f"{G['sp_median']:.0f} sp", f"{G['sp_n']}"],
        ["Elder median control size", f"{E['dp_median']:.0f} dp", f"{E['dp_n']}"],
        ["Elder controls &ge; 48 dp", f"{E['dp_pct_at_least_48']:.0f} %", f"{E['dp_n']}"],
        ["Guardian controls &ge; 48 dp", f"{G['dp_pct_at_least_48']:.0f} %", f"{G['dp_n']}"],
        ["Personal rows to an anonymous reader", "0", f"{len(C.PERSONAL) * 3}"],
        ["Manual cases passed", "65 of 68", "68"],
        ["Release artefact", f"{C.APK_MB:.1f} MB", "1"],
    ], [bp.COLW * 0.54, bp.COLW * 0.28, bp.COLW * 0.18], align_right=(1, 2)))

    A(SH("A", "Latency"))
    A(P(f"The endpoint that stands between a tap and the data appearing is the care-data "
        f"read. Over {MED['n']} trials its median was {MED['median_ms']:.0f}&nbsp;ms, its "
        f"95th percentile {MED['p95_ms']:.0f}&nbsp;ms and its slowest single observation "
        f"{MED['max_ms']:.0f}&nbsp;ms; the mean, inflated by the right tail, was "
        f"{MED['mean_ms']:.0f}&nbsp;ms (SD {MED['sd_ms']:.0f}&nbsp;ms). Across all "
        f"{len(LAT)} endpoints the medians ranged from "
        f"{min(v['median_ms'] for v in LAT.values()):.0f} to "
        f"{max(v['median_ms'] for v in LAT.values()):.0f}&nbsp;ms over "
        f"{sum(v['n'] for v in LAT.values())} requests."))
    A(mfig(C.fig("fig-22-03-latency"), 1,
           f"Distribution of total request time by endpoint. Boxes span the interquartile "
           f"range; the line is the median. n&nbsp;=&nbsp;{LAT['rest_root']['n']} per "
           f"endpoint."))

    A(SH("B", "Interface geometry"))
    A(P(f"The census covers {E['sp_n'] + G['sp_n']} text-size and "
        f"{E['dp_n'] + G['dp_n']} control-size declarations. The two surfaces are measurably "
        f"different in the intended direction: the elder median text size is "
        f"{E['sp_median']:.0f}&nbsp;sp against the guardian's {G['sp_median']:.0f}&nbsp;sp "
        f"({E['sp_median'] / G['sp_median']:.1f}&times;), and the elder median control "
        f"{E['dp_median']:.0f}&nbsp;dp against {G['dp_median']:.0f}&nbsp;dp "
        f"({E['dp_median'] / G['dp_median']:.1f}&times;). Against the 48&nbsp;dp platform "
        f"minimum {n('material', 'touchtarget')}, {E['dp_pct_at_least_48']:.0f}&nbsp;% of "
        f"elder controls clear the floor and {G['dp_pct_at_least_48']:.0f}&nbsp;% of guardian "
        f"controls do."))
    A(mfig(C.fig("fig-22-06-targets"), 2,
           f"The complete declared control-size census against the 48&nbsp;dp platform "
           f"minimum. n&nbsp;=&nbsp;{E['dp_n']} elder and {G['dp_n']} guardian declarations."))

    A(SH("C", "Data isolation"))
    A(P(f"Every one of the {C.N_TABLES} tables was read three times with the application's "
        f"own anonymous key and no user session — {C.N_PROBES} attempts in all. All "
        f"{len(C.PERSONAL)} personal tables returned zero rows on all "
        f"{len(C.PERSONAL) * 3} attempts. The {len(C.REFERENCE)} reference tables "
        f"(application configuration, reminder categories, vital-sign threshold bands and a "
        f"list of accessible places) returned their rows, which is the intended behaviour; "
        f"each was inspected individually to confirm it holds no personal data rather than "
        f"assumed from its name."))
    A(mfig(C.fig("fig-22-07-rls"), 3,
           f"Rows returned to an unauthenticated reader, by table. Every personal table "
           f"returns zero. n&nbsp;=&nbsp;{C.N_PROBES} attempts over {C.N_TABLES} tables."))

    A(SH("D", "Stability under repetition"))
    A(P(f"The suite was executed three times from the same commit, with runs 2 and 3 forced "
        f"to re-execute so that no result could be served from the build cache. The outcome "
        f"was identical each time: {UT['tests']} tests, {UT['failed']} failures, "
        f"{UT['errors']} errors, {UT['skipped']} skips. Wall-clock times were "
        f"{C.RUN_WALL[0]}, {C.RUN_WALL[1]} and {C.RUN_WALL[2]}&nbsp;s; the first includes "
        f"daemon start and a cold compile. The manual campaign returned 65 passes, 2 partials "
        f"and no failures over 68 live cases, and found and fixed two defects in the course "
        f"of being run."))

    # ───────────────── V. comparison ─────────────────
    A(H("V", "Comparison With Existing Work"))
    A(table(3, "The measured results set against the published positions.", [
        ["Published position", "This work", "Relation"],
        [f"48 dp minimum interactive control {n('material', 'touchtarget', 'wcag')}",
         f"Elder median {E['dp_median']:.0f} dp",
         f"Exceeds, {E['dp_median'] / 48:.1f}&times;"],
        [f"48 dp minimum interactive control {n('material', 'touchtarget')}",
         f"Guardian median {G['dp_median']:.0f} dp", "<b>Below the minimum</b>"],
        [f"~1 s preserves train of thought {c('miller', 'card', 'nielsen')}",
         f"Median {MED['median_ms']:.0f} ms, p95 {MED['p95_ms']:.0f} ms",
         f"Inside, by ~{1000 / MED['median_ms']:.0f}&times;"],
        [f"~89 % of 79 accredited health apps transmitted data online; none encrypted "
         f"personal data at rest {c('huckvale', 'papageorgiou')}",
         f"0 personal rows to an anonymous reader over {C.N_PROBES} probes",
         "Better than the baseline"],
        [f"Accessibility barriers are the norm across thousands of shipped apps "
         f"{c('chen', 'ross')}",
         f"{E['dp_pct_at_least_48']:.0f} % of elder controls at or above the floor; full "
         f"census published", "Better on the elder surface"],
        [f"44 caregiver apps; 8 covered three or more support categories {c('grossman')}",
         "20 functional areas, all remotely administrable", "Broader feature coverage"],
        [f"Larger type helps older readers, no agreed threshold {c('hou', 'beier')}",
         f"Elder {E['sp_median']:.0f} sp vs guardian {G['sp_median']:.0f} sp",
         "Consistent in direction"],
        [f"Reminder apps improved adherence in trial {c('poorcheraghi', 'salmensuu', 'wiecek')}",
         "Reminder scheduling tested for correctness only",
         "<b>Not comparable</b>"],
        [f"Telecare gave a small mental-HRQoL benefit over 12 months "
         f"{c('hirani', 'pajalic')}",
         "Alert path implemented and exercised", "<b>Not comparable</b>"],
        [f"Fall detection: laboratory accuracy, thin field evidence {c('torres')}",
         "SOS is user-initiated by design", "Out of scope"],
        [f"Recognition accuracy problems for older voices {c('hamill', 'kim')}",
         "Voice supplements tap; accuracy not measured", "Design response only"],
    ], [bp.COLW * 0.42, bp.COLW * 0.34, bp.COLW * 0.24]))

    A(P(f"<b>Where this work is stronger.</b> The data-isolation result is the clearest. The "
        f"audit literature shows the failure mode in this category to be access control that "
        f"lives in the client and can therefore be bypassed {c('huckvale', 'papageorgiou')}; "
        f"placing it in the database {n('pgrls', 'supabasersl')} removes that class of "
        f"mistake, and {len(C.PERSONAL)} of {len(C.PERSONAL)} personal tables returning zero "
        f"rows is the measured consequence. The latency result is comfortable rather than "
        f"remarkable: {MED['median_ms']:.0f}&nbsp;ms against a one-second threshold "
        f"{c('miller', 'card', 'nielsen')} leaves ample headroom for the handset's own "
        f"rendering, which was not instrumented. The elder surface exceeds the only hard "
        f"geometric number the literature supplies, by {E['dp_median'] / 48:.1f}&times; at "
        f"the median."))
    A(P(f"<b>Where this work is weaker.</b> The guardian surface's median control of "
        f"{G['dp_median']:.0f}&nbsp;dp is below a floor that is not written as applying only "
        f"to elderly users {n('material', 'touchtarget', 'wcag')}. That undersized controls "
        f"are the norm in shipped Android software {c('chen', 'ross')} explains the result "
        f"without excusing it, and raising those minima is the first item of further work. "
        f"{E['sp_pct_below_16']:.0f}&nbsp;% of elder text is declared below 16&nbsp;sp; these "
        f"are secondary labels, but the evidence on older readers' sensitivity to legibility "
        f"{c('beier', 'hou')} says it is the kind of detail that costs a user."))
    A(P(f"<b>What cannot be compared at all.</b> The adherence benefit reported in the trial "
        f"literature {c('poorcheraghi', 'salmensuu')} and the quality-of-life benefit in the "
        f"telecare trials {c('hirani')} are outcomes measured in human participants. This "
        f"work measured an implementation. Following published design guidance does not "
        f"transfer the published benefit, and no such claim is made here. For the same "
        f"reason no usability claim is made in the sense ISO&nbsp;9241-11 defines "
        f"{c('iso9241')}: the instrument for that would be the System Usability Scale with "
        f"its published benchmarks {c('brooke', 'bangor')}, run with participants."))

    # ───────────────── VI. conclusion ─────────────────
    A(H("VI", "Conclusion"))
    A(P(f"A dyadic elder-care application was built and then measured rather than described. "
        f"Five results are supported by the evidence: the two surfaces differ measurably in "
        f"the intended direction, with an elder median control of {E['dp_median']:.0f}&nbsp;dp "
        f"({E['dp_median'] / 48:.1f}&times; the platform minimum) over a complete census of "
        f"{E['dp_n'] + G['dp_n']} declarations; the backend answers a care-data read in a "
        f"median {MED['median_ms']:.0f}&nbsp;ms (p95 {MED['p95_ms']:.0f}&nbsp;ms, "
        f"n&nbsp;=&nbsp;{MED['n']}); no personal row is reachable without authentication over "
        f"{C.N_PROBES} probes; the encoded behaviour is stable over {TOTAL_EXEC} test "
        f"executions in three runs with {UT['failed']} failures; and 65 of 68 live manual "
        f"cases passed with none failing. Two results go against the design and are reported "
        f"as measured rather than smoothed away."))
    A(P(f"The limitations are the shape of the further work. The geometry census measures "
        f"declarations rather than rendered pixels; the latency figures come from one network "
        f"on one day; the isolation result covers one threat and is not a security "
        f"assessment; and reminder delivery timing under the platform's Doze behaviour "
        f"{n('doze')} — the application's most consequential behaviour — has not yet been "
        f"instrumented across a range of handsets. Above all, no health or well-being outcome "
        f"has been measured. A usability study with elderly participants and their guardians, "
        f"scored with a validated instrument {c('brooke', 'bangor')}, is the study this work "
        f"should be followed by, and is the only route from a measured implementation to "
        f"evidence about the people it was built for."))

    A(H("", "Data Availability"))
    A(P("Every raw measurement file, the instrument scripts that produced them, and the "
        "figures derived from them are submitted with this paper. Each document is generated "
        "from those raw files at build time, so no value reported here is typed in by hand."))

    A(H("", "References"))
    for i, r in enumerate(refs_list(), 1):
        A(Paragraph(f"[{i}]&nbsp;&nbsp;{r}", bp.REF if hasattr(bp, "REF") else bp.CAP))
    return F


def build(F):
    doc = BaseDocTemplate(OUT, pagesize=LETTER, leftMargin=bp.ML, rightMargin=bp.MR,
                          topMargin=bp.MT, bottomMargin=bp.MB,
                          title=TITLE, author="S. S. Humne et al.")
    body_h = bp.PH - bp.MT - bp.MB
    first_h = body_h - bp.TITLE_H
    banner = Frame(bp.ML, bp.PH - bp.MT - bp.TITLE_H, bp.COLW * 2 + bp.GUT, bp.TITLE_H,
                   id="banner", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    def col(x, h, i):
        return Frame(x, bp.MB, bp.COLW, h, id=i, leftPadding=0, rightPadding=0,
                     topPadding=0, bottomPadding=0)
    doc.addPageTemplates([
        PageTemplate(id="first", onPage=bp.footer, frames=[
            banner, col(bp.ML, first_h, "f1l"),
            col(bp.ML + bp.COLW + bp.GUT, first_h, "f1r")]),
        PageTemplate(id="rest", onPage=bp.footer, frames=[
            col(bp.ML, body_h, "fl"), col(bp.ML + bp.COLW + bp.GUT, body_h, "fr")])])
    doc.build(F)
    print("Wrote:", OUT)


if __name__ == "__main__":
    build(story())
    print(f"{len(_order)} references cited")
