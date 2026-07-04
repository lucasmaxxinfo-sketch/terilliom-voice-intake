import { createFileRoute } from "@tanstack/react-router";
import {
  Building2,
  Database,
  Mic,
  Palette,
  QrCode,
  Sliders,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { AppShell } from "@/components/app-shell/AppShell";
import { useSettings } from "@/hooks/useSettings";

export const Route = createFileRoute("/settings")({
  head: () => ({
    meta: [
      { title: "Settings — Terilliom Intake" },
      { name: "description", content: "Configure business profile, defaults, voice and barcode." },
    ],
  }),
  component: SettingsScreen,
});

interface SectionProps {
  icon: LucideIcon;
  title: string;
  description: string;
  value: string;
}

function Section({ icon: Icon, title, description, value }: SectionProps) {
  return (
    <div className="flex items-start gap-3 rounded-2xl border border-border bg-surface px-4 py-4">
      <span className="mt-0.5 flex h-10 w-10 items-center justify-center rounded-xl bg-surface-variant text-muted-foreground">
        <Icon className="h-5 w-5" />
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-3">
          <div className="text-sm font-semibold text-foreground">{title}</div>
          <div className="truncate text-xs font-medium text-primary">{value}</div>
        </div>
        <p className="mt-1 text-xs text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}

function SettingsScreen() {
  const { data: settings } = useSettings();

  return (
    <AppShell title="Settings" subtitle="Configure your workspace">
      <div className="space-y-3">
        <Section
          icon={Building2}
          title="Business profile"
          description="Business name, profile template and default currency."
          value={settings?.business.businessName ?? "—"}
        />
        <Section
          icon={Sliders}
          title="Default values"
          description="Status and condition applied to every new intake."
          value={settings?.defaults.status ?? "—"}
        />
        <Section
          icon={Palette}
          title="Theme"
          description="Dark, light or match system preference."
          value={settings?.theme.mode ?? "dark"}
        />
        <Section
          icon={Mic}
          title="Voice"
          description="Speech recognition provider, language and wake word."
          value={settings?.voice.enabled ? "Enabled" : "Disabled"}
        />
        <Section
          icon={QrCode}
          title="Barcode"
          description="Default symbology and printer target."
          value={settings?.barcode.defaultSymbology ?? "code128"}
        />
        <Section
          icon={Database}
          title="Database & backup"
          description="Local storage usage and backup schedule."
          value={settings?.database.autoBackup ? "Auto" : "Manual"}
        />
      </div>
    </AppShell>
  );
}
