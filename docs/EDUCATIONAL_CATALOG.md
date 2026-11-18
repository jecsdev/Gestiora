# Catálogo educativo local

2026-11-17. Implementado: biblioteca desde Mi bebé, búsqueda offline y guardados. Android compila y los host tests pasan. Los cinco tests instrumentados pasaron en Samsung y el smoke test de biblioteca en inglés/dark theme pasó; ver validación física al final. iOS tiene builder y binding, pero no se compiló ni ejecutó en Windows. Supabase, anuncios y nuevas respuestas médicas no forman parte de esta slice.

## Experiencia

- Mi bebé conserva su ilustración, semana explorada, scroll y FAQ. El botón de biblioteca abre una vista independiente dentro de esa sección; Volver y Android Back regresan al explorador.
- Todos / Guardados, búsqueda con limpieza, estados de carga, error con reintento y ausencia de resultados. Las fichas se expanden con fade y expansión finita; movimiento reducido usa duración cero.
- Cada lectura muestra idioma, actualización editorial, consulta de fuentes, enlaces originales y disclaimer. Revisión documentada solo aparece si existe; el seed no declara revisión médica. El enlace fallido expone la URL copiable.
- Guardar cambia el estado después del éxito de escritura. El fallo se informa y permite repetir la acción. No se guardan términos de búsqueda; query/filtros permanecen únicamente en RAM durante la vista/configuración.

## Contenido y recursos

Se reutilizan las siete preguntas existentes de OMS/UNICEF/NHS y sus traducciones ES/EN/FR, sin duplicar su texto en Kotlin ni presentar nuevas afirmaciones médicas. EducationalSeed es un adapter de recursos: resuelve la traducción activa en modelos domain y prepara ese idioma mediante ViewModel/use case/repository. El catálogo se materializa al abrirlo, sin red, también en la primera apertura de cada idioma. IDs estables permiten compartir favoritos entre traducciones.

La key catalog_locale identifica el idioma realmente resuelto por Compose, no el idioma introducido en el perfil clínico. El fallback para idiomas no soportados sigue los recursos base en inglés y se identifica en cada lectura. No hay fallback por artículo remoto: todas las fichas incluidas tienen los tres idiomas.

Seed v1, updated_at epoch-ms 1794787200000 (2026-11-16 UTC), source_checked_on 2026-11-16, reviewed_on null. Las fechas conservan la versión/consulta existentes: no se atribuye una nueva revisión a esta implementación. Una actualización del seed requiere aumentar versión y updated_at; no modificar silenciosamente los textos con los mismos metadatos. Las fechas de consulta/revisión son fechas civiles ISO, mientras updated_at utiliza epoch-ms.

Actualmente Home y las FAQ por semana siguen leyendo sus recursos incluidos. Una futura publicación remota deberá conectarlas al repository si también deben reflejar correcciones remotas; esta slice solo conecta la biblioteca.

## Persistencia y límites

- EducationalRepository / BrowseEducation / EducationViewModel respetan los módulos actuales. EducationalStore usa DTO de data y RoomEducationalStore adapta el DAO. No se añadieron dependencias ni módulos.
- Base separada gestiora_education.db, schema v1 exportado: educational_content, educational_content_source, educational_content_user_state y educational_seed. No modifica el schema clínico ni toca archivos/keys de migración.
- Fuentes múltiples por traducción, materializadas junto con el contenido y marker de seed en una transacción. Upsert de contenido, sustitución de fuentes de esa ficha y avance de versión ocurren juntos. No hay REPLACE ni cascada hacia favoritos.
- Favorite usa content_id estable, independiente del locale. Preparar contenido ignora cualquier isFavorite recibido. Un seed antiguo o repetido no sustituye una revisión más nueva ni duplica fuentes. La futura sincronización necesita aún su contrato, staging, retiradas y cursor; no está implementada.
- Búsqueda parametrizada con instr sobre título, cuerpo y etiquetas normalizados. Es una subcadena literal, no búsqueda aproximada, tokens AND, LIKE ni FTS. Mayúsculas, acentos latinos habituales ES/FR, marcas combinantes, ñ/ç y ligaduras œ/æ se normalizan; espacios se compactan y comilla curva se unifica. %, _, comillas y barra inversa no actúan como comodines ni SQL. Query limitada a 256 caracteres. Escaneo suficiente para las siete fichas; revisar índices/FTS cuando crezca el catálogo.
- El repository valida IDs únicos, límites, idioma, fechas civiles y referencias HTTPS a who.int / unicef.org / nhs.uk antes de escribir. Añadir instituciones exige ampliar deliberadamente la lista. No es un cliente remoto ni una validación clínica.
- Esta base contiene texto editorial público y preferencias de lectura (IDs favoritos), sin cifrado de blobs. Android la coloca en noBackupFilesDir. El builder iOS usa Library; la política de backup de esa base/preferencias necesita revisión antes de distribución. Las políticas de registros clínicos siguen siendo las documentadas en ROOM_PERSISTENCE.md.

## Evidencia

Ejecutados: assembleDebug, los cinco host-test tasks y assembleAndroidDeviceTest con --offline --no-configuration-cache --no-watch-fs. Resultado: BUILD SUCCESSFUL; 5 use-case + 11 datasource-core + 45 repository + 13 ViewModel + 36 UI = **110 tests, cero fallos/errores**. Log ignorado: build/education-verification.log. git diff --check pasó.

Los siete nuevos host tests cubren normalización/búsqueda literal, DTO/favoritos, validación previa y propagación de I/O, política de seed/actualización y traducciones/citas reales leídas de los XML incluidos. Los fakes no prueban SQLite.

Dos nuevas pruebas instrumentadas, compiladas y posteriormente ejecutadas con éxito en el Samsung, usan archivos exclusivos del paquete de test: búsqueda SQL con acentos/puntuación, idiomas y favoritos, actualización/reapertura; y fallo forzado con trigger en mitad del seed, rollback y reintento. Se suman a las tres pruebas clínicas pendientes. Inicialmente el usuario pospuso teléfono; posteriormente se ejecutaron los cinco tests con éxito, como documenta la validación física al final. No se abrió emulador ni se instaló nada sobre sus datos. Pendientes: ejecución SQLite real, UI/Back/teclado/texto grande/lector de pantalla y compilación/ejecución iOS. El build no equivale a validación visual.

Referencia técnica consultada: [Room KMP, builders y constructor generado](https://developer.android.com/kotlin/multiplatform/room). El builder iOS sigue ese mecanismo; su validación real continúa pendiente.

## Continuación

Cuando el usuario retome pruebas físicas, ejecutar los cinco tests aislados antes de instalar sobre datos reales y revisar la biblioteca en ES/EN/FR. Luego definir/publicar el contrato editorial de Supabase descrito en EDUCATIONAL_CONTENT_SYNC_PLAN.md; no subir favoritos, búsquedas ni registros clínicos. No declarar completadas sincronización, políticas de publicación/Play ni monetización.


## Validación física — 2026-11-17

El usuario volvió a conectar el teléfono y autorizó retomar la revisión. ADB detectó Samsung SM-A546E, Android 16 / API 36, por Wi-Fi. El arranque inicial del daemon falló dentro del sandbox; el mismo ADB funcionó fuera del sandbox. No fue un fallo de la aplicación.

Instalado únicamente el APK aislado com.jecsdev.gestiora.data_datasources_core.test; ejecutado AndroidJUnitRunner: **OK (5 tests)**, sin fallos. Evidencia: build/room-phone-tests.log (ignorado). Se ejecutaron los dos tests educativos y los tres clínicos: seed/búsqueda/idiomas/favoritos/reapertura, rollback/reintento, ambos grupos clínicos y cifrado/Keystore/tamper/key perdida con fixtures. Sus archivos y aliases de prueba se limpian en finally; no usan archivos ni claves de producción.

La app principal ya estaba instalada y abierta en Inicio, sin formulario activo; no se instaló ni se borró/reimportó su paquete en esta revisión. Smoke test de UI real, inglés/dark theme: Mi bebé → biblioteca, búsqueda ultrasound (resultados por título/etiquetas), marcar ecografía como favorita, filtro Saved, expandir/leer cuerpo/cita WHO/idioma/fechas/disclaimer; force-stop y arranque frío; volver a biblioteca/Saved y confirmar favorito persistido; quitar ese favorito temporal, confirmar Saved vacío, Android Back y regreso a Inicio. No se modificaron registros clínicos, tema ni locale. Se confirmó Your pregnancy/Home al terminar.

Pendiente: revisión visual de la biblioteca ES/FR, fuentes externas/fallback de enlace, texto grande/TalkBack, reduced motion y rotación; iOS build/runtime. No se desconectó la red (ADB usa Wi-Fi): las pruebas locales y el código sin red respaldan la consulta local, pero no se declara un smoke test de UI en modo avión. El smoke test confirma que Home abre tras reinicio, no una auditoría de todos los registros reales. No emulador, commit ni push.
