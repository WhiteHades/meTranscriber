package com.metranscriber.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionSession(
  val id: String,
  val title: String,
  val createdAt: Long,
  val durationMs: Long,
  val language: String,
  val engineUsed: String,
  val rawText: String,
  val wordCount: Int,
  val audioFilePath: String?,
  val notes: String?
)
