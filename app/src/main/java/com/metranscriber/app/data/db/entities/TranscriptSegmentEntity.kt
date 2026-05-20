package com.metranscriber.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.metranscriber.app.domain.model.TranscriptSegment

@Entity(
    tableName = "segments",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["session_id"])]
)
data class TranscriptSegmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "timestamp_ms") val timestampMs: Long,
    val text: String,
    val speaker: String?,
    val confidence: Float,
    @ColumnInfo(name = "is_partial") val isPartial: Boolean
) {
    fun toDomain(): TranscriptSegment = TranscriptSegment(
        id = id,
        sessionId = sessionId,
        timestampMs = timestampMs,
        text = text,
        speaker = speaker,
        confidence = confidence,
        isPartial = isPartial
    )

    companion object {
        fun fromDomain(segment: TranscriptSegment): TranscriptSegmentEntity = TranscriptSegmentEntity(
            id = segment.id,
            sessionId = segment.sessionId,
            timestampMs = segment.timestampMs,
            text = segment.text,
            speaker = segment.speaker,
            confidence = segment.confidence,
            isPartial = segment.isPartial
        )
    }
}
