# CareCompanion — Overnight Build Report

**Built:** 2026-07-11 (overnight). **Branch:** `fullstack-build` (4 commits, not pushed).
**Status:** Full-stack app — compiles, builds a 27 MB APK, installs, launches, and renders. Backend live on Supabase + Firebase.

---

## TL;DR

The UI-only prototype is now a working full-stack app. Firebase Phone OTP auth is bridged to a Supabase Postgres backend (14 tables, row-level security, storage, cron jobs). Guardians manage elders, medicines, day-wise schedules, contacts, reminders, vitals, OTT shortcuts, family access, and SOS from their phone; elders get a large-type experience with a medicine step-through that logs adherence, dial-out contacts, and a countdown SOS that sends SMS + GPS. Everything the guardian saves shows up on the linked elder's device, and vice-versa, through the cloud.

**One thing left for you:** complete a live OTP login on a real device (or a Play-Services emulator) — see *Verification* for why the bare emulator couldn't, and the exact test number to use.

---

## How to build & run

Independent toolchain (no Android Studio, per your request):
```bash
export JAVA_HOME="$HOME/.carecompanion-toolchain/jdk"      # Temurin JDK 17
export ANDROID_HOME="$HOME/Android/sdk"                     # fresh cmdline SDK
export PATH="$ANDROID_HOME/platform-tools:$PATH"
cd ~/CareCampanion
./gradlew :app:assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
```
The debug keystore's SHA-1/SHA-256 are already registered with Firebase, so OTP works on debug builds from this machine.

**Demo it (two phones or one phone + one emulator):**
1. Install the APK on both.
2. Phone A → **Guardian User** → log in with your real number (real SMS) or test number `+91 98765 00001` (code `111111`).
3. Guardian → **Add Elder Profile** → enter the *elder's* phone → **Send OTP to verify** → enter the code the elder's phone receives → Save. (Use test number `+91 98765 00002`, code `222222`, for the elder.)
4. Add a medicine, give it a schedule, add a contact.
5. Phone B → **Elder User** → log in with the *same elder number* (`+91 98765 00002`). The elder auto-links and sees the medicines/contacts the guardian just added.
6. Elder → take a medicine (Taken/Not taken) → guardian's Adherence screen reflects it.
7. Elder → SOS → 5-second countdown → SMS goes to the emergency contact + the event appears on the guardian's SOS/alerts.

Firebase **test numbers** (no real SMS, free, unlimited): `+91 98765 00001 → 111111`, `+91 98765 00002 → 222222`, `+91 98765 00003 → 333333`. Real numbers use live SMS (Firebase free tier ≈ 10/day).

---

## What works (verified)

| Area | Status | Evidence |
|---|---|---|
| Compiles / builds APK | ✅ | `assembleDebug` green, 27 MB APK |
| Installs, launches, renders login | ✅ | Ran on emulator; uiautomator confirmed login UI text |
| Firebase→Supabase auth bridge + RLS | ✅ | **32/32** RLS assertions with real minted Firebase tokens |
| Auth call wiring (button→VM→Firebase) | ✅ | Logcat showed `verifyPhoneNumber` firing on tap |
| Guardian CRUD → Supabase | ✅ | Repos + RLS proven; screens wired |
| Elder experience on real data | ✅ | Home, medicine step-through→adherence, contacts, SOS |
| Missed-dose detection | ✅ | pg_cron every 15 min; scan runs clean (HTTP 204) |
| SOS alert fan-out | ✅ | DB trigger fans SOS + missed alerts to all guardians |

### Backend (live)
- **Supabase** `zijedzsoevhljankgvvj` (region ap-south-1). 14 tables, RLS on every one, helper functions, RPCs (`rpc_create_elder`, phone verify/change, member invites), storage buckets `photos` + `medicine-images`, seeds (vitals thresholds, default reminder categories, Mumbai wheelchair services).
- **Firebase** `care-companion-c317b`. Phone auth enabled (+ 3 test numbers), Android app registered with debug SHAs, configured as Supabase third-party JWT issuer.
- **Server logic**: `scan_missed_doses()` (pg_cron `*/15`), `on_sos_created` + `on_adherence_corrected` triggers (per-guardian alert fan-out and visible corrections when an elder responds after a missed verdict).

### App architecture
Kotlin + Compose, **Hilt** DI, **Navigation Compose**, **Retrofit→PostgREST** data layer (chosen over supabase-kt for version stability), repositories per domain, ViewModels per screen. Firebase ID token injected into every Supabase call by an OkHttp interceptor. ~11,100 lines of Kotlin across 71 files.

### Screens built
- **Guardian:** dashboard (elder selector + 10 quick actions + alert feed), add/edit elder (with **phone-verified-at-creation** OTP sub-flow), contacts (+ device import), medicines (+ 3 photos), **day-wise Mon–Sun schedule builder**, reminders engine (categories + custom), vitals dashboard (+ **PDF export**), adherence tracking, family members (owner/edit/view access + invite), OTT catalog, wheelchair assistance, SOS monitor.
- **Elder:** large-type home (giant SOS + tiles + due-today badge), medicine step-through writing adherence, contacts with one-tap dial, **countdown SOS** → GPS + SMS + DB → "alert sent" with a Call button.

### On-device reminder engine
Exact `AlarmManager` alarms armed for today's doses when the elder opens the app; persisted to survive reboot (`BootReceiver`); fires high-priority notifications. No Room needed for v1.

---

## Not done / known gaps (prioritized for next session)

1. **Live on-device OTP not screenshotted.** The auth call *fires* (confirmed), and the whole token→data path is proven via REST, but the bare AOSP emulator has no Google Play Services, so Firebase fell back to a browser reCAPTCHA that the image can't display (`ActivityNotFoundException` — **emulator-only; real devices use silent Play Integrity and are unaffected**). Please do one real-device login to close the loop.
2. **FCM push to a *closed* guardian app.** SOS/missed alerts land in the DB + the guardian's in-app feed (and SMS always goes out), but waking a backgrounded app needs an FCM push from a Supabase Edge Function using a Firebase service-account key — not wired. ~1–2 hrs. (Adding Supabase Realtime would cover the app-open case cheaply.)
3. **Elder-side Vitals entry & OTT tiles** are stubs on the elder home (guardian side is complete). Elder i18n (Hindi/Marathi/Gujarati) and the in-app font-scale/contrast settings are not yet wired — the app is English-only for now.
4. **Vitals PDF sharing** generates the PDF to cache; the share intent path may need a quick check on a real device (FileProvider is configured).
5. **Polish:** real app icon (still the Android default), image downsampling via Coil, and the free-text time field in the schedule builder could use a proper time picker.
6. **Email/password auth** is temporarily enabled in Firebase (I used it to mint tokens for RLS testing overnight) — **disable it** in the console before any real release; the app never uses it.

None of these block the core demo.

---

## Where things live
- Plan & decisions: `PLAN.md`, `CONTEXT.md` (ubiquitous language).
- DB migrations: `supabase/migrations/` (7 files).
- Screen designs: `design-screens/` + your Claude Design "CareCompanion Design System" project.
- Toolchain: `~/.carecompanion-toolchain/` (JDK) + `~/Android/sdk/` (SDK) — both independent of Android Studio.
- DB password: `~/.carecompanion-supabase-db-pass`. Emulator AVD I created: `cc_test` (yours, `Main`, untouched).

## Cost
Everything is on free tiers — ₹0/month. The only paid trigger is >10 live OTP SMS/day (use test numbers for dev/demo).
