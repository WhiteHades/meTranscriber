package com.metranscriber.app.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TranscriptionDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(CREATE_SESSIONS_TABLE)
    db.execSQL(CREATE_SEGMENTS_TABLE)
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("DROP TABLE IF EXISTS segments")
    db.execSQL("DROP TABLE IF EXISTS sessions")
    onCreate(db)
  }

  companion object {
    const val DATABASE_NAME = "MeTranscriber.db"
    const val DATABASE_VERSION = 1

    private const val CREATE_SESSIONS_TABLE = """
      CREATE TABLE sessions (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        duration_ms INTEGER NOT NULL,
        language TEXT NOT NULL,
        engine_used TEXT NOT NULL,
        raw_text TEXT NOT NULL,
        word_count INTEGER NOT NULL,
        audio_file_path TEXT,
        notes TEXT
      )
    """

    private const val CREATE_SEGMENTS_TABLE = """
      CREATE TABLE segments (
        id TEXT PRIMARY KEY,
        session_id TEXT NOT NULL,
        timestamp_ms INTEGER NOT NULL,
        text TEXT NOT NULL,
        speaker TEXT,
        confidence REAL NOT NULL,
        is_partial INTEGER NOT NULL,
        FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE
      )
    """
  }
}
