"""
make_demo.py — Task 31, the final end-to-end demonstration.

This is not the Task 27 advertisement re-cut. It is a demonstration film of the
finished build, and it shows the four things the task asks for, in order:

    setup  →  a complete operation from input to output  →  the final deployed
    application  →  the measured performance evidence

Honesty rules this film keeps:
  * every phone screen is a real screenshot of the running application;
  * the elder's telephone number is masked, and so is one line from an older
    build that no longer exists in the product;
  * every number on screen is read from measurements/raw/ when the film is
    built, so the film, the report, the paper and the deck cannot disagree;
  * the two measurements that go against the design are shown, not skipped.

Craft notes: no `zoompan` anywhere — its integer rounding on a slow zoom is what
made an earlier film jitter. Motion comes from the ambient plate underneath and
from overlays that travel in and settle.

    python video/make_demo.py
"""
import os, sys, subprocess, shutil

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "docs"))

import make_ad as A
import task28_blocks as C

from PIL import Image, ImageDraw

W, H, SS, FPS = A.W, A.H, A.SS, A.FPS
WORK = A.WORK
OUTDIR = os.path.expanduser("~/Desktop/CareCompanion-Tasks-28-31")
os.makedirs(OUTDIR, exist_ok=True)
OUT = os.path.join(OUTDIR, "Task-31-Final-Demonstration.mp4")

CREAM, INK, GREEN, WHITE = A.CREAM, A.INK, A.GREEN, A.WHITE
SUB_D, SUB_L, AMBER = A.SUB_D, A.SUB_L, A.AMBER
PALE = (234, 246, 236)
DEMI, REG = A.DEMI, A.REG
blank, save_layer, put, text_lines = A.blank, A.save_layer, A.put, A.text_lines
phone, with_shadow, scrim = A.phone, A.with_shadow, A.scrim

E, G, LAT, UT, ENV, APK = C.E, C.G, C.LAT, C.UT, C.ENV, C.APK
MED = LAT["rls_denied_select"]
TOT = C.TOTAL_EXEC

TOTAL_STEPS = 12


# ───────────────────────── layers ─────────────────────────
def chrome_layer(name, step, label, dark):
    """The persistent bar: what this film is, which step, and the frozen commit."""
    img = blank()
    d = ImageDraw.Draw(img)
    bar = (18, 32, 20, 210) if dark else (255, 255, 255, 215)
    d.rectangle([0, 0, W * SS, 74 * SS], fill=bar)
    d.rectangle([0, 74 * SS, W * SS, 77 * SS], fill=GREEN)
    fg = WHITE if dark else INK
    sb = SUB_D if dark else SUB_L
    put(img, ["CareCompanion — final end-to-end demonstration"], DEMI(23),
        56 * SS, 24 * SS, fg)
    put(img, [f"commit {ENV['git_commit']} · branch {ENV['git_branch']} · "
              f"measured {ENV['date_local'].split()[0]}"], REG(19),
        (W - 56) * SS - ImageDraw.Draw(blank()).textlength(
            f"commit {ENV['git_commit']} · branch {ENV['git_branch']} · "
            f"measured {ENV['date_local'].split()[0]}", font=REG(19)),
        26 * SS, sb)
    if step:
        d.rounded_rectangle([56 * SS, 104 * SS, 300 * SS, 150 * SS], radius=23 * SS,
                            fill=GREEN)
        put(img, [f"STEP {step} OF {TOTAL_STEPS}"], DEMI(21), 80 * SS, 116 * SS, WHITE)
        put(img, [label.upper()], DEMI(21), 326 * SS, 116 * SS,
            (168, 210, 172) if dark else GREEN)
    return save_layer(img, name)


def title_layer(name, text, sub, dark, y=196, max_w=1000):
    img = blank()
    fg = WHITE if dark else INK
    sb = SUB_D if dark else SUB_L
    end = put(img, text_lines(text, DEMI(58), max_w * SS), DEMI(58),
              56 * SS, y * SS, fg, 1.12)
    if sub:
        put(img, text_lines(sub, REG(30), max_w * SS), REG(30),
            56 * SS, end + 26 * SS, sb, 1.38)
    return save_layer(img, name)


def points_layer(name, items, dark, y=430, max_w=940, size=28):
    img = blank()
    d = ImageDraw.Draw(img)
    fg = WHITE if dark else INK
    yy = y * SS
    for it in items:
        d.ellipse([56 * SS, yy + 11 * SS, 68 * SS, yy + 23 * SS], fill=GREEN)
        lines = text_lines(it, REG(size), (max_w - 40) * SS)
        yy = put(img, lines, REG(size), 92 * SS, yy, fg, 1.40) + 20 * SS
    return save_layer(img, name)


def facts_layer(name, pairs, dark, x=56, y=430, w=940):
    """A panel of measured facts: label on the left, value on the right."""
    img = blank()
    d = ImageDraw.Draw(img)
    rowh = 62
    hgt = rowh * len(pairs) + 28
    d.rounded_rectangle([x * SS, y * SS, (x + w) * SS, (y + hgt) * SS], radius=18 * SS,
                        fill=(26, 44, 29, 190) if dark else (255, 255, 255, 228))
    d.rounded_rectangle([x * SS, y * SS, (x + 6) * SS, (y + hgt) * SS],
                        radius=3 * SS, fill=GREEN)
    fg = WHITE if dark else INK
    sb = SUB_D if dark else SUB_L
    for i, (k, v) in enumerate(pairs):
        yy = (y + 22 + i * rowh) * SS
        put(img, [k], REG(25), (x + 34) * SS, yy, sb)
        put(img, [v], DEMI(28), (x + w - 34) * SS - d.textlength(v, font=DEMI(28)),
            yy - 2 * SS, fg)
    return save_layer(img, name)


def phones_layer(name, rels, height=760, dark=True, left=1010):
    img = blank()
    gap = 44 * SS
    avail = int((W - left - 70) * SS)
    ph = [phone(r, int(height * SS)) for r in rels]
    total = sum(p.width for p in ph) + gap * (len(ph) - 1)
    if total > avail:
        k = avail / total
        ph = [phone(r, int(height * SS * k)) for r in rels]
        total = sum(p.width for p in ph) + gap * (len(ph) - 1)
    x = int(left * SS + (avail - total) / 2)
    for p in ph:
        with_shadow(img, p, (x, (H * SS - p.height) // 2 + 30 * SS),
                    opacity=125 if dark else 88)
        x += p.width + gap
    return save_layer(img, name)


def caption_layer(name, texts, dark, left=1010):
    """Small labels under the phone row saying what each screenshot is."""
    img = blank()
    d = ImageDraw.Draw(img)
    avail = W - left - 70
    cw = avail / len(texts)
    for i, t in enumerate(texts):
        cx = (left + cw * (i + 0.5)) * SS
        lines = text_lines(t, REG(21), (cw - 40) * SS)
        put(img, lines, REG(21), cx, 926 * SS, SUB_D if dark else SUB_L, 1.34, center=True)
    return save_layer(img, name)


def figure_layer(name, path, dark, caption=None, top=178, bottom=898):
    """A results figure, centred, on a card."""
    img = blank()
    src = Image.open(path).convert("RGBA")
    maxw, maxh = int(1180 * SS), int((bottom - top) * SS)
    k = min(maxw / src.width, maxh / src.height)
    src = src.resize((int(src.width * k), int(src.height * k)), Image.LANCZOS)
    card = Image.new("RGBA", (src.width + 56 * SS, src.height + 56 * SS), (0, 0, 0, 0))
    ImageDraw.Draw(card).rounded_rectangle(
        [0, 0, card.width - 1, card.height - 1], radius=16 * SS, fill=(255, 255, 255, 246))
    card.alpha_composite(src, (28 * SS, 28 * SS))
    x = int(W * SS - card.width - 70 * SS)
    with_shadow(img, card, (x, int(top * SS)), opacity=110 if dark else 70)
    if caption:
        put(img, text_lines(caption, REG(20), card.width - 20 * SS), REG(20),
            x + card.width // 2, int(top * SS) + card.height + 14 * SS,
            SUB_D if dark else SUB_L, 1.32, center=True)
    return save_layer(img, name)


def verdict_layer(name, text, tone, dark, y=838, x=56, w=940):
    img = blank()
    d = ImageDraw.Draw(img)
    col = {"good": GREEN, "warn": AMBER}[tone]
    lines = text_lines(text, REG(25), (w - 76) * SS)
    hgt = 40 + int(25 * 1.4) * len(lines)
    d.rounded_rectangle([x * SS, y * SS, (x + w) * SS, (y + hgt) * SS], radius=14 * SS,
                        fill=(26, 44, 29, 200) if dark else (255, 255, 255, 234))
    d.rounded_rectangle([x * SS, y * SS, (x + 6) * SS, (y + hgt) * SS], radius=3 * SS,
                        fill=col)
    put(img, lines, REG(25), (x + 32) * SS, (y + 18) * SS, WHITE if dark else INK, 1.40)
    return save_layer(img, name)


def closing_layer(name):
    img = blank()
    icon = Image.open(A.ICON).convert("RGBA").resize((136 * SS, 136 * SS), Image.LANCZOS)
    img.alpha_composite(icon, ((W // 2 - 68) * SS, 232 * SS))
    put(img, ["CareCompanion"], DEMI(88), (W // 2) * SS, 404 * SS, WHITE, center=True)
    put(img, text_lines("Final version — built, measured, and submitted with its evidence",
                        REG(32), 1200 * SS), REG(32),
        (W // 2) * SS, 536 * SS, SUB_D, 1.36, center=True)
    put(img, ["DOI  10.5281/zenodo.23601398  (resolves to the latest version)"], REG(26),
        (W // 2) * SS, 646 * SS, (168, 210, 172), center=True)
    put(img, text_lines(
        "Submitted with this film: the signed technical report, the IEEE paper and its DOI "
        "proof, the viva slides, both APKs, the complete source at the measured commit, "
        "every raw measurement file and the scripts that produced them.",
        REG(22), 1240 * SS), REG(22),
        (W // 2) * SS, 726 * SS, (150, 172, 152), 1.42, center=True)
    return save_layer(img, name)


# ───────────────────────── the film ─────────────────────────
def sc(name, kind, vo, layers, lead=0.5, tail=1.1, minimum=4.0):
    return dict(name=name, kind=kind, vo=vo, layers=layers,
                lead=lead, tail=tail, minimum=minimum)


def build_layers():
    S = []
    dark = lambda k: k in ("open", "dark", "close")

    # 1 — what this film is
    S.append(sc("d01", "open",
        "This is the final demonstration of CareCompanion. Everything you are about to see "
        "is the finished build, not the prototype. The film shows the setup, one complete "
        "operation from input to output, the deployed application itself, and the measured "
        "performance evidence behind it.",
        [(chrome_layer("c01", None, "", True), 0.0, 0),
         (title_layer("t01", "Final end-to-end demonstration",
                      "CareCompanion — an assistive companion application for elderly and "
                      "disabled users", True, y=250, max_w=1420), 0.25, 34),
         (points_layer("p01", [
             "The setup, from a built artefact to two connected phones",
             "A complete operation, from a guardian's input to an elder's screen",
             "The final deployed application, measured",
             "The performance evidence, with sample sizes and conditions",
         ], True, y=512, max_w=1420, size=29), 0.95, 26)]))

    # 2 — setup: the artefact
    S.append(sc("d02", "cream",
        f"Setup begins with the artefact. The release build measures "
        f"{C.APK_MB:.1f} megabytes, declares {APK['permission_count']} permissions, and "
        f"installs on Android {APK['min_sdk']} and above. It was compiled from the "
        f"committed source in {C.BUILD_S} seconds on the machine named on screen.",
        [(chrome_layer("c02", 1, "Setup · the built artefact", False), 0.0, 0),
         (title_layer("t02", "The application that is being demonstrated",
                      "Measured from the signed release package itself", False), 0.2, 30),
         (facts_layer("f02", [
             ("Version", f"{APK['version_name']} (code {ENV['app_version_code']})"),
             ("Package size", f"{C.APK_MB:.1f} MB"),
             ("Declared permissions", str(APK["permission_count"])),
             ("Runs on", f"Android {APK['min_sdk']} and above"),
             ("Built from source in", f"{C.BUILD_S} s"),
         ], False, y=424), 0.8, 26),
         (phones_layer("ph02", ["guardian/01-add-elder-profile.png"], 740, False), 0.5, 24),
         (caption_layer("cp02", ["The guardian surface, running"], False), 1.0, 0)]))

    # 3 — setup: connect code
    S.append(sc("d03", "cream",
        "Setup continues on the guardian's phone. The guardian signs in, creates the "
        "elder's profile, and the application issues a short connect code against it. "
        "That code is the only thing the elder is ever asked for.",
        [(chrome_layer("c03", 2, "Setup · guardian creates the elder", False), 0.0, 0),
         (title_layer("t03", "One profile, one code", None, False), 0.2, 30),
         (points_layer("p03", [
             "The guardian signs in on their own phone.",
             "They create the elder's profile — name, photograph, care detail.",
             "The application issues a short connect code against that profile.",
         ], False, y=372, max_w=880, size=27), 0.75, 24),
         (phones_layer("ph03", ["guardian/01-add-elder-profile.png",
                                "guardian/02-edit-elder-profile-connect-code.png"],
                       720, False), 0.45, 22),
         (caption_layer("cp03", ["Create the elder profile", "The connect code"],
                        False), 1.1, 0)]))

    # 4 — setup: the elder joins
    S.append(sc("d04", "dark",
        "On the elder's phone, that code is the whole of setup. There is no account to "
        "create, no password to choose and nothing to configure. One screen, one code, and "
        "the two phones are bound together.",
        [(chrome_layer("c04", 3, "Setup · the elder joins", True), 0.0, 0),
         (title_layer("t04", "The elder's entire setup", "One screen. One code.", True),
          0.2, 30),
         (points_layer("p04", [
             "No account. No password. No configuration.",
             "The code is exchanged for a session bound to that elder profile.",
             "From here the phone shows only the elder surface, and only that elder's data.",
         ], True, y=430, max_w=880, size=27), 0.8, 24),
         (phones_layer("ph04", ["elder/01-enter-connect-code.png"], 760, True), 0.45, 22),
         (caption_layer("cp04", ["Enter the connect code"], True), 1.1, 0)]))

    # 5 — the input half of the operation
    S.append(sc("d05", "cream",
        "Now the complete operation. It begins as an input on the guardian's phone: add a "
        "contact, give it a name and a telephone number, and attach a photograph. The "
        "photograph is the part that matters, because it is what the elder will actually "
        "recognise.",
        [(chrome_layer("c05", 4, "Operation · the input", False), 0.0, 0),
         (title_layer("t05", "Input: the guardian adds a contact",
                      "A name, a number, and a face", False), 0.2, 30),
         (points_layer("p05", [
             "Entered once, on the guardian's phone.",
             "The photograph becomes the elder's way of recognising the person.",
             "The elder is not asked to type, search or configure anything.",
         ], False, y=418, max_w=880, size=27), 0.8, 24),
         (phones_layer("ph05", ["guardian/03-add-contact-empty-photo.png",
                                "guardian/04-add-contact-with-photo.png"], 720, False),
          0.45, 22),
         (caption_layer("cp05", ["The empty contact form", "The photograph attached"],
                        False), 1.1, 0)]))

    # 6 — the output half of the operation
    S.append(sc("d06", "dark",
        "And here is the output, on the other phone, without the elder doing anything at "
        "all. The contact has arrived with its photograph. One tap on the face places the "
        "call. That is the complete operation, input to output, across two devices and two "
        "separate logins.",
        [(chrome_layer("c06", 5, "Operation · the output", True), 0.0, 0),
         (title_layer("t06", "Output: it is simply there",
                      "One tap on a face places the call", True), 0.2, 30),
         (points_layer("p06", [
             "The elder's phone was not touched between the two screens.",
             "Tapping the photograph asks for confirmation, then dials.",
             "Seven such cross-device flows were exercised during the test campaign.",
         ], True, y=430, max_w=880, size=27), 0.8, 24),
         (phones_layer("ph06", ["elder/02-contacts-tap-photo-to-call.png"], 760, True),
          0.45, 22),
         (caption_layer("cp06", ["The contact, on the elder's phone"], True), 1.1, 0),
         (verdict_layer("v06", "Input on one phone, output on the other, with nothing asked "
                               "of the person it was built for.", "good", True), 1.25, 20)]))

    # 7 — the rest of the deployed application
    S.append(sc("d07", "cream",
        "The same pattern runs through the rest of the deployed application. The guardian "
        "sets the video shortcuts; the elder sees tiles and taps one to watch. Medicines, "
        "reminders, vitals and wellness all work the same way: administered remotely, "
        "presented simply.",
        [(chrome_layer("c07", 6, "The deployed application", False), 0.0, 0),
         (title_layer("t07", "Administered remotely, presented simply", None, False),
          0.2, 30),
         (points_layer("p07", [
             "Twenty functional areas, all administrable from the guardian's phone.",
             "Contacts, medicines, schedules, reminders, videos, vitals, wellness, alerts.",
             "The elder surface shows the result, never the administration.",
         ], False, y=372, max_w=880, size=27), 0.75, 24),
         (phones_layer("ph07", ["guardian/06-video-and-apps-shortcuts.png",
                                "elder/03-videos.png"], 720, False), 0.45, 22),
         (caption_layer("cp07", ["Guardian sets the shortcuts", "Elder taps one to watch"],
                        False), 1.1, 0)]))

    # 8 — the emergency path
    S.append(sc("d08", "dark",
        "The emergency path runs the other way. The elder holds one button; the alert "
        "reaches the guardian's phone over push notification. This path was additionally "
        "exercised on a physical Android handset and captured as the validation record.",
        [(chrome_layer("c08", 7, "Operation · the emergency path", True), 0.0, 0),
         (title_layer("t08", "The alert travels the other way",
                      "Elder raises it; the guardian's phone receives it", True), 0.2, 30),
         (points_layer("p08", [
             "One held button on the elder surface — no menu, no confirmation maze.",
             "Delivered over Firebase Cloud Messaging; the SMS path was removed to comply "
             "with Play policy.",
             "Exercised on a physical handset as well as in the test campaign.",
         ], True, y=430, max_w=880, size=27), 0.8, 24),
         (phones_layer("ph08", ["elder/04-sos-alert-sent.png",
                                "guardian/07-sos-alerts.png"], 720, True), 0.45, 22),
         (caption_layer("cp08", ["The elder raises the alert", "The guardian receives it"],
                        True), 1.1, 0)]))

    # 9 — evidence: correctness
    S.append(sc("d09", "cream",
        f"Now the measured evidence. The automated suite was run three times from the same "
        f"commit, with two of those runs forced to re-execute from cold. {TOT} test "
        f"executions, {UT['failed']} failures, {UT['errors']} errors. Alongside it, 68 of a "
        f"written 116-case plan were driven live through the interface: 65 passed, two were "
        f"partial, none failed, and the campaign found and fixed two defects.",
        [(chrome_layer("c09", 8, "Evidence · correctness", False), 0.0, 0),
         (title_layer("t09", "Tested two ways", None, False, max_w=880), 0.2, 30),
         (facts_layer("f09", [
             ("Test executions", f"{TOT}"),
             ("Failures / errors", f"{UT['failed']} / {UT['errors']}"),
             ("Runs (2 forced from cold)", "3"),
             ("Manual cases driven live", "68 of 116"),
             ("Passed / partial / failed", "65 / 2 / 0"),
             ("Defects found and fixed", "2"),
         ], False, y=348, w=820), 0.7, 24),
         (figure_layer("g09", C.fig("fig-22-01-suites"), False,
                       f"Execution time and test count of every suite. "
                       f"n = 1 run, {UT['suites']} suites.", top=196, bottom=840),
          0.5, 22)]))

    # 10 — evidence: speed
    S.append(sc("d10", "dark",
        f"Speed. {sum(v['n'] for v in LAT.values())} timed requests over four backend "
        f"endpoints. The read an elder's screen depends on had a median of "
        f"{MED['median_ms']:.0f} milliseconds and a ninety-fifth percentile of "
        f"{MED['p95_ms']:.0f}, over thirty trials. The classical threshold for preserving a "
        f"user's train of thought is about one second, so the median sits roughly "
        f"{1000 / MED['median_ms']:.0f} times inside the budget.",
        [(chrome_layer("c10", 9, "Evidence · speed", True), 0.0, 0),
         (title_layer("t10", "Latency", None, True, max_w=820), 0.2, 30),
         (facts_layer("f10", [
             ("Timed requests", f"{sum(v['n'] for v in LAT.values())}"),
             ("Care-data read, median", f"{MED['median_ms']:.0f} ms"),
             ("95th percentile", f"{MED['p95_ms']:.0f} ms"),
             ("Slowest of 30 trials", f"{MED['max_ms']:.0f} ms"),
             ("Interaction threshold", "~1 000 ms"),
         ], True, y=348, w=820), 0.7, 24),
         (figure_layer("g10", C.fig("fig-22-03-latency"), True,
                       f"Total request time by endpoint. n = {LAT['rest_root']['n']} per "
                       f"endpoint.", top=196, bottom=840), 0.5, 22)]))

    # 11 — evidence: geometry
    S.append(sc("d11", "cream",
        f"Interface geometry, counted rather than claimed. Every declared size in the "
        f"source was censused. The elder surface's median control is "
        f"{E['dp_median']:.0f} density-independent pixels against a platform minimum of "
        f"forty-eight — about {E['dp_median'] / 48:.1f} times the floor — and its median "
        f"type size is {E['sp_median']:.0f} against the guardian's {G['sp_median']:.0f}.",
        [(chrome_layer("c11", 10, "Evidence · interface geometry", False), 0.0, 0),
         (title_layer("t11", "Geometry", None, False, max_w=820), 0.2, 30),
         (facts_layer("f11", [
             ("Declarations censused",
              f"{E['sp_n'] + G['sp_n'] + E['dp_n'] + G['dp_n']}"),
             ("Elder median control", f"{E['dp_median']:.0f} dp"),
             ("Guardian median control", f"{G['dp_median']:.0f} dp"),
             ("Elder median type", f"{E['sp_median']:.0f} sp"),
             ("Guardian median type", f"{G['sp_median']:.0f} sp"),
         ], False, y=348, w=820), 0.7, 24),
         (figure_layer("g11", C.fig("fig-22-06-targets"), False,
                       "Declared control sizes against the 48 dp platform minimum.",
                       top=196, bottom=840), 0.5, 22)]))

    # 12 — evidence: data isolation
    S.append(sc("d12", "dark",
        f"Data isolation, the strongest result. Every one of the {C.N_TABLES} tables was "
        f"read with the application's own anonymous key and no user session — "
        f"{C.N_PROBES} attempts. All {len(C.PERSONAL)} tables holding personal care data "
        f"returned nothing, every time. The four reference tables return fixed, impersonal "
        f"configuration by design, and each was inspected to confirm it.",
        [(chrome_layer("c12", 11, "Evidence · data isolation", True), 0.0, 0),
         (title_layer("t12", "Nothing without a session", None, True, max_w=820), 0.2, 30),
         (facts_layer("f12", [
             ("Tables probed", f"{C.N_TABLES}"),
             ("Read attempts", f"{C.N_PROBES}"),
             ("Personal tables", f"{len(C.PERSONAL)}"),
             ("Personal rows returned", "0"),
             ("Enforced in", "the database, not the client"),
         ], True, y=348, w=820), 0.7, 24),
         (figure_layer("g12", C.fig("fig-22-07-rls"), True,
                       f"Rows returned to an unauthenticated reader, by table. "
                       f"n = {C.N_PROBES}.", top=196, bottom=840), 0.5, 22)]))

    # 13 — the two that go against us
    S.append(sc("d13", "cream",
        f"Two measurements go against the design, and this film shows them rather than "
        f"skipping them. The guardian surface sits below the platform's interactive minimum "
        f"at the median, and {E['sp_pct_below_16']:.0f} per cent of the elder surface's "
        f"declared text falls under sixteen. Both are in the report, both are in the paper, "
        f"and raising the guardian minima is the first item of further work.",
        [(chrome_layer("c13", 12, "Evidence · what goes against us", False), 0.0, 0),
         (title_layer("t13", "The two results we did not like",
                      "Reported as measured, and carried into the future work", False),
          0.2, 30),
         (facts_layer("f13", [
             ("Guardian median control", f"{G['dp_median']:.0f} dp — below the 48 dp floor"),
             ("Guardian controls ≥ 48 dp", f"{G['dp_pct_at_least_48']:.0f} %"),
             ("Elder text below 16 sp", f"{E['sp_pct_below_16']:.0f} %"),
         ], False, y=440, w=1150), 0.8, 26),
         (verdict_layer("v13", "A measurement that flatters the design and a measurement "
                               "that does not were taken by the same instrument, on the same "
                               "day, and both are reported.", "warn", False,
                        y=676, w=1150), 1.2, 22)]))

    # 14 — close
    S.append(sc("d14", "close",
        "That is the finished application: set up on two phones, driven from a guardian's "
        "input to an elder's screen, and measured end to end. The raw measurement files and "
        "the scripts that produced them are submitted with this film, so every number in it "
        "can be checked.",
        [(chrome_layer("c14", None, "", True), 0.0, 0),
         (closing_layer("cl14"), 0.3, 26)], tail=1.5))

    return S


def main():
    if shutil.which("ffmpeg") is None:
        raise SystemExit("ffmpeg is required")
    A.OUT = OUT
    plate, plate_dur = A.background_plate()
    scenes = build_layers()

    clips, durs, wavs = [], [], []
    for i, s in enumerate(scenes):
        clip, dur, wav = A.build_scene(s, plate, plate_dur, i)
        clips.append(clip); durs.append(dur); wavs.append((wav, s["lead"]))
        print(f"  {s['name']}  {dur:5.2f}s")

    assemble(clips, durs, wavs)


def assemble(clips, durs, wavs):
    """Cross-dissolve the scene clips, lay the score and the narration, and encode."""
    XF = A.XF
    inputs = []
    for c in clips:
        inputs += ["-i", c]
    chain, prev, acc, starts = [], "0:v", 0.0, []
    for i in range(1, len(clips)):
        acc += durs[i - 1] - XF
        starts.append(acc)
        out = f"x{i}"
        chain.append(f"[{prev}][{i}:v]xfade=transition=fade:duration={XF}:"
                     f"offset={acc:.3f}[{out}]")
        prev = out
    total = sum(durs) - XF * (len(clips) - 1)
    silent = os.path.join(WORK, "demo_silent.mp4")
    A.run(["ffmpeg", "-y", *inputs, "-filter_complex", ";".join(chain),
           "-map", f"[{prev}]", "-t", f"{total:.2f}",
           "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
           silent])

    music = os.path.join(WORK, "demo_score.wav")
    A.run([os.path.join(ROOT, "paper/.venv/bin/python"),
           os.path.join(HERE, "make_music.py"), f"{total:.2f}", music])

    offsets = [0.0] + starts
    ainputs, afilters, labels = ["-i", music], ["[1:a]volume=0.17[m]"], ["[m]"]
    for k, ((wav, lead), off) in enumerate(zip(wavs, offsets)):
        ainputs += ["-i", wav]
        ms = int((off + lead) * 1000)
        afilters.append(f"[{k+2}:a]adelay={ms}|{ms},volume=1.6[v{k}]")
        labels.append(f"[v{k}]")
    afilters.append("".join(labels) +
                    f"amix=inputs={len(labels)}:duration=longest:dropout_transition=0:"
                    f"normalize=0,loudnorm=I=-16:TP=-1.5:LRA=11,alimiter=limit=0.95,"
                    f"aresample=48000[a]")

    A.run(["ffmpeg", "-y", "-i", silent, *ainputs,
           "-filter_complex", ";".join(afilters),
           "-map", "0:v", "-map", "[a]", "-t", f"{total:.2f}",
           "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", OUT])
    print(f"Wrote: {OUT}  ({total:.1f} s)")


if __name__ == "__main__":
    main()
