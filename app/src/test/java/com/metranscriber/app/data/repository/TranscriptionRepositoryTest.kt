package com.metranscriber.app.data.repository

import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptionRepositoryTest {

  @Test
  fun saveAndGetSessions_worksCorrectly() = runTest {
    val repository = FakeTranscriptionRepository()
    val session = TranscriptionSession(
      id = "session_1",
      title = "Meeting 1",
      createdAt = 123456789L,
      durationMs = 5000L,
      language = "en",
      engineUsed = "vosk",
      rawText = "hello world",
      wordCount = 2,
      audioFilePath = null,
      notes = "notes test"
    )

    repository.saveSession(session)
    val sessions = repository.getSessions().first()
    assertEquals(1, sessions.size)
    assertEquals("Meeting 1", sessions[0].title)

    val fetched = repository.getSessionById("session_1")
    assertNotNull(fetched)
    assertEquals("hello world", fetched?.rawText)
  }

  @Test
  fun deleteSession_removesItFromRepository() = runTest {
    val repository = FakeTranscriptionRepository()
    val session = TranscriptionSession(
      id = "session_1",
      title = "Meeting 1",
      createdAt = 123456789L,
      durationMs = 5000L,
      language = "en",
      engineUsed = "vosk",
      rawText = "hello world",
      wordCount = 2,
      audioFilePath = null,
      notes = "notes test"
    )

    repository.saveSession(session)
    repository.deleteSession("session_1")
    
    val sessions = repository.getSessions().first()
    assertEquals(0, sessions.size)
    
    val fetched = repository.getSessionById("session_1")
    assertNull(fetched)
  }
}
