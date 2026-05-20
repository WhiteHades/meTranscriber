package com.metranscriber.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.metranscriber.app.domain.model.TranscriptionSession

@Entity(tableName = "sessions")
data class TranscriptionEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    val language: String,
    @ColumnInfo(name = "engine_used") val engineUsed: String,
    @ColumnInfo(name = "raw_text") val rawText: String,
    @ColumnInfo(name = "word_count") val wordCount: Int,
    @ColumnInfo(name = "audio_file_path") val audioFilePath: String?,
    val notes: String?
) {
    fun toDomain(): TranscriptionSession = TranscriptionSession(
        id = id,
        title = title,
        createdAt = createdAt,
        durationMs = durationMs,
        language = language,
        engineUsed = engineUsed,
        rawText = rawText,
        wordCount = wordCount,
        audioFilePath = audioFilePath,
        notes = notes
    )

    companion object {
        fun fromDomain(session: TranscriptionSession): TranscriptionEntity = TranscriptionEntity(
            id = session.id,
            title = session.title,
            createdAt = session.createdAt,
            durationMs = session.durationMs,
            language = session.language,
            engineUsed = session.engineUsed,
            rawText = session.rawText,
            wordCount = session.wordCount,
            audioFilePath = session.audioFilePath,
            notes = session.notes
        )
    }
}
