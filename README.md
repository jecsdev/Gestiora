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

`androidApp` and `iosApp` are platform hosts. The iOS app imports the static `Shared` framework from `shared:presentation:ui`. Gradle dependencies enforce the layer direction: UI reaches ViewModels, use cases reach domain repository contracts, and data repository is positioned to implement those contracts. `core:di` assembles Koin modules exposed by the layers; these lists are empty until the first feature adds bindings.

The generated greeting screen remains as a smoke-test UI. There is no pregnancy record model, persistence schema, API, or navigation flow yet. Those should be added as a feature slice when their requirements are defined, rather than copying the template's example data sources.

## Build

- Android: `./gradlew :androidApp:assembleDebug`
- Common Android host tests: `./gradlew :shared:core:common:testAndroidHostTest`
- iOS: open `iosApp` in Xcode on macOS and run the app there.
