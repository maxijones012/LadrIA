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
    private var videoReport by mutableStateOf<VideoReport?>(null)
    private var message by mutableStateOf("Grabá o importá un video. El análisis se realiza en tu teléfono.")
    private var lastDogName by mutableStateOf("")
    private var pendingVideo: android.net.Uri? = null
    private var updateMessage by mutableStateOf("Releases oficiales desde GitHub. La instalación requiere tu autorización.")
    private var availableUpdate: AppUpdate? by mutableStateOf(null)
    private var checkingUpdate by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyzeCaptured = { uri -> processVideo(uri) }
        setContent {
            LadriaScreen(
                active = isActive,
                busy = isBusy,
                audio = micFindings,
                report = videoReport,
                message = message,
                dogName = lastDogName,
                onDogName = { lastDogName = it },
                onToggleMic = { if (isActive) stopMic() else requestAndStartMic() },
                onRecordVideo = { recordWithCamera() },
                onSelectedVideo = { uri -> if (uri != null) processVideo(uri) },
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
        else message = "La grabación de video fue cancelada."
    }

    private var analyzeCaptured: (android.net.Uri) -> Unit = {}

    private fun recordWithCamera() {
        if (isBusy || isActive) return
        try {
            val destination = File.createTempFile("ladria_", ".mp4", cacheDir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", destination)
            pendingVideo = uri
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
        worker.execute {
            try {
                val report = VideoEngine.analyze(this, uri)
                runOnUiThread {
                    videoReport = report
                    message = "Análisis finalizado. Interpretá los resultados junto al contexto."
                    isBusy = false
                }
            } catch (error: Exception) {
                runOnUiThread {
                    message = "No se pudo analizar el archivo: ${error.message ?: "formato incompatible"}"
                    isBusy = false
                }
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
        message = "Escuchando... Mostramos eventos acústicos, no emociones verificadas."
        engine = LiveMicEngine(
            this,
            onResult = { result -> runOnUiThread { if (isActive) micFindings = result } },
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
        worker.shutdownNow()
        super.onDestroy()
    }
}

@Composable
private fun LadriaScreen(
    active: Boolean,
    busy: Boolean,
    audio: List<SoundFinding>,
    report: VideoReport?,
    message: String,
    dogName: String,
    onDogName: (String) -> Unit,
    onToggleMic: () -> Unit,
    onRecordVideo: () -> Unit,
    onSelectedVideo: (android.net.Uri?) -> Unit,
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
                    listOf("Analizar", "Créditos", "Ajustes").forEachIndexed { idx, label ->
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
                        }
                        SectionCard("01 · ESCUCHA EN VIVO") {
                            Text("Reconocimiento de eventos acústicos", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Ladridos, aullidos, gruñidos y gemidos cuando el modelo logra identificarlos.", color = Soft, fontSize = 13.sp)
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
                        }
                        SectionCard("02 · ANÁLISIS DE VIDEO") {
                            Text("Audio + imagen", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Analiza seis cuadros del primer tramo y hasta 30 segundos de audio. No detecta postura en esta versión.", color = Soft, fontSize = 13.sp)
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
                            if (report != null) VideoResults(report)
                        }
                        SectionCard("ESTADO DEL ANÁLISIS") { Text(message, color = Soft, fontSize = 13.sp) }
                    }
                    1 -> {
                        SectionCard("CRÉDITOS · PRÓXIMA ETAPA") {
                            Text("Anuncios recompensados, sin falsas recompensas", color=Color.White, fontSize=17.sp, fontWeight=FontWeight.SemiBold)
                            Text("Durante la beta inicial los análisis son gratuitos. No se muestra saldo ficticio ni anuncios de producción.", color=Soft, fontSize=13.sp)
                            HorizontalDivider(color=Cyan.copy(alpha=.15f))
                            Text("Ejemplo 80/20 futuro: audio 1 crédito · video 3 créditos · anuncio voluntario +${RewardSplit.previewUserCredits} créditos.", color=Cyan, fontSize=13.sp)
                            Text("De 5 unidades virtuales: ${RewardSplit.previewUserCredits} para usar en análisis (80%) y ${RewardSplit.previewReserveUnits} no emitida (20%). No es dinero ni reparto de ingresos de AdMob.", color=Soft, fontSize=12.sp)
                            Text("Solo se acreditará una recompensa cuando la verifique el servidor de AdMob. Mantendremos un acceso básico sin anuncios.", color=Soft, fontSize=12.sp)
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
        Text("No se infiere postura ni emoción automáticamente en esta versión.", color = Soft, fontSize = 12.sp)
    }
    HorizontalDivider(color = Cyan.copy(alpha = 0.20f))
    Text("RESULTADO ACÚSTICO", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    if (!report.audioAvailable) {
        Text("Audio sin procesar: ${report.audioError ?: "sin pista decodificable"}", color = Soft, fontSize = 12.sp)
    } else SoundResults(report.sounds)
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
