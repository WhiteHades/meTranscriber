package com.metranscriber.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.metranscriber.app.data.db.dao.TranscriptionDao
import com.metranscriber.app.data.db.entities.TranscriptSegmentEntity
import com.metranscriber.app.data.db.entities.TranscriptionEntity

@Database(
    entities = [TranscriptionEntity::class, TranscriptSegmentEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transcriptionDao(): TranscriptionDao
}
