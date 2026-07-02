package com.metranscriber.app.engine

import com.metranscriber.app.domain.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EngineManagerTest {
  @Test
  fun activeEngine_prefersAvailableEngineBeforeModelInitialization() {
    val engine = StubEngine(isModelDownloaded = false)

    val manager = EngineManager(listOf(engine))

    assertEquals("stub", manager.activeEngine.value.engineId)
  }

  @Test
  fun switchEngine_whenNewEngineFails_keepsCurrentEngineActive() = runTest {
    val current = StubEngine(id = "current", isModelDownloaded = true)
    val failing = StubEngine(id = "failing", isModelDownloaded = false, failInitialize = true)
    val manager = EngineManager(listOf(current, failing))

    val switched = manager.switchEngine("failing")

    assertFalse(switched)
    assertEquals("current", manager.activeEngine.value.engineId)
    assertEquals(0, current.releaseCount)
  }

  private class StubEngine(
    private val id: String = "stub",
    override val isModelDownloaded: Boolean,
    private val failInitialize: Boolean = false
  ) : TranscriberEngine {
    override val engineId: String = id
    override val displayName: String = "Stub"
    override val isAvailable: Boolean = true
    var releaseCount = 0
      private set

    override suspend fun initialize() {
      if (failInitialize) throw IllegalStateException("initialization failed")
    }

    override suspend fun release() {
      releaseCount++
    }

    override fun transcribeStream(audioFlow: Flow<ShortArray>): Flow<TranscriptSegment> = emptyFlow()
  }
}
