package com.metranscriber.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.metranscriber.app.data.db.entities.TranscriptSegmentEntity
import com.metranscriber.app.data.db.entities.TranscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM sessions ORDER BY created_at DESC")
    fun getAllSessions(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): TranscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TranscriptionEntity): Long

    @Query("UPDATE sessions SET notes = :notes WHERE id = :sessionId")
    suspend fun updateSessionNotes(sessionId: String, notes: String?)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT * FROM segments WHERE session_id = :sessionId ORDER BY timestamp_ms ASC")
    fun getSegmentsForSession(sessionId: String): Flow<List<TranscriptSegmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>): List<Long>

    @Transaction
    suspend fun saveTranscription(session: TranscriptionEntity, segments: List<TranscriptSegmentEntity>): Long {
        insertSession(session)
        insertSegments(segments)
        return 1L
    }
}
