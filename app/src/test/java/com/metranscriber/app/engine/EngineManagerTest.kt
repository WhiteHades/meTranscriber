package com.metranscriber.app.engine

import com.metranscriber.app.domain.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineManagerTest {
  @Test
  fun activeEngine_prefersAvailableEngineBeforeModelInitialization() {
    val engine = StubEngine(isModelDownloaded = false)

    val manager = EngineManager(listOf(engine))

    assertEquals("stub", manager.activeEngine.value.engineId)
  }

  private class StubEngine(
    override val isModelDownloaded: Boolean
  ) : TranscriberEngine {
    override val engineId: String = "stub"
    override val displayName: String = "Stub"
    override val isAvailable: Boolean = true

    override suspend fun initialize() = Unit

    override suspend fun release() = Unit

    override fun transcribeStream(audioFlow: Flow<ShortArray>): Flow<TranscriptSegment> = emptyFlow()
  }
}
