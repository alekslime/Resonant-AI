# Resonant — Interaction Prototype

A native Android (Kotlin + Jetpack Compose) prototype demonstrating a
**non-spatial interaction model**: navigate lessons, listen to content,
answer quizzes, and chat with an AI without ever needing to visually locate
a button.

No auth, no database, no cloud AI service, no API key — Lessons, Quiz, and
Settings are fully local and hardcoded. Chat is the one exception: it sends
what you say to an [Ollama](https://ollama.com) server on your own network and
speaks the reply back as it is generated (see "Chat / voice AI setup").

---

## How to open and run it

1. Open this folder in **Android Studio** (Koala/2024.1 or newer; the project
   targets AGP 8.5.2 / Kotlin 1.9.24 / compileSdk 34).
2. The Gradle wrapper files are not committed. When Android Studio offers to
   generate the wrapper, accept — or run `gradle wrapper` yourself.
3. Copy `local.properties.example` to `local.properties` (Android Studio may
   already have created that file for `sdk.dir` — just add the two Ollama lines
   to it) and set them for your network. See the next section.
4. Sync, then run the `app` configuration on a device or emulator on **API 26+**.

The app requests `VIBRATE` and `INTERNET` at install time (no runtime prompt),
plus `RECORD_AUDIO`, asked for at runtime the first time you tap center on the
Chat screen.

Unit tests are plain JVM tests — no device needed:

```
./gradlew :app:testDebugUnitTest      # or run them from Android Studio
```

---

## Chat / voice AI setup

Tap the center of the Chat screen to talk. Your speech is transcribed on the
device's own recognizer and sent to *your* Ollama server; the reply is spoken
sentence by sentence while the model is still writing the rest.

1. On a machine on your Wi-Fi, run `ollama serve` and `ollama pull llama3.2`
   (or any model you prefer).
2. In `local.properties` (git-ignored, so your LAN address is never committed):

   ```
   ollama.baseUrl=http://192.168.1.50:11434
   ollama.model=llama3.2
   ```

   - `ollama.baseUrl` — that machine's LAN address. The default, `10.0.2.2`,
     only resolves from the Android *emulator*; a physical phone needs the real
     LAN IP. Values are baked in at build time, so rebuild after changing them.
   - `ollama.model` — must match a model you've already pulled.
3. **Plain HTTP works in debug builds only** (`src/debug/AndroidManifest.xml`).
   A release build needs an `https://` URL.

Speech-to-text uses Android's built-in `SpeechRecognizer` (usually Google's; most
recognizers need the device online). Text-to-speech goes through the same
on-device `AudioManager` every other screen uses.

What Chat does so that silence never looks like a crash:

- "Listening." is spoken **first**, and the microphone opens only when it has
  finished — otherwise the recognizer can hear the app's own voice.
- While the model works, the phone pulses softly every couple of seconds and
  says "Still thinking. Tap the center to cancel." every ten. A tap cancels the
  request and closes the connection.
- Tapping while an answer is still being spoken starts a new question
  (barge-in) and stops the old stream.
- If the server is unreachable, Chat announces the error out loud.

---

## The interaction model, in code

| Concept | Where it lives |
|---|---|
| Gesture zones (LEFT_EDGE / CENTER / RIGHT_EDGE) + the pointer-input loop | `gestures/GestureManager.kt` |
| The pure decisions behind it (zone, swipe direction, hold step) — unit tested | `gestures/GestureClassifier.kt` |
| Gesture vocabulary | `gestures/InteractionZone.kt` |
| Centralized speech (semantic-unit queue, pause/resume/repeat/speed, streamed append, announcements with completion callbacks) | `audio/AudioManager.kt` |
| Centralized haptics (named pattern vocabulary, one place to retune) | `haptics/HapticPattern.kt`, `haptics/HapticManager.kt` |
| Hardcoded lesson / quiz content, chat framing prompt | `content/LessonData.kt`, `content/QuizData.kt`, `content/ChatData.kt` |
| Voice input (one-shot speech-to-text) | `speech/SpeechInputManager.kt` |
| Streamed reply → speakable sentences | `speech/SentenceChunker.kt` |
| Ollama streaming client + build-time config | `network/OllamaClient.kt`, `network/OllamaConfig.kt` |
| Shared gesture surface, debug wiring, TalkBack actions | `ui/components/GestureSurface.kt` |
| The screens | `ui/home`, `ui/lessons`, `ui/quiz`, `ui/chat`, `ui/settings`, `ui/onboarding`, `ui/talkback` |
| Debug/developer mode | `ui/settings/DebugScreen.kt` (Settings → Debug Mode) |

**Every screen attaches the same `GestureSurface`.** That's what keeps the
gesture grammar identical everywhere — no screen invents its own touch
handling. `GestureManager.kt` is the only file that reads raw pointer input; it
classifies touches into `ResonantGesture`s using **relative** zone boundaries,
so nothing depends on fixed screen coordinates.

Holds are decided by a real timeout, not by waiting for the next pointer event
(a perfectly still finger produces almost none), so the "hold engaged" haptic
fires at the 500 ms mark, not on release.

`AudioManager` treats all spoken content — lesson sections, quiz prompts and
options, chat replies — as a queue of small `SemanticUnit`s with a current
index. Lessons and chat auto-advance through their queue; quiz screens use the
same `next()`/`previous()` to *explore* answer options. Chat replies arrive
while the queue is being read, so units can be appended and playback continues
across the gap.

`HapticManager` is the only class that touches the system `Vibrator`. Every
pattern is a named `HapticPattern` with its timing in one map
(`HapticPatterns.timings`). Quiz options A–D are a separate, counted family of
long pulses (1–4) that shares nothing with the navigation patterns — a unit
test enforces it.

---

## The gesture grammar

The same on every screen:

| Where | Gesture | Does |
|---|---|---|
| Center | Swipe up / down | Next / previous (menu item, lesson unit, quiz option, chat sentence) |
| Center | Swipe right | Continue / submit (Lesson: next section; Quiz: submit) |
| Center | Swipe left | Back (Lesson: previous section) |
| Center | Tap | Select / confirm (Chat: ask a question, or cancel while thinking) |
| Left edge | Tap | Pause / resume speech |
| Left edge | Double-tap | Repeat the current unit |
| Right edge | Hold ~½ s (three quick ticks), then drag up / down | Speech faster / slower, in six steps (0.75×–2.0×) |
| Right edge | Hold ~½ s, release without dragging | Back / leave screen |
| Right edge | Tap | Nothing, deliberately (no accidental triggers) |
| Anywhere | Three-finger tap | Repeat the current unit |
| Anywhere | Three-finger hold | "Where am I?" — screen, position, playback state |

In the tutorial, a hold on the **left** edge skips it.

Try the full walkthrough without looking: open the app, start the lesson,
pause, resume, repeat a unit, change speed, move between sections, jump into
the quiz, explore every option by sound and touch alone (each option has its own
counted buzz), select one, submit it, hear whether you were right, and get back
to another section.

The Debug screen (Settings → Debug Mode) mirrors live state — screen, semantic
unit, selected option, speech state/speed, last gesture and haptic — the
fastest way to check the gesture classifier while testing blind.

---

## TalkBack

Resonant's gestures are raw touches read by its own detector. With TalkBack on,
TalkBack owns the touch stream and Resonant's swipes/taps/holds never arrive, so
the app detects it at launch (and live, if toggled from Quick Settings) and
shows a notice with two ways forward:

1. **Turn TalkBack off** and use the gestures above.
2. **Keep TalkBack on** and use its **Actions menu** on any Resonant screen.
   `GestureSurface` exposes the grammar as custom actions — Next, Previous,
   Select, Continue, Back, Leave screen, Pause or resume, Repeat, Where am I,
   Faster speech, Slower speech. Each one is dispatched through the same
   handler as the real gesture, so every screen supports it with no extra code.

Known limits of path 2: Resonant's own voice and TalkBack's voice can overlap,
and it has not yet been tested against real TalkBack users. Whether a
direct-touch / pass-through mechanism could let the raw gestures coexist with
TalkBack is still an open investigation.

---

## Known prototype limitations

- Only one lesson and one quiz set are included (`content/*.kt` is where you'd
  add more).
- Chat keeps the whole conversation in memory and resends all of it each turn;
  there is no cap and nothing is persisted.
- The right-edge "contextual action" is intentionally minimal per the spec.
- TextToSpeech quality and voice depend entirely on the engine installed on the
  test device.
