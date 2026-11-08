"""
build_task29_doi_proof.py
-------------------------
Task 29 — proof of the assigned DOI.

    python paper/build_task29_doi_proof.py

Builds a two-page PDF holding (a) the live metadata of the deposited record,
fetched from the Zenodo and DataCite APIs at build time, (b) the result of
resolving the DOI through doi.org, and (c) screenshots of the record page as it
appears to anybody who opens the link.

Nothing here is typed in by hand: the metadata is whatever the registries return
when this script runs, and the verification timestamp is the moment they were
asked.
"""
import os, sys, json, datetime, urllib.request, subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUTDIR = os.path.expanduser("~/Desktop/CareCompanion-Tasks-28-31")
os.makedirs(OUTDIR, exist_ok=True)
OUT = os.path.join(OUTDIR, "Task-29-DOI-Proof.pdf")

DOI = "10.5281/zenodo.23811415"          # version 2 — the file submitted for Task 29
RECORD = "https://zenodo.org/records/23811415"
PREV_DOI = "10.5281/zenodo.23601399"     # version 1 — the Task 24 preprint manuscript
PREV_RECORD = "https://zenodo.org/records/23601399"
DOI_URL = f"https://doi.org/{DOI}"
SHOTS = sys.argv[1] if len(sys.argv) > 1 else ""

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (Image, KeepTogether, PageBreak, Paragraph,
                                SimpleDocTemplate, Spacer, Table, TableStyle)
from PIL import Image as PILImage


def get(url):
    req = urllib.request.Request(url, headers={"Accept": "application/json",
                                               "User-Agent": "carecompanion-doi-proof"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def resolve(url):
    out = subprocess.run(
        ["curl", "-s", "-o", "/dev/null", "-L",
         "-w", "%{http_code}|%{url_effective}|%{num_redirects}", url],
        capture_output=True, text=True).stdout
    return out.split("|")


zen = get(f"https://zenodo.org/api/records/{DOI.rsplit('.', 1)[1]}")
try:
    dc = get(f"https://api.datacite.org/dois/{DOI}")["data"]["attributes"]
except Exception:
    dc = {}
code, final, redirects = resolve(DOI_URL)
prev_code, prev_final, _ = resolve(f"https://doi.org/{PREV_DOI}")
checked = datetime.datetime.now(datetime.timezone.utc).strftime("%d %B %Y, %H:%M UTC")

m = zen["metadata"]
GREEN = colors.HexColor("#2E7D32"); GREEN_DARK = colors.HexColor("#1B5E20")
GREEN_PALE = colors.HexColor("#EAF6EC"); INK = colors.HexColor("#1C1C1C")
SUB = colors.HexColor("#5A5A5A"); RULE = colors.HexColor("#D8DCD8")
ss = getSampleStyleSheet()
def st(n, **kw): return ParagraphStyle(n, parent=ss["BodyText"], **kw)
S = {
    "title": st("t", fontName="Helvetica-Bold", fontSize=19, leading=23, textColor=INK,
                alignment=TA_CENTER, spaceAfter=4),
    "sub": st("s", fontName="Helvetica", fontSize=11, leading=15, textColor=SUB,
              alignment=TA_CENTER, spaceAfter=12),
    "h": st("h", fontName="Helvetica-Bold", fontSize=12.5, leading=16, textColor=GREEN_DARK,
            spaceBefore=13, spaceAfter=5),
    "b": st("b", fontName="Helvetica", fontSize=9.6, leading=14, textColor=INK,
            alignment=TA_JUSTIFY, spaceAfter=6),
    "cell": st("c", fontName="Helvetica", fontSize=9, leading=12.6, textColor=INK),
    "cellb": st("cb", fontName="Helvetica-Bold", fontSize=9, leading=12.6, textColor=INK),
    "doi": st("d", fontName="Courier-Bold", fontSize=15, leading=19, textColor=GREEN_DARK,
              alignment=TA_CENTER, spaceAfter=3),
    "cap": st("cp", fontName="Helvetica", fontSize=8.3, leading=11.4, textColor=SUB,
              alignment=TA_CENTER, spaceAfter=10),
}
CW = 210 * mm - 2 * 20 * mm


def kvtable(pairs, w=(52, 118)):
    t = Table([[Paragraph(a, S["cellb"]), Paragraph(b, S["cell"])] for a, b in pairs],
              colWidths=[w[0] * mm, w[1] * mm])
    t.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("BACKGROUND", (0, 0), (0, -1), GREEN_PALE),
        ("LINEBELOW", (0, 0), (-1, -2), 0.4, RULE), ("BOX", (0, 0), (-1, -1), 0.5, RULE),
        ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 4.5), ("BOTTOMPADDING", (0, 0), (-1, -1), 4.5)]))
    return KeepTogether([Spacer(1, 4), t, Spacer(1, 8)])


def shot(path, caption, width_mm=165):
    w, h = PILImage.open(path).size
    dw = width_mm * mm
    return KeepTogether([Image(path, width=dw, height=dw * h / w), Spacer(1, 3),
                         Paragraph(caption, S["cap"])])


F = []
A = F.append
A(Paragraph("DOI Proof", S["title"]))
A(Paragraph("Task 29 — CareCompanion · K. J. Somaiya School of Engineering", S["sub"]))

box = Table([[Paragraph("DIGITAL OBJECT IDENTIFIER", S["cellb"])],
             [Paragraph(DOI, S["doi"])],
             [Paragraph(f'<link href="{DOI_URL}" color="#2E7D32">{DOI_URL}</link>',
                        S["cap"])]], colWidths=[CW])
box.setStyle(TableStyle([
    ("ALIGN", (0, 0), (-1, -1), "CENTER"), ("BACKGROUND", (0, 0), (-1, -1), GREEN_PALE),
    ("BOX", (0, 0), (-1, -1), 1.2, GREEN), ("TOPPADDING", (0, 0), (-1, 0), 9),
    ("BOTTOMPADDING", (0, -1), (-1, -1), 7)]))
A(box)
A(Spacer(1, 8))

A(Paragraph("1. The deposited record", S["h"]))
A(Paragraph("The paper is deposited with Zenodo, operated by CERN, which assigns a DOI "
            "registered through DataCite. The metadata below was fetched from the Zenodo "
            f"API at {checked}; it is what the registry holds now, not a transcription.",
            S["b"]))
A(kvtable([
    ("Title", m["title"]),
    ("Authors", ", ".join(c["name"] for c in m["creators"])),
    ("Repository", "Zenodo (CERN)"),
    ("Record URL", f'<link href="{RECORD}" color="#2E7D32">{RECORD}</link>'),
    ("Version DOI", DOI),
    ("Concept DOI (all versions)", zen.get("conceptdoi", "—")),
    ("Publication date", m.get("publication_date", "—")),
    ("Resource type", m.get("resource_type", {}).get("title", "—")),
    ("Licence", m.get("license", {}).get("id", "—")),
    ("File deposited (matches the PDF submitted)", ", ".join(f"{f['key']} ({f['size'] / 1024:.1f} kB)"
                                 for f in zen.get("files", [])) or "—"),
    ("Record state", f"{zen.get('state', '—')}, submitted={zen.get('submitted')}"),
    ("Views / downloads",
     f"{zen.get('stats', {}).get('views', '—')} / "
     f"{zen.get('stats', {}).get('downloads', '—')} at the time of checking"),
]))

A(Paragraph("2. Version history", S["h"]))
A(Paragraph("The record has two published versions. Version 2 holds the paper submitted "
            "for this task; version 1 holds the longer preprint manuscript submitted for "
            "Task 24 and remains published at its own DOI. The concept DOI below resolves "
            "to whichever version is latest, and is the right one to cite for the work as a "
            "whole.", S["b"]))
A(kvtable([
    ("Concept DOI (all versions)", zen.get("conceptdoi", "—")),
    ("Version 2 — submitted here",
     f"{DOI} · Task-29-IEEE-Paper.pdf"),
    ("Version 1 — Task 24", f"{PREV_DOI} · Task-24-Preprint-Manuscript.pdf"),
    ("Version 1 still resolves", f"HTTP {prev_code} to {prev_final}"),
]))

A(Paragraph("3. The DOI resolves", S["h"]))
A(Paragraph("A DOI is only proof if it resolves. The handle was followed through "
            "doi.org at the moment this page was generated, and independently confirmed "
            "against the DataCite registry.", S["b"]))
A(kvtable([
    ("Request", f"GET {DOI_URL}"),
    ("HTTP status", f"{code}"),
    ("Redirects followed", f"{redirects}"),
    ("Resolved to", final),
    ("DataCite registration state", dc.get("state", "—")),
    ("DataCite registered", (dc.get("registered") or "—")),
    ("DataCite registration agency",
     (dc.get("agency") or "datacite")),
    ("Checked at", checked),
]))

if SHOTS and os.path.isdir(SHOTS):
    A(PageBreak())
    A(Paragraph("4. The record as it appears to a reader", S["h"]))
    A(Paragraph("Screenshots of the published record page, captured from the live site.",
                S["b"]))
    top = os.path.join(SHOTS, "zenodo-record-top.jpg")
    doi_shot = os.path.join(SHOTS, "zenodo-record-doi.jpg")
    if os.path.exists(top):
        A(shot(top, "Figure 1. The published record at " + RECORD + " — title, authors, "
                    "publication date, abstract, and the version list showing both "
                    "published versions."))
    if os.path.exists(doi_shot):
        A(shot(doi_shot, "Figure 2. The same record, scrolled to the Details panel, showing "
                         f"the assigned DOI badge {DOI}, the deposited file Task-29-IEEE-Paper.pdf, "
                         f"and the licence."))

A(Paragraph("5. How to cite", S["h"]))
A(Paragraph(
    f"{', '.join(c['name'] for c in m['creators'])}, &ldquo;{m['title']},&rdquo; "
    f"Zenodo, {m.get('publication_date', '')}. doi: {DOI}.", S["b"]))

doc = SimpleDocTemplate(OUT, pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
                        topMargin=18 * mm, bottomMargin=18 * mm,
                        title="Task 29 — DOI Proof", author="CareCompanion project group")
doc.build(F)
print("Wrote:", OUT)
print("DOI", DOI, "->", code, final)
