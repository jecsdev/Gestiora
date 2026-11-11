# Gestiora migration handoff

## Current status

Migration in progress. The React prototype is at `C:\Users\john0\screen-capture-design-match` (read-only reference). Pregnancy dating and persisted onboarding slices are implemented. Android debug builds and host tests pass; physical-device and iOS validation remain.

## Completed features

- Template module structure and shared Android/iOS Compose host, from prior setup.
- React routes, state/data model, dating rules, styles, and assets inventoried; route mapping is in the checklist.
- Pure domain dating model and calculator, with FUM, due-date fallback, ultrasound/medical reference priority, 280-day EDD and 0–300-day ongoing validation.
- Shared Compose welcome, personal details, dating with live preview, privacy/finish flow, and profile-backed pregnancy dashboard with edit entry.
- Local profile repository with a versioned codec. Android uses AES-GCM via Android Keystore and an atomic file in `noBackupFilesDir`; iOS Keychain adapter uses `WhenUnlockedThisDeviceOnly` (not compiled on Windows).
- Explicit loading, empty, and error states. A failed read never opens a blank onboarding or silently replaces stored data.
- Soft cream/lavender/rose Material 3 light and dark palette.

## Files and modules modified

- `docs/MIGRATION_HANDOFF.md`, `docs/MIGRATION_CHECKLIST.md`.
- `shared/domain/models`: `PregnancyDating.kt`.
- `shared/domain/models`: `PregnancyProfile.kt`; `shared/domain/repository`: `PregnancyProfileRepository.kt`.
- `shared/domain/usecases`: `CalculatePregnancyAge.kt`, its host tests, DI registration and host-test Gradle configuration.
- `shared/data/datasources`: `SecureProfileStore.kt` contract; `shared/data/datasources-core`: Android/iOS adapters and DI registration.
- `shared/data/repository`: versioned `ProfileCodec.kt`, `SecurePregnancyProfileRepository.kt`, DI and host tests.
- `shared/presentation/viewmodels`: `ProfileViewModel.kt`, tests, DI and Gradle; previous dating ViewModel remains.
- `shared/presentation/ui`: `App.kt`, `OnboardingScreen.kt`, `PregnancyDashboard.kt`, fetal image, and Android/iOS `todayIsoDate` actual implementations.
- `shared/core/ui`: `AppTheme.kt`.
- `androidApp`: Context Koin binding, backup configuration and `data_extraction_rules.xml`.
- Gradle catalog/relevant modules: coroutines core 1.10.2 for off-main storage operations.
- Root `README.md` updated to reflect the new slices.
- Existing user change in `.gitignore` was left untouched.

## Decisions and rationale

- Implement dating calculation first because it is a pure, independently testable rule used by onboarding, profile, and dashboard.
- The first slice kept dating ephemeral. The current slice persists only the pregnancy profile; records and sync remain unimplemented.
- Preserve the React priority of medical/ultrasound reference over FUM and its 280-day due-date rule.
- Date arithmetic uses ISO civil calendar days in common Kotlin; the host supplies today's local date, avoiding timezone shifts in gestational age.
- The UI only passes fields for the selected dating method so stale form values cannot produce a result under another method.
- The new repository serializes reads/writes with a Mutex and runs blocking platform I/O off the UI thread. Coroutines core 1.10.2 was added for that purpose; no persistence framework was introduced.
- Storage errors and unsupported codec versions remain explicit. Saving reads/decodes existing data before replacing it, so a corrupt or inaccessible profile is not silently overwritten.
- Android profile data is excluded from backup/device transfer because its Keystore key cannot be restored on another device. iOS Keychain item is device-only. Product recovery/export policy is still open.

## Pending tasks

- Physical-device roundtrip and tamper test for Android encrypted profile; iOS compile and Keychain test on macOS.
- Prenatal records, baby development, auth/sync, bottom navigation, fonts, accessible native date input and design polish.

## Known bugs and limitations

- Only the profile is persisted. Prenatal records, sync, export/recovery, demo/account branches and account deletion are not implemented.
- Date input is ISO text rather than a native date picker; form visual/device validation remains pending.
- Android crypto code compiled, but no physical device was connected for an encryption roundtrip or UI test (`adb devices` returned no devices).
- iOS build/run requires macOS and Xcode.
- iOS Keychain interop is provisional: Kotlin/Native CF pointer signatures and actual Keychain read/write must be compiled/tested on macOS. A Keychain item may survive uninstall/reinstall on the same device; deletion policy is unresolved.
- Plus Jakarta Sans and DM Sans are referenced in React but not bundled there; native font assets still need sourcing. Current UI uses system typography.
- Prototype localStorage is unencrypted and its cloud sync is last-write-wins; do not port either behavior blindly for medical data.

## Tests and build

- `:shared:domain:usecases:testAndroidHostTest`: passed, 5 tests, 0 failures.
- `:shared:data:repository:testAndroidHostTest`: passed, 4 tests, 0 failures (codec/repository failures and non-overwrite).
- `:shared:presentation:viewmodels:testAndroidHostTest`: passed, 3 tests, 0 failures (read failure is not empty; save success/failure).
- `:androidApp:assembleDebug` with the above host tests, `--offline --no-configuration-cache`: passed. APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
- `git diff --check`: passed (Windows line-ending warnings only).
- iOS compile/run: not executed on Windows.

## Integration dependencies

- `kotlinx-coroutines-core` 1.10.2 added for serialized off-main profile I/O.
- Future cloud work requires inspecting Lovable Cloud contracts and secure credential handling.

## Exact next steps

1. On a physical Android phone, verify onboarding save, process restart/reload, edit, ciphertext tamper/error state, and UI layout. No emulator requested by the user.
2. On macOS, compile iOS framework and test Keychain save/reload/delete; correct any Kotlin/Native interop issues.
3. Resolve recovery/export and uninstall/account-deletion policy before making portability promises.
4. Implement native date input and accessibility polish, then bottom navigation and the first appointments CRUD slice with its own encrypted storage plan.
