# Privacy Policy — Care Companion

**Last updated: 9 August 2026**

Care Companion helps a family member (the "guardian") support an older adult (the "elder")
by keeping track of medicines, health readings, emergency contacts, and emergency alerts.
This policy explains exactly what the app collects, why, and what happens to it.

We do not sell your data. We do not use it for advertising. We do not share it with anyone
except the service providers listed below, who process it on our behalf.

---

## Who we are

Care Companion is operated by the app's publisher, contactable at the email address listed
on the Google Play store page for the app.

## What we collect, and why

### Account information
- **Your Google account email address, name, and profile picture**, provided when you sign
  in with Google.
- Used to identify you, to keep you signed in, and to show guardians who else has access
  to an elder's profile.
- We never receive or store your Google password.

### Elder care profile
Entered by the guardian, and visible to the elder on their own device:
- Name, age, address, photo, phone number
- Medicines, dosages, and reminder schedules
- Emergency contacts (name, phone number, photo, relationship)
- Health readings the elder or guardian records (blood pressure, blood sugar, temperature,
  pulse), including the date and time of each reading
- Whether each medicine dose was taken or skipped

This is the core function of the app: it is what the guardian sees and what generates the
elder's reminders.

### Location
- **Collected only at the moment the elder presses the SOS button.** A single location fix
  is taken and attached to that emergency alert.
- We do **not** track location in the background, and we do **not** collect location at any
  other time.
- The location is shown to the linked guardians so they can find the elder in an emergency.

### Contacts
- If the guardian chooses to add an emergency contact from the phone's address book, the
  app reads that contact's name and number **at that moment only**, and stores only the
  entry that was selected.
- The app does not upload, scan, or index your address book.

### Device notification token
- A Firebase Cloud Messaging token, so medicine reminders and emergency alerts can reach
  the right device. It is deleted when you sign out.

### What we do NOT collect
- No advertising identifiers, no analytics profiles, no browsing history
- No microphone, camera, call log, or SMS message content
- No background location

## Health information

Medicines, doses, and vital readings are health information, and we treat them as
sensitive. They are stored encrypted in transit (HTTPS) and at rest, and access is
restricted at the database level so that a given record can only be read by the elder it
belongs to and the guardians that elder is linked to. We do not use health information for
any purpose other than showing it inside the app to those people.

## How profiles are linked

A guardian creates the elder's profile and receives a six-digit connect code. Entering that
code once on the elder's device links the two accounts. Codes are single-use and expire
after seven days. This is the only way an account gains access to an elder's data.

## Who your data is shared with

- **Other guardians linked to the same elder**, as intended by the person who invited them.
- **Google Firebase** (Authentication, Cloud Messaging) — sign-in and notification delivery.
- **Supabase** (hosted Postgres) — where care data is stored.

These providers process data on our behalf under their own security commitments. We share
data with no one else, and we never sell it.

## Emergency SMS

When an emergency alert cannot be delivered over the internet, the app can open your
phone's own messaging app with an emergency message pre-filled. **The app never sends a
message by itself** — you have to press send, and the message goes only to the contact you
chose. Care Companion does not have permission to read or send SMS.

## How long we keep data

Care data is kept as long as the elder's profile exists. Deleting a profile deletes the
medicines, schedules, contacts, readings, and alerts belonging to it.

## Deleting your account and data

You can request deletion of your account and all associated data at any time by emailing
the address on the app's Play store listing from the Google account you signed in with. We
will delete it within 30 days and confirm by email. Guardians can also delete an individual
elder profile from inside the app.

## Children

Care Companion is not directed at children and is not intended for anyone under 18. We do
not knowingly collect data from children.

## Security

All traffic uses HTTPS. Database access is enforced per-row, so an account can only reach
the profiles it has been explicitly linked to. Sign-in is handled by Google; we never see
your password. No system is perfectly secure, but we take these measures seriously and act
promptly on any issue we become aware of.

## Changes

If we change this policy we will update the date at the top and, for material changes, tell
you in the app.

## Contact

Questions, requests, or complaints: use the developer email address on the Care Companion
Google Play store listing.
