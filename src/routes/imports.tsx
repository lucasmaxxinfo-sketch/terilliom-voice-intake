import { createFileRoute } from "@tanstack/react-router";
import { Download } from "lucide-react";

import { AppShell } from "@/components/app-shell/AppShell";
import { EmptyState } from "@/components/ui-kit/EmptyState";

export const Route = createFileRoute("/imports")({
  head: () => ({
    meta: [
      { title: "Imports — Terilliom Intake" },
      { name: "description", content: "Bulk import inventory from CSV or JSON." },
    ],
  }),
  component: ImportsScreen,
});

function ImportsScreen() {
  return (
    <AppShell title="Imports" subtitle="Bulk load inventory">
      <EmptyState
        icon={Download}
        title="No imports yet"
        description="CSV and JSON imports will be processed here. Each run keeps a summary of successful and failed rows."
      />
    </AppShell>
  );
}
