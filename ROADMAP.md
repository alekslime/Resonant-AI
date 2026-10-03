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
- [ ] 1. Agent state -> dots DONE in #4; still to do: thinking/speaking haptic pulses
- [ ] 2. Detect a missing PC agent (about 8 s timeout -> announce, Offline dots)
- [ ] 3. Live exchanges into the normal Chat history (captions)
- [ ] 5. Speaker routing check: loudspeaker default, toggle, headset first
- [ ] 7. Settings: voice and speed, hold-to-talk mode, show which PC is connected

## Known issues
- First audio comes 3-7 s after you stop talking (Kokoro fp32 on CPU). Try the int8 model or a shorter first chunk.
- The token server is LiveKit's dev sandbox, fine for testing only.
