package com.softwareparatodos.ladria

/**
 * Previsualización estática de la economía, sin conexión a AdMob y sin autorizar saldos.
 * En producción la fuente de verdad será el backend con SSV validada y política versionada.
 *
 * El 20% NO son ingresos monetarios ni puntos entregados al desarrollador.
 */
internal object RewardSplit {
    const val userPercent: Int = 80
    const val reservePercent: Int = 20
    const val previewTotalUnits: Int = 5

    val previewUserCredits: Int = previewTotalUnits * userPercent / 100
    val previewReserveUnits: Int = previewTotalUnits - previewUserCredits

    init {
        check(userPercent + reservePercent == 100)
        check(previewTotalUnits * userPercent % 100 == 0) {
            "La recompensa mostrada debe permitir distribución entera exacta"
        }
    }
}
