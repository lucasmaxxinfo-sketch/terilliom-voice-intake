import type { LucideIcon } from "lucide-react";

interface StatCardProps {
  label: string;
  value: string | number;
  hint?: string;
  icon?: LucideIcon;
}

export function StatCard({ label, value, hint, icon: Icon }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-4">
      <div className="flex items-center justify-between text-xs uppercase tracking-wide text-muted-foreground">
        <span>{label}</span>
        {Icon ? <Icon className="h-4 w-4" /> : null}
      </div>
      <div className="mt-2 text-2xl font-semibold text-foreground">{value}</div>
      {hint ? <div className="mt-1 text-xs text-muted-foreground">{hint}</div> : null}
    </div>
  );
}
