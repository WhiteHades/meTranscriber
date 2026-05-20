package com.metranscriber.app.data.repository

import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession

class FakeTranscriptionRepository : TranscriptionRepository {
  private val sessions = mutableMapOf<String, TranscriptionSession>()
  private val segments = mutableMapOf<String, MutableList<TranscriptSegment>>()

  override suspend fun saveSession(session: TranscriptionSession) {
    sessions[session.id] = session
  }

  override suspend fun getSessions(): List<TranscriptionSession> {
    return sessions.values.toList().sortedByDescending { it.createdAt }
  }

  override suspend fun getSessionById(id: String): TranscriptionSession? {
    return sessions[id]
  }

  override suspend fun deleteSession(id: String) {
    sessions.remove(id)
    segments.remove(id)
  }

  override suspend fun saveSegments(segments: List<TranscriptSegment>) {
    if (segments.isEmpty()) return
    val sessionId = segments[0].sessionId
    val list = this.segments.getOrPut(sessionId) { mutableListOf() }
    list.addAll(segments)
  }

  override suspend fun getSegmentsForSession(sessionId: String): List<TranscriptSegment> {
    return segments[sessionId] ?: emptyList()
  }
}
