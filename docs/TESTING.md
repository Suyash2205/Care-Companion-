# Testing CareCompanion

Everything here runs on the **JVM — no emulator, no device, no network**.

```bash
./gradlew :app:testDebugUnitTest    # the full unit + UI suite
./gradlew :app:lintDebug            # Android Lint (0 errors expected)
```

Test report: `app/build/reports/tests/testDebugUnitTest/index.html`

## What is covered

| Area | Location | Proves |
|---|---|---|
| **Offline dose durability** | `data/OutboxOfflineDurabilityTest`, `fixes/OutboxStoreConcurrencyTest` | A "Taken" tap is never lost: it persists offline, survives an app restart, is retained while the server keeps failing, drains once it recovers, and — critically — an item enqueued *while a flush is in flight* is not erased. |
| **Medicine reminders** | `notify/ReminderSchedulerTest`, `notify/BootPersistenceTest`, `fixes/DailyArmWorkerSchedulingTest` | Alarms fire at the right time, past doses are dropped, re-arming replaces rather than stacks, RTC_WAKEUP is used, reminders survive a reboot, and the daily worker re-arms them without the elder opening the app. |
| **Push / alarm delivery contract** | `notify/PushAndAlarmContractTest` | The `cc_alerts` channel the server targets exists and is IMPORTANCE_HIGH (a mismatch means Android silently drops the notification), channels are created on demand if a push arrives before first launch, and both dose-builders agree on the alarm key so a dose is never armed twice. |
| **Session resilience** | `fixes/AuthInterceptorRetryTest` | A 401 triggers exactly one force-refreshed retry; a successful request does not retry. |
| **Network resilience** | `data/SchemaDriftToleranceTest`, `MalformedResponseTest`, `HttpErrorCodeTest`, `NetworkFailureTest` | Unknown JSON fields are ignored (server schema can add columns safely), and malformed payloads, 401/403/404/409/500/503, timeouts and dropped connections all fail gracefully instead of crashing. |
| **Elder UI (Compose, on JVM)** | `ui/elder/ElderExperienceTest` | Mis-dial protection (tapping a contact opens a confirmation, never dials directly), silhouette fallback for photoless contacts, the correct empty states, Hindi/Marathi rendering, and high-contrast + enlarged fonts. |
| **Domain logic** | `logic/VitalsSeverityTest`, `DayBitmaskTest`, `DtoRoundTripTest`, `ElderStringsI18nTest` | Vitals severity boundaries, Mon=0 day bitmask, DTO round-trips, and — importantly — that every translated string has the same `%s`/`%d` count in all four languages (a mismatch crashes `String.format` at runtime). |
| **Security / RLS** | Backend suite (Supabase) | 58 assertions: guardians see only their linked elders, elders see only their own data, only the owner can invite/remove/deactivate, and a stranger cannot claim an elder profile. |

## Notes for future work

- **Robolectric's `getScheduledAlarms()` returns a copy** — `.clear()` on it is a no-op. Drain with `getNextScheduledAlarm()` in a loop.
- **Robolectric cannot move the wall clock** that `ReminderScheduler` reads, so alarm timing assertions are written *relative* to the real "now" rather than pinned to a fake date.
- `"SOS"` is intentionally identical in all four languages (international distress signal) and is allowlisted in the i18n fallback-detection test.

## What these tests deliberately do NOT prove

Three things are properties of external systems and cannot be verified without real hardware:

1. **FCM actually delivering** a push — depends on Google's infrastructure and device network state.
2. **Alarms surviving OEM battery optimisation** — OnePlus/Xiaomi/Samsung kill background work aggressively. The code uses the correct APIs (`setExactAndAllowWhileIdle`, `USE_EXACT_ALARM`), but no test can prove how a given OEM treats it.
3. **SMS reaching the recipient** — carrier-dependent.

Everything testable without hardware is tested here. If a push or reminder fails on a real device, the cause will be one of the three above, not a defect in this code.
