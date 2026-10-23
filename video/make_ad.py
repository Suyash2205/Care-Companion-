"""
make_ad.py — Task 27, the product advertisement.

Honesty rules this film keeps:
  * every phone screen is a real screenshot of the running application;
  * the elder's telephone number is masked, and so is one line from an older build
    that no longer exists in the app;
  * the narration claims nothing that has not been measured.

Craft notes:
  * There is no `zoompan` anywhere. Its integer rounding on a near-static zoom is what
    made the first cut jitter left and right. All movement comes either from the
    generated ambient footage underneath, or from overlays that travel and then settle.
  * The background is one continuous plate for the whole film, blurred and tinted per
    scene, so the picture is always alive instead of cutting between flat slides.
  * The score is synthesised in make_music.py, so nothing here is licensed from anyone.

    python video/make_ad.py
"""
import os, subprocess, json, shutil

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SHOTS = os.path.expanduser("~/Desktop/CareCompanion-Screenshots/app-screenshots")
ICON = os.path.join(ROOT, "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
WORK = os.path.join(HERE, "build")
OUTDIR = os.path.expanduser("~/Desktop/CareCompanion-Tasks-20-27")
OUT = os.path.join(OUTDIR, "Task-27-CareCompanion-Advertisement.mp4")
PLATE = os.path.join(HERE, "bg_open.mp4")          # MiniMax H3, image-to-video
os.makedirs(WORK, exist_ok=True)
os.makedirs(OUTDIR, exist_ok=True)

W, H, FPS = 1920, 1080, 30
SS = 2                                              # draw at 2x, downsample for clean type
VOICE = "Tara"
XF = 0.5                                            # cross-dissolve between scenes

from PIL import Image, ImageDraw, ImageFont, ImageFilter

CREAM = (247, 245, 240)
INK = (22, 30, 24)
GREEN = (46, 125, 50)
SUB_D = (196, 214, 198)
SUB_L = (108, 118, 108)
WHITE = (255, 255, 255)
AMBER = (214, 170, 92)

AV = "/System/Library/Fonts/Avenir Next.ttc"
def font(size, index=2):
    return ImageFont.truetype(AV, int(size * SS), index=index)
DEMI = lambda s: font(s, 6)
REG = lambda s: font(s, 2)


# ───────────────────────── drawing helpers ─────────────────────────
def blank():
    return Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))


def save_layer(img, name):
    p = os.path.join(WORK, name + ".png")
    img.resize((W, H), Image.LANCZOS).save(p)
    return p


def wrap(d, text, fnt, max_w):
    out, cur = [], ""
    for word in text.split():
        trial = (cur + " " + word).strip()
        if d.textlength(trial, font=fnt) <= max_w:
            cur = trial
        else:
            out.append(cur); cur = word
    if cur:
        out.append(cur)
    return out


def text_lines(text, fnt, max_w):
    return wrap(ImageDraw.Draw(blank()), text, fnt, max_w)


def put(img, lines, fnt, x, y, fill, leading=1.16, center=False):
    d = ImageDraw.Draw(img)
    lh = int(fnt.size * leading)
    for i, ln in enumerate(lines):
        xx = x - d.textlength(ln, font=fnt) / 2 if center else x
        d.text((xx, y + i * lh), ln, font=fnt, fill=fill)
    return y + len(lines) * lh


# Screenshots are the team's own test profiles, but this is a public film.
REDACT = {
    "guardian/01-add-elder-profile.png": [(44, 1152, 650, 1252)],
    "guardian/02-edit-elder-profile-connect-code.png": [(96, 548, 660, 628)],
    # From an older build; the line it shows about SMS is not in the product any more.
    "elder/04-sos-alert-sent.png": [(55, 818, 680, 915)],
}


def phone(rel, height_px):
    src = Image.open(os.path.join(SHOTS, rel)).convert("RGB")
    for box in REDACT.get(rel, []):
        r = src.crop(box)
        small = r.resize((max(1, r.width // 26), max(1, r.height // 14)), Image.BILINEAR)
        src.paste(small.resize(r.size, Image.NEAREST), box)
    bez = int(height_px * 0.013)
    rad = int(height_px * 0.052)
    sw = int(src.width * height_px / src.height)
    src = src.resize((sw, height_px), Image.LANCZOS)
    body = Image.new("RGBA", (sw + 2 * bez, height_px + 2 * bez), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle([0, 0, body.width - 1, body.height - 1], radius=rad,
                        fill=(14, 17, 15, 255))
    mask = Image.new("L", (sw, height_px), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, sw - 1, height_px - 1],
                                           radius=int(rad * 0.82), fill=255)
    body.paste(src, (bez, bez), mask)
    return body


def with_shadow(base, img, pos, blur=30, dy=18, opacity=92):
    sh = Image.new("RGBA", base.size, (0, 0, 0, 0))
    sh.paste(Image.new("RGBA", img.size, (0, 0, 0, opacity)),
             (pos[0], pos[1] + dy * SS), img)
    base.alpha_composite(sh.filter(ImageFilter.GaussianBlur(blur * SS / 2)))
    base.alpha_composite(img, pos)


def scrim(rgb, alpha, name):
    img = Image.new("RGBA", (W, H), rgb + (alpha,))
    p = os.path.join(WORK, name + ".png")
    img.save(p)
    return p


# ───────────────────────── scene layers ─────────────────────────
def rule_layer(name, color=GREEN, x=150, y=368, w=112):
    img = blank()
    ImageDraw.Draw(img).rectangle(
        [x * SS, y * SS, (x + w) * SS, (y + 7) * SS], fill=color)
    return save_layer(img, name)


def headline_layer(name, text, size, x, y, fill, max_w, center=False):
    img = blank()
    fnt = DEMI(size)
    lines = text_lines(text, fnt, max_w * SS)
    end = put(img, lines, fnt, x * SS, y * SS, fill, 1.14, center)
    return save_layer(img, name), end / SS


def body_layer(name, text, size, x, y, fill, max_w, center=False, weight=2):
    img = blank()
    fnt = font(size, weight)
    lines = text_lines(text, fnt, max_w * SS)
    end = put(img, lines, fnt, x * SS, y * SS, fill, 1.36, center)
    return save_layer(img, name), end / SS


def phones_layer(name, rels, height, dark):
    """The phone row, placed in the right-hand third (or centred when it is alone)."""
    img = blank()
    gap = 46 * SS
    avail = int((W - 1060 - 90) * SS)
    ph = [phone(r, int(height * SS)) for r in rels]
    total = sum(p.width for p in ph) + gap * (len(ph) - 1)
    if total > avail:
        k = avail / total
        ph = [phone(r, int(height * SS * k)) for r in rels]
        total = sum(p.width for p in ph) + gap * (len(ph) - 1)
    x = int(1060 * SS + ((W - 1060) * SS - total) / 2)
    for p in ph:
        with_shadow(img, p, (x, (H * SS - p.height) // 2),
                    opacity=120 if dark else 86)
        x += p.width + gap
    return save_layer(img, name)


def brand_layer(name, tagline, small=None):
    img = blank()
    icon = Image.open(ICON).convert("RGBA").resize((148 * SS, 148 * SS), Image.LANCZOS)
    img.alpha_composite(icon, ((W // 2 - 74) * SS, 318 * SS))
    put(img, ["CareCompanion"], DEMI(104), (W // 2) * SS, 512 * SS, WHITE, center=True)
    put(img, text_lines(tagline, REG(38), 1300 * SS), REG(38),
        (W // 2) * SS, 668 * SS, SUB_D, 1.35, center=True)
    if small:
        put(img, text_lines(small, REG(23), 1150 * SS), REG(23),
            (W // 2) * SS, 842 * SS, (150, 172, 152), 1.4, center=True)
    return save_layer(img, name)


# ───────────────────────── the film ─────────────────────────
def scene(name, kind, vo, layers, lead=0.55, tail=1.15, minimum=3.5):
    return dict(name=name, kind=kind, vo=vo, layers=layers,
                lead=lead, tail=tail, minimum=minimum)


def build_layers():
    """Returns the scene list. Each layer is (png, delay_s, rise_px)."""
    S = []

    S.append(scene("s01", "open", "This is CareCompanion.", [
        (brand_layer("l01", "For the elders we cannot be beside every day"), 0.25, 34),
    ], minimum=4.2))

    h, y = headline_layer("l02a", "More of our elders are living alone.", 76,
                          150, 430, WHITE, 1540)
    b, _ = body_layer("l02b", "The family is a city away. Sometimes a country away.", 38,
                      150, y + 34, SUB_D, 1280)
    S.append(scene("s02", "dark",
                   "More of our elders are living alone, and the family is a city away. "
                   "Sometimes a country away.",
                   [(rule_layer("l02r", GREEN, 150, 406, 112), 0.15, 0), (h, 0.32, 40), (b, 0.62, 30)]))

    h, y = headline_layer("l03a", "A missed dose. A number they cannot find.", 76,
                          150, 430, WHITE, 1540)
    b, _ = body_layer("l03b", "A fall, with nobody in the room.", 40,
                      150, y + 34, (236, 186, 176), 1280)
    S.append(scene("s03", "dark",
                   "A missed dose. A number they cannot find. A fall, with nobody in "
                   "the room.",
                   [(rule_layer("l03r", (192, 83, 70), 150, 406, 112), 0.15, 0), (h, 0.3, 40),
                    (b, 0.72, 30)]))

    h, y = headline_layer("l04a", "One app. Two sides.", 70, 140, 384, INK, 800)
    b, _ = body_layer("l04b",
                      "The family sets everything up on their own phone — the elder's "
                      "profile, their medicines, the people they call.", 33,
                      140, y + 30, SUB_L, 790)
    S.append(scene("s04", "cream",
                   "One app, two sides. The family sets everything up on their own phone.",
                   [(rule_layer("l04r", GREEN, 140, 352, 96), 0.1, 0), (h, 0.26, 36),
                    (b, 0.54, 26),
                    (phones_layer("l04p", ["guardian/01-add-elder-profile.png"], 790,
                                  False), 0.34, 44)]))

    h, y = headline_layer("l05a", "Six digits, and they are connected.", 70,
                          140, 384, INK, 800)
    b, _ = body_layer("l05b", "A code from the family's phone, typed into the elder's. "
                              "That is the whole setup.", 33, 140, y + 30, SUB_L, 790)
    S.append(scene("s05", "cream",
                   "Six digits, and the two phones are connected. That is the whole setup.",
                   [(rule_layer("l05r", GREEN, 140, 352, 96), 0.1, 0), (h, 0.26, 36),
                    (b, 0.54, 26),
                    (phones_layer("l05p",
                                  ["guardian/02-edit-elder-profile-connect-code.png",
                                   "elder/01-enter-connect-code.png"], 780, False),
                     0.34, 44)]))

    h, y = headline_layer("l06a", "Their side is bigger. On purpose.", 70,
                          140, 352, INK, 800)
    b, y2 = body_layer("l06b", "Larger type. Larger buttons. A contact is a face to tap, "
                               "not a name to find in a list.", 33, 140, y + 30, SUB_L, 790)
    n, _ = body_layer("l06c", "Measured: the elder's screens use larger type and larger "
                              "controls throughout.", 24, 140, y2 + 26, GREEN, 760, weight=6)
    S.append(scene("s06", "cream",
                   "Their side is bigger, on purpose. Larger type, larger buttons, and a "
                   "contact is a face to tap, not a name to find in a list.",
                   [(rule_layer("l06r", GREEN, 140, 320, 96), 0.1, 0), (h, 0.26, 36),
                    (b, 0.54, 26), (n, 0.86, 20),
                    (phones_layer("l06p", ["elder/02-contacts-tap-photo-to-call.png",
                                           "elder/03-videos.png"], 780, False),
                     0.34, 44)]))

    h, y = headline_layer("l07a", "It remembers the medicine,", 76, 150, 430, WHITE, 1500)
    b, _ = body_layer("l07b", "so they do not have to. And if they have already taken it, "
                              "it does not ask again.", 38, 150, y + 34, SUB_D, 1280)
    S.append(scene("s07", "dark",
                   "It remembers the medicine, so they do not have to. And if they have "
                   "already taken it, it does not ask again.",
                   [(rule_layer("l07r", GREEN, 150, 406, 112), 0.15, 0), (h, 0.3, 40), (b, 0.6, 30)]))

    h, y = headline_layer("l08a", "One button, if something is wrong.", 66,
                          140, 376, WHITE, 800)
    b, y2 = body_layer("l08b", "The family sees the alert, with the time and the place it "
                               "was raised.", 33, 140, y + 30, SUB_D, 780)
    n, _ = body_layer("l08c", "The elder's screen reports only what the app actually did.",
                      24, 140, y2 + 26, AMBER, 760, weight=6)
    S.append(scene("s08", "dark",
                   "One button, if something is wrong. The family sees the alert, with the "
                   "time and the place it was raised.",
                   [(rule_layer("l08r", AMBER, 140, 344, 96), 0.1, 0), (h, 0.26, 36),
                    (b, 0.54, 26), (n, 0.86, 20),
                    (phones_layer("l08p", ["elder/04-sos-alert-sent.png",
                                           "guardian/07-sos-alerts.png"], 780, True),
                     0.34, 44)]))

    S.append(scene("s09", "close",
                   "CareCompanion. Built so a family can be there, even when they are not "
                   "in the room.",
                   [(brand_layer("l09",
                                 "Built so a family can be there, even when they are not "
                                 "in the room.",
                                 "Not a medical device. It does not give medical advice and "
                                 "does not replace calling a doctor or the emergency "
                                 "services."), 0.2, 30)],
                   minimum=5.4))
    return S


# ───────────────────────── encoding ─────────────────────────
def run(cmd):
    return subprocess.run(cmd, check=True, capture_output=True, text=True)


def duration(path):
    return float(json.loads(run(["ffprobe", "-v", "quiet", "-print_format", "json",
                                 "-show_format", path]).stdout)["format"]["duration"])


def narrate(sc):
    aiff = os.path.join(WORK, sc["name"] + ".aiff")
    wav = os.path.join(WORK, sc["name"] + ".wav")
    run(["say", "-v", VOICE, "-r", "166", "-o", aiff, sc["vo"]])
    run(["ffmpeg", "-y", "-i", aiff, "-ar", "48000", "-ac", "2", wav])
    return wav, duration(wav)


def background_plate():
    """One continuous moving plate: the generated clip, ping-ponged and slowed."""
    out = os.path.join(WORK, "plate.mp4")
    run(["ffmpeg", "-y", "-i", PLATE, "-filter_complex",
         "[0:v]split[a][b];[b]reverse[r];[a][r]concat=n=2:v=1:a=0,"
         f"setpts=2.6*PTS,scale={W}:{H}:flags=lanczos,fps={FPS},format=yuv420p[v]",
         "-map", "[v]", "-an", "-c:v", "libx264", "-preset", "medium", "-crf", "17", out])
    return out, duration(out)


STYLE = {                      # blur sigma, scrim colour, scrim alpha
    "open":  (9,  (10, 28, 14), 142),
    "dark":  (14, (11, 26, 15), 178),
    "cream": (26, CREAM, 224),
    "close": (11, (9, 24, 12), 192),
}


def ease_y(delay, rise, dur=0.75):
    """Overlay y expression: starts `rise` px low, settles to 0 with a cubic ease-out."""
    if rise == 0:
        return "0"
    return (f"{rise}*pow(1-min(1\\,max(0\\,(t-{delay:.2f})/{dur}))\\,3)")


def build_scene(sc, plate, plate_dur, index):
    blur, col, alpha = STYLE[sc["kind"]]
    wav, vdur = narrate(sc)
    dur = max(round(vdur + sc["lead"] + sc["tail"], 2), sc["minimum"])
    sp = scrim(col, alpha, f"scrim_{sc['kind']}")
    start = round((index * 6.3) % max(plate_dur - dur - 0.2, 0.1), 2)

    inputs = ["-ss", str(start), "-stream_loop", "-1", "-i", plate,
              "-loop", "1", "-i", sp]
    for png, _, _ in sc["layers"]:
        inputs += ["-loop", "1", "-i", png]

    parts = [f"[0:v]gblur=sigma={blur},eq=saturation={0.75 if sc['kind']=='cream' else 1.05}"
             f":brightness={-0.02 if sc['kind']=='cream' else 0.0},fps={FPS}[bg]",
             "[1:v]format=rgba[sc]",
             "[bg][sc]overlay=0:0[b0]"]
    prev = "b0"
    for i, (png, delay, rise) in enumerate(sc["layers"]):
        idx = i + 2
        parts.append(f"[{idx}:v]format=rgba,fade=in:st={delay:.2f}:d=0.62:alpha=1[e{i}]")
        nxt = f"b{i+1}"
        parts.append(f"[{prev}][e{i}]overlay=x=0:y='{ease_y(delay, rise)}'[{nxt}]")
        prev = nxt
    # a gentle dip from black on the first scene only
    if index == 0:
        parts.append(f"[{prev}]fade=in:st=0:d=0.9,format=yuv420p[v]")
    else:
        parts.append(f"[{prev}]format=yuv420p[v]")

    clip = os.path.join(WORK, sc["name"] + "_v.mp4")
    run(["ffmpeg", "-y", *inputs, "-filter_complex", ";".join(parts),
         "-map", "[v]", "-t", str(dur), "-an",
         "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p", clip])
    return clip, dur, wav


def main():
    if shutil.which("ffmpeg") is None:
        raise SystemExit("ffmpeg is required")
    plate, plate_dur = background_plate()
    scenes = build_layers()

    clips, durs, wavs = [], [], []
    for i, sc in enumerate(scenes):
        clip, dur, wav = build_scene(sc, plate, plate_dur, i)
        clips.append(clip); durs.append(dur); wavs.append((wav, sc["lead"]))
        print(f"  {sc['name']}  {dur:5.2f}s")

    # cross-dissolve the scenes together
    inputs = []
    for c in clips:
        inputs += ["-i", c]
    chain, prev, acc = [], "0:v", 0.0
    starts = []
    for i in range(1, len(clips)):
        acc += durs[i - 1] - XF
        starts.append(acc)
        out = f"x{i}"
        chain.append(f"[{prev}][{i}:v]xfade=transition=fade:duration={XF}:"
                     f"offset={acc:.3f}[{out}]")
        prev = out
    total = sum(durs) - XF * (len(clips) - 1)
    silent = os.path.join(WORK, "silent.mp4")
    run(["ffmpeg", "-y", *inputs, "-filter_complex", ";".join(chain),
         "-map", f"[{prev}]", "-t", f"{total:.2f}",
         "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
         silent])

    # score, then the narration placed on the same timeline
    music = os.path.join(WORK, "score.wav")
    run([os.path.join(ROOT, "paper/.venv/bin/python"),
         os.path.join(HERE, "make_music.py"), f"{total:.2f}", music])

    offsets = [0.0] + starts
    ainputs, afilters, labels = ["-i", music], [], []
    afilters.append("[1:a]volume=0.30[m]")  # input 0 is the silent picture
    labels.append("[m]")
    for k, ((wav, lead), off) in enumerate(zip(wavs, offsets)):
        ainputs += ["-i", wav]
        ms = int((off + lead) * 1000)
        afilters.append(f"[{k+2}:a]adelay={ms}|{ms},volume=1.5[v{k}]")
        labels.append(f"[v{k}]")
    # amix attenuates by the input count; loudnorm then brings the mix to the level a
    # web advertisement is expected to sit at (about -16 LUFS) instead of -28.
    afilters.append("".join(labels) +
                    f"amix=inputs={len(labels)}:duration=longest:dropout_transition=0:"
                    f"normalize=0,"
                    f"loudnorm=I=-16:TP=-1.5:LRA=11,alimiter=limit=0.95,"
                    f"aresample=48000[a]")

    run(["ffmpeg", "-y", "-i", silent, *ainputs,
         "-filter_complex", ";".join(afilters),
         "-map", "0:v", "-map", "[a]", "-t", f"{total:.2f}",
         "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", OUT])
    print(f"Wrote: {OUT}  ({total:.1f} s)")


if __name__ == "__main__":
    main()
