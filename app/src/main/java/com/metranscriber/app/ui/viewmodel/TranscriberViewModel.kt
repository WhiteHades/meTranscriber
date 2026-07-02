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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
  private var startTimeMs = 0L

  init {
    loadSessions()
  }

  fun loadSessions() {
    // Handled by sessionsList Flow combine
  }

  fun startRecording() {
    if (_recordingState.value == RecordingState.RECORDING) return
    
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
        _timerText.value = String.format("%02d:%02d", mins, secs)
        delay(1000)
      }
    }

    // Start audio record flow
    recordingJob = viewModelScope.launch {
      val engine = activeEngine.value
      try {
        engine.initialize()
        
        val recordFlow = audioRecorder.recordStream().onEach(::updateWaveform)

        // Feed the same microphone stream to STT and waveform rendering.
        engine.transcribeStream(recordFlow).collect { segment ->
          _liveSegments.value = _liveSegments.value + segment
        }
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
    recordingJob?.cancel()
    
    val duration = System.currentTimeMillis() - startTimeMs
    val text = _liveSegments.value.joinToString(" ") { it.text }
    
    if (text.isNotBlank()) {
      viewModelScope.launch {
        val sessionId = UUID.randomUUID().toString()
        val wordCount = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        
        val newSession = TranscriptionSession(
          id = sessionId,
          title = "Recording " + String.format("%tF %<tR", System.currentTimeMillis()),
          createdAt = System.currentTimeMillis(),
          durationMs = duration,
          language = "en",
          engineUsed = activeEngine.value.engineId,
          rawText = text,
          wordCount = wordCount,
          audioFilePath = null,
          notes = ""
        )
        
        repository.saveSession(newSession)
        
        // Save segments mapped to this new session
        val mappedSegments = _liveSegments.value.mapIndexed { idx, seg ->
          seg.copy(id = UUID.randomUUID().toString(), sessionId = sessionId, timestampMs = idx * 2000L)
        }
        repository.saveSegments(mappedSegments)
        
        loadSessions()
      }
    }
    
    _timerText.value = "00:00"
    _waveformAmplitudes.value = emptyList()
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
    _selectedSession.value = session
    if (session != null) {
      _selectedSessionNotes.value = session.notes ?: ""
      viewModelScope.launch {
        repository.getSegmentsForSession(session.id).collect {
          _selectedSessionSegments.value = it
        }
      }
    } else {
      _selectedSessionSegments.value = emptyList()
      _selectedSessionNotes.value = ""
    }
  }

  fun updateNotes(notes: String) {
    _selectedSessionNotes.value = notes
    val current = _selectedSession.value ?: return
    viewModelScope.launch {
      val updated = current.copy(notes = notes)
      repository.saveSession(updated)
      _selectedSession.value = updated
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
    val sb = java.lang.StringBuilder()
    sb.append("{\n")
    sb.append("  \"id\": \"${session.id}\",\n")
    sb.append("  \"title\": \"${session.title.escapeJson()}\",\n")
    sb.append("  \"createdAt\": ${session.createdAt},\n")
    sb.append("  \"durationMs\": ${session.durationMs},\n")
    sb.append("  \"engineUsed\": \"${session.engineUsed}\",\n")
    sb.append("  \"notes\": \"${(session.notes ?: "").escapeJson()}\",\n")
    sb.append("  \"rawText\": \"${session.rawText.escapeJson()}\",\n")
    sb.append("  \"segments\": [\n")
    segments.forEachIndexed { index, segment ->
      sb.append("    {\n")
      sb.append("      \"timestampMs\": ${segment.timestampMs},\n")
      sb.append("      \"text\": \"${segment.text.escapeJson()}\",\n")
      sb.append("      \"speaker\": \"${(segment.speaker ?: "").escapeJson()}\",\n")
      sb.append("      \"confidence\": ${segment.confidence}\n")
      sb.append("    }${if (index < segments.size - 1) "," else ""}\n")
    }
    sb.append("  ]\n")
    sb.append("}")
    return sb.toString()
  }

  fun exportSessionAsSrt(segments: List<TranscriptSegment>): String {
    val sb = java.lang.StringBuilder()
    segments.forEachIndexed { index, segment ->
      val start = segment.timestampMs
      val end = segment.timestampMs + 2000L
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
    return String.format("%02d:%02d:%02d,%03d", hrs, mins, secs, millis)
  }

  private fun String.escapeJson(): String {
    return this.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
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
}
