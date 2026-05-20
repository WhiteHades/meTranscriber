package com.metranscriber.app.data.repository

import com.metranscriber.app.data.db.dao.TranscriptionDao
import com.metranscriber.app.data.db.entities.TranscriptSegmentEntity
import com.metranscriber.app.data.db.entities.TranscriptionEntity
import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTranscriptionRepository(private val dao: TranscriptionDao) : TranscriptionRepository {

    override suspend fun saveSession(session: TranscriptionSession) {
        dao.insertSession(TranscriptionEntity.fromDomain(session))
    }

    override fun getSessions(): Flow<List<TranscriptionSession>> {
        return dao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSessionById(id: String): TranscriptionSession? {
        return dao.getSessionById(id)?.toDomain()
    }

    override suspend fun deleteSession(id: String) {
        dao.deleteSession(id)
    }

    override suspend fun saveSegments(segments: List<TranscriptSegment>) {
        dao.insertSegments(segments.map { TranscriptSegmentEntity.fromDomain(it) })
    }

    override fun getSegmentsForSession(sessionId: String): Flow<List<TranscriptSegment>> {
        return dao.getSegmentsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveFullTranscription(session: TranscriptionSession, segments: List<TranscriptSegment>) {
        dao.saveTranscription(
            TranscriptionEntity.fromDomain(session),
            segments.map { TranscriptSegmentEntity.fromDomain(it) }
        )
    }
}
