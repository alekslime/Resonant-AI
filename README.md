# Resonant — Interaction Prototype

A native Android (Kotlin + Jetpack Compose) prototype demonstrating a
**non-spatial interaction model**: navigate lessons, listen to content,
answer quizzes, and get feedback without ever needing to visually locate
a button.

No backend, no network, no auth, no database, no AI — everything is local
and hardcoded, and the app runs fully offline.

---

## How to open and run it

This project was built in a sandboxed environment without network access,
so the Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) is **not**
included — it couldn't be downloaded here. To run the project:

1. Open the `Resonant/` folder in **Android Studio** (Koala/2024.1 or newer
   recommended, since this targets AGP 8.5.2 / Kotlin 1.9.24 / compileSdk 34).
2. When Android Studio opens a wrapper-less Gradle project, it will offer to
   regenerate the wrapper automatically — accept that, or run
   `gradle wrapper --gradle-version 8.7` yourself if you have a system Gradle
   install.
3. Let Gradle sync (it will download the AGP/Kotlin/Compose/Navigation
   dependencies listed in `app/build.gradle.kts`).
4. Run the `app` configuration on a device or emulator running **API 26+**.

The app requests the `VIBRATE` permission only (declared in the manifest,
no runtime prompt needed on modern Android for this permission).

---

## The interaction model, in code

| Concept | Where it lives |
|---|---|
| Gesture zones (LEFT_EDGE / CENTER / RIGHT_EDGE) + raw gesture detection | `gestures/InteractionZone.kt`, `gestures/GestureManager.kt` |
| Centralized speech (semantic-unit queue, pause/resume/repeat/speed) | `audio/AudioManager.kt` |
| Centralized haptics (named pattern vocabulary, one place to retune) | `haptics/HapticPattern.kt`, `haptics/HapticManager.kt` |
| Hardcoded lesson / quiz / chat content | `content/LessonData.kt`, `content/QuizData.kt`, `content/ChatData.kt` |
| Shared gesture-surface + debug wiring used by every screen | `ui/components/GestureSurface.kt` |
| The seven screens | `ui/home`, `ui/lessons`, `ui/quiz`, `ui/chat`, `ui/settings` |
| Debug/developer mode | `ui/settings/DebugScreen.kt` (Settings → Debug Mode) |

**Every screen attaches the same `GestureSurface`.** That's the mechanism
that keeps the gesture grammar identical everywhere — no screen invents its
own touch handling. `GestureManager.kt` is the only file that talks to raw
pointer input; it classifies touches into `ResonantGesture`s (taps, double
taps, long presses, swipes, the left-edge hold-to-adjust-speed stream, and
the universal three-finger gestures) based on **relative** zone boundaries
(percentage of current width), so nothing depends on fixed screen
coordinates or a specific orientation.

`AudioManager` treats all spoken content — lesson sections, quiz prompts
and options, chat replies — as a queue of small `SemanticUnit`s with a
current index. Lessons and the chat mockup auto-advance through their
queue; quiz screens use the identical `next()`/`previous()` primitives to
let you *explore* answer options, which is what makes "browse the home
menu," "move through a lesson," and "explore quiz answers" all feel like
the same underlying action.

`HapticManager` is the only class that touches the system `Vibrator`. Every
pattern is a named `HapticPattern` enum value with its millisecond timing
defined in one map (`HapticPatterns.timings`) — change a vibration by
editing one line there, nothing else.

---

## Testing it "with the screen ignored"

The gesture grammar is the same on every screen:

- **Swipe up / down** in the center — next / previous (menu item, lesson
  unit, quiz option, chat chunk).
- **Swipe right** in the center — continue / submit (context-dependent).
- **Swipe left** in the center — back (context-dependent; also jumps a
  lesson section backward).
- **Double-tap** in the center — select / confirm the focused item.
- **Single tap, left edge** — pause / resume speech.
- **Hold, left edge, then drag up/down** — adjust speech speed in six
  steps (0.75×–2.0×), with an ascending/descending tactile pattern as
  confirmation.
- **Long-press, right edge** — contextual action (in the Lesson screen,
  this exits back to the lesson list, standing in for a future contextual
  menu).
- **Three-finger tap, anywhere** — repeat the current semantic unit.
- **Three-finger hold, anywhere** — "Where am I?" — speaks the current
  screen, section/question position, and playback state.

Try the full walkthrough from the spec: open the app, start the lesson,
pause, resume, repeat a unit, change speed, move between sections, jump
into the quiz, explore every option by sound and touch alone, select one,
submit it, hear whether you were right, and get back to another section —
without looking at the screen.

The Debug screen (Settings → Debug Mode) mirrors live state — current
screen, semantic unit, selected option, speech state/speed, and the last
gesture and haptic fired — which is by far the fastest way to verify the
gesture classifier is doing what you expect while you're testing blind.

---

## Known prototype limitations

- Only one lesson and one quiz set are included (binary search / a short
  general-knowledge quiz) — `content/*.kt` is where you'd add more.
- The right-edge "contextual controls" and "contextual action menu" are
  intentionally minimal per the spec ("don't overload this area") — for
  now they mostly just demonstrate that the zone and gesture are wired up.
- TextToSpeech quality/voice depends entirely on the TTS engine installed
  on the test device.
