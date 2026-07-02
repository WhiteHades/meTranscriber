package com.metranscriber.app.ui.viewmodel

import com.metranscriber.app.data.repository.FakeTranscriptionRepository
import com.metranscriber.app.engine.EngineManager
import com.metranscriber.app.engine.FakeTranscriberEngine
import com.metranscriber.app.engine.TranscriberEngine
import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession
import com.metranscriber.app.data.repository.TranscriptionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
    viewModel = TranscriberViewModel(repository, engineManager, audioRecorder, testDispatcher)
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
    testScheduler.advanceUntilIdle()
    assertEquals(RecordingState.IDLE, viewModel.recordingState.value)
  }

  @Test
  fun startRecording_usesSingleAudioStream() = runTest(testDispatcher) {
    viewModel.startRecording()
    testScheduler.advanceTimeBy(100)

    assertEquals(1, audioRecorder.recordStreamCalls)

    viewModel.stopRecording()
    testScheduler.advanceUntilIdle()
  }

  @Test
  fun startRecording_whenEngineInitializationFails_resetsStateAndReportsError() = runTest(testDispatcher) {
    val failingEngine = FailingEngine()
    viewModel = TranscriberViewModel(repository, EngineManager(listOf(failingEngine)), audioRecorder, testDispatcher)

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
    assertTrue(segments.isNotEmpty())
    assertEquals(2000L, segments[0].timestampMs)
    assertEquals(sessions[0].rawText, segments.joinToString(" ") { it.text })
  }

  @Test
  fun stopRecording_waitsForFinalEngineSegmentBeforeSaving() = runTest(testDispatcher) {
    viewModel = TranscriberViewModel(repository, EngineManager(listOf(FinalOnCompletionEngine())), audioRecorder, testDispatcher)

    viewModel.startRecording()
    testScheduler.advanceTimeBy(100)
    viewModel.stopRecording()
    testScheduler.advanceUntilIdle()

    val sessions = repository.getSessions().first()
    assertEquals(1, sessions.size)
    assertEquals("final words", sessions[0].rawText)
  }

  @Test
  fun stopRecording_whenNoSpeechDetected_reportsErrorAndDoesNotSaveSession() = runTest(testDispatcher) {
    viewModel = TranscriberViewModel(repository, EngineManager(listOf(SilentEngine())), audioRecorder, testDispatcher)

    viewModel.startRecording()
    testScheduler.advanceTimeBy(100)
    viewModel.stopRecording()
    testScheduler.advanceUntilIdle()

    assertEquals("No speech detected", viewModel.recordingError.value)
    assertEquals(0, repository.getSessions().first().size)
  }

  @Test
  fun importWavFile_whenNoSpeechDetected_reportsErrorAndDoesNotSaveSession() = runTest(testDispatcher) {
    viewModel = TranscriberViewModel(repository, EngineManager(listOf(SilentEngine())), audioRecorder, testDispatcher)
    val wav = WavAudioReaderTest.wavBytes(frames = ShortArray(1024) { 1000 })

    viewModel.importWavFile("silent.wav", wav)
    testScheduler.advanceUntilIdle()

    assertEquals("No speech detected", viewModel.recordingError.value)
    assertEquals(0, repository.getSessions().first().size)
  }

  @Test
  fun updateNotes_preservesLatestEditWhenWritesCompleteOutOfOrder() = runTest(testDispatcher) {
    val delayedRepository = DelayedNotesRepository()
    val session = testSession(notes = "initial")
    delayedRepository.saveSession(session)
    viewModel = TranscriberViewModel(delayedRepository, EngineManager(listOf(FakeTranscriberEngine())), audioRecorder, testDispatcher)
    viewModel.selectSession(session)

    viewModel.updateNotes("first")
    viewModel.updateNotes("second")
    testScheduler.advanceUntilIdle()

    assertEquals("second", delayedRepository.getSessionById(session.id)?.notes)
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

  private class FinalOnCompletionEngine : TranscriberEngine {
    override val engineId: String = "final"
    override val displayName: String = "Final"
    override val isAvailable: Boolean = true
    override val isModelDownloaded: Boolean = true

    override suspend fun initialize() = Unit

    override suspend fun release() = Unit

    override fun transcribeStream(audioFlow: Flow<ShortArray>): Flow<TranscriptSegment> = flow {
      audioFlow.collect { }
      emit(TranscriptSegment("final", "session", 3000L, "final words", null, 1f, false))
    }
  }

  private class SilentEngine : TranscriberEngine {
    override val engineId: String = "silent"
    override val displayName: String = "Silent"
    override val isAvailable: Boolean = true
    override val isModelDownloaded: Boolean = true

    override suspend fun initialize() = Unit

    override suspend fun release() = Unit

    override fun transcribeStream(audioFlow: Flow<ShortArray>): Flow<TranscriptSegment> = flow {
      audioFlow.collect { }
    }
  }

  private class DelayedNotesRepository : TranscriptionRepository {
    private val sessions = MutableStateFlow<Map<String, TranscriptionSession>>(emptyMap())

    override suspend fun saveSession(session: TranscriptionSession) {
      sessions.value = sessions.value + (session.id to session)
    }

    override fun getSessions(): Flow<List<TranscriptionSession>> = sessions.map { it.values.toList() }

    override suspend fun getSessionById(id: String): TranscriptionSession? = sessions.value[id]

    override suspend fun updateSessionNotes(sessionId: String, notes: String?) {
      if (notes == "first") delay(100)
      val session = sessions.value[sessionId] ?: return
      sessions.value = sessions.value + (sessionId to session.copy(notes = notes))
    }

    override suspend fun deleteSession(id: String) {
      sessions.value = sessions.value - id
    }

    override suspend fun saveSegments(segments: List<TranscriptSegment>) = Unit

    override fun getSegmentsForSession(sessionId: String): Flow<List<TranscriptSegment>> = emptyFlow()

    override suspend fun saveFullTranscription(session: TranscriptionSession, segments: List<TranscriptSegment>) {
      saveSession(session)
    }
  }

  private fun testSession(notes: String? = null): TranscriptionSession = TranscriptionSession(
    id = "session_1",
    title = "Session",
    createdAt = 123L,
    durationMs = 456L,
    language = "en",
    engineUsed = "test",
    rawText = "text",
    wordCount = 1,
    audioFilePath = null,
    notes = notes
  )
}
