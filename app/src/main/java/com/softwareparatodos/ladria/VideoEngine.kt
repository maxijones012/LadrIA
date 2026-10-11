package com.softwareparatodos.ladria

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

/** Results are evidence hints, never translations or behavioral diagnoses. */
data class VideoReport(
    val framesChecked: Int,
    val dogFrames: Int,
    val topDogScore: Float,
    val audioAvailable: Boolean,
    val sounds: List<SoundFinding>,
    val moments: List<SoundMoment>,
    val audioSecondsInspected: Int,
    val visualSecondsInspected: Int,
    val audioError: String? = null,
    val visualError: String? = null
)

object VideoEngine {
    fun analyze(context: Context, uri: Uri, sensitivity: DetectionSensitivity = DetectionSensitivity.NORMAL): VideoReport {
        var framesChecked = 0
        var dogFrames = 0
        var dogMax = 0f
        var visualSecondsInspected = 0
        var visualError: String? = null
        var audioError: String? = null
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            val inspectedMs = kotlin.math.min(durationMs, 30_000L)
            visualSecondsInspected = kotlin.math.ceil(inspectedMs / 1000.0).toInt()
            val positions = if (inspectedMs > 0) {
                (1..12).map { inspectedMs * it / 13L * 1000L }
            } else listOf(0L)
            positions.forEach { position ->
                val frame = retriever.getFrameAtTime(position, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    try {
                        val labels = Tasks.await(labeler.process(InputImage.fromBitmap(frame, 0)))
                        framesChecked++
                        val best = labels.filter {
                            it.text.contains("dog", true) ||
                                it.text.contains("puppy", true) ||
                                it.text.contains("canine", true)
                        }.maxOfOrNull { it.confidence } ?: 0f
                        if (best >= 0.50f) dogFrames++
                        if (best > dogMax) dogMax = best
                    } finally {
                        frame.recycle()
                    }
                }
            }
        } catch (error: Exception) {
            visualError = error.message ?: "No se pudieron inspeccionar cuadros del video"
        } finally {
            retriever.release()
            labeler.close()
        }

        val decoded = try {
            VideoAudioDecoder.decode(context, uri)
        } catch (error: Exception) {
            audioError = error.message ?: "No se pudo decodificar el audio"
            null
        }
        val findings = mutableListOf<SoundFinding>()
        val moments = mutableListOf<SoundMoment>()
        var audioSecondsInspected = 0
        if (decoded != null) {
            try {
                SoundClassifier(context).use { classifier ->
                    val tensor = classifier.makeTensor()
                    VideoAudioDecoder.windows(decoded).forEachIndexed { second, clip ->
                        tensor.load(clip)
                        val detected = CanineSounds.canineOnly(classifier.classify(tensor), sensitivity.minScore)
                        audioSecondsInspected++
                        findings += detected
                        // Guardar solamente la señal principal de cada ventana de un segundo.
                        detected.firstOrNull()?.let { moments.add(SoundMoment(second, it)) }
                    }
                }
            } catch (error: Exception) {
                audioError = error.message ?: "Falló el modelo de audio"
            }
        }
        val selected = findings.groupBy { it.label }
            .map { (label, values) -> SoundFinding(label, values.maxOf { it.score }) }
            .sortedByDescending { it.score }
        return VideoReport(
            framesChecked = framesChecked,
            dogFrames = dogFrames,
            topDogScore = dogMax,
            audioAvailable = decoded != null && audioError == null,
            sounds = selected,
            moments = moments,
            audioSecondsInspected = audioSecondsInspected,
            visualSecondsInspected = visualSecondsInspected,
            audioError = audioError,
            visualError = visualError
        )
    }
}
