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
    val audioError: String? = null,
    val visualError: String? = null
)

object VideoEngine {
    fun analyze(context: Context, uri: Uri): VideoReport {
        var framesChecked = 0
        var dogFrames = 0
        var dogMax = 0f
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
            val positions = if (inspectedMs > 0) {
                (1..6).map { inspectedMs * it / 7L * 1000L }
            } else listOf(0L)
            positions.forEach { position ->
                val frame = retriever.getFrameAtTime(position, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
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
        if (decoded != null) {
            try {
                SoundClassifier(context).use { classifier ->
                    val tensor = classifier.makeTensor()
                    VideoAudioDecoder.windows(decoded).forEach { clip ->
                        tensor.load(clip)
                        findings += CanineSounds.canineOnly(classifier.classify(tensor))
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
            audioError = audioError,
            visualError = visualError
        )
    }
}
