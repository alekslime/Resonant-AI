# Resonant roadmap (for AI agents)

Read this first, keep it short, tick items off as you finish them.

## What this is
Android app (Kotlin/Compose). It is built to work eyes-free: gestures, haptics, spoken
announcements and sound cues. **Live mode is the existing Chat screen** (`Routes.CHAT`,
`ChatScreen(liveMode = true)`), not a separate screen.

Live pipeline, all free and local except the LiveKit relay (LiveKit Cloud free tier):

    phone mic -> LiveKit Cloud -> agent/agent.py on the user's PC
      Silero VAD -> faster-whisper -> Ollama (llama3.2) -> Kokoro TTS -> back to the phone

## Where things are
- `app/.../ui/chat/ChatScreen.kt` - Live UI and lifecycle
- `app/.../livekit/LiveKitManager.kt`, `LiveKitState.kt` - the only LiveKit code on Android
- `app/.../haptics`, `app/.../sound` - every `haptics.play(pattern)` also plays its sound cue
- `app/.../ui/components/ResonantDots.kt` - `DotsState` animation (Idle, Listening, Transcribing, Thinking, Speaking, Offline)
- `agent/agent.py` - PC agent. Python venv in `agent/.venv`. Run: `python agent.py dev`

## Rules
- Small steps. One commit per step, `git push` after every commit. Prefix: `feat:` `fix:` `docs:`.
- Never commit `agent/.env`, `agent/models/`, `.venv`, keys or tokens.
- LiveKit code stays behind `LiveKitManager`. No new screens: Live stays in Chat.
- Eyes-free first: every state change needs a haptic, sound or spoken cue, not only a visual one.
- The app's own speech must never play while the live mic is open (the agent would answer it).
  Open the mic only after announcements finish.
- Free services only.
- Test on the phone after each step (`.\gradlew.bat installDebug`, `python agent.py dev`).
  Say plainly what was not tested.

## Checklist (do in this order)
- [x] LiveKit connection layer, connect/disconnect owned by Chat
- [x] PC agent: VAD -> STT -> Ollama -> Kokoro voice
- [x] Live mic handling: mute toggle, no on-device Whisper in Live
- [x] 6. Feel and sound pass: mic-open cue, dots follow live state, error cue (thinking/speaking pulses come with #1)
- [x] 4. Tap to interrupt: while she thinks or speaks a tap stops her, otherwise a tap mutes (agent publishes state; voice barge-in later)
- [x] 1. Agent publishes its state -> dots, status line, vibration-only thinking/speaking/your-turn cues
- [x] 2. Detect a missing PC agent (12 s timeout -> announce, Offline dots; also if it drops later)
- [x] 3. Live exchanges into the normal Chat history (captions on screen, saved). Not done: swipe-to-review in Live (the app speech queue stays empty there)
- [x] 3b. Swipe-to-review past exchanges in Live: first swipe mutes the mic and reads the latest reply, up/down moves, tap center unmutes. Ignored while she is thinking or speaking. Not compiled or run yet
- [x] 5. Speaker routing: user reports the voice already plays from the loudspeaker, so no code change. If that ever breaks: set a preferred device list (speakerphone before earpiece) on LiveKit's AudioSwitchHandler
- [x] 7a. Voice and speed in Settings (cycle on tap, saved in prefs). Phone sends them on connect as attributes `voice`/`speed` plus a `voice` data packet; the agent validates and applies them, `KOKORO_VOICE`/`KOKORO_SPEED` stay as defaults. Takes effect next time Live opens. Not compiled or run yet
- [x] 7b. Hold-to-talk: Settings > Live mic. Hold the center (500 ms) to open the mic, release to send; a tap interrupts or says how. The phone sends `ptt` start/end (attribute + data packet, numbered) because a muted mic gives the agent no silence to detect. Mode is sent on connect as `mode`. Not compiled or run yet
- [x] 7c. Show which PC is connected: the agent publishes `host` (its computer name); the phone says it in the Live intro, adds it to the status line and to the three-finger hold. An older agent without it just skips the name (1 s wait). Not compiled or run yet
- [x] 8. Own token server: `agent/token_server.py` (run it next to the agent; same `.env` plus `TOKEN_KEY`). Set `live.tokenUrl` and `live.tokenKey` in `local.properties` and rebuild; empty = dev sandbox as before. Fresh room per connection, key checked, 1 h tokens. The server was run and called locally; the app side is not compiled or run yet

## Known issues
- First audio comes 3-7 s after you stop talking (Kokoro fp32 on CPU). Done in code, untested: the agent uses `agent/models/kokoro-v1.0.int8.onnx` when it exists (download it next to the fp32 file), and the first spoken piece may end at a comma after 5 words. Compare the `first audio` log line before and after.
- The token server is LiveKit's dev sandbox unless `live.tokenUrl` is set (see #8), fine for testing only.

## Resuming (older note, still true)
Everything above is untested until the user confirms a device run. Before starting a new item,
ask which items compiled and worked on the phone, and fix those first.

## Handoff (written 2026-10-03, end of a long session)

State: every checklist item above is coded. NONE of it has been compiled or run on the phone or PC
since 3b was added; the Android side was never built by the previous agent (no toolchain in its
sandbox). The user will test later. First job for the next agent: ask what compiled and what worked,
and fix compile errors before anything new.

Coded this session, in order, all untested on a device:
- 3b swipe-to-review in Live (`ChatScreen.kt`: `liveReviewing`, `liveReview()`)
- 7a Live voice and speed in Settings (`ResonantPrefs`, `SettingsScreen`, `LiveKitManager.sendVoiceSettings`, agent `apply_settings`)
- 7b hold-to-talk (new `ResonantGesture.LongPressEnd` in `gestures/`, `Settings > Live mic`, `LiveKitManager.pushToTalk`, agent `ptt_signal`/`_ptt_finish`)
- 7c PC name (agent attribute `host`, `LiveKitManager.agentHost`, intro/status line/three-finger hold)
- first-audio speedups: int8 Kokoro model preferred if present, first spoken piece may end at a comma (`split_chunk` in `agent.py`)
- 8 own token server (`agent/token_server.py`, `live.tokenUrl`/`live.tokenKey` in `local.properties`)

Most likely to break (check these first):
- Kotlin compile errors in `ChatScreen.kt` (edited many times by string replacement; the exhaustive `when` in `GestureSurface.kt` now needs `LongPressEnd`, already added) and in `SettingsScreen.kt` (`labelFor` now takes 6 arguments).
- Voice ids in `ResonantPrefs.LIVE_VOICES` may not all exist in the user's `voices-v1.0.bin`; the agent logs `unknown voice` and keeps the old one. Remove the bad ones.
- Hold-to-talk needs the NEW `agent.py`. An old agent never ends a held turn. TalkBack cannot do a hold, so hold mode is unusable with TalkBack on (always-open mode is fine).
- The token server: the user still has to put `TOKEN_KEY` in `agent/.env`, run `python token_server.py`, and set `live.tokenUrl` (their PC's LAN address, port 8787) and `live.tokenKey` in `local.properties`, then rebuild. Until `live.tokenUrl` is set the app uses the dev sandbox, so nothing is forced. Do not write their keys, secrets or IP into any committed file.
- The user pasted their LiveKit API secret into chat once; they were told to rotate it.

Not done / ideas (none are on the user's list yet, ask before starting):
- No on-phone screen for the token address (rebuild needed), unlike the Ollama address in Debug > Server setup.
- Plain http token URL works in debug builds only; a release build needs https.
- Gestures that speak (three-finger hold, speed gestures, left-edge pause) still talk while the live mic is open.
- Hold-to-talk waits the shared 500 ms long-press time; a shorter centre-only threshold would mean touching `GestureManager.kt`.
- If first audio is still slow after the int8 model: try a smaller `WHISPER_MODEL`.

How the user wants work delivered (they said so; follow it):
- Hand over COMPLETE changed files, each with its repo path to paste over, not diffs or find/replace blocks, and not a zip.
- Give as many separate commits as there are files or steps, each with the `git add` / `git commit -m` / `git push` lines. Prefixes `feat:` `fix:` `docs:`. Commit order must keep dependencies first.
- Keep answers short and direct. The user pushes back on long or repeated replies.
- Code stays comment-light with plain English names, no reformatting of existing files.
- Windows 11, PowerShell, repo at `Resonant-AI` on GitHub. Browsers drop the leading dot of downloaded files (`.env.example` arrived as `env.example`): remind them to rename dotfiles.
- Say plainly what was not tested. Never claim a device run happened.

