# Gestiora migration handoff

## Current status

Migration in progress. The React prototype is at `C:\Users\john0\screen-capture-design-match` (read-only reference). Pregnancy dating, persisted onboarding, local appointments, notes/follow-up tasks, consultation history, health records and professional-recorded conditions, vaccinations and examinations/results are implemented. Android debug builds and host tests pass; physical-device and iOS validation remain.

## Completed features

- Examinations/results: protected local CRUD for five study types and three statuses, request/scheduled/completion dates, requesting physician, supplied results, medical notes and document text reference. Completion requires its date; changing status retains historical completion until explicitly cleared. All six Records sections now have local CRUD; remaining parity/platform work is listed below.

- Vaccinations: protected local CRUD with four statuses, optional scheduled date, required application date when Applied, dose/provider/location, proof text reference and notes. Historical application dates remain editable and preserved after status changes unless explicitly cleared. Vacunas tab keeps existing Records routing/locks.

- Conditions and risks: protected local CRUD with required name/date/professional, three statuses, instructions, prescribed treatment, monitoring, optional follow-up date and historical notes. Recorded order is preserved. Conditions tab added; Home shows the first non-resolved condition with instructions/fallback and opens the tab directly. Read failure exposes retry instead of an empty result; failed saves preserve the form. No clinical assessment is performed.

- Health records: local CRUD for all eight prototype types, dynamic numeric/narrative fields, units and notes; newest-date history and last weight/blood-pressure summaries with explicit empty state/date. Salud is the default Records tab, with horizontal Consultas and Notas/tasks tabs; Home pending tasks still open Notes. Decimal comma/dot and finite exponent input are supported; saving clears fields inapplicable to the selected type. Failed writes retain the form.

- Consultation history: local create/edit/delete with newest dates first, optional historical gestational age, physician, observations, instructions, medications, requested studies, follow-up text and next-visit date. Records includes Consultations and Notes/tasks tabs; dashboard pending-task navigation still opens Notes. Editing, deletion and writes lock both record tabs and bottom navigation. Failed saves retain the form.

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
- Shared five-destination navigation shell, profile summary/edit entry and an offline baby-development explorer for weeks 4–40. Records contains local health records, consultations, notes and follow-up tasks.
- Calendar lists local appointments and supports creating, editing and deleting them. Storage uses a versioned codec and a separate Android encrypted file/key or iOS Keychain item, isolated from the profile.
- Calendar displays a Monday-first month grid with appointment markers, date selection and month navigation. It can show all appointments and prefills a selected date for new appointments. When an edited appointment changes date, the active filter and displayed month follow it.
- Dashboard loads up to three scheduled appointments dated today or later, sorted by date and time, with loading, empty and retryable error states and navigation to the calendar. As in the prototype, today's appointments remain listed regardless of their time.
- Selecting an upcoming appointment opens the calendar on its date; the general calendar button and bottom navigation open the unfiltered list.
- Appointment forms now use a shared Material 24-hour time input dialog and persist a reminder preference (none, one hour/day, two days or one week before). The UI explicitly states that no system notification is sent.
- Appointment codec v2 reads legacy v1 records with reminder NONE; reading does not rewrite storage. A successful mutation writes v2 while preserving existing records.

## Files and modules modified

- ExamRecord/ExamKind/ExamStatus, repository/use case/ViewModel and DI, strict codec/repository, isolated secure platform adapters; ExamRecordsScreen/draft/tests and Records Estudios tab.

- VaccineRecord/VaccineStatus, repository contract/use case/ViewModel and DI, strict codec/repository, isolated secure platform adapters; VaccineRecordsScreen/draft/tests and Records tab.

- Conditions slice: MedicalCondition/ConditionStatus, repository contract, ManageMedicalConditions, isolated SecureMedicalConditionsStore/platform adapters, bounded codec and serialized repository, MedicalConditionViewModel and Koin bindings; MedicalConditionsScreen/draft helper/tests, Records tab, MedicalFollowUpSection/selection test and NavigationShell direct navigation.

- Health-record slice: VitalRecord/VitalType/validation, repository contract, ManageVitalRecords, secure datasource contract and platform adapters, codec, serialized repository and Koin registrations, VitalRecordViewModel; VitalRecordsScreen, queries/decimal parser, Records tabs and associated host tests.

- Consultation slice: domain model/contract/use case, secure datasource contract and Android/iOS adapters, bounded codec and serialized repository, ViewModel and all Koin bindings, repository/ViewModel host tests; shared ConsultationsScreen and RecordsScreen, NavigationShell integration and Notes header adjustment.

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

- Study completion requires a valid completion date; request/scheduled dates are optional and validated if supplied. Historical completion survives status changes. Results are supplied text, never automatically interpreted. Attachments are text references; no file upload. No invented chronological constraints are added.

- Vaccination application dates are required when ADMINISTERED; other states may retain a historical application date. All supplied dates are validated without speculative chronology rules. Proof is a text reference, not an uploaded file. No recommended vaccine schedule or notifications are generated.

- Conditions record a professional-provided name, identification date/professional, instructions, prescribed treatment, monitoring, optional follow-up date, historical notes and ACTIVE/FOLLOW_UP/RESOLVED status. No diagnosis, risk inference or treatment suggestion is generated. The Home medical-follow-up card follows the prototype: first non-resolved condition in recorded order, without ranking urgency. Its follow-up date does not schedule a notification.

- Health-record input follows the eight prototype types: maternal weight (kg), blood pressure (mmHg), maternal/fetal heart rate (lpm), fundal height (cm), ultrasound findings, symptoms and custom numeric measures. The prototype bounds (numeric 0–400 and diastolic 20–200) are entry constraints, not clinical reference ranges or an assessment. Type-inapplicable fields must be absent to avoid stale data overriding the selected measurement. Decimal dot/comma input is supported without thousands separators; nonfinite values are rejected. No clinical advice, automatic interpretation or chart is added.

- Consultation gestational age is manually recorded historical text, independent of the current pregnancy dating. Follow-up is narrative text; next-visit date does not create an appointment or notification. Attachment references and appointment linking are deferred. Storage is isolated (Android pregnancy_consultations.gpr, Keystore consultations.v1; iOS consultations service/account). Codec v1 limits: 1,000 records, 1,000,000 payload characters, ID128, gestational age/doctor512, each narrative4096; strict civil dates. Corruption/unknown versions and I/O failures remain explicit and prevent overwrite.

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

- Examinations storage: Android pregnancy_exams.gpr / com.jecsdev.gestiora.examRecords.v1 / GEX01; provisional iOS examRecords service / pregnancy-exams-v1 account. Codec v1: 1,000 records/1,000,000 payload chars, ID128, name/physician/attachment512, results/notes4096; strict enums/dates. No actual document attachment storage.

- Vaccination storage: Android pregnancy_vaccines.gpr / com.jecsdev.gestiora.vaccineRecords.v1 / GVC01; provisional iOS vaccineRecords service / pregnancy-vaccines-v1 account. Codec v1: 1,000 records/1,000,000 payload chars, ID128, name/dose/provider/location/proof512, notes4096; strict dates/status. No actual proof attachments.

- Conditions are local records of professional-provided information. Follow-up dates do not schedule appointments or notifications. Codec v1 limits: 1,000 records/1,000,000 payload characters, ID128, required name/professional512, each narrative4096, strict civil dates/enums. Android pregnancy_conditions.gpr / com.jecsdev.gestiora.medicalConditions.v1 / GCD01; provisional iOS service com.jecsdev.gestiora.medicalConditions/account pregnancy-conditions-v1. Device encryption/UI and iOS compile/run remain unverified.

- Health records have calendar dates without a time. Same-day summary ties use a deterministic ID order and cannot establish the actual most recent measurement time. Prototype numeric input bounds are retained; clinical review and physical-device validation remain pending. Vitals storage is isolated: Android pregnancy_vitals.gpr / com.jecsdev.gestiora.vitalRecords.v1 (GVT01 authenticated envelope), iOS service com.jecsdev.gestiora.vitalRecords / pregnancy-vitals-v1. Codec v1 limits 1,000 records, 1,000,000 payload characters, ID128, label512, text/notes4096.

- Draft forms/navigation currently use Compose remember state. Activity recreation (for example rotation) can lose unsaved input and return to Home without a discard prompt; this affects existing forms, consultations, health-record and condition forms. Preserve draft/navigation lifecycle state in a dedicated follow-up before release.
- Consultations: attachment references and appointment linking are not implemented. Android protected-store and Compose interaction need physical-device validation; iOS Keychain adapter remains uncompiled on Windows.

- Profile, appointments, notes, consultations, health records, conditions, vaccinations and examinations are persisted locally. Sync, export/recovery, demo/account branches and account deletion are not implemented; no backup or device transfer is available.
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

- Examinations/results (2026-11-12): Android debug build and repository/ViewModel/UI Android host tasks passed (`--offline --no-configuration-cache`). Four new repository tests cover CRUD/concurrency/reload, kind/status combinations/nulls/Unicode/limits, required completion dates and historical retention, invalid payloads/enums/versions and I/O non-overwrite. One ViewModel test covers IDs/CRUD/errors/cancellation; two draft tests cover fields/dates/limits/status retention and explicit clearing. Read-only review found no new blockers. ADB devices listed no devices; mDNS advertised the known Samsung phone, but connecting to its advertised endpoint failed with connection refused (10061). No install or device test performed; iOS remains uncompiled.

- Vaccinations (2026-11-12): Android debug build and repository/ViewModel/UI Android host test tasks passed (`--offline --no-configuration-cache`). Four new repository tests cover CRUD/concurrency/reload, statuses/null/Unicode/bounds, conditional application dates and historical retention, invalid payloads/enums/versions/IO non-overwrite. One ViewModel test covers generated IDs/errors/cancellation; two draft tests cover fields/dates/limits/state changes and explicit clearing. Read-only review found no new blocker. Android encryption and Compose interactions are not device-validated; iOS uncompiled.

- Conditions slice (2026-11-12): `:androidApp:assembleDebug :shared:data:repository:testAndroidHostTest :shared:presentation:viewmodels:testAndroidHostTest :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed. Four repository tests cover concurrent CRUD/reload, statuses/Unicode/nulls/bounds, required fields/dates/limits, I/O errors and malformed/unsupported/duplicate/truncated payload non-overwrite. One ViewModel test covers IDs/CRUD/errors/cancellation. Two draft helper tests cover required fields, dates, limits, optional trimming and statuses; one Home selection test excludes resolved records while preserving recorded order. Read-only review found no new blockers. Host stores are fakes: UI navigation and platform encryption remain untested on device. ADB devices -l succeeded and listed no devices; iOS was not compiled.

- Health-record slice (2026-11-12): `:androidApp:assembleDebug :shared:data:repository:testAndroidHostTest :shared:presentation:viewmodels:testAndroidHostTest :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed. Four new repository tests cover CRUD/concurrency/reload, eight types/decimals/bounds/nulls, required/hidden fields, finite numbers, invalid dates and limits, I/O failures, malformed/unknown payloads and non-overwrite. One new ViewModel test covers generated IDs, CRUD/errors/cancellation. Three new UI helper tests cover decimal/exponent parsing, history/latest type/date/order and draft-to-record field clearing. Read-only code review found no new blockers. These are host tests with fake stores, not Android encryption/UI runtime tests. ADB devices -l listed no devices; iOS remains uncompiled on Windows.

- Consultation slice (2026-11-12): `:androidApp:assembleDebug :shared:data:repository:testAndroidHostTest :shared:presentation:viewmodels:testAndroidHostTest :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed. Three new repository tests exercise CRUD, concurrent additions, reload with a new repository, Unicode/null fields, invalid dates/limits, failed I/O, corrupt/unsupported/truncated/duplicate payloads and non-overwrite. One new ViewModel test checks generated IDs, CRUD, errors and cancellation. Initial test discovery failed because an expression-bodied test inferred a non-Unit return; explicit runBlocking<Unit> fixed it and the complete command passed. ADB devices -l succeeded outside sandbox and listed no devices. No Android UI/encryption runtime or iOS testing performed.

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
2. On macOS, compile iOS framework and test all eight Keychain items and their save/reload/delete behavior; correct any Kotlin/Native interop issues.
3. Resolve recovery/export and uninstall/account-deletion policy before making portability promises.
4. Verify navigation, baby explorer, date picker and typography on the physical device, including accessibility and localization. Obtain clinical copy review and confirm illustration distribution rights.
5. After validating the appointments slice, plan reminders and the next prenatal records slice with an appropriate protected storage design.

Next notes checks: physical-device CRUD, discard/delete confirmations, follow-up dates, restart persistence and tamper/error behavior. Compile/test notes Keychain on macOS. Next MVP-local work: preserve form/navigation drafts across Activity recreation, then physical-device UI/persistence/navigation/accessibility checks against the prototype. Actual document attachments, appointment links and standalone reminders remain deferred; auth/sync is a later slice.
