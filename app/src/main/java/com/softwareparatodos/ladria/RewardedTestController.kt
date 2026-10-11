package com.softwareparatodos.ladria

import androidx.activity.ComponentActivity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem

/**
 * Integración EXCLUSIVA de prueba: anuncios oficiales de prueba de Google,
 * consentimiento UMP y cero escrituras de saldo.
 *
 * NO conectar a la unidad real sin sesión de Supabase y validación SSV end-to-end.
 * El callback local no es prueba de recompensa ni acredita créditos.
 */
internal class RewardedTestController(
    private val activity: ComponentActivity,
    private val updateStatus: (String) -> Unit,
    private val updateBusy: (Boolean) -> Unit
) {
    private val consent: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)
    private var consentRequested = false
    private var consentCompleted = false
    private var sdkReady = false
    private var initializingSdk = false
    private var busy = false
    private var destroyed = false

    private fun status(message: String) {
        activity.runOnUiThread { if (!destroyed) updateStatus(message) }
    }

    private fun setBusy(value: Boolean) {
        busy = value
        activity.runOnUiThread { if (!destroyed) updateBusy(value) }
    }

    fun refreshConsentAtLaunch() {
        if (consentRequested) return
        consentRequested = true
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                consentCompleted = true
                status("Privacidad comprobada. Podés probar un anuncio sin ganar créditos.")
            },
            { error ->
                consentCompleted = true
                status("No se pudo actualizar el consentimiento: ${error.message}. Intentá más tarde.")
            }
        )
    }

    /** Solo se llama desde un botón pulsado por el usuario. */
    fun watchTestAd() {
        if (busy || destroyed) return
        if (!consentCompleted) {
            status("Esperá un momento: estamos comprobando las opciones de privacidad.")
            return
        }
        setBusy(true)
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
            if (destroyed) return@loadAndShowConsentFormIfRequired
            if (error != null) {
                status("No se pudo mostrar el formulario de privacidad: ${error.message}")
                setBusy(false)
                return@loadAndShowConsentFormIfRequired
            }
            if (!consent.canRequestAds()) {
                status("No se puede solicitar publicidad con las opciones de privacidad actuales.")
                setBusy(false)
            } else {
                initializeAndLoad()
            }
        }
    }

    private fun initializeAndLoad() {
        if (sdkReady) {
            loadAndShow()
            return
        }
        if (initializingSdk) return
        initializingSdk = true
        status("Preparando anuncio de prueba...")
        // Google exige inicializar GMA Next-Gen fuera del hilo de la interfaz.
        Thread({
            try {
                MobileAds.initialize(
                    activity.applicationContext,
                    InitializationConfig.Builder(AdMobConfig.appId).build()
                ) {
                    activity.runOnUiThread {
                        initializingSdk = false
                        if (!destroyed) {
                            sdkReady = true
                            loadAndShow()
                        }
                    }
                }
            } catch (_: Exception) {
                initializingSdk = false
                status("No se pudo inicializar el servicio de anuncios.")
                setBusy(false)
            }
        }, "ladria-ads-init").start()
    }

    private fun loadAndShow() {
        if (destroyed) { setBusy(false); return }
        status("Cargando publicidad de prueba de Google...")
        // NUNCA solicitar unidad de producción en esta fase.
        val request = AdRequest.Builder(AdMobConfig.testRewardedAdUnitId).build()
        try {
            RewardedAd.load(request, object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    activity.runOnUiThread {
                        if (destroyed) { setBusy(false); return@runOnUiThread }
                        ad.adEventCallback = object : RewardedAdEventCallback {
                            override fun onAdDismissedFullScreenContent() {
                                status("Prueba terminada. No se otorgaron créditos.")
                                setBusy(false)
                            }
                            override fun onAdFailedToShowFullScreenContent(
                                fullScreenContentError: FullScreenContentError
                            ) {
                                status("No se pudo mostrar el video de prueba.")
                                setBusy(false)
                            }
                        }
                        try {
                            ad.show(activity, object : OnUserEarnedRewardListener {
                                override fun onUserEarnedReward(rewardItem: RewardItem) {
                                    status("Google confirmó el anuncio de prueba. Saldo sin cambios.")
                                    // Intencionalmente: NO guardar ni sumar créditos aquí.
                                }
                            })
                        } catch (_: Exception) {
                            status("Falló la reproducción del anuncio de prueba.")
                            setBusy(false)
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    status("Anuncio de prueba no disponible: ${adError.message}")
                    setBusy(false)
                }
            })
        } catch (_: Exception) {
            status("Error al solicitar el anuncio de prueba.")
            setBusy(false)
        }
    }

    fun close() {
        destroyed = true
        updateBusy(false)
    }
}
