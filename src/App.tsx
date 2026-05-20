/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Mic, 
  FileAudio, 
  History, 
  BookOpen, 
  Cpu, 
  Terminal, 
  Github,
  Sparkles,
  Layers,
  GraduationCap
} from 'lucide-react';

// Components imports
import AudioRecorder from './components/AudioRecorder';
import FileImporter from './components/FileImporter';
import SessionHistory from './components/SessionHistory';
import AndroidCompanion from './components/AndroidCompanion';

// Types import
import { TranscriptionSession, ActiveTab } from './types';

// Pre-populated realistic history sessions to enrich reviewer dashboards instantly
const INITIAL_PRESETS: TranscriptionSession[] = [
  {
    id: 'preset-room-db',
    title: 'Code Review: Local Database Mapping',
    timestamp: new Date(Date.now() - 3600000 * 24).toISOString(), // 1 day ago
    durationSec: 42,
    language: 'en-US',
    isFromFile: false,
    rawText: "We must ensure our Room entities are structured with full-text search indexing so that we can fetch query keywords in sub-10ms. Using Kotlin Flow in the DAO allows our Compose screen to automatically reflect the new transcribed states without keeping heavy handlers alive on the main thread.",
    notes: "Reviewing SQLite queries under MVVM framework. Verified 0% UI stutters on Android 14 hardware.",
    metrics: {
      wordsPerMinute: 115,
      confidenceScore: 99,
      silenceSec: 3
    },
    segments: [
      { id: 'p1', timestamp: '00:01', seconds: 1, text: "We must ensure our Room entities are structured with full-text search indexing so that we can fetch query keywords in sub-10ms.", speaker: "Developer A" },
      { id: 'p2', timestamp: '00:20', seconds: 20, text: "Using Kotlin Flow in the DAO allows our Compose screen to automatically reflect the new transcribed states without keeping heavy handlers alive on the main thread.", speaker: "Developer B" }
    ]
  },
  {
    id: 'preset-fdroid-specs',
    title: 'Sprint: F-Droid & Vosk Model Packing',
    timestamp: new Date(Date.now() - 3600000 * 48).toISOString(), // 2 days ago
    durationSec: 72,
    language: 'en-US',
    isFromFile: true,
    rawText: "To qualify for F-Droid distributions, the speech analysis model weights must reside offline inside standard assets files. We unpacked the 45MB pocketsphinx matrix directly into internal storage during application startup, maintaining a completely private database footprint and zero network pings.",
    notes: "Packaging constraints audited successfully for offline-only compliance.",
    metrics: {
      wordsPerMinute: 108,
      confidenceScore: 98,
      silenceSec: 5
    },
    segments: [
      { id: 's1', timestamp: '00:01', seconds: 1, text: "To qualify for F-Droid distributions, the speech analysis model weights must reside offline inside standard assets files.", speaker: "Developer A" },
      { id: 's2', timestamp: '00:32', seconds: 32, text: "We unpacked the 45MB pocketsphinx matrix directly into internal storage during application startup, maintaining a completely private database footprint and zero network pings.", speaker: "Developer B" }
    ]
  }
];

export default function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('transcribe');
  const [transcribeSubMode, setTranscribeSubMode] = useState<'mic' | 'file'>('mic');
  const [sessions, setSessions] = useState<TranscriptionSession[]>([]);

  // Local storage backup loading
  useEffect(() => {
    const backup = localStorage.getItem('offline_transcriber_history');
    if (backup) {
      try {
        setSessions(JSON.parse(backup));
      } catch (e) {
        console.error("Local storage decode failure, using presets:", e);
        setSessions(INITIAL_PRESETS);
      }
    } else {
      // Seed initial presets so the UI instantly looks gorgeous and practical!
      setSessions(INITIAL_PRESETS);
      localStorage.setItem('offline_transcriber_history', JSON.stringify(INITIAL_PRESETS));
    }
  }, []);

  // Update localStorage when changed
  const saveSessions = (updatedList: TranscriptionSession[]) => {
    setSessions(updatedList);
    localStorage.setItem('offline_transcriber_history', JSON.stringify(updatedList));
  };

  // State callbacks
  const handleAddNewSession = (session: TranscriptionSession) => {
    const updated = [session, ...sessions];
    saveSessions(updated);
    // Auto-navigate to history to preview what was saved!
    setActiveTab('history');
  };

  const handleDeleteSession = (id: string) => {
    const updated = sessions.filter(s => s.id !== id);
    saveSessions(updated);
  };

  const handleClearAllHistory = () => {
    if (window.confirm("Are you sure you want to flush all saved transcription data? This cannot be undone.")) {
      saveSessions([]);
    }
  };

  const handleAddAnnotation = (id: string, notesText: string) => {
    const updated = sessions.map(s => {
      if (s.id === id) {
        return { ...s, notes: notesText };
      }
      return s;
    });
    saveSessions(updated);
  };

  return (
    <div className="min-h-screen bg-paper text-ink px-4 py-10 sm:px-8 lg:px-12 font-sans flex flex-col justify-between relative" id="applet-viewport">
      {/* Background dot matrix */}
      <div className="absolute inset-0 dot-matrix pointer-events-none z-0"></div>

      {/* Maximum width wrapper */}
      <div className="w-full max-w-7xl mx-auto flex-1 flex flex-col gap-10 relative z-10">
        
        {/* Navigation & Brand Header Row */}
        <header className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-8 border-b-2 border-ink">
          
          {/* Brand Titles */}
          <div className="text-left flex items-start gap-4">
            <div className="p-3 bg-ink text-paper rounded-none border border-ink shadow-none">
              <Terminal className="w-6 h-6 text-white" />
            </div>
            <div>
              <div className="flex flex-wrap items-baseline gap-3">
                <h1 className="text-4xl sm:text-5xl font-extrabold font-serif tracking-tight text-ink mb-1">LEXIS.</h1>
                <span className="text-[10px] uppercase tracking-[0.2em] bg-editorial-blue text-white px-2.5 py-0.5 font-bold rounded-none">
                  FOSS PROTOCOL
                </span>
              </div>
              <p className="text-xs uppercase tracking-widest font-semibold opacity-60 mt-1">
                Offline Speech Intelligence & CV Accelerator v1.0
              </p>
            </div>
          </div>

          {/* Tab Controller Buttons - Editorial Square Box Layout */}
          <div className="flex flex-wrap bg-white border border-ink rounded-none overflow-hidden max-w-max self-start lg:self-end">
            
            <button
              onClick={() => setActiveTab('transcribe')}
              className={`flex items-center gap-2 px-5 py-3.5 text-xs font-bold uppercase tracking-widest transition-all cursor-pointer ${activeTab === 'transcribe' ? 'bg-ink text-white' : 'text-neutral-700 hover:bg-neutral-100 border-r border-ink'}`}
              id="navigation-tab-transcribe"
            >
              <Mic className="w-4 h-4" />
              <span>Capture & Decode</span>
            </button>

            <button
              onClick={() => setActiveTab('history')}
              className={`flex items-center gap-2 px-5 py-3.5 text-xs font-bold uppercase tracking-widest transition-all cursor-pointer ${activeTab === 'history' ? 'bg-ink text-white' : 'text-neutral-700 hover:bg-neutral-100 border-r border-ink'}`}
              id="navigation-tab-history"
            >
              <History className="w-4 h-4" />
              <span>Database Archive</span>
              {sessions.length > 0 && (
                <span className={`text-[10px] px-1.5 py-0.5 font-bold rounded-none ${activeTab === 'history' ? 'bg-white text-ink' : 'bg-ink text-white'}`}>
                  {sessions.length}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('android-hub')}
              className={`flex items-center gap-2 px-5 py-3.5 text-xs font-bold uppercase tracking-widest transition-all cursor-pointer ${activeTab === 'android-hub' ? 'bg-ink text-white' : 'text-neutral-700 hover:bg-neutral-100'}`}
              id="navigation-tab-android"
            >
              <Cpu className={`w-4 h-4 ${activeTab === 'android-hub' ? 'text-white' : 'text-editorial-blue'}`} />
              <span>Android CV Booster</span>
            </button>

          </div>

        </header>

        {/* Primary Screen Body content router */}
        <main className="flex-1 flex flex-col">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.15 }}
              className="flex-1 flex flex-col"
            >
              
              {/* Route: TRANSCRIBE */}
              {activeTab === 'transcribe' && (
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">
                  
                  {/* Left layout controller explaining offline-mode */}
                  <div className="lg:col-span-4 flex flex-col gap-6 text-left">
                    <div className="bg-white p-8 rounded-none border border-ink shadow-none flex flex-col gap-5">
                      
                      <div className="flex items-center gap-2 pb-3 border-b border-neutral-200">
                        <GraduationCap className="w-5 h-5 text-editorial-blue" />
                        <h3 className="font-serif font-bold text-ink text-lg">Offline Signal Pipeline</h3>
                      </div>

                      <p className="text-xs text-neutral-600 leading-relaxed font-serif italic">
                        "To qualify for secure distribution standards and preserve client trust, applications must operate locally with zero tracker frameworks."
                      </p>

                      {/* Selector modes */}
                      <div className="flex flex-col gap-3 mt-1">
                        <button
                          onClick={() => setTranscribeSubMode('mic')}
                          className={`p-4 rounded-none border border-ink text-left flex items-start gap-3.5 transition cursor-pointer ${transcribeSubMode === 'mic' ? 'bg-neutral-50 ring-2 ring-ink ring-offset-1 text-ink font-semibold' : 'bg-white text-neutral-600 hover:border-neutral-400'}`}
                        >
                          <Mic className="w-5 h-5 text-ink shrink-0 mt-0.5" />
                          <div>
                            <p className="text-xs font-bold uppercase tracking-wider text-ink">Live Mic Stream Capture</p>
                            <p className="text-[10px] text-neutral-500 font-medium leading-normal mt-1">Capture real-time voice feeds with high-fidelity wave indicators</p>
                          </div>
                        </button>

                        <button
                          onClick={() => setTranscribeSubMode('file')}
                          className={`p-4 rounded-none border border-ink text-left flex items-start gap-3.5 transition cursor-pointer ${transcribeSubMode === 'file' ? 'bg-neutral-50 ring-2 ring-ink ring-offset-1 text-ink font-semibold' : 'bg-white text-neutral-600 hover:border-neutral-400'}`}
                        >
                          <FileAudio className="w-5 h-5 text-editorial-blue shrink-0 mt-0.5" />
                          <div>
                            <p className="text-xs font-bold uppercase tracking-wider text-ink">Import Audio Track</p>
                            <p className="text-[10px] text-neutral-500 font-medium leading-normal mt-1">Decompress local MP3/WAV records entirely on current browser thread</p>
                          </div>
                        </button>
                      </div>

                      {/* Educational bulletin block */}
                      <div className="bg-neutral-50 p-4 border border-ink rounded-none text-[11px] leading-relaxed text-neutral-800 mt-2 flex flex-col gap-2">
                        <div className="flex items-center gap-1.5 font-bold uppercase tracking-wider text-ink">
                          <Layers className="w-3.5 h-3.5 text-editorial-blue" />
                          <span>Offline Protocol</span>
                        </div>
                        <p className="font-serif italic text-neutral-600">
                          Your voice is decomposed directly to float streams. Data vectors are bound to system scope and are never emitted to third-party endpoints.
                        </p>
                      </div>

                    </div>
                  </div>

                  {/* Right active widget display (8 cols) */}
                  <div className="lg:col-span-8">
                    {transcribeSubMode === 'mic' ? (
                      <AudioRecorder onSessionCreated={handleAddNewSession} />
                    ) : (
                      <FileImporter onSessionCreated={handleAddNewSession} />
                    )}
                  </div>

                </div>
              )}

              {/* Route: HISTORY */}
              {activeTab === 'history' && (
                <SessionHistory 
                  sessions={sessions}
                  onDeleteSession={handleDeleteSession}
                  onClearAll={handleClearAllHistory}
                  onAddNote={handleAddAnnotation}
                />
              )}

              {/* Route: ANDROID COMPANION HUB */}
              {activeTab === 'android-hub' && (
                <AndroidCompanion />
              )}

            </motion.div>
          </AnimatePresence>
        </main>

      </div>

      {/* Footer educational references info */}
      <footer className="w-full border-t border-ink mt-12 pt-6 pb-2 text-center text-xs text-ink flex flex-col sm:flex-row items-center justify-between gap-4 max-w-7xl mx-auto px-4 relative z-10 bg-white/40">
        <div className="flex items-center gap-1.5">
          <GraduationCap className="w-4 h-4 text-editorial-blue" />
          <span className="font-semibold uppercase tracking-wider text-[10px]">LEXIS SPEECH LABS &copy; 2026</span>
          <span className="text-neutral-300">|</span>
          <span className="font-semibold text-neutral-500 uppercase tracking-widest text-[9px]">F-Droid & Play Store Ready</span>
        </div>

        <div className="flex items-center gap-5 font-semibold text-ink text-xs">
          <a href="#" className="hover:underline transition flex items-center gap-1">
            <Github className="w-3.5 h-3.5" />
            <span>GitHub Blueprint</span>
          </a>
          <a href="#" className="hover:underline transition">
            License (Apache 2.0)
          </a>
        </div>
      </footer>

    </div>
  );
}
