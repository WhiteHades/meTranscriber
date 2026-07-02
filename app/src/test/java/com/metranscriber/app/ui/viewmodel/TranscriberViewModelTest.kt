package com.metranscriber.app.ui.viewmodel

import com.metranscriber.app.data.repository.FakeTranscriptionRepository
import com.metranscriber.app.engine.EngineManager
import com.metranscriber.app.engine.FakeTranscriberEngine
import com.metranscriber.app.engine.TranscriberEngine
import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import com.metranscriber.app.engine.audio.FakeAudioRecorder
import com.metranscriber.app.engine.audio.WavAudioReaderTest
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    assertTrue(sessions[0].title.startsWith("Recording "))
    val segments = repository.getSegmentsForSession(sessions[0].id).first()
    assertEquals(1, segments.size)
    assertEquals(2000L, segments[0].timestampMs)
    assertEquals(sessions[0].rawText, segments.joinToString(" ") { it.text })
  }

  @Test
  fun exportSessionAsJson_handlesSpecialCharacters() {
    val session = TranscriptionSession(
      id = "session_1",
      title = "Quote \" and slash \\",
      createdAt = 123L,
      durationMs = 456L,
      language = "en",
      engineUsed = "test",
      rawText = "line one\nline two \\",
      wordCount = 4,
      audioFilePath = null,
      notes = "notes \" \\"
    )
    val segment = TranscriptSegment(
      id = "segment_1",
      sessionId = "session_1",
      timestampMs = 1000L,
      text = "segment \" text \\",
      speaker = null,
      confidence = 0.9f,
      isPartial = false
    )

    val parsed = Json.parseToJsonElement(viewModel.exportSessionAsJson(session, listOf(segment))).jsonObject

    assertEquals(session.title, parsed["title"]?.jsonPrimitive?.content)
    assertEquals(segment.text, parsed["segments"]?.jsonArray?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content)
  }

  @Test
  fun exportSessionAsSrt_usesNextSegmentTimestampForEndTime() {
    val segments = listOf(
      TranscriptSegment("1", "session", 0L, "first", null, 1f, false),
      TranscriptSegment("2", "session", 1500L, "second", null, 1f, false),
    )

    val srt = viewModel.exportSessionAsSrt(segments)

    assertTrue(srt.contains("00:00:00,000 --> 00:00:01,500"))
    assertTrue(srt.contains("00:00:01,500 --> 00:00:03,500"))
  }

  @Test
  fun importWavFile_savesImportedSession() = runTest(testDispatcher) {
    val wav = WavAudioReaderTest.wavBytes(frames = ShortArray(1024) { 1000 })

    viewModel.importWavFile("sample.wav", wav)
    testScheduler.advanceUntilIdle()

    val sessions = repository.getSessions().first()
    assertEquals(1, sessions.size)
    assertEquals("Imported sample.wav", sessions[0].title)
    assertEquals("sample.wav", sessions[0].audioFilePath)
    assertFalse(viewModel.isImporting.value)
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
