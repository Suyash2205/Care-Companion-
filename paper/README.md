# Care Companion — IEEE paper

Reproducible build of the paper and every figure in it.

## Build

```bash
python3 -m venv .venv
./.venv/bin/pip install matplotlib reportlab pyphen
./.venv/bin/python gen_figures.py    # figures, parsed from ../app source
./.venv/bin/python build_paper.py    # -> CareCompanion-IEEE-Paper.pdf
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
