# Anexo: catálogo educativo, sincronización y monetización

Fecha: 2026-11-17. Requisitos aportados por el usuario, con ajustes de diseño documentados abajo. Estado: plan; no hay Room, Supabase, WorkManager ni AdMob integrados por este anexo. No se ha configurado infraestructura remota.

Avance posterior del mismo día: los nueve stores clínicos Android ya tienen bindings Room cifrados e importación en dos grupos; la validación física fue pospuesta por el usuario; ver ROOM_PERSISTENCE.md. La biblioteca local con seed ES/EN/FR, búsqueda y favoritos también está implementada; ver EDUCATIONAL_CATALOG.md. La sincronización, publicación remota y monetización siguen pendientes.

## Alcance y orden

1. Integrar Room incrementalmente, conservando la protección de los registros médicos y una importación transaccional/idempotente de los nueve stores existentes. Room por sí solo no sustituye el cifrado actual.
2. Llevar el catálogo educativo ES/EN/FR a almacenamiento local, sembrado desde el contenido incluido en la aplicación. Añadir búsqueda y guardados offline.
3. Incorporar publicación editorial en Supabase y sincronización del catálogo público. No requiere login de la usuaria ni sube su perfil, historial, favoritos o consultas de búsqueda.
4. Monetización después de validar contenido, privacidad y comportamiento de lectura. La IA, autenticación y sincronización clínica siguen fuera de esta fase.

Se mantienen los límites domain/data/presentation y Koin existentes. No se añaden módulos o dependencias hasta implementar la vertical slice correspondiente.

## Modelo propuesto

Separar contenido remoto de estado local para que ninguna actualización borre favoritos:

- `educational_content`: clave estable `(content_id, locale)`; título, cuerpo de texto, etiquetas, versión editorial, `updated_at`, `reviewed_at` opcional, `source_checked_at`, estado de publicación y retirada. Locales soportados: es/en/fr. Sin revisión documentada, no mostrar una aprobación médica.
- `educational_content_source`: una o más referencias por ficha/traducción; `source_name`, título publicado, `source_url`, idioma y fecha consultada. Los campos singulares del anexo son insuficientes para fichas con varias fuentes.
- `educational_content_user_state`: clave `content_id`, `is_favorite` y fecha local de marcado. Nunca forma parte del DTO remoto ni de su sobrescritura. Mantenerlo independiente de la eliminación física del contenido evita perder el estado por cascadas.
- `educational_sync_state`: cursor opaco del servidor, versión de protocolo y fecha local del último éxito. Las fechas UTC deben tener unidad explícita; usar epoch-milisegundos si se representan como Long.

`updated_at` significa modificación técnica/editorial; `reviewed_at` significa revisión documentada del contenido; `source_checked_at` significa consulta de la fuente. No usar una fecha en sustitución de otra. La UI muestra etiquetas distintas y la fecha exacta pertinente. Las traducciones son adaptaciones de Gestiora, no traducciones avaladas por las instituciones.

## Contrato de sincronización

La propuesta inicial `updated_at > last_sync_at` no basta: empates, cambios durante paginación, eliminaciones y transacciones concurrentes pueden perder datos. Tampoco se usará el reloj del teléfono como cursor.

Antes de implementar el cliente, definir y probar un endpoint/RPC con estas garantías:

- Cursor generado por el servidor y paginación determinista sobre un conjunto estable. Un snapshot/versionado publicado o un feed con orden de publicación confirmado son opciones; un contador secuencial o `(updated_at,id)` sin garantía de visibilidad de commits no resuelve por sí solo las carreras.
- Incluye tombstones para retirada/eliminación y un mecanismo de snapshot completo cuando un cursor expire. La publicación/revisión puede realizarse en Supabase; una Edge Function solo se añade si el contrato la necesita.
- Descarga a staging y valida todas las páginas: esquema, IDs, locales, límites de tamaño, fuentes HTTPS y referencias. En una transacción Room aplicar el lote completo y avanzar el cursor. Si falla red, validación o escritura, conservar catálogo y cursor previos. Reintentar es idempotente.
- Usar upsert/actualizaciones explícitas de tablas remotas. Evitar `REPLACE` indiscriminado y sus efectos de borrado/reinserción sobre relaciones; nunca escribir el estado de favoritos desde el remoto.
- Conservar un catálogo incluido en el binario para primera apertura offline. Una sincronización fallida no bloquea la lectura ni oculta su antigüedad. Una ficha retirada deja de recomendarse/buscarse; si estaba guardada, mostrar que fue retirada y evitar presentarla como vigente.
- Sincronización al abrir con antigüedad mínima, exclusión mutua y retry acotado. Android puede añadir WorkManager con restricción de red y trabajo único; la ejecución no es inmediata ni garantizada. iOS necesita su propio mecanismo; no trasladar WorkManager al código común. La lectura nunca espera al worker.

## Supabase y privacidad

Catálogo publicado de lectura pública mediante publishable key y RLS/grants que solo expongan contenido publicado. Borradores, revisiones y escrituras restringidos al flujo editorial autenticado. Ninguna secret/service-role key en el APK o framework iOS. Probar lectura anónima permitida y acceso a borradores/escritura denegado.

No enviar semana de embarazo, identificadores clínicos, favoritos ni términos de búsqueda para seleccionar contenido o publicidad. La búsqueda se ejecuta localmente. El acceso al servidor y a enlaces externos puede generar metadatos de red; describirlo con precisión en privacidad. No confundir catálogo público remoto con sincronización de datos médicos.

## UI y búsqueda

Mantener Calma editorial, arte actual, ES/EN/FR y soporte de movimiento reducido. Fuentes y disclaimer accesibles desde onboarding y lectura, además de los requisitos aplicables a la ficha de Play. No esconder orientación urgente en carruseles o detrás de anuncios.

Primera búsqueda: consulta local parametrizada sobre título, etiquetas y cuerpo, con tratamiento explícito de comodines y comportamiento definido de mayúsculas/acentos. Elegir LIKE o FTS tras verificar compatibilidad de Room KMP y necesidades reales; probar es/en/fr. Los favoritos tienen vista dedicada y sobreviven a cambios de idioma/contenido. Definir fallback de traducción con idioma visible; nunca mezclar cuerpo y referencias de versiones distintas.

## Monetización posterior

Solo banners adaptativos al pie de lectura o nativos identificados en listados, sin cubrir contenido/controles ni provocar saltos de layout. Sin intersticiales ni anuncios de apertura. Evitar anuncios en orientación urgente y formularios clínicos.

Configurar bloqueos de categorías/URLs disponibles para medicamentos, suplementos, productos prenatales y servicios médicos; verificar creatividades reales. AdMob ofrece categorías y cobertura por idioma limitadas, no una garantía de exclusión semántica perfecta. Si no se puede cumplir la restricción editorial, no servir anuncios en esas superficies.

Antes de activar SDK: revisar consentimiento y tratamiento de datos según distribución, inventariar tráfico real y completar Privacy Policy/Data safety/Health apps declaration de forma coherente. Los registros clínicos pueden permanecer locales mientras un SDK transmite otros datos; evitar la promesa absoluta de que toda la app carece de transferencias.

## Auditoría y aceptación

Revisión editorial trimestral como máximo, con responsable y registro de fuentes/cambios; mensual para contenido sensible o cambiante y revisión extraordinaria ante nuevas guías. Cadencia propuesta, pendiente de asignar responsable; no equivale a validación clínica ya realizada.

Pruebas antes de declarar esta fase completada: lectura en primera apertura offline; actualización multilingüe; favoritos preservados; empates y cambios concurrentes; varias páginas; retirada; cursor expirado; fallo a mitad de descarga; rollback; reinicio/reintento sin duplicados; payload inválido y límites; RLS; búsqueda sin red; UI con texto grande/lector de pantalla. Verificar importación médica y cifrado por separado. No se han ejecutado estas pruebas porque aún no existe la implementación.

## Referencias técnicas y de políticas

Consultadas el 2026-11-17; volver a comprobar antes del lanzamiento:

- Supabase RLS: https://supabase.com/docs/guides/database/postgres/row-level-security
- Supabase keys: https://supabase.com/docs/guides/getting-started/api-keys
- Room DAO/upsert: https://developer.android.com/training/data-storage/room/accessing-data
- AdMob blocking controls: https://support.google.com/admob/answer/3150235
- Google Play Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Google Play sensitive information permissions: https://support.google.com/googleplay/android-developer/answer/16558241


## V1 scope decision — 2026-11-17

Supabase/CMS/catalog synchronization, login and AI are deferred explicitly by the user to phase 2. V1 continues using the bundled offline catalog; corrections require an app update. See LOCAL_CONTENT_POLICY.md for ownership/review/versioning/release process and its limitations. The design above remains a future contract, not an active backend or a v1 closure requirement.
