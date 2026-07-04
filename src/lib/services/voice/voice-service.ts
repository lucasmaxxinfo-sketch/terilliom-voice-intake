/**
 * Voice service abstraction. Real speech recognition is intentionally not
 * implemented — the intake screen consumes this interface and a concrete
 * provider is registered later.
 */

export type VoiceStatus = "idle" | "listening" | "processing" | "error";

export interface VoiceTranscript {
  text: string;
  isFinal: boolean;
  confidence: number;
  createdAt: string;
}

export interface VoiceStartOptions {
  language?: string;
  continuous?: boolean;
  interimResults?: boolean;
}

export interface VoiceEventMap {
  status: VoiceStatus;
  transcript: VoiceTranscript;
  error: Error;
}

export type VoiceListener<K extends keyof VoiceEventMap> = (
  event: VoiceEventMap[K],
) => void;

export interface VoiceService {
  readonly id: string;
  readonly available: boolean;
  readonly status: VoiceStatus;

  start(options?: VoiceStartOptions): Promise<void>;
  stop(): Promise<void>;
  cancel(): Promise<void>;

  on<K extends keyof VoiceEventMap>(event: K, listener: VoiceListener<K>): () => void;
}

class UnavailableVoiceService implements VoiceService {
  readonly id = "unavailable";
  readonly available = false;
  readonly status: VoiceStatus = "idle";

  private fail(): never {
    throw new Error(
      "No voice service is registered. Call registerVoiceService() with a provider before starting capture.",
    );
  }

  start(): Promise<void> {
    this.fail();
  }
  stop(): Promise<void> {
    return Promise.resolve();
  }
  cancel(): Promise<void> {
    return Promise.resolve();
  }
  on(): () => void {
    return () => {};
  }
}

let current: VoiceService = new UnavailableVoiceService();

export function registerVoiceService(service: VoiceService): void {
  current = service;
}

export function getVoiceService(): VoiceService {
  return current;
}
