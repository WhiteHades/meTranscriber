package com.metranscriber.app.engine.audio

import kotlinx.coroutines.flow.Flow

interface AudioRecorder {
    fun recordStream(): Flow<ShortArray>
    fun isCurrentlyRecording(): Boolean
}
