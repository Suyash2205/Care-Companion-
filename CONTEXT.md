# CareCompanion — Ubiquitous Language

## Roles & identity

- **Guardian** — a person who manages care for one or more Elders from their own device. Authenticates with their own phone number (OTP). A user is either a Guardian or an Elder, never both (dual-role is out of scope for v1).
- **Elder** — the person receiving care. Logs in on their own device with their own phone number; sees the large-type elder experience.
- **Elder Profile** — the record a Guardian creates that represents an Elder (name, photo, medicines, contacts…). Exists before the Elder has ever logged in.
- **Verified (elder profile)** — the Elder's phone number was proven by OTP *at profile setup, on the Guardian's device*. Only a Verified profile's Elder can log in. Changing the number requires re-verification.
- **Unverified (elder profile)** — saved but the OTP step hasn't completed. Guardian-side management works; Elder login is impossible.
- **Linking** — the association of a logged-in Elder to their Elder Profile. Automatic: the OTP done at profile setup pre-creates the Elder's auth identity (same phone → same identity), so first login self-links. There are no invite codes.

## Reminders & adherence

- **Medicine Schedule** — when a specific medicine is taken (slot label, time, Mon–Sun days). The *only* source of medicine reminders. Belongs to a Medicine.
- **Reminder** — a non-medicine recurring prompt (Water, Walk, Vitals, or a custom category) with Done/Not-done responses.
- **Reminder Category** — grouping for Reminders. "Medicine" appears as a category in the UI but is virtual: it is a read-only view over Medicine Schedules, and no free-form Reminder can be created in it.
- **Adherence Log** — one elder response (taken/skipped) or system timeout (missed) for one due occurrence of a Medicine Schedule or Reminder. Exactly one log per occurrence — never two prompts for the same event.
- **Grace Window** — the period after a due time (default 60 min, server-configurable) during which an occurrence is merely *pending*. Only after it lapses with no response is the occurrence **Missed**.
- **Missed** — a system verdict, not an elder action. An elder response timestamped inside the Grace Window always overrides a Missed verdict; any alert already sent to guardians receives a visible correction (never silently disappears).

## SOS

- **SOS Event** — one confirmed emergency trigger by an Elder. Confirmation is a short countdown ("Sending in 5… CANCEL"), not a yes/no dialog; cancelling during the countdown records nothing sent.
- **Emergency Contact** — a contact flagged to receive the SOS SMS. The SMS (with location link when GPS is available) is the primary channel and must go out even with no internet; the database record and guardian push notification are best-effort and may arrive later.

## Connectivity

- **Daily loop (elder)** — seeing today's medicines, receiving reminders, responding Taken/Skip, viewing/calling contacts, triggering SOS. Must work fully offline; reminder alarms fire from local data only.
- **Outbox** — the queue of elder-originated writes (adherence logs, vitals, SOS events) awaiting sync. Guardians have no outbox: guardian edits require connectivity and fail loudly.

## Localization & accessibility

- **Elder languages** — English, Hindi, Marathi, Gujarati. The entire elder experience (including reminder notifications and SOS screens) renders in the elder's chosen language.
- **Guardian language** — English only (v1). All strings live in Android resources for both roles, so adding guardian languages later is translation work only.
- **Font scale (elder)** — an in-app Small/Medium/Large setting in elder settings, independent of the system setting.

## Vitals

- **Vital Reading** — one measurement (BP, sugar, temperature, or pulse) entered by an Elder or Guardian. Sugar readings carry a context (fasting / post-meal).
- **Severity badge** — normal / warning / high, computed from global thresholds keyed by (type, context); for paired values (BP) the worse value decides. Thresholds are system-wide in v1, not per-elder.
- **Vitals Report** — the shareable PDF: elder header, guardian-chosen date range, one color-coded table per vital type. A doctor-visit artifact, not an analytics document.

## Entertainment

- **OTT Shortcut** — a tile on the elder home that launches an installed app. Guardians enable them from a preset catalog (known apps with bundled logos) or add a custom link (letter tile). If the app isn't installed, the elder sees a friendly "ask your family member" message — the install job belongs to the Guardian.

## Care relationships

- **Owner** — the Guardian who created an Elder Profile. Only the Owner can invite/remove other guardians, change access levels, change the Elder's verified phone number, or deactivate the profile. Not transferable (v1).
- **Linked family member** — an additional Guardian on an Elder Profile. **Edit** access = full care management (medicines, schedules, reminders, contacts, vitals) without membership/identity powers. **View** access = read-only care data. *Every* linked guardian, regardless of level, receives all alerts including SOS — alerting follows the relationship, not the permission.
- **Pending (link)** — a family member invited by phone number who hasn't logged in yet; becomes active automatically on that number's first guardian login.
- **Deactivated (elder profile)** — a soft, Owner-reversible state: the Elder can no longer log in (their device shows a contact-your-family screen and cancels all local alarms), no reminders or alerts are generated, and all history remains readable to linked guardians. Permanent deletion is not an in-app operation (v1).
