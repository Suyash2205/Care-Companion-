"""
build_task28_report.py
----------------------
Task 28 — the signed final technical report.

    python docs/build_task28_report.py

Renders docs/task28_blocks.py twice, from the same block list:

    ~/Desktop/CareCompanion-Tasks-28-31/Task-28-Final-Technical-Report.pdf
    ~/Desktop/CareCompanion-Tasks-28-31/Task-28-Final-Technical-Report.docx

The .docx is the editable source file the task asks for. Both come from one
content module, so they cannot drift apart.

If a signature image exists at ~/Desktop/signature.png it is placed on the
declaration page for the submitting member; otherwise a ruled signature line is
drawn for signing by hand.
"""
import os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import task28_blocks as C

OUTDIR = os.path.expanduser("~/Desktop/CareCompanion-Tasks-28-31")
os.makedirs(OUTDIR, exist_ok=True)

TITLE = "CareCompanion — Final Technical Report"
SUBTITLE = "Assistive Companion Mobile Application for Elderly and Disabled Users"
COURSE = "Last Year Extra Curricular Project (EXCP)"
DEPT = "EXCP"
INSTITUTION = "K. J. Somaiya School of Engineering"
SUBMITTED = "30 October 2026"
SIGNATURE_IMG = os.path.expanduser("~/Desktop/signature.png")

# ───────────────────────────── shared markup ─────────────────────────────
TAG = re.compile(r"</?(b|i|c)>|<br\s*/?>")

def plain(t):
    return TAG.sub("", str(t))

def runs(t):
    """Split inline markup into (text, bold, italic, code) runs."""
    out, bold, ital, code, buf = [], False, False, False, ""
    i = 0
    s = str(t)
    while i < len(s):
        m = TAG.match(s, i)
        if m:
            if buf:
                out.append((buf, bold, ital, code)); buf = ""
            tag, closing = m.group(1), m.group(0).startswith("</")
            if tag is None:
                buf += "\n"
            elif tag == "b": bold = not closing
            elif tag == "i": ital = not closing
            else: code = not closing
            i = m.end()
        else:
            buf += s[i]; i += 1
    if buf:
        out.append((buf, bold, ital, code))
    return out


# ═══════════════════════════════ PDF ═══════════════════════════════
def build_pdf(blocks, out):
    from reportlab.lib import colors
    from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
    from reportlab.lib.units import mm
    from reportlab.platypus import (
        BaseDocTemplate, Frame, Image, KeepTogether, ListFlowable, ListItem,
        NextPageTemplate, PageBreak, PageTemplate, Paragraph, Spacer, Table, TableStyle)

    GREEN = colors.HexColor("#2E7D32"); GREEN_DARK = colors.HexColor("#1B5E20")
    GREEN_PALE = colors.HexColor("#EAF6EC"); AMBER_PALE = colors.HexColor("#FFF8E1")
    RED_PALE = colors.HexColor("#FDECEC"); RED = colors.HexColor("#C0392B")
    INK = colors.HexColor("#1C1C1C"); SUB = colors.HexColor("#5A5A5A")
    RULE = colors.HexColor("#D8DCD8")
    PAGE_W, PAGE_H = A4
    MARGIN = 19 * mm
    CW = PAGE_W - 2 * MARGIN

    ss = getSampleStyleSheet()
    def _s(name, **kw):
        return ParagraphStyle(name, parent=ss["BodyText"], **kw)
    S = {
        "ck": _s("ck", fontName="Helvetica-Bold", fontSize=10.5, leading=14,
                 textColor=GREEN_DARK, alignment=TA_CENTER, spaceAfter=4),
        "ct": _s("ct", fontName="Helvetica-Bold", fontSize=24, leading=29, textColor=INK,
                 alignment=TA_CENTER, spaceAfter=6),
        "cs": _s("cs", fontName="Helvetica", fontSize=12.5, leading=17, textColor=SUB,
                 alignment=TA_CENTER, spaceAfter=3),
        "h1": _s("h1", fontName="Helvetica-Bold", fontSize=15, leading=19,
                 textColor=GREEN_DARK, spaceBefore=16, spaceAfter=7),
        "h2": _s("h2", fontName="Helvetica-Bold", fontSize=11, leading=15, textColor=INK,
                 spaceBefore=11, spaceAfter=4),
        "body": _s("body", fontName="Helvetica", fontSize=9.5, leading=14, textColor=INK,
                   alignment=TA_JUSTIFY, spaceAfter=6),
        "lead": _s("lead", fontName="Helvetica-Oblique", fontSize=10, leading=15,
                   textColor=SUB, spaceAfter=9),
        "cell": _s("cell", fontName="Helvetica", fontSize=7.5, leading=10.6, textColor=INK),
        "cellb": _s("cellb", fontName="Helvetica-Bold", fontSize=7.5, leading=10.6,
                    textColor=INK),
        "cellr": _s("cellr", fontName="Helvetica", fontSize=7.5, leading=10.6, textColor=INK,
                    alignment=2),
        "cap": _s("cap", fontName="Helvetica", fontSize=8.2, leading=11.4, textColor=SUB,
                  alignment=TA_CENTER, spaceAfter=2),
        "tcap": _s("tcap", fontName="Helvetica-Bold", fontSize=8.6, leading=12, textColor=INK,
                   spaceBefore=4, spaceAfter=3),
        "co": _s("co", fontName="Helvetica", fontSize=9, leading=13, textColor=INK),
        "coh": _s("coh", fontName="Helvetica-Bold", fontSize=9, leading=13, textColor=INK),
        "ref": _s("ref", fontName="Helvetica", fontSize=8.3, leading=11.6, textColor=INK,
                  alignment=TA_JUSTIFY, spaceAfter=3.5),
    }

    def rl(t):
        return str(t).replace("<c>", '<font face="Courier">').replace("</c>", "</font>")

    def P(t, s="body"):
        return Paragraph(rl(t), S[s])

    counters = {"tbl": 0, "fig": 0}
    F = []

    # ── cover ──
    F.append(Spacer(1, 26 * mm))
    F.append(P("FINAL TECHNICAL REPORT · TASK 28", "ck"))
    F.append(P("CARECOMPANION", "ct"))
    F.append(P(SUBTITLE, "cs"))
    F.append(Spacer(1, 10 * mm))
    F.append(P(COURSE, "cs"))
    F.append(P(INSTITUTION, "cs"))
    F.append(Spacer(1, 14 * mm))
    meta = [["Project", "CareCompanion"],
            ["Document", "Final technical report (Task 28)"],
            ["Submitted by", C.SUBMITTING_MEMBER],
            ["Group members", "; ".join(m[0] for m in C.MEMBERS)],
            ["Department", DEPT],
            ["Institution", INSTITUTION],
            ["Source state measured",
             f"commit {C.ENV['git_commit']}, branch {C.ENV['git_branch']}"],
            ["Measurements taken", C.ENV["date_local"]],
            ["Submission date", SUBMITTED]]
    t = Table([[Paragraph(a, S["cellb"]), Paragraph(rl(b), S["cell"])] for a, b in meta],
              colWidths=[42 * mm, 98 * mm], hAlign="CENTER")
    t.setStyle(TableStyle([
        ("LINEBELOW", (0, 0), (-1, -2), 0.4, RULE), ("BOX", (0, 0), (-1, -1), 0.5, RULE),
        ("BACKGROUND", (0, 0), (0, -1), GREEN_PALE), ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    F.append(t)
    F.append(NextPageTemplate("content"))
    F.append(PageBreak())

    # ── body ──
    for blk in blocks:
        kind = blk[0]
        if kind in ("h1", "h2", "p", "lead"):
            F.append(P(blk[1], "body" if kind == "p" else kind))
        elif kind == "bullets":
            F.append(ListFlowable(
                [ListItem(P(i), leftIndent=12) for i in blk[1]],
                bulletType="bullet", start="•", leftIndent=14, bulletFontSize=8,
                bulletOffsetY=-1))
            F.append(Spacer(1, 5))
        elif kind == "steps":
            F.append(ListFlowable(
                [ListItem(P(i), leftIndent=14) for i in blk[1]],
                bulletType="1", leftIndent=16, bulletFormat="%s."))
            F.append(Spacer(1, 5))
        elif kind == "callout":
            _, title, text, tone = blk
            bg = {"tip": GREEN_PALE, "warn": AMBER_PALE, "danger": RED_PALE}[tone]
            edge = {"tip": GREEN, "warn": colors.HexColor("#B07D00"), "danger": RED}[tone]
            ct = Table([[P(title, "coh")], [P(text, "co")]], colWidths=[CW])
            ct.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, -1), bg),
                ("LINEBEFORE", (0, 0), (0, -1), 2.2, edge),
                ("LEFTPADDING", (0, 0), (-1, -1), 8), ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, 0), 7), ("BOTTOMPADDING", (0, -1), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, 0), 2), ("TOPPADDING", (0, -1), (-1, -1), 0)]))
            F.append(KeepTogether([Spacer(1, 4), ct, Spacer(1, 9)]))
        elif kind == "table":
            _, title, data, widths, right = blk
            counters["tbl"] += 1
            body = []
            for i, row in enumerate(data):
                cells = []
                for j, cv in enumerate(row):
                    st = "cellb" if i == 0 else ("cellr" if j in right else "cell")
                    cells.append(Paragraph(rl(cv), S[st]))
                body.append(cells)
            tt = Table(body, colWidths=[w * mm for w in widths], repeatRows=1)
            tt.setStyle(TableStyle([
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("BACKGROUND", (0, 0), (-1, 0), GREEN_PALE),
                ("LINEBELOW", (0, 0), (-1, 0), 0.8, GREEN),
                ("LINEBELOW", (0, 1), (-1, -2), 0.35, RULE),
                ("BOX", (0, 0), (-1, -1), 0.5, RULE),
                ("LEFTPADDING", (0, 0), (-1, -1), 4), ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                ("TOPPADDING", (0, 0), (-1, -1), 3.5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3.5)]))
            F.append(Spacer(1, 5))
            if title:
                F.append(Paragraph(f"Table {counters['tbl']}. {rl(title)}", S["tcap"]))
            F.append(tt)
            F.append(Spacer(1, 10))
        elif kind == "figure":
            _, path, caption, wmm = blk
            from PIL import Image as PILImage
            w, h = PILImage.open(path).size
            counters["fig"] += 1
            dw = wmm * mm
            img = Image(path, width=dw, height=dw * h / w)
            cap = Paragraph(f"<b>Figure {counters['fig']}.</b> {rl(caption)}", S["cap"])
            ft = Table([[img], [cap]], colWidths=[CW])
            ft.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"),
                                    ("TOPPADDING", (0, 1), (-1, 1), 5)]))
            F.append(KeepTogether([Spacer(1, 5), ft, Spacer(1, 11)]))
        elif kind == "numrefs":
            start = blk[3] if len(blk) > 3 else 1
            for i, r in enumerate(blk[2], start=start):
                F.append(Paragraph(f"[{i}]&nbsp;&nbsp;{rl(r)}", S["ref"]))
            F.append(Spacer(1, 6))
        elif kind == "pagebreak":
            F.append(PageBreak())

    def cover(canvas, doc):
        canvas.saveState()
        canvas.setFillColor(GREEN); canvas.rect(0, PAGE_H - 12 * mm, PAGE_W, 12 * mm, 0, 1)
        canvas.setFillColor(GREEN_DARK); canvas.rect(0, 0, PAGE_W, 8 * mm, 0, 1)
        canvas.restoreState()

    def page(canvas, doc):
        canvas.saveState()
        canvas.setStrokeColor(RULE); canvas.setLineWidth(0.6)
        canvas.line(MARGIN, PAGE_H - MARGIN + 6 * mm, PAGE_W - MARGIN, PAGE_H - MARGIN + 6 * mm)
        canvas.setFont("Helvetica", 7.4); canvas.setFillColor(SUB)
        canvas.drawString(MARGIN, PAGE_H - MARGIN + 8 * mm,
                          "CareCompanion — Task 28: Final Technical Report")
        canvas.drawRightString(PAGE_W - MARGIN, PAGE_H - MARGIN + 8 * mm, C.SUBMITTING_MEMBER)
        canvas.line(MARGIN, MARGIN - 7 * mm, PAGE_W - MARGIN, MARGIN - 7 * mm)
        canvas.drawString(MARGIN, MARGIN - 12 * mm, f"Submitted {SUBMITTED}")
        canvas.drawRightString(PAGE_W - MARGIN, MARGIN - 12 * mm,
                               f"Page {canvas.getPageNumber() - 1}")
        canvas.restoreState()

    doc = BaseDocTemplate(out, pagesize=A4, leftMargin=MARGIN, rightMargin=MARGIN,
                          topMargin=MARGIN, bottomMargin=MARGIN, title=TITLE,
                          author=C.SUBMITTING_MEMBER)
    frame = Frame(MARGIN, MARGIN, CW, PAGE_H - 2 * MARGIN, id="body")
    doc.addPageTemplates([PageTemplate(id="cover", frames=[frame], onPage=cover),
                          PageTemplate(id="content", frames=[frame], onPage=page)])
    doc.build(F)
    return out, counters


# ═══════════════════════════════ DOCX ═══════════════════════════════
def build_docx(blocks, out):
    from docx import Document
    from docx.shared import Pt, Mm, RGBColor
    from docx.enum.text import WD_ALIGN_PARAGRAPH
    from docx.enum.table import WD_TABLE_ALIGNMENT
    from docx.oxml.ns import qn
    from docx.oxml import OxmlElement

    d = Document()
    sec = d.sections[0]
    sec.page_width, sec.page_height = Mm(210), Mm(297)
    for a in ("left_margin", "right_margin", "top_margin", "bottom_margin"):
        setattr(sec, a, Mm(19))

    base = d.styles["Normal"]
    base.font.name = "Calibri"
    base.font.size = Pt(10.5)
    base.paragraph_format.space_after = Pt(6)

    GREEN = RGBColor(0x1B, 0x5E, 0x20)

    def write(par, text, size=None, color=None):
        for txt, b, i, code in runs(text):
            r = par.add_run(txt)
            r.bold, r.italic = b, i
            if code:
                r.font.name = "Consolas"
                r._element.rPr.rFonts.set(qn("w:eastAsia"), "Consolas")
            if size:
                r.font.size = Pt(size)
            if color:
                r.font.color.rgb = color
        return par

    def shade(cell, hexcolor):
        el = OxmlElement("w:shd")
        el.set(qn("w:val"), "clear"); el.set(qn("w:fill"), hexcolor)
        cell._tc.get_or_add_tcPr().append(el)

    # cover
    p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    write(p, "FINAL TECHNICAL REPORT · TASK 28", 11, GREEN)
    p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    write(p, "<b>CARECOMPANION</b>", 26)
    p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    write(p, SUBTITLE, 13)
    p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    write(p, f"{COURSE}\n{INSTITUTION}", 11)
    meta = [("Project", "CareCompanion"),
            ("Document", "Final technical report (Task 28)"),
            ("Submitted by", C.SUBMITTING_MEMBER),
            ("Group members", "; ".join(m[0] for m in C.MEMBERS)),
            ("Department", DEPT), ("Institution", INSTITUTION),
            ("Source state measured",
             f"commit {C.ENV['git_commit']}, branch {C.ENV['git_branch']}"),
            ("Measurements taken", C.ENV["date_local"]),
            ("Submission date", SUBMITTED)]
    tb = d.add_table(rows=0, cols=2); tb.style = "Table Grid"
    tb.alignment = WD_TABLE_ALIGNMENT.CENTER
    for k, v in meta:
        cells = tb.add_row().cells
        write(cells[0].paragraphs[0], f"<b>{k}</b>", 9)
        shade(cells[0], "EAF6EC")
        write(cells[1].paragraphs[0], v, 9)
    d.add_page_break()

    counters = {"tbl": 0, "fig": 0}
    for blk in blocks:
        kind = blk[0]
        if kind == "h1":
            h = d.add_heading(level=1)
            write(h, plain(blk[1]))
            for r in h.runs:
                r.font.color.rgb = GREEN; r.font.size = Pt(15)
        elif kind == "h2":
            h = d.add_heading(level=2)
            write(h, plain(blk[1]))
            for r in h.runs:
                r.font.color.rgb = RGBColor(0x1C, 0x1C, 0x1C); r.font.size = Pt(11.5)
        elif kind == "p":
            p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            write(p, blk[1])
        elif kind == "lead":
            p = d.add_paragraph(); write(p, f"<i>{plain(blk[1])}</i>")
        elif kind == "bullets":
            for item in blk[1]:
                write(d.add_paragraph(style="List Bullet"), item)
        elif kind == "steps":
            for item in blk[1]:
                write(d.add_paragraph(style="List Number"), item)
        elif kind == "callout":
            _, title, text, tone = blk
            fill = {"tip": "EAF6EC", "warn": "FFF8E1", "danger": "FDECEC"}[tone]
            ct = d.add_table(rows=1, cols=1); ct.style = "Table Grid"
            cell = ct.rows[0].cells[0]; shade(cell, fill)
            write(cell.paragraphs[0], f"<b>{plain(title)}</b>", 9.5)
            write(cell.add_paragraph(), text, 9.5)
            d.add_paragraph()
        elif kind == "table":
            _, title, data, widths, right = blk
            counters["tbl"] += 1
            if title:
                write(d.add_paragraph(), f"<b>Table {counters['tbl']}.</b> {plain(title)}", 9)
            tb = d.add_table(rows=0, cols=len(data[0])); tb.style = "Table Grid"
            for i, row in enumerate(data):
                cells = tb.add_row().cells
                for j, cv in enumerate(row):
                    par = cells[j].paragraphs[0]
                    if i == 0:
                        write(par, f"<b>{plain(cv)}</b>", 8); shade(cells[j], "EAF6EC")
                    else:
                        write(par, cv, 8)
                        if j in right:
                            par.alignment = WD_ALIGN_PARAGRAPH.RIGHT
            d.add_paragraph()
        elif kind == "figure":
            _, path, caption, wmm = blk
            counters["fig"] += 1
            p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.add_run().add_picture(path, width=Mm(min(wmm, 170)))
            cap = d.add_paragraph(); cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            write(cap, f"<b>Figure {counters['fig']}.</b> {plain(caption)}", 8.5)
        elif kind == "numrefs":
            start = blk[3] if len(blk) > 3 else 1
            for i, r in enumerate(blk[2], start=start):
                p = d.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
                p.paragraph_format.space_after = Pt(3)
                write(p, f"[{i}]  {r}", 9)
        elif kind == "pagebreak":
            d.add_page_break()

    d.save(out)
    return out, counters


if __name__ == "__main__":
    blocks = C.blocks()
    pdf, pc = build_pdf(blocks, os.path.join(OUTDIR, "Task-28-Final-Technical-Report.pdf"))
    print(f"Wrote: {pdf}  ({pc['tbl']} tables, {pc['fig']} figures)")
    docx, dc = build_docx(blocks, os.path.join(OUTDIR, "Task-28-Final-Technical-Report.docx"))
    print(f"Wrote: {docx}  ({dc['tbl']} tables, {dc['fig']} figures)")
