package com.softwareparatodos.ladria

import android.content.Context
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Audio event detection, NOT canine-emotion or intent recognition. */
data class SoundFinding(val label: String, val score: Float)

object CanineSounds {
    private val patterns = listOf(
        "bark" to "Ladridos",
        "bow-wow" to "Ladridos",
        "growl" to "Gruñidos",
        "howl" to "Aullidos",
        "whimper" to "Gemidos",
        "whine" to "Gemidos",
        "dog" to "Sonido de perro"
    )

    /** Scores are classifier outputs, not a probability of the animal's intention. */
    fun canineOnly(scores: List<SoundFinding>, minScore: Float = 0.20f): List<SoundFinding> =
        scores.mapNotNull { value ->
            val translated = patterns.firstOrNull { (token, _) ->
                value.label.contains(token, ignoreCase = true)
            }?.second
            if (translated != null && value.score >= minScore) {
                SoundFinding(translated, value.score)
            } else null
        }.groupBy { it.label }
            .map { (label, candidates) -> SoundFinding(label, candidates.maxOf { it.score }) }
            .sortedByDescending { it.score }
}

/** One shared classifier is never used simultaneously by two threads. */
class SoundClassifier(context: Context) : AutoCloseable {
    private val classifier = AudioClassifier.createFromFileAndOptions(
        context,
        "yamnet.tflite",
        AudioClassifier.AudioClassifierOptions.builder()
            .setMaxResults(12)
            .setScoreThreshold(0f)
            .build()
    )

    fun createRecorder(): AudioRecord = classifier.createAudioRecord()
    fun makeTensor(): TensorAudio = classifier.createInputTensorAudio()

    @Synchronized
    fun classify(tensor: TensorAudio): List<SoundFinding> =
        classifier.classify(tensor).flatMap { it.categories }
            .map { SoundFinding(it.label, it.score) }

    override fun close() = classifier.close()
}

class LiveMicEngine(
    private val context: Context,
    private val onResult: (List<SoundFinding>) -> Unit,
    private val onError: (String) -> Unit
) {
    private var worker: ExecutorService? = null
    private val running = AtomicBoolean(false)
    private var recording: AudioRecord? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        worker = Executors.newSingleThreadExecutor().also { executor ->
            executor.execute {
                try {
                    SoundClassifier(context).use { detector ->
                        val recorder = detector.createRecorder()
                        recording = recorder
                        try {
                            val tensor = detector.makeTensor()
                            recorder.startRecording()
                            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                                error("El micrófono no pudo iniciar")
                            }
                            while (running.get() && !Thread.currentThread().isInterrupted) {
                                Thread.sleep(650)
                                if (!running.get()) break
                                tensor.load(recorder)
                                onResult(CanineSounds.canineOnly(detector.classify(tensor)))
                            }
                        } finally {
                            try { recorder.stop() } catch (_: Exception) { }
                            recorder.release()
                            recording = null
                        }
                    }
                } catch (_: InterruptedException) {
                    // Normal shutdown.
                } catch (error: Exception) {
                    if (running.get()) onError(error.message ?: "No se pudo analizar el audio")
                } finally {
                    running.set(false)
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        worker?.shutdownNow()
        worker = null
    }
}

/**
 * Decode up to 30 seconds of compressed audio locally, returning mono float PCM.
 * Simplified MVP decoder supporting PCM 16-bit and PCM float codec outputs.
 * Unsupported codecs are reported as unavailable, never as no dog detected.
 */
object VideoAudioDecoder {
    data class Decoded(val samples: FloatArray, val sampleRate: Int)
    private const val MAX_SECONDS = 30

    fun decode(context: Context, uri: Uri): Decoded? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            val bufferInfo = MediaCodec.BufferInfo()
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = android.media.AudioFormat.ENCODING_PCM_16BIT
            // Fixed maximum of 30s at up to 192kHz: capped memory and no uploads.
            val output = FloatArray(MAX_SECONDS * 192_000)
            var used = 0
            var inputDone = false
            var outputDone = false
            var loops = 0

            while (!outputDone && used < min(output.size, MAX_SECONDS * sampleRate) && loops++ < 15_000) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val data = codec.getInputBuffer(inIndex) ?: error("Buffer de entrada vacío")
                        val count = extractor.readSampleData(data, 0)
                        if (count < 0 || extractor.sampleTime > MAX_SECONDS * 1_000_000L) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, count, max(0L, extractor.sampleTime), 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val out = codec.outputFormat
                        sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        encoding = if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            out.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        } else android.media.AudioFormat.ENCODING_PCM_16BIT
                        if (encoding != android.media.AudioFormat.ENCODING_PCM_16BIT &&
                            encoding != android.media.AudioFormat.ENCODING_PCM_FLOAT) {
                            error("Formato de audio PCM no soportado: $encoding")
                        }
                    }
                    in 0..Int.MAX_VALUE -> {
                        if (bufferInfo.size > 0) {
                            val data = codec.getOutputBuffer(outIndex) ?: error("Buffer de audio vacío")
                            data.order(ByteOrder.LITTLE_ENDIAN)
                            data.position(bufferInfo.offset)
                            data.limit(bufferInfo.offset + bufferInfo.size)
                            val bytesPerSample = if (encoding == android.media.AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                            val frames = bufferInfo.size / max(1, channels * bytesPerSample)
                            for (frame in 0 until frames) {
                                var mono = 0f
                                for (channel in 0 until channels) {
                                    mono += if (bytesPerSample == 4) data.float else data.short / 32768f
                                }
                                if (used < output.size && used < MAX_SECONDS * sampleRate) {
                                    output[used++] = mono / channels
                                }
                            }
                        }
                        outputDone = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outIndex, false)
                    }
                }
            }
            return if (used == 0) null else Decoded(output.copyOf(used), sampleRate)
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            codec?.release()
            extractor.release()
        }
    }

    /** Uniform linear interpolation to 16 kHz and exact 15600-sample model windows. */
    fun windows(data: Decoded): List<FloatArray> {
        if (data.sampleRate <= 0) return emptyList()
        val clips = mutableListOf<FloatArray>()
        val availableSeconds = ceil(data.samples.size.toDouble() / data.sampleRate).toInt()
        for (time in 0 until min(availableSeconds, 30)) {
            val samples = FloatArray(15_600)
            for (index in samples.indices) {
                val source = (time + index / 16_000.0) * data.sampleRate
                val lower = source.toInt().coerceIn(0, data.samples.lastIndex)
                val upper = min(lower + 1, data.samples.lastIndex)
                val fraction = (source - lower).toFloat().coerceIn(0f, 1f)
                samples[index] = data.samples[lower] * (1f - fraction) + data.samples[upper] * fraction
            }
            clips += samples
        }
        return clips
    }
}
