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

The first migration slices provide a shared Compose pregnancy dating flow and profile-backed dashboard. Dating accepts FUM, a known due date, or an ultrasound/medical reference. The profile is stored locally through Android Keystore encrypted storage or iOS Keychain; iOS compilation and device behavior still require macOS verification. The five-destination shell includes local appointment, note/task, consultation, health-record, medical-condition, vaccination and examination/result CRUD. Remaining prenatal records, cloud sync and device verification are pending. See `docs/MIGRATION_HANDOFF.md` and `docs/MIGRATION_CHECKLIST.md` for verified status and next steps.

## Build

- Android: `./gradlew :androidApp:assembleDebug`
- Common Android host tests: `./gradlew :shared:core:common:testAndroidHostTest`
- iOS: open `iosApp` in Xcode on macOS and run the app there.
