# LadrIA — plan maestro, reglas y puertas de calidad

Actualizado: 2026-10-11. Estado de este documento: **plan aprobado para ejecución incremental**, no funcionalidades terminadas.

## Propósito y alcance

App Android de análisis experimental de vocalizaciones y señales visuales caninas. Debe informar **observaciones verificables, incertidumbre y contexto**, jamás afirmar que traduce frases, lee pensamientos, diagnostica dolor o agresividad ni garantiza seguridad. Idioma inicial español (Argentina). Proyecto abierto MIT para código propio, con avisos de terceros y modelos licenciados por separado.

## Arquitectura objetivo

- **Cliente Android (Kotlin/Compose)**: micrófono, selección/grabación de videos, detección local, resultados, permiso mínimo, visualización moderna y accesible, almacenamiento optativo y actualización voluntaria.
- **IA audio v1**: YAMNet TFLite detecta categorías de sonidos (perro/ladridos/etc.), no emociones. Eventos temporales, agregación por ventanas, umbrales ajustados en pruebas.
- **IA video v1**: ML Kit Image Labeling identifica presencia probable de perro en muestras de cuadros; NO postura ni correlación individuo-sonido. Para pose y gestos se necesita otro modelo canino validado.
- **Fusión posterior**: modelo propio audio+video+contexto con opción explícita «sin evidencia suficiente». Nunca inferir certeza a partir de un único puntaje acústico.
- **Backend opcional para la app oficial monetizada**: Supabase Auth anónimo/registrado + ledger de créditos + verificación server-to-server (SSV) de AdMob + políticas RLS + función de consumo transaccional. Nunca enviar audio/video por defecto; backend solamente identidad, operaciones y créditos.
- **Distribución**: GitHub Releases con APK release firmado siempre por la misma clave; comprobación de nuevas versiones desde Ajustes; instalación consentida por Android. El código abierto no incluye la clave privada ni credenciales.

## Contradicciones detectadas y decisiones

| Contradicción / riesgo | Resolución | Regla comprobable |
|---|---|---|
| «Entender lo que dice» vs modelo de sonidos | Mostrar eventos y contextos posibles, no traducciones ni diagnósticos | Siempre hay advertencia y «no concluyente» |
| Analizar «posturas» vs modelo de etiquetas de cuadros | No mostrar postura hasta integrar modelo específico validado | Nada de emociones a partir del etiquetado genérico |
| «Offline y privado» vs AdMob, créditos y actualizaciones | Análisis multimedia **local**; red solo para anuncios, saldo/identidad y actualización, con aviso claro | No enviar medios al backend; modo limitado sin red |
| «Open source» vs créditos seguros | Copias/forks pueden modificar el cliente; solo el **servicio oficial** puede llevar créditos verificados | Backend autoritativo; nunca saldo seguro en SharedPreferences |
| «Gratis» vs cobrar todos los escaneos con anuncios | Base gratuita diaria; análisis avanzado adicional por créditos optativos | Cancelar anuncio nunca bloquea la funcionalidad básica gratuita |
| «Anuncio visto» vs saldo real | Recompensa confirmada por servidor con SSV firmado; callback local es UX, no autorización económica | No acreditar por cierre/apertura ni por callback sin verificación |
| «Actualizar desde la app» vs restricciones Android | Buscar actualización adentro y abrir APK/release para instalación con consentimiento | No instalar silenciosamente; no cambiar package/clave |
| «Se instala al principio» vs «problema al actualizar» | Una única firma release permanente y versionCode creciente | APK previo con otra clave exige reinstalación o migración |
| «Open source comercial» vs Barkopedia CC BY-NC-SA | No incorporar datos NC a producto monetizado sin licencia comercial expresa | Inventario de modelos y datasets obligatorio |
| Video entero vs escanear seis cuadros y 30 s de audio | UI debe decir cuánto se inspeccionó; progresivamente cubrir intervalos configurables | No llamar «video completo» a muestreo parcial |
| Publicar APK rápido vs gasto GitHub Actions | Manual workflow_dispatch; un build por hito; cachés de Gradle | No crear builds automáticos por cada commit |

## Etapas y criterios de aceptación

0. **Auditoría y congelamiento**: inspección de código, dependencias, modelo, licencias, alcance; registrar riesgos actuales. Entregable: este documento y backlog. **Sin APK**.
1. **Fundación Android**: identidad visual, gradle reproducible, permisos, errores claros, accesibilidad y minSdk. Evitar bloqueos/crashes. **Gate:** `assembleDebug` en CI manual + test en dispositivo.
2. **Diseño premium**: sistema de colores índigo/cian/violeta, gradientes moderados, identidad de perro/onda, navegación Analizar/Créditos/Ajustes, animaciones accesibles y estados vacíos. **Gate:** pantallas pequeñas, modo oscuro, texto grande.
3. **Audio fiable**: registro temporal, silencios, muestreo consistente, no ejecución inadvertida en background; detectar ladrido/aullido/gruñido cuando el modelo lo reconozca. **Gate:** pruebas con sonidos de perros y no perros.
4. **Video fiable**: video local, análisis temporal explícito, vista previa, manejo de códecs, frames sin perro, reporte separado de audio/video. **Gate:** videos de varios formatos y duraciones.
5. **Modelo canino especializado**: inventario legal de Barkopedia/Dog2vec y evaluación independiente. **Gate:** ninguna emoción sin dataset licenciado y validación con perros no vistos.
6. **Postura y gestos**: detector/keypoints caninos (orejas/cola/cuerpo) con licencia compatible y rango de incertidumbre. **Gate:** benchmark con videos propios etiquetados, no confundir razas.
7. **Fusión contextual**: sonido + postura + escena y consulta opcional al usuario. **Gate:** mejora real contra modelo audio solo, registro de falsos positivos y abstención.
8. **Privacidad y almacenamiento**: historial optativo local cifrable, borrado, exportación, no subir videos, sin grabación en segundo plano. **Gate:** auditoría de permisos y flujo de datos.
9. **Sistema de créditos**: definir ledger servidor, costos configurables, reserva/confirmación/reversión idempotentes, cupo gratuito y saldos. **Gate:** transacciones concurrentes y pruebas de fraude.
10. **AdMob recompensado (GMA Next-Gen)**: consentimiento UMP, anuncios exclusivamente opt-in, anuncios de prueba en QA, integración de SSV y reconexiones. **Gate:** solo acredita SSV verificada; si falla el anuncio no pierde créditos.
11. **Actualización interna**: consultar GitHub Releases vía HTTPS, comparar versiones, descargar o abrir publicación oficial, mostrar cambios; validar APK por firma Android. **Gate:** instalación encima de la anterior conservando datos.
12. **Firma y lanzamiento controlado**: generar keystore offline una vez, agregar GitHub Secrets, `assembleRelease` firmado y `apksigner verify`, publicar tag + APK + SHA-256. **Gate:** misma clave verificada entre dos releases, `versionCode` siempre mayor.
13. **Resiliencia**: errores de red, permisos denegados, poca memoria, cancelación, rotación, proceso terminado, anuncios no disponibles. **Gate:** sin pérdidas de saldo ni cierres evitables.
14. **Evaluación científica**: dataset con consentimiento y diversidad de razas/ambientes, split por perro, métricas precision/recall/F1 macro y calibración. **Gate:** reporte reproducible por modelo, umbral documentado; «sin conclusión» cuando no cumple.
15. **Beta pública**: pruebas en diversos Android, feedback voluntario, políticas de privacidad, licencias, avisos, issues y guía de contribución. **Gate:** publicación solo tras pruebas de firma, privacidad, anuncios y mediciones.
16. **Lanzamientos continuos**: changelog, compatibilidad de datos, rollback seguro del backend, migraciones, versiones regulares y monitoreo optativo sin multimedia. **Gate:** sin regresiones en actualización/saldo.

## Economía propuesta (parámetros a validar, NO implementados)

- 3 créditos gratuitos por día de calendario del servidor, para el cliente oficial, sujeto a control antiabuso.
- Audio rápido: 1 crédito; video de hasta 30 segundos: 3 créditos. **Nada se descuenta si el análisis falla o se cancela antes de comenzar**.
- Video recompensado voluntario: **4 créditos de uso** en el ejemplo inicial de 5 unidades virtuales de recompensa (80% usuario, 20% reserva no emitida del proyecto), acreditados únicamente tras SSV verificada. Limitar tasa, abuso y anuncios por usuario. No equivale a distribuir dinero ni garantiza una rentabilidad del 20%.
- Sin transferencias, conversiones a dinero o redención fuera de la app. Sin compras por ahora.
- Para quienes no quieran anuncios: mantener al menos funciones básicas gratuitas de prueba; nunca ad blocking obligatorio.
- Los valores son un punto de partida de producto, no una obligación contractual; quedan remotos/configurables mediante servidor.


## Regla 80/20 de recompensas virtuales (nueva decisión)

- El 80/20 se aplica **exclusivamente a unidades virtuales de recompensa presupuestadas por la app**, NO a la facturación real de AdMob. El titular de la cuenta AdMob recibe las liquidaciones publicitarias conforme a su contrato y debe afrontar gastos e impuestos.
- Configuración de ejemplo: bolsa de **5 unidades virtuales** por anuncio verificado; **4 créditos** (80%) acreditados a la persona para escaneos; **1 unidad** (20%) **no se emite** y queda como reserva de diseño del programa. No constituye una moneda, una transferencia al dueño, una ganancia contable ni un saldo que alguien pueda retirar.
- Todo crédito otorgado es de **uso exclusivo dentro de LadrIA**, no transferible, sin valor de rescate y jamás convertible en dinero, tarjetas de regalo, criptoactivos o bienes externos. Está prohibido anunciar «cobrás el 80%» por ver publicidad.
- Los valores concretos deben definirse en **configuración firmada/servida por backend** (porcentaje usuario 80, reserva 20; total unidades 5), versionada y auditada. La app solo muestra una vista preliminar hasta que exista backend verificado.
- Para una bolsa que no permita partición entera, no hacer cálculos en el cliente ni redondear de manera oculta. Usar paquetes preaprobados en el servidor con cantidades enteras, y comunicar antes del anuncio cuántos créditos exactos se reciben.
- Si una persona completa un anuncio recompensado, se registra la transacción SSV válida una sola vez, se acreditan los créditos prometidos y se deja una traza de la política aplicada; no se usa la cifra de ingresos de AdMob como una contraparte monetaria de esos créditos.
- La reserva de 20% **no se acredita a una cuenta personal en LadrIA**, porque no representa dinero. Las métricas de ingresos reales son independientes de los puntos virtuales y se consultan en AdMob, nunca en la billetera de usuarios.
- Los análisis básicos siguen siendo gratuitos sin mirar anuncios en la fase beta. Nunca exigir clics en anuncios, ni recompensar por clics, ni modificar saldos desde el APK o SharedPreferences.
- Antes de producción: consentimiento UMP si corresponde, SSV validada criptográficamente, transacciones idempotentes, límites razonables, criterios antiabuso, transparencia del costo de cada análisis y pruebas de reembolso.

Detalle implementativo: [RECOMPENSAS-80-20.md](RECOMPENSAS-80-20.md).

## Seguridad del ledger

`reward_events` con UNIQUE por ID de transacción SSV, validación de firma y timestamps; `wallet_entries` inmutable para altas y bajas con causal; `analysis_jobs` con `idempotency_key` y estados `reserved/committed/refunded`; política RLS y RPC transaccionales. Eventos duplicados/reordenados no deben duplicar crédito. Token de servicio nunca en APK; `custom_data` incluye nonce vinculado a cuenta/sesión, **no datos de la mascota**. La callback local solo muestra «validando recompensa».

## Pruebas científicas

Separar identidad de perros en train/test (evita memorizar individuos). Medir por condición acústica, iluminación, raza, cámara y dispositivos; comparar YAMNet vs modelo especializado; registrar incertidumbre y falsos positivos. Las métricas objetivo se decidirán antes de entrenar, y ningún número publicitario se mostrará sin pruebas externas.

## Dependencias externas y términos

- AdMob Rewarded Ads: https://support.google.com/admob/answer/7313578 ; https://developers.google.com/admob/android/next-gen/rewarded
- UMP Consent: https://developers.google.com/admob/android/privacy
- Android actualización y firma: https://developer.android.com/google/play/app-updates
- YAMNet audio baseline: https://developers.google.com/codelabs/tflite-audio-classification-basic-android
- Barkopedia NC dataset: https://huggingface.co/datasets/ArlingtonCL2/BarkopediaDogEmotionClassification_Data

**No se afirma que ninguna etapa futura esté implementada por el hecho de figurar acá.**
