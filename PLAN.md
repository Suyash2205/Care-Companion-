# CareCompanion — Full-Stack Build Plan

> **Grilled 2026-07-11 — binding decisions (override anything below that contradicts them):**
> 1. **Auth**: Firebase Phone Auth + Supabase third-party-auth bridge. Day-one spike: login → RLS query returns correct rows, before anything else. Test numbers for dev + demo backup; real SMS for live demo (10/day quota — dry-run the day before).
> 2. **Elder identity**: *verified-at-creation*. Guardian enters elder's phone during profile setup; OTP to the elder's phone is verified right there (secondary FirebaseAuth instance → capture UID, sign out). Elder's later login auto-links by UID. **No invite codes** (`invite_codes` table deleted). Profile saves as **Unverified** if OTP can't complete (guardian-side works; elder login blocked until verified). Number change ⇒ re-verify.
> 3. **Reminders**: "Medicine" category is **virtual** — a read-only view over medicine schedules; free-form reminders only for Water/Walk/Vitals/custom. One prompt per occurrence, ever.
> 4. **SOS**: countdown confirm (5s, giant CANCEL) → GPS with ~5s timeout → **SMS first** (works offline) → best-effort DB insert + FCM via outbox. **No auto-call**; "Alert sent" screen shows a giant Call button.
> 5. **Missed**: 60-min grace window (re-nag +20), server-configurable. Elder response inside window overrides `missed`; guardian alerts get visible corrections, never silent deletion.
> 6. **Access**: owner + members. Owner-only: invites, access levels, phone change, deactivate. Edit = care CRUD. View = read-only. **All** linked guardians get all alerts incl. SOS. Pending link resolves on invited number's first login.
> 7. **Offline**: elder offline-first (daily loop from Room, images pre-fetched, outbox for logs/vitals/SOS); guardian online-required (stale-cache banner, no offline writes).
> 8. **Vitals**: global thresholds keyed (type, context); worst-value-wins badges; PDF = tabular doctor-visit artifact, no charts. Per-elder thresholds = future scope.
> 9. **i18n**: elder fully en/hi/mr/gu; guardian English-only. All strings in resources. Elder font-scale = in-app 3-step; contrast = bold-text toggle.
> 10. **Voice calling: CUT** (photo tiles suffice). Remove SpeechRecognizer + RECORD_AUDIO everywhere. Future scope in report.
> 11. **OTT**: preset catalog (package launch, bundled logos) + custom URL (letter tile). Not-installed ⇒ friendly message, never Play Store. `thumb_url` removed.
> 12. **Wheelchair**: admin-seeded master list (demo city) + contacts taggable `is_service` shown on the same screen. No new CRUD.
> 13. **Deactivation**: soft + owner-reversible; elder device locks out & cancels alarms on sync; history stays readable; real deletion out of scope.
> 14. **Process**: mockups already exist (no Phase-0 gate); `design-system/` landed and synced. QA on **two emulators** (emulator↔emulator SMS via port numbers; Firebase test numbers for OTP). **Timeline: single overnight build** — phases below become one compressed sequence; demoable vertical slice (login → guardian CRUD → elder real data + adherence) is the must-have bar, then SOS, reminders engine, vitals+PDF, OTT/wheelchair/polish in that order.
>
> Domain vocabulary: see `CONTEXT.md`.

Goal: take the current Compose UI prototype to a **fully functional, production-grade app** covering every module in the project proposal, report, and expected-screens list — at **zero/near-zero running cost**.

Sources consulted:
- `CareCompanion_Report_Formatted.pdf` (project report — modules, objectives, future scope)
- `Care Companion – Updated Project Proposal.docx` (functional modules, incl. Vitals + PDF export, generic reminder engine, OTT shortcuts, wheelchair assistance)
- `Scrrens Expected.docx` (complete screen inventory, both roles)
- Figma "Home Screen" file (initial visual design; confirms SOS confirm→sent/cancelled flow)
- Current codebase (all 21 Kotlin files reviewed)

---

## 1. Tech stack (chosen for best-fit + free tier)

| Concern | Choice | Why / cost |
|---|---|---|
| App | **Kotlin + Jetpack Compose** (keep current code) | Proposal says Java, but the existing codebase is already Compose; rewriting to Java is pure waste. Free. |
| Auth | **Firebase Phone Auth (OTP)** | Real SMS OTP; free tier ~10 live SMS/day + unlimited **test phone numbers** for dev. |
| Database | **Supabase Postgres** | Free tier: 500 MB DB, Row Level Security, Realtime, Edge Functions, pg_cron. |
| Auth bridge | **Supabase third-party auth (Firebase as JWT issuer)** | RLS policies read the Firebase UID from the token; no custom token-exchange server. |
| Images | **Supabase Storage** (1 GB free) + **Coil** for loading | Replaces device-local `Uri`s that can't cross devices. |
| Push | **FCM** (free, unlimited) triggered by Supabase Edge Functions / DB webhooks | SOS alerts, missed-dose alerts to guardians. |
| Reminders | **AlarmManager (exact)** + POST_NOTIFICATIONS, re-register on boot; WorkManager for sync | On-device, works offline, free. |
| Offline cache | **Room** mirror of elder's own data + outbox queue for logs | Elder flow must survive no-network. |
| Maps/location | **FusedLocationProvider** for SOS GPS; open location via **geo: intent → Google Maps app** (v1); optional Maps SDK for in-app map later (mobile SDK map loads are $0 but need a billing account) | Zero cost, zero API-key friction for v1. |
| Vitals PDF | Android **PdfDocument** API, share intent (WhatsApp/email) | On-device, free. |
| Voice calling | Android **SpeechRecognizer** ("Call Ramesh") | On-device, free. |
| Navigation | **Navigation Compose** (replaces sealed-class `when` in MainActivity) | Back stack + notification deep links. |
| DI | **Hilt** | Standard, keeps repositories testable. |
| Design | **design-system/ package** (being extracted by another session — treat as read-only source of truth for tokens/colors/typography) + **Claude Design** for new-screen mockups | All new screens get a mockup approved by Suyash before implementation. |

**Total monthly cost: ₹0** while in free tiers. Only real-world cost trigger: >10 live OTP SMS/day (Firebase Blaze pay-per-SMS) — avoidable during dev with test numbers.

---

## 2. Data model (Supabase)

```
users               (id, firebase_uid, phone, role[guardian|elder], name, photo_url, fcm_token, language)
elders              (id, name, photo_url, avatar_key, dob?, address?, is_active, created_by)
guardian_elder_links(guardian_id, elder_id, access[view|edit], status[pending|active])
elder_devices       (elder_id, user_id)              -- which logged-in user IS this elder
invite_codes        (code, elder_id, expires_at)     -- elder onboarding + linking extra family
contacts            (id, elder_id, name, phone, photo_url, is_emergency, sort)
ott_shortcuts       (id, elder_id, title, url_or_package, thumb_url, sort)
medicines           (id, elder_id, name, dosage, form, instructions, with[water|milk], meal[before|after],
                     is_active, pill_url, packet_front_url, packet_back_url)
medicine_schedules  (id, medicine_id, label[Breakfast|Lunch|Dinner|Custom], time, days[Mon..Sun bitmask], enabled)
reminder_categories (id, elder_id?, name, icon, is_default)   -- Medicine, Water, Walk, Vitals + custom
reminders           (id, elder_id, category_id, title, times[], repeat[daily|days|interval], enabled)
adherence_logs      (id, elder_id, source[schedule|reminder], source_id, due_at, status[taken|skipped|missed], responded_at)
vitals              (id, elder_id, type[bp|sugar|temp|pulse], value_1, value_2?, context[fasting|post_meal]?, note?, taken_at, entered_by)
sos_events          (id, elder_id, triggered_at, lat, lng, address?, status[active|cancelled|dismissed|resolved])
alerts              (id, elder_id, guardian_id, kind[sos|missed_dose|reminder_missed], ref_id, read, created_at)
wheelchair_places   (id, name, phone?, lat/lng or maps_url, kind[service|place])  -- master data
```

**RLS**: guardians read/write only linked elders; elder devices read own elder's data + write only logs/vitals/sos. Vitals normal/warning/high thresholds computed in app from a config table.

**Realtime subscriptions**: `sos_events`, `adherence_logs`, `alerts` → guardian dashboard updates live.

**Server-side (Edge Functions + pg_cron)**:
- `on sos_events insert` → FCM to all linked guardians + SMS text body for elder device to send
- pg_cron every 5 min → mark overdue schedules `missed`, insert `alerts`, FCM to guardians
- weekly adherence summary generation (report "future scope" item)

---

## 3. Screen inventory (from Scrrens Expected.docx → build status)

**Common**: Splash ✚, Login (exists, wire real OTP ✎), Role selection (exists ✎), Permissions onboarding screen ✚
**Guardian**: Dashboard w/ elder selector (exists ✎), Elder profiles list/add/edit/deactivate (add=exists, edit+deactivate ✚), Linked family members + access mgmt ✚, Medicine list/add/edit (add=exists, edit ✚), Schedule builder w/ **day-wise Mon–Sun** ✚ (current is time-only), Schedule summary today/week ✚, Reminder categories + custom ✚, Reminders CRUD ✚ (current is hardcoded), Vitals dashboard/history/detail/PDF export ✚, Contact shortcuts (exists ✎), OTT shortcuts config ✚, Emergency contacts + SOS settings + SOS history ✚, Adherence summary/detail/logs ✚, Settings ✚
**Elder**: Home w/ big tiles (exists ✎), Today's medicines + step-through (exists, wire to real data ✎), Reminder popups w/ Done/Not-done ✚, Vitals entry + history ✚, Contacts photo tiles (exists ✎), Voice calling ✚, OTT tiles → real intents ✎, SOS confirm → sent/cancelled screens ✚ (per Figma), Settings (font size/contrast) ✚

✚ = new screen (needs Claude Design mockup + approval) ✎ = exists, needs rework/wiring

---

## 4. Phases

### Phase 0 — Design & foundations (gate: Suyash approves mockups)
- Wait for/consume `design-system/` + `.design-sync/NOTES.md` (other session; **do not edit those paths**)
- Produce Claude Design mockups for every ✚ screen (batch 1: elder-side; batch 2: guardian-side) → **approval gate**
- Create Firebase project (Phone Auth + FCM) and Supabase project; commit config templates (`google-services.json` git-ignored)
- Repo hygiene: `.gitignore`, move reference PNGs to `docs/`, version catalog, Hilt + Navigation Compose + Room + Coil + supabase-kt + firebase-bom deps

### Phase 1 — Auth + architecture skeleton
- Real Firebase OTP flow (send, verify, resend w/ cooldown, error states), test numbers for dev
- Role selection persisted to `users`; session restore on launch (splash decides route)
- Supabase third-party auth wiring; RLS policies deployed (SQL migrations in `supabase/`)
- Refactor: Navigation Compose graph, Hilt, repository layer, ViewModels per screen (UI composables mostly unchanged)
- Logout, account deletion stub

### Phase 2 — Core data online (guardian CRUD)
- Elders: create/edit/deactivate, photo upload to Storage
- Linked family members: invite second guardian by phone, access levels
- Contacts + OTT shortcuts CRUD w/ photos/thumbnails
- Medicines CRUD w/ 3 images; **day-wise schedule builder** (Mon–Sun, multiple slots, proper time picker — kill free-text time field)
- Generic reminder engine: default categories (Medicine/Water/Walk/Vitals) + custom categories + reminders CRUD
- Room cache + sync for all of the above

### Phase 3 — Elder link + elder experience on real data
- Invite-code / phone-match onboarding: elder login → linked to their `elders` row
- Elder home pulls real contacts, OTT shortcuts, medicines (delete all hardcoded demo data)
- Medicine step-through flow writes `adherence_logs`
- Reminder popups (Done / Not done) writing logs
- One-tap + voice calling (dial intent; direct call w/ CALL_PHONE where granted)
- OTT tiles open apps/URLs via intents
- Elder settings: font scale, contrast, language (merge `tr()` into resource qualifiers: en/hi/mr/gu across BOTH roles)

### Phase 4 — Reminders engine on-device
- AlarmManager exact alarms from schedules/reminders; full-screen high-priority notifications w/ pill image + Taken/Skip actions
- Boot receiver re-registration; Doze handling; notification permission onboarding
- Offline outbox: logs queue in Room, sync when online

### Phase 5 — SOS + alerts + tracking
- SOS: confirm screen ("Are you sure?") → capture GPS → insert `sos_events` → auto-SMS to emergency contacts (SEND_SMS) + call primary → "Alert sent" / "SOS cancelled" screens (per Figma)
- Guardian: FCM push → SOS alert screen w/ timestamp, address (reverse geocode), location opens in Maps; dismiss/resolve states
- Missed-dose server cron → alerts feed + push
- Guardian adherence screens: daily summary, medicine detail (day-wise, timestamps), reminder completion log; weekly summary
- SOS settings (recipients, message template) + SOS history

### Phase 6 — Vitals + wheelchair + polish
- Vitals entry (elder + guardian), color-badge dashboard, tabbed history w/ date filters, detail view
- PDF export (PdfDocument) + share sheet
- Wheelchair assistance: master-data list of services/places → dialer + Google Maps intents (per proposal: no SDK)
- Real app icon, splash, dark theme audit, Coil image downsampling
- End-to-end QA on 2 physical devices (guardian + elder), release build + ProGuard, signed APK

Each phase = a PR-sized set of small commits, verified on-device before moving on.

---

## 5. Permissions matrix (request-in-context, with onboarding explainer screen)

| Permission | When asked | Role |
|---|---|---|
| POST_NOTIFICATIONS | onboarding (mandatory per spec) | both |
| READ_CONTACTS | on "Import from contacts" | guardian |
| ACCESS_FINE_LOCATION | first SOS setup / trigger | elder |
| SEND_SMS | SOS setup | elder |
| CALL_PHONE (optional; else ACTION_DIAL) | first direct-call | elder |
| RECORD_AUDIO | first voice-call use | elder |
| SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM | reminder setup | elder |
| Camera (via photo picker — no permission needed) | — | guardian |

---

## 6. Risks / open decisions

1. **Firebase OTP live SMS quota** (10/day free) — fine for dev/demo; real deployment needs Blaze (pay-per-SMS, still cheap) or fallback to Supabase email OTP.
2. **SEND_SMS Play Store policy** — auto-SMS on SOS is allowed for emergency use cases but flagged in review; for a college/demo distribution (APK) it's a non-issue.
3. **Exact alarms on Android 14+** need `SCHEDULE_EXACT_ALARM` grant; fallback to inexact + high-priority push.
4. **design-system/ not yet present** — Phase 0 blocks on the other session landing it; mockups will use its tokens.
5. **Elder identity**: decided — elder logs in with their own phone number; invite code links device→elder profile (name-matching removed).
6. GPS indoors is imprecise (noted in report) — always send maps link + address text, never rely on pin alone.
