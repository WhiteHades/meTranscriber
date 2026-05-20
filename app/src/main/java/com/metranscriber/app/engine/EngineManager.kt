package com.metranscriber.app.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EngineManager(
  private val engines: List<TranscriberEngine>
) {
  private val _activeEngine = MutableStateFlow<TranscriberEngine>(
    engines.firstOrNull { it.isAvailable && it.isModelDownloaded } ?: FakeTranscriberEngine()
  )
  val activeEngine: StateFlow<TranscriberEngine> = _activeEngine.asStateFlow()

  fun getAvailableEngines(): List<TranscriberEngine> {
    return engines.filter { it.isAvailable }
  }

  suspend fun switchEngine(engineId: String): Boolean {
    val engine = engines.firstOrNull { it.engineId == engineId } ?: return false
    if (!engine.isAvailable || !engine.isModelDownloaded) return false

    _activeEngine.value.release()
    engine.initialize()
    _activeEngine.value = engine
    return true
  }
}
