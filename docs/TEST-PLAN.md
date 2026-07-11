# CareCompanion — Full Test Plan

Exhaustive test cases for a confidently-shippable APK. Run each on the relevant role; **cross-device (XDEV)** cases need a guardian device + an elder device (two emulators, or one device switching logins). Legend: **P** = pass expected, **fix-first** = if it fails, stop and fix before continuing.

**Test accounts (Firebase test numbers — no real SMS):**
- Guardian: `+91 98765 00001` / `111111`
- Elder: `+91 98765 00002` / `222222`
- Second guardian / elder: `+91 98765 00003` / `333333`

> ⚠️ Elder linking now works by **phone match**: create the elder profile with the *same number* the elder will log in with.

---

## 1. Authentication & session (AUTH)
| ID | Steps | Expected |
|----|-------|----------|
| AUTH-01 | Launch app first time | Splash → Login screen (role cards, phone field) |
| AUTH-02 | Guardian role → phone `9876500001` → Get OTP → `111111` → Login | Lands on guardian dashboard (via one-time permissions screen) |
| AUTH-03 | Force-close & reopen app while logged in | Opens straight to dashboard (session persists), no re-login |
| AUTH-04 | Log out → log back in as **Elder** (`9876500002`/`222222`) | Shows **phone field** (NOT the OTP field), logs in as elder *(regression: the stale-OTP bug)* |
| AUTH-05 | On login, tap Elder then tap Guardian repeatedly | Role toggles cleanly; always resets to phone step |
| AUTH-06 | Enter phone < 10 digits → Get OTP | Button disabled / no request |
| AUTH-07 | Enter wrong OTP code | "Incorrect code" error, stays on OTP step |
| AUTH-08 | Resend OTP link | Clears the code field |
| AUTH-09 | Log out from guardian Settings and from elder Settings | Returns to login both times |

## 2. Permissions onboarding (PERM)
| ID | Steps | Expected |
|----|-------|----------|
| PERM-01 | First login (either role) | One-time "A few permissions" screen appears |
| PERM-02 | Guardian: Continue | Requests Notifications only |
| PERM-03 | Elder: Continue | Requests Notifications + Location + SMS + Call |
| PERM-04 | Skip for now | Proceeds to the app without granting |
| PERM-05 | Second login (same role) | Onboarding does NOT show again |

## 3. Guardian — elder profiles (GRD-ELD)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-ELD-01 | Dashboard with no elders | Empty state "Add an elder profile" |
| GRD-ELD-02 | Add elder: name, age, address, phone `9876500002`, photo | Saves; appears in selector; status card shows name |
| GRD-ELD-03 | Add a 2nd elder; use the selector chips | Switches selected elder; quick actions target the selected one |
| GRD-ELD-04 | Open elder profile → edit name → Save | Name updates on dashboard |
| GRD-ELD-05 | Edit profile → Deactivate | Stays on screen (in-place toggle), status shows "OFF"/deactivated *(regression)* |
| GRD-ELD-06 | Reactivate | Toggles back to active |
| GRD-ELD-07 | Change photo | New photo shows on dashboard + status card |
| GRD-ELD-08 | Add elder with blank name | Save disabled / validation |

## 4. Guardian — contacts (GRD-CON)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-CON-01 | Contacts (empty) | "No contacts yet" |
| GRD-CON-02 | Add contact: name, phone, relation, **photo**, Emergency ON | Saves; card shows photo + "SOS" tag |
| GRD-CON-03 | Add contact without photo | Yellow nudge to add a photo; still saves |
| GRD-CON-04 | Import from device contacts | Name + number auto-filled |
| GRD-CON-05 | Edit contact (pencil) → change name → Save | Updates in list |
| GRD-CON-06 | Delete contact | Removed |

## 5. Guardian — medicines & schedules (GRD-MED)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-MED-01 | Add medicine: name, dosage, form chip, Water, After meal, 3 photos | Saves; card shows details + pill photo |
| GRD-MED-02 | Tap Schedule → pick days (M/W/F), Breakfast, time via picker, With water | Saves; schedule chips show on the medicine card |
| GRD-MED-03 | Add a 2nd schedule slot (Dinner) to same medicine | Both slots show |
| GRD-MED-04 | Toggle medicine Active off | Shows INACTIVE; stops generating reminders |
| GRD-MED-05 | Edit medicine → change dosage → Save | Persists after leaving/returning |
| GRD-MED-06 | Delete medicine | Removed (schedules gone too) |
| GRD-MED-07 | Time picker: set 21:30 | Slot shows 21:30 |

## 6. Guardian — reminders (GRD-REM)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-REM-01 | Reminders screen | Default categories present (Water/Walk/Vitals); Medicine category is read-only |
| GRD-REM-02 | Add a Water reminder with a time; toggle on | Appears in list, enabled |
| GRD-REM-03 | Add a custom category + reminder in it | Custom shows with tag |
| GRD-REM-04 | Toggle a reminder off | Reflects disabled |

## 7. Guardian — vitals & PDF (GRD-VIT)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-VIT-01 | Add BP 150/95 | Shows with **High** badge |
| GRD-VIT-02 | Add BP 118/78 | Shows **Normal** badge |
| GRD-VIT-03 | Add sugar 165 fasting | **High** badge |
| GRD-VIT-04 | Switch tabs BP/Sugar/Temp | History filters by type |
| GRD-VIT-05 | Export PDF | Share sheet opens with a PDF vitals report |
| GRD-VIT-06 | Stat cards | Show latest per type + severity |

## 8. Guardian — adherence (GRD-ADH)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-ADH-01 | Open Adherence (after elder logs doses) | % ring + week strip + per-medicine dots |
| GRD-ADH-02 | Adherence with no data | Sensible empty/zero state |

## 9. Guardian — family / access (GRD-FAM)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-FAM-01 | Family screen | Owner (You) shown |
| GRD-FAM-02 | Invite `9876500003` as View Only | Invite sent; appears pending or active |
| GRD-FAM-03 | Log in as `9876500003` (guardian) | Sees the elder; care data **read-only** |
| GRD-FAM-04 | Invite as Can Edit | That guardian can modify care data |
| GRD-FAM-05 | Owner tries to invite own number | Blocked ("can't change your own access") |

## 10. Guardian — OTT / videos (GRD-OTT)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-OTT-01 | Add from catalog (YouTube) | Appears in configured list |
| GRD-OTT-02 | Add custom link (title + URL) | Appears as a letter tile |
| GRD-OTT-03 | Delete a shortcut | Removed |

## 11. Guardian — SOS settings (GRD-SOS)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-SOS-01 | SOS settings → toggle which contacts get alerts | Emergency flags update |
| GRD-SOS-02 | Edit alert message template → Save | Persists (used by elder SOS) |
| GRD-SOS-03 | SOS history | Lists past SOS events; resolve works |

## 12. Guardian — wheelchair & settings (GRD-MISC)
| ID | Steps | Expected |
|----|-------|----------|
| GRD-MISC-01 | Wheelchair screen | Seeded Mumbai services; Call + Directions open dialer/maps |
| GRD-MISC-02 | Guardian Settings | Account info, notifications toggle, help, logout |

## 13. Elder — linking & home (ELD-HOME)
| ID | Steps | Expected |
|----|-------|----------|
| ELD-HOME-01 | Elder logs in with a number that has a profile | Home shows the elder's name + tiles |
| ELD-HOME-02 | Elder logs in with a number with **no** profile | Clear "no care profile linked to this number" message |
| ELD-HOME-03 | Home tiles | Giant SOS, Medicines (with due badge), Contacts, Vitals, Videos, gear+logout |
| ELD-HOME-04 | Due-today badge | Reflects pending doses count |

## 14. Elder — medicines / adherence (ELD-MED)
| ID | Steps | Expected |
|----|-------|----------|
| ELD-MED-01 | Open Medicines | Today's medicines list (from schedules due today) |
| ELD-MED-02 | Start taking → Taken | Advances; logs "taken" |
| ELD-MED-03 | Not taken | Logs "skipped" |
| ELD-MED-04 | Finish all | "Great job" screen |
| ELD-MED-05 | No meds due today | Friendly "no medicines due today" |

## 15. Elder — contacts (ELD-CON)
| ID | Steps | Expected |
|----|-------|----------|
| ELD-CON-01 | Contacts | Big **photo tiles**; "Tap a photo to call" hint |
| ELD-CON-02 | Contact with no photo | Person **silhouette** (never a letter) |
| ELD-CON-03 | Tap a face | "Call [Name]?" confirmation with the photo + Yes/No |
| ELD-CON-04 | Confirm "Yes, Call" | Dialer opens with the number |
| ELD-CON-05 | Scroll the list, incidental tap | A confirmation appears — **never dials directly** *(mis-dial protection)* |
| ELD-CON-06 | "No" on confirmation | Dismisses, no call |

## 16. Elder — vitals / videos / settings (ELD-MISC)
| ID | Steps | Expected |
|----|-------|----------|
| ELD-VIT-01 | Add a BP reading | Saves; shows in history with severity pill |
| ELD-VID-01 | Videos (none configured) | "No videos added yet" *(not "no contacts")* |
| ELD-VID-02 | Videos (configured) | Tiles launch the app/URL; friendly message if app not installed |
| ELD-SET-01 | Settings → Language → हिन्दी | Whole elder UI switches to Hindi (also try मराठी, ગુજરાતી) |
| ELD-SET-02 | Text size Large | Text scales up app-wide |
| ELD-SET-03 | High contrast on | Contrast increases (tiles get borders, darker text) |

## 17. Elder — SOS (ELD-SOS)
| ID | Steps | Expected |
|----|-------|----------|
| ELD-SOS-01 | Tap SOS | 5-second countdown with a giant CANCEL |
| ELD-SOS-02 | Tap CANCEL during countdown | Returns home, nothing sent |
| ELD-SOS-03 | Let countdown finish | "Alert Sent" screen: SMS count, location, big Call button |
| ELD-SOS-04 | Tap SOS a 2nd time in the session | Fresh countdown with CANCEL — NOT a stale "sent" screen *(regression)* |
| ELD-SOS-05 | With a mock GPS set | Location line shows coordinates |
| ELD-SOS-06 | Call button on sent screen | Dialer opens with the emergency contact |

## 18. Cross-device — the core promise (XDEV) — needs both devices
| ID | Steps | Expected |
|----|-------|----------|
| XDEV-01 | Guardian adds a **contact** → elder opens Contacts | Contact appears on the elder (with photo) |
| XDEV-02 | Guardian adds a **medicine + schedule for today** → elder Medicines | Dose appears in today's list |
| XDEV-03 | Elder marks a dose **Taken** → guardian Adherence | Reflected in guardian's adherence |
| XDEV-04 | Guardian adds an **OTT shortcut** → elder Videos | Tile appears |
| XDEV-05 | Guardian edits the **SOS message template** → elder triggers SOS | SMS uses the custom template |
| XDEV-06 | Elder triggers **SOS** → guardian dashboard/SOS + push | SOS alert appears; guardian gets a notification |
| XDEV-07 | Guardian **deactivates** the elder → elder app | Elder is locked out / shows contact-family state |
| XDEV-08 | Elder enters a **vital** → guardian Vitals history | Reading appears with correct severity |
| XDEV-09 | Second guardian (View) added → tries to edit | Read-only enforced |

## 19. On-device reminders & notifications (DEV)
| ID | Steps | Expected |
|----|-------|----------|
| DEV-01 | Schedule a medicine ~2 min ahead; open elder app once | Notification fires at the time with medicine name |
| DEV-02 | Missed dose (don't respond > grace) | Guardian gets a "missed dose" alert |
| DEV-03 | Elder marks late after missed | Guardian gets a correction |
| DEV-04 | Reboot device (if testable) | Today's future reminders re-arm |

## 20. Offline (OFF)
| ID | Steps | Expected |
|----|-------|----------|
| OFF-01 | Airplane mode → elder marks a dose Taken | Accepted (no error), queued |
| OFF-02 | Restore network → reopen elder app | Queued dose syncs to guardian |
| OFF-03 | Guardian offline write | Fails loudly (guardian is online-required) |

## 21. Edge cases & negative (EDGE)
| ID | Steps | Expected |
|----|-------|----------|
| EDGE-01 | Rapid back navigation across guardian screens | No crash; clean back stack |
| EDGE-02 | Rotate device on various screens | State preserved, no crash |
| EDGE-03 | Very long names / big numbers | UI doesn't break (ellipsis/wrap) |
| EDGE-04 | Add duplicate contact/medicine | Allowed or handled gracefully |
| EDGE-05 | Deny a permission then use the feature | Graceful (e.g. SOS still tries, dialer still opens) |
| EDGE-06 | Photo picker cancelled | No crash, no photo set |
| EDGE-07 | Empty required fields on every form | Save disabled |

## 22. Security / data isolation (SEC) — verified via RLS test suite
| ID | Check | Expected |
|----|-------|----------|
| SEC-01 | Guardian sees only linked elders | ✅ (37 backend assertions) |
| SEC-02 | Elder sees only own data | ✅ |
| SEC-03 | Edit-guardian can't change identity/verification columns | ✅ |
| SEC-04 | Only owner invites/removes/deactivates | ✅ |
| SEC-05 | Stranger can't claim an elder profile | ✅ |
| SEC-06 | Elder-only SOS insert; per-guardian alert isolation | ✅ |

---

### How to run cross-device (XDEV) with two emulators
1. Boot two Google-Play emulators. Install the APK on both.
2. On emulator A log in as **Guardian**; create an elder with phone `9876500002`.
3. On emulator B log in as **Elder** with `9876500002` → it auto-links.
4. Make a change on one; refresh the other (guardian alert feed polls; elder re-opens the screen).
