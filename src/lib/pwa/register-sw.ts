/**
 * Guarded service worker registration. Registration is a no-op in:
 *   - dev / non-production builds
 *   - Lovable preview hosts and iframe embeds
 *   - any URL containing ?sw=off
 *
 * A concrete service worker is not shipped in this stage; when one is added
 * at /sw.js this helper will pick it up without any additional changes.
 */
export function registerServiceWorker(): void {
  if (typeof window === "undefined") return;
  if (!("serviceWorker" in navigator)) return;

  const url = new URL(window.location.href);
  if (url.searchParams.get("sw") === "off") {
    void unregisterAll();
    return;
  }

  if (!import.meta.env.PROD) return;

  const host = window.location.hostname;
  const blockedHost =
    host.startsWith("id-preview--") ||
    host.startsWith("preview--") ||
    host === "lovableproject.com" ||
    host.endsWith(".lovableproject.com") ||
    host === "lovableproject-dev.com" ||
    host.endsWith(".lovableproject-dev.com") ||
    host === "beta.lovable.dev" ||
    host.endsWith(".beta.lovable.dev");

  const inIframe = window.self !== window.top;
  if (blockedHost || inIframe) {
    void unregisterAll();
    return;
  }

  // Only register when the worker file exists.
  fetch("/sw.js", { method: "HEAD" })
    .then((res) => {
      if (!res.ok) return;
      navigator.serviceWorker.register("/sw.js").catch((err) => {
        console.warn("[pwa] service worker registration failed", err);
      });
    })
    .catch(() => {
      /* no worker deployed yet */
    });
}

async function unregisterAll(): Promise<void> {
  try {
    const registrations = await navigator.serviceWorker.getRegistrations();
    await Promise.all(registrations.map((r) => r.unregister()));
  } catch {
    /* ignore */
  }
}
