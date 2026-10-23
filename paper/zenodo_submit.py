"""
zenodo_submit.py — Task 24: deposit the preprint on Zenodo and capture the receipt.

The token is never passed on the command line and never printed. It is read from a
file the user creates themselves (default ~/.config/zenodo/token), so it does not pass
through the conversation, the shell history or this file.

    python paper/zenodo_submit.py                # dry run: shows exactly what would be sent
    python paper/zenodo_submit.py --publish      # creates, uploads, publishes, saves receipt

A dry run needs no token. --publish needs a token with scopes:
    deposit:write   deposit:actions
"""
import argparse, json, os, re, sys, datetime, urllib.request, urllib.error, pathlib

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
OUTDIR = pathlib.Path(os.path.expanduser("~/Desktop/CareCompanion-Tasks-20-27"))
PDF = OUTDIR / "Task-24-Preprint-Manuscript.pdf"
TOKEN_FILE = pathlib.Path(os.path.expanduser("~/.config/zenodo/token"))
API = "https://zenodo.org/api"
SANDBOX = "https://sandbox.zenodo.org/api"

TITLE = ("An Instrumented Measurement Study of a Dual-Surface Elder-Care "
         "Android Application")
AFFIL = ("K. J. Somaiya School of Engineering, Somaiya Vidyavihar University, "
         "Mumbai, India")
CREATORS = [{"name": n, "affiliation": AFFIL} for n in (
    "Humne, Suyash S.", "Pandey, Devanshi", "Menezes, Deon",
    "Wankhede, Sonali", "Singh, Balwant Kumar")]
KEYWORDS = ["mobile health", "elderly users", "accessibility measurement",
            "software measurement", "row-level security", "Android",
            "empirical software engineering", "assistive technology"]


def abstract():
    """Read the abstract out of the typeset manuscript, so the two cannot drift."""
    from pypdf import PdfReader
    t = PdfReader(str(PDF)).pages[0].extract_text()
    a = t[t.index("Abstract"):]
    a = a[:a.index("Index Terms")] if "Index Terms" in a else a
    a = re.sub(r"Abstract[—-]\s*", "", a)
    a = re.sub(r"-\n", "", a).replace("\n", " ")
    a = re.sub(r"\s{2,}", " ", a).strip()
    # the extractor loses a few spaces across line breaks; repair the known ones
    for bad, good in (("roundtrip", "round-trip"), ("onesecond", "one-second")):
        a = a.replace(bad, good)
    return a


def metadata():
    return {"metadata": {
        "upload_type": "publication",
        "publication_type": "preprint",
        "title": TITLE,
        "creators": CREATORS,
        "description": "<p>" + abstract() + "</p>",
        "keywords": KEYWORDS,
        "license": "cc-by-4.0",
        "access_right": "open",
        "publication_date": datetime.date.today().isoformat(),
        "language": "eng",
        "communities": [],
        "notes": ("Companion measurement study to the system-design paper accepted at "
                  "IMPACT-2027 (paper 676). The scripts and the raw observation files "
                  "behind every figure are released with the manuscript."),
    }}


def token():
    if not TOKEN_FILE.exists():
        sys.exit(f"No token at {TOKEN_FILE}. Create it first (see the instructions), "
                 f"then run this again.")
    tok = TOKEN_FILE.read_text().strip()
    if not tok:
        sys.exit(f"{TOKEN_FILE} is empty.")
    return tok


def call(method, url, tok, payload=None, raw=None, ctype="application/json"):
    data = raw if raw is not None else (json.dumps(payload).encode() if payload else None)
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", f"Bearer {tok}")
    if data is not None:
        req.add_header("Content-Type", ctype)
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            body = r.read()
            return r.status, (json.loads(body) if body else {})
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors="replace")
        sys.exit(f"Zenodo refused {method} {url.split('?')[0]}\n  HTTP {e.code}\n  {body[:600]}")


def receipt_pdf(rec, path):
    """A one-page receipt: what was deposited, when, by whom, and the DOI."""
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.units import mm
    from reportlab.lib import colors
    from reportlab.lib.styles import ParagraphStyle
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
    GREEN = colors.HexColor("#2E7D32")
    H = ParagraphStyle("h", fontName="Helvetica-Bold", fontSize=15, leading=19,
                       textColor=colors.HexColor("#1B5E20"), spaceAfter=10)
    B = ParagraphStyle("b", fontName="Helvetica", fontSize=9.6, leading=14.5,
                       textColor=colors.HexColor("#1C1C1C"), spaceAfter=7)
    C = ParagraphStyle("c", fontName="Helvetica", fontSize=8.6, leading=12.4)
    CB = ParagraphStyle("cb", fontName="Helvetica-Bold", fontSize=8.6, leading=12.4)
    md = rec.get("metadata", {})
    rows = [["Field", "Value"],
            ["Title", md.get("title", TITLE)],
            ["Authors", "; ".join(c["name"] for c in md.get("creators", CREATORS))],
            ["Repository", "Zenodo (CERN)"],
            ["Record", str(rec.get("links", {}).get("record_html", rec.get("doi_url", "")))],
            ["DOI", str(rec.get("doi", ""))],
            ["DOI URL", str(rec.get("doi_url", ""))],
            ["Deposition id", str(rec.get("id", ""))],
            ["Publication date", md.get("publication_date", "")],
            ["License", "Creative Commons Attribution 4.0 International (CC BY 4.0)"],
            ["File deposited", PDF.name],
            ["Deposited at", datetime.datetime.now().astimezone().strftime("%Y-%m-%d %H:%M %Z")],
            ["State", str(rec.get("state", ""))]]
    doc = SimpleDocTemplate(str(path), pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
                            topMargin=20 * mm, bottomMargin=20 * mm,
                            title="Task 24 — preprint submission receipt")
    t = Table([[Paragraph(a, CB), Paragraph(b, C)] for a, b in rows],
              colWidths=[38 * mm, 132 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#EAF6EC")),
        ("LINEBELOW", (0, 0), (-1, 0), 0.8, GREEN),
        ("LINEBELOW", (0, 1), (-1, -2), 0.4, colors.HexColor("#DDE3DD")),
        ("BOX", (0, 0), (-1, -1), 0.5, colors.HexColor("#DDE3DD")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    doc.build([Paragraph("Task 24 — preprint submission receipt", H),
               Paragraph("This record confirms that the manuscript below was deposited on "
                         "Zenodo and issued a DOI. The full machine-readable response is "
                         "saved alongside this page as Task-24-Submission-Receipt.json.", B),
               Spacer(1, 4), t, Spacer(1, 10),
               Paragraph("CareCompanion · Devanshi Pandey · 16014023016 · EXCP · "
                         "K. J. Somaiya School of Engineering", B)])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--publish", action="store_true",
                    help="actually create, upload and publish (irreversible)")
    ap.add_argument("--sandbox", action="store_true",
                    help="use sandbox.zenodo.org for a rehearsal")
    a = ap.parse_args()
    base = SANDBOX if a.sandbox else API

    if not PDF.exists():
        sys.exit(f"Manuscript not found: {PDF}")
    md = metadata()

    if not a.publish:
        print("DRY RUN — nothing is sent.\n")
        print(f"Endpoint      : {base}/deposit/depositions")
        print(f"File          : {PDF}  ({PDF.stat().st_size/1e3:.0f} kB)")
        print(f"Token file    : {TOKEN_FILE}  "
              f"({'present' if TOKEN_FILE.exists() else 'NOT PRESENT'})")
        print("\nMetadata that would be sent:")
        print(json.dumps(md, indent=2)[:2600])
        print("\nRun again with --publish to deposit. That step cannot be undone.")
        return

    tok = token()
    print("1/5 creating the deposition …")
    _, dep = call("POST", f"{base}/deposit/depositions", tok, payload={})
    dep_id = dep["id"]
    bucket = dep["links"]["bucket"]

    print("2/5 uploading the manuscript …")
    call("PUT", f"{bucket}/{PDF.name}", tok, raw=PDF.read_bytes(),
         ctype="application/octet-stream")

    print("3/5 writing the metadata …")
    call("PUT", f"{base}/deposit/depositions/{dep_id}", tok, payload=md)

    print("4/5 publishing …")
    _, rec = call("POST", f"{base}/deposit/depositions/{dep_id}/actions/publish", tok)

    print("5/5 saving the receipt …")
    (OUTDIR / "Task-24-Submission-Receipt.json").write_text(json.dumps(rec, indent=2))
    receipt_pdf(rec, OUTDIR / "Task-24-Submission-Receipt.pdf")
    print(f"\nDOI   : {rec.get('doi')}")
    print(f"Record: {rec.get('links', {}).get('record_html', rec.get('doi_url'))}")
    print(f"Receipt: {OUTDIR/'Task-24-Submission-Receipt.pdf'}")


if __name__ == "__main__":
    main()
