import { registerVoiceService } from "./voice-service";
import { WebSpeechVoiceService } from "./web-speech-voice-service";

let registered = false;

/**
 * Register the default voice service (Web Speech API) on the client.
 * Safe to call multiple times; safe to call during SSR (becomes a no-op).
 */
export function registerDefaultVoiceService(): void {
  if (registered) return;
  if (typeof window === "undefined") return;
  registerVoiceService(new WebSpeechVoiceService());
  registered = true;
}
