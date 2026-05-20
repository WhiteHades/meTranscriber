/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { motion } from 'motion/react';
import { 
  Trash2, 
  Search, 
  Calendar, 
  Clock, 
  Download, 
  Copy, 
  Check, 
  Mic, 
  FileAudio, 
  TrendingUp, 
  AlertCircle, 
  Edit3, 
  Save, 
  FileText,
  BadgeAlert
} from 'lucide-react';
import { TranscriptionSession } from '../types';

interface SessionHistoryProps {
  sessions: TranscriptionSession[];
  onDeleteSession: (id: string) => void;
  onClearAll: () => void;
  onAddNote: (id: string, notes: string) => void;
}

export default function SessionHistory({ 
  sessions, 
  onDeleteSession, 
  onClearAll,
  onAddNote
}: SessionHistoryProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [isEditingNote, setIsEditingNote] = useState<boolean>(false);
  const [noteText, setNoteText] = useState<''>('');

  const filteredSessions = sessions.filter(session => 
    session.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    session.rawText.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const activeSession = sessions.find(s => s.id === selectedSessionId);

  const startEditNote = (session: TranscriptionSession) => {
    setIsEditingNote(true);
    setNoteText(session.notes as any || '');
  };

  const saveEditedNote = (sessionId: string) => {
    onAddNote(sessionId, noteText);
    setIsEditingNote(false);
  };

  const handleCopyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleDownloadFile = (session: TranscriptionSession, format: 'txt' | 'json') => {
    let content = '';
    let fileName = `${session.title.toLowerCase().replace(/\s+/g, '_')}_transcript`;
    let mimeType = 'text/plain';

    if (format === 'txt') {
      content = `Title: ${session.title}\nDate: ${new Date(session.timestamp).toLocaleString()}\nDuration: ${Math.floor(session.durationSec / 60)}m ${session.durationSec % 60}s\nSource: ${session.isFromFile ? 'Uploaded File' : 'Mic Capture'}\n\n`;
      session.segments.forEach(segment => {
        content += `[${segment.timestamp}] ${segment.speaker}: ${segment.text}\n`;
      });
      fileName += '.txt';
    } else {
      content = JSON.stringify(session, null, 2);
      mimeType = 'application/json';
      fileName += '.json';
    }

    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const formatSecToTime = (totalSec: number) => {
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 text-ink font-sans" id="offline-history-panel">
      
      {/* Session selector column (5 cols) */}
      <div className="lg:col-span-5 flex flex-col gap-4">
        
        {/* Search header layout */}
        <div className="bg-white p-5 rounded-none border border-ink shadow-none flex flex-col gap-4 text-ink">
          <div className="flex items-center justify-between">
            <h3 className="font-extrabold text-ink text-xs uppercase tracking-wider flex items-center gap-2">
              <span>Saved Audio Database</span>
              <span className="text-[10px] bg-ink text-paper px-2 py-0.5 rounded-none font-mono font-bold">
                {sessions.length}
              </span>
            </h3>
            
            {sessions.length > 0 && (
              <button 
                onClick={onClearAll}
                className="text-xs text-red-600 hover:text-red-700 hover:underline transition font-bold uppercase tracking-widest cursor-pointer"
                id="btn-clear-database"
              >
                Flush Storage
              </button>
            )}
          </div>

          <div className="relative">
            <Search className="absolute left-3.5 top-3.5 w-4 h-4 text-neutral-400" />
            <input 
              type="text"
              placeholder="Search offline logs..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-paper border border-ink rounded-none py-3.5 px-10 text-xs focus:bg-white focus:outline-hidden focus:border-ink transition font-bold text-ink"
            />
          </div>
        </div>

        {/* Sessions list */}
        <div className="flex-1 flex flex-col gap-3 max-h-[500px] overflow-y-auto pr-2 scrollbar-thin">
          {filteredSessions.length === 0 ? (
            <div className="bg-white p-8 rounded-none border-2 border-dashed border-ink text-center flex flex-col items-center justify-center gap-3">
              <BadgeAlert className="w-8 h-8 text-neutral-400" />
              <div className="text-xs text-ink font-bold uppercase tracking-wider">
                {sessions.length === 0 ? "No transcriptions recorded yet." : "No matching results found."}
              </div>
              <p className="text-[11px] text-neutral-500 max-w-[200px] font-sans">
                {sessions.length === 0 ? "Go to the Transcribe tab to record live mic audio or import audio files!" : "Try adjusting your query term"}
              </p>
            </div>
          ) : (
            filteredSessions.map(session => (
              <div 
                key={session.id}
                onClick={() => {
                  setSelectedSessionId(session.id);
                  setIsEditingNote(false);
                }}
                className={`p-4 rounded-none border transition cursor-pointer text-left flex flex-col gap-2.5 relative group ${selectedSessionId === session.id ? 'bg-paper border-ink border-2 shadow-none' : 'bg-white border-ink/40 hover:bg-neutral-50 hover:border-ink'}`}
              >
                
                {/* Header row */}
                <div className="flex items-start justify-between gap-1.5">
                  <h4 className="font-bold text-ink text-xs uppercase tracking-wider line-clamp-1">
                    {session.title}
                  </h4>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      if (selectedSessionId === session.id) setSelectedSessionId(null);
                      onDeleteSession(session.id);
                    }}
                    className="p-1 text-neutral-400 hover:text-red-500 rounded-none transition opacity-0 group-hover:opacity-100 cursor-pointer"
                    title="Delete record"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                {/* Subtitle preview */}
                <p className="text-xs text-neutral-600 line-clamp-2 leading-relaxed font-serif">
                  {session.rawText || "No text transcribed."}
                </p>

                {/* Meta details metadata row */}
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-[10px] text-neutral-500 font-bold uppercase tracking-wider pt-2 border-t border-dashed border-ink/30">
                  <div className="flex items-center gap-1.5 font-mono">
                    {session.isFromFile ? (
                      <FileAudio className="w-3.5 h-3.5 text-neutral-400" />
                    ) : (
                      <Mic className="w-3.5 h-3.5 text-editorial-blue" />
                    )}
                    <span>{session.isFromFile ? 'Audio File' : 'Mic Capture'}</span>
                  </div>

                  <div className="flex items-center gap-1 font-mono">
                    <Clock className="w-3 h-3" />
                    <span>{formatSecToTime(session.durationSec)}</span>
                  </div>

                  <div className="flex items-center gap-1 font-mono">
                    <Calendar className="w-3 h-3" />
                    <span>{new Date(session.timestamp).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</span>
                  </div>
                </div>

              </div>
            ))
          )}
        </div>

      </div>

      {/* Main viewer detailed column (7 cols) */}
      <div className="lg:col-span-7 flex flex-col">
        {activeSession ? (
          <div className="bg-white p-6 rounded-none shadow-none border border-ink flex flex-col gap-5 flex-1 select-text text-ink">
            
            {/* Session Detail Header */}
            <div className="flex flex-col gap-2.5 pb-4 border-b border-ink">
              <div className="flex items-start justify-between gap-4">
                <h3 className="font-extrabold text-ink text-base uppercase tracking-wider select-text font-serif">
                  {activeSession.title}
                </h3>
                
                {/* Export menu triggers */}
                <div className="flex items-center gap-1.5 shrink-0">
                  <button 
                    onClick={() => handleCopyToClipboard(activeSession.rawText, activeSession.id)}
                    className="p-2 hover:bg-neutral-100 border border-ink rounded-none text-ink transition flex items-center justify-center gap-1 cursor-pointer"
                    title="Copy full transcript"
                  >
                    {copiedId === activeSession.id ? (
                      <Check className="w-4 h-4 text-green-500" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>

                  <button 
                    onClick={() => handleDownloadFile(activeSession, 'txt')}
                    className="p-2 hover:bg-neutral-100 border border-ink rounded-none text-ink transition flex items-center justify-center cursor-pointer"
                    title="Download Text Document (.txt)"
                  >
                    <FileText className="w-4 h-4" />
                  </button>

                  <button 
                    onClick={() => handleDownloadFile(activeSession, 'json')}
                    className="px-2.5 py-1.5 hover:bg-neutral-100 border border-ink rounded-none text-ink transition flex items-center justify-center gap-1.5 text-[10px] font-mono font-bold uppercase tracking-wider cursor-pointer"
                    title="Export Structured Schema (.json)"
                  >
                    <span>JSON</span>
                    <Download className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Advanced statistics metrics bar */}
              <div className="grid grid-cols-3 gap-3 bg-paper p-3.5 rounded-none border border-ink mt-1">
                <div className="flex flex-col gap-0.5">
                  <span className="text-[9px] text-neutral-500 font-bold uppercase tracking-wider">Words Count</span>
                  <span className="text-sm font-black text-ink">
                    {activeSession.rawText.split(/\s+/).filter(Boolean).length}
                  </span>
                </div>

                <div className="flex flex-col gap-0.5">
                  <span className="text-[9px] text-neutral-500 font-bold uppercase tracking-wider">Est. Speed</span>
                  <span className="text-sm font-black text-ink flex items-center gap-1">
                    <span>{activeSession.metrics?.wordsPerMinute || 120}</span>
                    <span className="text-[9px] text-neutral-500 font-mono">WPM</span>
                  </span>
                </div>

                <div className="flex flex-col gap-0.5">
                  <span className="text-[9px] text-neutral-500 font-bold uppercase tracking-wider">Accuracy</span>
                  <span className="text-sm font-black text-ink flex items-center gap-1">
                    <span>{activeSession.metrics?.confidenceScore || 98}%</span>
                    <TrendingUp className="w-3.5 h-3.5 text-editorial-blue" />
                  </span>
                </div>
              </div>
            </div>

            {/* Conversation Flow Segment Timeline */}
            <div className="flex-1 flex flex-col gap-4 overflow-y-auto max-h-[340px] pr-2 scrollbar-thin select-text">
              {activeSession.segments.length === 0 ? (
                <p className="text-xs text-ink leading-relaxed italic font-serif">{activeSession.rawText}</p>
              ) : (
                activeSession.segments.map((segment) => (
                  <div key={segment.id} className="flex gap-4 items-start select-text">
                    <span className="text-[9px] font-mono font-bold bg-ink text-paper px-2 py-0.5 rounded-none shrink-0 border border-ink">
                      {segment.timestamp}
                    </span>
                    <div className="flex flex-col gap-0.5 text-left select-text">
                      <span className="text-[10px] font-bold uppercase tracking-wide text-neutral-500">
                        {segment.speaker}
                      </span>
                      <p className="text-xs text-ink leading-relaxed select-text font-serif">
                        {segment.text}
                      </p>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Notes Section with Inline Editor */}
            <div className="mt-auto pt-4 border-t border-ink flex flex-col gap-2 bg-paper p-4 rounded-none border border-ink">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-ink uppercase tracking-wider font-serif">Developer Notes / Context</span>
                {isEditingNote ? (
                  <button 
                    onClick={() => saveEditedNote(activeSession.id)}
                    className="text-xs text-editorial-blue hover:underline transition flex items-center gap-1 font-bold uppercase tracking-widest cursor-pointer"
                  >
                    <Save className="w-3.5 h-3.5" /> Save Note
                  </button>
                ) : (
                  <button 
                    onClick={() => startEditNote(activeSession)}
                    className="text-xs text-neutral-500 hover:text-ink hover:underline transition flex items-center gap-1 font-bold uppercase tracking-widest cursor-pointer"
                  >
                    <Edit3 className="w-3.5 h-3.5" /> Edit Note
                  </button>
                )}
              </div>

              {isEditingNote ? (
                <textarea
                  value={noteText}
                  onChange={(e) => setNoteText(e.target.value as any)}
                  placeholder="Record observations, project milestones, or specific test cases..."
                  rows={2}
                  className="w-full bg-white border border-ink rounded-none p-2 text-xs focus:ring-1 focus:ring-ink focus:outline-hidden text-ink font-mono"
                />
              ) : (
                <p className="text-xs text-neutral-600 leading-relaxed italic font-serif">
                  {activeSession.notes || "No developer annotations filed. Use this to bind specific testing configurations or Kotlin audio source notes to this session."}
                </p>
              )}
            </div>

          </div>
        ) : (
          <div className="bg-white p-12 rounded-none border border-dashed border-ink flex flex-col items-center justify-center gap-3 text-center flex-1 min-h-[300px]">
            <div className="p-4 bg-paper text-ink rounded-none border border-ink">
              <AlertCircle className="w-8 h-8 text-neutral-400" />
            </div>
            <h4 className="font-bold text-ink text-xs uppercase tracking-wider mt-1">No Active Selection</h4>
            <p className="text-xs text-neutral-500 max-w-[260px] leading-relaxed">
              Select any audio transcription log from the history roster on the left to view metrics, segments, annotations, and export capabilities.
            </p>
          </div>
        )}
      </div>

    </div>
  );
}
