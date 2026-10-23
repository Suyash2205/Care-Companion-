"""
task_docs_common.py
-------------------
Shared layout and data loading for the LY EXCP Task 20-27 submission documents.

Every number these documents print is read at build time from measurements/raw/,
which the Task 21 run produced. Nothing is typed in by hand, so re-running the
measurements and rebuilding keeps the documents and the evidence in step.
"""
import csv, json, os, hashlib, datetime

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate, Frame, Image, KeepTogether, ListFlowable, ListItem,
    NextPageTemplate, PageBreak, PageTemplate, Paragraph, Spacer, Table, TableStyle,
)

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RAW = os.path.join(ROOT, "measurements", "raw")
FIGS = os.path.join(ROOT, "measurements", "figs")
SHOTS = os.path.expanduser("~/Desktop/CareCompanion-Screenshots/app-screenshots")
OUTDIR = os.path.expanduser("~/Desktop/CareCompanion-Tasks-20-27")
os.makedirs(OUTDIR, exist_ok=True)

STUDENT = "Devanshi Pandey"
ROLL = "16014023016"
DEPT = "EXCP"
INSTITUTION = "K. J. Somaiya School of Engineering"
PROJECT = "CareCompanion"
SUBTITLE = "Assistive Companion Mobile Application for Elderly and Disabled Users"
SUBMITTED = "23 October 2026"

# ---------- data ----------
def rows(name):
    with open(os.path.join(RAW, name)) as fh:
        return list(csv.DictReader(fh))

def jload(name):
    with open(os.path.join(RAW, name)) as fh:
        return json.load(fh)

def kv(name):
    return {r["key"]: r["value"] for r in rows(name)}

def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()

# ---------- palette ----------
GREEN = colors.HexColor("#2E7D32")
GREEN_DARK = colors.HexColor("#1B5E20")
GREEN_PALE = colors.HexColor("#EAF6EC")
BLUE = colors.HexColor("#3B6EA5")
RED = colors.HexColor("#C0392B")
RED_PALE = colors.HexColor("#FDECEC")
AMBER_PALE = colors.HexColor("#FFF8E1")
INK = colors.HexColor("#1C1C1C")
SUB = colors.HexColor("#5A5A5A")
RULE = colors.HexColor("#D8DCD8")

PAGE_W, PAGE_H = A4
MARGIN = 20 * mm
CONTENT_W = PAGE_W - 2 * MARGIN

ss = getSampleStyleSheet()
def _s(name, **kw):
    return ParagraphStyle(name, parent=kw.pop("parent", ss["BodyText"]), **kw)

S = {
    "cover_kicker": _s("ck", fontName="Helvetica-Bold", fontSize=11, leading=15,
                       textColor=GREEN_DARK, alignment=TA_CENTER, spaceAfter=4),
    "cover_title": _s("ct", fontName="Helvetica-Bold", fontSize=25, leading=30,
                      textColor=INK, alignment=TA_CENTER, spaceAfter=6),
    "cover_sub": _s("cs", fontName="Helvetica", fontSize=13, leading=18,
                    textColor=SUB, alignment=TA_CENTER, spaceAfter=3),
    "h1": _s("h1", fontName="Helvetica-Bold", fontSize=14, leading=18,
             textColor=GREEN_DARK, spaceBefore=15, spaceAfter=6),
    "h2": _s("h2", fontName="Helvetica-Bold", fontSize=11, leading=15,
             textColor=INK, spaceBefore=11, spaceAfter=4),
    "body": _s("body", fontName="Helvetica", fontSize=9.6, leading=14.2,
               textColor=INK, alignment=TA_JUSTIFY, spaceAfter=6),
    "lead": _s("lead", fontName="Helvetica-Oblique", fontSize=10.2, leading=15,
               textColor=SUB, spaceAfter=9),
    "cell": _s("cell", fontName="Helvetica", fontSize=8.4, leading=12, textColor=INK),
    "cellb": _s("cellb", fontName="Helvetica-Bold", fontSize=8.4, leading=12, textColor=INK),
    "cellr": _s("cellr", fontName="Helvetica", fontSize=8.4, leading=12, textColor=INK,
                alignment=2),
    "caption": _s("cap", fontName="Helvetica", fontSize=8.4, leading=11.6,
                  textColor=SUB, alignment=TA_CENTER, spaceAfter=2),
    "tcap": _s("tcap", fontName="Helvetica-Bold", fontSize=8.8, leading=12,
               textColor=INK, spaceBefore=4, spaceAfter=3),
    "step": _s("step", fontName="Helvetica", fontSize=9.6, leading=14.2, textColor=INK),
    "mono": _s("mono", fontName="Courier", fontSize=7.8, leading=10.4, textColor=INK),
    "callout": _s("co", fontName="Helvetica", fontSize=9.2, leading=13.4, textColor=INK),
    "cohead": _s("coh", fontName="Helvetica-Bold", fontSize=9.2, leading=13.4, textColor=INK),
}

def P(t, s="body"): return Paragraph(t, S[s])

def bullets(items, s="body"):
    return ListFlowable([ListItem(Paragraph(i, S[s]), leftIndent=12) for i in items],
                        bulletType="bullet", start="•", leftIndent=14,
                        bulletFontSize=8, bulletOffsetY=-1)

def steps(items):
    return ListFlowable([ListItem(Paragraph(i, S["step"]), leftIndent=14) for i in items],
                        bulletType="1", leftIndent=16, bulletFormat="%s.")

def callout(title, text, tone="tip"):
    bg = {"tip": GREEN_PALE, "warn": AMBER_PALE, "danger": RED_PALE}[tone]
    edge = {"tip": GREEN, "warn": colors.HexColor("#B07D00"), "danger": RED}[tone]
    t = Table([[Paragraph(title, S["cohead"])], [Paragraph(text, S["callout"])]],
              colWidths=[CONTENT_W])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), bg),
        ("LINEBEFORE", (0, 0), (0, -1), 2.2, edge),
        ("LEFTPADDING", (0, 0), (-1, -1), 8), ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, 0), 7), ("BOTTOMPADDING", (0, -1), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, 0), 2), ("TOPPADDING", (0, -1), (-1, -1), 0),
    ]))
    return KeepTogether([Spacer(1, 4), t, Spacer(1, 9)])

_TBL = [0]
def table(title, data, widths, right=(), caption_first=True, fontsize=None):
    """A numbered table. data[0] is the header row."""
    _TBL[0] += 1
    body = []
    for i, row in enumerate(data):
        cells = []
        for j, c in enumerate(row):
            st = "cellb" if i == 0 else ("cellr" if j in right else "cell")
            cells.append(Paragraph(str(c), S[st]))
        body.append(cells)
    t = Table(body, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("BACKGROUND", (0, 0), (-1, 0), GREEN_PALE),
        ("LINEBELOW", (0, 0), (-1, 0), 0.8, GREEN),
        ("LINEBELOW", (0, 1), (-1, -2), 0.4, RULE),
        ("BOX", (0, 0), (-1, -1), 0.5, RULE),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    cap = Paragraph(f"Table {_TBL[0]}. {title}", S["tcap"])
    return KeepTogether([Spacer(1, 5), cap, t, Spacer(1, 10)])

_FIG = [0]
def figure(path, caption, width_mm=150):
    from PIL import Image as PILImage
    w, h = PILImage.open(path).size
    _FIG[0] += 1
    dw = width_mm * mm
    img = Image(path, width=dw, height=dw * h / w)
    cap = Paragraph(f"<b>Figure {_FIG[0]}.</b> {caption}", S["caption"])
    t = Table([[img], [cap]], colWidths=[CONTENT_W])
    t.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"),
                           ("TOPPADDING", (0, 1), (-1, 1), 5)]))
    return KeepTogether([Spacer(1, 5), t, Spacer(1, 11)])

def shots(pairs, width_mm=44):
    """Two or three phone screenshots side by side, each labelled."""
    cells, caps = [], []
    for rel, cap in pairs:
        from PIL import Image as PILImage
        path = os.path.join(SHOTS, rel)
        w, h = PILImage.open(path).size
        dw = width_mm * mm
        _FIG[0] += 1
        cells.append(Image(path, width=dw, height=dw * h / w))
        caps.append(Paragraph(f"<b>Figure {_FIG[0]}.</b> {cap}", S["caption"]))
    t = Table([cells, caps], colWidths=[CONTENT_W / len(cells)] * len(cells))
    t.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"),
                           ("VALIGN", (0, 1), (-1, 1), "TOP"),
                           ("TOPPADDING", (0, 1), (-1, 1), 5)]))
    return KeepTogether([Spacer(1, 5), t, Spacer(1, 11)])

def reset_counters():
    _TBL[0] = 0
    _FIG[0] = 0

# ---------- document ----------
def build(filename, task_no, task_title, story_fn, doc_kind):
    out = os.path.join(OUTDIR, filename)
    reset_counters()

    def cover(canvas, doc):
        canvas.saveState()
        canvas.setFillColor(GREEN)
        canvas.rect(0, PAGE_H - 12 * mm, PAGE_W, 12 * mm, stroke=0, fill=1)
        canvas.setFillColor(GREEN_DARK)
        canvas.rect(0, 0, PAGE_W, 8 * mm, stroke=0, fill=1)
        canvas.restoreState()

    def page(canvas, doc):
        canvas.saveState()
        canvas.setStrokeColor(RULE); canvas.setLineWidth(0.6)
        canvas.line(MARGIN, PAGE_H - MARGIN + 6 * mm, PAGE_W - MARGIN, PAGE_H - MARGIN + 6 * mm)
        canvas.setFont("Helvetica", 7.6); canvas.setFillColor(SUB)
        canvas.drawString(MARGIN, PAGE_H - MARGIN + 8 * mm,
                          f"{PROJECT} — Task {task_no}: {task_title}")
        canvas.drawRightString(PAGE_W - MARGIN, PAGE_H - MARGIN + 8 * mm,
                               f"{STUDENT} · {ROLL}")
        canvas.line(MARGIN, MARGIN - 7 * mm, PAGE_W - MARGIN, MARGIN - 7 * mm)
        canvas.drawRightString(PAGE_W - MARGIN, MARGIN - 12 * mm,
                               f"Page {canvas.getPageNumber() - 1}")
        canvas.drawString(MARGIN, MARGIN - 12 * mm, f"Submitted {SUBMITTED}")
        canvas.restoreState()

    doc = BaseDocTemplate(out, pagesize=A4, leftMargin=MARGIN, rightMargin=MARGIN,
                          topMargin=MARGIN, bottomMargin=MARGIN,
                          title=f"{PROJECT} — Task {task_no}: {task_title}", author=STUDENT)
    frame = Frame(MARGIN, MARGIN, CONTENT_W, PAGE_H - 2 * MARGIN, id="body")
    doc.addPageTemplates([PageTemplate(id="cover", frames=[frame], onPage=cover),
                          PageTemplate(id="content", frames=[frame], onPage=page)])

    F = []
    F.append(Spacer(1, 28 * mm))
    F.append(P(f"TASK {task_no}", "cover_kicker"))
    F.append(P(task_title.upper(), "cover_title"))
    F.append(Spacer(1, 3 * mm))
    F.append(P(PROJECT, "cover_sub"))
    F.append(P(SUBTITLE, "cover_sub"))
    F.append(Spacer(1, 16 * mm))
    meta = [["Student name", STUDENT], ["Roll number", ROLL], ["Department", DEPT],
            ["Institution", INSTITUTION], ["Document type", doc_kind],
            ["Submission date", SUBMITTED]]
    t = Table([[Paragraph(a, S["cellb"]), Paragraph(b, S["cell"])] for a, b in meta],
              colWidths=[45 * mm, 85 * mm], hAlign="CENTER")
    t.setStyle(TableStyle([
        ("LINEBELOW", (0, 0), (-1, -2), 0.4, RULE),
        ("BOX", (0, 0), (-1, -1), 0.5, RULE),
        ("BACKGROUND", (0, 0), (0, -1), GREEN_PALE),
        ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    F.append(t)
    F.append(NextPageTemplate("content"))
    F.append(PageBreak())
    F.extend(story_fn())
    doc.build(F)
    print("Wrote:", out)
    return out
