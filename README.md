# Gestiora

Kotlin Multiplatform app for Android and iOS. Both platforms use the shared Compose UI.

## Architecture

The module layout follows [afalabarce/kmp-template](https://github.com/afalabarce/kmp-template) (inspected at `ae4e1f9e504aa0589b82039952f4a9edb150b3bf`), adapted to this project's existing Kotlin, Compose and Android Gradle Plugin versions.

| Layer | Modules | Responsibility |
| --- | --- | --- |
| `core` | `common`, `viewmodels`, `ui`, `di` | Platform utilities, reusable ViewModel/UI foundations and DI assembly |
| `domain` | `models`, `repository`, `usecases` | Business models, repository contracts and operations |
| `data` | `models`, `datasources`, `datasources-core`, `repository` | Storage/remote models, source contracts and implementations, repository adapters |
| `presentation` | `viewmodels`, `ui` | UI state and shared Compose screens |

`androidApp` and `iosApp` are platform hosts. The iOS app imports the static `Shared` framework from `shared:presentation:ui`. Gradle dependencies enforce the layer direction: UI reaches ViewModels, use cases reach domain repository contracts, and data repository is positioned to implement those contracts. `core:di` assembles Koin modules exposed by the layers; these modules register the implemented features.

The first migration slices provide a shared Compose pregnancy dating flow and profile-backed dashboard. Dating accepts FUM, a known due date, or an ultrasound/medical reference. The profile is stored locally through Android Keystore encrypted storage or iOS Keychain; iOS compilation and device behavior still require macOS verification. The five-destination shell includes local appointment, note/task, consultation, health-record, medical-condition, vaccination and examination/result CRUD. Independent calendar reminders and consultation attachment text references are implemented too. AI is explicitly deferred to a future update; login/cloud sync and ads also remain in their later phase. See `docs/MIGRATION_CLOSURE.md` for the database entry criteria and current parity gaps. See `docs/MIGRATION_HANDOFF.md` and `docs/MIGRATION_CHECKLIST.md` for verified status and next steps.

Shared UI strings now use Spanish, English and French Compose resources. FAQ and screen entries have finite motion with reduced-motion adapters; Home offers two manually browsed WHO-sourced answers. See `docs/LOCALIZATION_AND_MOTION.md` for validation and limitations. All nine Android clinical stores now use encrypted Room payloads with two transactional/idempotent legacy import groups. iOS retains its protected Keychain adapters; all six isolated Room/Keystore tests now pass on the connected Samsung. See `docs/ROOM_PERSISTENCE.md` for scope and verification limits, and `docs/EDUCATIONAL_CONTENT_SYNC_PLAN.md` for the remaining remote publishing/sync plan. Mi bebé now opens a Room-backed offline educational library with literal accent/case-folded search, ES/EN/FR resource seeds, citations and independent favorites. See docs/EDUCATIONAL_CATALOG.md; Android build, 120 host tests and six isolated Samsung device tests pass. English dark-theme library/search/favorite/restart smoke passed; ES/FR visual/accessibility and iOS validation remain pending.

## Android local v1 checkpoint

Local appointment/examination notifications and verified legacy file/key cleanup are implemented. Android exports encrypted clinical backups from Profile and restores them during first-run onboarding. See [LOCAL_REMINDERS.md](docs/LOCAL_REMINDERS.md), [ROOM_PERSISTENCE.md](docs/ROOM_PERSISTENCE.md) and the current migration checkpoint. Login, AI and Supabase/CMS are deferred to phase 2. The small educational catalog ships offline and receives corrections through app updates; see [LOCAL_CONTENT_POLICY.md](docs/LOCAL_CONTENT_POLICY.md). Reset, appointment associations, demo and device/iOS validation remain open.

## Build

- Android internal: `./gradlew :androidApp:assembleInternalDebug`. Installs as **Gestiora Debug** (`com.jecsdev.gestiora.debug`) alongside production.
- Android production: in Android Studio, use **Build → Generate Signed Bundle / APK** with the `productionRelease` variant and your release key. `./gradlew :androidApp:bundleProductionRelease` also builds an unsigned bundle for local R8 checks.
- Common Android host tests: `./gradlew :shared:core:common:testAndroidHostTest`
- iOS: open `iosApp` in Xcode on macOS and run the app there.
