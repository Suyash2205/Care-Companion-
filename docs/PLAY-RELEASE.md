# Google Play release guide — Care Companion

Everything needed to publish, in the order the Play Console asks for it.
Build config, permissions, and signing are already done — sections 1–3 below are code that
has shipped; sections 4 onward are console work only you can do.

---

## 1. Build configuration (done)

| Item | Value |
|---|---|
| Application ID | `com.carecompanion.app` |
| `versionCode` / `versionName` | `2` / `1.2` |
| `minSdk` / `targetSdk` / `compileSdk` | 24 / **36** / 36 |
| AGP / Gradle | 8.9.1 / 8.11.1 |
| `allowBackup` | `false` (health data must not sync to the user's Drive) |

Play requires new uploads to target API 36; the previous target of 34 would have been
rejected at upload.

## 2. Permissions (done)

Declared, all either normal or runtime-prompted with a visible reason:

`INTERNET`, `ACCESS_NETWORK_STATE`, `READ_CONTACTS`, `POST_NOTIFICATIONS`,
`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `SCHEDULE_EXACT_ALARM`,
`USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`

**Removed:** `SEND_SMS` and `CALL_PHONE`. Both are restricted permissions requiring a
Permissions Declaration Form, and `SEND_SMS` is granted almost exclusively to default
messaging apps — declaring it was the single largest rejection risk. The app now hands the
emergency text to the phone's own messaging app (`ACTION_SENDTO`) and calls through the
dialer (`ACTION_DIAL`), neither of which needs any permission.

### The one declaration you still have to fill in

**`USE_EXACT_ALARM`** — the console will ask why. Answer:

> Care Companion is a medication reminder app for elderly users. Its core function is
> alerting the user at the exact prescribed time to take a specific medicine. A delayed or
> batched alarm would deliver the reminder at the wrong time, which for medications such as
> insulin or blood-pressure drugs makes the reminder useless or unsafe. Exact alarms are
> used only for user-scheduled medication reminders.

This is the approved use case for the permission and should pass.

## 3. Signing (done)

An upload keystore has been generated:

- Keystore: `keystore/carecompanion-upload.jks`
- Alias: `carecompanion`
- Credentials: `keystore.properties` in the project root
- Both are **git-ignored** — they are not in the repository

> ### Back this up now
> If you lose `carecompanion-upload.jks` you can no longer update this app under this
> listing. Copy both `keystore/` and `keystore.properties` somewhere safe and private (a
> password manager or an encrypted drive — **not** a public repo). Recovering from a lost
> upload key requires a support request to Google and is not guaranteed.

### Firebase fingerprints — important

Google Sign-In only works for builds signed with a certificate registered in Firebase.

- ✅ Debug key `9120d76b…` — registered (already there)
- ✅ Upload key `f8ebba64c9eb764a6c44a343fc45f1b50d631411` — **registered** (added for this
  release, so directly-installed release APKs can sign in)
- ⬜ **Play App Signing key — you must add this after your first upload.**

When you enable Play App Signing (the default), Google re-signs the app with *its own* key
before delivering it to users. That key's SHA-1 is different from the upload key's, so
**Google Sign-In will fail for everyone who installs from Play until you register it.**

After your first upload:

1. Play Console → your app → **Test and release → Setup → App signing**
2. Copy the **SHA-1** under "App signing key certificate"
3. Firebase Console → Project settings → Your apps → Android → **Add fingerprint**
4. Download the refreshed `google-services.json` and replace `app/google-services.json`

This is the single most likely cause of "sign-in worked in testing but not from Play".

## 4. Data safety form

Answer **yes** to collecting data, **yes** to encryption in transit, and **yes** to users
being able to request deletion. Then declare:

| Data type | Collected | Shared | Required | Purpose |
|---|---|---|---|---|
| Name | Yes | No | Required | App functionality, Account management |
| Email address | Yes | No | Required | App functionality, Account management |
| User IDs | Yes | No | Required | App functionality |
| Photos | Yes | No | Optional | App functionality |
| Phone number | Yes | No | Optional | App functionality |
| Address | Yes | No | Optional | App functionality |
| Approximate location | Yes | No | Optional | App functionality |
| Precise location | Yes | No | Optional | App functionality |
| Contacts | Yes | No | Optional | App functionality |
| Health info | Yes | No | Optional | App functionality |
| Other personal info (age) | Yes | No | Optional | App functionality |

Notes for the reviewer, if a field allows free text:

- Location is collected **only** at the moment the user presses SOS; there is no background
  location collection.
- Contacts are read only when the guardian picks one from the address book; the book is not
  uploaded or indexed.
- "Shared" is No throughout: data goes only to our own backend (Supabase) and Firebase,
  which are processors, not third-party recipients.

Also tick **"Data is encrypted in transit"** and **"Users can request that data be
deleted"**.

## 5. Health apps declaration

Play asks a separate set of questions for apps handling health data. Care Companion:

- Is a **medication reminder and care-coordination app**
- Does **not** provide diagnosis, treatment advice, or dosage recommendations
- Does **not** connect to medical devices
- Is **not** a regulated medical device and makes no clinical claims

The vitals screen shows Normal / Borderline / High bands. If asked, describe these as
general wellness reference ranges shown for the user's own record-keeping, not as medical
advice — the app never tells anyone to change a medication.

## 6. Privacy policy

`docs/PRIVACY-POLICY.md` is written and ready. It needs a **public URL**.

Fastest option — GitHub Pages on the repo you already have:

```bash
mkdir -p docs-site
cp docs/PRIVACY-POLICY.md docs-site/index.md
git add docs-site && git commit -m "Add privacy policy site" && git push
```

Then Settings → Pages → Source: `main` / `/docs-site` → the URL becomes
`https://suyash2205.github.io/Care-Companion-/`. Paste that into the console.

## 7. Store listing assets — you need to produce these

This is the remaining work I could not do for you:

- **App icon** — 512×512 PNG (the launcher icon exists in the project but Play wants a
  standalone hi-res upload)
- **Feature graphic** — 1024×500 PNG, required
- **Phone screenshots** — 2 to 8, minimum 320px on the short side. Take these on a real
  device: elder home, medicines, SOS, guardian dashboard, elder profile with the connect
  code
- **Short description** — max 80 characters
- **Full description** — max 4000 characters

Suggested short description:

> Medicine reminders, vitals and one-tap SOS — for elders and the family caring for them.

## 8. Other console items

- **Content rating questionnaire** — answer honestly; this app should come out "Everyone"
- **Target audience** — 18+; do not tick any child age band
- **Ads** — declare **no ads**
- **App access** — the reviewer cannot get past sign-in without help. Provide either a test
  Google account with a pre-made elder profile, or instructions plus a live connect code.
  Not doing this is a common cause of rejection
- **Government apps / financial features** — no to both

## 9. Build the upload artifact

```bash
JAVA_HOME=$HOME/.carecompanion-toolchain/jdk ANDROID_HOME=$HOME/Android/sdk \
  ./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab` — this is what you upload. Play
does not accept APKs for new apps.

## 10. Recommended before a public launch

Not blockers, but worth doing:

- **Internal testing track first.** Upload the AAB, add yourself as a tester, install from
  Play, and confirm Google Sign-In works with the Play signing key registered (§3). This is
  the one thing that cannot be verified any other way.
- **Enable R8** (`isMinifyEnabled = true`). Currently off, so the app ships unobfuscated at
  ~21 MB. Turning it on needs keep rules for kotlinx-serialization, Retrofit, and Hilt, and
  needs a real device test afterwards — worth a dedicated pass rather than a rushed one
  before submission.
