# Migration closure and database entry criteria

Last reviewed: 2026-11-16. This is an evidence-based gap list, not a declaration of full migration completion.

## Implemented local scope

Shared Compose onboarding, pregnancy dating/profile, Home, calendar appointments, independent personal reminders, all six prenatal Records sections, Baby weeks 4–40, appearance preferences and five-destination navigation are implemented. The approved Calma editorial components now cover the remaining screens and forms. Existing fonts and fetal illustrations are preserved.

The app still uses nine protected stores (profile plus eight collections), not a database. Android uses Keystore/AES-GCM and AtomicFile; provisional iOS adapters use Keychain. Appearance is separate. No dependency or architecture replacement was introduced in this closure slice.

## Reference audit

Read-only source: `C:/Users/john0/screen-capture-design-match/src`.

| Reference | KMP result / remaining difference |
| --- | --- |
| `routes/onboarding.tsx`, `lib/pregnancy.ts` | Local onboarding/dating/profile implemented. Demo/account branches pending; no medical demo data is injected into real stores. |
| `routes/index.tsx` | Real profile, next appointments, dated note tasks and first unresolved professional-recorded condition implemented. |
| `routes/calendario.tsx`, `components/app/entities.ts` | Appointment CRUD/preferences and independent reminder CRUD now implemented. Calendar switches between Citas and Recordatorios; each mode marks its own dates rather than combining both kinds in one grid. |
| `components/app/entities.ts`, consultations | All editable consultation fields now include attachment text reference; historical gestational-age text is preserved. Codec v2 reads v1 without rewriting on load. |
| `components/app/entities.ts`, vitals/conditions/vaccines/exams/notes | All six sections have local CRUD and editable fields. Native validation is stricter for malformed dates, completed/applied dates and bounded input. |
| `lib/types.ts`, `lib/demo.ts` | Prototype consultation/note `appointmentId` associations exist in the model/demo, but the generic React editor does not expose them. KMP associations remain a real model/navigation gap; do not mark complete. |
| `routes/perfil.tsx` | Profile/dating editing and appearance implemented. JSON export, reset-all and demo replacement remain pending. Account actions belong to the user-deferred cloud phase. |
| `routes/bebe.tsx`, BabyDevelopmentChat | Offline educational explorer implemented; AI integration remains pending. |

The React reminder hint explicitly says it does not send system notifications. Its attachment/proof fields are strings. Native file attachments and notification delivery are additional work, not features already functioning in the reference. Text references are now supported in consultations, vaccines and exams.

## Executed checks

- Android debug APK built with `--offline --no-configuration-cache --no-watch-fs`.
- Host regression: 5 use-case + 38 repository + 13 ViewModel + 28 UI-helper tests = **84 tests, zero failures**.
- Added reminder coverage: concurrent CRUD/reload, all five kinds, all-day/null-time entries, completion, invalid dates/times/limits, failed I/O, corrupt/unknown/truncated/duplicate/oversized payload non-overwrite, generated IDs and cooperative cancellation.
- Added consultation v1-to-v2 compatibility test: read leaves bytes unchanged, explicit edit upgrades without losing historical fields, attachment Unicode/length validation.
- Samsung SM-A546E over Wi-Fi: installed with `adb install -r`, existing profile and appointment data remained visible, dark Home/calendar inspected. Created a clearly named temporary personal reminder, force-stopped/reopened the app and confirmed the saved record reloaded; edited completion and deleted it through confirmation. Empty state confirmed afterward.
- On that phone the reminder file had the expected authenticated envelope and did not contain the plaintext test title. This is not a tamper test or a proof for every store/platform.
- Samsung read-only navigation smoke also opened all six Records sections, Baby and Profile. Light appearance was inspected and survived process restart; app was left in Dark. Profile Back/discard confirmation returned to Profile without a save. No emulator used. iOS was not compiled or run.

## Remaining gates

1. Full physical-device pass for existing clinical CRUD, date/time dialogs, configuration recreation during drafts/writes, large text, landscape, TalkBack, failure recovery and all theme modes. The reminder smoke test does not verify the whole application.
2. macOS/Xcode compile/run and Keychain verification. Windows cannot close this gate.
3. Implement or explicitly agree to exclude local export/reset, consultation/note appointment associations, demo and AI. These are still open migration items, not implicitly removed from scope.
4. Clinical copy review and illustration distribution rights before release.

## Database work: required design decisions

The user's next objective is database integration. Preserve domain/repository boundaries and replace the data implementation incrementally. Choosing a database is still open; no framework has been selected or installed.

- Keep local data protected. Do not replace encrypted files with a plaintext database merely to gain SQL queries. Decide and test encryption/key availability, backup exclusions and iOS behavior with the chosen driver.
- Import all nine protected stores through their existing decoders. Preserve IDs, ISO civil dates, historical dating/gestational-age text, status values, nulls and text references; theme preferences stay separate.
- Validate the entire source before committing an import. A failed/corrupt/unsupported source must leave both its files and any pre-existing database data intact.
- Commit the import and its completion marker in one transaction. Test restart/retry idempotence, conflicting IDs, partial failure and supported legacy codec versions before switching repository bindings. Keep source files until successful verification and a deliberate cleanup policy.
- Define appointment-association behavior before adding foreign keys. Deleting an appointment must not cascade-delete historical consultations or notes.
- Implement reset-all and consistent export using a database transaction/snapshot rather than promising atomicity across nine unrelated files. Native file delivery, JSON format/version, recovery and user confirmation remain part of that feature work.
- First implementation slice should import/test the profile and one collection, then expand across the remaining stores. Do not replace all repositories in one unverified operation.
