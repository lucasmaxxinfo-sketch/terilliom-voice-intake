import { createFileRoute } from "@tanstack/react-router";
import { Clock } from "lucide-react";

import { AppShell } from "@/components/app-shell/AppShell";
import { EmptyState } from "@/components/ui-kit/EmptyState";

export const Route = createFileRoute("/history")({
  head: () => ({
    meta: [
      { title: "History — Terilliom Intake" },
      { name: "description", content: "Review every change made to inventory records." },
    ],
  }),
  component: HistoryScreen,
});

function HistoryScreen() {
  return (
    <AppShell title="History" subtitle="Every change is logged">
      <EmptyState
        icon={Clock}
        title="No history yet"
        description="Each intake, edit and status change is written to a local audit trail. Entries appear here as soon as they happen."
      />
    </AppShell>
  );
}
