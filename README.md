# Terilliom Intake

Voice-first, offline-first inventory intake for workshops, warehouses, and service businesses. The project includes a responsive web/PWA interface and a native Android assistant foundation.

## What is included

- Voice-driven inventory intake and spoken responses.
- Local inventory history and offline-first storage.
- Barcode and photo capture foundations.
- Search, import, and settings screens.
- Android Agent foundation in `agent-android/`.
- GitHub Actions workflow for building the Android debug APK.

The Android assistant's implemented capabilities and limitations are documented in [agent-android/README.md](agent-android/README.md).

## Development

This project uses open-source tooling directly through GitHub:

- React and TypeScript
- TanStack Start and Router
- Vite, Tailwind CSS, and Nitro
- Bun
- Native Android Gradle build

Install [Bun](https://bun.sh/), then run:

```sh
git clone https://github.com/lucasmaxxinfo-sketch/terilliom-voice-intake.git
cd terilliom-voice-intake
bun install --frozen-lockfile
bun run dev
```

## Checks

```sh
bun run build
bun run typecheck
bun run lint
```

GitHub Actions runs the checks for the working branch and pull requests. The Android APK workflow remains separate and continues to build from changes under `agent-android/`.

## Android APK

1. Open the repository's **Actions** tab.
2. Select **Build Agent Android APK**.
3. Open the latest successful run.
4. Download the `Agent-debug-apk` artifact.

## Security and privacy

No API keys or secrets belong in source control. Android permissions and accessibility features must be enabled by the device owner. SMS actions require explicit confirmation.

## Licence

Released under the MIT licence. See [LICENSE](LICENSE).
