import { createFileRoute, Link } from "@tanstack/react-router";
import {
  Boxes,
  CalendarCheck,
  ChevronRight,
  Mic,
  Package,
  Search,
} from "lucide-react";

import { AppShell } from "@/components/app-shell/AppShell";
import { EmptyState } from "@/components/ui-kit/EmptyState";
import { StatCard } from "@/components/ui-kit/StatCard";
import { useRecentInventory, useTodayIntakeCount } from "@/hooks/useRecentInventory";
import { useSettings } from "@/hooks/useSettings";

export const Route = createFileRoute("/")({
  component: HomeScreen,
});

function HomeScreen() {
  const settings = useSettings();
  const recent = useRecentInventory(6);
  const today = useTodayIntakeCount();
  const businessName = settings.data?.business.businessName ?? "Terilliom Intake";

  return (
    <AppShell title={businessName} subtitle="Terilliom Intake">
      <section className="mb-6">
        <Link
          to="/intake"
          className="group relative flex flex-col items-center justify-center overflow-hidden rounded-3xl bg-primary/10 p-8 text-center shadow-elevation-2 transition-transform active:scale-[0.99]"
        >
          <span
            aria-hidden
            className="absolute inset-0 bg-gradient-to-b from-primary/15 via-transparent to-transparent"
          />
          <span className="relative flex h-28 w-28 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-elevation-3 transition-transform group-active:scale-95">
            <Mic className="h-12 w-12" strokeWidth={2.2} />
          </span>
          <span className="relative mt-5 text-lg font-semibold text-foreground">
            Start voice intake
          </span>
          <span className="relative mt-1 text-sm text-muted-foreground">
            Tap and speak — the item is captured for you
          </span>
        </Link>
      </section>

      <section className="mb-6 grid grid-cols-2 gap-3">
        <StatCard
          label="Today"
          value={today.data ?? 0}
          hint="items intaken"
          icon={CalendarCheck}
        />
        <StatCard
          label="Recent"
          value={recent.data?.length ?? 0}
          hint="visible below"
          icon={Boxes}
        />
      </section>

      <section className="mb-4">
        <Link
          to="/search"
          className="flex h-14 items-center gap-3 rounded-2xl border border-border bg-surface px-4 text-muted-foreground transition-colors hover:bg-surface-variant"
        >
          <Search className="h-5 w-5" />
          <span className="text-sm">Search inventory, barcodes, serials…</span>
        </Link>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
            Recent intake
          </h2>
          <Link
            to="/history"
            className="text-sm font-medium text-primary hover:underline"
          >
            View all
          </Link>
        </div>

        {recent.isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => (
              <div
                key={i}
                className="h-16 animate-pulse rounded-2xl border border-border bg-surface"
              />
            ))}
          </div>
        ) : (recent.data?.length ?? 0) === 0 ? (
          <EmptyState
            icon={Package}
            title="No items yet"
            description="Start a voice intake to add your first item. Everything is saved locally on this device."
          />
        ) : (
          <ul className="space-y-2">
            {recent.data!.map((item) => (
              <li key={item.id}>
                <div className="flex items-center gap-3 rounded-2xl border border-border bg-surface px-4 py-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-surface-variant text-muted-foreground">
                    <Package className="h-5 w-5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-foreground">
                      {[item.brand, item.model].filter(Boolean).join(" ") ||
                        "Untitled item"}
                    </div>
                    <div className="truncate text-xs text-muted-foreground">
                      {item.serialNumber
                        ? `SN ${item.serialNumber}`
                        : item.barcode
                          ? `Barcode ${item.barcode}`
                          : "No identifier"}
                    </div>
                  </div>
                  <ChevronRight className="h-4 w-4 text-muted-foreground" />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </AppShell>
  );
}
