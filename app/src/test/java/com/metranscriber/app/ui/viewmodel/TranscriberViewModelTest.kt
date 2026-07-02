package com.metranscriber.app.ui.viewmodel

import com.metranscriber.app.data.repository.FakeTranscriptionRepository
import com.metranscriber.app.engine.EngineManager
import com.metranscriber.app.engine.FakeTranscriberEngine
import com.metranscriber.app.engine.TranscriberEngine
import com.metranscriber.app.domain.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import com.metranscriber.app.engine.audio.FakeAudioRecorder
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriberViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: FakeTranscriptionRepository
  private lateinit var audioRecorder: FakeAudioRecorder
  private lateinit var viewModel: TranscriberViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = FakeTranscriptionRepository()
    val engineManager = EngineManager(listOf(FakeTranscriberEngine()))
    audioRecorder = FakeAudioRecorder()
    viewModel = TranscriberViewModel(repository, engineManager, audioRecorder)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_isCorrect() = runTest {
    assertEquals(RecordingState.IDLE, viewModel.recordingState.value)
    assertEquals("00:00", viewModel.timerText.value)
    val sessions = repository.getSessions().first()
    assertEquals(0, sessions.size)
    assertEquals(0, viewModel.waveformAmplitudes.value.size)
    assertEquals(0, viewModel.liveSegments.value.size)
    assertNotNull(viewModel.activeEngine.value)
  }

  @Test
  fun startRecording_changesStateToRecording() = runTest(testDispatcher) {
    viewModel.startRecording()
    testScheduler.advanceTimeBy(100)
    assertEquals(RecordingState.RECORDING, viewModel.recordingState.value)
    viewModel.stopRecording()
    testScheduler.advanceTimeBy(100)
  }

  @Test
  fun startRecording_usesSingleAudioStream() = runTest(testDispatcher) {
    viewModel.startRecording()
    testScheduler.advanceTimeBy(100)

    assertEquals(1, audioRecorder.recordStreamCalls)

    viewModel.stopRecording()
    testScheduler.advanceTimeBy(100)
  }

  @Test
  fun startRecording_whenEngineInitializationFails_resetsStateAndReportsError() = runTest(testDispatcher) {
    val failingEngine = FailingEngine()
    viewModel = TranscriberViewModel(repository, EngineManager(listOf(failingEngine)), audioRecorder)

    viewModel.startRecording()
    testScheduler.runCurrent()

    assertEquals(RecordingState.IDLE, viewModel.recordingState.value)
    assertEquals("missing model", viewModel.recordingError.value)
  }

  @Test
  fun stopRecording_savesSessionCorrectly() = runTest(testDispatcher) {
    viewModel.startRecording()
    testScheduler.advanceTimeBy(500) // Allow some segments to be emitted
    viewModel.stopRecording()
    testScheduler.advanceTimeBy(500)
    val sessions = repository.getSessions().first()
    assertEquals(1, sessions.size)
    // The title starts with "Recording " followed by the date
    assert(sessions[0].title.startsWith("Recording "))
  }

  private class FailingEngine : TranscriberEngine {
    override val engineId: String = "failing"
    override val displayName: String = "Failing"
    override val isAvailable: Boolean = true
    override val isModelDownloaded: Boolean = false

    override suspend fun initialize() {
      throw IllegalStateException("missing model")
    }

    override suspend fun release() = Unit

    override fun transcribeStream(audioFlow: Flow<ShortArray>): Flow<TranscriptSegment> = emptyFlow()
  }
}
