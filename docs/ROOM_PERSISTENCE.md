# Room persistence — Android clinical stores

2026-11-17. Scope: all nine Android clinical stores (profile, reminders, appointments, consultations, vitals, conditions, vaccines, examinations and notes). All iOS bindings retain their previous Keychain adapters. Educational Room tables and Supabase synchronization are separate subsequent slices; see EDUCATIONAL_CONTENT_SYNC_PLAN.md.

## Implementation

- Room 2.8.5, SQLite bundled 2.6.2 and KSP 2.3.14. Shared entities/DAO/database in `data:datasources-core`; generated schema v1 is version-controlled under its `schemas/` directory. KSP is configured for Android and both existing iOS targets; no iOS database builder or encrypted database binding is enabled yet.
- `protected_payloads` holds one ciphertext blob per migrated store. `legacy_imports` holds the group completion marker. This is an incremental storage replacement, not a normalized clinical SQL model. Existing bounded codecs preserve IDs, civil dates, historical dating, nulls and Unicode without a decode/re-encode import.
- Android Koin switches all nine SecureStore bindings to Room bridges. Domain, repositories and ViewModels retain existing interfaces. Legacy adapters remain available only to the importer. The codec validator is registered by the repository module through a datasource contract, without reversing module dependencies.
- The database is `noBackupFilesDir/gestiora_clinical.db`, including its SQLite sidecars. The new non-exportable Keystore alias is `com.jecsdev.gestiora.room.clinical.v1`. Each blob uses AES-256-GCM with a fresh IV and authenticated envelope version plus store identity as AAD. Encryption happens before Room receives bytes; clinical plaintext is not deliberately passed to SQLite/WAL. Store names, schema, blob sizes and marker are visible metadata; this is not whole-database encryption.
- Existing synchronous storage contracts are bridged with runBlocking on Dispatchers.IO on Android. Main-thread calls are explicitly rejected; current repositories already dispatch storage work off main. A future suspend-contract change should be separate from the data migration.

## Import and failure behavior

First access checks the durable group marker. If absent, read/decrypt BOTH legacy profile and personal-reminder files, validate using their existing codecs, encrypt both payloads, then insert rows and the marker in one Room DAO transaction. An absent source is permitted and still results in a marker. Reads do not rewrite legacy versions.

A bad source, missing legacy key, failed encryption or failed transaction propagates an error. The importer does not treat failure as absence, does not silently initialize empty content and never rewrites/deletes either source. Existing unmarked database rows cause refusal rather than conflict replacement. The transaction rechecks the marker for competing import attempts. The production storage singleton serializes its operations with a Mutex.

After each group succeeds, the database is authoritative for that group. Restart/retry must not re-read stale source files. Profile clear removes only its database row; marker and shared encryption key remain so reminders still decrypt and the old profile is not reimported. Missing database key or authentication failure must not fall back to old files or generate a replacement key while ciphertext exists.

Legacy encrypted files/keys are deliberately retained for the migration verification window. **Profile clear is not complete erasure of the retained migration source.** Reset-all, source cleanup, uninstall/recovery and key lifecycle need an explicit tested policy before a release or privacy promise. Downgrading to an old APK would expose stale file-backed state; downgrade support is not provided. Do not delete the database manually to roll back user data.

A second durable marker (clinical-collections-v1) covers appointments, consultations, vitals, conditions, vaccines, examinations and notes. Access to one of these collections first ensures the original profile/reminder group is imported, then reads and validates all seven legacy sources before encrypting or committing the second group. Its DAO transaction checks the first marker and rejects existing rows in any second-group store, including absent legacy sources. Existing first-group rows are allowed and never replaced. A second-group failure leaves an already imported profile/reminder group accessible; successful/cleared first-group data is never reimported. Empty sources still receive a group marker. Both groups use the same unchanged schema v1, database and ciphertext key; no DDL migration is required. No app-wide transaction/export/reset guarantee is made yet. To normalize clinical entities later, define privacy-preserving query/index requirements and a tested schema migration rather than introducing plaintext medical columns.

## Verification

Nine common host tests use fake DAO/cipher implementations for import gating, source validation, retry, concurrency, empty imports, clear/reopen, corrupt ciphertext and failed writes. Three repository tests verify the real legacy codec validator accepts existing data and rejects unsupported/corrupt sources. Additional host cases verify first-group upgrades, all seven collections, edit/reopen without legacy resurrection, invalid sources, encryption failure/retry, absent-source conflicts and empty-group markers. Real codec mapping tests reject corrupt, unsupported and cross-store payloads. These host tests do not prove SQLite rollback or Android Keystore operation.

Three Android device tests use an isolated test-package database and unique test-only key aliases. They cover actual SQLite rollback/reopen/conflict handling for both groups, encrypted import/edit/clear/reopen, AAD/tamper failures, missing-key behavior and plaintext checks on database/sidecar files. Compile these with `:shared:data:datasources-core:assembleAndroidDeviceTest`; execute only against a confirmed physical phone. Never use production files or aliases as fixtures.

Executed on 2026-11-17: Android assembleDebug, isolated Android device-test APK compilation, and all five host-test tasks passed offline: 5 use-case + 41 repository + 9 datasource-core + 13 ViewModel + 35 UI = **103 tests, zero failures/errors**. `git diff --check` passed. Initial device-test compilation exposed an unsupported RoomDatabase.use call; explicit try/finally close fixed it and the final build passed. Nonfatal expect/actual beta and native-symbol stripping warnings remain.

The three device tests were **not executed**. The user explicitly deferred phone checks and authorized continuing local implementation; no phone was contacted during the expansion. ADB listed no connected phone; mDNS advertised the known Samsung endpoint but connection was refused. No APK was installed over real data, no emulator was used, and no iOS compile/run was performed. Device test compilation is distinct from execution. Room runtime/encryption on a phone and iOS remain verification gates.

## Build environment note

Initial dependency resolution reported a missing KSP plugin. A minimal HTTPS probe using Gradle's Azul 21 JDK established the actual failure: PKIX certificate-chain validation. The same URL returned HTTP 200 using Windows trusted roots. Download/build was retried with command-scoped `-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NUL`. No certificate validation was disabled and no global JDK/Gradle settings were modified. After dependencies are cached, normal offline commands remain available.

## Next

Implement educational seed/search/favorites next. Physical tests are deferred by user: when resumed, run the isolated tests on the physical phone before installing the migration build over real data, then verify preserved real data without destructive edits. Legacy source cleanup and complete erasure remain a release gate; Supabase publishing/sync and AdMob remain later phases. AI and clinical cloud synchronization remain deferred.

Latest full verification log: build/room-collections-verification.log (ignored build artifact). The expansion initially built successfully; a subsequent import-order cleanup introduced duplicate imports, caught by compileAndroidMain and corrected. The final full command passed. No application-data or device mutation occurred.


## Validación física — 2026-11-17

El usuario volvió a conectar el teléfono y autorizó retomar la revisión. ADB detectó Samsung SM-A546E, Android 16 / API 36, por Wi-Fi. El arranque inicial del daemon falló dentro del sandbox; el mismo ADB funcionó fuera del sandbox. No fue un fallo de la aplicación.

Instalado únicamente el APK aislado com.jecsdev.gestiora.data_datasources_core.test; ejecutado AndroidJUnitRunner: **OK (5 tests)**, sin fallos. Evidencia: build/room-phone-tests.log (ignorado). Se ejecutaron los dos tests educativos y los tres clínicos: seed/búsqueda/idiomas/favoritos/reapertura, rollback/reintento, ambos grupos clínicos y cifrado/Keystore/tamper/key perdida con fixtures. Sus archivos y aliases de prueba se limpian en finally; no usan archivos ni claves de producción.

La app principal ya estaba instalada y abierta en Inicio, sin formulario activo; no se instaló ni se borró/reimportó su paquete en esta revisión. Smoke test de UI real, inglés/dark theme: Mi bebé → biblioteca, búsqueda ultrasound (resultados por título/etiquetas), marcar ecografía como favorita, filtro Saved, expandir/leer cuerpo/cita WHO/idioma/fechas/disclaimer; force-stop y arranque frío; volver a biblioteca/Saved y confirmar favorito persistido; quitar ese favorito temporal, confirmar Saved vacío, Android Back y regreso a Inicio. No se modificaron registros clínicos, tema ni locale. Se confirmó Your pregnancy/Home al terminar.

Pendiente: revisión visual de la biblioteca ES/FR, fuentes externas/fallback de enlace, texto grande/TalkBack, reduced motion y rotación; iOS build/runtime. No se desconectó la red (ADB usa Wi-Fi): las pruebas locales y el código sin red respaldan la consulta local, pero no se declara un smoke test de UI en modo avión. El smoke test confirma que Home abre tras reinicio, no una auditoría de todos los registros reales. No emulador, commit ni push.


## Verified legacy cleanup — 2026-11-17

The previous retention-window paragraph is historical. The user authorized source cleanup. finishMigration imports both groups, authenticates and validates every existing Room payload under the storage mutex, and passes the verified store identities to the Android cleanup adapter. Before deleting anything, that adapter requires every extant legacy collection to have a verified destination (profile is the only intentionally row-deletable store). It then deletes only the nine named legacy AtomicFiles, checks base/bak/new absence, and removes their old individual Keystore aliases. The Room key is never removed. This is idempotent, and durable import markers survive cleanup interruption and profile clear. It does not implement reset-all, export, recovery, downgrade or iOS Keychain cleanup.

Build plus three new shared cleanup tests pass (14 datasource-core tests total). Six isolated Samsung tests pass, including actual legacy encrypted files/keys, Room re-read and cleanup, guard refusal before deletion with no verified destinations, preservation of unrelated files and of the active Room key, and no profile resurrection. Main-package install/read-back/UI evidence is recorded in MIGRATION_HANDOFF.md separately.
