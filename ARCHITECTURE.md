# Architecture

## Product boundary

Terilliom Intake provides the inventory-intake experience. The web/PWA and Android Agent are related surfaces, but they have different runtime permissions and capabilities.

## Web and PWA
- `src/routes/`: user-facing screens.
- `src/lib/db/`: local database schema and client.
- `src/lib/repositories/`: data access boundaries.
- `src/lib/services/`: voice, barcode, settings, and AI service boundaries.
- `src/lib/pwa/`: service-worker registration.

## Android Agent
- `agent-android/app/`: native Android application.
- `WakeWordService.kt`: optional foreground wake listening.
- `AgentAccessibilityService.kt`: user-enabled best-effort UI assistance.
- `ReminderReceiver.kt`: local reminder delivery.

## Integration rules
- Keep browser and Android permissions separate and explicit.
- Local/offline functionality must continue to work without cloud credentials.
- Do not imply that SMS, email, OAuth, or external AI integrations are connected unless verified end to end.
- Keep secrets out of the repository and browser bundle.
