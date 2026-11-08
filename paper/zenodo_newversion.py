"""
zenodo_newversion.py — Task 29: deposit the final IEEE paper as version 2 of the
existing Zenodo record, so that the DOI points at exactly the PDF submitted.

Version 1 (the Task 24 preprint manuscript) stays published and reachable for
ever; Zenodo never removes a published version. Version 2 receives its own
version DOI under the same concept DOI.

The token is never passed on the command line and never printed. It is read from
~/.config/zenodo/token.

    python paper/zenodo_newversion.py              # dry run: shows what would be sent
    python paper/zenodo_newversion.py --publish    # creates v2, uploads, publishes
"""
import argparse, json, os, re, sys, datetime, pathlib

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))

from zenodo_submit import call, token, TOKEN_FILE, API, SANDBOX, AFFIL, CREATORS

OUTDIR = pathlib.Path(os.path.expanduser("~/Desktop/CareCompanion-Tasks-28-31"))
PDF = OUTDIR / "Task-29-IEEE-Paper.pdf"
PARENT_DEPOSITION = 23601399          # the published version 1

TITLE = ("Measuring a Dual-Surface Elder-Care Android Application: "
         "Interface Geometry, Latency and Data Isolation")
KEYWORDS = ["mobile health", "elderly users", "accessibility measurement",
            "software measurement", "row-level security", "Android",
            "empirical software engineering", "assistive technology",
            "dyadic design", "access control"]
NOTES = ("Version 2 is the final IEEE-format paper of the same measurement study. "
         "Version 1 of this record is the longer preprint manuscript and remains "
         "published and citable at its own version DOI. The instrument scripts and the "
         "raw observation files behind every figure are released with the paper.")


def abstract():
    """Read the abstract out of the typeset paper, so the two cannot drift apart."""
    from pypdf import PdfReader
    t = PdfReader(str(PDF)).pages[0].extract_text()
    a = t[t.index("Abstract"):]
    a = a[:a.index("Index Terms")] if "Index Terms" in a else a
    a = re.sub(r"Abstract\s*[—–-]\s*", "", a)
    a = re.sub(r"-\n", "", a).replace("\n", " ")
    a = re.sub(r"\s{2,}", " ", a).strip()
    for bad, good in (("roundtrip", "round-trip"), ("onesecond", "one-second"),
                      ("caredata", "care-data"), ("dualsurface", "dual-surface")):
        a = a.replace(bad, good)
    return a


def metadata():
    return {"metadata": {
        "upload_type": "publication",
        "publication_type": "preprint",
        "title": TITLE,
        "version": "2",
        "creators": CREATORS,
        "description": "<p>" + abstract() + "</p>",
        "keywords": KEYWORDS,
        "license": "cc-by-4.0",
        "access_right": "open",
        "publication_date": datetime.date.today().isoformat(),
        "language": "eng",
        "notes": NOTES,
    }}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--publish", action="store_true",
                    help="actually create version 2, upload and publish (irreversible)")
    ap.add_argument("--sandbox", action="store_true")
    a = ap.parse_args()
    base = SANDBOX if a.sandbox else API

    if not PDF.exists():
        sys.exit(f"Paper not found: {PDF}")
    md = metadata()

    if not a.publish:
        print("DRY RUN — nothing is sent.\n")
        print(f"Parent record : {base}/deposit/depositions/{PARENT_DEPOSITION}")
        print(f"File          : {PDF}  ({PDF.stat().st_size/1e3:.0f} kB)")
        print(f"Token file    : {TOKEN_FILE}  "
              f"({'present' if TOKEN_FILE.exists() else 'NOT PRESENT'})")
        print("\nMetadata that would be sent:\n")
        print(json.dumps(md, indent=2)[:3000])
        print("\nRun again with --publish. Publishing a version cannot be undone.")
        return

    tok = token()

    print("1/6 opening a new version of the record …")
    _, res = call("POST",
                  f"{base}/deposit/depositions/{PARENT_DEPOSITION}/actions/newversion",
                  tok, payload={})
    draft_url = res["links"]["latest_draft"]
    _, draft = call("GET", draft_url, tok)
    draft_id = draft["id"]
    bucket = draft["links"]["bucket"]
    print(f"    draft deposition {draft_id}")

    print("2/6 removing the file inherited from version 1 …")
    _, files = call("GET", f"{base}/deposit/depositions/{draft_id}/files", tok)
    for f in files:
        print(f"    dropping {f['filename']}")
        call("DELETE", f"{base}/deposit/depositions/{draft_id}/files/{f['id']}", tok)

    print("3/6 uploading the final IEEE paper …")
    call("PUT", f"{bucket}/{PDF.name}", tok, raw=PDF.read_bytes(),
         ctype="application/octet-stream")

    print("4/6 writing the metadata …")
    call("PUT", f"{base}/deposit/depositions/{draft_id}", tok, payload=md)

    print("5/6 publishing version 2 …")
    _, rec = call("POST", f"{base}/deposit/depositions/{draft_id}/actions/publish", tok)

    print("6/6 saving the raw response …")
    (OUTDIR / "Task-29-Zenodo-Version2.json").write_text(json.dumps(rec, indent=2))

    print(f"\nVersion DOI : {rec.get('doi')}")
    print(f"Concept DOI : {rec.get('conceptdoi')}")
    print(f"Record      : {rec.get('links', {}).get('record_html', rec.get('doi_url'))}")
    print(f"File        : {PDF.name}")


if __name__ == "__main__":
    main()
