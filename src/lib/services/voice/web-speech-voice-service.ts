/**
 * Web Speech API implementation of VoiceService.
 * Uses the browser's SpeechRecognition (webkitSpeechRecognition on Safari/Chrome).
 * Falls back to `available = false` when the API is missing.
 */

import type {
  VoiceEventMap,
  VoiceListener,
  VoiceService,
  VoiceStartOptions,
  VoiceStatus,
  VoiceTranscript,
} from "./voice-service";

type SpeechRecognitionCtor = new () => SpeechRecognitionInstance;

interface SpeechRecognitionInstance extends EventTarget {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  maxAlternatives: number;
  start(): void;
  stop(): void;
  abort(): void;
  onresult: ((event: SpeechRecognitionEvent) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEvent) => void) | null;
  onend: (() => void) | null;
  onstart: (() => void) | null;
}

interface SpeechRecognitionEvent extends Event {
  readonly resultIndex: number;
  readonly results: SpeechRecognitionResultList;
}

interface SpeechRecognitionResultList {
  readonly length: number;
  item(index: number): SpeechRecognitionResult;
  [index: number]: SpeechRecognitionResult;
}

interface SpeechRecognitionResult {
  readonly length: number;
  readonly isFinal: boolean;
  item(index: number): SpeechRecognitionAlternative;
  [index: number]: SpeechRecognitionAlternative;
}

interface SpeechRecognitionAlternative {
  readonly transcript: string;
  readonly confidence: number;
}

interface SpeechRecognitionErrorEvent extends Event {
  readonly error: string;
  readonly message?: string;
}

function getRecognitionCtor(): SpeechRecognitionCtor | null {
  if (typeof window === "undefined") return null;
  const w = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor;
    webkitSpeechRecognition?: SpeechRecognitionCtor;
  };
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null;
}

type Listeners = {
  [K in keyof VoiceEventMap]: Set<VoiceListener<K>>;
};

export class WebSpeechVoiceService implements VoiceService {
  readonly id = "web-speech";
  readonly available: boolean;

  private _status: VoiceStatus = "idle";
  private recognition: SpeechRecognitionInstance | null = null;
  private ctor: SpeechRecognitionCtor | null;
  private listeners: Listeners = {
    status: new Set(),
    transcript: new Set(),
    error: new Set(),
  };

  constructor() {
    this.ctor = getRecognitionCtor();
    this.available = this.ctor !== null;
  }

  get status(): VoiceStatus {
    return this._status;
  }

  private setStatus(next: VoiceStatus) {
    if (this._status === next) return;
    this._status = next;
    this.listeners.status.forEach((l) => l(next));
  }

  private emitError(err: Error) {
    this.listeners.error.forEach((l) => l(err));
  }

  private emitTranscript(t: VoiceTranscript) {
    this.listeners.transcript.forEach((l) => l(t));
  }

  async start(options: VoiceStartOptions = {}): Promise<void> {
    if (!this.ctor) {
      const err = new Error(
        "Speech recognition is not supported in this browser. Try Chrome, Edge, or Safari.",
      );
      this.setStatus("error");
      this.emitError(err);
      throw err;
    }

    // Ensure any previous instance is torn down before starting a new one.
    if (this.recognition) {
      try {
        this.recognition.abort();
      } catch {
        // ignore
      }
      this.recognition = null;
    }

    const rec = new this.ctor();
    rec.lang = options.language ?? "en-US";
    rec.continuous = options.continuous ?? true;
    rec.interimResults = options.interimResults ?? true;
    rec.maxAlternatives = 1;

    rec.onstart = () => this.setStatus("listening");
    rec.onend = () => {
      this.recognition = null;
      if (this._status !== "error") this.setStatus("idle");
    };
    rec.onerror = (event) => {
      const message = event.message || event.error || "Speech recognition error";
      const err = new Error(message);
      this.setStatus("error");
      this.emitError(err);
    };
    rec.onresult = (event) => {
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i];
        const alt = result[0];
        if (!alt) continue;
        this.emitTranscript({
          text: alt.transcript,
          isFinal: result.isFinal,
          confidence: alt.confidence,
          createdAt: new Date().toISOString(),
        });
      }
    };

    this.recognition = rec;
    try {
      rec.start();
    } catch (e) {
      this.recognition = null;
      const err = e instanceof Error ? e : new Error(String(e));
      this.setStatus("error");
      this.emitError(err);
      throw err;
    }
  }

  async stop(): Promise<void> {
    if (!this.recognition) return;
    try {
      this.recognition.stop();
    } catch {
      // ignore
    }
  }

  async cancel(): Promise<void> {
    if (!this.recognition) return;
    try {
      this.recognition.abort();
    } catch {
      // ignore
    }
    this.recognition = null;
    this.setStatus("idle");
  }

  on<K extends keyof VoiceEventMap>(
    event: K,
    listener: VoiceListener<K>,
  ): () => void {
    const set = this.listeners[event] as Set<VoiceListener<K>>;
    set.add(listener);
    return () => set.delete(listener);
  }
}
