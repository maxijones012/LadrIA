package com.softwareparatodos.ladria

/**
 * IDs públicos registrados en AdMob para LadrIA (11/10/2026).
 * Los IDs no son secretos. No poner contraseñas ni claves privadas en Android.
 *
 * Los análisis locales son gratuitos. El SDK de anuncios solo se utilizará
 * cuando se implemente UMP, sesión de usuario y SSV correctamente.
 */
internal object AdMobConfig {
    const val appId: String = "ca-app-pub-3368082820065806~7527535526"
    const val rewardedAdUnitId: String = "ca-app-pub-3368082820065806/3913839553"
    const val testRewardedAdUnitId: String = "ca-app-pub-3940256099942544/5224354917"
    const val rewardCredits: Int = 4

    // IMPORTANTE: No activar esto al publicar el APK sin revisar políticas y backend.
    const val productionEnabled: Boolean = false
}
