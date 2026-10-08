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
- `app/.../gestures/GestureManager.kt` - the touch detector. `GestureSurface` options: `twoFingerSwipe` (a vertical swipe needs two fingers, one finger scrolls), `contentScrolls` (read touches before a scrolling list; defaults to `twoFingerSwipe`), `ignoreChildTaps` (a tap an item handled is not also a centre tap)

## Rules
- Small steps. One commit per step, `git push` after every commit. Prefix: `feat:` `fix:` `docs:`.
- Never commit `agent/.env`, `agent/models/`, `.venv`, keys or tokens.
- LiveKit code stays behind `LiveKitManager`. No new screens: Live stays in Chat.
- Eyes-free first: every state change needs a haptic, sound or spoken cue, not only a visual one.
- The app's own speech must never play while the live mic is open (the agent would answer it).
- Free services only.
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
- [-] "Spoken replies on/off" setting: cancelled, the user wants speech always
- [ ] Test by talking (voice, hold-to-talk, interrupt, swipe-review). Never run on a device
- [ ] Make `LiveKitManager` lazy (only matters for the x86 emulator, see below)

## Known issues and gotchas
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