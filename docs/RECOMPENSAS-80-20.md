# LadrIA — política 80/20 de recompensas virtuales (propuesta)

**Estado al 11/10/2026: diseño documentado y previsualización UI, NO pagos, anuncios reales ni backend activo.**

## Qué significa y qué NO significa el 80/20

El objetivo es que, por una referencia de **5 unidades virtuales de bonificación**, se otorguen **4 créditos de uso (80%) a la persona** y se reserven **1 unidad (20%) sin emitir** como margen de diseño del programa. Los créditos habilitan funciones dentro de LadrIA. La reserva no se transfiere a una wallet del propietario y NO es dinero.

Los ingresos monetarios de los anuncios se liquidan por AdMob a la cuenta de su titular. **No existe un reparto automático de facturación 80/20 a usuarios.** No debe mostrarse esa afirmación en la app, en su tienda ni en campañas. Las políticas de AdMob prohíben recompensas monetarias directas (efectivo, cripto, gift cards).

Fuentes oficiales:
- Políticas: https://support.google.com/admob/answer/7313578?hl=es
- Validación SSV Next-Gen: https://developers.google.com/admob/android/next-gen/ssv

## Ejemplo de economía de prueba

| Evento | Coste / bonificación |
| --- | ---: |
| Escaneo de audio breve | -1 crédito |
| Escaneo de video de hasta 30 segundos | -3 créditos |
| Anuncio completado + SSV verificada | +4 créditos |
| Reserva virtual no emitida por anuncio | 1 unidad |
| Asignación diaria gratuita inicial | +3 créditos |

**Son parámetros de diseño, no valores ya cobrados ni promesas comerciales.** Mantener acceso básico gratuito y evitar monetizar una predicción canina como si fuera diagnóstico.

## Reglas de backend y antifraude

1. El backend define y versiona `policy_id`, `user_share_percent=80`, `reserved_percent=20`, `base_units=5`, `user_reward_credits=4`, `audio_cost=1`, `video_cost=3`; valores configurables y auditables. Exponer al cliente únicamente datos públicos de la política, no claves.
2. Antes del anuncio la UI explica claramente la bonificación exacta y exige aceptación voluntaria del anuncio.
3. El cliente recibe el callback local, pero únicamente el backend valida la **firma de AdMob SSV** y vincula el evento único `transaction_id` a usuario y sesión. El cliente no acredita nada por sí solo; muestra “Validando recompensa…” hasta confirmación.
4. Cada evento verificado acredita exactamente 4 créditos de uso, una sola vez, y registra la versión de la regla. Registrar contadores de unidad reservada solo para analítica virtual: nunca venderlos como ganancias reales.
5. La billetera se mantiene en un ledger transaccional inmutable con operaciones de consumo, reversión y expiración explícita solo si se informa; aplicar RLS, límites, rate limits y anti-replay.
6. Se debitan créditos solo al comenzar una operación válida o mediante reserva atómica; reembolsar una operación cancelada, fallida o sin resultado por un error de servicio. No penalizar rechazar el anuncio.
7. No se muestran equivalencias en ARS/USD; los créditos no pueden comprarse, transferirse, retirarse, canjearse fuera de LadrIA ni convertirse en dinero en esta versión.
8. Implementar una ruta de configuración autenticada para ajustar precios y recompensas sin actualizar APK, pero no cambiar retroactivamente el premio de un anuncio ya mostrado: atar cada evento a la política vigente al iniciar su visualización.
9. Para recompensas no divisibles en enteros, configurar paquetes de unidades que permitan el reparto exacto antes de anunciarlo; nunca confiar en decimales calculados por el cliente.
10. Si el proyecto se hace privado, la licencia ya concedida sobre copias distribuidas bajo MIT no se revoca por ese cambio. Las claves de firma, credenciales AdMob y secretos de backend jamás deben almacenarse en el repositorio.

## Pruebas imprescindibles para activar la economía

- SSV duplicada, vencida, alterada, firmada por clave desconocida o de unidad de anuncios incorrecta: nunca duplicar/acreditar una recompensa falsa.
- Anuncio cerrado prematuramente, fallos de red, callback diferido y reinicios de Android.
- Dos teléfonos con el mismo usuario, consumos simultáneos y reintentos: saldo jamás negativo o duplicado.
- Cambio de política 80/20 a otro valor desde backend sin recompensar diferente lo prometido antes de ver el anuncio.
- Confirmar con pruebas que la app puede ofrecer la función gratuita sin anuncios y que ninguna operación cruza datos de audio/video al backend por defecto.
