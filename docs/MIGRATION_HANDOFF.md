# Gestiora migration handoff

## Current status

Migration in progress; full parity is not declared complete. See `docs/MIGRATION_CLOSURE.md` for the current reference audit, executed phone checks and database entry criteria. The React prototype is at `C:\Users\john0\screen-capture-design-match` (read-only reference). Pregnancy dating, persisted onboarding, local appointments, notes/follow-up tasks, consultation history, health records and professional-recorded conditions, vaccinations and examinations/results and independent personal reminders are implemented. Android debug builds and host tests pass; physical-device and iOS validation remain.

## Completed features

- Closure slice (2026-11-16): Calma editorial now extends to Calendar, all six Records lists/forms, Baby, Profile/preferences and onboarding. Shared editorial headers/record surfaces/status chips and rounded Material shapes unify spacing/contrast; action/chip rows wrap and calendar rows have a 48dp minimum height. Fields, errors, draft retention and navigation locks remain.
- Independent personal reminders: five types, optional 24-hour time/all-day, completion, date filtering/marked calendar and protected local CRUD. Citas/Recordatorios tabs have independent calendars and are disabled during edit/delete/write. Android AES-GCM/AtomicFile and provisional iOS Keychain adapters follow existing boundaries. No system notifications.
- Consultations now include the prototype attachment text reference. Codec v2 reads legacy v1 without a rewrite on load and upgrades on mutation while preserving historical content.

- Dark-theme text correction (2026-11-16): AppTheme now supplies a root Material Surface with background/onBackground so uncolored text inherits the active palette. Welcome title explicitly uses onSurface while its accented span keeps primary. Android assembleDebug passed with offline/no-configuration-cache/no-watch-fs; visual confirmation on device remains pending.

- Approved Calma editorial direction, first implementation slice (2026-11-16): Home now has a compact brand/profile header, prominent real gestational week and preserved baby artwork, separate due-date/days-left summary, appointment previews with calendar date tiles, and quieter task/medical follow-up rows. Upcoming appointments precede educational development content. The duplicated gestational-age line and full-width Home edit action were removed; Home profile access opens Profile, where editing remains. Navigation uses the existing five destinations with theme-aware selected colors, a quiet divider and rounded line-icon strokes. Narrow/large-font layouts retain stacked fallbacks and scroll. Data queries, reminder behavior and storage are unchanged.

- Welcome viewport layout (2026-11-16): separate shared welcome uses the reference's full-height composition, 36%-viewport illustration (240 dp minimum) without the artificial lavender card, and a full-width rounded lower panel. It fills tall windows and scrolls on short windows/large fonts. Existing local-only actions/copy remain; demo and account actions are still deferred. Screen containers now use safeDrawingPadding instead of safeContentPadding so system-gesture exclusion does not add lateral margins to ordinary content; system bars, cutouts and IME protection remain.

- Spanish civil-date presentation (2026-11-14 local): compact dates match the prototype's `d MMM yyyy` convention across onboarding preview, profile, dashboard, appointments, all six Records sections and date fields. Picker/calendar accessibility labels use full Spanish month names. Calendar day semantics expose today, appointment markers and selection without also reading the child day number; weekday headings have full names. ISO model values, date ordering/filtering, picker callbacks and storage are unchanged.

- Appearance preference (2026-11-13 local): Profile offers Seguir el sistema/Claro/Oscuro, applied at the app root without recreating the navigation/forms. The choice is stored separately from medical records and defaults to System only when absent. Read failures temporarily use System and expose retry in Profile; writes update the visible selection only after success. Retained state/operation scope prevents replaying a theme write on configuration recreation. Native system appearance is updated too; iOS clears its controller override when returning to System.

- Home/Baby parity: shared BabyIllustration uses the existing seven bundled assets and reference-week mapping. Home hero shows actual gestational week next to the labeled reference illustration; weekly stage card and Ver bebé both open Mi bebé. Invalid/unknown pregnancy age hides the illustration/stage card; out-of-explorer-range weeks remain explicit rather than silently replaced. Hero and due-date summaries stack below320dp content width or fontScale>1.2. Baby explorer keeps its chosen week in RAM across configuration recreation and places its title above the two navigation buttons.

- MVP flow hardening: onboarding/profile System Back shares the visible Back policy, moves through steps, consumes Back during save and confirms discard when leaving profile editing. Personal-detail changes can preserve existing historical pregnancy dates outside the ongoing window, matching the React profile flow; new/changed dates still validate against the current day when submitted. Date/time button accessibility labels now identify their field and selected value, retaining native button semantics without duplicate child text.

- Draft/configuration lifecycle: onboarding/profile, appointments and all six Records sections use in-memory Compose retain for user drafts/editing/error/busy states. Root and tab/navigation selectors retained. Koin graph and operation scopes are retained so the same repositories/mutexes and in-flight writes continue across configuration changes. Root preserves Ready/Empty and does not briefly remove drafts during reload; profile saves launch from events, without LaunchedEffect replay.

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

- Closure slice: AppTheme shapes; EditorialComponents, CalendarScreen, PersonalRemindersScreen, AppointmentCalendar, TimeField, all Records screens, RecordsScreen, BabyScreen, NavigationShell, ThemePreferences and OnboardingScreen. PersonalReminder/ReminderKind, domain repository/use case, secure datasource and Android/iOS adapters, bounded codec/serialized repository, ViewModel and DI. Consultation model/codec/form and backward-compatibility tests. New reminder repository/ViewModel host tests.

- EditorialComponents.kt provides small shared Home section/card/follow-up row composables. PregnancyDashboard.kt, PendingNotesSection.kt, MedicalFollowUpSection.kt and NavigationShell.kt implement the approved Home/navigation slice. No new dependency, domain, data or persistence changes.

- WelcomeScreen.kt and OnboardingScreen.kt for welcome composition; App, AppointmentsScreen, BabyScreen, ConsultationsScreen, ExamRecordsScreen, MedicalConditionsScreen, NavigationShell, PregnancyDashboard, PrenatalNotesScreen, VaccineRecordsScreen and VitalRecordsScreen for drawing-safe insets. Presentation-only; no persistence, dependency or host changes.

- DateLabels.kt and DateLabelsTest.kt; DateField, AppointmentCalendar, AppointmentsScreen, ConsultationsScreen, ExamRecordsScreen, MedicalConditionsScreen, NavigationShell, OnboardingScreen, PendingNotesSection, PregnancyDashboard, PrenatalNotesScreen, VaccineRecordsScreen and VitalRecordsScreen. Presentation-only slice; no dependencies or persistence migrations.

- ThemeMode, ThemePreferenceRepository/Store, LocalThemePreferenceRepository, ManageThemePreference, ThemePreferenceViewModel and their DI bindings; Android/iOS ThemePreferenceModule. AppTheme accepts the resolved dark flag; ThemePreferences/ThemeRoot, App and NavigationShell integrate Profile controls. SystemAppearance Android/iOS adapters keep native appearance aligned. Repository and ViewModel host tests added. No dependency changes.

- BabyIllustration.kt, PregnancyDashboard.kt, BabyScreen.kt and NavigationShell.kt for shared artwork, weekly card/navigation, adaptive layout and selected-week retention.

- OnboardingPolicy.kt/OnboardingPolicyTest.kt, OnboardingScreen.kt, DateField.kt and TimeField.kt for Back/discard, historical dating preservation and accessible button labels.

- RetainedResources.kt and RetainedResourcesTest.kt; App.kt, NavigationShell.kt, RecordsScreen.kt, OnboardingScreen.kt and all seven record/appointment screens. No domain/data changes or new dependencies.

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

- Changing personal profile fields must not require inventing new pregnancy dates for a historical profile. Compare effective inputs for the selected dating method; if unchanged, keep the original dating object including inactive fields. Null referenceDays originally displays as zero and is treated accordingly; clearing the displayed value is not an unchanged input. The initial onboarding welcome still uses normal platform Back; unsaved process-exit recovery remains unsupported. Profile field-length policy is unchanged (codec16,384 chars); no new512-char cap was introduced.

- Use Compose runtime-retain already exported by the installed Compose UI. Explicit draft values live in RAM, without SavedStateHandle/rememberSaveable serialization. App also provides a null LocalSaveableStateRegistry so Material picker internals do not serialize medical dates/time; scroll position and unconfirmed picker dialogs may reset. RetainObserver resources cancel/close onRetired/onUnused, not onExitedComposition (configuration handoff). KoinIsolatedContext wraps the retained local graph, keeping one repository/mutex instance. Android bindings use applicationContext. Unsaved drafts intentionally do not survive process death or finishing the Activity.
- User deferred login/cloud synchronization and advertisement sections until the current local MVP/stability work is complete.

- Study completion requires a valid completion date; request/scheduled dates are optional and validated if supplied. Historical completion survives status changes. Results are supplied text, never automatically interpreted. Attachments are text references; no file upload. No invented chronological constraints are added.

- Vaccination application dates are required when ADMINISTERED; other states may retain a historical application date. All supplied dates are validated without speculative chronology rules. Proof is a text reference, not an uploaded file. No recommended vaccine schedule or notifications are generated.

- Conditions record a professional-provided name, identification date/professional, instructions, prescribed treatment, monitoring, optional follow-up date, historical notes and ACTIVE/FOLLOW_UP/RESOLVED status. No diagnosis, risk inference or treatment suggestion is generated. The Home medical-follow-up card follows the prototype: first non-resolved condition in recorded order, without ranking urgency. Its follow-up date does not schedule a notification.

- Health-record input follows the eight prototype types: maternal weight (kg), blood pressure (mmHg), maternal/fetal heart rate (lpm), fundal height (cm), ultrasound findings, symptoms and custom numeric measures. The prototype bounds (numeric 0–400 and diastolic 20–200) are entry constraints, not clinical reference ranges or an assessment. Type-inapplicable fields must be absent to avoid stale data overriding the selected measurement. Decimal dot/comma input is supported without thousands separators; nonfinite values are rejected. No clinical advice, automatic interpretation or chart is added.

- Consultation gestational age is manually recorded historical text, independent of the current pregnancy dating. Follow-up is narrative text; next-visit date does not create an appointment or notification. Attachment text references are implemented; appointment linking is still pending. Storage is isolated (Android pregnancy_consultations.gpr, Keystore consultations.v1; iOS consultations service/account). Codec v2 (reads v1) limits: 1,000 records, 1,000,000 payload characters, ID128, gestational age/doctor/attachment512, each narrative4096; strict civil dates. Corruption/unknown versions and I/O failures remain explicit and prevent overwrite.

- Notes use a separate serialized repository and bounded versioned codec, with an isolated Android AES-GCM/Keystore file/key in noBackupFilesDir and provisional iOS Keychain item. Limits: 1,000 notes, 1,000,000 payload characters, 512 title characters and 4,096 body characters. Notes are independent of pregnancy dating. Follow-up dates do not schedule notifications. Prototype appointment-to-note links are not exposed in this slice.

- Implement dating calculation first because it is a pure, independently testable rule used by onboarding, profile, and dashboard.
- The first slice kept dating ephemeral. Subsequent slices added separate protected stores for all six Records sections and personal reminders; sync remains unimplemented.
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
- Physical-device validation of all six implemented Records sections, navigation/forms, appearance and design parity. Standalone personal reminders and consultation text references are implemented. Appointment associations, export/reset, demo and AI remain open migration items; native file attachments/notification delivery are additional features. Auth/sync and advertisements follow the user's later phase.

## Known bugs and limitations

- Personal reminders: Android pregnancy_reminders.gpr / com.jecsdev.gestiora.personalReminders.v1 / GRM01; iOS personalReminders service / pregnancy-reminders-v1 account. Codec v1 accepts up to 1,000 entries/1,000,000 payload characters, ID128/title512, strict dates/optional HH:mm/kind/boolean. Native iOS remains uncompiled. Reminder marks belong to the Recordatorios calendar, not the Citas calendar.

- Calma editorial components are implemented across the app; rendered visual/accessibility validation is still incomplete. Home and Calendar were inspected on the physical Samsung in dark mode, and reminder CRUD/restart was exercised. Physical-device visual/TalkBack checks, long names/narratives, large fonts, landscape and iOS remain pending. The proposal's example data was not introduced into the app. An earlier slice detected only an emulator; the 2026-11-16 closure slice used the physical Samsung without using the emulator.

- Welcome/inset correction: Android visual confirmation at normal/large font sizes, landscape, gesture/three-button navigation and iOS safe areas remains pending. ADB listed only an existing emulator during this slice; it was not used for tests under the user's physical-device-only preference. Do not claim pixel parity with React: gradient/decorative icons and deferred demo/account actions are not part of this spacing fix.

- Date labels deliberately use Spanish, matching the current UI and React `es` formatter; a stored profile language is not an app-language selector. Calendar/picker TalkBack reading and layout with large fonts still require physical-device verification. Missing dates remain explicit; malformed nonempty dates display `Fecha no válida` instead of being normalized into another civil day.

- Appearance: native preference adapters and system-bar appearance are not device-tested; iOS NSUserDefaults/controller integration is uncompiled on Windows. Until the initial asynchronous read completes, the app uses System; a brief appearance change at startup is possible. Android writes use checked synchronous SharedPreferences commit off the UI thread; iOS uses normal NSUserDefaults write semantics, without claiming synchronous disk durability. Only a non-medical display setting goes into these stores. Device/system theme switching and return from forced appearance need runtime verification.

- Examinations storage: Android pregnancy_exams.gpr / com.jecsdev.gestiora.examRecords.v1 / GEX01; provisional iOS examRecords service / pregnancy-exams-v1 account. Codec v1: 1,000 records/1,000,000 payload chars, ID128, name/physician/attachment512, results/notes4096; strict enums/dates. No actual document attachment storage.

- Vaccination storage: Android pregnancy_vaccines.gpr / com.jecsdev.gestiora.vaccineRecords.v1 / GVC01; provisional iOS vaccineRecords service / pregnancy-vaccines-v1 account. Codec v1: 1,000 records/1,000,000 payload chars, ID128, name/dose/provider/location/proof512, notes4096; strict dates/status. No actual proof attachments.

- Conditions are local records of professional-provided information. Follow-up dates do not schedule appointments or notifications. Codec v1 limits: 1,000 records/1,000,000 payload characters, ID128, required name/professional512, each narrative4096, strict civil dates/enums. Android pregnancy_conditions.gpr / com.jecsdev.gestiora.medicalConditions.v1 / GCD01; provisional iOS service com.jecsdev.gestiora.medicalConditions/account pregnancy-conditions-v1. Device encryption/UI and iOS compile/run remain unverified.

- Health records have calendar dates without a time. Same-day summary ties use a deterministic ID order and cannot establish the actual most recent measurement time. Prototype numeric input bounds are retained; clinical review and physical-device validation remain pending. Vitals storage is isolated: Android pregnancy_vitals.gpr / com.jecsdev.gestiora.vitalRecords.v1 (GVT01 authenticated envelope), iOS service com.jecsdev.gestiora.vitalRecords / pregnancy-vitals-v1. Codec v1 limits 1,000 records, 1,000,000 payload characters, ID128, label512, text/notes4096.

- Draft/navigation retention across configuration recreation is implemented but not verified on a real Activity/device. Host tests cover the actual retention store and resource callbacks, not the Android owner or rendered forms. Process-death recovery of unsaved drafts is unsupported by design. Unconfirmed date/time dialogs and scroll positions may reset after recreation.
- Consultations: attachment text references are implemented; appointment linking is not implemented. Android protected-store and Compose interaction need physical-device validation; iOS Keychain adapter remains uncompiled on Windows.

- Profile, appointments, notes, consultations, health records, conditions, vaccinations and examinations are persisted locally. Sync, export/recovery, demo/account branches and account deletion are not implemented; no backup or device transfer is available.
- Appointment list/create/edit/delete, date filtering, inline month grid and reminder preferences are implemented. Independent personal reminders are implemented; system notifications are not implemented. Host tests use a fake store; Android Keystore, persistence and UI interactions have not been tested on a device, and the iOS adapter has not been compiled or tested.
- The shared Material date picker, including accessibility and localization, still needs physical-device visual and interaction validation.
- A physical Samsung phone was previously detected over Wi-Fi; ADB listed no connected devices on 2026-11-12. No profile or UI/storage test was performed.
- iOS build/run requires macOS and Xcode.
- iOS Keychain interop is provisional: Kotlin/Native CF pointer signatures and actual Keychain read/write must be compiled/tested on macOS. A Keychain item may survive uninstall/reinstall on the same device; deletion policy is unresolved.
- Bundled fonts compile on Android; visual verification on device and iOS is pending.
- The user stated that the reference illustrations were generated with Lovable; rights for distribution have not been verified. Development copy is inherited/adapted from the prototype and has no documented clinical review; obtain review before release.
- Navigation, screen insets, larger text, TalkBack and baby explorer interactions remain unverified on a device. AI assistant is not included.
- Prototype localStorage is unencrypted and its cloud sync is last-write-wins; do not port either behavior blindly for medical data.

## Tests and build

- Closure slice (2026-11-16): Android assembleDebug plus domain use-case/repository/ViewModel/UI host tests passed with offline/no-configuration-cache/no-watch-fs: 5 + 38 + 13 + 28 = 84 tests, zero failures. New tests cover reminder corruption/concurrent CRUD/validation/IO, ViewModel IDs/cancellation, and consultation v1 compatibility. Samsung SM-A546E: install -r preserved visible existing data; dark Home/calendar inspection; temporary reminder create, process force-stop/reload, edit completion, delete confirmation and resulting empty state passed. Reminder encrypted envelope present and test title absent from plaintext bytes; no tamper test and no claim for every store. No emulator/iOS test. Read-only navigation smoke opened all six Records sections, Baby and Profile. Light theme survived process restart, then the app was left in Dark. Profile Back/discard returned without saving. Final compact Records headings passed Android build/UI host tests. See MIGRATION_CLOSURE.md.

- Calma editorial Home/navigation slice (2026-11-16): `:androidApp:assembleDebug :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache --no-watch-fs` passed. Existing host tests verify data-selection/policy regressions; they do not verify rendered Compose layouts or touch interactions. `git diff --check` passed. No new tests were added for cosmetic layout; physical-device and iOS checks remain pending.

- Welcome/insets slice (2026-11-16): `:androidApp:assembleDebug :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache --no-watch-fs` passed. Existing UI host tests are regression checks, not Compose layout or device verification. No emulator interaction or iOS build performed.

- 2026-11-14 Spanish dates: `:androidApp:assembleDebug :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed; 28 UI host tests, zero failures, including three date-label tests. `git diff --check` passed. ADB devices and mDNS discovery were empty; no physical-device or TalkBack test was possible, and no emulator was used. iOS remains uncompiled on Windows.

- Appearance preference (2026-11-13 local): Android debug build and `:shared:data:repository:testAndroidHostTest :shared:presentation:viewmodels:testAndroidHostTest :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed: 34 repository + 12 ViewModel + 25 UI tests = 71, zero failures. Two new repository tests cover absent preference without writing, all modes across new repository instances, unsupported values/read failures without rewriting, explicit write failure and recovery. One ViewModel test covers success, failure propagation and cooperative cancellation. Final Android/UI build rerun after system-appearance integration. No phone was connected and mDNS returned no services; physical appearance/persistence, OS theme updates, accessibility and rotation remain untested. iOS not compiled.

- Home/Baby parity (2026-11-12 local): `:androidApp:assembleDebug :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed, 25 UI host tests/0 failures. Existing baby-stage/reference boundary and upcoming-appointment tests remain passing. No extra tests were added to mirror reversible visual changes. Read-only review confirmed asset mapping, null/out-of-range handling and Baby callback wiring. Layout thresholds are implementation choices, not visually verified measurements. ADB devices and mDNS reported no available phone/service; no screenshot, TalkBack or click/rotation interaction test performed. iOS remains uncompiled.

- MVP flow hardening (2026-11-12 local): Android debug build and `:shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed, 25 UI host tests/0 failures. Three new policy tests cover saving/back/discard/step decisions, preserving historical/inactive dating fields and null-day normalization, and requiring ongoing validation for new/changed dates. Read-only review found and prompted submit-time date revalidation to avoid a stale day at final confirmation. Accessibility semantics compile but TalkBack and actual Back/dialog interactions are not device-verified. ADB devices and mDNS both listed no available phone/service.

- Draft retention (2026-11-12): `:androidApp:assembleDebug :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache` passed; 22 UI host tests, 0 failures. Three new tests directly exercise ManagedRetainedValuesStore, draft identity/content, same Koin singleton, an in-flight suspended write completing once, retirement/unconsumed-resource cleanup and abandoned-resource cleanup. An attempted host Composition integration failed on Android Log stubs; it was replaced by real-store lifecycle tests without adding mocks or test frameworks. A test lifecycle setup mistake (exit before enter) was corrected, then the full command passed. No real Activity recreation or physical UI test performed. Wi-Fi ADB service advertised the phone but connection timed out (10060). iOS not compiled.

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

- Read MIGRATION_CLOSURE.md first. The app-wide design implementation and local reminder/reference gaps are closed in code; full migration is still open. Review remaining parity scope with the user, then implement the database as an incremental data-layer replacement with protected, transactional/idempotent import. Export/reset should use a consistent database transaction/snapshot. Keep iOS/device/AI/demo/association gaps explicit.

1. On a physical Android phone, verify onboarding and appointments save, process restart/reload, edit/delete, ciphertext tamper/error states, navigation and UI layout without overwriting existing data. No emulator requested by the user.
2. On macOS, compile iOS framework and test all nine Keychain items and their save/reload/delete behavior; correct any Kotlin/Native interop issues.
3. Resolve recovery/export and uninstall/account-deletion policy before making portability promises.
4. Verify navigation, baby explorer, date picker and typography on the physical device, including accessibility and localization. Obtain clinical copy review and confirm illustration distribution rights.
5. Validate Profile appearance selection across restart, rotation, system dark-mode changes and return to System, including native bars and failed writes. Then choose the next remaining local parity slice from the reference; all six prenatal Records sections already have CRUD.

Next notes checks: physical-device CRUD, discard/delete confirmations, follow-up dates, restart persistence and tamper/error behavior. Compile/test notes Keychain on macOS. Next MVP-local work: validate the implemented draft retention with real Activity recreation (including in-flight save/delete, Ready/Empty root, section routing and explicit discard), then physical-device UI/persistence/navigation/accessibility checks against the prototype. Independent reminders are implemented; native document attachments and appointment links remain pending; auth/sync is a later slice.
