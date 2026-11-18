# Migration checklist

Statuses: Not started, In progress, Implemented, Verified, Blocked. Verified requires a relevant executed test.

| Feature | Status | Evidence / next step |
| --- | --- | --- |
| React inventory and KMP mapping | Implemented | Inspected routes, store, models, dating, styles and assets; mapping below. |
| Shared design system | Implemented | Light/dark palette and bundled Plus Jakarta Sans/DM Sans applied; Android build passes, Reusable editorial components/shapes are shared across screens; full rendered/accessibility verification remains pending. |
| ES/EN/FR localization | Implemented | Shared Compose XML resources, localized labels/errors/accessibility/dates/FAQ and source metadata display; Android/iOS locale declarations. Real XML contracts and localized date/user-text tests pass. Full device/language/clinical review pending; see LOCALIZATION_AND_MOTION.md. |
| FAQ and navigation motion | Implemented | Finite FAQ fade/expansion and entry-only destination/form fade/slide. Native reduced-motion adapters; no background timers or auto-rotation. Android build passes; motion/device accessibility and iOS verification pending. |
| Home WHO questions | Implemented | Reuses two WHO-backed answers with manual previous/next, position and citations; selection test passes. Phone visual/source-link checks pending. |
| Room integration | In progress | All nine Android stores use AES-GCM Room payloads and two transactional/idempotent import groups; legacy sources retained. 103 host tests pass; all five isolated device tests passed on Samsung; English library smoke/restart passed. iOS remains on Keychain. See ROOM_PERSISTENCE.md; native verification and source cleanup pending. |
| Calma editorial redesign | Implemented | User approved the Calma editorial proposal. Home hierarchy, week/artwork hero, separate date summary, calendar-date appointment previews, quieter follow-up rows and navigation styling implemented. Profile editing lives in Profile. Android build and UI host regressions pass; Editorial headers/cards/status chips, shapes and wrapping actions now cover Calendar/Records/forms/Baby/Profile/onboarding. Samsung dark Home/calendar inspected; broader phone/TalkBack/large-font and iOS checks pending. |
| Welcome and screen spacing | Implemented | Welcome follows the reference's viewport-height composition, proportional illustration and full-width lower panel, with scroll fallback. Drawing-safe insets replace gesture-safe padding across screen containers to remove gesture-related lateral margins. Physical-device/large-font/landscape and iOS visual validation pending; demo/account actions remain deferred. |
| Spanish date presentation | Implemented | Shared compact Spanish civil-date labels across forms, Home, Profile, calendar and all Records sections; expanded picker/calendar accessibility labels and calendar selection semantics. ISO storage/filtering unchanged. Host coverage for months, leap days, year boundaries and invalid/missing dates; device/TalkBack and iOS validation pending. |
| Appearance preference | Implemented | Profile offers System/Light/Dark; root applies the saved selection across the app. Separate Android SharedPreferences/iOS NSUserDefaults adapters, serialized repository and explicit load/save errors. Android build and repository/ViewModel host tests pass; Samsung light/dark switching and light persistence after process restart checked; System/OS transitions, broader system-bar cases and iOS compile/run pending. |
| Drafts and configuration recreation | Implemented | In-memory Compose retain for root/navigation/forms, retained Koin graph/operation scopes, no app subtree SavedState serialization. Android build and 22 UI host tests pass, including three resource/store lifecycle tests. Actual Activity recreation/in-flight saves on phone and iOS pending; process-death draft recovery unsupported. |
| Shared navigation | Implemented | Five-destination shell, profile summary and appointment list implemented; notes in Records implemented; Samsung smoke opened all five destinations/all six Records sections. Full interaction/accessibility matrix pending. |
| Pregnancy dating calculation | Verified | Five Android host tests pass; FUM, due date, medical/ultrasound reference, leap day, invalid input. |
| Dating input and preview | Implemented | Shared Compose onboarding and Material date picker; Android build and ISO/UTC conversion tests pass, device and accessibility validation pending. |
| Profile/onboarding Back and historical edits | Implemented | System Back and visible Back share step policy; saving consumes Back, profile exit confirms discard. Personal-detail edits preserve unchanged historical dating; changed/new dates still validate at submission. Three policy tests and Android build pass; Samsung profile Back/discard checked without saving; other steps, in-flight operations and TalkBack pending. |
| Onboarding and pregnancy profile | Implemented | Three-step form and encrypted local profile adapter; Android build/repository and presentation host tests pass. Device storage validation pending. |
| Pregnancy dashboard | Implemented | Profile-backed pregnancy summary, shared reference illustration, weekly-stage card linking to Baby, upcoming appointments, note tasks and medical follow-up. Adaptive hero/date layout implemented; Android build/UI host tests pass; Samsung Home inspected in dark/light. Adaptive/large-font/landscape interaction tests pending. |
| Medical appointments and calendar | In progress | CRUD, date filtering, month grid, 24-hour time input and persisted reminder preferences implemented; compatible codec migration and host tests pass. Independent personal reminders now have protected CRUD, date filters, five kinds, all-day/time and completion in a separate calendar mode. Samsung reminder create/edit/delete/restart passed; notifications and full device/iOS validation pending. |
| Independent personal reminders | Implemented | Protected CRUD, five kinds, all-day/optional time, completion, calendar/filter and edit/delete/discard locks. Repository/ViewModel tests and Samsung create/restart/edit/delete/envelope smoke passed. No system notifications; iOS pending. |
| Consultation history | Implemented | Local CRUD, historical gestational age and narrative fields, optional next-visit date, isolated protected store and Records tabs. Android build and repository/ViewModel host tests pass; Attachment text references and v1-to-v2 codec migration implemented/tested; clinical device/iOS checks and appointment links pending. |
| Vital signs and measurements | Implemented | Eight record types, protected local CRUD, latest weight/BP summary, finite decimal input and type-specific fields. Android build and repository/ViewModel/UI helper tests pass. Device encryption/UI and iOS checks pending; no clinical interpretation. |
| Conditions and risks | Implemented | Protected local CRUD, professional/date, three statuses, treatment/monitoring/notes and optional follow-up; Home first non-resolved card opens Conditions. Android build and repository/ViewModel/draft/selection host tests pass; device/iOS pending. |
| Vaccinations | Implemented | Protected local CRUD, four statuses, scheduled/application dates and proof text references. Application date required for Applied; historical dates retained across state changes. Android build and repository/ViewModel/draft tests pass; device/iOS pending. |
| Examinations and results | Implemented | Protected CRUD, five study types/three statuses, requested/scheduled/completed dates, results/medical notes and document text reference. Completion date required when completed; historical dates preserved. Android build and repository/ViewModel/draft tests pass; device/iOS pending. |
| Notes and follow-up tasks | Implemented | Local CRUD, categories, task/date, combined text/status filters, Home pending summary and isolated store adapters. Android build and repository/ViewModel/query tests pass; device/iOS checks and appointment linking pending. No notifications. |
| Baby development and illustrations | Implemented | Offline weeks 4–40, stages and seven reference illustrations shared with Home; selected week retained and controls spaced for large text. Boundaries tested. Device, clinical copy and asset rights review pending; AI separate. |
| Local export and reset-all | Not started | Reference Profile has JSON export/reset. Implement consistent export and transactional reset on the new data layer; not removed from scope. |
| Consultation/note appointment associations | Not started | Model/demo association in React is not yet represented in KMP. Define historical deletion behavior when adding DB relations. |
| Demo mode | Not started | Must be isolated from real medical data; never replace existing records implicitly. |
| AI assistant (future update) | Not started | Deferred explicitly by the user on 2026-11-16; outside current migration closure. No AI backend is configured. Revisit managed server-side options, provider access, usage limits and privacy in a later feature. |
| Offline sourced educational questions | Implemented | Seven Spanish FAQ summaries with specific OMS/UNICEF/NHS citations, date consulted, stage selection and external-source links. Android build and 30 UI host tests pass. Clinical review, device link/render checks and iOS pending; see EDUCATIONAL_CONTENT.md. |
| Authentication and account deletion | Not started | Deferred by user until MVP-local stability; then inspect Lovable Cloud backend. |
| Advertisement sections | Not started | Deferred by user until current local migration/stability work is complete. |
| Secure offline persistence | In progress | Separate profile, appointment, note, consultation, health-record, condition, vaccine and exam Android Keystore and iOS Keychain adapters; fake-store host tests pass, physical-device roundtrip/tamper and iOS validation pending. Includes the ninth protected store for personal reminders. Samsung reminder roundtrip/envelope passed; no backup/sync. |
| Cloud sync | Not started | Inspect owner isolation and backend contracts before implementation. |
| Android verification | Implemented | Debug APK and 84 host tests pass; Samsung reminder CRUD/restart/envelope smoke and dark Home/calendar inspected. Full clinical/device/accessibility matrix pending. |
| iOS verification | Blocked | Requires macOS/Xcode. |

## Prototype route to KMP mapping

| React route/source | KMP target |
| --- | --- |
| `/onboarding`, `lib/pregnancy.ts` | Domain dating + profile, encrypted storage adapter, three-step shared Compose onboarding. Demo/account branches pending. |
| `/` dashboard | Profile-backed pregnancy summary and upcoming appointments in `presentation:ui`; medical condition follow-up card implemented. |
| `/calendario` | Month grid and local appointment list and CRUD with separate protected storage; reminders pending. |
| `/registros` | Consultation, vital, condition, vaccine, exam, note CRUD. |
| `/bebe` | Offline weekly development and seven bundled reference illustrations; AI deferred to a future update; content/asset reviews pending. |
| `/perfil` | Profile and dating edits, data export/reset, account actions. |
| `/auth`, `/reset-password` | Optional cloud account integration after backend contract review. |

## Closure audit

See [MIGRATION_CLOSURE.md](MIGRATION_CLOSURE.md) for the 2026-11-16 reference comparison, 84 passing host tests, Samsung reminder CRUD/restart/envelope checks and explicit remaining gates. Database integration is the next objective; the full migration is not marked complete.
# Educational catalog extension — 2026-11-17

Local catalog implemented; see EDUCATIONAL_CATALOG.md for exact scope and test limits. Remote publishing/sync and monetization remain planned in EDUCATIONAL_CONTENT_SYNC_PLAN.md.

| Feature | Status | Scope |
| --- | --- | --- |
| Room educational catalog and seed | Implemented; native tests passed | Separate v1 database, seven resource-backed ES/EN/FR entries, per-locale atomic seed, multiple citations and distinct dates. iOS builder uncompiled. |
| Offline search and favorites | Implemented; English UI/SQLite passed | Literal local substring search with ES/FR folding; independent favorites shared across locales. Host and isolated native tests pass; English UI/restart passed, ES/FR visual review pending. |
| Supabase editorial publishing and catalog sync | Not started | Published-only RLS, stable server cursor/snapshot, tombstones, transactional apply/checkpoint, failure/retry tests. |
| Educational disclaimers and publication review | Partial | Library has visible disclaimer and dates/citations. Onboarding/Play audit, clinical/editorial review and assigned owner remain pending. |
| Non-interruptive AdMob | Not started | Later phase; category controls, privacy/consent assessment and actual creative review. |
