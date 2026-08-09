"""
build_user_manual.py
--------------------
Generates the Care Companion User Manual as a PDF.

Written for families, not engineers: no technical terms, short sentences, and every
instruction phrased as something you can actually do on the phone in front of you.

Requirements:
    pip install reportlab

Usage:
    python build_user_manual.py
    -> writes CareCompanion-User-Manual.pdf next to this script
"""

import os

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate, Frame, KeepTogether, ListFlowable, ListItem, NextPageTemplate,
    PageBreak, PageTemplate, Paragraph, Spacer, Table, TableStyle,
)

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "CareCompanion-User-Manual.pdf")

# ---------- Palette (matches the app) ----------
GREEN       = colors.HexColor("#2E7D32")
GREEN_DARK  = colors.HexColor("#1B5E20")
GREEN_PALE  = colors.HexColor("#EAF6EC")
RED         = colors.HexColor("#D32F2F")
RED_PALE    = colors.HexColor("#FDECEC")
AMBER_PALE  = colors.HexColor("#FFF8E1")
AMBER_INK   = colors.HexColor("#6B4D00")
INK         = colors.HexColor("#1C1C1C")
SUB         = colors.HexColor("#5A5A5A")
RULE        = colors.HexColor("#DDE3DD")

PAGE_W, PAGE_H = A4
MARGIN = 20 * mm

# ---------- Styles ----------
ss = getSampleStyleSheet()


def style(name, **kw):
    base = kw.pop("parent", ss["BodyText"])
    return ParagraphStyle(name, parent=base, **kw)


S = {
    "cover_title": style("cover_title", fontName="Helvetica-Bold", fontSize=40, leading=44,
                         textColor=colors.white, alignment=TA_CENTER),
    "cover_sub": style("cover_sub", fontName="Helvetica", fontSize=14, leading=20,
                       textColor=colors.white, alignment=TA_CENTER),
    "cover_foot": style("cover_foot", fontName="Helvetica", fontSize=10, leading=15,
                        textColor=SUB, alignment=TA_CENTER),
    "part": style("part", fontName="Helvetica-Bold", fontSize=26, leading=30,
                  textColor=GREEN_DARK, spaceAfter=4),
    "part_sub": style("part_sub", fontName="Helvetica", fontSize=12, leading=17,
                      textColor=SUB, spaceAfter=14),
    "h1": style("h1", fontName="Helvetica-Bold", fontSize=17, leading=22,
                textColor=GREEN_DARK, spaceBefore=16, spaceAfter=6),
    "h2": style("h2", fontName="Helvetica-Bold", fontSize=13, leading=18,
                textColor=INK, spaceBefore=12, spaceAfter=4),
    "body": style("body", fontName="Helvetica", fontSize=10.5, leading=16,
                  textColor=INK, spaceAfter=7),
    "lead": style("lead", fontName="Helvetica", fontSize=12, leading=18,
                  textColor=SUB, spaceAfter=10),
    "step": style("step", fontName="Helvetica", fontSize=10.5, leading=16, textColor=INK),
    "callout": style("callout", fontName="Helvetica", fontSize=10, leading=15, textColor=INK),
    "callout_head": style("callout_head", fontName="Helvetica-Bold", fontSize=10,
                          leading=15, textColor=INK),
    "cell": style("cell", fontName="Helvetica", fontSize=9.5, leading=14, textColor=INK),
    "cell_b": style("cell_b", fontName="Helvetica-Bold", fontSize=9.5, leading=14, textColor=INK),
    "toc": style("toc", fontName="Helvetica", fontSize=11, leading=19, textColor=INK),
}


# ---------- Building blocks ----------
def para(text, s="body"):
    return Paragraph(text, S[s])


def steps(items):
    """A numbered list of instructions."""
    return ListFlowable(
        [ListItem(Paragraph(t, S["step"]), leftIndent=6) for t in items],
        bulletType="1", bulletFontName="Helvetica-Bold", bulletFontSize=10.5,
        leftIndent=16, bulletOffsetY=0, spaceAfter=9,
    )


def bullets(items):
    return ListFlowable(
        [ListItem(Paragraph(t, S["step"]), leftIndent=6) for t in items],
        bulletType="bullet", bulletFontName="Helvetica", bulletFontSize=9,
        leftIndent=16, spaceAfter=9,
    )


def callout(title, text, tone="tip"):
    """A shaded box. tone: tip (green), warn (amber), danger (red)."""
    bg, ink = {
        "tip": (GREEN_PALE, GREEN_DARK),
        "warn": (AMBER_PALE, AMBER_INK),
        "danger": (RED_PALE, RED),
    }[tone]
    head = Paragraph(f'<font color="{ink.hexval()}"><b>{title}</b></font>', S["callout_head"])
    body = Paragraph(text, S["callout"])
    t = Table([[head], [body]], colWidths=[PAGE_W - 2 * MARGIN])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), bg),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
        ("RIGHTPADDING", (0, 0), (-1, -1), 10),
        ("TOPPADDING", (0, 0), (0, 0), 9),
        ("BOTTOMPADDING", (0, -1), (-1, -1), 9),
        ("TOPPADDING", (0, 1), (-1, -1), 2),
        ("LINEBEFORE", (0, 0), (0, -1), 3, ink),
    ]))
    return KeepTogether([Spacer(1, 4), t, Spacer(1, 10)])


def table(rows, widths, header=True):
    data = []
    for i, row in enumerate(rows):
        s = "cell_b" if (header and i == 0) else "cell"
        data.append([Paragraph(str(c), S[s]) for c in row])
    t = Table(data, colWidths=widths, repeatRows=1 if header else 0)
    st = [
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
        ("LINEBELOW", (0, 0), (-1, -2), 0.5, RULE),
        ("BOX", (0, 0), (-1, -1), 0.5, RULE),
    ]
    if header:
        st += [("BACKGROUND", (0, 0), (-1, 0), GREEN_PALE),
               ("LINEBELOW", (0, 0), (-1, 0), 0.8, GREEN)]
    t.setStyle(TableStyle(st))
    return KeepTogether([Spacer(1, 2), t, Spacer(1, 12)])


def screen(name, what, *following):
    """A section heading plus its intro, kept with whatever follows so a heading never
    strands alone at the foot of a page."""
    return KeepTogether([Paragraph(name, S["h1"]), Paragraph(what, S["body"]), *following])


# ---------- Page furniture ----------
def cover_page(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(GREEN)
    canvas.rect(0, PAGE_H - 150 * mm, PAGE_W, 150 * mm, stroke=0, fill=1)
    canvas.setFillColor(GREEN_DARK)
    canvas.rect(0, PAGE_H - 150 * mm, PAGE_W, 6 * mm, stroke=0, fill=1)
    canvas.restoreState()


def content_page(canvas, doc):
    canvas.saveState()
    # header rule
    canvas.setStrokeColor(RULE)
    canvas.setLineWidth(0.6)
    canvas.line(MARGIN, PAGE_H - MARGIN + 6 * mm, PAGE_W - MARGIN, PAGE_H - MARGIN + 6 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(SUB)
    canvas.drawString(MARGIN, PAGE_H - MARGIN + 8 * mm, "Care Companion — User Manual")
    # footer
    canvas.line(MARGIN, MARGIN - 6 * mm, PAGE_W - MARGIN, MARGIN - 6 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.drawRightString(PAGE_W - MARGIN, MARGIN - 11 * mm, f"Page {canvas.getPageNumber() - 1}")
    canvas.restoreState()


def build():
    doc = BaseDocTemplate(
        OUT, pagesize=A4,
        leftMargin=MARGIN, rightMargin=MARGIN, topMargin=MARGIN, bottomMargin=MARGIN,
        title="Care Companion — User Manual", author="Care Companion",
    )
    frame = Frame(MARGIN, MARGIN, PAGE_W - 2 * MARGIN, PAGE_H - 2 * MARGIN, id="body")
    doc.addPageTemplates([
        PageTemplate(id="cover", frames=[frame], onPage=cover_page),
        PageTemplate(id="content", frames=[frame], onPage=content_page),
    ])
    doc.build(story())
    print("Wrote:", OUT)


# ---------- The manual ----------
def story():
    F = []
    A = F.append

    # ===================== COVER =====================
    A(Spacer(1, 42 * mm))
    A(para("Care Companion", "cover_title"))
    A(Spacer(1, 6 * mm))
    A(para("User Manual", "cover_sub"))
    A(Spacer(1, 3 * mm))
    A(para("A simple guide for families and their elders", "cover_sub"))
    A(Spacer(1, 78 * mm))
    A(para("Everything this app can do, explained in plain language.<br/>"
           "No technical knowledge needed.", "cover_foot"))
    A(NextPageTemplate("content"))
    A(PageBreak())

    # ===================== WELCOME =====================
    A(para("Welcome", "part"))
    A(para("Care Companion helps a family look after an older parent or relative, "
           "even when you are not in the same house.", "part_sub"))

    A(para("The app works across <b>two phones</b> at the same time:", "body"))
    A(table([
        ["Phone", "Who uses it", "What it looks like"],
        ["<b>Your phone</b><br/>(the Guardian)",
         "The son, daughter or family member doing the organising.",
         "A normal app with a dashboard. You add medicines, contacts and reminders, and you "
         "receive emergency alerts."],
        ["<b>Their phone</b><br/>(the Elder)",
         "The older person being cared for.",
         "Very large buttons, very few of them, and a big red SOS button. Nothing to set up, "
         "nothing to configure, nothing that can be broken by accident."],
    ], [32 * mm, 46 * mm, 92 * mm]))

    A(para("You do all the work on your phone. Their phone simply shows the results: today's "
           "medicines, the people they can call, and a way to shout for help.", "body"))

    A(callout("You will need",
              "Two Android phones, a Google account on each one, and an internet connection "
              "on both. That is all — there is nothing to buy and no account to create.", "tip"))

    A(para("How to read this manual", "h1"))
    A(bullets([
        "<b>Part 1</b> is for you, the family member. Start here — the elder's phone cannot be "
        "set up until you have made their profile.",
        "<b>Part 2</b> is what the elder sees. Read it so you can explain it to them.",
        "<b>Part 3</b> walks through an ordinary day, and what happens in an emergency.",
        "<b>Part 4</b> is what to do when something does not look right.",
    ]))

    A(PageBreak())

    # ===================== PART 1: GUARDIAN =====================
    A(para("Part 1 — Your phone", "part"))
    A(para("Setting everything up and looking after it day to day.", "part_sub"))

    # 1.1 Signing in
    A(screen("1. Signing in",
             "Open Care Companion. The first screen asks <b>“Who is using this phone?”</b>"))
    A(steps([
        "Tap <b>Guardian User</b>.",
        "Tap <b>Sign in with Google</b>.",
        "Choose your Google account from the list that appears.",
    ]))
    A(para("That is the whole sign-in. There is no password to invent and no code sent by SMS. "
           "You stay signed in afterwards — you will not be asked again unless you log out.", "body"))
    A(callout("Choose carefully",
              "Pick the Google account you will keep using. Everything you create is tied to it. "
              "If you sign in with a different account later, you will not see the profiles you "
              "made with the first one.", "warn"))

    # 1.2 Creating the profile
    A(screen("2. Creating the elder's profile",
             "This is the record of the person you are caring for. Everything else hangs off it."))
    A(steps([
        "On the dashboard, tap the <b>+</b> beside the profile circles at the top.",
        "Tap the photo circle to add their picture. This is worth doing — the elder recognises "
        "faces far more easily than names.",
        "Fill in their <b>Full Name</b>, <b>Age</b> and <b>Address</b>.",
        "Enter their <b>Phone Number</b>. This is used for calling them and for emergency "
        "messages — it is <i>not</i> used to sign in.",
        "Tap <b>Save Profile</b>.",
    ]))
    A(para("You can create more than one profile if you look after two people. Their circles "
           "appear side by side at the top of the dashboard, and you tap a circle to switch "
           "between them.", "body"))

    # 1.3 Connecting the elder's phone
    A(screen("3. Connecting the elder's phone",
             "This is the one step people find confusing, so here it is slowly. It happens "
             "<b>once</b>, and never again."))
    A(para("Because signing in now uses a Google account, the app cannot tell on its own which "
           "phone belongs to which profile. So you are given a short code that joins the two "
           "together.", "body"))
    A(steps([
        "Open the elder's profile and scroll to <b>Connect [name]'s phone</b>.",
        "You will see a <b>6-digit code</b> in large green numbers.",
        "Tap <b>Copy</b> or <b>Share</b> to send it to yourself, or simply read it out — "
        "whatever is easiest.",
        "Now pick up the <b>elder's phone</b>. Install Care Companion and open it.",
        "Tap <b>Elder User</b>, then <b>Sign in with Google</b>, and choose their account.",
        "The screen says <b>“Enter your code”</b>. Type the 6 digits and tap <b>Connect</b>.",
    ]))
    A(para("Their phone now shows their home screen. From this moment on they simply open the "
           "app — no code, no sign-in, ever again.", "body"))
    A(callout("About the code",
              "It works <b>once</b> and expires after <b>7 days</b>. If it stops working, open "
              "the profile again and a fresh code will be waiting. This is deliberate: a code "
              "that lived forever would be a way into someone's health information.", "tip"))
    A(callout("Doing this together is easiest",
              "Most families do the whole thing sitting side by side, with the family member "
              "typing the code into the elder's phone themselves. That is completely fine.",
              "tip"))

    # 1.4 Dashboard tour
    A(screen("4. The dashboard",
             "Your home screen. The top shows who you are caring for; the coloured tiles below "
             "are everything you can do."))
    A(table([
        ["Tile", "What it is for"],
        ["Contacts", "The people the elder can call, with their photos."],
        ["Medicines", "Add and manage each medicine."],
        ["Schedule", "Set the days and times a medicine should be taken."],
        ["Reminders", "Other nudges — drink water, go for a walk, check vitals."],
        ["Vitals", "Blood pressure, sugar, temperature and pulse readings."],
        ["Adherence", "Which doses were actually taken, and which were missed."],
        ["Videos", "Shortcuts to YouTube, WhatsApp and similar, for their home screen."],
        ["Wheelchair", "Assistance and service contacts."],
        ["Family", "Let another relative see the profile too."],
        ["SOS Alerts", "Emergency alerts they have raised, and marking them resolved."],
    ], [34 * mm, 136 * mm]))
    A(para("At the very top you will also see a <b>SAFE</b> badge and a bell icon. The bell "
           "carries a red number when there is something new for you to look at. Along the "
           "bottom are <b>Home</b>, <b>Alerts</b> and <b>Settings</b>.", "body"))

    # 1.5 Contacts
    A(screen("5. Contacts",
             "The people the elder can reach with one tap. Add these before anything else — they "
             "matter most in an emergency."))
    A(steps([
        "Open <b>Contacts</b> and tap the green <b>+</b>.",
        "Add a photo. On the elder's phone people appear as <i>faces</i>, not names, so this "
        "is the single most useful thing you can do for them.",
        "Enter the <b>Full Name</b> and <b>Phone Number</b>.",
        "Fill in <b>Relation</b> — Daughter, Son, Doctor, Neighbour.",
        "Turn on <b>Emergency contact</b> if this person should be reachable when SOS is pressed.",
        "Tap <b>Save Contact</b>.",
    ]))
    A(para("<b>Import from Contacts</b> pulls a name and number straight from your phone's "
           "address book, so you do not have to type them.", "body"))
    A(callout("Mark at least one emergency contact",
              "If nobody is marked as an emergency contact, the elder's SOS screen has no one "
              "to offer them to call. Take a moment to check this.", "warn"))

    # 1.6 Medicines
    A(screen("6. Medicines",
             "Each medicine is added once, then given a schedule. They are two separate steps."))
    A(steps([
        "Open <b>Medicines</b> and tap the green <b>+</b>.",
        "Enter the <b>Name</b> — use whatever the elder calls it, not the pharmacy's long name.",
        "Enter the <b>Dosage</b>, such as “1 tablet” or “5 ml”.",
        "Choose the <b>Form</b>: tablet, syrup, and so on.",
        "Choose whether it is taken <b>with water</b> or <b>with milk</b>, and "
        "<b>before</b> or <b>after</b> a meal.",
        "You can add photos of the pill and of the packet, front and back. The pill photo shows "
        "on the elder's screen at the moment they take it, which helps them be sure they have "
        "the right one.",
        "Tap <b>Save Medicine</b>.",
    ]))
    A(para("Back on the Medicines list, each medicine has three actions: <b>Edit</b>, "
           "<b>Schedule</b>, and <b>Remove</b>. The switch on the right pauses a medicine "
           "without deleting it — useful when a course finishes but may start again.", "body"))

    # 1.7 Schedule
    A(screen("7. Schedule — when to take it",
             "Nothing appears on the elder's phone until a medicine has a schedule. This is the "
             "step people most often forget."))
    A(steps([
        "On the Medicines list, tap <b>Schedule</b> under the medicine.",
        "<b>Days</b> — tap the circles to choose days. Green means on. "
        "<b>Every day</b> and <b>Mon–Fri</b> set them in one tap.",
        "<b>Meal</b> — choose Breakfast, Lunch or Dinner, and whether it is before or after "
        "the meal.",
        "<b>Time</b> — pick one of the suggested times, or tap <b>Pick time</b> for any time "
        "you like.",
        "<b>Water</b> — turn on if it should be taken with water.",
        "Tap <b>Save Schedule</b>.",
    ]))
    A(para("You can add several schedules to the same medicine — for example one at 8 in the "
           "morning and one at 9 at night. Just repeat the steps.", "body"))
    A(callout("Check it worked",
              "After saving you are returned to the Medicines list, and the medicine now shows a "
              "small green label such as <b>Breakfast · 08:00</b>. If you do not see that label, "
              "the schedule did not save — try again.", "tip"))

    # 1.8 Reminders
    A(screen("8. Reminders",
             "Nudges that are not medicines: drinking water, going for a walk, checking blood "
             "pressure."))
    A(steps([
        "Open <b>Reminders</b> and tap the green <b>+</b>.",
        "Choose a category — Water, Walk or Vitals — or write your own title.",
        "Choose the <b>times</b> of day. You can set several.",
        "Choose the <b>days</b> of the week.",
        "Save.",
    ]))
    A(para("The elder's phone will show a notification at each time. The categories along the "
           "top of the screen — All, Medicine, Water, Walk, Vitals — filter the list.", "body"))
    A(callout("Medicines are not set up here",
              "Medicine reminders come from the Schedule screen, not this one. The note at the "
              "top of the Reminders screen says the same thing.", "warn"))

    # 1.9 Vitals
    A(screen("9. Vitals",
             "A record of blood pressure, sugar, temperature and pulse over time."))
    A(steps([
        "Open <b>Vitals</b> and tap the green <b>+</b>.",
        "Choose <b>Blood Pressure</b>, <b>Sugar</b>, <b>Temperature</b> or <b>Pulse</b>.",
        "For blood pressure, enter both the upper (systolic) and lower (diastolic) numbers.",
        "For sugar, say whether it was taken fasting or after a meal.",
        "Tap <b>Save reading</b>. The date and time are recorded for you.",
    ]))
    A(para("Each reading is labelled <b>Normal</b>, <b>Borderline</b> or <b>High</b> so you can "
           "see at a glance whether something has changed. <b>Export PDF</b> at the top produces "
           "a file you can send to a doctor or show at an appointment.", "body"))
    A(callout("These labels are not medical advice",
              "They are general guides to help you spot a change worth mentioning. The app never "
              "tells anyone to change a medicine or a dose. Always talk to a doctor.", "danger"))
    A(para("The elder can also record their own readings on their phone, and those appear here "
           "too.", "body"))

    # 1.10 Adherence
    A(screen("10. Adherence",
             "Whether medicines are actually being taken."))
    A(para("Every time the elder answers <b>Taken</b> or <b>Not taken</b> on their phone, it is "
           "recorded here. A dose nobody answered is marked as missed. This is how you notice a "
           "pattern — a dose regularly missed at night, for instance — without having to "
           "telephone and ask every day.", "body"))

    # 1.11 Videos
    A(screen("11. Videos and apps",
             "Big shortcut buttons on the elder's home screen."))
    A(steps([
        "Open <b>Videos</b>.",
        "Tap <b>YouTube</b>, <b>Hotstar</b>, <b>WhatsApp</b> or <b>Prime</b> to add it.",
        "Or paste a link under <b>Custom link</b> — a favourite devotional playlist, a family "
        "photo album — give it a title and save.",
    ]))
    A(para("Each one becomes a large button under <b>Videos</b> on their phone. Custom links "
           "show as a coloured letter tile.", "body"))

    # 1.12 Family
    A(screen("12. Family members",
             "Let another relative see and help with the same profile."))
    A(steps([
        "Open <b>Family</b>.",
        "Choose what they may do: <b>View Only</b> (they can see everything but change nothing) "
        "or <b>Can Edit</b> (they can add and update medicines, contacts and reminders).",
        "Tap <b>Create invite code</b>.",
        "Send them the 6-digit code with <b>Copy</b> or <b>Share</b>.",
        "They install the app, sign in with Google, tap <b>Guardian User</b>, and enter the "
        "code where it says <b>“Invited by family?”</b>",
    ]))
    A(para("They then see the same profile you do. You remain the owner and can remove anyone "
           "at any time from this screen.", "body"))
    A(callout("One code, one person",
              "Each code works once and expires after 7 days. Inviting two relatives means "
              "creating two codes. A code also carries the access level you chose — so if you "
              "change your mind between View Only and Can Edit, create a fresh code.", "tip"))

    # 1.13 SOS alerts
    A(screen("13. SOS alerts",
             "What you receive when the elder presses the big red button."))
    A(para("A notification arrives on your phone straight away, with their name, the time, and "
           "a link to where they were. It arrives even if the app is closed, and it will still "
           "make a sound if you have muted the app's other notifications — an emergency should "
           "never be silenced by a setting.", "body"))
    A(steps([
        "Open <b>SOS Alerts</b> to see the alert, marked <b>ACTIVE</b> in red.",
        "Call them, or use the map link to see where they are.",
        "Once you have reached them and everything is fine, tap <b>Mark Resolved</b>.",
    ]))
    A(para("<b>Settings</b> on that screen lets you write the message that is sent, using "
           "<b>{name}</b> where their name should appear.", "body"))

    A(PageBreak())

    # ===================== PART 2: ELDER =====================
    A(para("Part 2 — Their phone", "part"))
    A(para("What the elder sees. Simple on purpose.", "part_sub"))

    A(para("The elder's side is deliberately plain. There are no menus to get lost in, no "
           "settings that can break anything, and nothing that needs remembering. Six buttons, "
           "all of them large.", "body"))

    A(screen("The home screen",
             "It greets them by name — “Namaste, Kiran Pandey” — and shows six things."))
    A(table([
        ["Button", "What happens"],
        ["<b>SOS</b> (large, red)", "Calls for help. Explained on the next page."],
        ["Medicines", "Today's medicines. Shows a small label such as “1 due today”."],
        ["Contacts", "Family photos. Tap a face to call that person."],
        ["Vitals", "Record a blood pressure or sugar reading."],
        ["Videos", "The shortcuts you added."],
        ["Settings (gear)", "Text size, language, high contrast."],
    ], [42 * mm, 128 * mm]))
    A(para("There is no way to accidentally leave the app or end up somewhere confusing. "
           "Pressing the phone's back button always returns them to this screen — it never "
           "closes the app.", "body"))

    A(screen("Taking medicines",
             "The part of the app they will use most."))
    A(steps([
        "At the scheduled time the phone shows a notification: <b>“Time for Thyroid Medicine”</b>.",
        "They tap it, and the app opens <b>directly</b> on the medicine screen.",
        "They tap <b>Start taking</b>.",
        "One medicine at a time is shown — the name, the picture, and instructions such as "
        "“Breakfast · with water”.",
        "They tap the big green <b>✓ Taken</b>, or the red <b>✗ Not taken</b>.",
        "When they are done, a <b>“Great job!”</b> screen appears.",
    ]))
    A(para("They can also open Medicines themselves at any time without waiting for a "
           "notification. Either way, their answer appears on your phone under Adherence.", "body"))
    A(callout("If they take it early",
              "Tapping Taken cancels that reminder, so the phone will not ask again later for "
              "something already taken.", "tip"))

    A(screen("Calling someone",
             "Faces, not phone numbers."))
    A(steps([
        "They tap <b>Contacts</b>.",
        "They tap the photo of the person they want.",
        "The phone asks <b>“Call Akanksha Pandey?”</b>",
        "They tap <b>Yes, Call</b> — or <b>No</b> if they tapped by mistake.",
    ]))
    A(para("The confirmation step is deliberate: it makes an accidental call almost impossible.",
           "body"))

    A(screen("The SOS button", "The most important button in the app."))
    A(steps([
        "They press the big red <b>SOS</b> button.",
        "A countdown begins — <b>5 seconds</b> — with a large <b>“CANCEL — I'm OK”</b> button.",
        "If they pressed it by mistake, tapping Cancel stops everything. Nothing is sent.",
        "If the countdown finishes, the alert goes to every family member, along with where "
        "they are.",
        "The screen then says <b>“Alert Sent! Your family has been notified”</b>, with a big "
        "green button to call their main contact.",
    ]))
    A(callout("If there is no internet",
              "The screen says plainly that the alert did <b>not</b> go through, and offers two "
              "large buttons: <b>Send text to [name]</b>, which opens their messaging app with "
              "the emergency message and their location already written, and a button to call. "
              "The app never pretends help is coming when it is not.", "danger"))

    A(screen("Recording a reading",
             "The elder can record their own blood pressure or sugar."))
    A(steps([
        "They tap <b>Vitals</b>, then <b>Add reading</b>.",
        "They choose Blood Pressure, Sugar, Temperature or Pulse.",
        "They type the numbers and tap <b>Save reading</b>.",
    ]))
    A(para("The date and time are filled in automatically, and the reading appears on your "
           "phone as well.", "body"))

    A(screen("Settings", "Three things, all designed for comfort."))
    A(table([
        ["Setting", "What it does"],
        ["Text size", "Three sizes. Everything in the app grows, not just one screen."],
        ["Language", "English, Hindi, Marathi or Gujarati. Each is shown in its own script "
                     "on the phone. The whole elder side changes immediately."],
        ["High contrast", "Darkens greys to near-black for easier reading in poor light or "
                          "with weaker eyesight."],
    ], [34 * mm, 136 * mm]))
    A(callout("Worth doing on day one",
              "Set the text size and language <i>with</i> them, and ask whether it is "
              "comfortable. It takes two minutes and makes the difference between an app they "
              "use and one they avoid.", "tip"))

    A(PageBreak())

    # ===================== PART 3: EVERYDAY =====================
    A(para("Part 3 — An ordinary day", "part"))
    A(para("How the two phones work together once everything is set up.", "part_sub"))

    A(para("At medicine time", "h1"))
    A(table([
        ["On their phone", "On your phone"],
        ["A notification appears at the exact time you set.", "Nothing yet."],
        ["They tap it and the medicine screen opens.", ""],
        ["They tap <b>Taken</b>.", "The dose appears under <b>Adherence</b> as taken."],
        ["If they ignore it, nothing else happens on their phone.",
         "The dose is recorded as <b>missed</b>, so you can follow it up."],
    ], [85 * mm, 85 * mm]))

    A(para("When SOS is pressed", "h1"))
    A(table([
        ["On their phone", "On your phone"],
        ["5-second countdown, with a Cancel button.", "Nothing yet — the countdown may be cancelled."],
        ["The alert is sent with their location.",
         "A notification arrives immediately, with their name, the time and a map link."],
        ["“Alert Sent!” and a large button to call family.",
         "The alert shows as <b>ACTIVE</b> under SOS Alerts."],
        ["", "You call them, then tap <b>Mark Resolved</b>."],
    ], [85 * mm, 85 * mm]))

    A(para("Things worth knowing", "h1"))
    A(bullets([
        "<b>Reminders work even if they never open the app.</b> The phone checks through the day "
        "on its own, and reminders survive the phone being switched off and on again.",
        "<b>Their answers are not lost without internet.</b> If they tap Taken while offline, it "
        "is saved on the phone and sent as soon as there is a connection.",
        "<b>You do not need to be signed in for alerts to reach you.</b> They arrive even when "
        "the app is closed.",
        "<b>Emergency alerts cannot be silenced</b> by turning off the app's other notifications.",
    ]))

    A(PageBreak())

    # ===================== PART 4: HELP =====================
    A(para("Part 4 — When something is not right", "part"))
    A(para("The usual causes, and what to do about them.", "part_sub"))

    A(table([
        ["What you see", "What to do"],
        ["<b>The elder's phone still asks for a code</b>",
         "The code has been used already, or more than 7 days have passed. Open their profile on "
         "your phone — a fresh code will be there."],
        ["<b>“This profile is already set up on another phone”</b>",
         "The profile was connected to a different phone. Only that phone can open it. Contact "
         "support if the old phone is gone."],
        ["<b>Nothing appears on the elder's Medicines screen</b>",
         "The medicine has no schedule. Go to Medicines, tap <b>Schedule</b> under it, and set "
         "the days and time. A medicine on its own never appears."],
        ["<b>The reminder did not go off</b>",
         "Check the day is selected in the schedule, and that the medicine's switch is on. Then "
         "check notifications are allowed for Care Companion in the phone's own settings."],
        ["<b>Saving does nothing</b>",
         "Almost always the internet. Check the connection and try again — an error message "
         "should now tell you what went wrong."],
        ["<b>Signing in with Google fails</b>",
         "The phone needs Google Play Services, which almost every Android phone has. Check the "
         "phone is online and that a Google account is added in its settings."],
        ["<b>Text looks faint or hard to read</b>",
         "Turn on <b>High contrast</b> in the elder's Settings, and increase the text size."],
        ["<b>They keep pressing SOS by accident</b>",
         "There is a 5-second countdown with a large Cancel button. Practise it together once, "
         "so they know a mistake is easily undone."],
    ], [50 * mm, 120 * mm]))

    A(para("Your information", "h1"))
    A(para("Care Companion holds health information, and treats it that way.", "body"))
    A(bullets([
        "Only you, the family members you have invited, and the elder can see the profile. "
        "Nobody else can.",
        "Location is recorded <b>only</b> at the moment SOS is pressed. The app never follows "
        "anyone around.",
        "Nothing is sold, and nothing is used for advertising.",
        "Choosing a contact from your address book reads only that one contact. Your address "
        "book is never uploaded.",
        "Deleting a profile deletes its medicines, contacts, readings and alerts.",
    ]))

    A(Spacer(1, 10))
    A(callout("A last word",
              "This app helps a family stay organised. It is not a medical device, it does not "
              "give medical advice, and it should never replace calling a doctor or the "
              "emergency services when something is wrong.", "danger"))

    return F


if __name__ == "__main__":
    build()
