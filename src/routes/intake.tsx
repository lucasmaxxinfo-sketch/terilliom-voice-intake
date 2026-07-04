import { createFileRoute } from "@tanstack/react-router";
import { Mic, Sparkles } from "lucide-react";

import { AppShell } from "@/components/app-shell/AppShell";
import { EmptyState } from "@/components/ui-kit/EmptyState";
import { getVoiceService } from "@/lib/services/voice/voice-service";

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
  const voice = getVoiceService();

  return (
    <AppShell title="New intake" subtitle="Voice-driven capture">
      <div className="mb-8 flex flex-col items-center pt-4">
        <button
          type="button"
          disabled
          aria-label="Start listening"
          className="flex h-40 w-40 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-elevation-3 transition-transform disabled:cursor-not-allowed disabled:opacity-60"
        >
          <Mic className="h-16 w-16" strokeWidth={2.2} />
        </button>
        <p className="mt-6 text-center text-sm text-muted-foreground">
          {voice.available
            ? "Tap and hold to describe the item."
            : "Voice capture will activate once a provider is configured."}
        </p>
      </div>

      <EmptyState
        icon={Sparkles}
        title="Conversation ready"
        description="Extracted fields, confidence scores and follow-up questions will appear here once the AI and voice services are connected."
      />
    </AppShell>
  );
}
