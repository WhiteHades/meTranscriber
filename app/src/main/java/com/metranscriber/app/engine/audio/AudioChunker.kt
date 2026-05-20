package com.metranscriber.app.engine.audio

class AudioChunker(
  private val chunkDurationMs: Long = 2000,
  private val overlapMs: Long = 500,
  private val sampleRate: Int = 16000
) {
  private val chunkSize = (chunkDurationMs * sampleRate / 1000).toInt()
  private val overlapSize = (overlapMs * sampleRate / 1000).toInt()
  private val stride = chunkSize - overlapSize

  fun chunk(audioBuffer: ShortArray): List<ShortArray> {
    if (audioBuffer.size < chunkSize) {
      return emptyList()
    }

    val chunks = mutableListOf<ShortArray>()
    var start = 0
    while (start + chunkSize <= audioBuffer.size) {
      val chunk = ShortArray(chunkSize)
      System.arraycopy(audioBuffer, start, chunk, 0, chunkSize)
      chunks.add(chunk)
      start += stride
    }
    return chunks
  }
}
