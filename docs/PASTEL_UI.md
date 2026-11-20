# Pastel visual refresh — 2026-11-17

User-approved reference: the eleven Lovable screenshots supplied in this chat. This evolves the existing Calma editorial style; it does not replace the architecture or change clinical behavior.

- Shared light background fades from lavender through soft blush to warm cream. Dark uses plum/rose surfaces with light text. The saved app theme controls both palettes.
- Hero cards use lavender-to-rose gradients, rounded corners and subtle decorative circles; Home groups the week, preserved fetal artwork, progress and date summary in one compact card. Narrow screens/large text keep stacked fallbacks.
- Primary actions share GradientButton, retaining Material button behavior, enabled state and touch target. Gradient endpoints are darker than the screenshot's pink to preserve small-text contrast. Cards/chips and fields use shared round shapes; PastelTextField delegates all editing, keyboard, errors and accessibility to Material.
- Navigation sits in an elevated rounded surface above the system navigation inset; five destinations, translated labels and edit locks are retained. Scaffold reserves its height rather than covering content.
- Baby keeps its existing artwork and week selection; profile gets a pastel identity header. Health summaries use two pastel cards side by side where width/font scale allow. Empty record collections have consistent illustrated-icon surfaces.
- Onboarding retains its existing steps, validation and copy, with segmented progress exposing ProgressBarRangeInfo. Welcome keeps the existing baby illustration and localized disclaimer with an italic accent and gradient action.

No font, illustration, dependency, data schema, scheduling or source-content change is introduced. Phase-2 account/demo/advertising controls in the screenshots are not added by this visual change.

## Verification

`:androidApp:assembleDebug :shared:presentation:ui:testAndroidHostTest --offline --no-configuration-cache --no-watch-fs` passed: 36 host tests, no failures. These existing regression tests cover actual UI policy/localization helpers; they do not render the new UI.

One-off sRGB endpoint contrast calculation: light action text/gradient 6.42:1 purple and 5.52:1 rose; dark 7.13:1 and 7.38:1. Hero text/container endpoints are at least 8.20:1. These selected-color checks do not constitute a full accessibility audit.

Physical light/dark screenshots, large-font/landscape/TalkBack and iOS rendering require separate checks. Samsung was ADB-connected but locked at the initial visual verification attempt; user was asked to unlock it. See the latest handoff for any subsequent device result.

## Physical visual verification — 2026-11-17

Samsung SM-A546E / Android 16, ADB Wi-Fi, portrait, existing English app locale. User explicitly unlocked the phone and authorized both themes. Before installation, the retained date dialog was confirmed to belong to the unsaved GestioraExamTest draft from the previous test; that draft alone was discarded. Main-package adb install -r preserved the visible existing profile, clinical summary and appointments. Existing user launcher-icon changes were included in the build and left untouched.

Reviewed actual screenshots of Home, Baby, Calendar, Health records and Profile in dark (original SYSTEM resolved dark) and explicit LIGHT. Also reviewed the empty new-appointment form in LIGHT and explicit DARK; fields/dates, disabled navigation during editing and Back/discard confirmation rendered. No form was saved and no clinical record was added/edited/deleted in this visual pass.

The first Home screenshot exposed adjacent unbounded date-summary columns. Fixed with equal weighted columns and a 16dp gap. Shortened only the presentation counter to resource-backed days_count (EN/ES/FR), reduced shared section headings to titleMedium, and softened hero/nav shadows to 3dp. Final Home screenshots in LIGHT and explicit DARK confirm separate readable headings/values, compact counter, artwork beside the week and no clipping in those reviewed sections. Final APK installed on the phone.

Build and all 36 UI host regression tests passed after the final code/resource changes (build/pastel-device-build.log, ignored). git diff --check passed. Original appearance was SYSTEM, not DARK; restored SYSTEM and confirmed its radio row checked, then confirmed Your pregnancy/Home as final screen. Locale and clinical data were not changed. Temporary phone/local screenshots and UI dumps were removed after review. No emulator, commit or push.

Limits: this is an English portrait visual smoke, not complete accessibility or clinical CRUD coverage. ES/FR rendered layouts, large font, rotation, TalkBack, welcome/onboarding replay and iOS remain unverified in this slice. Export/reset remains the next agreed implementation; appointment associations, demo, login, AI and Supabase remain phase 2.
