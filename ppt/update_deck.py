"""
update_deck.py — adds the slides the Internal Evaluation-1 guidelines require.

The deck's slides are hand-positioned shapes on a Somaiya letterhead rather than
layout placeholders, so new slides are made by deep-copying an existing slide and
rewriting its text. That keeps the branding byte-identical instead of trying to
re-create it.

    python3 update_deck.py
    -> CareCompanion-IE1.pptx
"""
import copy
import os

from pptx import Presentation
from pptx.util import Emu, Pt

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "CareCompanion-source.pptx")
OUT = os.path.join(HERE, "CareCompanion-IE1.pptx")

# Geometry of the title textbox on every content slide, used to identify it.
TITLE_L, TITLE_T = 1851709, 171450
MODEL_TITLE = "Conclusion"   # title + one bullet body: the cleanest slide to clone


# ---------- slide plumbing ----------
def clone(prs, src_index):
    """Deep-copy every shape of a slide onto a new one appended at the end."""
    src = prs.slides[src_index]
    new = prs.slides.add_slide(src.slide_layout)
    for shape in list(new.shapes):                    # drop layout placeholders
        shape._element.getparent().remove(shape._element)
    for shape in src.shapes:
        new.shapes._spTree.append(copy.deepcopy(shape._element))
    return new


def move(prs, old_index, new_index):
    lst = prs.slides._sldIdLst
    ids = list(lst)
    lst.remove(ids[old_index])
    lst.insert(new_index, ids[old_index])


def model_index(prs):
    """Locate the model slide by its title every time.

    A fixed index goes stale the moment a slide is inserted ahead of it, which
    silently makes later clones copy the wrong template.
    """
    for i, s in enumerate(prs.slides):
        tb = title_box(s)
        if tb is not None and tb.text_frame.text.strip() == MODEL_TITLE:
            return i
    raise LookupError(f"no slide titled {MODEL_TITLE!r}")


def title_box(slide):
    for sh in slide.shapes:
        if sh.has_text_frame and abs(sh.left - TITLE_L) < 5000 and abs(sh.top - TITLE_T) < 5000:
            return sh
    return None


def body_box(slide):
    """The largest text box sitting in the content area of the slide.

    Constrained by vertical position as well as size: the footer bar is a wide
    text frame too, and without the bound the body text lands in it.
    """
    best, best_area = None, 0
    tb = title_box(slide)
    for sh in slide.shapes:
        if not sh.has_text_frame or sh is tb:
            continue
        if not (600000 < (sh.top or 0) < 4500000):     # below the title, above the footer
            continue
        t = sh.text_frame.text
        if "SOMAIYA" in t.upper() or "TRUST" in t.upper():
            continue
        area = (sh.width or 0) * (sh.height or 0)
        if area > best_area:
            best, best_area = sh, area
    return best


def set_para(par, text):
    """Replace a paragraph's text, keeping the first run's formatting."""
    runs = par.runs
    if not runs:
        par.text = text
        return
    runs[0].text = text
    for r in runs[1:]:
        r._r.getparent().remove(r._r)


def set_lines(tf, lines, size=None):
    """Rewrite a text frame as `lines`, reusing the first paragraph's styling."""
    p0 = tf.paragraphs[0]
    for p in list(tf.paragraphs[1:]):
        p._p.getparent().remove(p._p)
    set_para(p0, lines[0])
    for line in lines[1:]:
        newp = copy.deepcopy(p0._p)
        p0._p.getparent().append(newp)
    for par, line in zip(tf.paragraphs, lines):
        set_para(par, line)
        if size:
            for r in par.runs:
                r.font.size = Pt(size)


def add_slide(prs, title, lines, size=None):
    s = clone(prs, model_index(prs))
    set_para(title_box(s).text_frame.paragraphs[0], title)
    set_lines(body_box(s).text_frame, lines, size=size)
    return s


# ---------- content ----------
TECHNOLOGIES = [
    "• Language & UI: Kotlin, Jetpack Compose, Material 3",
    "• Architecture: MVVM, Hilt dependency injection, Navigation Compose",
    "• Backend: Supabase (PostgreSQL) with row-level security — 17 tables, 33 policies",
    "• Authentication: Firebase Authentication, Google Sign-In via Credential Manager",
    "• Notifications: Firebase Cloud Messaging (data-only payloads)",
    "• Scheduling: AlarmManager (exact alarms), WorkManager, Boot receiver",
    "• Networking: Retrofit, OkHttp, kotlinx.serialization",
    "• Testing: JUnit, Robolectric, MockWebServer — 142 automated tests",
    "• Build: Gradle 8.11, Android Gradle Plugin 8.9, target API 36, minimum API 24",
]

STATUS_OF_WORK = [
    "• Replaced phone-OTP login with Google Sign-In for both user roles",
    "• Added single-use invite codes to bind an elder's device to a care profile",
    "• Completed guardian modules: medicines, schedules, reminders, vitals, adherence,",
    "   contacts, family access, SOS monitoring and media shortcuts",
    "• Completed elder modules: home, medicines, contacts, vitals, videos and settings",
    "   with four languages, three text sizes and a high-contrast mode",
    "• Backend deployed: 17 tables, 33 row-level security policies, scheduled missed-dose scan",
    "• Reliability work: three-path reminder arming, offline adherence queue, fail-honest SOS",
    "• 142 automated tests written; Android Lint clean at release configuration",
    "• Play-readiness: target API 36, signed release build, restricted permissions removed",
    "• IEEE-format research paper drafted (7 pages, 21 references)",
]

RESULTS = [
    "• Functional application running on physical Android devices; medicine reminders",
    "   verified firing at the scheduled time on device",
    "• Elder interface measured directly from source code:",
    "   – Median declared text size 20 sp, against 13 sp on the guardian interface",
    "   – Every primary control 64 dp or larger, against the 48 dp platform minimum",
    "   – Emergency control 130 dp, roughly 2.7x the platform minimum",
    "• Interaction cost: raising an alarm takes 1 tap; confirming a dose from its",
    "   notification takes 3 taps",
    "• Structured code audit found 13 defects, of which 8 (62%) would have failed with no",
    "   message to the user at all; all were corrected and covered by regression tests",
    "• Discussion: silent failure — not usability — proved the dominant risk. The app now",
    "   never reports an outcome it has not actually observed.",
]

TEST_CASES = [
    "142 automated tests run on the JVM with a shadowed Android runtime (no emulator needed).",
    "TC-01  Medicine reminder fires at the scheduled time — Pass",
    "TC-02  A dose answered early cancels its pending alarm (no double-dose prompt) — Pass",
    "TC-03  Armed alarms are restored after a device restart — Pass",
    "TC-04  Adherence recorded while offline is queued and synced later — Pass",
    "TC-05  SOS reaches the guardian; if it fails, the screen says so and offers a fallback — Pass",
    "TC-06  Invite code is single-use, expires in 7 days, cannot rebind a claimed profile — Pass",
    "TC-07  Elder interface renders correctly in English, Hindi, Marathi and Gujarati — Pass",
    "TC-08  Guardian save operations show a readable error when the network fails — Pass",
    "TC-09  General reminders (water, walk) are armed on the elder's device — Pass",
    "TC-10  Emergency alerts are not silenced by the general notification setting — Pass",
    "Android Lint: 0 errors at release configuration.",
]

PAPER_STATUS = [
    "Title: CareCompanion: Assistive Companion Mobile Application for Elderly and Disabled Users",
    "",
    "• Format: IEEE two-column, 7 pages, 6 figures, 1 table, 21 references",
    "• Contributions: asymmetric dual-surface design; out-of-band device binding;",
    "   fail-honest emergency alerting; an empirical defect taxonomy",
    "• Figures 3, 4 and 6 are generated directly from the application source, so the",
    "   reported measurements cannot drift from the implementation",
    "• All references verified against Crossref for authors, venue and identifiers",
    "• Status: draft complete and internally reviewed — not yet submitted",
    "• Pending: target venue selection and a user study to support the evaluation section",
]

FUTURE_SCOPE = [
    "• Medicine stock database — record how much of each medicine is in hand. Stock is",
    "   reduced automatically as the elder marks doses taken, and the guardian is notified",
    "   when a medicine is running low so it can be restocked before it runs out.",
    "• iOS application — extend CareCompanion to iPhone so elders on iOS are covered.",
    "• Publish on the Google Play Store for public distribution and real-world use.",
    "• Conduct a user study with elderly participants and their caregivers.",
    "• Voice input and vernacular speech support for users who find typing difficult.",
    "• Integration with pharmacies or clinics for prescription and refill data.",
]

LINKS = [
    "• Source code (GitHub): https://github.com/Suyash2205/Care-Companion-",
    "• Release APK: https://github.com/Suyash2205/Care-Companion-/releases",
    "• Research paper (IEEE format): submitted alongside this presentation",
    "• User manual (PDF): submitted alongside this presentation",
    "• Demo video: to be added before the demonstration",
]

# Slide 10 still described the Semester VI prototype (Java, Room, Android Studio only).
METHODOLOGY_FIX = {
    "Java": "Kotlin (Jetpack Compose)",
    "Room (Local Storage)": "Supabase / PostgreSQL with row-level security",
    "Android Notifications Manager": "Firebase Cloud Messaging + Notification Manager",
}


def main():
    prs = Presentation(SRC)
    before = len(prs.slides)

    # Correct the outdated stack on the Methodology slide.
    for sh in prs.slides[9].shapes:
        if not sh.has_text_frame:
            continue
        for par in sh.text_frame.paragraphs:
            for run in par.runs:
                if run.text.strip() in METHODOLOGY_FIX:
                    run.text = METHODOLOGY_FIX[run.text.strip()]

    # Build the new slides, then move each into place. Positions are 0-based and are
    # applied in ascending order so earlier inserts do not shift later ones.
    plan = [
        ("Technologies Used", TECHNOLOGIES, 10, 12),
        ("Status of Work Completed", STATUS_OF_WORK, 24, 11),
        ("Implementation Results and Discussion", RESULTS, 25, 11),
        ("Software Test Cases", TEST_CASES, 26, 11),
        ("Status of Research Paper", PAPER_STATUS, 27, 12),
        ("Future Scope", FUTURE_SCOPE, 28, 12),
        ("Supporting Documents and Links", LINKS, 37, 13),
    ]
    for title, lines, target, size in plan:
        add_slide(prs, title, lines, size=size)
        move(prs, len(prs.slides) - 1, target)

    prs.save(OUT)
    print(f"{before} slides -> {len(prs.slides)} slides")
    print("Wrote:", OUT)


if __name__ == "__main__":
    main()
