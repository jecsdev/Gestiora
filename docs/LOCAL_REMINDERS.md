# Local appointment and examination reminders — Android v1

2026-11-17. No backend, login, push service or network permission is required.

## Behavior

- Scheduled appointments honor their existing None / 1 hour / 1 day / 2 days / 1 week preference. Completed, cancelled and deleted appointments are excluded. Offsets in days follow the local civil calendar, including DST; a one-hour offset is an elapsed hour.
- Examinations have dates but no appointment time. Their new explicit opt-in means **09:00 on the preceding local day**, only with status Scheduled and a scheduled date. This is never represented as the examination time. Existing v1 exam payloads decode with the option disabled; explicit saves encode v2, preserving all previous fields.
- A contextual button requests notification permission after a tap on Android 13+, or opens app notification settings after a previous request. Global and channel disabling are detected on Activity resume. No request at onboarding/startup.
- One inexact `AlarmManager.setAndAllowWhileIdle` alarm targets the next pending reminder. Android can delay it, especially in idle/battery-saving states. The UI explicitly explains this. No exact-alarm entitlement is requested. See [Android alarm documentation](https://developer.android.com/develop/background-work/services/alarms).
- Saving/editing/deleting agenda records sends a payload-free explicit broadcast after durable storage succeeds. Boot, package replacement, clock/zone changes and foreground entry reconcile the schedule. A single process-owned Koin graph shares repository locks between the Activity and receivers.
- At delivery the receiver rereads protected Room content. Changed/cancelled/completed/deleted records cannot reuse the former delivery token. Persisted SHA-256 delivery/armed tokens prevent routine duplicate delivery; notification tags also replace a repeated post if the process dies between posting and recording delivery.
- Only previously armed reminders may be delivered late, up to 24 hours and before the event expires. Newly discovered overdue reminders are not sent as catch-up notifications. Exams expire at the end of their scheduled date; appointments expire at the appointment time. Permission-denied delivery attempts do not create a notification.
- Android force-stop disables background delivery until the app is opened again; normal process death differs from force-stop. Notifications cannot be guaranteed after device-specific task restrictions. The app makes no emergency/treatment alarm promise.
- Notices contain generic appointment/exam text, not names, physicians, results or medical details. Tapping opens Gestiora, preserving existing navigation/form state when possible. Titles, channel and body are localized ES/EN/FR. User settings control lock-screen display; the app requests private visibility.
- A storage/authentication failure is not treated as an empty agenda. The existing schedule is not replaced with an empty list; a local 15-minute retry is scheduled and the UI exposes a retry state. A notification scheduling failure cannot invalidate an already committed clinical edit.

## Validation

Android debug build and 120 host tests pass: 5 host-level reminder policy tests plus 115 shared tests. Policy coverage includes all offsets, leap-day boundaries, opt-in/status eligibility, DST/zone changes, stable and changed identity, prior arming, expiry and deduplication. Repository tests check durable-save ordering and failure isolation; exam v1/v2 compatibility is covered.

Room/Keystore cleanup has six isolated Samsung instrumentation tests (see ROOM_PERSISTENCE.md). Those storage tests do not prove OS notification delivery. The main-package permission/alarm/notification smoke test is recorded separately in MIGRATION_HANDOFF.md when executed.

iOS system notification scheduling is not implemented. Shared UI explicitly reports this; the saved preferences do not imply iOS delivery. Work requires UserNotifications authorization/delegate/reconciliation and a macOS/Xcode test.
