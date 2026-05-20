package com.metranscriber.app.engine

import com.metranscriber.app.domain.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow

interface TranscriberEngine {
  val engineId: String
  val displayName: String
  val isAvailable: Boolean
  val isModelDownloaded: Boolean

  suspend fun initialize()
  suspend fun release()

  fun transcribeStream(audioFlow: Flow<ShortArray>): Flow<TranscriptSegment>
}
