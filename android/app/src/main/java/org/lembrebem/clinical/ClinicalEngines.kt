package org.lembrebem.clinical

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class MlKitDocumentExtractionEngine(private val context: Context) : DocumentExtractionEngine {
    override suspend fun extract(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        val recognizer = if (context.resources.configuration.locales[0].language == "ja")
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        else TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image).addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result.text)
                recognizer.close()
            }.addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
                recognizer.close()
            }
        } catch (error: Exception) {
            recognizer.close()
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }
}

/** UI-scoped recorder: never starts on receiver, background service, or automatic callback. */
class ConsentRecorder(private val context: Context, private val storage: ClinicalStorage) {
    init {
        context.cacheDir.listFiles()?.filter { it.name.startsWith("consultation-") || it.name.startsWith("playback-") }?.forEach { it.delete() }
    }
    private var recorder: MediaRecorder? = null
    private var temporary: File? = null
    val isRecording get() = recorder != null
    fun start(consented: Boolean) {
        check(consented && recorder == null)
        val file = File.createTempFile("consultation-", ".m4a", context.cacheDir)
        temporary = file
        val active = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            active.setAudioSource(MediaRecorder.AudioSource.MIC)
            active.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            active.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            active.setOutputFile(file.absolutePath)
            active.prepare()
            active.start()
            recorder = active
        } catch (error: Exception) {
            active.release(); file.delete(); temporary = null; throw error
        }
    }
    fun stop(): String? {
        val active = recorder ?: return null
        recorder = null
        try {
            active.stop()
            val file = temporary ?: return null
            val id = "audio-${UUID.randomUUID()}.m4a.enc"
            storage.save(id, file.readBytes())
            return id
        } finally {
            active.release(); temporary?.delete(); temporary = null
        }
    }
    fun discard() {
        recorder?.let { runCatching { it.stop() }; it.release() }; recorder = null
        temporary?.delete(); temporary = null
    }
}
