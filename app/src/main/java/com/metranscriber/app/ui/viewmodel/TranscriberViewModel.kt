package com.metranscriber.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metranscriber.app.data.repository.RoomTranscriptionRepository
import com.metranscriber.app.data.db.AppDatabase
import androidx.room.Room
import com.metranscriber.app.data.repository.TranscriptionRepository
import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession
import com.metranscriber.app.engine.EngineManager
import com.metranscriber.app.engine.TranscriberEngine
import com.metranscriber.app.engine.VoskTranscriberEngine
import com.metranscriber.app.engine.audio.AudioRecorder
import com.metranscriber.app.engine.audio.AndroidAudioRecorder
import com.metranscriber.app.engine.audio.WavAudioReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

enum class RecordingState {
  IDLE, RECORDING
}

class TranscriberViewModel(
  private val repository: TranscriptionRepository,
  private val engineManager: EngineManager,
  private val audioRecorder: AudioRecorder
) : ViewModel() {
  
  private val _recordingState = MutableStateFlow(RecordingState.IDLE)
  val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

  val activeEngine: StateFlow<TranscriberEngine> = engineManager.activeEngine

  private val _timerText = MutableStateFlow("00:00")
  val timerText: StateFlow<String> = _timerText.asStateFlow()

  private val _waveformAmplitudes = MutableStateFlow<List<Float>>(emptyList())
  val waveformAmplitudes: StateFlow<List<Float>> = _waveformAmplitudes.asStateFlow()

  private val _liveSegments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
  val liveSegments: StateFlow<List<TranscriptSegment>> = _liveSegments.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _recordingError = MutableStateFlow<String?>(null)
  val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

  private val _isImporting = MutableStateFlow(false)
  val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

  val sessionsList: StateFlow<List<TranscriptionSession>> = combine(
    repository.getSessions(),
    _searchQuery
  ) { sessions, query ->
    if (query.isBlank()) {
      sessions
    } else {
      sessions.filter {
        it.title.contains(query, ignoreCase = true) ||
        it.rawText.contains(query, ignoreCase = true) ||
        (it.notes?.contains(query, ignoreCase = true) == true)
      }
    }
  }.stateInViewModel(emptyList())

  private val _selectedSession = MutableStateFlow<TranscriptionSession?>(null)
  val selectedSession: StateFlow<TranscriptionSession?> = _selectedSession.asStateFlow()

  private val _selectedSessionSegments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
  val selectedSessionSegments: StateFlow<List<TranscriptSegment>> = _selectedSessionSegments.asStateFlow()

  private val _selectedSessionNotes = MutableStateFlow("")
  val selectedSessionNotes: StateFlow<String> = _selectedSessionNotes.asStateFlow()

  private var recordingJob: Job? = null
  private var timerJob: Job? = null
  private var selectedSessionJob: Job? = null
  private var startTimeMs = 0L
  private val exportJson = Json { prettyPrint = true }
  private val notesSaveMutex = Mutex()

  init {
    loadSessions()
  }

  fun loadSessions() {
    // Handled by sessionsList Flow combine
  }

  fun startRecording() {
    if (_recordingState.value == RecordingState.RECORDING || _isImporting.value || recordingJob?.isActive == true) return
    
    _recordingState.value = RecordingState.RECORDING
    _recordingError.value = null
    _liveSegments.value = emptyList()
    _waveformAmplitudes.value = emptyList()
    startTimeMs = System.currentTimeMillis()

    // Start timer job
    timerJob = viewModelScope.launch {
      while (_recordingState.value == RecordingState.RECORDING) {
        val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000
        val mins = elapsedSec / 60
        val secs = elapsedSec % 60
        _timerText.value = String.format(Locale.US, "%02d:%02d", mins, secs)
        delay(1000)
      }
    }

    // Start audio record flow
    recordingJob = viewModelScope.launch {
      val engine = activeEngine.value
      try {
        engine.initialize()
        if (_recordingState.value != RecordingState.RECORDING) return@launch
        
        val recordFlow = audioRecorder.recordStream().onEach(::updateWaveform)

        // Feed the same microphone stream to STT and waveform rendering.
        engine.transcribeStream(recordFlow).collect { segment ->
          _liveSegments.value = _liveSegments.value + segment
        }
        saveRecordingSession(System.currentTimeMillis() - startTimeMs)
        resetRecordingUiState(cancelRecordingJob = false)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _recordingError.value = e.message ?: "Transcription failed"
        resetRecordingUiState(cancelRecordingJob = false)
      }
    }
  }

  fun stopRecording() {
    if (_recordingState.value == RecordingState.IDLE) return
    
    _recordingState.value = RecordingState.IDLE
    timerJob?.cancel()
    audioRecorder.stopRecording()
    _timerText.value = "00:00"
    _waveformAmplitudes.value = emptyList()
  }

  fun importWavFile(displayName: String, wavBytes: ByteArray) {
    if (_recordingState.value == RecordingState.RECORDING || _isImporting.value) return

    _isImporting.value = true
    _recordingError.value = null
    _liveSegments.value = emptyList()
    _waveformAmplitudes.value = emptyList()
    startTimeMs = System.currentTimeMillis()

    viewModelScope.launch {
      try {
        val decodedAudio = withContext(Dispatchers.Default) {
          WavAudioReader.decode(wavBytes)
        }
        val engine = activeEngine.value
        engine.initialize()

        val importedSegments = mutableListOf<TranscriptSegment>()
        val audioFlow = flow {
          decodedAudio.chunks.forEach { chunk ->
            updateWaveform(chunk)
            emit(chunk)
          }
        }

        engine.transcribeStream(audioFlow).collect { segment ->
          importedSegments += segment
          _liveSegments.value = importedSegments.toList()
        }

        val segmentsToSave = segmentsForSavedSession(importedSegments)
        val text = segmentsToSave.joinToString(" ") { it.text }
        if (text.isNotBlank()) {
          val sessionId = UUID.randomUUID().toString()
          saveCompletedTranscription(
            sessionId = sessionId,
            title = "Imported ${displayName.ifBlank { "WAV audio" }}",
            durationMs = decodedAudio.durationMs,
            sourcePath = displayName,
            text = text,
            wordCount = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size,
            segments = segmentsToSave
          )
          loadSessions()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _recordingError.value = e.message ?: "Audio import failed"
      } finally {
        _isImporting.value = false
        _waveformAmplitudes.value = emptyList()
      }
    }
  }

  fun clearRecordingError() {
    _recordingError.value = null
  }

  private fun updateWaveform(chunk: ShortArray) {
    if (chunk.isEmpty()) return
    val avgAmplitude = chunk.map { abs(it.toInt()).toFloat() }.average().toFloat()
    val scaled = (avgAmplitude / 32768f) * 1.5f
    val currentList = _waveformAmplitudes.value.toMutableList()
    if (currentList.size > 40) {
      currentList.removeAt(0)
    }
    currentList.add(scaled.coerceIn(0.05f, 1f))
    _waveformAmplitudes.value = currentList
  }

  private fun resetRecordingUiState(cancelRecordingJob: Boolean = true) {
    _recordingState.value = RecordingState.IDLE
    timerJob?.cancel()
    if (cancelRecordingJob) {
      recordingJob?.cancel()
    }
    _timerText.value = "00:00"
    _waveformAmplitudes.value = emptyList()
  }

  fun switchEngine(engineId: String) {
    viewModelScope.launch {
      engineManager.switchEngine(engineId)
    }
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun deleteSession(sessionId: String) {
    viewModelScope.launch {
      repository.deleteSession(sessionId)
      if (_selectedSession.value?.id == sessionId) {
        _selectedSession.value = null
      }
      loadSessions()
    }
  }

  fun selectSession(session: TranscriptionSession?) {
    selectedSessionJob?.cancel()
    _selectedSession.value = session
    if (session != null) {
      _selectedSessionNotes.value = session.notes ?: ""
      selectedSessionJob = viewModelScope.launch {
        repository.getSegmentsForSession(session.id).collect {
          _selectedSessionSegments.value = it
        }
      }
    } else {
      _selectedSessionSegments.value = emptyList()
      _selectedSessionNotes.value = ""
      selectedSessionJob = null
    }
  }

  fun updateNotes(notes: String) {
    _selectedSessionNotes.value = notes
    val current = _selectedSession.value ?: return
    viewModelScope.launch {
      val updated = current.copy(notes = notes)
      notesSaveMutex.withLock {
        repository.updateSessionNotes(current.id, notes)
      }
      if (_selectedSession.value?.id == current.id) {
        _selectedSession.value = updated
      }
      loadSessions()
    }
  }

  fun exportSessionAsTxt(session: TranscriptionSession): String {
    val sb = java.lang.StringBuilder()
    sb.append("Title: ${session.title}\n")
    sb.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(session.createdAt)}\n")
    sb.append("Engine Used: ${session.engineUsed}\n")
    sb.append("Notes: ${session.notes ?: ""}\n")
    sb.append("\nTranscript:\n")
    sb.append(session.rawText)
    return sb.toString()
  }

  fun exportSessionAsJson(session: TranscriptionSession, segments: List<TranscriptSegment>): String {
    return exportJson.encodeToString(TranscriptionExport.from(session, segments))
  }

  fun exportSessionAsSrt(segments: List<TranscriptSegment>): String {
    val sb = java.lang.StringBuilder()
    val exportSegments = segments.filter { it.text.isNotBlank() }.sortedBy { it.timestampMs }
    exportSegments.forEachIndexed { index, segment ->
      val start = segment.timestampMs
      val nextStart = exportSegments.getOrNull(index + 1)?.timestampMs
      val end = if (nextStart != null && nextStart > start) nextStart else start + 2000L
      sb.append("${index + 1}\n")
      sb.append("${formatSrtTime(start)} --> ${formatSrtTime(end)}\n")
      sb.append("${segment.text}\n\n")
    }
    return sb.toString()
  }

  private fun formatSrtTime(ms: Long): String {
    val hrs = ms / 3600000
    val mins = (ms % 3600000) / 60000
    val secs = (ms % 60000) / 1000
    val millis = ms % 1000
    return String.format(Locale.US, "%02d:%02d:%02d,%03d", hrs, mins, secs, millis)
  }

  private fun segmentsForSavedSession(segments: List<TranscriptSegment> = _liveSegments.value): List<TranscriptSegment> {
    val finalized = segments.filter { !it.isPartial && it.text.isNotBlank() }
    if (finalized.isNotEmpty()) return finalized
    return segments.lastOrNull { it.text.isNotBlank() }?.let { listOf(it) } ?: emptyList()
  }

  private suspend fun saveRecordingSession(durationMs: Long) {
    val segmentsToSave = segmentsForSavedSession()
    val text = segmentsToSave.joinToString(" ") { it.text }
    if (text.isBlank()) return

    val sessionId = UUID.randomUUID().toString()
    saveCompletedTranscription(
      sessionId = sessionId,
      title = "Recording " + String.format(Locale.US, "%tF %<tR", System.currentTimeMillis()),
      durationMs = durationMs,
      sourcePath = null,
      text = text,
      wordCount = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size,
      segments = segmentsToSave
    )
    loadSessions()
  }

  private suspend fun saveCompletedTranscription(
    sessionId: String,
    title: String,
    durationMs: Long,
    sourcePath: String?,
    text: String,
    wordCount: Int,
    segments: List<TranscriptSegment>
  ) {
    val newSession = TranscriptionSession(
      id = sessionId,
      title = title,
      createdAt = System.currentTimeMillis(),
      durationMs = durationMs,
      language = "en",
      engineUsed = activeEngine.value.engineId,
      rawText = text,
      wordCount = wordCount,
      audioFilePath = sourcePath,
      notes = ""
    )

    val mappedSegments = segments.map { segment ->
      segment.copy(
        id = UUID.randomUUID().toString(),
        sessionId = sessionId,
        timestampMs = normalizedTimestamp(segment.timestampMs),
        isPartial = false
      )
    }
    repository.saveFullTranscription(newSession, mappedSegments)
  }

  private fun normalizedTimestamp(timestampMs: Long): Long {
    val relative = if (timestampMs >= startTimeMs) timestampMs - startTimeMs else timestampMs
    return relative.coerceAtLeast(0L)
  }

  private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInViewModel(initialValue: T): StateFlow<T> {
    val flow = this
    val state = MutableStateFlow(initialValue)
    viewModelScope.launch {
      flow.collect { state.value = it }
    }
    return state.asStateFlow()
  }
  
  companion object {
    fun createFactory(context: Context): TranscriberViewModel {
      val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "transcriptions.db"
      ).build()
      val repository = RoomTranscriptionRepository(db.transcriptionDao())
      val engineManager = EngineManager(listOf(VoskTranscriberEngine(context, UUID.randomUUID().toString())))
      val audioRecorder = AndroidAudioRecorder()
      return TranscriberViewModel(repository, engineManager, audioRecorder)
    }
  }

  @Serializable
  private data class TranscriptionExport(
    val id: String,
    val title: String,
    val createdAt: Long,
    val durationMs: Long,
    val language: String,
    val engineUsed: String,
    val rawText: String,
    val wordCount: Int,
    val audioFilePath: String?,
    val notes: String?,
    val segments: List<TranscriptSegment>
  ) {
    companion object {
      fun from(session: TranscriptionSession, segments: List<TranscriptSegment>): TranscriptionExport =
        TranscriptionExport(
          id = session.id,
          title = session.title,
          createdAt = session.createdAt,
          durationMs = session.durationMs,
          language = session.language,
          engineUsed = session.engineUsed,
          rawText = session.rawText,
          wordCount = session.wordCount,
          audioFilePath = session.audioFilePath,
          notes = session.notes,
          segments = segments
        )
    }
  }
}
