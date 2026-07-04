import { Link } from "@tanstack/react-router";
import { Clock, Download, Home, Mic, Search } from "lucide-react";
import type { LucideIcon } from "lucide-react";

interface NavItem {
  to: "/" | "/intake" | "/search" | "/history" | "/imports";
  label: string;
  icon: LucideIcon;
  primary?: boolean;
}

const items: NavItem[] = [
  { to: "/", label: "Home", icon: Home },
  { to: "/search", label: "Search", icon: Search },
  { to: "/intake", label: "Intake", icon: Mic, primary: true },
  { to: "/history", label: "History", icon: Clock },
  { to: "/imports", label: "Imports", icon: Download },
];

export function BottomNav() {
  return (
    <nav
      aria-label="Primary"
      className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface/95 backdrop-blur"
    >
      <ul className="mx-auto flex w-full max-w-3xl items-stretch justify-between gap-1 px-2 py-2">
        {items.map(({ to, label, icon: Icon, primary }) => (
          <li key={to} className="flex-1">
            <Link
              to={to}
              activeOptions={{ exact: to === "/" }}
              activeProps={{
                className:
                  "text-primary [&_[data-nav-pill]]:bg-primary-container [&_[data-nav-pill]]:text-primary-container-foreground",
              }}
              inactiveProps={{ className: "text-muted-foreground" }}
              className="flex h-16 flex-col items-center justify-center gap-1 rounded-2xl text-xs font-medium transition-colors hover:text-foreground"
            >
              <span
                data-nav-pill
                className={
                  primary
                    ? "flex h-10 w-14 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-elevation-2"
                    : "flex h-8 w-12 items-center justify-center rounded-2xl"
                }
              >
                <Icon className={primary ? "h-6 w-6" : "h-5 w-5"} />
              </span>
              <span>{label}</span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
