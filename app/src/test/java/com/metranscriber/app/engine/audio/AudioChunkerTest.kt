package com.metranscriber.app.engine.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioChunkerTest {
  @Test
  fun chunk_withCorrectLength_producesProperChunks() {
    val chunker = AudioChunker(chunkDurationMs = 2000, overlapMs = 500, sampleRate = 16000)
    // 2000ms chunk = 32000 samples. 500ms overlap = 8000 samples.
    // Total samples for 2 chunks = chunk + (chunk - overlap) = 32000 + 24000 = 56000 samples.
    val audioData = ShortArray(56000) { it.toShort() }
    val chunks = chunker.chunk(audioData)
    
    assertEquals(2, chunks.size)
    assertEquals(32000, chunks[0].size)
    assertEquals(32000, chunks[1].size)
    // Verify overlap values
    // First chunk is index 0..31999
    // Second chunk starts at 24000..55999 (since overlap is 8000 samples)
    assertEquals(24000.toShort(), chunks[1][0])
  }

  @Test
  fun chunk_withShortAudio_returnsEmptyList() {
    val chunker = AudioChunker(chunkDurationMs = 2000, overlapMs = 500, sampleRate = 16000)
    val audioData = ShortArray(10000) { it.toShort() }
    val chunks = chunker.chunk(audioData)
    
    assertEquals(0, chunks.size)
  }
}
