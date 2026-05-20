package com.metranscriber.app.data.repository

import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession

interface TranscriptionRepository {
  suspend fun saveSession(session: TranscriptionSession)
  suspend fun getSessions(): List<TranscriptionSession>
  suspend fun getSessionById(id: String): TranscriptionSession?
  suspend fun deleteSession(id: String)
  suspend fun saveSegments(segments: List<TranscriptSegment>)
  suspend fun getSegmentsForSession(sessionId: String): List<TranscriptSegment>
}
