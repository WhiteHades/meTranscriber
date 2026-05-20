package com.metranscriber.app.engine

import com.metranscriber.app.domain.model.TranscriptSegment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class FakeTranscriberEngine : TranscriberEngine {
  override val engineId: String = "fake"
  override val displayName: String = "Simulated Engine"
  override val isAvailable: Boolean = true
  override val isModelDownloaded: Boolean = true

  private var initialized = false

  override suspend fun initialize() {
    initialized = true
  }

  override suspend fun release() {
    initialized = false
  }

  override fun transcribeStream(audioFlow: Flow<ShortArray>): Flow<TranscriptSegment> = flow {
    var count = 1
    val phrases = listOf(
      "welcome to metranscriber offline stt engine",
      "we are capturing your speech natively in sixteen kilohertz mono",
      "offline speech recognition is fast secure and private",
      "this application runs completely on your device",
      "thank you for using metranscriber"
    )

    audioFlow.collect { chunk ->
      if (!initialized) return@collect
      delay(300)
      val phrase = phrases[(count - 1) % phrases.size]
      emit(
        TranscriptSegment(
          id = UUID.randomUUID().toString(),
          sessionId = "demo_session",
          timestampMs = count * 2000L,
          text = phrase,
          speaker = "Speaker 1",
          confidence = 0.95f,
          isPartial = false
        )
      )
      count++
    }
  }
}
