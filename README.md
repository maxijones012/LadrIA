# 🐾 LadrIA — Inteligencia canina experimental (Android)

**Código abierto MIT** para analizar señales sonoras de perros y presencia de perros en video, sin prometer traducciones de ladridos ni diagnósticos. App Kotlin/Jetpack Compose para Android 8+.

Repositorio oficial: https://github.com/maxijones012/LadrIA

## Implementado en el código base (NO verificado en dispositivo aún)

- Interfaz oscura con acentos cian/violeta, pestañas Analizar / Créditos / Ajustes.
- Clasificación local de eventos sonoros con YAMNet (TensorFlow Lite). La v0.3 analiza todas las categorías y no solo las 12 primeras, añade Yip y evita Whimper humano.
- Micrófono activo solo cuando el usuario inicia escucha; se detiene al abandonar la pantalla. Sensibilidad Normal/Sensible y últimos eventos con segundos relativos.
- Selección y captura de video mediante Android, etiquetado de perro en hasta doce cuadros e inferencia acústica del tramo inicial de hasta 30 segundos, con eventos por segundo.
- Búsqueda voluntaria de actualizaciones oficiales en GitHub Releases, con apertura de publicación cuando existe versión más nueva.
- Workflow **manual** de GitHub Actions con modos `test` (APK debug) y `release` (APK firmado con clave estable + publicación). Sin compilación automática por commits.
- Se informa cuando la evidencia es insuficiente. Nada se envía al servidor para su análisis.

## Preparación de anuncios de prueba (código agregado, sin APK validado)

- `RewardedTestController.kt` pide información de privacidad a UMP, muestra su formulario cuando hace falta e inicializa GMA Next-Gen fuera del hilo principal.
- El botón en **Créditos** solicita exclusivamente la unidad de **prueba oficial de Google** y no acredita ningún saldo, ni siquiera si el anuncio informa recompensa.
- No se usa la unidad real de LadrIA; la validación SSV de AdMob está configurada en consola, pero el circuito de usuarios/SSV/billetera sigue pendiente.
- Pueden existir errores de compilación o diferencias en SDK que deban detectarse en el próximo único build manual; aún no se compiló esta modificación.


## v0.5.0 — «Si pudiera hablar…» (interpretación simpática)
- Frases amigables **creativas** a partir del tipo de sonido detectado y el contexto aportado por la persona; aparecen durante la escucha y junto al video.
- Estilos Tierno / Divertido / Tranquilo, con un mensaje de incertidumbre visible en pantalla.
- Sin sonido distinguible o con etiqueta genérica `Dog`: no inventar frases. Con gruñidos o gemidos, indicar precauciones apropiadas.
- No utiliza IA generativa remota: YAMNet clasifica sonidos y reglas locales construyen frases. Esto mantiene privacidad, uso offline y cero créditos.
- Los registros de video guardan la frase como texto etiquetado de ficción; se conservan las advertencias.
- Nuevas pruebas unitarias sobre ausencia de sonido, tono/contexto, gruñidos y gemidos.

## v0.4.0 — cuaderno y contexto
- Perfiles locales por nombre y selector.
- Contexto y observaciones corporales manuales, diferenciadas explícitamente de las señales que detecta YAMNet/ML Kit.
- Lectura conjunta descriptiva que evita diagnosticar emociones, incluso si se observa audio e imagen de perro.
- Historial local de hasta 60 análisis de video, opción de compartir como texto y borrar registros con confirmación.
- Eliminación de MP4 temporal grabado por la app tras analizar; videos importados no se borran.
- Pruebas unitarias obligatorias en el único build de CI.
- Las etapas 5, 6, 7 y 14 requieren datasets y modelos caninos especializados evaluados independientemente: **NO están científicamente completadas**. Ver [estado de etapas](docs/CIERRE-ETAPAS-0-16.md).

## v0.3.0 — compilación anterior

- Compilación confirmada por GitHub Actions: https://github.com/maxijones012/LadrIA/actions/runs/38168414959
- Detector consulta todas las clases YAMNet, incluye ladridos agudos, sensibilidad Normal/Sensible, momentos acústicos por segundo y 12 cuadros de video.
- APK debug generado como artefacto. **No es una release firmada con certificado estable**: es posible que no actualice sobre APK de prueba anterior; si Android indica firma incompatible, hacer copia de lo necesario y reinstalar.
- Los medios se analizan en el dispositivo; análisis canino contextual y posturas aún no implementados. Créditos productivos y anuncios reales desactivados.
- GitHub Actions vuelve a ejecutarse **solo a demanda** mediante workflow_dispatch.

## Proyectado, todavía NO operativo

- **Registro AdMob recibido**: App ID `ca-app-pub-3368082820065806~7527535526` y unidad recompensada `ca-app-pub-3368082820065806/3913839553`. Se agregaron metadatos Android y dependencias Next-Gen/UMP, **sin inicializar ni mostrar anuncios** hasta implementar consentimiento, identidad de usuario y SSV, y verificar el importe/item del premio. Recompensas por publicidad **AdMob GMA Next-Gen** y billetera de créditos (requieren cuenta, consentimiento UMP, backend y verificación SSV de cada recompensa). **Regla propuesta 80/20 de unidades virtuales**: de cada 5 unidades de presupuesto, 4 créditos para la persona y 1 no emitida como reserva del proyecto. **NO** es un reparto en dinero de los ingresos publicitarios. Por ahora, los análisis beta no cobran créditos ni muestran anuncios reales. Detalle: [`docs/RECOMPENSAS-80-20.md`](docs/RECOMPENSAS-80-20.md).
- Interpretación contextual, reconocimiento de postura corporal, entrenamiento propio multimodal, perfiles persistentes e historial.
- Instalación del APK sin salir del flujo de Android: la app busca releases; **Android solicita instalación** y exige misma firma para actualizar sin desinstalar.

## Para desarrolladores

- `scripts/prepare_model.py` descarga un modelo YAMNet con SHA-256 verificado al directorio de assets. El código no afirma precisión emocional.
- `app/src/main/java/com/softwareparatodos/ladria/` contiene pantalla, audio, video y consulta de actualizaciones.
- `.github/workflows/android.yml`: Actions > Compilar LadrIA APK (manual) > Run workflow. `test` genera APK debug **NO apto para actualizar entre CI runs**, porque la firma debug puede cambiar.
- `release` requiere antes configurar los cuatro secretos de firma. Ver [`docs/FIRMA-Y-ACTUALIZACIONES.md`](docs/FIRMA-Y-ACTUALIZACIONES.md).
- Plan completo, decisiones y etapas: [`docs/PLAN-DE-ETAPAS.md`](docs/PLAN-DE-ETAPAS.md).

Requisitos locales: JDK 17, Android SDK 35, Gradle 8.10.2. Ejecutar `python3 scripts/prepare_model.py` y `gradle :app:assembleDebug`.

## Publicación, privacidad y límites

- **El código es MIT**, pero los modelos y SDKs tienen licencias y condiciones propias. Antes de publicar una versión comercial, completar el inventario de dependencias y permisos de redistribución. Evitar el dataset emocional NC de Barkopedia salvo autorización.
- La búsqueda de versiones necesita `INTERNET`; la futura publicidad y backend también. Eso NO significa que subamos audio o video. No se graba en segundo plano.
- La v0.3.0 se compiló como APK de prueba; la v0.4.0 requiere compilación y pruebas en dispositivo. La cuenta AdMob está registrada y el backend tiene funciones iniciales, pero **no se muestran anuncios reales ni se han probado créditos productivos o el circuito completo**.
- El score de YAMNet indica resultado de clasificación acústica, no seguridad, emoción, estado médico o probabilidad de una traducción.
