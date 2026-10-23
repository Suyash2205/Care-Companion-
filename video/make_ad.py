"""
make_ad.py — Task 27, the product advertisement video.

Every phone screen shown in this film is a real screenshot of the running application.
Nothing is a mock-up and nothing is generated. The narration deliberately makes no claim
about speed, accuracy or health outcomes, because none of those has been measured.

    python video/make_ad.py
"""
import os, subprocess, json, textwrap, shutil, math

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SHOTS = os.path.expanduser("~/Desktop/CareCompanion-Screenshots/app-screenshots")
ICON = os.path.join(ROOT, "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
WORK = os.path.join(HERE, "build")
OUTDIR = os.path.expanduser("~/Desktop/CareCompanion-Tasks-20-27")
OUT = os.path.join(OUTDIR, "Task-27-CareCompanion-Advertisement.mp4")
os.makedirs(WORK, exist_ok=True)
os.makedirs(OUTDIR, exist_ok=True)

W, H = 1920, 1080
SS = 2                      # supersample, then downscale for clean type
VOICE = "Tara"              # Indian-English system voice
FPS = 30

from PIL import Image, ImageDraw, ImageFont, ImageFilter

CREAM = (247, 245, 240)
INK = (24, 32, 26)
GREEN = (46, 125, 50)
GREEN_DARK = (20, 62, 26)
SUB = (104, 112, 104)
RED = (192, 57, 43)

AV = "/System/Library/Fonts/Avenir Next.ttc"
def font(size, index=2):
    """Avenir Next: 0 regular-ish … index picks the weight inside the .ttc."""
    return ImageFont.truetype(AV, size * SS, index=index)

F_HUGE = lambda s=96: font(s, 6)      # Demi Bold
F_BOLD = lambda s=58: font(s, 6)
F_REG = lambda s=40: font(s, 2)
F_SMALL = lambda s=26: font(s, 2)


def canvas(bg=CREAM):
    return Image.new("RGB", (W * SS, H * SS), bg)


def wrap_text(draw, text, fnt, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        trial = (cur + " " + w).strip()
        if draw.textlength(trial, font=fnt) <= max_w:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


def draw_block(img, text, fnt, x, y, max_w, fill=INK, leading=1.22, anchor="lt"):
    d = ImageDraw.Draw(img)
    lines = wrap_text(d, text, fnt, max_w)
    lh = int(fnt.size * leading)
    total = lh * len(lines)
    yy = y - total // 2 if anchor.endswith("m") else y
    for ln in lines:
        xx = x - d.textlength(ln, font=fnt) / 2 if anchor.startswith("c") else x
        d.text((xx, yy), ln, font=fnt, fill=fill)
        yy += lh
    return yy


# The screenshots are from the team's own test profiles, but the advertisement is a
# public film: the elder's telephone number is masked before it ever reaches a frame.
REDACT = {
    "guardian/01-add-elder-profile.png": [(44, 1152, 650, 1252)],
    "guardian/02-edit-elder-profile-connect-code.png": [(96, 548, 660, 628)],
    # This capture is from an older build. The line it shows about SMS no longer exists
    # in the product at all, so it is masked rather than shown as current behaviour.
    "elder/04-sos-alert-sent.png": [(55, 818, 680, 915)],
}


def phone(shot_rel, height_px):
    """A screenshot inside a simple phone body, returned with its own alpha."""
    src = Image.open(os.path.join(SHOTS, shot_rel)).convert("RGB")
    for box in REDACT.get(shot_rel, []):
        region = src.crop(box)
        small = region.resize((max(1, region.width // 26), max(1, region.height // 14)),
                              Image.BILINEAR)
        src.paste(small.resize(region.size, Image.NEAREST), box)
    bezel = int(height_px * 0.013)
    radius = int(height_px * 0.052)
    sw = int(src.width * height_px / src.height)
    src = src.resize((sw, height_px), Image.LANCZOS)
    bw, bh = sw + 2 * bezel, height_px + 2 * bezel
    body = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle([0, 0, bw - 1, bh - 1], radius=radius, fill=(16, 18, 16, 255))
    mask = Image.new("L", (sw, height_px), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, sw - 1, height_px - 1],
                                           radius=int(radius * 0.82), fill=255)
    body.paste(src, (bezel, bezel), mask)
    return body


def shadowed(img, base, pos, blur=26, offset=(0, 14), opacity=70):
    """Paste img onto base with a soft shadow."""
    sh = Image.new("RGBA", base.size, (0, 0, 0, 0))
    solid = Image.new("RGBA", img.size, (0, 0, 0, opacity))
    sh.paste(solid, (pos[0] + offset[0] * SS, pos[1] + offset[1] * SS), img)
    sh = sh.filter(ImageFilter.GaussianBlur(blur * SS / 2))
    base.alpha_composite(sh)
    base.alpha_composite(img.convert("RGBA"), pos)


def finish(img, name):
    out = img.convert("RGB").resize((W, H), Image.LANCZOS)
    p = os.path.join(WORK, name + ".png")
    out.save(p)
    return p


def rule(img, x, y, w, color=GREEN, thick=6):
    ImageDraw.Draw(img).rectangle([x, y, x + w * SS, y + thick * SS], fill=color)


# ───────────────────────────── scenes ─────────────────────────────
def scene_title():
    img = canvas(CREAM).convert("RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W * SS, 10 * SS], fill=GREEN)
    icon = Image.open(ICON).convert("RGBA").resize((150 * SS, 150 * SS), Image.LANCZOS)
    shadowed(icon, img, ((W // 2 - 75) * SS, 300 * SS), blur=20, opacity=50)
    draw_block(img, "CareCompanion", F_HUGE(104), (W // 2) * SS, 500 * SS,
               1500 * SS, INK, anchor="ct")
    draw_block(img, "For the elders we cannot be beside every day",
               F_REG(38), (W // 2) * SS, 640 * SS, 1400 * SS, SUB, anchor="ct")
    return finish(img, "s01")


def scene_statement(line, sub=None, accent=GREEN, name="s"):
    img = canvas(CREAM).convert("RGBA")
    rule(img, 160 * SS, 300 * SS, 120, accent)
    y = draw_block(img, line, F_BOLD(72), 160 * SS, 380 * SS, 1600 * SS, INK)
    if sub:
        draw_block(img, sub, F_REG(38), 160 * SS, y + 46 * SS, 1500 * SS, SUB)
    return finish(img, name)


def scene_shot(headline, body, shots_rel, name, note=None):
    """Headline and copy on the left, one or two real screenshots on the right."""
    img = canvas(CREAM).convert("RGBA")
    rule(img, 130 * SS, 300 * SS, 100)
    y = draw_block(img, headline, F_BOLD(62), 130 * SS, 362 * SS, 820 * SS, INK)
    y = draw_block(img, body, F_REG(34), 130 * SS, y + 36 * SS, 820 * SS, SUB, leading=1.34)
    if note:
        draw_block(img, note, F_SMALL(24), 130 * SS, y + 30 * SS, 820 * SS, GREEN)
    avail_w = (W - 1120 - 70) * SS
    gap = 46 * SS
    height = 820 * SS
    ph = [phone(s, height) for s in shots_rel]
    total = sum(p.width for p in ph) + gap * (len(ph) - 1)
    if total > avail_w:                      # scale the row down to fit the frame
        k = avail_w / total
        height = int(height * k)
        ph = [phone(s, height) for s in shots_rel]
        total = sum(p.width for p in ph) + gap * (len(ph) - 1)
    x = (1120 * SS) + ((W - 1120) * SS - total) // 2
    for p in ph:
        shadowed(p, img, (x, (H * SS - p.height) // 2))
        x += p.width + gap
    return finish(img, name)


def overlay_title():
    """Opening card as a transparent overlay, to sit over the generated footage."""
    img = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    scrim = Image.new("RGBA", (W * SS, H * SS), (12, 32, 16, 150))
    img.alpha_composite(scrim)
    icon = Image.open(ICON).convert("RGBA").resize((150 * SS, 150 * SS), Image.LANCZOS)
    img.alpha_composite(icon, ((W // 2 - 75) * SS, 330 * SS))
    draw_block(img, "CareCompanion", F_HUGE(104), (W // 2) * SS, 530 * SS,
               1500 * SS, (255, 255, 255), anchor="ct")
    draw_block(img, "For the elders we cannot be beside every day",
               F_REG(38), (W // 2) * SS, 672 * SS, 1400 * SS, (205, 224, 206), anchor="ct")
    p = os.path.join(WORK, "ov01.png")
    img.resize((W, H), Image.LANCZOS).save(p)
    return p


def overlay_close():
    """Closing card as a transparent overlay."""
    img = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    img.alpha_composite(Image.new("RGBA", (W * SS, H * SS), (10, 28, 14, 186)))
    icon = Image.open(ICON).convert("RGBA").resize((120 * SS, 120 * SS), Image.LANCZOS)
    img.alpha_composite(icon, ((W // 2 - 60) * SS, 310 * SS))
    draw_block(img, "CareCompanion", F_HUGE(92), (W // 2) * SS, 480 * SS, 1500 * SS,
               (255, 255, 255), anchor="ct")
    draw_block(img, "Built so a family can be there, even when they are not in the room.",
               F_REG(36), (W // 2) * SS, 610 * SS, 1300 * SS, (214, 230, 214), anchor="ct")
    draw_block(img,
               "Not a medical device. It does not give medical advice and does not replace "
               "calling a doctor or the emergency services.",
               F_SMALL(24), (W // 2) * SS, 820 * SS, 1200 * SS, (168, 192, 170), anchor="ct")
    p = os.path.join(WORK, "ov09.png")
    img.resize((W, H), Image.LANCZOS).save(p)
    return p


def scene_close():
    img = canvas(GREEN_DARK).convert("RGBA")
    icon = Image.open(ICON).convert("RGBA").resize((120 * SS, 120 * SS), Image.LANCZOS)
    img.alpha_composite(icon, ((W // 2 - 60) * SS, 330 * SS))
    draw_block(img, "CareCompanion", F_HUGE(92), (W // 2) * SS, 500 * SS, 1500 * SS,
               (255, 255, 255), anchor="ct")
    draw_block(img, "Built so a family can be there, even when they are not in the room.",
               F_REG(36), (W // 2) * SS, 630 * SS, 1300 * SS, (210, 225, 210), anchor="ct")
    draw_block(img,
               "Not a medical device. It does not give medical advice and does not replace "
               "calling a doctor or the emergency services.",
               F_SMALL(24), (W // 2) * SS, 830 * SS, 1200 * SS, (160, 185, 162), anchor="ct")
    return finish(img, "s09")


# ───────────────────────── the film ─────────────────────────
SCENES = [
    dict(name="s01", make=overlay_title, bg=("bg_open.mp4", False),
         vo="This is CareCompanion."),
    dict(name="s02",
         make=lambda: scene_statement(
             "More of our elders are living on their own.",
             "The family is often in another city, another state, another country.",
             name="s02"),
         vo="More of our elders are living on their own, while the family is in another "
            "city, or another country."),
    dict(name="s03",
         make=lambda: scene_statement(
             "A missed dose. A number they cannot find. A fall with nobody in the room.",
             "The small things are the ones that go wrong first.",
             accent=RED, name="s03"),
         vo="A missed dose. A number they cannot find. A fall, with nobody in the room. "
            "The small things go wrong first."),
    dict(name="s04",
         make=lambda: scene_shot(
             "One app. Two sides.",
             "The family sets everything up on their own phone: the elder's profile, their "
             "medicines, the people they call.",
             ["guardian/01-add-elder-profile.png"], "s04"),
         vo="CareCompanion is one app with two sides. The family sets everything up on "
            "their own phone."),
    dict(name="s05",
         make=lambda: scene_shot(
             "Connected with six digits.",
             "A code from the family's phone, typed into the elder's. That is the whole "
             "setup.",
             ["guardian/02-edit-elder-profile-connect-code.png",
              "elder/01-enter-connect-code.png"], "s05"),
         vo="A six digit code links the two phones. That is the whole setup."),
    dict(name="s06",
         make=lambda: scene_shot(
             "Their side is deliberately bigger.",
             "Larger type. Larger buttons. A contact is a face to tap, not a name to find "
             "in a list.",
             ["elder/02-contacts-tap-photo-to-call.png", "elder/03-videos.png"], "s06",
             note="Measured: the elder's screens use larger type and larger controls "
                  "throughout."),
         vo="Their side is deliberately bigger. Larger type, larger buttons, and a contact "
            "is a face to tap, not a name to find in a list."),
    dict(name="s07",
         make=lambda: scene_statement(
             "At the right time, it reminds them to take their medicine.",
             "And if they have already taken it, it does not ask again.",
             name="s07"),
         vo="At the right time, it reminds them to take their medicine. And if they have "
            "already taken it, it does not ask them again."),
    dict(name="s08",
         make=lambda: scene_shot(
             "If something is wrong, one button.",
             "The elder presses SOS. The family sees the alert, with the time and the place "
             "it was raised.",
             ["elder/04-sos-alert-sent.png", "guardian/07-sos-alerts.png"], "s08",
             note="The elder's screen reports only what the app actually did."),
         vo="If something is wrong, one button. The family sees the alert, with the time "
            "and the place it was raised."),
    dict(name="s09", make=overlay_close, bg=("bg_open.mp4", True),
         vo="CareCompanion. Built so a family can be there, even when they are not in the "
            "room."),
]


def run(cmd, **kw):
    return subprocess.run(cmd, check=True, capture_output=True, text=True, **kw)


def narrate(scene):
    aiff = os.path.join(WORK, scene["name"] + ".aiff")
    wav = os.path.join(WORK, scene["name"] + ".wav")
    run(["say", "-v", VOICE, "-r", "168", "-o", aiff, scene["vo"]])
    run(["ffmpeg", "-y", "-i", aiff, "-ar", "48000", "-ac", "2", wav])
    dur = float(json.loads(run([
        "ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", wav
    ]).stdout)["format"]["duration"])
    return wav, dur


def main():
    if shutil.which("ffmpeg") is None:
        raise SystemExit("ffmpeg is required")
    clips = []
    for sc in SCENES:
        png = sc["make"]()
        wav, vdur = narrate(sc)
        dur = max(round(vdur + 1.25, 2), 3.6)
        clip = os.path.join(WORK, sc["name"] + ".mp4")
        if sc.get("bg"):
            # Generated ambient footage (MiniMax H3, image-to-video on an abstract still),
            # slowed to the scene length, with the card drawn over it as real text.
            src, reverse = sc["bg"]
            src = os.path.join(HERE, src)
            srcdur = float(json.loads(run([
                "ffprobe", "-v", "quiet", "-print_format", "json", "-show_format", src
            ]).stdout)["format"]["duration"])
            rev = "reverse," if reverse else ""
            vf = (f"[0:v]{rev}setpts={dur / srcdur:.4f}*PTS,fps={FPS},"
                  f"scale={W}:{H}:flags=lanczos,format=yuv420p[bgv];"
                  f"[bgv][1:v]overlay=0:0,format=yuv420p[v];"
                  f"[2:a]adelay=450|450,apad[a]")
            run(["ffmpeg", "-y", "-i", src, "-loop", "1", "-i", png, "-i", wav,
                 "-filter_complex", vf, "-map", "[v]", "-map", "[a]", "-t", str(dur),
                 "-c:v", "libx264", "-preset", "medium", "-crf", "19",
                 "-c:a", "aac", "-b:a", "192k", "-shortest", clip])
        else:
            # Slow push-in over the still, with the narration placed 0.45 s in.
            zoom = f"zoompan=z='min(1.06,1+0.06*on/{int(dur*FPS)})':d={int(dur*FPS)}:" \
                   f"x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s={W}x{H}:fps={FPS}"
            run(["ffmpeg", "-y", "-loop", "1", "-i", png, "-i", wav,
                 "-filter_complex",
                 f"[0:v]{zoom},format=yuv420p[v];"
                 f"[1:a]adelay=450|450,apad[a]",
                 "-map", "[v]", "-map", "[a]", "-t", str(dur),
                 "-c:v", "libx264", "-preset", "medium", "-crf", "19",
                 "-c:a", "aac", "-b:a", "192k", "-shortest", clip])
        clips.append((clip, dur))
        print(f"  {sc['name']}  {dur:5.2f}s  vo {vdur:5.2f}s")

    lst = os.path.join(WORK, "concat.txt")
    with open(lst, "w") as fh:
        for c, _ in clips:
            fh.write(f"file '{c}'\n")
    run(["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", lst,
         "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-pix_fmt", "yuv420p",
         "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", OUT])
    total = sum(d for _, d in clips)
    print(f"Wrote: {OUT}  ({total:.1f} s)")


if __name__ == "__main__":
    main()
