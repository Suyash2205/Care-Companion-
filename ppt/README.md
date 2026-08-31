# CareCompanion presentation

Kept in the repository so it cannot be lost again — the previous copy lived in
`~/Downloads/presentation code/` and was deleted during a storage clear-out.

- `CareCompanion-source.pptx` — the deck as it stood before the IE-1 additions
- `update_deck.py` — adds the slides the Internal Evaluation-1 guidelines require
- `CareCompanion-IE1.pptx` — the output, 39 slides

## Rebuild

```bash
python3 update_deck.py      # needs python-pptx
```

The deck's slides are hand-positioned shapes on a Somaiya letterhead rather than
layout placeholders, so `update_deck.py` deep-copies an existing slide and rewrites
its text. That keeps the branding byte-identical. The model slide is found by its
title each time, never by index — an index goes stale the moment a slide is
inserted ahead of it.

## Design

New slides reuse the deck's own vocabulary rather than plain bullets: white cards
with numbered badges (as on the Scope slides), red header bars over white panels
(as on Overview of Implementation), and red chip rows (as on Module Diagram).
Brand colours and type styles are read off the existing slides, not guessed.

## Still to do before the evaluation

- The guidelines say the deck must follow an attached PPT template. That template
  was not supplied, so this uses the existing Somaiya letterhead design.
- Add the demo video link on the Supporting Documents slide.
- Confirm the full name of ICISS 2027. The site (www.iciss2027.in) shows only
  the acronym and that review runs through Microsoft CMT, and several unrelated
  conferences share the ICISS acronym, so the slide states only what is verifiable.
- Screenshots on slides 21–24 are one revision behind the current build.
