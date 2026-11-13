# Migration checklist

Statuses: Not started, In progress, Implemented, Verified, Blocked. Verified requires a relevant executed test.

| Feature | Status | Evidence / next step |
| --- | --- | --- |
| React inventory and KMP mapping | Implemented | Inspected routes, store, models, dating, styles and assets; mapping below. |
| Shared design system | In progress | Light/dark palette and bundled Plus Jakarta Sans/DM Sans applied; Android build passes, visual verification and reusable components pending. |
| Drafts and configuration recreation | Implemented | In-memory Compose retain for root/navigation/forms, retained Koin graph/operation scopes, no app subtree SavedState serialization. Android build and 22 UI host tests pass, including three resource/store lifecycle tests. Actual Activity recreation/in-flight saves on phone and iOS pending; process-death draft recovery unsupported. |
| Shared navigation | In progress | Five-destination shell, profile summary and appointment list implemented; notes in Records implemented; device/accessibility checks pending. |
| Pregnancy dating calculation | Verified | Five Android host tests pass; FUM, due date, medical/ultrasound reference, leap day, invalid input. |
| Dating input and preview | Implemented | Shared Compose onboarding and Material date picker; Android build and ISO/UTC conversion tests pass, device and accessibility validation pending. |
| Profile/onboarding Back and historical edits | Implemented | System Back and visible Back share step policy; saving consumes Back, profile exit confirms discard. Personal-detail edits preserve unchanged historical dating; changed/new dates still validate at submission. Three policy tests and Android build pass; phone/TalkBack interaction pending. |
| Onboarding and pregnancy profile | Implemented | Three-step form and encrypted local profile adapter; Android build/repository and presentation host tests pass. Device storage validation pending. |
| Pregnancy dashboard | Implemented | Profile-backed pregnancy summary, shared reference illustration, weekly-stage card linking to Baby, upcoming appointments, note tasks and medical follow-up. Adaptive hero/date layout implemented; Android build/UI host tests pass, visual/device test pending. |
| Medical appointments and calendar | In progress | CRUD, date filtering, month grid, 24-hour time input and persisted reminder preferences implemented; compatible codec migration and host tests pass. Notifications, standalone reminders and platform/device validation pending. |
| Consultation history | Implemented | Local CRUD, historical gestational age and narrative fields, optional next-visit date, isolated protected store and Records tabs. Android build and repository/ViewModel host tests pass; device/iOS checks, attachment references and appointment links pending. |
| Vital signs and measurements | Implemented | Eight record types, protected local CRUD, latest weight/BP summary, finite decimal input and type-specific fields. Android build and repository/ViewModel/UI helper tests pass. Device encryption/UI and iOS checks pending; no clinical interpretation. |
| Conditions and risks | Implemented | Protected local CRUD, professional/date, three statuses, treatment/monitoring/notes and optional follow-up; Home first non-resolved card opens Conditions. Android build and repository/ViewModel/draft/selection host tests pass; device/iOS pending. |
| Vaccinations | Implemented | Protected local CRUD, four statuses, scheduled/application dates and proof text references. Application date required for Applied; historical dates retained across state changes. Android build and repository/ViewModel/draft tests pass; device/iOS pending. |
| Examinations and results | Implemented | Protected CRUD, five study types/three statuses, requested/scheduled/completed dates, results/medical notes and document text reference. Completion date required when completed; historical dates preserved. Android build and repository/ViewModel/draft tests pass; device/iOS pending. |
| Notes and follow-up tasks | Implemented | Local CRUD, categories, task/date, combined text/status filters, Home pending summary and isolated store adapters. Android build and repository/ViewModel/query tests pass; device/iOS checks and appointment linking pending. No notifications. |
| Baby development and illustrations | In progress | Offline weeks 4–40, stages and seven reference illustrations shared with Home; selected week retained and controls spaced for large text. Boundaries tested. Device, clinical copy and asset rights review pending; AI separate. |
| AI assistant | Not started | Inspect gateway contract and privacy. |
| Authentication and account deletion | Not started | Deferred by user until MVP-local stability; then inspect Lovable Cloud backend. |
| Advertisement sections | Not started | Deferred by user until current local migration/stability work is complete. |
| Secure offline persistence | In progress | Separate profile, appointment, note, consultation, health-record, condition, vaccine and exam Android Keystore and iOS Keychain adapters; fake-store host tests pass, physical-device roundtrip/tamper and iOS validation pending. No backup/sync. |
| Cloud sync | Not started | Inspect owner isolation and backend contracts before implementation. |
| Android verification | Implemented | Debug APK built; host tests pass; physical-device UI/storage verification pending. |
| iOS verification | Blocked | Requires macOS/Xcode. |

## Prototype route to KMP mapping

| React route/source | KMP target |
| --- | --- |
| `/onboarding`, `lib/pregnancy.ts` | Domain dating + profile, encrypted storage adapter, three-step shared Compose onboarding. Demo/account branches pending. |
| `/` dashboard | Profile-backed pregnancy summary and upcoming appointments in `presentation:ui`; medical condition follow-up card implemented. |
| `/calendario` | Month grid and local appointment list and CRUD with separate protected storage; reminders pending. |
| `/registros` | Consultation, vital, condition, vaccine, exam, note CRUD. |
| `/bebe` | Offline weekly development and seven bundled reference illustrations; AI integration and reviews pending. |
| `/perfil` | Profile and dating edits, data export/reset, account actions. |
| `/auth`, `/reset-password` | Optional cloud account integration after backend contract review. |
