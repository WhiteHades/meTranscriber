package com.metranscriber.app.engine.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class WavAudioReaderTest {
  @Test
  fun decode_resamplesStereoPcmWavToSixteenKilohertzMonoChunks() {
    val wav = wavBytes(
      sampleRate = 8000,
      channels = 2,
      frames = ShortArray(8000) { (it % 200).toShort() }
    )

    val decoded = WavAudioReader.decode(wav)
    val sampleCount = decoded.chunks.sumOf { it.size }

    assertEquals(1000L, decoded.durationMs)
    assertEquals(16000, sampleCount)
  }

  @Test
  fun decode_rejectsNonWaveInput() {
    assertThrows(IllegalArgumentException::class.java) {
      WavAudioReader.decode(byteArrayOf(1, 2, 3, 4))
    }
  }

  @Test
  fun decode_rejectsAudioDataOverLimit() {
    val wav = wavBytes(frames = ShortArray(64) { 100 })

    assertThrows(IllegalArgumentException::class.java) {
      WavAudioReader.decode(wav, maxAudioDataBytes = 32)
    }
  }

  companion object {
    fun wavBytes(sampleRate: Int = 16000, channels: Int = 1, frames: ShortArray): ByteArray {
      val dataSize = frames.size * channels * 2
      return ByteArrayOutputStream().use { out ->
        out.writeAscii("RIFF")
        out.writeIntLe(36 + dataSize)
        out.writeAscii("WAVE")
        out.writeAscii("fmt ")
        out.writeIntLe(16)
        out.writeShortLe(1)
        out.writeShortLe(channels)
        out.writeIntLe(sampleRate)
        out.writeIntLe(sampleRate * channels * 2)
        out.writeShortLe(channels * 2)
        out.writeShortLe(16)
        out.writeAscii("data")
        out.writeIntLe(dataSize)
        frames.forEach { sample ->
          repeat(channels) {
            out.writeShortLe(sample.toInt())
          }
        }
        out.toByteArray()
      }
    }

    private fun ByteArrayOutputStream.writeAscii(value: String) {
      write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun ByteArrayOutputStream.writeShortLe(value: Int) {
      write(value and 0xff)
      write((value shr 8) and 0xff)
    }

    private fun ByteArrayOutputStream.writeIntLe(value: Int) {
      write(value and 0xff)
      write((value shr 8) and 0xff)
      write((value shr 16) and 0xff)
      write((value shr 24) and 0xff)
    }
  }
}
