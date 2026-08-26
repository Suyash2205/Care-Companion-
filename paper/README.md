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

- Replace the placeholder author names and email addresses on page 1.
- Verify every reference against the publisher record. Metadata was taken from Crossref,
  but reference [4] has no confirmed author list and [2] is a web source.
- The paper reports no user study, because none has been run. Section VIII says so
  explicitly. Do not add usability or clinical claims without collecting the data.
