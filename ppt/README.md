# CareCompanion presentation

Kept in the repository so it cannot be lost again — the previous copy lived in
`~/Downloads/presentation code/` and was deleted during a storage clear-out.

- `CareCompanion-source.pptx` — the deck as it stood before the IE-1 additions
- `update_deck.py` — adds the slides the Internal Evaluation-1 guidelines require
- `CareCompanion-IE1.pptx` — 39 slides on the old 10 x 5.625 in canvas
- `KJSSE-PPT-Template.pptx` — the official college template
- `retemplate.py` — ports the deck onto that template
- `CareCompanion-IE1-KJSSE.pptx` — **the file to submit**

## Rebuild

```bash
python3 update_deck.py      # adds the IE-1 slides
python3 retemplate.py       # ports the result onto the college template
```

The deck's slides are hand-positioned shapes on a Somaiya letterhead rather than
layout placeholders, so `update_deck.py` deep-copies an existing slide and rewrites
its text. That keeps the branding byte-identical. The model slide is found by its
title each time, never by index — an index goes stale the moment a slide is
inserted ahead of it.

## Porting onto the college template

The template carries the real Somaiya crest, the TRUST logo, the three-part footer
and the page number on its slide master, at 13.333 x 7.5 in. Our deck was built at
10 x 5.625 in with a text-box imitation of the letterhead. Both are 16:9, so every
shape ports with a single uniform scale of 4/3 — positions, sizes and font sizes.

Three things that are easy to get wrong here, all of which produced a file that
python-pptx would open and LibreOffice would not:

- copied pictures keep an `r:embed` pointing at a relationship that does not exist
  in the destination, so image parts must be re-added by blob;
- re-adding them by relating the source part reuses its partname and collides with
  the template's own `ppt/media/image1.png`;
- the tables reference a table-style GUID the template does not define, so
  `ppt/tableStyles.xml` has to be carried across too.

Tables also sit in a `graphicFrame` whose transform is `p:xfrm`, not `a:xfrm`, and
their real geometry is in `a:gridCol/@w` and `a:tr/@h` — miss those and the tables
neither scale nor move.

## Design

New slides reuse the deck's own vocabulary rather than plain bullets: white cards
with numbered badges (as on the Scope slides), red header bars over white panels
(as on Overview of Implementation), and red chip rows (as on Module Diagram).
Brand colours and type styles are read off the existing slides, not guessed.

## Still to do before the evaluation

- Add the demo video link on the Supporting Documents slide.
- Confirm the full name of ICISS 2027. The site (www.iciss2027.in) shows only
  the acronym and that review runs through Microsoft CMT, and several unrelated
  conferences share the ICISS acronym, so the slide states only what is verifiable.
- Screenshots on slides 21–24 are one revision behind the current build.
