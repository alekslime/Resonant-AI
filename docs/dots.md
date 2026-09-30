# Resonant dots

The animated braille "R" on the Chat screen (and the splash). Six dot slots make one braille
cell; the logo is dots 1-2-3-5. Code: `ui/components/ResonantDots.kt`. Everything else in the app
uses the static mark (`BrailleRMark` in `ResonantScaffold`).

## States and what triggers them (Chat)

Priority is top to bottom; the first match wins.

| State | Shown when | Animation |
|---|---|---|
| Transcribing | mic session is active **and** the recording has ended, waiting for text | tight, fast orbit |
| Listening | mic session active (from tap until the result) | R drifts and sways; the right "ear" dot reaches out with your mic level |
| Thinking | a question was sent, no reply sentence yet | four dots orbit, speed-ramped, never reverses |
| Speaking | Resonant is talking: a reply sentence **or** an announcement, held 600 ms between sentences | dots ride a wave with squash/stretch, driven by the speech level |
| Offline | idle, and the AI server is unreachable (`serverUp == false`) | R with the ear dot cut loose, drifting and flickering |
| Idle | anything else | spells R-e-s-o-n-a-n-t in braille, eased, with speed ramps |

State selection lives in `ChatScreen.kt` (`realDotsState`).

## Where the level comes from

`ResonantDots(level = { ... })` is read every frame; it returns 0..1, or a negative number for
"none" (the animation then uses its built-in baseline / fake envelope).

- **Speaking, replies and announcements:** real PCM loudness. `AudioManager` synthesizes each
  utterance with `synthesizeToFile`, and `PcmSpeaker` plays the WAV on its own `AudioTrack`. The
  level is looked up from the track's playback-head position, so it matches what you hear, and is
  normalised per utterance (independent of voice, rate and volume). Code: `audio/Pcm.kt`
  (WAV parse + loudness windows), `audio/PcmSpeaker.kt`, `AudioManager.speechLevel()`.
  Short announcements are cached per text+speed, so repeats start with no synthesis delay.
  If synthesis or decoding fails, that utterance falls back to plain TTS and the level is a
  word-timed estimate (`onRangeStart`).
- **Listening:** live mic loudness. `AudioRecorder` (Whisper path) maps -50..-10 dB to 0..1 per
  chunk; the fallback `SpeechRecognizer` maps `onRmsChanged` (-2..10 dB) to 0..1.
  `VoiceInputController.micLevel()` returns whichever is active.
- **Transcribing signal:** `WhisperSpeechInputManager.transcribing` (recording ended -> result)
  and `SpeechInputManager.transcribing` (end of speech -> result).

## Debug switcher

Settings > Debug Mode > swipe left in the middle of the screen turns on "Dots preview" (it
announces on/off). Chat then shows a chip strip above the Ask pill: Auto plus every state. Once a
state is forced, "Sim level" uses the built-in fake level; tap it to drive the level with the
slider instead. In-memory only: it resets when the app restarts.

## Tuning

- Mic range: `levelOf` in `AudioRecorder.kt` (-50..-10 dB) and the `onRmsChanged` mapping.
- Idle word timing: `HOLD` / `TRANS` in `ResonantDots.kt` (slow-fast-slow speed ramp).
- Splash speed: `SPLASH_SPEED` in `SplashScreen.kt` (idle playback multiplier).
- Speaking hold between sentences: the `delay(600)` in `ChatScreen.kt`.
- Attack/release smoothing of the level: `rate` in `DotsEngine.frame`.

## Tests

`DotsEngineTest` (pose math: finite in every state, idle lands on each braille letter, no jumps
when switching states, splash grow-in, reduce-motion) and `PcmTest` (WAV parsing, loudness
normalisation). Both are pure JVM: `./gradlew testDebugUnitTest`.

## Known limits

- Each sentence is fully synthesized before it plays, so there is a small gap per sentence
  (announcements are cached after the first play).
- With "remove animations" on, the dots hold a static pose.
- The dots are hidden from TalkBack on purpose; state is already spoken and felt through haptics.
