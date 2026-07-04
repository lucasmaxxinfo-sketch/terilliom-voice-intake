import type { ReactNode } from "react";

import { BottomNav } from "./BottomNav";
import { TopBar } from "./TopBar";

interface AppShellProps {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
  showNav?: boolean;
}

export function AppShell({
  title,
  subtitle,
  action,
  children,
  showNav = true,
}: AppShellProps) {
  return (
    <div className="flex min-h-dvh flex-col bg-background text-foreground">
      <TopBar title={title} subtitle={subtitle} action={action} />
      <main className="flex-1 overflow-y-auto pb-28">
        <div className="mx-auto w-full max-w-3xl px-4 py-4 md:px-6 md:py-6">
          {children}
        </div>
      </main>
      {showNav ? <BottomNav /> : null}
    </div>
  );
}
