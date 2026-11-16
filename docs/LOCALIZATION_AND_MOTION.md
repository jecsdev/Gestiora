# Localization, motion and Home questions

Implemented 2026-11-16. Room is the user's selected next persistence technology; this slice does not introduce a database.

## Strings and locale selection

- Shared Compose resources live in `shared/presentation/ui/src/commonMain/composeResources/values*/strings.xml`. Spanish, English and French have matching keys and positional placeholders. Default resources and `values-en` are English, so unsupported languages fall back to English.
- `UiCopyProvider` resolves the generated, typed `StringResource` catalog through Compose `stringResource` at the app root. The immutable `UiCopy` is also available to event callbacks and pure presentation formatters, where calling a composable is not possible. It is not a second translation service and does not maintain a separate language preference.
- Navigation, forms, validation/save/read messages, accessibility descriptions, dates, stage text and educational answers use the resources. URLs, stable IDs, ISO date/time encoding, numeric units and canonical stored country names remain technical/data values.
- Never translate or rewrite patient-entered text, historical clinical narratives, IDs or existing profile metadata. Country/language labels are localized at display time; country choices retain their existing stored values. New profiles record the current supported language code; edits preserve the existing profile language. Pregnancy dating rules and historical metadata remain unchanged.
- Locale comes from the platform. Android declares the three locales in `locales_config.xml`, enabling the OS per-app language setting on supported versions. iOS declares them in Info.plist. The stored profile language remains metadata, not an in-app language selector.
- Calendar months/weekdays and date accessibility labels are localized. Civil dates remain ISO in domain/storage/filtering and are not converted through a device time zone. Date formatting is resource-driven; English and French ordering can differ from Spanish.
- Formatting substitutes numbered tokens in one pass. Text supplied by a user is not parsed again as a format string, and a literal percent sign remains literal.
- For new copy, add all three translations and preserve numbered placeholders. NHS/UNICEF publication titles retain their published language; the WHO citation uses a localized descriptive label with its original official URL. Institution names and explanatory labels are localized. Medical translations are editorial adaptations and still need independent clinical/language review before release.

## Motion

- FAQ answers use a 220 ms fade plus vertical expansion/collapse. A collapsing answer stops exposing its source action to accessibility and disables that action while it exits.
- Main destinations enter with a short fade and 12 dp horizontal displacement. Forms enter with 28 dp displacement. This is entry-only motion: the previous destination is disposed using the existing navigation policy, rather than keeping a hidden outgoing screen and its editable callbacks alive for a crossfade.
- The entrance completion flag is retained across configuration recreation. An already-entered screen does not restart its entrance merely because the Activity is recreated. Form values and in-flight operations retain their existing ownership/policies.
- Android observes animator duration scale; when animations are disabled, all added motion resolves immediately. The provisional iOS adapter observes Reduce Motion and its change notification. iOS compilation/runtime verification requires macOS/Xcode.
- There are no infinite animations, automated slides or timers. No data write is triggered by an animation.

## Home questions

Home reuses the same two WHO-backed educational answers already in the catalog: prenatal contacts and early ultrasound. It displays one answer at a time with explicit previous/next controls and a position label. Each card has its own official source, source-language label and consultation date. These are summaries of WHO material, not institutional endorsement or individual care plans.

The full Baby FAQ keeps movement-change guidance available across every supported explorer week. It is not placed in a rotating card. FAQ content remains bundled/offline; opening a source requires internet.

## Verification and open checks

- Android debug build passes, using offline/no-configuration-cache/no-watch-fs.
- 91 executed host tests passed: 5 use-case, 38 repository, 13 ViewModel and 35 UI tests. The latter include real XML locale-key/placeholder parity, ES/EN/FR date formatting, preservation of user text with formatting-like characters, calendar boundaries, historical dating/drafts and Home's WHO-only selection. Cosmetic animation timings are not asserted by these tests.
- Physical Samsung: the first pass rendered Home and WHO cards in ES/EN/FR, with working manual next/previous in EN/FR. It exposed the Spanish position-label format fragment and visible apostrophe escapes in French; both were corrected and covered by host regression checks. English form/date text also rendered. The final corrected-build device pass was interrupted by automatic screen lock; no user draft was discarded or saved. The original empty/system per-app locale selection was restored. Final corrected screenshots, FAQ animation and native reduced-motion toggling still need the unlocked phone.
- Full UI inspection in all three languages, large text/landscape, TalkBack/VoiceOver focus, rapid navigation, native motion-setting toggles, source-link delivery and iOS remain open until separately exercised. Do not describe host tests as rendered-device verification or these translations as clinically approved.


Session stop (2026-11-16, requested by user): the final Samsung locale pass completed successfully for ES/EN/FR before shutdown. In each language, Home title, manual second/previous WHO card, clean formatting and absence of visible apostrophe escapes passed. Original system app-locale selection and charging-screen preference were restored; temporary phone captures were removed. No further device work was performed. FAQ animation/reduced-motion toggles, full accessibility matrix and iOS remain pending. Code/build status remains 91 passing host tests; no commit/push performed. Next implementation: user-selected Room, after reviewing this UI slice.
