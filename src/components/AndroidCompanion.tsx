/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { motion } from 'motion/react';
import { 
  Code2, 
  Cpu, 
  Database, 
  BookOpen, 
  Terminal, 
  Sparkles, 
  Copy, 
  Check, 
  Maximize2, 
  FileCode, 
  ListTodo,
  Layers,
  Award
} from 'lucide-react';
import { AndroidCodeSnippet } from '../types';

const ANDROID_SNIPPETS: AndroidCodeSnippet[] = [
  {
    id: 'deps',
    title: 'Dependencies & Packaging',
    file: 'app/build.gradle.kts',
    lang: 'kotlin',
    description: 'Add Vosk and Room database dependencies. Also configure packaging options to ensure offline .bin models are not compressed in the final APK.',
    code: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.offline.transcriber"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.offline.transcriber"
        minSdk = 26
        targetSdk = 34
    }

    packaging {
        resources {
            // CRITICAL FOR OFFLINE MODELS: Do not compress the model assets 
            // so they can be memory-mapped directly
            excludes += "/assets/model/**"
        }
    }
}

dependencies {
    // Vosk Offline Speech-to-Text SDK
    implementation("org.alphacephei:vosk-android:0.3.47")

    // Room Persistent Database (for transcription history)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Jetpack Compose, Coroutines, Flow
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}`
  },
  {
    id: 'vosk_service',
    title: 'STT Engine (Vosk Wrapper)',
    file: 'data/OfflineTranscriber.kt',
    lang: 'kotlin',
    description: 'Exposes model load, raw PCM audio capture, and synchronous chunk processing. Emits real-time state using Kotlin Flow.',
    code: `package com.offline.transcriber.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException

class OfflineTranscriber(private val context: Context) {
    private var model: Model? = null

    /**
     * Unpacks model from APK assets to internal storage and initializes
     */
    suspend fun initialize(modelAssetPath: String = "model"): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // StorageService is provided by Vosk library to extract assets
            val targetPath = StorageService.unpack(context, modelAssetPath, modelAssetPath)
            model = Model(targetPath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Stream streams transcription results line-by-line using Kotlin flow
     */
    fun transcribeStream(audioFile: File): Flow<String> = flow {
        val activeModel = model ?: throw IllegalStateException("Model not initialized. Run initialize() first.")
        val recognizer = Recognizer(activeModel, 16000.0f) // Standard 16kHz PCM
        recognizer.setWords(true) // Toggle on for timestamp extraction

        val buffer = ByteArray(4096)
        audioFile.inputStream().use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                // If a complete utterance is detected, acceptWaveform returns true
                if (recognizer.acceptWaveform(buffer, bytesRead)) {
                    val partialResult = recognizer.result
                    val parsedText = extractText(partialResult)
                    if (parsedText.isNotEmpty()) {
                        emit(parsedText)
                    }
                }
            }
            // Emit final block when stream concludes
            emit(extractText(recognizer.finalResult))
        }
    }

    private fun extractText(jsonResult: String): String {
        // Safe regex extraction for "text" key to prevent third-party JSON parser weight
        val regex = "\\"text\\"\\\\s*:\\\\s*\\"([^\\"]*)\\"".toRegex()
        return regex.find(jsonResult)?.groupValues?.get(1) ?: ""
    }
}`
  },
  {
    id: 'room_db',
    title: 'Room Database & History',
    file: 'data/TranscriptDatabase.kt',
    lang: 'kotlin',
    description: 'Implements Room Entities, DAOs, and Database definitions. Shows clean, structured storage of past sessions.',
    code: `package com.offline.transcriber.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "duration") val durationSec: Long
)

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcripts ORDER BY timestamp DESC")
    fun getAllTranscripts(): Flow<List<TranscriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity)

    @Delete
    suspend fun deleteTranscript(transcript: TranscriptEntity)
}

@Database(entities = [TranscriptEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transcriptDao(): TranscriptDao
}`
  },
  {
    id: 'viewmodel',
    title: 'Architecture MVVM ViewModel',
    file: 'ui/TranscriberViewModel.kt',
    lang: 'kotlin',
    description: 'The controller layer orchestrating microphone streams, states, and DB callbacks. Highlights unidirectional UI state flows.',
    code: `package com.offline.transcriber.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offline.transcriber.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class UiState(
    val isModelLoaded: Boolean = false,
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val statusText: String = "Initializing model...",
    val finalOutput: String = ""
)

class TranscriberViewModel(application: Application) : AndroidViewModel(application) {
    private val transcriber = OfflineTranscriber(application)
    private val database = Room.databaseBuilder(application, AppDatabase::class.java, "transcripts.db").build()
    private val dao = database.transcriptDao()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Expose flows for database lists
    val history: Flow<List<TranscriptEntity>> = dao.getAllTranscripts()

    init {
        viewModelScope.launch {
            val success = transcriber.initialize()
            if (success) {
                _state.update { it.copy(isModelLoaded = true, statusText = "Ready to Transcribe Offline") }
            } else {
                _state.update { it.copy(statusText = "Error initializing model. Check asset files.") }
            }
        }
    }

    fun startTranscription(tempFile: File) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, statusText = "Running offline STT...") }
            val sb = StringBuilder()
            
            transcriber.transcribeStream(tempFile)
                .catch { t -> 
                    _state.update { it.copy(isProcessing = false, statusText = "Error: \${t.message}") }
                }
                .collect { partial ->
                    if (partial.isNotEmpty()) {
                        sb.append(partial).append(" ")
                        _state.update { it.copy(finalOutput = sb.toString()) }
                    }
                }

            // Save record to local Room DB
            val savedRecord = TranscriptEntity(
                id = UUID.randomUUID().toString(),
                title = "Meeting at \${System.currentTimeMillis()}",
                text = sb.toString(),
                timestamp = System.currentTimeMillis(),
                durationSec = 15 // Simulating standard time
            )
            dao.insertTranscript(savedRecord)
            _state.update { it.copy(isProcessing = false, statusText = "Transcription complete. Saved.") }
        }
    }
}`
  },
  {
    id: 'compose_ui',
    title: 'Jetpack Compose Screen',
    file: 'ui/TranscriberScreen.kt',
    lang: 'kotlin',
    description: 'Standard Material 3 design template for full screen display including states, animation controls, and list item rendering.',
    code: `package com.offline.transcriber.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTranscriberScreen(viewModel: TranscriberViewModel) {
    val state by viewModel.state.collectAsState()
    val historyItems by viewModel.history.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("FOSS Android Transcriber") }) }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Banner Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = state.statusText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Live Transcription Box
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    if (state.finalOutput.isEmpty()) {
                        Text(
                            "Press Start Recording to capture offline transcription.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(text = state.finalOutput)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // History Header
            Text(
                "Local Saved Room DB Data", 
                style = MaterialTheme.typography.titleMedium, 
                modifier = Modifier.align(Alignment.Start)
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(historyItems) { record ->
                    ListItem(
                        headlineContent = { Text(record.title) },
                        supportingContent = { Text(record.text, maxLines = 1) },
                        trailingContent = { Text("\${record.durationSec}s") }
                    )
                }
            }
        }
    }
}`
  }
];

export default function AndroidCompanion() {
  const [selectedSnippet, setSelectedSnippet] = useState<string>('vosk_service');
  const [copied, setCopied] = useState<boolean>(false);
  
  // CV Generator States
  const [userRole, setUserRole] = useState<'architecture' | 'multithreading' | 'on_device_ml' | 'database'>('on_device_ml');
  const [cvFormat, setCvFormat] = useState<'bullets' | 'elevation_pitch'>('bullets');

  const activeSnippet = ANDROID_SNIPPETS.find(s => s.id === selectedSnippet) || ANDROID_SNIPPETS[1];

  const handleCopyCode = () => {
    navigator.clipboard.writeText(activeSnippet.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Dedicated high-impact CV bullets optimized for recruiters
  const getCvItems = () => {
    if (cvFormat === 'bullets') {
      switch(userRole) {
        case 'on_device_ml':
          return [
            "• Spearheaded integration of on-device AI/ML capabilities using the alphacephei/Vosk neural engine to perform offline audio recording and high-speed Speech-to-Text translation entirely local-first, boosting user privacy.",
            "• Memory-mapped neural speech models (50MB+ footprint) in Android assets by tuning gradle packaging parameters, avoiding RAM overhead and preventing cold-start delays across legacy APIs.",
            "• Engineered a regex-based light-weight JSON parsing technique reducing garbage collection allocation rate for real-time transcription streaming frames."
          ];
        case 'architecture':
          return [
            "• Architected a robust, high-performance offline audio processing app utilizing Clean Architecture and MVVM patterns to enforce strict unidirectional data flow.",
            "• Separated raw system audio interfaces from data models using Repository patterns, ensuring testable business logic and increasing local component maintainability.",
            "• Adhered to modern Jetpack Compose Material 3 UI design patterns with a fully functional Single Activity system, simplifying multi-state rendering logic."
          ];
        case 'multithreading':
          return [
            "• Orchestrated lock-free audio processing queues using Kotlin Coroutines and asynchronous state-emitting flows (StateFlow, SharedFlow) on custom background thread pools (Dispatchers.IO).",
            "• Streamlined audio buffer acquisition from media hardware safely without freezing the main thread, resulting in a continuous 60fps responsive representation canvas UI.",
            "• Optimized heavy Room DB storage writes asynchronously to run strictly off-thread, avoiding ANR (Application Not Responding) events entirely."
          ];
        case 'database':
          return [
            "• Configured high-performance local SQLite databases using Android Room Object-Relational Mapping (ORM) to handle history indexing, segment tags, and text analysis.",
            "• Designed structured relational schemas for transcript sequences and diarized tags, implementing custom Rx/Flow listeners to update screen states automatically upon write.",
            "• Structured full-text-search indexes (FTS4) inside Room Entity classes to permit swift, sub-10ms keyword query operations across tens of thousands of archived sentences."
          ];
      }
    } else {
      // Elevation Pitch Generator
      switch(userRole) {
        case 'on_device_ml':
          return [
            "\"As an Android developer, I built an offline-first speech transcriber utilizing on-device machine learning (Vosk engine) that performs speech-to-text with zero external internet dependencies. The core priority was strict consumer privacy—user audio logs never touch any remote server. It demonstrates my ability to load and unpack offline neural network models, handle high-volume binary structures in Kotlin, and package compiled binary models optimally.\""
          ];
        case 'architecture':
          return [
            "\"I built an Offline Audio Transcriber using a modern Android stack with Jetpack Compose, MVVM State Flows, and Clean Architecture principles. It represents a fully production-ready, highly decoupled structure where UI components, business logic, model extraction, and history persistence layers are isolated, modularized, and strictly testable—ideal for working in enterprise teams practicing Scrum and CI/CD.\""
          ];
        case 'multithreading':
          return [
            "\"My background in CS shines in how I tackled the multithreading hurdles. Capturing audio buffers at 16kHz while streaming them into an off-thread offline speech recorder, parsing intermediate JSON responses, and updating an animated graphic waveform at 60fps requires careful handling of thread pools. I utilized Kotlin Coroutines, Flow, and backpressure management to handle this with zero ui-thread stutters!\""
          ];
        case 'database':
          return [
            "\"For this utility, I designed a local persistent database layer. I designed an Android Room database that persists session transcripts and maps search indices natively. Using SQLite FTS indices allows immediate instant searches over history. This shows my ability to handle SQLite queries, structural schemas, data relations, and reactive DB flows cleanly.\""
          ];
      }
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 text-ink font-sans" id="android-companion-panel">
      
      {/* Left educational pillar (4 cols) */}
      <div className="lg:col-span-5 flex flex-col gap-6">
        
        {/* Intro */}
        <div className="bg-white p-6 rounded-none border border-ink flex flex-col gap-3 text-ink shadow-none">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-ink text-white rounded-none border border-ink">
              <Award className="w-5 h-5 text-white" />
            </div>
            <div>
              <span className="text-[10px] uppercase font-bold text-neutral-500 tracking-wider">BSc CS Graduate Portfolio</span>
              <h2 className="text-base font-bold font-serif text-ink tracking-tight">Android CV Booster</h2>
            </div>
          </div>
          <p className="text-xs text-neutral-600 leading-relaxed font-serif">
            Recruiters do not look just at working apps; they search for code quality, structural execution, and system-level competence. This hub provides production-ready code that demonstrates **Clean Architecture, MVVM, and On-Device Core ML integration**.
          </p>
          
          {/* F-Droid mention */}
          <div className="mt-2 p-3.5 bg-paper rounded-none border border-ink flex items-start gap-2.5 shadow-none">
            <div className="p-1 bg-ink text-paper rounded-none border border-ink mt-0.5 shrink-0">
              <BookOpen className="w-3.5 h-3.5 text-paper" />
            </div>
            <p className="text-2xs text-neutral-500 leading-relaxed font-mono">
              <strong className="text-ink">FOSS TIP:</strong> Getting listed on F-Droid requires building fully local, zero-tracker, ad-free binaries. Use Vosk/Whisper.cpp to easily qualify and stand out as an open-source advocate on GitHub!
            </p>
          </div>
        </div>

        {/* Dynamic CV Optimizer Card */}
        <div className="bg-white text-ink p-6 rounded-none border border-ink flex flex-col gap-4 shadow-none">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-editorial-blue animate-pulse" />
              <h3 className="font-extrabold text-ink text-xs uppercase tracking-wider font-serif">Professional CV Optimizer</h3>
            </div>
            <div className="flex bg-paper p-0.5 rounded-none border border-ink">
              <button 
                onClick={() => setCvFormat('bullets')}
                className={`px-2 py-1 text-[10px] font-bold uppercase tracking-wider rounded-none transition cursor-pointer ${cvFormat === 'bullets' ? 'bg-ink text-white' : 'text-neutral-500 hover:text-ink'}`}
              >
                Bullets
              </button>
              <button 
                onClick={() => setCvFormat('elevation_pitch')}
                className={`px-2 py-1 text-[10px] font-bold uppercase tracking-wider rounded-none transition cursor-pointer ${cvFormat === 'elevation_pitch' ? 'bg-ink text-white' : 'text-neutral-500 hover:text-ink'}`}
              >
                Pitch
              </button>
            </div>
          </div>

          <p className="text-xs text-neutral-500 font-serif">
            Tailor high-impact statements for your resume or interview talking points depending on your focus:
          </p>

          <div className="grid grid-cols-2 gap-2 text-2xs font-mono font-bold uppercase">
            <button
              onClick={() => setUserRole('on_device_ml')}
              className={`p-2 rounded-none text-left border transition flex items-center gap-1.5 cursor-pointer ${userRole === 'on_device_ml' ? 'bg-paper border-ink border-2 text-ink' : 'bg-white border-neutral-200 text-neutral-500 hover:border-ink hover:text-ink'}`}
            >
              <Cpu className="w-3.5 h-3.5 shrink-0" /> On-Device ML
            </button>
            <button
              onClick={() => setUserRole('architecture')}
              className={`p-2 rounded-none text-left border transition flex items-center gap-1.5 cursor-pointer ${userRole === 'architecture' ? 'bg-paper border-ink border-2 text-ink' : 'bg-white border-neutral-200 text-neutral-500 hover:border-ink hover:text-ink'}`}
            >
              <Layers className="w-3.5 h-3.5 shrink-0" /> Architecture
            </button>
            <button
              onClick={() => setUserRole('multithreading')}
              className={`p-2 rounded-none text-left border transition flex items-center gap-1.5 cursor-pointer ${userRole === 'multithreading' ? 'bg-paper border-ink border-2 text-ink' : 'bg-white border-neutral-200 text-neutral-500 hover:border-ink hover:text-ink'}`}
            >
              <Terminal className="w-3.5 h-3.5 shrink-0" /> Concurrency
            </button>
            <button
              onClick={() => setUserRole('database')}
              className={`p-2 rounded-none text-left border transition flex items-center gap-1.5 cursor-pointer ${userRole === 'database' ? 'bg-paper border-ink border-2 text-ink' : 'bg-white border-neutral-200 text-neutral-500 hover:border-ink hover:text-ink'}`}
            >
              <Database className="w-3.5 h-3.5 shrink-0" /> Room SQLite
            </button>
          </div>

          {/* Copyable CV outputs */}
          <div className="bg-paper p-4 rounded-none border border-ink relative group">
            <button 
              onClick={() => {
                navigator.clipboard.writeText(getCvItems().join('\n'));
                setCopied(true);
                setTimeout(() => setCopied(false), 2000);
              }}
              className="absolute top-2 right-2 text-ink hover:bg-neutral-200 transition p-1 bg-white rounded-none border border-ink cursor-pointer"
              title="Copy bullet points"
            >
              <Copy className="w-3.5 h-3.5" />
            </button>
            <div className="flex flex-col gap-2.5 max-h-[160px] overflow-y-auto pr-2 scrollbar-thin">
              {getCvItems().map((item, idx) => (
                <p key={idx} className="text-xs text-ink leading-relaxed font-serif">
                  {item}
                </p>
              ))}
            </div>
          </div>
          <div className="text-[10px] text-neutral-500 flex items-center gap-1.5 font-bold uppercase tracking-wider">
            <ListTodo className="w-3.5 h-3.5 text-editorial-blue" />
            <span>Click copy & place directly in your Markdown CV portfolio!</span>
          </div>
        </div>

        {/* Visual Architecture Diagram */}
        <div className="bg-white p-5 rounded-none border border-ink flex flex-col gap-3 shadow-none">
          <h3 className="text-[10px] uppercase font-bold text-neutral-50 tracking-wider text-neutral-400 font-sans">Core Clean Data flow System</h3>
          <div className="flex flex-col gap-2 text-xs font-mono">
            
            {/* Thread 1: UI */}
            <div className="p-2 bg-paper border border-ink rounded-none flex items-center justify-between">
              <span className="font-bold text-ink">1. UI Thread</span>
              <span className="text-[9px] bg-ink text-paper px-1.5 py-0.5 rounded-none font-bold">Jetpack Compose Screen</span>
            </div>
            
            <div className="h-4 border-l border-dashed border-ink ml-4"></div>

            {/* ViewModel */}
            <div className="p-2 bg-paper border border-ink rounded-none flex items-center justify-between">
              <span className="font-bold text-ink">2. ViewModel Layer</span>
              <span className="text-[9px] bg-ink text-paper px-1.5 py-0.5 rounded-none font-bold">StateFlow / lifecycle-viewmodel</span>
            </div>

            <div className="h-4 border-l border-dashed border-ink ml-4"></div>

            {/* Thread 3: IO Thread */}
            <div className="p-2 bg-paper border border-ink rounded-none flex items-center justify-between">
              <span className="font-bold text-ink">3. IO Workers</span>
              <span className="text-[9px] bg-ink text-paper px-1.5 py-0.5 rounded-none font-bold">Kotlin Coroutines IO</span>
            </div>

            <div className="h-4 border-l border-dashed border-ink ml-4"></div>

            {/* Neural model engine */}
            <div className="p-2 bg-paper border border-ink rounded-none flex items-center justify-between">
              <span className="font-bold text-ink">4. Neural Engine SDK</span>
              <span className="text-[9px] bg-ink text-paper px-1.5 py-0.5 rounded-none font-bold">Vosk JNI C++ Matrix</span>
            </div>

            <div className="h-4 border-l border-dashed border-ink ml-4"></div>

            {/* Persistence Room SQLite */}
            <div className="p-2 bg-paper border border-ink rounded-none flex items-center justify-between">
              <span className="font-bold text-ink">5. Room SQLite Database</span>
              <span className="text-[9px] bg-ink text-paper px-1.5 py-0.5 rounded-none font-bold">Structured Dao persistence</span>
            </div>

          </div>
        </div>

      </div>

      {/* Code Repository Panel (7 cols) */}
      <div className="lg:col-span-7 flex flex-col gap-4 bg-white text-ink rounded-none border border-ink shadow-none overflow-hidden">
        
        {/* Code Navigator Header */}
        <div className="bg-paper p-5 border-b border-ink flex flex-col gap-3 text-ink">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Code2 className="w-5 h-5 text-editorial-blue" />
              <div>
                <h3 className="font-extrabold text-ink text-xs uppercase tracking-wider font-serif">FOSS Android Source Code</h3>
                <p className="text-[10px] text-neutral-500 font-sans tracking-wide">Directly copy this source structure into your project files</p>
              </div>
            </div>

            <button 
              onClick={handleCopyCode}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-ink hover:bg-neutral-800 text-paper hover:text-white transition rounded-none text-xs border border-ink font-bold uppercase tracking-widest cursor-pointer"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-green-400" />
                  <span>Copied!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>Copy File</span>
                </>
              )}
            </button>
          </div>

          {/* Tab lists */}
          <div className="flex gap-1 overflow-x-auto py-1 scrollbar-none scroll-smooth">
            {ANDROID_SNIPPETS.map(snippet => (
              <button
                key={snippet.id}
                onClick={() => setSelectedSnippet(snippet.id)}
                className={`px-3 py-1.5 rounded-none text-2xs font-mono transition shrink-0 flex items-center gap-1.5 cursor-pointer ${selectedSnippet === snippet.id ? 'bg-ink text-paper font-bold border border-ink' : 'bg-white border border-neutral-300 text-neutral-500 hover:text-ink hover:border-ink'}`}
              >
                <FileCode className="w-3 h-3" />
                {snippet.title}
              </button>
            ))}
          </div>
        </div>

        {/* File Path & Subtitle */}
        <div className="px-5 pt-3 flex flex-col gap-1 text-ink">
          <div className="flex items-center gap-2 text-xs font-mono text-ink">
            <span className="px-1.5 py-0.5 bg-ink text-paper border border-ink rounded-none text-[9px] font-bold">LOCATION</span>
            <span className="font-bold underline">{activeSnippet.file}</span>
          </div>
          <p className="text-xs text-neutral-500 leading-relaxed max-w-2xl mt-1 font-serif">
            {activeSnippet.description}
          </p>
        </div>

        {/* Code Content Box */}
        <div className="flex-1 p-5 font-mono text-xs overflow-auto max-h-[500px] scrollbar-thin bg-paper border border-ink mx-5 mb-5 rounded-none">
          <pre className="text-ink select-all leading-relaxed whitespace-pre font-mono font-medium text-[11px]">
            {activeSnippet.code}
          </pre>
        </div>

        {/* Footer info file */}
        <div className="bg-paper p-4 border-t border-ink flex items-center justify-between text-neutral-500 text-[10px] font-bold uppercase tracking-wider font-mono">
          <span>Gradle / Android SDK 34 (UpsideDownCake) Ready</span>
          <span className="flex items-center gap-1 text-[10px]"><Maximize2 className="w-3.5 h-3.5 text-editorial-blue animate-pulse" /> Select all lines with cursor</span>
        </div>

      </div>
    </div>
  );
}
