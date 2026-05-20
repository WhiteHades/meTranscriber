package com.metranscriber.app.engine.audio

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAudioRecorder : AudioRecorder {
    private var isRecording = false

    override fun recordStream(): Flow<ShortArray> = flow {
        isRecording = true
        // Emit some dummy audio chunks
        repeat(10) {
            if (!isRecording) return@flow
            emit(ShortArray(1024) { 1000 }) // Constant amplitude
            delay(100)
        }
    }

    override fun isCurrentlyRecording(): Boolean = isRecording

    fun stop() {
        isRecording = false
    }
}
