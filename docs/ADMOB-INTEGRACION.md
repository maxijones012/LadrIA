# AdMob LadrIA — preparación inicial

IDs públicos extraídos de la pantalla de confirmación de AdMob:

- Aplicación: `ca-app-pub-3368082820065806~7527535526`
- Anuncio recompensado: `ca-app-pub-3368082820065806/3913839553`
- Anuncio recompensado de prueba Google: `ca-app-pub-3940256099942544/5224354917`.

Dependencias agregadas (según documentación Google octubre de 2026): `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0`, `com.google.android.ump:user-messaging-platform:4.0.0`. Manifest incluye ID de aplicación. **No se ejecuta inicialización, solicitud ni visualización publicitaria en esta versión:** faltan paso de consentimiento UMP, Auth Supabase, sesión verificada, configuración SSV y pruebas reales.

El backend vive en repositorio privado `maxijones012/LadrIA-Backend`, y su endpoint configurado de SSV será:
`https://lsckeoimavnsixdbvgxf.supabase.co/functions/v1/admob-ssv`

Premio del usuario previsto: 4 créditos por anuncio SSV verificado. **La captura no confirma cantidad ni nombre del artículo en AdMob**. Revisarlos y configurarlos en Supabase antes de habilitar anuncios; no probar producción con el bloque verdadero por riesgo de incumplimiento.

Para habilitar:
1. Configurar consentimiento con UMP antes de cualquier solicitud de anuncios.
2. Integrar Auth Supabase con cuenta recuperable/vinculable; el backend no debe confiar en ID del cliente.
3. Crear sesión de recompensa remota, usar `custom_data=session_id` y `user_id=auth.user.id` en opciones SSV del anuncio.
4. Configurar la SSV en la consola AdMob para el bloque recompensado y secretos de Edge Function en Supabase.
5. Probar con unidad oficial de test; **los anuncios de prueba no acreditan créditos monetizables de producción**.
6. Sólo después de las verificaciones publicar una beta firmada, evitando cambiar la clave del APK entre actualizaciones.
