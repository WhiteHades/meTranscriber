package com.metranscriber.app.data.repository

import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTranscriptionRepository : TranscriptionRepository {
    private val _sessions = MutableStateFlow<Map<String, TranscriptionSession>>(emptyMap())
    private val _segments = MutableStateFlow<Map<String, List<TranscriptSegment>>>(emptyMap())

    override suspend fun saveSession(session: TranscriptionSession) {
        _sessions.value = _sessions.value + (session.id to session)
    }

    override fun getSessions(): Flow<List<TranscriptionSession>> {
        return _sessions.map { it.values.toList().sortedByDescending { it.createdAt } }
    }

    override suspend fun getSessionById(id: String): TranscriptionSession? {
        return _sessions.value[id]
    }

    override suspend fun updateSessionNotes(sessionId: String, notes: String?) {
        val session = _sessions.value[sessionId] ?: return
        _sessions.value = _sessions.value + (sessionId to session.copy(notes = notes))
    }

    override suspend fun deleteSession(id: String) {
        _sessions.value = _sessions.value - id
        _segments.value = _segments.value - id
    }

    override suspend fun saveSegments(segments: List<TranscriptSegment>) {
        if (segments.isEmpty()) return
        val sessionId = segments[0].sessionId
        val currentSegments = _segments.value[sessionId] ?: emptyList()
        _segments.value = _segments.value + (sessionId to (currentSegments + segments))
    }

    override fun getSegmentsForSession(sessionId: String): Flow<List<TranscriptSegment>> {
        return _segments.map { it[sessionId] ?: emptyList() }
    }

    override suspend fun saveFullTranscription(session: TranscriptionSession, segments: List<TranscriptSegment>) {
        saveSession(session)
        saveSegments(segments)
    }
}
