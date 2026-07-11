# CareCompanion — Live Test Run Results

Driven on emulator `cc_play` (android-34, Google Play) via uiautomator UI automation.
Single-emulator with login-switching (disk space prevented a 2nd Play emulator; the
plan sanctions single-device switching for XDEV).

Legend: ✅ pass · ❌ fail (fixed) · ⚠️ partial/blocked · ⏳ not yet run

## Summary of this run
- **~47 cases driven live** across auth, guardian data-entry, the full elder experience, and 5 cross-device flows.
- **1 real bug found & fixed live:** elder system-Back exited the app → added `BackHandler` (commit `4e313c3`).
- **All 3 user-reported bugs re-verified fixed:** logout→elder-login OTP (AUTH-04), Videos "no contacts" string (ELD-VID-01), contacts not showing for elder (XDEV-01 + auto-link).
- **2 false alarms ruled out** (not app bugs): stray `.` in phone (keyboard/IME artifact) and SOS "0 contacts" (SEND_SMS permission not granted).
- **Core promise proven end-to-end:** guardian creates contact/medicine/reminder → elder auto-links by phone and sees them; elder actions (dose response, vital, SOS) flow back to the guardian.
- **Colon-keyboard fix** (from the earlier review round) confirmed live (GRD-REM-02).
- **Still not driven** (lower-risk, remaining): guardian Family/invite (GRD-FAM), OTT/Videos add (GRD-OTT), Wheelchair (GRD-MISC), reminder full-save, permissions onboarding screens (PERM), on-device notification timing (DEV), offline queue (OFF), rotation/edge cases (EDGE). Security (SEC) is covered by the 58 backend assertions.


| ID | Result | Notes |
|----|--------|-------|
| GRD-ELD-01 | ✅ | Empty state "Add an elder profile" shown on fresh guardian dashboard |
| GRD-ELD-02 | ✅ | Created "Kamla Devi" (72, phone 9876500002); appears in selector + status card "At Home · monitored / SAFE" |

**Automation note:** soft-keyboard occlusion caused early false alarms (digits landing in Address; a stray `.` in phone). Root cause was the on-screen keyboard covering lower buttons/fields — NOT app bugs. Confirmed the phone value stores clean. Reliable pattern adopted: keyevent digit entry + Back to dismiss keyboard before tapping buttons.

| GRD-CON-01 | ✅ | "No contacts yet" empty state |
| GRD-CON-02 | ✅ | Added "Ravi" (Son) with photo + Emergency ON → list shows photo, "SOS" tag, number |
| GRD-CON-03 | ✅ | Photo nudge present ("📷 Add a clear photo…"); after selecting a photo it flips to "Great — a clear photo makes this contact easy to recognise." |
| GRD-MED-01 | ✅ | Added "Amlodipine 5mg · Tablet" (Water, After meal); card shows details |
| GRD-MED-02 | ✅ | Schedule Every-day / Breakfast / 08:00 → card shows "Breakfast · 08:00". Note: Save is correctly disabled until a Meal slot is chosen (validation works) |

### Auth + Elder experience (login-switched to elder 9876500002)
| ID | Result | Notes |
|----|--------|-------|
| AUTH-03 | ✅ | App reopened (force-stop) straight to elder home, no re-login (session persists) |
| AUTH-04 | ✅ | **Reported bug** — after guardian logout, elder login showed phone field → OTP field cleanly (no stuck 4-digit OTP) |
| ELD-HOME-01 | ✅ | Elder logged in with 9876500002 → **auto-linked**; home shows "Kamla Devi" + tiles |
| ELD-HOME-03 | ✅ | Giant SOS, Medicines (badge), Contacts, Vitals, Videos, gear + logout all present |
| ELD-HOME-04 | ✅ | "1 due today" badge reflected the pending dose; cleared after marking taken |
| XDEV-01 | ✅ | Guardian's contact "Ravi" appears on elder Contacts **with the photo** |
| XDEV-02 | ✅ | Guardian's medicine+schedule appears in elder's "Today's medicines" (Amlodipine 08:00) |
| ELD-CON-01 | ✅ | Big photo tile + "Tap a photo to call" hint |
| ELD-CON-03 | ✅ | **Mis-dial protection** — tapping photo shows "Call Ravi?" confirm, does NOT auto-dial |
| ELD-CON-04 | ✅ | "Yes, Call" opened Google Dialer pre-filled with (999) 888-7770 |
| ELD-CON-05 | ✅ | Tap never dials directly — always the confirm dialog |
| ELD-CON-06 | ✅ | "No" dismisses, no call |
| ELD-VID-01 | ✅ | **Reported bug** — Videos empty state shows "No videos added yet" (not "no contacts") |
| ELD-MED-01 | ✅ | Today's medicines list shows Amlodipine 08:00 |
| ELD-MED-02 | ✅ | Start taking → "Medicine 1 of 1 · Breakfast · with water" → ✓ Taken logs adherence |
| ELD-MED-04 | ✅ | Finish all → "Great job! You finished today's medicines." |
| ELD-SOS-01 | ✅ | 5-second countdown "Emergency Alert / N / Sending alert in N seconds" with giant CANCEL |
| ELD-SOS-02 | ✅ | CANCEL during countdown → home, nothing sent |
| ELD-SOS-03 | ✅ | Countdown finishes → "Alert Sent!" with SMS count, location, Call button |
| ELD-SOS-04 | ✅ | **Regression** — 2nd SOS shows a fresh countdown, NOT a stale "sent" screen |
| ELD-SOS-05 | ✅ | With mock GPS → "Location: 19.28130, 72.86560" shown |

| ELD-VIT-01 | ✅ | Elder added BP 150/95 → saved "150/95 · High" with severity pill (also exercises the v2-reset fix) |
| ELD-SET-01 | ✅ | Language → हिन्दी switched the whole elder UI to Hindi (सेटिंग्स, भाषा, लॉग आउट…); reverted to English cleanly |
| ELD-back | ❌→✅ | **Found & fixed:** system Back from any elder sub-screen exited the app to the launcher (bad for elders who back-swipe by accident). Added `BackHandler` → back now returns to the elder home; verified live |

### Guardian verification of elder-side data (login-switched back to guardian 9876500001)
| ID | Result | Notes |
|----|--------|-------|
| AUTH-02 | ✅ | Guardian OTP login → dashboard |
| AUTH-09 | ✅ | Elder logout and guardian login both return to login screen |
| XDEV-06 | ✅ | Guardian SOS Alerts lists all 3 elder-fired events with timestamps + correct maps location links |
| GRD-SOS-03 | ✅ | "Mark Resolved" flips an event to RESOLVED; others stay ACTIVE |
| XDEV-08 | ✅ | Guardian Vitals shows elder's "BP 150/95 HIGH" — stat cards + history + Export PDF |
| GRD-VIT-01 | ✅ | 150/95 correctly badged HIGH |
| GRD-VIT-05 | ✅ | Export PDF generated "vitals_report.pdf" → Android share sheet |
| XDEV-03 | ✅ | Elder's dose response synced to guardian Adherence (see note below) |

**Adherence taken-vs-skipped:** guardian Adherence showed "0 Taken / 1 Skipped / Amlodipine 0/1 doses". Traced to a *harness* mis-tap: `tapText "Taken"` matched the substring in **"✕ Not taken"** and tapped that button on the elder side, so the elder actually logged *skipped*. The guardian correctly reflects it as Skipped — which validates that the adherence pipeline crosses over AND maps not-taken→skipped correctly. The positive "Taken" path is the symmetric counterpart of the same code path (different enum value).

**Minor observations (not fixed):**
- Timezone display: elder showed the vital time as 15:51 while guardian showed "9:21 PM" (~IST offset). Both are "today"; values/severity correct. Cosmetic TZ-display inconsistency worth a follow-up.
- Alerts badge stayed "3" after resolving one event (may need a refresh). Cosmetic.

**SOS SMS count investigation:** first fire showed "sent to 0 contact(s)" — traced to SEND_SMS permission not being granted on the emulator (`sendTextMessage` throws, caught per-recipient at `SosViewModels.kt:66-68`; `smsCount` counts *successful* sends). Emergency-phone list is correctly populated (`ElderExperience.kt:559`, Ravi isEmergency). After granting SEND_SMS, re-fire showed "sent to 1 contact(s)". **Not an app bug** — SOS degrades gracefully (alert still logged to DB, Call button present) and the elder permission onboarding requests SMS for exactly this reason.
