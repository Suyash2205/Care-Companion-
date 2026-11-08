# Response to reviewers — ICISS 2027, Paper 31

**CareCompanion: A Caregiver-Mediated Android System for Older Adults, with Fail-Honest Alerting**

We thank both reviewers. Three comments identified claims our implementation did not
support, and checking them against the source changed the paper rather than only its
wording. Where a reviewer was mistaken we say so, and where checking showed *we* were
mistaken in a way the reviewer had not noticed, we say that too.

---

## Reviewer 1

### R1.1 — "Server acknowledgment of storing an alert is not caregiver receipt. The stated fail-honest confirmation must precisely name what was observed."

**Accepted; this was the most important comment we received.** You are right, and the
defect was in the paper *and* in the shipped application.

We traced the emergency path in source. The client makes up to three attempts to write an
alert row; the confirmation is permitted by exactly one event — **the server acknowledged
persistence of the alert record**. The submitted paper said the terminal state was selected
by whether the record was "acknowledged", which reads as caregiver acknowledgement. Worse,
the shipped interface string read *"Your family has been notified"* and *"Sent to your
family's phones"*, which assert an outcome the application cannot observe. That is the
exact failure our own rule forbids.

Section 3.3 now names the permitted event explicitly and lists what the confirmation is
*not*: not push acceptance, not device delivery, not notification display, not caregiver
acknowledgement. The interface strings are corrected in source to *"Alert received"* and
*"Your family is being alerted"*, with a comment recording why they must not be changed
back. We also note that caregiver acknowledgement exists in the data model but is not
propagated back to the elder device, and identify closing that loop as the most valuable
extension of the discipline.

We report this in the paper as evidence *for* its thesis rather than quietly fixing it.

### R1.2 — "A six-digit code valid seven days requires attempt limits, authenticated rate limiting, atomic redemption and token security; these properties are not fully specified or adversarially tested."

**Accepted.** We inspected all fifteen database migrations. There was no attempt limit, no
lockout and no rate limiting of any kind, and redemption was not atomic: both routines read
the code row and updated it later without locking, so two concurrent redemptions could each
observe it as unredeemed. The submitted paper called the code "short-lived", which a
seven-day lifetime does not justify.

We have since implemented and deployed: a per-account failure counter refusing further
attempts after five failures in fifteen minutes, cleared by a successful redemption; and a
row lock (`SELECT … FOR UPDATE`) held for the remainder of the transaction. Section 3.2 now
gives the threat model explicitly, including the $10^6$ space and the birthday-style
exposure that grows with deployment size.

Two things we did **not** do, stated as decisions. An already-redeemed code still returns a
message distinct from the unknown-code message. This is in principle an enumeration oracle;
we retain it because collapsing it would tell an older adult holding a spent code only that
it is invalid, removing the hint that lets them recover without assistance, and attempt
limiting makes enumeration through it infeasible. And while the deployed endpoint was
tested against unauthenticated guessing, the limiter has not been exercised end-to-end
under an authenticated session — so the paper claims it as implemented, not as measured.

### R1.3 — "Single-developer audit lacks an independent reviewer and systematic device/network fault injection. Static target heights do not establish accessible width or observed user outcomes."

**Accepted in full; partially remedied, and we are explicit about which part.**

*Independent rating.* We obtained a blind re-rating: each defect was reduced to a
mechanism-only description with the label removed and classified against a written rubric
by a rater with no access to the repository, the paper or the original labels. We state
plainly in Section 5.3 that this rater was a large language model and **does not substitute
for an independent human expert**. On the twelve comparable items the raters agreed on
eleven ($\kappa = 0.82$).

The re-rating produced something more useful than agreement. The rater gave its *lowest*
confidence to the three lost-state defects, reasoning that a form which does not navigate
after a successful save is arguably a visible symptom. Were those reclassified the silent
proportion falls from 8/13 to 5/13. The headline therefore rests on one judgement applied
to three items, and the paper now says so instead of reporting 62% as robust. We also state
what no re-rating can repair: the defects were *selected* by the developers.

*Fault injection.* Not yet present, and Section 5.5 now says so in those words rather than
leaving a reader to infer it. On-device tests against the real `AlarmManager` have been
written covering dose cancellation, reboot re-arming, past-dose skipping and key-collision;
the protocol for connectivity, permission-denial and Doze injection is defined. We have
deliberately **not** reported results we do not yet have.

*Target sizes.* Agreed. Section 5.1 now states these are declared heights of a single
control — a one-dimensional static property that establishes neither effective touch width,
nor spacing between adjacent targets, nor any observed user outcome.

### R1.4 — "Does not expose a verifiable artifact link. Novelty is a modest transferable design insight, not a new intelligent algorithm."

**Accepted, both parts.** An Artifact Availability statement now gives the Zenodo concept
DOI `10.5281/zenodo.23601398`, which resolves to the latest version and carries the
measurement scripts and raw observation files. On novelty we agree and no longer imply
otherwise: Section 6.2 states the contribution is a transferable design insight rather than
a new algorithm.

---

## Reviewer 2

### R2.1 — "Fig. 1 states 11 caregiver / 6 elder screens, while the text states 17 navigation destinations and 7 elder screens. Please reconcile."

**The figure does not say 11/6 — but checking this found a real error of ours, so thank
you.** Figure 1 in the submitted PDF reads *"17 destinations"* and *"7 screens"*, matching
the text. We believe the confusion arose from Table 1, which lists the elder surface as
**6** — that is a count of *source files*, not screens, and sat two pages from a figure
saying 7.

Counting from the navigation graph, the caregiver surface registers **20** destinations,
not 17. Our number was wrong; the figure was not. The paper now fixes a counting rule
("one entry registered in the client's navigation graph"), states 20 and 7 plus a one-time
binding screen, and Table 1's caption says explicitly that its counts are of source files
and should not be conflated with destinations.

### R2.2 — "A 6-digit code valid for seven days is not 'short-lived'. Describe the rate limiting or lockout that prevents brute-forcing the 10^6 code space."

**Accepted.** See R1.2 — the word is removed, the arithmetic is given, and rate limiting
and atomic redemption are now implemented and deployed.

### R2.3 — "Frame the defect taxonomy as a case study throughout, including the abstract."

**Accepted.** The abstract now says "as a single-system case study"; Section 5.3 is titled
*Defect Profile: A Single-System Case Study* and opens by stating it is not a population
estimate; and the two generalisable observations are explicitly labelled hypotheses rather
than findings.

### R2.4 — "The title mentions 'Disabled Users', but the design and evaluation address only ageing. Adjust the title or add content."

**Accepted; we narrowed the title.** It is now *CareCompanion: A Caregiver-Mediated Android
System for Older Adults, with Fail-Honest Alerting*. We considered adding disability
content, but doing so without having evaluated the system with users whose impairments
arise independently of ageing would be precisely the unsupported claim Reviewer 1 objects
to. Limitations now states this scope boundary explicitly.

### R2.5 — "Reformat to the Springer LNNS template."

**Done.** The camera-ready uses `llncs.cls` v2.21, within the 12-page allowance plus one
chargeable page.

### R2.6 — "A small formative study with older adult–caregiver dyads would be a valuable next step."

**Agreed, and adopted as the stated plan.** Section 7 now sets out a two-stage path:
fault-injection on device first, then a formative dyad study measuring delivery-level
outcomes the literature usually assumes — proportion of scheduled reminders actually
presented, time from alarm to caregiver acknowledgement, and frequency of the degraded
emergency path.

---

## One change neither reviewer asked for

Preparing this revision, an independent run of our test suite found that the Gradle
configuration sets `ignoreFailures` on unit tests, so the build prints BUILD SUCCESSFUL
regardless of results — and that ten Compose interface tests fail in the release variant
while all pass in debug. We had read the build's exit status as evidence the suite was
green.

This is our own thesis, in our own tooling: a build that reports success it has not
observed is the same defect class as an interface that does. Section 4 reports it.
