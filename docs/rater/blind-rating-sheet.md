# Blind defect rating sheet

You are classifying 13 defects found in an Android elder-care application during a
post-implementation audit. Each was later fixed. You are given the mechanism only.
**No classification has been supplied to you. Do not try to infer one from wording.**

## Rubric

For each item, assign exactly one label:

- **SILENT** — a user of the application (either the older adult or the caregiver) could
  NOT have detected this from the interface alone. The interface either reported success,
  or reported nothing at all, while the operation failed or never occurred.
- **OBSERVABLE** — a user could have detected this from the interface alone: something
  looked wrong, was unreadable, was mispositioned, failed visibly, or took more steps
  than expected.

Judge *detectability from the interface by a non-technical user*, not severity, and not
whether a developer reading logs could find it. If an operation appeared to succeed but
did not, that is SILENT even if a later screen would eventually look wrong.

For each item output: `id | LABEL | one-sentence justification | confidence 1-5`.

## Items

**D01** — On a medicine scheduling screen the user presses Save. The write to the server
succeeds. A background refresh that was already in flight returns afterwards and replaces
the screen's state object with a newly constructed one, resetting the completion flag the
screen uses to decide to navigate away. The screen stays on the form.

**D02** — A second view model on a different screen exhibits the same construction:
a completed action's transient flag is discarded when a background refresh lands after it.

**D03** — A third view model exhibits the same construction as D01 and D02.

**D04** — Every repository write ends by taking the first element of the list the server
returns. When the access policy hides the returned row, that list is empty and the
resulting exception carries a null message. 17 call sites pass that null message to the
interface, which renders it as an empty string in the error area.

**D05** — An offline queue's flush routine snapshotted the pending queue, awaited the
network call, then wrote back only that snapshot — discarding any item added to the queue
while the network call was in flight. Items affected include a recorded medication dose.

**D06** — A caregiver can create general reminders (for example water or walking). They
are validated, saved to the database, and listed back on the caregiver's own reminders
screen. No alarm is ever armed on the older adult's device for them, and the only
server-side scheduled job scans medication doses.

**D07** — The caregiver invitation path matched users on telephone number. After the
project migrated to federated sign-in, the identity provider returns no telephone number,
so the stored value is the empty string for every account. The matching predicate compares
an empty string against a real number. Invitations are recorded in a pending state and the
resolution routine runs indefinitely.

**D08** — Medication alarms for the day were armed only in response to the older adult
opening the application. No periodic re-arming job existed.

**D09** — Answering a dose early (taking an 08:00 tablet at 07:45) did not cancel the
pending 08:00 alarm. The alarm fires at the scheduled time and prompts the user to take a
medicine they have already taken.

**D10** — Alarm keys were not namespaced by their source. The key's hash is used both as
the pending-intent request code and as the notification identifier, so a general reminder
and a medication dose sharing a hash replace one another.

**D11** — Text on one screen was rendered in a colour that is very low contrast against
the background when the device is in dark mode.

**D12** — A control on one screen is clipped by its container at some screen sizes, so
part of it is cut off.

**D13** — A control on one screen uses a colour outside the application's defined palette,
making it visually inconsistent with equivalent controls elsewhere.
