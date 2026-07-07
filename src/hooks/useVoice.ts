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
    await service.start({ continuous: true, interimResults: true });
  }, [service]);

  const stop = useCallback(async () => {
    await service.stop();
  }, [service]);

  const cancel = useCallback(async () => {
    await service.cancel();
    setInterimText("");
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
