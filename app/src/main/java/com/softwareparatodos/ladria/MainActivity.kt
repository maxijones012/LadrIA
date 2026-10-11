package com.softwareparatodos.ladria

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.util.Locale
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val Bg = Color(0xFF070E19)
private val Panel = Color(0xFF111E2E)
private val Cyan = Color(0xFF24D6E2)
private val Soft = Color(0xFFAABFCE)
private val Violet = Color(0xFF8E72FF)

class MainActivity : ComponentActivity() {
    private var engine: LiveMicEngine? = null
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private var isActive by mutableStateOf(false)
    private var isBusy by mutableStateOf(false)
    private var micFindings by mutableStateOf<List<SoundFinding>>(emptyList())
    private var micHistory by mutableStateOf<List<SoundMoment>>(emptyList())
    private var startedListeningMs: Long = 0L
    private var sensitivity by mutableStateOf(DetectionSensitivity.NORMAL)
    private var videoReport by mutableStateOf<VideoReport?>(null)
    private var message by mutableStateOf("Grabá o importá un video. El análisis se realiza en tu teléfono.")
    private var lastDogName by mutableStateOf("")
    private var savedDogs by mutableStateOf<List<String>>(emptyList())
    private var observationHistory by mutableStateOf<List<SavedObservation>>(emptyList())
    private var contextChoice by mutableStateOf("Sin contexto")
    private var postureChoice by mutableStateOf("No observada")
    private var translationTone by mutableStateOf(TranslationTone.TIERNO)
    private var pendingVideo: android.net.Uri? = null
    private var pendingCameraFile: File? = null
    private var updateMessage by mutableStateOf("Releases oficiales desde GitHub. La instalación requiere tu autorización.")
    private var availableUpdate: AppUpdate? by mutableStateOf(null)
    private var checkingUpdate by mutableStateOf(false)
    private var adTestStatus by mutableStateOf("Podés probar un video de Google. No se acreditan créditos.")
    private var adTestBusy by mutableStateOf(false)
    private var rewardedTestController: RewardedTestController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyzeCaptured = { uri -> processVideo(uri) }
        savedDogs = LocalNotebook.dogs(this)
        observationHistory = LocalNotebook.records(this)
        rewardedTestController = RewardedTestController(
            activity = this,
            updateStatus = { adTestStatus = it },
            updateBusy = { adTestBusy = it }
        ).also { it.refreshConsentAtLaunch() }
        setContent {
            LadriaScreen(
                active = isActive,
                busy = isBusy,
                audio = micFindings,
                history = micHistory,
                sensitivity = sensitivity,
                onSensitivityChange = { selected ->
                    if (isActive) stopMic()
                    sensitivity = selected
                    message = "Sensibilidad actualizada. Tocá Comenzar escucha para aplicarla."
                },
                report = videoReport,
                message = message,
                dogName = lastDogName,
                onDogName = { lastDogName = it },
                savedDogs = savedDogs,
                onSaveDog = { savedDogs = LocalNotebook.saveDog(this, lastDogName) },
                onPickDog = { lastDogName = it },
                onDeleteDog = { dog -> savedDogs = LocalNotebook.deleteDog(this, dog); if (lastDogName == dog) lastDogName = "" },
                contextChoice = contextChoice,
                postureChoice = postureChoice,
                translationTone = translationTone,
                onTranslationTone = { translationTone = it },
                onContextChoice = { contextChoice = it },
                onPostureChoice = { postureChoice = it },
                observationHistory = observationHistory,
                onClearHistory = { observationHistory = LocalNotebook.clear(this) },
                onShareObservation = { entry ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, LocalNotebook.shareText(entry))
                    }
                    startActivity(Intent.createChooser(intent, "Compartir registro LadrIA"))
                },
                onToggleMic = { if (isActive) stopMic() else requestAndStartMic() },
                onRecordVideo = { recordWithCamera() },
                onSelectedVideo = { uri -> if (uri != null) processVideo(uri) },
                adTestStatus = adTestStatus,
                adTestBusy = adTestBusy,
                onTestRewarded = { rewardedTestController?.watchTestAd() },
                checkingUpdate = checkingUpdate,
                updateMessage = updateMessage,
                updateAvailable = availableUpdate != null,
                onCheckUpdate = { checkUpdates() },
                onOpenUpdate = {
                    val url = availableUpdate?.releaseUrl ?: "https://github.com/maxijones012/LadrIA/releases"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )
        }
    }

    private val captureVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        val captured = pendingVideo
        pendingVideo = null
        if (success && captured != null) analyzeCaptured(captured)
        else {
            pendingCameraFile?.delete()
            pendingCameraFile = null
            message = "La grabación de video fue cancelada."
        }
    }

    private var analyzeCaptured: (android.net.Uri) -> Unit = {}

    private fun recordWithCamera() {
        if (isBusy || isActive) return
        try {
            val destination = File.createTempFile("ladria_", ".mp4", cacheDir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", destination)
            pendingVideo = uri
            pendingCameraFile = destination
            captureVideo.launch(uri)
        } catch (error: Exception) {
            message = "No se pudo abrir la cámara: ${error.message}"
        }
    }

    private fun processVideo(uri: android.net.Uri) {
        if (isBusy) return
        stopMic()
        isBusy = true
        videoReport = null
        message = "Analizando cuadros y audio. Esto puede demorar unos segundos..."
        val selectedName = lastDogName
        val selectedContext = contextChoice
        val selectedPosture = postureChoice
        val selectedTone = translationTone
        worker.execute {
            try {
                val report = VideoEngine.analyze(this, uri, sensitivity)
                val records = LocalNotebook.add(this, selectedName, selectedContext, selectedPosture, report, selectedTone)
                runOnUiThread {
                    observationHistory = records
                    videoReport = report
                    message = "Análisis finalizado. Interpretá los resultados junto al contexto."
                    isBusy = false
                }
            } catch (error: Exception) {
                runOnUiThread {
                    message = "No se pudo analizar el archivo: ${error.message ?: "formato incompatible"}"
                    isBusy = false
                }
            } finally {
                // Solo se borra el archivo temporal de cámara creado por LadrIA.
                // Nunca borrar el video importado por la persona.
                pendingCameraFile?.delete()
                pendingCameraFile = null
            }
        }
    }

    private fun checkUpdates() {
        if (checkingUpdate) return
        checkingUpdate = true
        updateMessage = "Consultando GitHub Releases..."
        worker.execute {
            try {
                val result = UpdateChecker.check(this)
                runOnUiThread {
                    availableUpdate = result
                    updateMessage = if (result == null) "No hay una versión pública más nueva disponible."
                        else "¡Disponible la versión ${result.version}! Podés abrir la publicación oficial e instalarla con autorización de Android."
                    checkingUpdate = false
                }
            } catch (_: Exception) {
                runOnUiThread {
                    updateMessage = "No se pudo comprobar la versión. Revisá tu conexión e intentá nuevamente."
                    checkingUpdate = false
                }
            }
        }
    }

    private fun requestAndStartMic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED) startMic()
        else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { allowed ->
        if (allowed) startMic() else message = "Se necesita permiso de micrófono para escuchar en vivo."
    }

    private fun startMic() {
        if (isBusy || isActive) return
        isActive = true
        micFindings = emptyList()
        micHistory = emptyList()
        startedListeningMs = android.os.SystemClock.elapsedRealtime()
        message = "Escuchando... Mostramos eventos acústicos, no emociones verificadas."
        engine = LiveMicEngine(
            this,
            sensitivity = sensitivity,
            onResult = { result -> runOnUiThread {
                if (isActive) {
                    micFindings = result
                    val strongest = result.firstOrNull()
                    if (strongest != null) {
                        val second = ((android.os.SystemClock.elapsedRealtime() - startedListeningMs) / 1000L).toInt()
                        val latest = micHistory.lastOrNull()
                        if (latest == null || latest.finding.label != strongest.label || second - latest.second >= 3) {
                            micHistory = (micHistory + SoundMoment(second, strongest)).takeLast(18)
                        }
                    }
                }
            } },
            onError = { error -> runOnUiThread { message = error; stopMic() } }
        ).also { it.start() }
    }

    private fun stopMic() {
        engine?.stop()
        engine = null
        isActive = false
    }

    override fun onStop() {
        stopMic() // No grabar audio en segundo plano.
        super.onStop()
    }

    override fun onDestroy() {
        stopMic()
        rewardedTestController?.close()
        worker.shutdownNow()
        super.onDestroy()
    }
}

@Composable
private fun LadriaScreen(
    active: Boolean,
    busy: Boolean,
    audio: List<SoundFinding>,
    history: List<SoundMoment>,
    sensitivity: DetectionSensitivity,
    onSensitivityChange: (DetectionSensitivity) -> Unit,
    report: VideoReport?,
    message: String,
    dogName: String,
    onDogName: (String) -> Unit,
    savedDogs: List<String>,
    onSaveDog: () -> Unit,
    onPickDog: (String) -> Unit,
    onDeleteDog: (String) -> Unit,
    contextChoice: String,
    postureChoice: String,
    translationTone: TranslationTone,
    onTranslationTone: (TranslationTone) -> Unit,
    onContextChoice: (String) -> Unit,
    onPostureChoice: (String) -> Unit,
    observationHistory: List<SavedObservation>,
    onClearHistory: () -> Unit,
    onShareObservation: (SavedObservation) -> Unit,
    onToggleMic: () -> Unit,
    onRecordVideo: () -> Unit,
    onSelectedVideo: (android.net.Uri?) -> Unit,
    adTestStatus: String,
    adTestBusy: Boolean,
    onTestRewarded: () -> Unit,
    checkingUpdate: Boolean,
    updateMessage: String,
    updateAvailable: Boolean,
    onCheckUpdate: () -> Unit,
    onOpenUpdate: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        onSelectedVideo(it)
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var confirmDeleteHistory by remember { mutableStateOf(false) }
    if (confirmDeleteHistory) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDeleteHistory = false },
            title = { Text("¿Borrar historial?") },
            text = { Text("Se eliminarán todos los análisis guardados en este teléfono. No se puede deshacer.") },
            confirmButton = { Button(onClick = { onClearHistory(); confirmDeleteHistory = false }) { Text("Borrar") } },
            dismissButton = { OutlinedButton(onClick = { confirmDeleteHistory = false }) { Text("Cancelar") } }
        )
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel, onSurface = Color.White, onBackground = Color.White)) {
        Scaffold(containerColor = Bg) { insets ->
            Column(
                modifier = Modifier.fillMaxSize().padding(insets)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(17.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.background(Brush.linearGradient(listOf(Cyan.copy(alpha=0.30f), Violet.copy(alpha=0.28f))), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("🐾", fontSize = 27.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("LadrIA", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("INTELIGENCIA CANINA  •  BETA", color = Cyan, fontSize = 11.sp, letterSpacing = 0.8.sp)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF18365A), Color(0xFF2B2056), Color(0xFF111E2E))), RoundedCornerShape(24.dp))
                        .border(1.dp, Cyan.copy(alpha=0.23f), RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("ESCUCHÁ · OBSERVÁ · COMPRENDÉ", color = Cyan, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                        Text("Descubrí señales en los sonidos y videos de tu perro", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                        Text("IA local para detectar indicios. No traduce ladridos ni diagnostica emociones.", color = Color(0xFFD0D9E8), fontSize = 13.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("●  Procesamiento en tu dispositivo", color = Color(0xFF89E5DE), fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Analizar", "Historial", "Créditos", "Ajustes").forEachIndexed { idx, label ->
                        if (idx == selectedTab) Button(
                            onClick = { selectedTab = idx },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 1.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                        ) { Text(label, color = Bg, fontSize = 12.sp) }
                        else OutlinedButton(
                            onClick = { selectedTab = idx },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 1.dp),
                            border = BorderStroke(1.dp, Soft.copy(alpha=0.25f))
                        ) { Text(label, color = Soft, fontSize = 12.sp) }
                    }
                }
                when (selectedTab) {
                    0 -> {
                        SectionCard("PERFIL OPCIONAL") {
                            OutlinedTextField(
                                value = dogName,
                                onValueChange = onDogName,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Nombre del perro") },
                                singleLine = true,
                                placeholder = { Text("Ej.: Toby") }
                            )
                            Button(onClick=onSaveDog, enabled=dogName.isNotBlank(), modifier=Modifier.fillMaxWidth()) {
                                Text("Guardar perfil local", color=Bg)
                            }
                            savedDogs.take(8).forEach { dog ->
                                Row(modifier=Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                                    OutlinedButton(onClick={ onPickDog(dog) }, modifier=Modifier.weight(1f)) {
                                        Text(dog, color=Cyan)
                                    }
                                    OutlinedButton(onClick={ onDeleteDog(dog) }) { Text("Borrar", color=Soft) }
                                }
                            }
                        }
                        SectionCard("CONTEXTO · INFORMADO POR VOS") {
                            Text("La situación y la postura las indicás vos; no se detectan automáticamente.",color=Soft,fontSize=12.sp)
                            ObservationSelector("Situación", ObservationGuide.situations, contextChoice, onContextChoice)
                            ObservationSelector("Postura", ObservationGuide.postures, postureChoice, onPostureChoice)
                        }
                        SectionCard("🐶 SI PUDIERA HABLAR…") {
                            Text("Imaginamos una frase amable basada en el sonido. No es traducción literal.",color=Soft,fontSize=13.sp)
                            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                TranslationTone.entries.forEach { tone ->
                                    OutlinedButton(
                                        onClick={ onTranslationTone(tone) },
                                        modifier=Modifier.weight(1f),
                                        border=BorderStroke(1.dp, if(translationTone==tone) Cyan else Soft.copy(alpha=.25f)),
                                        contentPadding=androidx.compose.foundation.layout.PaddingValues(horizontal=3.dp,vertical=2.dp)
                                    ) {
                                        Text(tone.title,color=if(translationTone==tone) Cyan else Soft,fontSize=11.sp)
                                    }
                                }
                            }
                            Text("Elegí un tono, y escuchá o importá un video.",color=Soft,fontSize=12.sp)
                        }
                        SectionCard("01 · ESCUCHA EN VIVO") {
                            Text("Reconocimiento de eventos acústicos", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Ladridos, ladridos agudos, aullidos, gruñidos y gemidos. El detector puede equivocarse.", color = Soft, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetectionSensitivity.entries.forEach { option ->
                                    val selected = sensitivity == option
                                    if (selected) Button(
                                        onClick = { onSensitivityChange(option) },
                                        enabled = !active,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Cyan)
                                    ) {
                                        Text(if (option == DetectionSensitivity.NORMAL) "Normal" else "Sensible", color = Bg, fontSize = 12.sp)
                                    } else OutlinedButton(
                                        onClick = { onSensitivityChange(option) },
                                        enabled = !active,
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, Violet.copy(alpha = 0.65f))
                                    ) {
                                        Text(if (option == DetectionSensitivity.NORMAL) "Normal" else "Sensible", color = Soft, fontSize = 12.sp)
                                    }
                                }
                            }
                            Text(
                                if (sensitivity == DetectionSensitivity.SENSITIVE)
                                    "Modo sensible: encuentra sonidos más suaves, pero puede confundirse con ruido."
                                else "Modo normal: reduce algunas falsas alarmas. Si no detecta, probá Sensible.",
                                color = Soft, fontSize = 12.sp
                            )
                            Button(
                                onClick = onToggleMic,
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFFFF777E) else Cyan),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (active) "■ Detener escucha" else "🎙  Comenzar escucha", color = Bg) }
                            if (active) {
                                Text("● Micrófono activo", color = Cyan, fontSize = 12.sp)
                                SoundResults(audio, isListening = true)
                            }
                            if (active || history.isNotEmpty()) {
                                DogTalkCard(
                                    findings = if (audio.isNotEmpty()) audio
                                        else history.lastOrNull()?.let { listOf(it.finding) } ?: emptyList(),
                                    context = contextChoice,
                                    posture = postureChoice,
                                    tone = translationTone,
                                    dogName = dogName
                                )
                            }
                            if (history.isNotEmpty()) {
                                Text("SEÑALES RECIENTES", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                history.takeLast(6).asReversed().forEach { moment ->
                                    Text("${moment.second}s · ${moment.finding.label}", color = Soft, fontSize = 12.sp)
                                }
                            }
                        }
                        SectionCard("02 · ANÁLISIS DE VIDEO") {
                            Text("Audio + imagen", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Revisa hasta 12 cuadros y 30 segundos de audio por intervalos. No detecta postura ni emociones todavía.", color = Soft, fontSize = 13.sp)
                            OutlinedButton(
                                onClick = { picker.launch(arrayOf("video/*")) },
                                enabled = !busy && !active,
                                border = BorderStroke(1.dp, Cyan),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (busy) "Analizando..." else "🎞  Elegir video", color = Cyan) }
                            OutlinedButton(
                                onClick = onRecordVideo, enabled = !busy && !active,
                                border = BorderStroke(1.dp, Violet.copy(alpha=0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("📷  Grabar con la cámara", color = Color(0xFFBCAFFF)) }
                            if (report != null) {
                                DogTalkCard(report.sounds, contextChoice, postureChoice, translationTone, dogName)
                                VideoResults(report)
                                SectionCard("LECTURA ORIENTATIVA") {
                                    Text(
                                        ObservationGuide.build(
                                            report.dogFrames > 0,
                                            report.sounds.map { it.label },
                                            contextChoice,
                                            postureChoice
                                        ), color=Soft, fontSize=12.sp
                                    )
                                    Text("El contexto y la postura fueron informados manualmente.", color=Cyan, fontSize=11.sp)
                                }
                            }
                        }
                        SectionCard("ESTADO DEL ANÁLISIS") { Text(message, color = Soft, fontSize = 13.sp) }
                    }
                    1 -> {
                        SectionCard("CUADERNO LOCAL") {
                            Text("Se conservan hasta 60 registros de texto. No se almacenan los videos.",color=Soft,fontSize=13.sp)
                            if (observationHistory.isEmpty()) Text("Todavía no hay videos analizados.",color=Soft)
                            observationHistory.take(15).forEach { item ->
                                HorizontalDivider(color=Cyan.copy(alpha=.15f))
                                Text(item.dogName.ifBlank { "Perro sin nombre" } + " · " +
                                    java.text.SimpleDateFormat("dd/MM HH:mm", Locale("es","AR"))
                                        .format(java.util.Date(item.timestamp)), color=Cyan,fontSize=13.sp)
                                Text(item.text.take(350),color=Soft,fontSize=12.sp)
                                OutlinedButton(onClick={onShareObservation(item)}) { Text("Compartir texto",color=Cyan) }
                            }
                            if (observationHistory.isNotEmpty()) {
                                OutlinedButton(onClick={ confirmDeleteHistory=true }, modifier=Modifier.fillMaxWidth()) {
                                    Text("Borrar historial", color=Color(0xFFFFA1A1))
                                }
                            }
                        }
                    }
                    2 -> {
                        SectionCard("CRÉDITOS · PRÓXIMA ETAPA") {
                            Text("Anuncios recompensados, sin falsas recompensas", color=Color.White, fontSize=17.sp, fontWeight=FontWeight.SemiBold)
                            Text("Durante la beta inicial los análisis son gratuitos. No se muestra saldo ficticio ni anuncios de producción.", color=Soft, fontSize=13.sp)
                            HorizontalDivider(color=Cyan.copy(alpha=.15f))
                            Text("Ejemplo 80/20 futuro: audio 1 crédito · video 3 créditos · anuncio voluntario +${RewardSplit.previewUserCredits} créditos.", color=Cyan, fontSize=13.sp)
                            Text("De 5 unidades virtuales: ${RewardSplit.previewUserCredits} para usar en análisis (80%) y ${RewardSplit.previewReserveUnits} no emitida (20%). No es dinero ni reparto de ingresos de AdMob.", color=Soft, fontSize=12.sp)
                            Text("AdMob registrado para LadrIA. Publicidad de producción todavía desactivada; usaremos anuncios de prueba y confirmación del servidor antes de acreditar créditos.", color=Soft, fontSize=12.sp)
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = onTestRewarded,
                                enabled = !adTestBusy,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Violet)
                            ) {
                                Text(if (adTestBusy) "Preparando prueba..." else "▶ Ver anuncio de prueba (0 créditos)", color = Color.White)
                            }
                            Text(adTestStatus, color=Soft, fontSize=12.sp)
                        }
                    }
                    else -> {
                        SectionCard("ACTUALIZACIONES OFICIALES") {
                            Text("Consultá nuevas versiones sin salir de LadrIA", color = Color.White, fontSize=16.sp, fontWeight=FontWeight.SemiBold)
                            Text("La app revisa las publicaciones oficiales de GitHub. Android debe autorizar la instalación; las versiones públicas necesitan la misma firma.", color = Soft, fontSize=13.sp)
                            Button(onClick=onCheckUpdate, enabled=!checkingUpdate, modifier=Modifier.fillMaxWidth()) {
                                Text(if (checkingUpdate) "Consultando..." else "↻ Buscar actualizaciones", color=Bg)
                            }
                            Text(updateMessage, color=Soft, fontSize=12.sp)
                            if (updateAvailable) OutlinedButton(onClick=onOpenUpdate, modifier=Modifier.fillMaxWidth()) {
                                Text("Abrir versión oficial", color=Cyan)
                            }
                        }
                        SectionCard("ALMACENAMIENTO PRIVADO") {
                            Text("Los perfiles y registros permanecen en este teléfono. Desde Historial podés compartirlos o borrarlos.",color=Soft,fontSize=13.sp)
                            Text("LadrIA no copia los archivos originales a su historial.",color=Soft,fontSize=13.sp)
                        }
                        SectionCard("PRIVACIDAD Y CÓDIGO ABIERTO") {
                            Text("Los audios y videos se procesan en el celular; la búsqueda de actualizaciones usa internet. La monetización futura utilizará red para anuncios y créditos.", color=Soft, fontSize=13.sp)
                            Text("github.com/maxijones012/LadrIA", color=Cyan, fontSize=13.sp)
                            Text("Modelo experimental. Sin análisis veterinario ni traducciones de lenguaje.", color=Soft, fontSize=12.sp)
                        }
                    }
                }
                Text(
                    "LadrIA muestra señales acústicas y visuales, no interpreta pensamientos ni reemplaza la evaluación veterinaria o etológica.",
                    color = Soft, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                )
            }
        }
    }
}


@Composable
private fun ObservationSelector(
    label: String,
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Text(label, color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Medium)
    androidx.compose.foundation.layout.FlowRow(
        modifier=Modifier.fillMaxWidth(),
        horizontalArrangement=Arrangement.spacedBy(6.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ) {
        values.forEach { value ->
            OutlinedButton(
                onClick={onSelect(value)},
                border=BorderStroke(1.dp, if(selected == value) Cyan else Soft.copy(alpha=.25f)),
                contentPadding=androidx.compose.foundation.layout.PaddingValues(horizontal=10.dp,vertical=2.dp)
            ) {
                Text(value, color=if(selected == value) Cyan else Soft, fontSize=12.sp)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, body: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            body()
        }
    }
}

@Composable
private fun DogTalkCard(
    findings: List<SoundFinding>,
    context: String,
    posture: String,
    tone: TranslationTone,
    dogName: String
) {
    val candidate = DogTalk.imagine(findings, context, posture, tone)
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFF153B48),Color(0xFF232546))),
                RoundedCornerShape(19.dp)
            )
            .border(1.dp, Violet.copy(alpha=.55f), RoundedCornerShape(19.dp))
            .padding(17.dp)
    ) {
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("🐾 SI PUDIERA HABLAR...",color=Cyan,fontSize=11.sp,fontWeight=FontWeight.Bold)
            if (candidate == null) {
                Text("Todavía no escuché una vocalización clara como para imaginar una frase.",
                    color=Color.White,fontSize=14.sp)
                Text("Probá el modo Sensible si los sonidos son suaves.", color=Soft,fontSize=12.sp)
            } else {
                val heading = dogName.trim().take(40).ifBlank { "Tu perro" }
                Text(heading + " podría decir:",color=Soft,fontSize=12.sp)
                Text("“" + candidate.phrase + "”",color=Color.White,fontWeight=FontWeight.SemiBold,
                    fontSize=21.sp,lineHeight=27.sp)
                Text(candidate.evidence,color=Color(0xFFABD5E7),fontSize=12.sp)
                Text(candidate.caution,color=Soft,fontSize=11.sp)
            }
            Text("Interpretación creativa y orientativa · No traduce literalmente ladridos.",
                color=Cyan.copy(alpha=.9f),fontSize=11.sp)
        }
    }
}

@Composable
private fun SoundResults(findings: List<SoundFinding>, isListening: Boolean = false) {
    if (findings.isEmpty()) {
        Text(
            if (isListening) "Todavía no se detectó un sonido canino distinguible."
            else "No se detectaron sonidos caninos con el umbral de esta versión.",
            color = Soft, fontSize = 13.sp
        )
    } else {
        findings.take(5).forEach { sound ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sound.label, color = Color.White, fontSize = 14.sp)
                Text("${String.format(Locale.US, "%.0f", sound.score * 100)}% señal", color = Cyan, fontSize = 12.sp)
            }
        }
        Text("Puntajes del detector de sonidos; no son certeza de emoción o intención.", color = Soft, fontSize = 11.sp)
    }
}

@Composable
private fun VideoResults(report: VideoReport) {
    HorizontalDivider(color = Cyan.copy(alpha = 0.20f))
    Text("RESULTADO VISUAL", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    if (report.visualError != null) {
        Text("Análisis visual no disponible: ${report.visualError}", color = Soft, fontSize = 12.sp)
    } else {
        Text(
            "Perro reconocido en ${report.dogFrames} de ${report.framesChecked} cuadros examinados.",
            color = Color.White, fontSize = 14.sp
        )
        Text("Se revisó un tramo de hasta ${report.visualSecondsInspected}s. No se infieren posturas ni emociones.", color = Soft, fontSize = 12.sp)
    }
    HorizontalDivider(color = Cyan.copy(alpha = 0.20f))
    Text("RESULTADO ACÚSTICO", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    if (!report.audioAvailable) {
        Text("Audio sin procesar: ${report.audioError ?: "sin pista decodificable"}", color = Soft, fontSize = 12.sp)
    } else {
        SoundResults(report.sounds)
        Text("Se procesaron ${report.audioSecondsInspected} segundos de audio.", color = Soft, fontSize = 12.sp)
        if (report.moments.isNotEmpty()) {
            Text("MOMENTOS DESTACADOS", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            report.moments.take(12).forEach { moment ->
                Text("${moment.second}s · ${moment.finding.label} · señal ${String.format(Locale.US, "%.0f", moment.finding.score * 100)}",
                    color = Color.White, fontSize = 12.sp)
            }
            if (report.moments.size > 12) Text("Y ${report.moments.size - 12} intervalos más...", color = Soft, fontSize = 12.sp)
            val barkTimes = report.moments.filter { it.finding.label.startsWith("Ladrido") }.map { it.second }
            if (barkTimes.size >= 3 && barkTimes.last() - barkTimes.first() >= 3) {
                Text("Patrón observado: ladridos detectados en varios segundos del tramo.", color = Cyan, fontSize = 12.sp)
            }
        }
    }
    HorizontalDivider(color = Cyan.copy(alpha = 0.20f))
    Text("LECTURA CONJUNTA", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text(
        if (report.dogFrames > 0 && report.sounds.isNotEmpty())
            "Hay señales de perro en imágenes y en audio. No podemos asegurar que sean del mismo animal ni establecer su intención."
        else
            "Las señales disponibles no alcanzan para relacionar con seguridad imagen y vocalizaciones.",
        color = Soft, fontSize = 12.sp
    )
}
