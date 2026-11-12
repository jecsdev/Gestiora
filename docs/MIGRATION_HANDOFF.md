# Gestiora migration handoff

## Current status

Migration in progress. The React prototype is at `C:\Users\john0\screen-capture-design-match` (read-only reference). Pregnancy dating, persisted onboarding and an initial local appointments slice are implemented. Android debug builds and host tests pass; physical-device and iOS validation remain.

## Completed features

- Notes support combined title/body search and task-status filters, with an explicit no-match state and filter reset. After saving, the active status filter follows the saved note so it remains visible.
- Home shows up to three pending notes with follow-up dates, ordered oldest first and including overdue tasks. Its independent loading/error/retry states distinguish read failures from empty results. Opening this section navigates to Records filtered to pending tasks, including undated tasks.

- Template module structure and shared Android/iOS Compose host, from prior setup.
- React routes, state/data model, dating rules, styles, and assets inventoried; route mapping is in the checklist.
- Pure domain dating model and calculator, with FUM, due-date fallback, ultrasound/medical reference priority, 280-day EDD and 0–300-day ongoing validation.
- Shared Compose welcome, personal details, dating with live preview, privacy/finish flow, and profile-backed pregnancy dashboard with edit entry.
- Local profile repository with a versioned codec. Android uses AES-GCM via Android Keystore and an atomic file in `noBackupFilesDir`; iOS Keychain adapter uses `WhenUnlockedThisDeviceOnly` (not compiled on Windows).
- Explicit loading, empty, and error states. A failed read never opens a blank onboarding or silently replaces stored data.
- Soft cream/lavender/rose Material 3 light and dark palette.
- Plus Jakarta Sans for display, headline and title styles, and DM Sans for body and labels, bundled with OFL licenses in `shared/core/ui/licenses`.
- Shared Material date picker for onboarding and profile editing, with optional FUM/due-date clearing and future-date limits for FUM and clinical reference dates.
- Shared five-destination navigation shell, profile summary/edit entry and an offline baby-development explorer for weeks 4–40. Records contains local notes and follow-up tasks.
- Calendar lists local appointments and supports creating, editing and deleting them. Storage uses a versioned codec and a separate Android encrypted file/key or iOS Keychain item, isolated from the profile.
- Calendar displays a Monday-first month grid with appointment markers, date selection and month navigation. It can show all appointments and prefills a selected date for new appointments. When an edited appointment changes date, the active filter and displayed month follow it.
- Dashboard loads up to three scheduled appointments dated today or later, sorted by date and time, with loading, empty and retryable error states and navigation to the calendar. As in the prototype, today's appointments remain listed regardless of their time.
- Selecting an upcoming appointment opens the calendar on its date; the general calendar button and bottom navigation open the unfiltered list.
- Appointment forms now use a shared Material 24-hour time input dialog and persist a reminder preference (none, one hour/day, two days or one week before). The UI explicitly states that no system notification is sent.
- Appointment codec v2 reads legacy v1 records with reminder NONE; reading does not rewrite storage. A successful mutation writes v2 while preserving existing records.

## Files and modules modified

- Notes slice: PrenatalNote, repository contract, ManagePrenatalNotes, isolated store contract and Android/iOS adapters, codec, repository, ViewModel, Records screen, Koin bindings and host tests. CivilDateValidation is shared by appointment and note codecs.

- `docs/MIGRATION_HANDOFF.md`, `docs/MIGRATION_CHECKLIST.md`.
- `shared/domain/models`: `PregnancyDating.kt`.
- `shared/domain/models`: `PregnancyProfile.kt`; `shared/domain/repository`: `PregnancyProfileRepository.kt`.
- Appointment slice: `Appointment.kt`, `AppointmentRepository.kt`, `ManageAppointments.kt`, `SecureAppointmentsStore.kt`, platform adapters, `AppointmentCodec.kt`, `SecureAppointmentRepository.kt`, `AppointmentViewModel.kt`, `AppointmentsScreen.kt`, platform back handlers, DI and host tests.
- `shared/domain/usecases`: `CalculatePregnancyAge.kt`, its host tests, DI registration and host-test Gradle configuration.
- `shared/data/datasources`: `SecureProfileStore.kt` contract; `shared/data/datasources-core`: Android/iOS adapters and DI registration.
- `shared/data/repository`: versioned `ProfileCodec.kt`, `SecurePregnancyProfileRepository.kt`, DI and host tests.
- `shared/presentation/viewmodels`: `ProfileViewModel.kt`, tests, DI and Gradle; previous dating ViewModel remains.
- `shared/presentation/ui`: `App.kt`, `NavigationShell.kt`, `BabyScreen.kt`, `DateField.kt`, `OnboardingScreen.kt`, `PregnancyDashboard.kt`, fetal illustrations, and Android/iOS `todayIsoDate` actual implementations.
- `shared/core/ui`: `AppTheme.kt`.
- `androidApp`: Context Koin binding, backup configuration and `data_extraction_rules.xml`.
- Gradle catalog/relevant modules: coroutines core 1.10.2 for off-main storage operations.
- Root `README.md` updated to reflect the new slices.
- Existing user change in `.gitignore` was left untouched.

## Decisions and rationale

- Notes use a separate serialized repository and bounded versioned codec, with an isolated Android AES-GCM/Keystore file/key in noBackupFilesDir and provisional iOS Keychain item. Limits: 1,000 notes, 1,000,000 payload characters, 512 title characters and 4,096 body characters. Notes are independent of pregnancy dating. Follow-up dates do not schedule notifications. Prototype appointment-to-note links are not exposed in this slice.

- Implement dating calculation first because it is a pure, independently testable rule used by onboarding, profile, and dashboard.
- The first slice kept dating ephemeral. The profile and appointments now have separate persistence; other records and sync remain unimplemented.
- Preserve the React priority of medical/ultrasound reference over FUM and its 280-day due-date rule.
- Date arithmetic uses ISO civil calendar days in common Kotlin; the host supplies today's local date, avoiding timezone shifts in gestational age.
- Date picker values convert between ISO civil dates and UTC midnight, as required by Material's picker; domain validation remains the final check.
- Baby development is an offline educational explorer, not an assessment; numeric lengths of unspecified measurement origin were omitted. If the actual week is outside 4–40, the UI displays it separately from the nearest available illustration week.
- The UI only passes fields for the selected dating method so stale form values cannot produce a result under another method.
- The new repository serializes reads/writes with a Mutex and runs blocking platform I/O off the UI thread. Coroutines core 1.10.2 was added for that purpose; no persistence framework was introduced.
- Storage errors and unsupported codec versions remain explicit. Saving reads/decodes existing data before replacing it, so a corrupt or inaccessible profile is not silently overwritten.
- Appointment mutations serialize the complete read/decode/change/encode/write cycle under a singleton repository mutex; corrupt or unsupported payloads are not overwritten. Local date and time remain civil values. Limits: 1,000 appointments, 1,000,000 payload characters, 512 characters per required text field and 4,096 for notes.
- Android profile data is excluded from backup/device transfer because its Keystore key cannot be restored on another device. iOS Keychain item is device-only. Product recovery/export policy is still open.

## Pending tasks

- Physical-device roundtrip and tamper tests for Android encrypted profile and appointments; iOS compile and Keychain tests on macOS.
- Prenatal records, auth/sync, remaining navigation destinations and design polish.

## Known bugs and limitations

- Profile, appointments and notes are persisted locally. Consultations, vitals, conditions, vaccines, examinations, sync, export/recovery, demo/account branches and account deletion are not implemented; no backup or device transfer is available.
- Appointment list/create/edit/delete, date filtering, inline month grid and reminder preferences are implemented. System notifications and standalone reminders are not implemented. Host tests use a fake store; Android Keystore, persistence and UI interactions have not been tested on a device, and the iOS adapter has not been compiled or tested.
- The shared Material date picker, including accessibility and localization, still needs physical-device visual and interaction validation.
- A physical Samsung phone was previously detected over Wi-Fi; ADB listed no connected devices on 2026-11-12. No profile or UI/storage test was performed.
- iOS build/run requires macOS and Xcode.
- iOS Keychain interop is provisional: Kotlin/Native CF pointer signatures and actual Keychain read/write must be compiled/tested on macOS. A Keychain item may survive uninstall/reinstall on the same device; deletion policy is unresolved.
- Bundled fonts compile on Android; visual verification on device and iOS is pending.
- The user stated that the reference illustrations were generated with Lovable; rights for distribution have not been verified. Development copy is inherited/adapted from the prototype and has no documented clinical review; obtain review before release.
- Navigation, screen insets, larger text, TalkBack and baby explorer interactions remain unverified on a device. AI assistant is not included.
- Prototype localStorage is unencrypted and its cloud sync is last-write-wins; do not port either behavior blindly for medical data.

## Tests and build

- Notes search/follow-up summary: Android debug build and UI host tests passed. Two new query tests cover text/status combinations, absent bodies, no-match results, dated pending selection, completed/undated exclusion, overdue retention, ordering and limit. Compose interactions remain unverified on device.

- Notes slice: Android debug build and repository, ViewModel and UI host tests passed. Three new repository tests cover CRUD, concurrency, enum/Unicode roundtrips, validation, failed I/O, corrupt/unknown payloads and non-overwrite. A new ViewModel test covers generated IDs, CRUD and error propagation. UI interaction, Android encryption and iOS adapter remain unverified on device/macOS.

- `:shared:domain:usecases:testAndroidHostTest`: passed, 5 tests, 0 failures.
- `:shared:data:repository:testAndroidHostTest`: passed, 4 tests, 0 failures (codec/repository failures and non-overwrite).
- `:shared:presentation:viewmodels:testAndroidHostTest`: passed, 3 tests, 0 failures (read failure is not empty; save success/failure).
- `:androidApp:assembleDebug` with the above host tests, `--offline --no-configuration-cache`: passed. APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
- After adding typography, `:androidApp:assembleDebug --offline --no-configuration-cache` passed; no device visual test was performed.
- After adding the date picker, `:androidApp:assembleDebug :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed; the two host tests cover ISO/UTC conversion and invalid dates.
- After adding navigation and baby development, the same Android build and host-test command passed; four UI host tests cover date conversion and stage/illustration boundaries. No UI or device test was run.
- After the appointments slice, Android debug build and repository, presentation ViewModel and UI Android host tests passed. Appointment tests cover repository CRUD, validation, failures/corruption and concurrent adds using a fake store, plus ViewModel operations/errors. These do not validate platform encryption or UI interactions.
- On 2026-11-12, after adding date filtering, `:androidApp:assembleDebug :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed. Existing host tests do not cover the new filter interaction; device UI validation remains pending.
- After the inline month grid, the same Android build and UI host tests passed. New tests cover Monday-first grids, leap February, adjacent days, year rollover and supported date bounds. They do not test Compose interaction or accessibility.
- After adding upcoming appointments to the dashboard, the same Android build and UI host tests passed. A host test checks status/date filtering, ordering, the three-item limit and the empty result; loading and UI interactions remain untested on a device.
- After linking dashboard appointment cards to their calendar date, Android debug build and UI host tests passed. The navigation interaction itself still needs device validation. ADB detected no connected devices on the latest check.
- After reminder preferences, Android debug build and repository, ViewModel and UI host tests passed. New repository tests cover all reminder options, v1 read without rewriting, v1-to-v2 update preservation and rejection/non-overwrite of unknown reminder values. After the time input dialog, Android debug build and UI host tests passed again. Time input interaction and platform storage remain unverified on device.
- `git diff --check`: passed (Windows line-ending warnings only).
- iOS compile/run: not executed on Windows.

## Integration dependencies

- `kotlinx-coroutines-core` 1.10.2 added for serialized off-main profile I/O.
- Future cloud work requires inspecting Lovable Cloud contracts and secure credential handling.

## Exact next steps

1. On a physical Android phone, verify onboarding and appointments save, process restart/reload, edit/delete, ciphertext tamper/error states, navigation and UI layout without overwriting existing data. No emulator requested by the user.
2. On macOS, compile iOS framework and test both Keychain items and their save/reload/delete behavior; correct any Kotlin/Native interop issues.
3. Resolve recovery/export and uninstall/account-deletion policy before making portability promises.
4. Verify navigation, baby explorer, date picker and typography on the physical device, including accessibility and localization. Obtain clinical copy review and confirm illustration distribution rights.
5. After validating the appointments slice, plan reminders and the next prenatal records slice with an appropriate protected storage design.

Next notes checks: physical-device CRUD, discard/delete confirmations, follow-up dates, restart persistence and tamper/error behavior. Compile/test notes Keychain on macOS. Next records slice: consultations.
