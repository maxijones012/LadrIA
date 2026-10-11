# 🐾 LadrIA — Inteligencia canina experimental (Android)

**Código abierto MIT** para analizar señales sonoras de perros y presencia de perros en video, sin prometer traducciones de ladridos ni diagnósticos. App Kotlin/Jetpack Compose para Android 8+.

Repositorio oficial: https://github.com/maxijones012/LadrIA

## Implementado en el código base (NO verificado en dispositivo aún)

- Interfaz oscura con acentos cian/violeta, pestañas Analizar / Créditos / Ajustes.
- Clasificación local de eventos sonoros con YAMNet (TensorFlow Lite).
- Micrófono activo solo cuando el usuario inicia escucha; se detiene al abandonar la pantalla.
- Selección y captura de video mediante Android, etiquetado de perro en hasta seis cuadros e inferencia acústica del tramo inicial de hasta 30 segundos.
- Búsqueda voluntaria de actualizaciones oficiales en GitHub Releases, con apertura de publicación cuando existe versión más nueva.
- Workflow **manual** de GitHub Actions con modos `test` (APK debug) y `release` (APK firmado con clave estable + publicación). Sin compilación automática por commits.
- Se informa cuando la evidencia es insuficiente. Nada se envía al servidor para su análisis.

## Proyectado, todavía NO operativo

- Recompensas por publicidad **AdMob GMA Next-Gen** y billetera de créditos (requieren cuenta, consentimiento UMP, backend y verificación SSV de cada recompensa). Por ahora, los análisis beta no cobran créditos ni muestran anuncios reales.
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
- El estado actual es **código de prototipo sin APK compilado ni pruebas en Android verificadas en esta sesión**. No hay cuenta AdMob, créditos productivos ni backend conectado.
- El score de YAMNet indica resultado de clasificación acústica, no seguridad, emoción, estado médico o probabilidad de una traducción.
