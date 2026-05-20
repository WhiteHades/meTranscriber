package com.metranscriber.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptSegment(
  val id: String,
  val sessionId: String,
  val timestampMs: Long,
  val text: String,
  val speaker: String?,
  val confidence: Float,
  val isPartial: Boolean
)
