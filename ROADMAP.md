# Resonant roadmap (for AI agents)

Read this first, keep it short, tick items off as you finish them.

## What this is
Android app (Kotlin/Compose). It is built to work eyes-free: gestures, haptics, spoken
announcements and sound cues. **Chat is always Live**: `Routes.CHAT`, `ChatScreen(liveMode = true)`.
There is one mode, no text/live switch. The user talks or types, and the PC agent's Kokoro voice
speaks every reply.

    voice: phone mic -> LiveKit Cloud -> agent/agent.py on the user's PC
    typed: phone pill -> LiveKit (attribute + data packet "text") -> same agent turn
      Silero VAD -> faster-whisper -> Ollama (llama3.2) -> Kokoro TTS -> back to the phone

If the agent is not there (never joins within 12 s, or Offline), typed text falls back to
phone -> Ollama directly + the phone's own TTS voice. Typing right after opening Chat waits for the
agent to join instead of falling back.

## Where things are
- `app/.../ui/chat/ChatScreen.kt` - Live UI and lifecycle (`askTyped`, `askTypedOnPhone`)
- `app/.../livekit/LiveKitManager.kt`, `LiveKitState.kt` - the only LiveKit code on Android (`sendText`)
- `app/.../haptics`, `app/.../sound` - every `haptics.play(pattern)` also plays its sound cue
- `app/.../ui/components/ResonantDots.kt` - `DotsState` animation
- `agent/agent.py` - PC agent (`submit`, `submit_text`, `text_signal`). Venv in `agent/.venv`
- `agent/token_server.py` - gives the phone a LiveKit token (port 8787)
- `dev.ps1` - repo root. Starts Ollama, token server and agent in three windows. `dev.sh` is the Linux version (one terminal); `scripts/setup-linux.sh` is the one-time Linux setup, see `docs/linux.md`
- Design system (the "Design pass" below): `ui/theme/ThemePalette.kt` (colors, outlines, card/button radius per theme), `ThemeBorders.kt` (`cardBorder`, `controlBorder`, `cardShape()`, `controlShape()`), `Dimens.kt` (`MinTouchTarget`), `Motion.kt` (`ActiveMotion`), `ui/components/Press.kt` (`pressScale`), `ui/components/FocusFade.kt` (`focusColor`)
- `app/.../gestures/GestureManager.kt` - the touch detector. `GestureSurface` options: `twoFingerSwipe` (a vertical swipe needs two fingers, one finger scrolls), `contentScrolls` (read touches before a scrolling list; defaults to `twoFingerSwipe`), `ignoreChildTaps` (a tap an item handled is not also a centre tap)

## Rules
- Small steps. One commit per step, `git push` after every commit. Prefix: `feat:` `fix:` `docs:`.
- Never commit `agent/.env`, `agent/models/`, `.venv`, keys or tokens.
- LiveKit code stays behind `LiveKitManager`. No new screens: Live stays in Chat.
- Eyes-free first: every state change needs a haptic, sound or spoken cue, not only a visual one.
- The app's own speech must never play while the live mic is open (the agent would answer it).
- Free services only.
- **Motion goes through `ActiveMotion`.** Never read `Settings.Global.ANIMATOR_DURATION_SCALE` directly. Use `ActiveMotion.reduce` or `ActiveMotion.ms(base)` (0 when reduced). Every new animation must be off or instant with Reduce motion, and must never be the only signal (haptic/sound/speech still carry it). No looping, bouncing or idle motion.
- **Shapes and sizes come from the theme.** Cards: `cardShape()` + `cardBorder(...)`. Buttons and bars: `controlShape()` + `controlBorder(...)`. Anything you tap is at least `MinTouchTarget` (56 dp). Do not hard-code `RoundedCornerShape(28.dp)` or `RoundedCornerShape(50)`.
- **Every new theme must keep its card, bar and button edges visible** (3:1 against the screen, or an outline). `ThemePaletteTest` checks it; the default theme is exempt on purpose (Figma brand look).
- Test on the phone after each step. Say plainly what was not tested.

## Checklist
- [x] LiveKit connection layer, PC agent (VAD -> STT -> Ollama -> Kokoro), mic handling
- [x] Agent state -> dots, status line, cues; missing-agent detection (12 s); exchanges in Chat history
- [x] Swipe-to-review in Live, tap to interrupt, speaker routing, voice and speed in Settings
- [x] Hold-to-talk (Settings > Live mic), PC name shown, own token server
- [x] 9. One mode: typed text goes to the agent and Kokoro speaks it (confirmed on the phone)
- [x] 10. `dev.ps1` starts the three PC processes
- [x] Linux (Arch/Omarchy): `dev.sh`, `scripts/setup-linux.sh`, `docs/linux.md`. Built, tested and installed on the phone from Linux
- [x] Scrolling: one finger scrolls; a two-finger vertical swipe moves focus on Home, Lessons, Lesson, Quiz list and Settings. Quiz questions, Lesson mode and Chat still use one finger. Tutorial and README updated
- [x] Tapping an item opens that item (not the focused one) on Home, Lessons, Quiz list, Settings and Lesson mode
- [x] `OfflineAnswersTest` fixed (it now answers from the binary search lesson only)
- [x] 11. Faster Chat open: agent keeps one warmed process (`num_idle_processes=1`) and warms Kokoro in `prewarm`. Not tested yet: compare the time from opening Chat to "Live. Connected"
- [x] Practice area (sandbox) after the tutorial lessons: every gesture is answered out loud with what it means, nothing is acted on. Swipe right twice to finish, left-edge hold leaves. `GestureExplainer.kt` + test. Not compiled or run on a device yet
- [x] Spoken hints when the user is stuck: 25 s of silence, or 3 gestures in a row that played no haptic cue. `GestureSurface(hints, hint)`, `HintEngine.kt` + test, `HapticManager.playCount`. Off in Chat (live mic must not hear the app) and the tutorial. Not compiled or run on a device yet
- [x] Spatial cues: with headphones, cues pan a little left/right (`CuePan.kt`, `HeadphoneDetector.kt`, `AudioTrack.setStereoVolume`), speech stays centred, no panning on the speaker or with Mono audio on. No setting yet. Not tested on a device
- [x] Tutorial and practice area teach pinch and four-finger rewind: two new lessons (ten in all), `GestureSurface(onRewind)` lets a screen take over the rewind, `explainPinch` / `explainRewind` in `GestureExplainer.kt` + tests. Explainer tested on the JVM; screens not compiled or run on a device yet
- [x] Pause keeps its place: `AudioManager.resume()` plays the paused unit's audio from where it stopped (`ResumePlanner.kt` + test). Plain-TTS fallback, a speed change or Repeat still start the unit again. Planner tested on the JVM; not compiled or run on a device yet
- [x] Theme preview in Settings: hold the middle on Color theme, Font or Text spacing, swipe left/right to preview live, tap keeps, right-edge hold cancels (`SettingsScreen.kt`, `OptionPicker.kt` + test). No detector change. Tap still cycles; TalkBack keeps tap-to-cycle (no entry action yet). Pure part tested on the JVM; not compiled or run on a device yet
- [-] "Spoken replies on/off" setting: cancelled, the user wants speech always
- [x] Design pass step 1: `ActiveMotion` (one answer to "should this animate?": Settings switch OR Android remove-animations). `ui/theme/Motion.kt`, `ResonantPrefs.reduceMotion`, set in `ResonantContainer`
- [x] Step 2: Splash, `ResonantDots` and the quiz score bar use `ActiveMotion`. Splash is now ~2.7 s (user asked 2-3 s): `SPLASH_SPEED`, `HOLD_MS`, `EXIT_MS`; with Reduce motion it shows still for 1.5 s then fades
- [x] Step 3: Settings > "Reduce motion" row (tap toggles, spoken, CONFIRM haptic, saved)
- [x] Step 4: per-theme `cardRadius` / `controlRadius` and outlines in `ThemePalette` (defaults 28 dp / pill). Low vision 12/14 + yellow outline, Cataracts 20/20, Monochrome 4/4 + black outline, Handsfree 8/8, Photophobia 36/pill, Warm 24/pill
- [x] Step 5: every card and pill on every screen uses `cardShape()` / `controlShape()` (Lessons, Lesson mode, Quiz list, Quiz question and results, Settings, Onboarding). Circles and the score bar stay as they are
- [x] Step 6: `MinTouchTarget = 56.dp` in `Dimens.kt`; chat settings circle, Lessons back pill and settings circle, ask-pill mic and buttons use it as a floor
- [x] Step 7: `pressScale` (97% while pressed, 120 ms) on `ResonantButton`, Lessons back pill, lesson cards, quiz cards, Lesson mode cards; quiz result colors fade in over 200 ms
- [x] Step 8: outlines on Photophobia, Warm, Night red, Blue on yellow, Soft cream, Paper gray, White on blue (their own ink faded to 3.5:1 against the screen, 2 dp) and Cataracts controls; two new tests in `ThemePaletteTest`
- [x] Step 9a: the focused card is drawn inverted (ink fill, onInk text, button flipped) on the Lessons list, Quiz list and Lesson mode, and the lists plus Settings scroll the focused item into view (`BringIntoViewRequester`). Before this, focus on those lists was only spoken and could sit off screen
- [x] Step 9b: `focusColor()` fades focus changes over 150 ms (instant with Reduce motion) on Home, Settings, Lessons, Quiz list and Lesson mode
- [ ] Step 10: screen transitions. Short crossfade (~150 ms, no sliding) between screens through `ActiveMotion.ms`. Read `ui/nav/Navigation.kt` first; do not change the routes or the Live chat rules
- [ ] Step 11: progress fill. Lesson and quiz progress shapes fill smoothly. The quiz score bar already does (step 2). Grep for other progress UI first; skip what does not exist
- [ ] Step 12: dots settle. Check `DotsEngine` first: `frame()` already eases a state change over 0.9 s (`ease(...)`), so this may already be done. Only change it if a state visibly jumps; keep `DotsEngineTest` green
- [ ] Step 13: tip and banner fades. `TtsUnavailableBanner` uses `AnimatedVisibility(fadeIn(), fadeOut())` with the default duration: put it on `ActiveMotion.ms(...)`. Other banners and spoken-hint text only if they exist
- [ ] Step 14: README and docs. README.md has the Color themes / Font sections: add Reduce motion, outlines and radius, the inverted focus card, `MinTouchTarget`, press and focus fades, then `docs:` commit. Remove the two unused `colors` variables (QuizResultsScreen.kt, SettingsScreen.kt) while there
- [ ] Phone check of the design pass (the user said they would look): Photophobia and Night red outlines (may be too bright), the 97% press (too subtle or too much?), the inverted focus card in Low vision and Cataracts (very bright), and the 150-200 ms fades (contrast dips briefly while two colors blend; shorten if muddy)
- [ ] Test by talking (voice, hold-to-talk, interrupt, swipe-review). Never run on a device
- [ ] Make `LiveKitManager` lazy (only matters for the x86 emulator, see below)

## Design pass brief (from the user, 2026-10-11)
- One cohesive design system: consistent spacing, type, buttons, cards, icons, motion.
- Themes should feel like different experiences, not color swaps. So far only radius and outlines differ. Ideas not done: per-theme spacing or type weight, a different focus style per theme (for example a thick outline for Handsfree), a default font per theme.
- Accessible defaults: strong contrast, readable text, comfortable touch targets, nothing communicated by color alone.
- Reduced stimulation: restrained motion plus a setting to reduce it. Themes for light sensitivity.
- Personality without noise: satisfying feedback, no constant bouncing, flashing or decoration.
- The user wants it **one step at a time**: one small step, then wait. Full files, not diffs. Exact `mv` and `git add / commit / push` lines (files land in `~/Downloads`; the user is on Omarchy, repo `~/Resonant-AI`). After a batch the user runs `./gradlew :app:compileDebugKotlin` and `./gradlew :app:testDebugUnitTest`.
- Steps 1-9b above compile and pass the unit tests on the user's machine. They have not been checked on a phone unless the user says so.

## Known issues and gotchas
- **Scroll-to-focus ignores Reduce motion.** `bringIntoView()` always glides. A fix would scroll with `ScrollState.scrollTo` when `ActiveMotion.reduce`; not done.
- The press cue is not on Home or Settings rows (they carry hold and focus logic) or on the chat circles. Left out on purpose.
- Quiz result, focus and press fades blend two colors for a moment, so contrast dips briefly. Watch for it on the phone.
- The default (orange) theme is exempt from the edge-contrast tests: white cards on orange measure 1.86:1 but are the Figma brand look.
- **Debug signing key differs per machine.** Installing over a build made on another machine fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Fix: copy `debug.keystore` between machines (Windows `C:\Users\<you>\.android\`, Linux `~/.android/`), or `adb uninstall com.resonant.app` (wipes the app's saved data).
- **Gesture layer rule.** A screen with items you can tap AND a vertical swipe needs `ignoreChildTaps = true`, otherwise the row's own click and the centre tap both fire and the focused item wins. A screen whose list scrolls needs `twoFingerSwipe = true` or one finger can never scroll.
- **PC clock must be right.** A wrong clock makes LiveKit reject the token ("Connection error", Logcat `Could not fetch region settings: 401`). Test: get a token from `http://localhost:8787/token` and call `https://<project>.livekit.cloud/settings/regions` with it; 200 is good. Fix: Windows time settings, set time and time zone automatically, Sync now.
- The phone needs the token address `<PC Wi-Fi IPv4>:8787` and the same `TOKEN_KEY` as `agent/.env` (Settings > Debug Mode > swipe right > Server setup). Port 8787 needs a Windows firewall rule (Administrator PowerShell). The PC's address can change after a reboot.
- Empty token fields = LiveKit's dev sandbox (testing only). Plain http token URLs work in debug builds only; a release build needs https.
- First audio comes 3-7 s after you stop talking (Kokoro fp32 on CPU). The user chose fp32 for the most natural voice. Do NOT add `kokoro-v1.0.int8.onnx` (the agent uses it automatically if it exists).
- `abiFilters` is arm64/armeabi-v7a only and `ResonantApp` creates `LiveKitManager` at startup, so an x86_64 emulator likely crashes on launch. The phone is fine.
- Hold-to-talk needs the current `agent.py`. TalkBack cannot do a hold, so use always-open mic with TalkBack.
- Gestures that speak (three-finger hold, speed gestures, left-edge pause) still talk while the live mic is open.
- Voice ids in `ResonantPrefs.LIVE_VOICES` may not all exist in `voices-v1.0.bin`; the agent logs `unknown voice` and keeps the old one.

## Setup facts (the user's PC, 2026-10-07)
- Windows 11, PowerShell, Python 3.13. Two repo copies: `C:\Users\aleks\Resonant-AI` (agent, `.venv`, `.env`) and `C:\Users\aleks\AndroidStudioProjects\Resonant-AI` (what Android Studio builds). Ask which is the main one.
- `app/src/main/cpp/whisper.cpp` (tag v1.7.6) is cloned locally and not in git; a fresh clone needs the command in `app/src/main/cpp/CMakeLists.txt`.
- `local.properties` has `ollama.baseUrl` and `ollama.model`. The server addresses are saved on the phone (Settings > Debug Mode > swipe right > Server setup).
- Ollama must listen on the network: `$env:OLLAMA_HOST="0.0.0.0"` (or `setx OLLAMA_HOST "0.0.0.0"` once). Phone and PC on the same Wi-Fi.
- Linux (Arch/Omarchy, `~/Resonant-AI`): `./scripts/setup-linux.sh` once, then `./dev.sh`. JDK 17 is pinned in `~/.gradle/gradle.properties`, Python 3.13 comes from uv, `dev.sh` starts Ollama on `0.0.0.0`, the setup script opens ports 8787 and 11434 in `ufw`. Details in `docs/linux.md`.
- Run (Windows): `.\dev.ps1` from the repo root, then Android Studio: Sync, Run on the phone. If Windows blocks the script: `Unblock-File .\dev.ps1`.

## How the user wants work delivered (follow it)
- Short, direct answers. They push back on long or repeated replies and on over-design. Plainest implementation unless asked otherwise.
- Hand over COMPLETE changed files with their repo path to paste over, not diffs, not a zip.
- One commit per file or step, with the `git add` / `git commit -m` / `git push` lines. Prefixes `feat:` `fix:` `docs:`. Dependencies first.
- Code comment-light, plain English names, no reformatting of existing files.
- Browsers drop the leading dot of downloaded files (`.env.example` arrives as `env.example`): remind them to rename it.
- Say plainly what was not tested. Never claim a device run happened.
- Do not bring up key rotation again; the user knows.=