package com.metranscriber.app.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.metranscriber.app.domain.model.TranscriptSegment
import com.metranscriber.app.domain.model.TranscriptionSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteTranscriptionRepository(private val dbHelper: TranscriptionDbHelper) : TranscriptionRepository {

  override suspend fun saveSession(session: TranscriptionSession) = withContext(Dispatchers.IO) {
    val db = dbHelper.writableDatabase
    val values = ContentValues().apply {
      put("id", session.id)
      put("title", session.title)
      put("created_at", session.createdAt)
      put("duration_ms", session.durationMs)
      put("language", session.language)
      put("engine_used", session.engineUsed)
      put("raw_text", session.rawText)
      put("word_count", session.wordCount)
      put("audio_file_path", session.audioFilePath)
      put("notes", session.notes)
    }
    db.replace("sessions", null, values)
    Unit
  }

  override suspend fun getSessions(): List<TranscriptionSession> = withContext(Dispatchers.IO) {
    val db = dbHelper.readableDatabase
    val cursor = db.query(
      "sessions",
      null,
      null,
      null,
      null,
      null,
      "created_at DESC"
    )
    val list = mutableListOf<TranscriptionSession>()
    cursor.use {
      while (it.moveToNext()) {
        list.add(it.toSession())
      }
    }
    list
  }

  override suspend fun getSessionById(id: String): TranscriptionSession? = withContext(Dispatchers.IO) {
    val db = dbHelper.readableDatabase
    val cursor = db.query(
      "sessions",
      null,
      "id = ?",
      arrayOf(id),
      null,
      null,
      null
    )
    cursor.use {
      if (it.moveToFirst()) {
        it.toSession()
      } else {
        null
      }
    }
  }

  override suspend fun deleteSession(id: String) = withContext(Dispatchers.IO) {
    val db = dbHelper.writableDatabase
    db.delete("segments", "session_id = ?", arrayOf(id))
    db.delete("sessions", "id = ?", arrayOf(id))
    Unit
  }

  override suspend fun saveSegments(segments: List<TranscriptSegment>) = withContext(Dispatchers.IO) {
    if (segments.isEmpty()) return@withContext
    val db = dbHelper.writableDatabase
    db.beginTransaction()
    try {
      for (segment in segments) {
        val values = ContentValues().apply {
          put("id", segment.id)
          put("session_id", segment.sessionId)
          put("timestamp_ms", segment.timestampMs)
          put("text", segment.text)
          put("speaker", segment.speaker)
          put("confidence", segment.confidence)
          put("is_partial", if (segment.isPartial) 1 else 0)
        }
        db.replace("segments", null, values)
      }
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  override suspend fun getSegmentsForSession(sessionId: String): List<TranscriptSegment> = withContext(Dispatchers.IO) {
    val db = dbHelper.readableDatabase
    val cursor = db.query(
      "segments",
      null,
      "session_id = ?",
      arrayOf(sessionId),
      null,
      null,
      "timestamp_ms ASC"
    )
    val list = mutableListOf<TranscriptSegment>()
    cursor.use {
      while (it.moveToNext()) {
        list.add(it.toSegment())
      }
    }
    list
  }

  private fun Cursor.toSession(): TranscriptionSession {
    return TranscriptionSession(
      id = getString(getColumnIndexOrThrow("id")),
      title = getString(getColumnIndexOrThrow("title")),
      createdAt = getLong(getColumnIndexOrThrow("created_at")),
      durationMs = getLong(getColumnIndexOrThrow("duration_ms")),
      language = getString(getColumnIndexOrThrow("language")),
      engineUsed = getString(getColumnIndexOrThrow("engine_used")),
      rawText = getString(getColumnIndexOrThrow("raw_text")),
      wordCount = getInt(getColumnIndexOrThrow("word_count")),
      audioFilePath = getString(getColumnIndexOrThrow("audio_file_path")),
      notes = getString(getColumnIndexOrThrow("notes"))
    )
  }

  private fun Cursor.toSegment(): TranscriptSegment {
    return TranscriptSegment(
      id = getString(getColumnIndexOrThrow("id")),
      sessionId = getString(getColumnIndexOrThrow("session_id")),
      timestampMs = getLong(getColumnIndexOrThrow("timestamp_ms")),
      text = getString(getColumnIndexOrThrow("text")),
      speaker = getString(getColumnIndexOrThrow("speaker")),
      confidence = getFloat(getColumnIndexOrThrow("confidence")),
      isPartial = getInt(getColumnIndexOrThrow("is_partial")) == 1
    )
  }
}
