# Agent for Android

Voice-first personal assistant foundation for Android. Target device includes Android 13 / Galaxy A12.

## Included in this first build
- Voice input through Android speech recognition and spoken replies through Android text-to-speech.
- Simple "Oi, Agent" command prefix when using the microphone button.
- Local task and note capture.
- Local reminder notifications (for example, "remind me in 10 minutes to check the document").
- Launch Google Maps search and navigation intents.
- Launch ChatGPT and hand control back to the user.
- Approval prompt before launching consequential actions supported by this starter.

## Build the APK
GitHub Actions builds a debug APK on pushes under this directory. Open the repository's Actions tab, select **Build Agent APK**, then download the **Agent-debug-apk** artifact from a successful run. Install the APK on Android and allow microphone and notification permissions.

## Honest limitations / next engineering steps
This is an MVP foundation, not yet a fully autonomous phone operator. Android's normal speech recognizer listens only when invoked; it does not continuously detect "Oi, Agent" in the background. Always-on wake-word detection requires a carefully designed foreground microphone service, visible notification, battery-exemption setup, and a wake-word engine. ChatGPT can be opened, but automatically starting its internal voice session is not exposed as a stable public Android intent. Full calendar CRUD, email attachments, Drive file search, contact resolution, SMS send/reply tracking, persistent workflow memory, and accessibility-based screen control still need implementation and permissions. The app must not claim actions succeeded unless the result is verified.

No secrets or API keys are included.
