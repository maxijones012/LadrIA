# Etapa: anuncios recompensados de prueba — 2026-10-11

Se verificó por el usuario que **AdMob aceptó la URL de SSV**. Esto solo demuestra que Google pudo comunicarse con el webhook en la consola; todavía NO demuestra que Android acredite créditos.

## Implementación de esta etapa

- UMP: al abrir app se consulta la necesidad de consentimiento, y al solicitar el anuncio se muestra formulario si corresponde. Sin permiso para solicitar anuncios, no hay carga.
- SDK Next-Gen 1.5.0: se inicializa en hilo de fondo al necesitar la prueba (no al entrar a analizar perros).
- Solicitud `AdRequest` usa **solo** `AdMobConfig.testRewardedAdUnitId`; muestra un anuncio si carga, sin registrar recompensas locales.
- Cierre/carga fallida: texto claro y botón vuelve a habilitarse.
- El callback de recompensa local únicamente informa una confirmación de TEST. No se guarda ni acredita saldo.
- `AdMobConfig.productionEnabled = false` continúa inalterado.
- No habilitar producción hasta Auth anónimo/vinculable, sesiones ad_id, SSV google verificada de extremo a extremo, consentimientos y pruebas con APK firmado estable.

## Revisión antes del APK
1. Validar imports y firmas de clases GMA Next-Gen/UMP en build Android real.
2. Abrir la app y confirmar que la pantalla de escucha/video original sigue funcional.
3. Probar cancelación de formulario, falta de red, anuncio rechazado, botón repetido, rotación y cierre en segundo plano.
4. Confirmar que se usa unidad de prueba: `ca-app-pub-3940256099942544/5224354917`.
5. Confirmar que `ladria_wallet_entries` y `ladria_ad_events` NO cambian al ver anuncios de prueba.
6. Implementar posteriormente cliente Supabase Auth y wallet con reglas de recuperación y antifraude.

Docs oficiales:
- https://developers.google.com/admob/android/next-gen/quick-start
- https://developers.google.com/admob/android/next-gen/rewarded/single-load
- https://developers.google.com/admob/android/next-gen/privacy
