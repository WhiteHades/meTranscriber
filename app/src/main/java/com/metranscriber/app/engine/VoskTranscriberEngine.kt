package com.metranscriber.app.engine

import android.content.Context
import com.metranscriber.app.domain.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VoskTranscriberEngine(
    private val context: Context,
    private val sessionId: String
) : TranscriberEngine {

    override val engineId: String = "vosk_offline"
    override val displayName: String = "Vosk Offline (Acoustic)"
    override var isAvailable: Boolean = true
        private set
    override var isModelDownloaded: Boolean = false
        private set

    private var model: Model? = null
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun initialize() {
        if (model != null) return
        
        model = suspendCancellableCoroutine { continuation ->
            StorageService.unpack(context, "model", "model",
                { loadedModel ->
                    isModelDownloaded = true
                    continuation.resume(loadedModel)
                },
                { exception ->
                    continuation.resumeWithException(exception)
                }
            )
        }
    }

    override suspend fun release() {
        // Vosk Model doesn't have a specific close() but we can null it
        model = null
    }

    override fun transcribeStream(audioFlow: Flow<ShortArray>): Flow<TranscriptSegment> = flow {
        val currentModel = model ?: throw IllegalStateException("Vosk Model not initialized")
        val recognizer = Recognizer(currentModel, 16000.0f)
        
        audioFlow.collect { audioData ->
            if (recognizer.acceptWaveForm(audioData, audioData.size)) {
                val result = recognizer.result
                parseVoskResult(result, false)?.let { emit(it) }
            } else {
                val partialResult = recognizer.partialResult
                parseVoskResult(partialResult, true)?.let { emit(it) }
            }
        }
        
        // Final result
        val finalResult = recognizer.finalResult
        parseVoskResult(finalResult, false)?.let { emit(it) }
        
    }.flowOn(Dispatchers.Default)

    private fun parseVoskResult(jsonString: String, isPartial: Boolean): TranscriptSegment? {
        return try {
            val element = json.parseToJsonElement(jsonString).jsonObject
            val text = if (isPartial) {
                element["partial"]?.jsonPrimitive?.content ?: ""
            } else {
                element["text"]?.jsonPrimitive?.content ?: ""
            }
            
            if (text.isBlank()) return null
            
            TranscriptSegment(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                timestampMs = System.currentTimeMillis(), // Better to get from audio clock if possible
                text = text,
                speaker = null,
                confidence = 1.0f, // Vosk full result has confidence per word, we could average it
                isPartial = isPartial
            )
        } catch (e: Exception) {
            null
        }
    }
}
