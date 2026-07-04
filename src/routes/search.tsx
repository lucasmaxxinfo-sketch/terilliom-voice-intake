import { createFileRoute } from "@tanstack/react-router";
import { Search } from "lucide-react";

import { AppShell } from "@/components/app-shell/AppShell";
import { EmptyState } from "@/components/ui-kit/EmptyState";

export const Route = createFileRoute("/search")({
  head: () => ({
    meta: [
      { title: "Search — Terilliom Intake" },
      { name: "description", content: "Search inventory by barcode, serial or attributes." },
    ],
  }),
  component: SearchScreen,
});

function SearchScreen() {
  return (
    <AppShell title="Search" subtitle="Find inventory quickly">
      <div className="mb-4 flex h-14 items-center gap-3 rounded-2xl border border-border bg-surface px-4">
        <Search className="h-5 w-5 text-muted-foreground" />
        <input
          type="search"
          placeholder="Barcode, serial, brand, model…"
          aria-label="Search inventory"
          className="h-full flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
        />
      </div>

      <EmptyState
        icon={Search}
        title="Start typing to search"
        description="Search runs against the local database. Results include barcode, serial number, brand, model and department."
      />
    </AppShell>
  );
}
