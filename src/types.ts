/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

// Define global types for Speech Recognition
export interface SpeechRecognitionResult {
  readonly length: number;
  [index: number]: {
    readonly length: number;
    [itemIndex: number]: {
      readonly transcript: string;
      readonly confidence: number;
    };
    isFinal?: boolean;
  };
}

export interface SpeechRecognitionEvent extends Event {
  readonly resultIndex: number;
  readonly results: SpeechRecognitionResult;
}

export interface SpeechRecognitionErrorEvent extends Event {
  readonly error: string;
  readonly message?: string;
}

export interface TranscriptSegment {
  id: string;
  timestamp: string; // MM:SS format or seconds
  seconds: number;
  text: string;
  speaker: string;
}

export interface TranscriptionSession {
  id: string;
  title: string;
  timestamp: string; // ISO String
  durationSec: number;
  segments: TranscriptSegment[];
  rawText: string;
  language: string;
  audioSizeMb?: number;
  isFromFile?: boolean;
  notes?: string;
  metrics?: {
    wordsPerMinute: number;
    confidenceScore: number; // 0-100 percentage
    silenceSec: number;
  };
}

export type ActiveTab = 'transcribe' | 'history' | 'android-hub';

export interface AndroidCodeSnippet {
  id: string;
  title: string;
  file: string;
  lang: 'kotlin' | 'xml' | 'groovy';
  code: string;
  description: string;
}
