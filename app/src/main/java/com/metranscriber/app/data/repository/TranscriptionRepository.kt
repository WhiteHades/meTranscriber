package com.metranscriber.app.data.repository

import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession

import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {
  suspend fun saveSession(session: TranscriptionSession)
  fun getSessions(): Flow<List<TranscriptionSession>>
  suspend fun getSessionById(id: String): TranscriptionSession?
  suspend fun deleteSession(id: String)
  suspend fun saveSegments(segments: List<TranscriptSegment>)
  fun getSegmentsForSession(sessionId: String): Flow<List<TranscriptSegment>>
  suspend fun saveFullTranscription(session: TranscriptionSession, segments: List<TranscriptSegment>)
}
