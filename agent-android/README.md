# Agent for Android

Voice-first Android personal assistant foundation, targeted at Android 13 and the Galaxy A12.

## Implemented in the current source
- Tap-to-speak voice input and spoken replies.
- Optional foreground “Oi Agent” wake-phrase listening with a persistent notification. Android may restrict recognition availability and battery use; stop it from the app when not wanted.
- Local daily task, weekly task and note capture.
- Scheduled reminder notifications by relative delay or clock time.
- Read the next event from the Android calendar after permission is granted.
- Google Maps address search and turn-by-turn navigation intents.
- Launch common apps: ChatGPT, Maps, Gmail, Calendar, Drive and Chrome.
- Contact lookup and SMS draft creation with an explicit confirmation and final review in the messaging app.
- Email composition with files selected through Android's document picker.
- Optional accessibility service for tapping visible controls, typing into a focused field, scrolling and back/home after the user enables it in Android settings.

## Build and install
GitHub Actions builds a debug APK when this directory changes. Open the repository's **Actions** tab, open the newest **Build Agent Android APK** run, and download the **Agent-debug-apk** artifact after a successful run.

## Enable optional automation
For wake listening, tap **Start wake listening** and keep the persistent notification visible. For app UI control, open Android Settings > Accessibility > Installed apps/services > Agent and enable the service. Android requires the user to enable these explicitly.

## Still not fully autonomous
- Wake-word recognition depends on Android's speech recognizer and is not a dedicated offline wake-word engine; availability, battery policy and recognition reliability vary by device.
- Accessibility control is best-effort. Apps can change layouts, hide controls or block automation. Use “tap [visible label]” only when the intended app is in the foreground.
- SMS and email are opened as drafts for final review. Agent does not claim they were sent.
- Direct Google OAuth integrations for full Calendar/Drive/Gmail operations, reading replies, reliable workflow memory, recurring schedules and verified send status are not yet connected.
- Opening ChatGPT does not automatically start its internal voice session; there is no stable public Android intent for that control.

No API keys or secrets are included.
