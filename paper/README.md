# Care Companion — IEEE paper

Reproducible build of the paper and every figure in it.

## Two formats

| File | Format | For |
| --- | --- | --- |
| `CareCompanion-IEEE-Paper.pdf` | IEEE two-column, US Letter | the ICISS 2027 submission |
| `CareCompanion-IMPACT.docx` | IMPACT template: single-column A4, Word | the IMPACT submission |

The IMPACT template is single-column A4 Word with no header or footer, Times New Roman
throughout (16 pt title, 11 pt body, 10 pt abstract and references), US spelling, numbered
sections, and figures supplied separately as 600 dpi TIF. That is different enough from
IEEE two-column that it is a separate builder, `build_paper_impact.py`, rather than a
switch in the IEEE one. `Paper-Template-IMPACT.docx` and `IMPACT-guidelines.docx` are the
sources those rules come from.

The builder checks the limits it has to meet on every run: abstract 200 words, conclusions
250 to 500, running head 45 characters.

## Build

```bash
python3 -m venv .venv
./.venv/bin/pip install matplotlib reportlab pyphen
./.venv/bin/python gen_figures.py    # figures, parsed from ../app source
./.venv/bin/python build_paper.py          # -> CareCompanion-IEEE-Paper.pdf
./.venv/bin/python build_paper_impact.py   # -> CareCompanion-IMPACT.docx
```

## Why the figures are generated, not drawn

`gen_figures.py` parses the Kotlin sources under `../app/src/main` for declared text
sizes and control heights. Figs. 3 and 4 are therefore measurements of the shipped
implementation and cannot drift from it. Re-running after a UI change updates the paper's
numbers. Figs. 1, 2 and 5 are diagrams; Fig. 6 is the recorded defect log.

## Before submitting

- Add email addresses to the title block if your faculty requires them; they are
  currently omitted. Names, roll numbers and both departments are confirmed.
- The paper reports no user study, because none has been run. Section VIII says so
  explicitly. Do not add usability or clinical claims without collecting the data.

## Claims verified against the repository

Every quantitative claim below was re-measured, not estimated:

| Claim | Source of truth |
| --- | --- |
| 83 Kotlin files, 13,473 lines | `find app/src/main/java -name '*.kt'` |
| 142 automated tests, 0 lint errors | Gradle test XML + lint report |
| 33 row-level policies, 17 tables | `select ... from pg_policies` on the live database |
| 17 caregiver destinations, 7 elder | navigation graph in `GuardianApp.kt`, `ElderDest` enum |
| Median 20 sp / 13 sp; controls >= 64 dp | `gen_figures.py`, parsed from source |
| 13 defects, 8 silent (62%) | one per commit in version-control history |
| 6-digit, single-use, 7-day, no rebind | `supabase/migrations/*_invite_codes.sql` |
| Missed doses marked server-side | `scan_missed_doses()`, scheduled every 15 min |

All 21 references were resolved through Crossref for exact authors, venue, volume and
article number.
