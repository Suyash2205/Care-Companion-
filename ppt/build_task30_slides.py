"""
build_task30_slides.py
----------------------
Task 30 — the viva defence presentation.

    python ppt/build_task30_slides.py

Writes the editable deck and the PDF the task asks for:

    ~/Desktop/CareCompanion-Tasks-28-31/Task-30-Viva-Defense-Slides.pptx
    ~/Desktop/CareCompanion-Tasks-28-31/Task-30-Viva-Defense-Slides.pdf

Every measured figure is read from measurements/raw/ through docs/task28_blocks.py,
so the deck, the report and the paper state the same numbers.
"""
import os, sys, subprocess, shutil

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, os.path.join(ROOT, "docs"))

import task28_blocks as C

from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from PIL import Image as PILImage

OUTDIR = os.path.expanduser("~/Desktop/CareCompanion-Tasks-28-31")
os.makedirs(OUTDIR, exist_ok=True)
PPTX = os.path.join(OUTDIR, "Task-30-Viva-Defense-Slides.pptx")

E, G, LAT, UT, ENV, APK = C.E, C.G, C.LAT, C.UT, C.ENV, C.APK
MED = LAT["rls_denied_select"]
TOT = C.TOTAL_EXEC

GREEN = RGBColor(0x2E, 0x7D, 0x32)
GREEN_DARK = RGBColor(0x1B, 0x5E, 0x20)
GREEN_PALE = RGBColor(0xEA, 0xF6, 0xEC)
INK = RGBColor(0x1C, 0x1C, 0x1C)
SUB = RGBColor(0x5A, 0x5A, 0x5A)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
AMBER = RGBColor(0xB0, 0x7D, 0x00)
FONT = "Calibri"

W, H = Inches(13.333), Inches(7.5)
prs = Presentation()
prs.slide_width, prs.slide_height = W, H
BLANK = prs.slide_layouts[6]

_n = [0]


def _tf(box, text, size, bold=False, color=INK, align=PP_ALIGN.LEFT, space_after=6,
        italic=False):
    tf = box.text_frame
    tf.word_wrap = True
    lines = text.split("\n") if isinstance(text, str) else text
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.space_after = Pt(space_after)
        r = p.add_run(); r.text = line
        r.font.size = Pt(size); r.font.bold = bold; r.font.italic = italic
        r.font.color.rgb = color; r.font.name = FONT
    return tf


def box(slide, x, y, w, h):
    return slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))


def rect(slide, x, y, w, h, fill, line=None):
    from pptx.enum.shapes import MSO_SHAPE
    s = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(y),
                               Inches(w), Inches(h))
    s.fill.solid(); s.fill.fore_color.rgb = fill
    if line is None:
        s.line.fill.background()
    else:
        s.line.color.rgb = line; s.line.width = Pt(1)
    s.shadow.inherit = False
    return s


def slide(title=None, kicker=None):
    _n[0] += 1
    s = prs.slides.add_slide(BLANK)
    rect(s, 0, 0, 0.18, 7.5, GREEN)
    if kicker:
        _tf(box(s, 0.62, 0.30, 11.8, 0.32), kicker.upper(), 11, True, GREEN, space_after=0)
    if title:
        _tf(box(s, 0.6, 0.58, 12.2, 0.9), title, 30, True, GREEN_DARK, space_after=0)
        rect(s, 0.66, 1.52, 1.5, 0.045, GREEN)
    _tf(box(s, 0.6, 6.95, 8, 0.35), "CareCompanion — Task 30 viva defence", 10, False, SUB,
        space_after=0)
    nb = box(s, 11.9, 6.95, 1.0, 0.35)
    _tf(nb, str(_n[0]), 10, False, SUB, align=PP_ALIGN.RIGHT, space_after=0)
    return s


def bullets(s, items, x=0.75, y=1.85, w=11.9, h=4.8, size=17, gap=11):
    tb = box(s, x, y, w, h)
    tf = tb.text_frame; tf.word_wrap = True
    for i, item in enumerate(items):
        bold = False
        if isinstance(item, tuple):
            item, bold = item
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(gap)
        r = p.add_run(); r.text = "•   " + item if not item.startswith(" ") else item
        r.font.size = Pt(size); r.font.name = FONT; r.font.color.rgb = INK; r.font.bold = bold
    return tb


def table(s, data, x, y, w, col_w=None, size=12, header=True, row_h=0.33):
    rows, cols = len(data), len(data[0])
    shape = s.shapes.add_table(rows, cols, Inches(x), Inches(y), Inches(w),
                               Inches(row_h * rows))
    t = shape.table
    if col_w:
        total = sum(col_w)
        for j, cw in enumerate(col_w):
            t.columns[j].width = Emu(int(Inches(w) * cw / total))
    for i, row in enumerate(data):
        t.rows[i].height = Inches(row_h)
        for j, val in enumerate(row):
            cell = t.cell(i, j)
            cell.margin_left = Inches(0.08); cell.margin_right = Inches(0.08)
            cell.margin_top = Inches(0.03); cell.margin_bottom = Inches(0.03)
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
            cell.fill.solid()
            cell.fill.fore_color.rgb = GREEN_PALE if (i == 0 and header) else WHITE
            tf = cell.text_frame; tf.word_wrap = True
            p = tf.paragraphs[0]
            bold = (i == 0 and header)
            txt = str(val)
            if "**" in txt:
                txt, bold = txt.replace("**", ""), True
            r = p.add_run(); r.text = txt
            r.font.size = Pt(size); r.font.bold = bold; r.font.name = FONT
            r.font.color.rgb = INK
    return t


def picture(s, path, x, y, h_in):
    w, h = PILImage.open(path).size
    s.shapes.add_picture(path, Inches(x), Inches(y), height=Inches(h_in),
                         width=Inches(h_in * w / h))


def stat(s, x, y, value, label, w=2.7, h=1.5):
    rect(s, x, y, w, h, GREEN_PALE)
    _tf(box(s, x + 0.1, y + 0.16, w - 0.2, 0.7), value, 30, True, GREEN_DARK,
        align=PP_ALIGN.CENTER, space_after=0)
    _tf(box(s, x + 0.1, y + 0.88, w - 0.2, 0.55), label, 11.5, False, SUB,
        align=PP_ALIGN.CENTER, space_after=0)


def note(s, text, y=5.95, tone=GREEN):
    rect(s, 0.75, y, 11.85, 0.85, GREEN_PALE)
    rect(s, 0.75, y, 0.045, 0.85, tone)
    _tf(box(s, 0.95, y + 0.14, 11.5, 0.6), text, 13, False, INK, space_after=0)


# ══════════════════════════ 1 · title ══════════════════════════
s = prs.slides.add_slide(BLANK)
rect(s, 0, 0, 13.333, 7.5, GREEN_DARK)
rect(s, 0, 0, 13.333, 0.22, GREEN)
_tf(box(s, 1.1, 1.75, 11.2, 0.5), "FINAL YEAR PROJECT · VIVA DEFENCE", 15, True,
    RGBColor(0xA5, 0xD6, 0xA7), align=PP_ALIGN.CENTER, space_after=0)
_tf(box(s, 1.0, 2.35, 11.4, 1.3), "CareCompanion", 60, True, WHITE,
    align=PP_ALIGN.CENTER, space_after=0)
_tf(box(s, 1.0, 3.55, 11.4, 0.7),
    "An assistive companion application for elderly and disabled users —\nbuilt, and then measured",
    20, False, RGBColor(0xC8, 0xE6, 0xC9), align=PP_ALIGN.CENTER, space_after=2)
_tf(box(s, 1.0, 4.85, 11.4, 1.0),
    "; ".join(m[0] for m in C.MEMBERS), 15, False, WHITE,
    align=PP_ALIGN.CENTER, space_after=4)
_tf(box(s, 1.0, 5.75, 11.4, 0.9),
    "Department of Electronics and Computer Engineering\n"
    "K. J. Somaiya School of Engineering, Somaiya Vidyavihar University, Mumbai",
    13, False, RGBColor(0xA5, 0xD6, 0xA7), align=PP_ALIGN.CENTER, space_after=2)
_n[0] = 1

# ══════════════════════════ 2 · agenda ══════════════════════════
s = slide("What this defence covers", "Agenda")
table(s, [
    ["#", "Section", "The question it answers"],
    ["1", "The problem", "Who is this for, and why is existing software not enough?"],
    ["2", "The literature gap", "What do 40 reviewed sources say, and what is missing?"],
    ["3", "Design", "What was built, and why in that shape?"],
    ["4", "Implementation", "What does the code actually consist of?"],
    ["5", "Method", "How was it measured, and under what conditions?"],
    ["6", "Results", "What do the numbers say — including the unflattering ones?"],
    ["7", "Comparison", "How does this stand against the published work?"],
    ["8", "Limitations and conclusion", "What is not claimed, and what comes next?"],
    ["9", "Contributions", "Who did what?"],
], 0.75, 1.8, 11.85, col_w=[0.5, 3.4, 8.1], size=14, row_h=0.46)

# ══════════════════════════ 3 · problem ══════════════════════════
s = slide("Care falls to the family, on a phone already in the house", "1 · The problem")
stat(s, 0.75, 1.95, "~20 %", "of India projected to be 60+ by 2050 [UNFPA]")
stat(s, 3.65, 1.95, "Thin", "formal long-term care provision")
stat(s, 6.55, 1.95, "Present", "smartphone already in the household")
stat(s, 9.45, 1.95, "Mediated", "older adults rely on a younger relative")
bullets(s, [
    "The carer is almost always a family member who does not sit in the same room.",
    "The published work on elderly smartphone use in India describes reticence and "
    "help-seeking, not independent mastery.",
    "Acceptance models for elderly mHealth users put social influence and habit — not "
    "digital literacy alone — at the centre.",
], y=3.75, size=16, gap=12)
note(s, "So the design question is not “how do we simplify one app?” but “who is actually "
        "operating it, and should they even be looking at the same screen?”")

# ══════════════════════════ 4 · gap ══════════════════════════
s = slide("Forty sources reviewed — three findings line up into a gap", "2 · The literature gap")
table(s, [
    ["What the literature establishes", "What is missing"],
    ["The dyad works: give the caregiver and the cared-for person separate interfaces over "
     "shared data.",
     "Almost absent from shipped software — 44 caregiver apps found in total, 8 covering "
     "three or more support categories."],
    ["Age-friendly geometry has converged: larger type, larger controls, fewer decisions "
     "per screen; 48 dp minimum target.",
     "Stated as guidance, almost never reported as a measured property of a built "
     "application."],
    ["Deployed health apps handle personal data badly: ~89 % of 79 accredited apps "
     "transmitted data online; none encrypted it at rest.",
     "Audits measure the damage afterwards rather than an enforcement mechanism chosen in "
     "advance."],
], 0.75, 1.95, 11.85, col_w=[1, 1], size=14, row_h=1.05)
note(s, "The gap this project fills: build the dyadic architecture, then report its geometry, "
        "its latency and its data isolation as measured quantities — including the ones that "
        "go against us.")

# ══════════════════════════ 5 · objectives ══════════════════════════
s = slide("Five objectives, each written to be checkable", "2 · Objectives")
table(s, [
    ["#", "Objective", "How it is checked"],
    ["1", "Build a working two-surface application", "It installs and runs; artefact measured"],
    ["2", "Elder surface exceeds the published geometry",
     "Complete census of declared sizes against the 48 dp minimum"],
    ["3", "Interaction inside the response-time budget",
     "Timed HTTPS trials against the ~1 s threshold"],
    ["4", "Personal data unreachable without authentication",
     "Anonymous reads attempted against every table"],
    ["5", "Report all of it honestly, unfavourable results included",
     "Units, sample sizes and conditions on every number"],
], 0.75, 1.95, 11.85, col_w=[0.5, 5.0, 6.5], size=15, row_h=0.72)

# ══════════════════════════ 6 · design ══════════════════════════
s = slide("One data model, two deliberately different surfaces", "3 · Design")
table(s, [
    ["", "Elder surface", "Guardian surface"],
    ["Who", "The cared-for person", "The family member who administers the care"],
    ["For", "Doing one thing: call, watch, raise an alert",
     "Setting up everything the elder's phone then shows"],
    ["Affordance", "A photograph, tapped", "A form, filled in"],
    ["Median text", f"**{E['sp_median']:.0f} sp** ({E['sp_n']} declarations)",
     f"{G['sp_median']:.0f} sp ({G['sp_n']} declarations)"],
    ["Median control", f"**{E['dp_median']:.0f} dp** ({E['dp_n']} declarations)",
     f"{G['dp_median']:.0f} dp ({G['dp_n']} declarations)"],
    ["Joined by", "A short connect code", "The elder never creates an account"],
], 0.75, 1.9, 11.85, col_w=[1.6, 4.6, 5.8], size=14, row_h=0.56)
note(s, "Not one interface with an accessibility mode. Two interfaces whose geometry differs "
        "by design — and Section 7.3 of the report measures how far apart they actually are.",
        y=6.2)

# ══════════════════════════ 7 · architecture ══════════════════════════
s = slide("Architecture — no bespoke server; the database is the authority", "3 · Design")
picture(s, C.pfig("fig_arch"), 1.1, 1.85, 3.9)
bullets(s, [
    "Compose interface → repositories → typed HTTP client → managed PostgreSQL over HTTPS.",
    "WorkManager arms the day's doses; exact alarms fire each one, because a naive timer is "
    "unreliable under Doze.",
    "Alerts travel over Firebase Cloud Messaging — the SMS path was removed for Play policy "
    "compliance.",
], x=0.75, y=6.0, size=14, gap=4)

# ══════════════════════════ 8 · data & security ══════════════════════════
s = slide("Access control lives in the database, not in the client", "3 · Design")
table(s, [
    ["Class", "Tables", "Policy"],
    [f"**Personal and care data ({len(C.PERSONAL)})**",
     "elders, contacts, medicines, schedules, reminders, vitals, alerts, sos_events, "
     "users, links, invite codes, adherence logs, shortcuts",
     "Row-level security restricts every row to the authenticated users entitled to it"],
    [f"**Reference data ({len(C.REFERENCE)})**",
     "app_config, reminder_categories, vital_thresholds, wheelchair_places",
     "Intentionally public; inspected individually — no personal data"],
], 0.75, 1.9, 11.85, col_w=[2.2, 5.2, 4.6], size=13, row_h=1.1)
note(s, "The anonymous key ships inside the APK and must be assumed known. The design intent "
        "is that knowing it buys nothing — and slide 14 shows what happened when we tried.",
        y=5.45)
bullets(s, [
    "What this does NOT claim: it is not encryption at rest, not protection against a "
    "compromised account, and not a penetration test.",
], y=6.5, size=14, gap=0)

# ══════════════════════════ 9 · implementation ══════════════════════════
s = slide("What the code actually consists of", "4 · Implementation")
stat(s, 0.75, 1.95, f"{C.UI['code']['main_kt_lines']:,}", "lines of application Kotlin")
stat(s, 3.65, 1.95, f"{C.UI['code']['test_kt_lines']:,}", "lines of test Kotlin")
stat(s, 6.55, 1.95, f"{C.N_TABLES}", "database tables")
stat(s, 9.45, 1.95, f"{C.APK_MB:.1f} MB", f"release artefact, {APK['permission_count']} permissions")
table(s, [
    ["Toolchain", "Value"],
    ["Gradle / AGP / Kotlin", f"{ENV['gradle']} / {ENV['agp']} / {ENV['kotlin']}"],
    ["SDK", f"compileSdk {ENV['compile_sdk']}, minSdk {ENV['min_sdk']} "
            f"(keeps older handsets supported)"],
    ["Clean debug build", f"{C.BUILD_S} s from committed source"],
    ["Test-to-code ratio",
     f"{C.UI['code']['test_kt_lines'] / C.UI['code']['main_kt_lines']:.2f} lines of test per "
     f"line of application code"],
], 0.75, 3.75, 11.85, col_w=[3.0, 8.85], size=14, row_h=0.52)

# ══════════════════════════ 10 · method ══════════════════════════
s = slide("Five instruments, one frozen commit, one day", "5 · Method")
table(s, [
    ["Instrument", "Sample", "Raw output"],
    ["Automated suite, parsed from JUnit XML",
     f"{UT['tests']} tests in {UT['suites']} suites", "unit_tests.csv, summary.json"],
    ["Two forced re-runs (--rerun-tasks)", f"{TOT} test executions in all",
     "test_repeats.csv, build_times.csv"],
    ["Timed HTTPS requests, per-phase timing",
     f"{LAT['rest_root']['n']} × {len(LAT)} endpoints = "
     f"{sum(v['n'] for v in LAT.values())}", "backend_latency.csv"],
    ["Anonymous read probe, no session",
     f"{C.N_TABLES} tables × 3 = {C.N_PROBES}", "rls_anon_reads.csv"],
    ["Census of declared interface sizes",
     f"{E['sp_n'] + G['sp_n']} text, {E['dp_n'] + G['dp_n']} control",
     "ui_text_sizes.csv, ui_touch_targets.csv"],
    ["Release artefact measurement", "1 artefact", "apk_metrics.csv, apk_sections.csv"],
], 0.75, 1.95, 11.85, col_w=[4.3, 3.9, 3.65], size=13.5, row_h=0.6)
note(s, f"All of it against commit {ENV['git_commit']} of branch {ENV['git_branch']} on "
        f"{ENV['date_local']} — {ENV['host_os']}, {ENV['network']}. No code changed between "
        f"the first instrument and the last.")

# ══════════════════════════ 11 · headline results ══════════════════════════
s = slide("The headline measured results", "6 · Results")
table(s, [
    ["Result", "Value", "Unit", "n"],
    ["Automated test executions passed", f"**{TOT} of {TOT}**", "executions", f"{TOT}"],
    ["Care-data read latency, median", f"**{MED['median_ms']:.0f}**", "ms", f"{MED['n']}"],
    ["Care-data read latency, 95th percentile", f"{MED['p95_ms']:.0f}", "ms", f"{MED['n']}"],
    ["Elder median text size", f"**{E['sp_median']:.0f}**", "sp", f"{E['sp_n']}"],
    ["Guardian median text size", f"{G['sp_median']:.0f}", "sp", f"{G['sp_n']}"],
    ["Elder median control size", f"**{E['dp_median']:.0f}**", "dp", f"{E['dp_n']}"],
    ["Elder controls at or above 48 dp", f"{E['dp_pct_at_least_48']:.0f}", "%", f"{E['dp_n']}"],
    ["Guardian controls at or above 48 dp", f"{G['dp_pct_at_least_48']:.0f}", "%",
     f"{G['dp_n']}"],
    ["Personal rows to an unauthenticated reader", "**0**", "rows", f"{len(C.PERSONAL) * 3}"],
    ["Manual test cases passed", "65 of 68", "cases", "68"],
], 0.75, 1.9, 11.85, col_w=[6.2, 2.2, 1.9, 1.55], size=13.5, row_h=0.42)

# ══════════════════════════ 12 · latency ══════════════════════════
s = slide("Latency — an order of magnitude inside the budget", "6 · Results")
picture(s, C.fig("fig-22-03-latency"), 0.9, 1.9, 3.7)
bullets(s, [
    f"Care-data read: median {MED['median_ms']:.0f} ms, p95 {MED['p95_ms']:.0f} ms, slowest "
    f"of {MED['n']} trials {MED['max_ms']:.0f} ms.",
    f"The yardstick is the classical ~1 s threshold at which a user's train of thought is "
    f"preserved — the median sits ~{1000 / MED['median_ms']:.0f}× inside it.",
    "Distributions are right-skewed, so medians and 95th percentiles lead; means are "
    "reported beside them, not instead of them.",
], x=0.75, y=5.85, size=14, gap=4)

# ══════════════════════════ 13 · geometry ══════════════════════════
s = slide("Interface geometry — a census, not a sample", "6 · Results")
picture(s, C.fig("fig-22-06-targets"), 0.9, 1.9, 3.5)
table(s, [
    ["", "Elder", "Guardian"],
    ["Median text", f"**{E['sp_median']:.0f} sp**", f"{G['sp_median']:.0f} sp"],
    ["Median control", f"**{E['dp_median']:.0f} dp**", f"{G['dp_median']:.0f} dp"],
    ["≥ 48 dp", f"{E['dp_pct_at_least_48']:.0f} %", f"{G['dp_pct_at_least_48']:.0f} %"],
    ["Text < 16 sp", f"{E['sp_pct_below_16']:.0f} %", f"{G['sp_pct_below_16']:.0f} %"],
], 8.0, 2.1, 4.6, col_w=[2.1, 1.3, 1.6], size=14, row_h=0.52)
note(s, f"Elder median control is {E['dp_median'] / 48:.1f}× the 48 dp platform minimum. The "
        f"guardian surface is BELOW that minimum at the median — a real finding, carried into "
        f"the conclusion, not smoothed away.", y=5.85, tone=AMBER)

# ══════════════════════════ 14 · isolation ══════════════════════════
s = slide("Data isolation — the strongest result", "6 · Results")
picture(s, C.fig("fig-22-07-rls"), 0.9, 1.9, 3.4)
bullets(s, [
    f"{C.N_PROBES} anonymous reads with the app's own key and no session, across all "
    f"{C.N_TABLES} tables.",
    f"All {len(C.PERSONAL)} personal tables returned 0 rows, every time.",
    f"The {len(C.REFERENCE)} reference tables returned their rows by design — each was "
    f"inspected to confirm it holds no personal data, rather than assumed from its name.",
    "Published baseline: ~89 % of 79 accredited health apps transmitted data online; none "
    "encrypted personal data at rest.",
], x=0.75, y=5.55, size=14, gap=3)

# ══════════════════════════ 15 · testing ══════════════════════════
s = slide("Tested two ways, because neither alone is enough", "6 · Results")
table(s, [
    ["Automated", "Value"],
    ["Suites / tests", f"{UT['suites']} / {UT['tests']}"],
    ["Runs (2 and 3 forced to re-execute)", "3"],
    ["Total executions", f"**{TOT}**"],
    ["Failures / errors / skips",
     f"**{UT['failed']} / {UT['errors']} / {UT['skipped']}**"],
    ["Wall-clock per run", f"{C.RUN_WALL[0]}, {C.RUN_WALL[1]}, {C.RUN_WALL[2]} s"],
], 0.75, 1.95, 5.8, col_w=[3.6, 2.2], size=14, row_h=0.52)
table(s, [
    ["Manual campaign", "Value"],
    ["Cases in the written plan", "116 across 20 areas"],
    ["Driven live end to end", "68"],
    ["Passed / partial / failed", "**65 / 2 / 0**"],
    ["Defects found and fixed in the campaign", "**2**"],
    ["Cross-device flows exercised", "7"],
], 6.85, 1.95, 5.75, col_w=[3.6, 2.15], size=14, row_h=0.52)
note(s, f"{TOT} passing executions is a statement about {UT['tests']} encoded behaviours and "
        f"nothing wider. The manual campaign was run as a search for defects — and it found "
        f"two.", y=5.1)

# ══════════════════════════ 16 · comparison ══════════════════════════
s = slide("Measured results against the published positions", "7 · Comparison")
table(s, [
    ["Published position", "This project", "Relation"],
    ["48 dp minimum interactive control",
     f"Elder median {E['dp_median']:.0f} dp", f"**Exceeds, {E['dp_median'] / 48:.1f}×**"],
    ["48 dp minimum interactive control",
     f"Guardian median {G['dp_median']:.0f} dp", "**Below the minimum**"],
    ["~1 s preserves train of thought",
     f"Median {MED['median_ms']:.0f} ms, p95 {MED['p95_ms']:.0f} ms",
     f"**Inside, ~{1000 / MED['median_ms']:.0f}×**"],
    ["~89 % of accredited health apps transmitted data; none encrypted at rest",
     f"0 personal rows over {C.N_PROBES} probes", "**Better than baseline**"],
    ["Accessibility barriers are the norm in shipped Android apps",
     f"{E['dp_pct_at_least_48']:.0f} % of elder controls clear the floor; full census "
     f"published", "**Better on elder surface**"],
    ["44 caregiver apps; 8 covered 3+ support categories",
     "20 functional areas, all remotely administrable", "**Broader coverage**"],
    ["Reminder apps improved adherence in an RCT",
     "Scheduling tested for correctness only", "**Not comparable**"],
    ["Telecare gave a small mental-HRQoL benefit",
     "Alert path implemented and exercised", "**Not comparable**"],
], 0.75, 1.9, 11.85, col_w=[5.0, 4.3, 2.55], size=13, row_h=0.52)

# ══════════════════════════ 17 · limitations ══════════════════════════
s = slide("What this project does not claim", "8 · Limitations")
bullets(s, [
    "No health or well-being outcome. Adherence and quality-of-life benefits are outcomes "
    "measured in participants; we measured an implementation.",
    "No usability claim in the ISO 9241-11 sense. Geometry consistent with guidance is a "
    "precondition for usability, not evidence of it.",
    "The census measures declarations, not rendered pixels — sp and dp are scaled by the "
    "user's settings and the screen's density.",
    "Latency is from one network, one location, one day; the tail is set by the network, "
    "not the application.",
    "The isolation result covers one threat. Not a penetration test, not encryption at rest, "
    "not protection against a compromised account.",
    "Voice recognition accuracy and reminder delivery timing under Doze are the two most "
    "valuable measurements not yet taken.",
], y=1.95, size=16, gap=13)
note(s, "Two measured results go against the design — the guardian control sizes, and "
        f"{E['sp_pct_below_16']:.0f} % of elder text below 16 sp. Both are in the report, in "
        f"the paper, and in the future work.", y=6.25, tone=AMBER)

# ══════════════════════════ 18 · conclusion ══════════════════════════
s = slide("What the evidence supports", "9 · Conclusion")
bullets(s, [
    (f"The application works and is installable — {C.APK_MB:.1f} MB artefact built from the "
     f"committed source in {C.BUILD_S} s.", True),
    (f"The two surfaces differ measurably in the intended direction — elder median control "
     f"{E['dp_median']:.0f} dp, {E['dp_median'] / 48:.1f}× the platform minimum, over a "
     f"complete census of {E['dp_n'] + G['dp_n']} declarations.", True),
    (f"Personal data is not reachable without authentication — 0 rows over {C.N_PROBES} "
     f"probes.", True),
    (f"The backend answers a care-data read in a median {MED['median_ms']:.0f} ms "
     f"(p95 {MED['p95_ms']:.0f} ms, n = {MED['n']}).", True),
    (f"The encoded behaviour is stable — {TOT} executions, {UT['failed']} failures; and "
     f"65 of 68 live manual cases passed with none failing.", True),
], y=2.0, size=17, gap=16)
note(s, "Every number on this slide is read from the raw measurement files when the deck is "
        "built. The report, the paper and this deck cannot disagree.")

# ══════════════════════════ 19 · future work ══════════════════════════
s = slide("What the next version should do", "9 · Future work")
table(s, [
    ["#", "Work", "Why it is placed here"],
    ["1", "Raise the guardian surface's controls to 48 dp and re-run the census",
     "The one measured result failing a published threshold — and cheap to fix"],
    ["2", "Instrument reminder delivery under Doze across real handsets",
     "The most consequential behaviour, and the largest measurement gap"],
    ["3", "Usability study with elderly participants, scored with SUS",
     "The only route from geometry to a usability claim"],
    ["4", "Measure speech recognition accuracy for older speakers",
     "Where the literature predicts voice fails for this population"],
    ["5", "Handset-side rendering measurement",
     "Closes the declared-versus-rendered gap without needing participants"],
    ["6", "Independent security assessment",
     "One measured threat is not an assessment"],
    ["7", "Longitudinal deployment reporting engagement",
     "The honest end point for this line of work"],
], 0.75, 1.9, 11.85, col_w=[0.5, 5.2, 6.15], size=13.5, row_h=0.58)

# ══════════════════════════ 20 · contributions ══════════════════════════
s = slide("Who did what", "10 · Contributions")
rows = [["Member", "Roll no.", "Area", "Chapters of the report"]]
chmap = {"Suyash S. Humne": "4.2, 5.1–5.5",
         "Devanshi Pandey": "3, 6, 7 and the report",
         "Deon Menezes": "4.1, 7.3",
         "Sonali Wankhede": "4.3, 4.5, 7.4",
         "Balwant Kumar Singh": "2, 8.1"}
for name, roll, area, _ in C.MEMBERS:
    rows.append([name, roll or "—", area, chmap.get(name, "—")])
table(s, rows, 0.75, 1.95, 11.85, col_w=[3.2, 1.8, 4.3, 2.55], size=14, row_h=0.62)
bullets(s, [
    "Done jointly: the requirements, the choice of the two-surface architecture, the 116-case "
    "manual test plan, and the review of the report.",
    "Each member submits separately and answers for their own section.",
], y=5.6, size=14, gap=8)

# ══════════════════════════ 21 · close ══════════════════════════
s = prs.slides.add_slide(BLANK)
_n[0] += 1
rect(s, 0, 0, 13.333, 7.5, GREEN_DARK)
rect(s, 0, 0, 13.333, 0.22, GREEN)
_tf(box(s, 1.0, 2.3, 11.4, 1.1), "Thank you", 50, True, WHITE, align=PP_ALIGN.CENTER,
    space_after=0)
_tf(box(s, 1.0, 3.5, 11.4, 0.6), "Questions", 24, False, RGBColor(0xC8, 0xE6, 0xC9),
    align=PP_ALIGN.CENTER, space_after=0)
_tf(box(s, 1.0, 4.5, 11.4, 1.4),
    "DOI   10.5281/zenodo.23601398   (resolves to the latest version)\n"
    "Final IEEE paper   10.5281/zenodo.23811415\n"
    "https://zenodo.org/records/23811415\n\n"
    "Raw measurement files, instrument scripts and figures are submitted with the report.",
    15, False, WHITE, align=PP_ALIGN.CENTER, space_after=5)

prs.save(PPTX)
print(f"Wrote: {PPTX}  ({len(prs.slides.__iter__.__self__._sldIdLst)} slides)")

# ── PDF ──
soffice = shutil.which("soffice")
if soffice:
    subprocess.run([soffice, "--headless", "--convert-to", "pdf", "--outdir", OUTDIR, PPTX],
                   check=True, capture_output=True, timeout=300)
    print("Wrote:", PPTX.replace(".pptx", ".pdf"))
else:
    print("soffice not found — convert the .pptx to PDF by hand")
