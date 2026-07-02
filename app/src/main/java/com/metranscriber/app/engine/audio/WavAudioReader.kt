package com.metranscriber.app.engine.audio

import kotlin.math.floor
import kotlin.math.roundToInt

object WavAudioReader {
  private const val TARGET_SAMPLE_RATE = 16000
  private const val CHUNK_SIZE = 1024
  private const val MAX_AUDIO_DATA_BYTES = 25 * 1024 * 1024

  data class DecodedAudio(
    val chunks: List<ShortArray>,
    val durationMs: Long
  )

  fun decode(bytes: ByteArray, maxAudioDataBytes: Int = MAX_AUDIO_DATA_BYTES): DecodedAudio {
    require(bytes.size >= 44) { "WAV file is too small" }
    require(bytes.ascii(0, 4) == "RIFF" && bytes.ascii(8, 4) == "WAVE") { "Only RIFF/WAVE files are supported" }

    var offset = 12
    var channels = 0
    var sampleRate = 0
    var bitsPerSample = 0
    var audioFormat = 0
    var dataOffset = -1
    var dataSize = 0

    while (offset + 8 <= bytes.size) {
      val chunkId = bytes.ascii(offset, 4)
      val chunkSize = bytes.readIntLe(offset + 4)
      val chunkDataOffset = offset + 8
      require(chunkSize >= 0 && chunkDataOffset + chunkSize <= bytes.size) { "Invalid WAV chunk size" }

      when (chunkId) {
        "fmt " -> {
          require(chunkSize >= 16) { "Invalid WAV fmt chunk" }
          audioFormat = bytes.readShortLe(chunkDataOffset).toInt()
          channels = bytes.readShortLe(chunkDataOffset + 2).toInt()
          sampleRate = bytes.readIntLe(chunkDataOffset + 4)
          bitsPerSample = bytes.readShortLe(chunkDataOffset + 14).toInt()
        }
        "data" -> {
          dataOffset = chunkDataOffset
          dataSize = chunkSize
        }
      }

      offset = chunkDataOffset + chunkSize + (chunkSize % 2)
    }

    require(audioFormat == 1) { "Only PCM WAV files are supported" }
    require(channels == 1 || channels == 2) { "Only mono or stereo WAV files are supported" }
    require(sampleRate > 0) { "Invalid WAV sample rate" }
    require(bitsPerSample == 16) { "Only 16-bit WAV files are supported" }
    require(dataOffset >= 0 && dataSize > 0) { "WAV file has no audio data" }
    require(dataSize <= maxAudioDataBytes) { "WAV audio data is too large. Choose a file under 25 MB." }

    val monoSamples = readMonoSamples(bytes, dataOffset, dataSize, channels)
    val durationMs = monoSamples.size * 1000L / sampleRate
    val resampled = resample(monoSamples, sampleRate, TARGET_SAMPLE_RATE)
    return DecodedAudio(
      chunks = resampled.toChunks(CHUNK_SIZE),
      durationMs = durationMs
    )
  }

  private fun readMonoSamples(bytes: ByteArray, dataOffset: Int, dataSize: Int, channels: Int): ShortArray {
    val bytesPerFrame = channels * 2
    val frameCount = dataSize / bytesPerFrame
    return ShortArray(frameCount) { frame ->
      val frameOffset = dataOffset + frame * bytesPerFrame
      if (channels == 1) {
        bytes.readShortLe(frameOffset)
      } else {
        val left = bytes.readShortLe(frameOffset).toInt()
        val right = bytes.readShortLe(frameOffset + 2).toInt()
        ((left + right) / 2).toShort()
      }
    }
  }

  private fun resample(samples: ShortArray, sourceRate: Int, targetRate: Int): ShortArray {
    if (sourceRate == targetRate) return samples
    if (samples.isEmpty()) return samples

    val targetSize = (samples.size * targetRate.toDouble() / sourceRate).roundToInt().coerceAtLeast(1)
    val ratio = sourceRate.toDouble() / targetRate
    return ShortArray(targetSize) { index ->
      val sourcePosition = index * ratio
      val leftIndex = floor(sourcePosition).toInt().coerceIn(0, samples.lastIndex)
      val rightIndex = (leftIndex + 1).coerceAtMost(samples.lastIndex)
      val fraction = sourcePosition - leftIndex
      val value = samples[leftIndex] + (samples[rightIndex] - samples[leftIndex]) * fraction
      value.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
  }

  private fun ShortArray.toChunks(chunkSize: Int): List<ShortArray> {
    if (isEmpty()) return emptyList()
    val chunks = mutableListOf<ShortArray>()
    var offset = 0
    while (offset < size) {
      val end = (offset + chunkSize).coerceAtMost(size)
      chunks += copyOfRange(offset, end)
      offset = end
    }
    return chunks
  }

  private fun ByteArray.ascii(offset: Int, length: Int): String =
    String(this, offset, length, Charsets.US_ASCII)

  private fun ByteArray.readShortLe(offset: Int): Short {
    val low = this[offset].toInt() and 0xff
    val high = (this[offset + 1].toInt() and 0xff) shl 8
    return (low or high).toShort()
  }

  private fun ByteArray.readIntLe(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or
      ((this[offset + 1].toInt() and 0xff) shl 8) or
      ((this[offset + 2].toInt() and 0xff) shl 16) or
      ((this[offset + 3].toInt() and 0xff) shl 24)
  }
}
