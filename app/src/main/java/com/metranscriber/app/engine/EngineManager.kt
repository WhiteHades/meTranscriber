package com.metranscriber.app.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException

class EngineManager(
  private val engines: List<TranscriberEngine>
) {
  private val _activeEngine = MutableStateFlow<TranscriberEngine>(
    engines.firstOrNull { it.isAvailable } ?: FakeTranscriberEngine()
  )
  val activeEngine: StateFlow<TranscriberEngine> = _activeEngine.asStateFlow()

  fun getAvailableEngines(): List<TranscriberEngine> {
    return engines.filter { it.isAvailable }
  }

  suspend fun switchEngine(engineId: String): Boolean {
    val engine = engines.firstOrNull { it.engineId == engineId } ?: return false
    if (!engine.isAvailable) return false

    return try {
      engine.initialize()
      if (engine !== _activeEngine.value) {
        _activeEngine.value.release()
        _activeEngine.value = engine
      }
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      false
    }
  }
}
