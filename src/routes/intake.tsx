import { createFileRoute } from "@tanstack/react-router";
import { ExternalLink, Mic, MicOff, RotateCcw, Square } from "lucide-react";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/app-shell/AppShell";
import { useVoice } from "@/hooks/useVoice";
import { registerDefaultVoiceService } from "@/lib/services/voice/register";

export const Route = createFileRoute("/intake")({
  head: () => ({
    meta: [
      { title: "New intake — Terilliom Intake" },
      { name: "description", content: "Capture a new inventory item by voice." },
    ],
  }),
  component: IntakeScreen,
});

function IntakeScreen() {
  const [ready, setReady] = useState(false);
  const [inIframe, setInIframe] = useState(false);
  const [permission, setPermission] = useState<PermissionState | "unknown">("unknown");

  useEffect(() => {
    registerDefaultVoiceService();
    setReady(true);
    try {
      setInIframe(window.top !== window.self);
    } catch {
      setInIframe(true);
    }
    if (navigator.permissions?.query) {
      navigator.permissions
        .query({ name: "microphone" as PermissionName })
        .then((s) => {
          setPermission(s.state);
          s.onchange = () => setPermission(s.state);
        })
        .catch(() => setPermission("unknown"));
    }
  }, []);

  const micBlocked = permission === "denied";

  return (
    <AppShell title="New intake" subtitle="Voice-driven capture">
      {micBlocked ? <MicBlockedBanner inIframe={inIframe} /> : null}
      {ready ? <VoicePanel micBlocked={micBlocked} /> : null}
    </AppShell>
  );
}

function MicBlockedBanner({ inIframe }: { inIframe: boolean }) {
  const title = inIframe
    ? "Microphone is blocked in this preview."
    : "Microphone access is blocked.";
  const message = inIframe
    ? "Browsers deny microphone access to embedded previews. Open the app in its own tab to record voice."
    : "Allow microphone access in the browser settings, then reload this screen.";

  return (
    <div className="mb-4 rounded-3xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200">
      <p className="font-semibold text-amber-100">{title}</p>
      <p className="mt-1 leading-relaxed">{message}</p>
      {inIframe ? (
        <a
          href={typeof window !== "undefined" ? window.location.href : "#"}
          target="_blank"
          rel="noreferrer"
          className="mt-3 inline-flex h-9 items-center gap-1.5 rounded-2xl bg-amber-400 px-3 text-xs font-semibold text-amber-950 transition-colors hover:bg-amber-300"
        >
          <ExternalLink className="h-3.5 w-3.5" />
          Open in new tab
        </a>
      ) : null}
    </div>
  );
}

function VoicePanel({ micBlocked }: { micBlocked: boolean }) {
  const { status, available, finalText, interimText, error, start, stop, cancel, reset } =
    useVoice();
  const listening = status === "listening";

  const handleToggle = () => {
    if (micBlocked) return;
    if (listening) {
      void stop();
    } else {
      void start();
    }
  };

  const statusLabel = micBlocked
    ? "Microphone access is blocked. Open in a new tab or allow microphone access."
    : !available
    ? "Voice recognition is not supported in this browser."
    : error
      ? error
      : listening
        ? "Listening — speak now."
        : finalText
          ? "Paused. Tap the mic to keep going."
          : "Tap the mic to start capturing.";

  return (
    <>
      <div className="mb-6 flex flex-col items-center pt-4">
        <button
          type="button"
          onClick={handleToggle}
          disabled={!available || micBlocked}
          aria-pressed={listening}
          aria-label={listening ? "Stop listening" : "Start listening"}
          className={[
            "relative flex h-40 w-40 items-center justify-center rounded-full text-primary-foreground shadow-elevation-3 transition-transform",
            "disabled:cursor-not-allowed disabled:opacity-60",
            listening
              ? "bg-red-500 scale-105 animate-pulse"
              : "bg-primary hover:scale-[1.02] active:scale-95",
          ].join(" ")}
        >
          {!available ? (
            <MicOff className="h-16 w-16" strokeWidth={2.2} />
          ) : listening ? (
            <Square className="h-14 w-14" strokeWidth={2.4} fill="currentColor" />
          ) : (
            <Mic className="h-16 w-16" strokeWidth={2.2} />
          )}
        </button>
        <p
          role="status"
          aria-live="polite"
          className={`mt-6 text-center text-sm ${error ? "text-red-400" : "text-muted-foreground"}`}
        >
          {statusLabel}
        </p>
      </div>

      <div className="rounded-3xl border border-border bg-surface p-4 md:p-6">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-foreground">Transcript</h2>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                void cancel();
                reset();
              }}
              disabled={!finalText && !interimText && status === "idle"}
              className="inline-flex h-9 items-center gap-1.5 rounded-2xl border border-border px-3 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground disabled:opacity-40"
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Clear
            </button>
          </div>
        </div>
        <div className="min-h-32 whitespace-pre-wrap break-words text-base leading-relaxed text-foreground">
          {finalText ? <span>{finalText}</span> : null}
          {interimText ? (
            <span className="text-muted-foreground">
              {finalText ? " " : ""}
              {interimText}
            </span>
          ) : null}
          {!finalText && !interimText ? (
            <span className="text-muted-foreground">
              Your words will appear here as you speak.
            </span>
          ) : null}
        </div>
      </div>
    </>
  );
}
