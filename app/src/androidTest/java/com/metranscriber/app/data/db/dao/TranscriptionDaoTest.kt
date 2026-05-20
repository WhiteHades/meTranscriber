package com.metranscriber.app.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.metranscriber.app.data.db.AppDatabase
import com.metranscriber.app.data.db.entities.TranscriptionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranscriptionDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TranscriptionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.transcriptionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetSession() = runBlocking {
        val session = TranscriptionEntity(
            id = "test_id",
            title = "Test Session",
            createdAt = 123456789L,
            durationMs = 5000L,
            language = "en",
            engineUsed = "vosk",
            rawText = "hello world",
            wordCount = 2,
            audioFilePath = null,
            notes = "test notes"
        )
        dao.insertSession(session)
        val sessions = dao.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals(session, sessions[0])
    }

    @Test
    fun deleteSession() = runBlocking {
        val session = TranscriptionEntity(
            id = "test_id",
            title = "Test Session",
            createdAt = 123456789L,
            durationMs = 5000L,
            language = "en",
            engineUsed = "vosk",
            rawText = "hello world",
            wordCount = 2,
            audioFilePath = null,
            notes = "test notes"
        )
        dao.insertSession(session)
        dao.deleteSession("test_id")
        val sessions = dao.getAllSessions().first()
        assertEquals(0, sessions.size)
    }
}
