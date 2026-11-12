# Migration checklist

Statuses: Not started, In progress, Implemented, Verified, Blocked. Verified requires a relevant executed test.

| Feature | Status | Evidence / next step |
| --- | --- | --- |
| React inventory and KMP mapping | Implemented | Inspected routes, store, models, dating, styles and assets; mapping below. |
| Shared design system | In progress | Light/dark palette and bundled Plus Jakarta Sans/DM Sans applied; Android build passes, visual verification and reusable components pending. |
| Shared navigation | In progress | Five-destination shell, profile summary and appointment list implemented; notes in Records implemented; device/accessibility checks pending. |
| Pregnancy dating calculation | Verified | Five Android host tests pass; FUM, due date, medical/ultrasound reference, leap day, invalid input. |
| Dating input and preview | Implemented | Shared Compose onboarding and Material date picker; Android build and ISO/UTC conversion tests pass, device and accessibility validation pending. |
| Onboarding and pregnancy profile | Implemented | Three-step form and encrypted local profile adapter; Android build/repository and presentation host tests pass. Device storage validation pending. |
| Pregnancy dashboard | Implemented | Profile-backed pregnancy summary and up to three upcoming local appointments, including read failure state; Android build and selection host test pass, visual/device test pending. |
| Medical appointments and calendar | In progress | CRUD, date filtering, month grid, 24-hour time input and persisted reminder preferences implemented; compatible codec migration and host tests pass. Notifications, standalone reminders and platform/device validation pending. |
| Consultations and vital signs | Not started | CRUD. |
| Conditions and risks | Not started | CRUD. |
| Vaccinations | Not started | CRUD. |
| Examinations and results | Not started | CRUD. |
| Notes and follow-up tasks | Implemented | Local CRUD, categories, task/date, combined text/status filters, Home pending summary and isolated store adapters. Android build and repository/ViewModel/query tests pass; device/iOS checks and appointment linking pending. No notifications. |
| Baby development and illustrations | In progress | Offline weeks 4–40, stages and seven reference illustrations implemented; boundaries tested. Device, clinical copy and asset rights review pending; AI separate. |
| AI assistant | Not started | Inspect gateway contract and privacy. |
| Authentication and account deletion | Not started | Inspect Lovable Cloud backend. |
| Secure offline persistence | In progress | Separate profile and appointment Android Keystore and iOS Keychain adapters; fake-store host tests pass, physical-device roundtrip/tamper and iOS validation pending. No backup/sync. |
| Cloud sync | Not started | Inspect owner isolation and backend contracts before implementation. |
| Android verification | Implemented | Debug APK built; host tests pass; physical-device UI/storage verification pending. |
| iOS verification | Blocked | Requires macOS/Xcode. |

## Prototype route to KMP mapping

| React route/source | KMP target |
| --- | --- |
| `/onboarding`, `lib/pregnancy.ts` | Domain dating + profile, encrypted storage adapter, three-step shared Compose onboarding. Demo/account branches pending. |
| `/` dashboard | Profile-backed pregnancy summary and upcoming appointments in `presentation:ui`; condition section pending. |
| `/calendario` | Month grid and local appointment list and CRUD with separate protected storage; reminders pending. |
| `/registros` | Consultation, vital, condition, vaccine, exam, note CRUD. |
| `/bebe` | Offline weekly development and seven bundled reference illustrations; AI integration and reviews pending. |
| `/perfil` | Profile and dating edits, data export/reset, account actions. |
| `/auth`, `/reset-password` | Optional cloud account integration after backend contract review. |
