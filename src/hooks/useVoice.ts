import { useCallback, useEffect, useRef, useState } from "react";

import {
  getVoiceService,
  type VoiceStatus,
  type VoiceTranscript,
} from "@/lib/services/voice/voice-service";

interface UseVoiceState {
  status: VoiceStatus;
  available: boolean;
  finalText: string;
  interimText: string;
  error: string | null;
  start: () => Promise<void>;
  stop: () => Promise<void>;
  cancel: () => Promise<void>;
  reset: () => void;
}

export function useVoice(): UseVoiceState {
  const service = getVoiceService();
  const [status, setStatus] = useState<VoiceStatus>(service.status);
  const [finalText, setFinalText] = useState("");
  const [interimText, setInterimText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const finalRef = useRef("");

  useEffect(() => {
    const offStatus = service.on("status", (s) => setStatus(s));
    const offTranscript = service.on("transcript", (t: VoiceTranscript) => {
      if (t.isFinal) {
        finalRef.current = (finalRef.current + " " + t.text).trim();
        setFinalText(finalRef.current);
        setInterimText("");
      } else {
        setInterimText(t.text);
      }
    });
    const offError = service.on("error", (e) => setError(e.message));
    return () => {
      offStatus();
      offTranscript();
      offError();
    };
  }, [service]);

  const start = useCallback(async () => {
    setError(null);
    try {
      await service.start({ continuous: true, interimResults: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Voice capture could not start.");
    }
  }, [service]);

  const stop = useCallback(async () => {
    try {
      await service.stop();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Voice capture could not stop.");
    }
  }, [service]);

  const cancel = useCallback(async () => {
    try {
      await service.cancel();
      setInterimText("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Voice capture could not be cleared.");
    }
  }, [service]);

  const reset = useCallback(() => {
    finalRef.current = "";
    setFinalText("");
    setInterimText("");
    setError(null);
  }, []);

  return {
    status,
    available: service.available,
    finalText,
    interimText,
    error,
    start,
    stop,
    cancel,
    reset,
  };
}
