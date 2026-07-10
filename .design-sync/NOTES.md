# design-sync notes — CareCompanion

- This is NOT the app's own code: the repo is an Android/Compose app. `design-system/` is a hand-built React mirror of the Compose theme (source of truth: `app/src/main/java/com/carecompanion/app/ui/theme/*.kt` and `GuardianSharedComponents.kt`). When the Android theme changes, update `design-system/src/` to match before re-syncing.
- Build: `cd design-system && npm install && npm run build` (tsc → `dist/`). Converter runs with `--node-modules design-system/node_modules --entry design-system/dist/index.js`.
- Tokens are inlined at the top of `src/styles.css` (`:root { --cc-* }`) — a separate tokens.css file doesn't work because `copyTokens` only reads from a separate npm package (`tokensPkg`), and a relative `@import` in cssEntry ships unresolved (`[CSS_IMPORT_MISSING]`).
- No fonts ship by design: the app uses the system sans stack; Wordmark uses Georgia/serif fallback. No `[FONT_MISSING]` expected.
- The DS ships no icon components — previews use inline SVGs (Feather-style, stroke 1.6–2, currentColor).
- All 17 components have authored previews in `.design-sync/previews/`; all graded good on 2026-07-11.

## Known render warns

(none)

## Re-sync risks

- The mirror can silently drift from the Android app — nothing ties the Kotlin theme values to `design-system/src`. Diff `Color.kt` / `GuardianSharedComponents.kt` tokens against `src/styles.css` `:root` block when the app's look changes.
- Preview content (names like Sunita/Aarav, medicine names) is inlined in `.design-sync/previews/*.tsx`; it matches the app's sample data as of 2026-07-11.
- Build assumed node 23 / npm; playwright chromium-headless-shell installed via `.ds-sync` (playwright pinned by whatever `npm i playwright` resolved on 2026-07-11).
