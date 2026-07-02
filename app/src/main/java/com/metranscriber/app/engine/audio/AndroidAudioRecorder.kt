package com.metranscriber.app.engine.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class AndroidAudioRecorder : AudioRecorder {
  private val sampleRate = 16000
  private val channelConfig = AudioFormat.CHANNEL_IN_MONO
  private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
  
  @Volatile
  private var isRecording = false

  @Volatile
  private var currentAudioRecord: AudioRecord? = null

  @SuppressLint("MissingPermission")
  override fun recordStream(): Flow<ShortArray> = callbackFlow {
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
      close(IllegalStateException("failed to get min buffer size"))
      return@callbackFlow
    }

    val recordBufferSize = bufferSize * 2
    val audioRecord = AudioRecord(
      MediaRecorder.AudioSource.MIC,
      sampleRate,
      channelConfig,
      audioFormat,
      recordBufferSize
    )

    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
      audioRecord.release()
      close(IllegalStateException("failed to initialize audiorecord"))
      return@callbackFlow
    }

    try {
      audioRecord.startRecording()
    } catch (e: Exception) {
      audioRecord.release()
      close(IllegalStateException("failed to start audiorecord", e))
      return@callbackFlow
    }

    if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
      audioRecord.release()
      close(IllegalStateException("audiorecord did not start recording"))
      return@callbackFlow
    }

    isRecording = true
    currentAudioRecord = audioRecord

    val buffer = ShortArray(1024)
    val readJob = launch(Dispatchers.IO) {
      try {
        while (isRecording && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
          val read = audioRecord.read(buffer, 0, buffer.size)
          if (read > 0) {
            val chunk = ShortArray(read)
            System.arraycopy(buffer, 0, chunk, 0, read)
            send(chunk)
          } else if (read < 0) {
            if (isRecording) {
              close(IllegalStateException("audiorecord read error: $read"))
            }
            break
          }
        }
        close()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        close(e)
      }
    }

    awaitClose {
      stopRecording()
      if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
        try {
          audioRecord.stop()
        } catch (e: Exception) {
          // ignore
        }
        audioRecord.release()
      }
      if (currentAudioRecord === audioRecord) {
        currentAudioRecord = null
      }
      readJob.cancel()
    }
  }.flowOn(Dispatchers.IO)

  override fun isCurrentlyRecording(): Boolean = isRecording

  override fun stopRecording() {
    isRecording = false
    val audioRecord = currentAudioRecord ?: return
    if (audioRecord.state == AudioRecord.STATE_INITIALIZED && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
      try {
        audioRecord.stop()
      } catch (e: Exception) {
        // ignore; awaitClose still releases the recorder
      }
    }
  }
}
