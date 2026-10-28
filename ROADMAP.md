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
- Motion and life (the "Alive" items below): `ui/components/Enter.kt` (`enterStagger`, `focusLift`, `focusGlow` = the resonance ping, `GlowTiming`), `ui/components/Press.kt` (`pressScale`, `SpringFeel`), `ui/components/DotsPing.kt` (ping behind the dots), `ui/components/ResonantSurface.kt` (`AmbientMotion`, the slow background drift), `ui/theme/MotionProfile.kt` (per-theme motion personality: standard, gentle, quiet, clear), `ui/theme/ThemeTypeDefaults.kt` (per-theme starting font and spacing; `ActiveFont.chosen` / `ActiveSpacing.chosen` say whether the person picked)
- Profile: `ui/profile/ProfileScreen.kt`, `ProfileSummary.kt` (`ProfileMotion`, `BrailleAvatar`), `ProfileMotion.kt` (`CountUp`, `AvatarMotion`), `Milestones.kt` (rules), `content/MilestoneStore.kt` (which milestones were already celebrated)
- `app/.../gestures/GestureManager.kt` - the touch detector. `GestureSurface` options: `twoFingerSwipe` (a vertical swipe needs two fingers, one finger scrolls), `contentScrolls` (read touches before a scrolling list; defaults to `twoFingerSwipe`), `ignoreChildTaps` (a tap an item handled is not also a centre tap)

## Rules
- Small steps. One commit per step, `git push` after every commit. Prefix: `feat:` `fix:` `docs:`.
- Never commit `agent/.env`, `agent/models/`, `.venv`, keys or tokens.
- LiveKit code stays behind `LiveKitManager`. No new screens: Live stays in Chat.
- Eyes-free first: every state change needs a haptic, sound or spoken cue, not only a visual one.
- The app's own speech must never play while the live mic is open (the agent would answer it).
- Free services only.
- **Motion goes through `ActiveMotion`.** Never read `Settings.Global.ANIMATOR_DURATION_SCALE` directly. Use `ActiveMotion.reduce` or `ActiveMotion.ms(base)` (0 when reduced). Every new animation must be off or instant with Reduce motion, and must never be the only signal (haptic/sound/speech still carry it). No bouncing, and nothing that loops fast or flashes. Idle motion is allowed only when it is very slow (a lap of minutes), faint, and off with Reduce motion (the background drift). A theme can turn motion down further through `MotionProfile`; the light-sensitive and low-vision themes stay still.
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
- [x] Practice area (sandbox) after the tutorial lessons: every gesture is answered out loud with what it means, nothing is acted on. Swipe right twice to finish, left-edge hold leaves. `GestureExplainer.kt` + test. Tested on the phone: works
- [x] Spoken hints when the user is stuck: 25 s of silence, or 3 gestures in a row that played no haptic cue. `GestureSurface(hints, hint)`, `HintEngine.kt` + test, `HapticManager.playCount`. Off in Chat (live mic must not hear the app) and the tutorial. Tested on the phone: works
- [x] Spatial cues: with headphones, cues pan a little left/right (`CuePan.kt`, `HeadphoneDetector.kt`, `AudioTrack.setStereoVolume`), speech stays centred, no panning on the speaker or with Mono audio on. No setting yet. Tested on the phone: works
- [x] First-launch setup before the tutorial: theme, font, text size, speaking speed, name (`ui/onboarding/SetupScreen.kt`; gesture mapping in `SetupGestures`, tested in `SetupFlowTest`). Tried on the phone
- [x] Profile screen (Home, between Quiz and Settings): name in braille as the avatar, quiz and material counts, spoken "How am I doing?", theme and font change in place (`ui/profile/`, `ProfileSummaryTest`). Tried on the phone
- [x] `ListGestures.kt`: the list/menu gesture grammar as one tested function (`ListGesturesTest`); Profile uses it
- [x] Voice: "profile", "my progress", "stats", "account", "how am I doing" open Profile (`agent/voice_commands.py`, `agent/test_voice_commands.py`; run `python -m unittest test_voice_commands` in `agent/`). Not tested on the phone yet
- [x] Home scrolls with one finger and moves focus with two, like Lessons. Not tested on the phone yet
- [x] Tutorial restyled like the setup: big dots (talk while a prompt plays, bounce on a right gesture, dip on a wrong one), a segment per lesson, and a touch layer that shows the zones and the fingers (`TouchLayer` in `OnboardingScreen.kt`; it only watches, so it cannot change a gesture). Not tested on the phone yet
- [x] Streaks and listening time on Profile (`content/ActivityLog.kt` rules, `ActivityStore.kt` storage, `ActivityLogTest`): a day counts after a minute of speech or a finished lesson (last sentence reached) or quiz; a streak stays alive until a day is missed; lessons finished and total listening shown; included in the spoken "How am I doing?". Not tested on the phone yet
- [x] Pinch changes the text size of the whole app, on every screen that has a gesture surface (`GestureSurface(pinchResize = true)` by default; `ActiveTextScale` + `TextScaleProvider` at the theme; range 75% to 250%; saved). Off in the tutorial and setup. Home, Lesson mode and the quiz score are capped (`CappedTextScale`). Not tested on the phone yet: look for text that overflows at 200% and 250% on every screen
- [x] Settings has a "Text size" row (tap = next size up, wraps; hold the middle to preview) for anyone who cannot pinch; presets live in `TextScale.PRESETS`, shared with the setup step. Not tested on the phone yet
- [x] Animations: staggered list arrival, focus lift, Home focus colour fade, screens scale in with the fade (`ui/components/Enter.kt`). Not tested on the phone yet
- [x] Tutorial and practice area teach pinch and four-finger rewind: two new lessons (ten in all), `GestureSurface(onRewind)` lets a screen take over the rewind, `explainPinch` / `explainRewind` in `GestureExplainer.kt` + tests. Explainer tested on the JVM; screens tested on the phone: work
- [x] Pause keeps its place: `AudioManager.resume()` plays the paused unit's audio from where it stopped (`ResumePlanner.kt` + test). Plain-TTS fallback, a speed change or Repeat still start the unit again. Planner tested on the JVM; not compiled or run on a device yet
- [x] Theme preview in Settings: hold the middle on Color theme, Font or Text spacing, swipe left/right to preview live, tap keeps, right-edge hold cancels (`SettingsScreen.kt`, `OptionPicker.kt` + test). No detector change. Tap still cycles; TalkBack keeps tap-to-cycle (no entry action yet). Pure part tested on the JVM; tested on the phone: works
- [-] "Spoken replies on/off" setting: cancelled, the user wants speech always
- [x] Design pass step 1: `ActiveMotion` (one answer to "should this animate?": Settings switch OR Android remove-animations). `ui/theme/Motion.kt`, `ResonantPrefs.reduceMotion`, set in `ResonantContainer`
- [x] Step 2: Splash, `ResonantDots` and the quiz score bar use `ActiveMotion`. Splash is now ~2.7 s (user asked 2-3 s): `SPLASH_SPEED`, `HOLD_MS`, `EXIT_MS`; with Reduce motion it shows still for 1.5 s then fades
- [x] Step 3: Settings > "Reduce motion" row (tap toggles, spoken, CONFIRM haptic, saved)
- [x] Step 4: per-theme `cardRadius` / `controlRadius` and outlines in `ThemePalette` (defaults 28 dp / pill). Low vision 12/14 + yellow outline, Cataracts 20/20, Monochrome 4/4 + black outline, Handsfree 8/8, Photophobia 36/pill, Warm 24/pill
- [x] Step 5: every card and pill on every screen uses `cardShape()` / `controlShape()` (Lessons, Lesson mode, Quiz list, Quiz question and results, Settings, Onboarding). Circles and the score bar stay as they are
- [x] Step 6: `MinTouchTarget = 56.dp` in `Dimens.kt`; chat settings circle, Lessons back pill and settings circle, ask-pill mic and buttons use it as a floor
- [x] Step 7: `pressScale` (97% while pressed, 120 ms) on `ResonantButton`, Lessons back pill, lesson cards, quiz cards, Lesson mode cards; quiz result colors fade in over 250 ms
- [x] Step 8: outlines on Photophobia, Warm, Night red, Blue on yellow, Soft cream, Paper gray, White on blue (their own ink faded to 3.5:1 against the screen, 2 dp) and Cataracts controls; two new tests in `ThemePaletteTest`
- [x] Step 9a: the focused card is drawn inverted (ink fill, onInk text, button flipped) on the Lessons list, Quiz list and Lesson mode, and the lists plus Settings scroll the focused item into view (`BringIntoViewRequester`). Before this, focus on those lists was only spoken and could sit off screen
- [x] Step 9b: `focusColor()` fades focus changes over 250 ms (instant with Reduce motion) on Home, Settings, Lessons, Quiz list and Lesson mode
- [x] Step 10: screen transitions in `ui/nav/Navigation.kt`: 250 ms fade, no sliding, through `ActiveMotion.ms` (`SCREEN_FADE_MS`; was 150, stretched after the phone check). The arriving screen fades in over the old one; on Back the leaving screen fades out over the one under it, so the window behind never shows through. Routes and Live chat rules untouched. Compiles and tests pass; not checked on a phone
- [-] Step 11: progress fill. Skipped, nothing to fill: the only progress UI drawn is the quiz score bar (already animated in step 2; the bar was removed on 2026-10-26). Lesson and quiz screens only speak their position, and the quiz list's progress is spoken, not drawn (`ProgressBarBackground` / `ProgressBarFill` in `QuizBrowserScreen.kt` are unused)
- [x] Step 12: dots settle. Already done, no code change: `DotsEngine.frame()` snapshots the current pose on every state change and eases to the new one over 0.9 s (`ease`), also when a change lands mid-ease, and the audio level is smoothed (fast attack, slow release). `DotsEngineTest.switchingStatesNeverJumps` covers every state pair (max 60 units per frame). Not watched on a phone
- [x] Step 13: `TtsUnavailableBanner` fades in and out over `ActiveMotion.ms(250)` (instant with Reduce motion). It is the only `AnimatedVisibility` in the app; there are no other banners or tip texts to fade (hints are spoken only). Not checked on a phone
- [x] Step 14: README has new Motion, Shape/outline/touch size and Focus paragraphs after Color themes (existing text untouched); the two unused `colors` variables (and their now-unused `LocalResonantColors` imports) are gone from `QuizResultsScreen.kt` and `SettingsScreen.kt`
- [x] Splash redesign (from the user's HTML prototype): dots fly in and gather, orbit, land one by one in the R and pulse, rings ripple out, RESONANT and "Learn by listening." rise in. Flat theme colors (`BrandOrange` / `BrandInk`), no gradient. `ui/splash/SplashMotion.kt` (pure, tested), `SplashScreen.kt`, `sound/SplashSound.kt` (synthesized soundtrack, `SoundCueManager.playSplash()`, honours Sound cues), a CONFIRM buzz per landing (vibration only). About 2.9 s of animation (`SplashSound.SPEED` 1.35) then a 1.5 s hold (`HOLD_MS`) so the wordmark can be read; Reduce motion shows the finished mark still for 1.5 s, no sound. The old braille spell-out on the splash is gone (`ResonantDots` itself is untouched). Home and the tutorial are prepared under the splash but stay silent until the fade starts: `AudioManager.holdPlayback()` / `releasePlayback()` (synthesis runs, playback waits; self-releases after 8 s), released by `SplashScreen(onExitStart)`. Tested on the phone: works
- [x] Own material 1: `content/MaterialParser.kt` turns text or Markdown into a `Lesson` (headings -> sections, sentences -> units, Markdown cleaned for speech, code blocks announced, long sections split into parts) + `MaterialParserTest`. Tested on the phone: works
- [x] Own material 2: `MaterialStore` (files in app storage + an index; add, list, delete). Done in earlier commits
- [x] Own material 3: intake of .txt / .md: share sheet, file picker in Settings and in Chat, spoken result (`MaterialImporter`). Done in earlier commits
- [x] Own material 4: Lessons list shows the person's material after the built-in lessons (`LessonLibrary`). Done in earlier commits
- [x] Own material 5: Chat answers from the chosen material, on the phone path and on the PC agent. Done in earlier commits
- [x] Own material 6: PDFs: `PdfTextExtractor` (pdfbox-android) + `PdfTextCleaner` (page numbers, repeated headers, hyphenation, ligatures) feed the same `MaterialImporter` (`isPdf`, `decodePdfText`, results `PdfLocked` / `PdfNoText`, 30 MB limit; text files stay at 2 MB). Pickers and the share filter accept `application/pdf`. A PDF with no text (a scan) is told so out loud until step 7. Pure parts tested on the JVM; the PDF library itself tested on the phone: works
- [x] Own material 7: pictures. `ImageTextReader` (ML Kit text recognition, bundled model, works offline; photos made upright and shrunk to 1568 px) reads the words; `OllamaClient.describeImage` asks a vision model (`ollama.visionModel`, default `llama3.2-vision`) to describe graphs, tables, diagrams and photos for a listener, with the recognised words as a hint. `ImageMaterial` (pure) builds the material: "What the picture shows" then "Text in the picture". If the server or vision model is unreachable only the words are kept, and the spoken result says so. A scanned PDF (no real text) now has its first 30 pages rendered and read the same way (words only). Pickers and the share filter accept `image/*`; a picture is announced as "Reading the picture. This can take a minute." (20 MB limit). Pure parts tested on the JVM; ML Kit, PdfRenderer and the vision call tested on the phone: work
- [x] Own material 8: graphs and figures inside PDFs. `PdfFigures` (pure) picks pages with a large embedded picture (`PdfTextExtractor.read` counts them, >= 200 x 120 px) or a "Figure N" / "Chart N" caption, or for scans a caption or almost no words; at most 6. `ImageTextReader.renderPagesJpeg` renders them, `MaterialImporter.describeFigures` asks the vision model about each page (answers NONE when there is no figure; two failures in a row stop the loop), and `PdfFigures.compose` puts each explanation right after its page, announced as "Figure on page N." `PdfTextCleaner.cleanPages` keeps page numbers. Picking and composing tested on the JVM; PDFBox image counting, PdfRenderer and the model calls tested on the phone: work
- [X] Own material 9: Chat answering about the picture itself, not just its saved description; describing just the figure, not the whole page; catching uncaptioned vector charts in text PDFs
- [X] Phone check of the design pass (the user said they would look): Photophobia and Night red outlines (may be too bright), the 97% press (too subtle or too much?), the inverted focus card in Low vision and Cataracts (very bright). (The fades were checked on the phone 2026-10-11: all stretched to 250 ms and the user likes it; the animation scale must be 1x to judge them. Still to look at: the outlines, the press and the focus card.)
- [X] Test by talking (voice, hold-to-talk, interrupt, swipe-review).
- [-] Make `LiveKitManager` lazy: skipped, the phone is the only target and the gain is a little startup time. Revisit only if the x86 emulator is needed (one line in `ResonantApp`: `by lazy`)
- [x] Firewall: `scripts/allow-lan.sh` opens 8787 and 11434 to every private range (10.x, 172.16-31.x, 192.168.x), called from `setup-linux.sh`; `dev.sh` warns when the PC address changed or `local.properties` holds another one. Script logic tested here against a stand-in `ufw`; not run on a real firewall
- [x] Spoken Live connection errors: `LiveErrors` (pure, tested) tells timeout, refused, unknown host, wrong token key, bad token address, token server crash, no internet and a refused token apart; `LiveKitState.Error` carries what is said and the short status text. Pure part tested on the JVM; not yet checked on the phone by breaking each thing on purpose

## Alive (2026-10-22 to 26)
Compiles and the unit tests pass on the PC. None of it has been seen on a phone yet.
- [x] Resonance ping: when focus lands on a card, a soft filled wave in the theme's ink leaves its edge, spreads about 12 dp and fades in about 0.5 s, once (`focusGlow`, `GlowTiming`; through `focusLift`). It replaced a pulsing glow the user did not like. Off with Reduce motion
- [x] Profile avatar: one braille cell per letter, shrinking to fit the width (`BrailleAvatar.cellWidthDp`); each cell's raised dots light one after another, then one light travels along the name (`AvatarMotion`, `ProfileMotion.shine`); the numbers count up once (`CountUp`). Off with Reduce motion
- [x] Ambient background: two very faint patches of ink drift across `ResonantSurface`, a lap takes 2.5 to 3.5 minutes (`AmbientMotion`, 8 percent ink per patch). Position refreshed 10 times a second. Off with Reduce motion. Check it is visible on every screen: Lessons, Quiz and Chat may paint their own background
- [x] Light springs on presses and focus lifts (`SpringFeel`). Screen transitions stay 250 ms fades
- [x] Per-theme motion personality (`MotionProfile`): standard, gentle (Warm ambiance, Soft cream, Paper gray), quiet (Photophobia, Night red: no ping, no drift, no bounce), clear (Low vision, Cataracts, Handsfree, Monochrome: no ping, flat background, a steady 4 dp ring around the focused card, which stays with Reduce motion)
- [x] Profile milestones: 14 of them (`Milestones`), a "Milestones: N of 14" row that speaks what was reached and the next goal, and a haptic plus one spoken line the first time new ones are reached (`MilestoneStore` remembers which). The first open after the update celebrates everything already reached
- [x] Chat: a ping behind the dots when the conversation changes state (`DotsPing`, skipped if closer than 1.2 s to the last), the answer crossfades when you move to another exchange (keyed on the exchange, not its text, which grows while it streams), the greeting rises in. Off with Reduce motion and in the quiet and clear themes
- [x] Per-theme starting font and text spacing (`ThemeTypeDefaults`): Low vision and Cataracts and Handsfree start with Atkinson, the calm themes with Lexend, the native one with Metropolis. A font or spacing the person picks wins in every theme. Known edge: cancelling a hold-to-preview of the font in Settings leaves the font pinned as a pick
- [x] Quiz results (2026-10-26): the score bar and the count-up are gone (the user did not want the bar fill). The score shows at once, the card and the hint rise in (`enterStagger`), a perfect score adds a second, brighter haptic (`HapticPattern.CORRECT`, 650 ms after the first). Nothing is spoken later. Unused `ProgressBarBackground` / `ProgressBarFill` removed from `QuizBrowserScreen.kt`. Not seen on a phone
- [x] Lesson finished moment (`LessonScreen.kt`): reaching the last sentence still marks the lesson finished at once (streaks, milestones); once that sentence has been read out to the end (not while paused, once per visit) there is a `CORRECT` haptic, the spoken line "Lesson finished. Long press the right edge to go back.", and a "Lesson finished" card rises in at the bottom and scrolls into view (instant with Reduce motion). Not seen on a phone. Watch for: firing too early or twice, or never in manual mode
- [x] More built-in content (`content/MoreContent.kt`, `MoreContentTest.kt`): 12 new lessons (water cycle, fractions, sorting, solar system, sound, the heart, percentages, ancient Egypt, parts of speech, continents and oceans, Newton's laws, digestion; five sections each, keywords for offline answers) and 14 quiz sets (one for each, plus binary search and plate tectonics). `LessonData.allLessons` and `QuizData.allQuizSets` append them. Now 18 lessons and 18 quizzes. The "no lesson on that" offline reply reads every lesson title, so it is long now: shorten it
- [ ] Per-theme focus styles beyond the clear themes' ring (shape, thickness). Wait until the current ones have been seen on a phone
- [ ] Phone check of all of the above: ping size and speed, drift visible, springs not too soft, the thick ring against the yellow outline of Low vision, Photophobia still, fonts per theme, milestones on first open, Chat ping and crossfade

## On-phone AI (2026-10-27)
Chat can answer with no PC: PC agent -> Ollama -> language model on the phone -> lesson text. Target: Galaxy A17 4G (Helio G99, 4 GB). **Written and compiled on a PC only: never run on a phone, and the Android build has not been run.**
- [x] `llama/` Gradle module (own CMake project, because whisper.cpp and llama.cpp both define `ggml`). llama.cpp is cloned, not committed: `cd llama/src/main/cpp && git clone --depth 1 --branch b11904 https://github.com/ggml-org/llama.cpp.git`. arm64 only
- [x] `engine.cpp` (no JNI) keeps what the model already read, so question two only reads what is new. Built and run on a PC against a tiny random-weight model: load, reuse, stop, cancel (also while reading the prompt), too-long prompt
- [x] `phoneai/`: `PhoneAiSupport.kt` + `PhoneAiSupportTest` (18 tests, ran on the JVM), `PhoneAiModelManager.kt` (resumable download of Qwen2.5-1.5B-Instruct Q4_K_M, about 1 GB, from Qwen's Hugging Face repo), `PhoneAi.kt`
- [x] Settings > "On-phone AI" downloads it. Chat: tap the centre uses the phone's own Whisper when the PC agent is missing (`phoneVoiceNow`)
- [ ] First run on the A17: does it load, tokens per second (Logcat tag `ResonantPhoneAi`: `prompt= reused= firstWord= speed=`), time to the first word with and without material, thread count (`PhoneAi.loadEngine`: try 2, 3, 4)
- [ ] The model URL has never been fetched from the build machine (blocked there): check the download works
- [ ] If too slow: `Qwen2.5-0.5B` or `Llama-3.2-1B` (one filename in `PhoneAiModelManager`); `PhoneAiPrompt.SYSTEM_CHARS` smaller; warm the lesson text when Chat opens
- Known: the first question with a long lesson reads about 750 tokens, which on this processor may take 20 s or more. Hold-to-talk has no phone fallback yet (tap works)

## Meta hackathon (2026-10-28)
Meta Global AI Developer Hackathon, Indie track, submissions due 2026-11-04 21:00 ET (about 03:00 on 5 Nov in Albania). Judging: Impact 30, Use of Meta AI tooling 25, Demo 25, Technical depth 20. Numbers come from third-party summaries: read the Official Rules.
- [x] Cloud client (`network/OpenAiProtocol.kt` pure + tested, `OpenAiClient.kt`): OpenAI-style chat to the Meta Model API (`https://api.meta.ai/v1`, `muse-spark-1.3`). Key and URL in `local.properties` (`cloud.apiKey`, `cloud.baseUrl`, `cloud.model`), compiled into your own build only. Chat order: cloud -> PC/Ollama -> phone model -> lesson text. Cloud sees up to 120,000 characters of the material (`MaterialContext.CLOUD_MAX_CHARS`). Pictures and PDF figures are described by the cloud model first. A wrong key or missing billing switches cloud off until the app restarts (`CloudAiConfig.disabledReason`)
- [ ] **Blocked: Meta billing.** `/chat/completions` returns `billing_not_configured`. The team must finish https://dev.meta.ai/billing/ (and see whether the $150 credit appears)
- [ ] Not yet run against the real API: streaming shape, `reasoning_effort: low`, `max_completion_tokens`, picture input as an `image_url` data URL (Meta's docs do not show it; check "Image understanding")
- [ ] Muse Voice Transcribe as a speech engine (`POST /v1/asr/transcribe`, request format not found; realtime websocket: handshake JSON then 24 kHz PCM16 frames)
- [ ] Open-weight option: Muse Glimmer 30B on Baseten (H100, OpenAI-compatible: base URL `https://model-<id>.api.baseten.co/environments/production/sync/v1`, model `meta-models/Muse-Glimmer-30B`). Same client: only the three `cloud.*` lines change
- [ ] Modal proxy so a demo APK needs no key; demo video; short write-up; test with a blind or low-vision student; confirm the teammate was accepted individually

## Gesture and content fixes (2026-10-28)
Compile and the unit tests pass on the PC. Not checked on a phone.
- [x] A two-finger vertical swipe was read as a pinch when the fingers drifted apart (the gap changed by more than 48 px), so only a perfectly parallel swipe worked. Now a pinch must change the gap by more than 1.5 times the distance the pair moved together (`GestureClassifier.PINCH_DOMINANCE`, `isPinch(..., centroidTravel)`), and once the pair has clearly moved together it is locked as a swipe for the rest of that touch (`isTwoFingerPan`, `twoFingerPanning` in `GestureManager.kt`). Tests in `GestureClassifierTest`. Not tested on the phone yet: crooked and diagonal two-finger swipes on Home, Lessons and Settings; a real pinch still resizes text; the tutorial pinch lesson. If swipes still read as a pinch raise `PINCH_DOMINANCE` to 2 or 2.5; if pinching got hard, lower it toward 1. Known edge: a pair that moves more than `TAP_MAX_DRIFT_PX` together before the pinch starts is locked as a swipe
- [x] `LessonData.allLessons` and `QuizData.allQuizSets` had lost their `+ MoreContent.all` / `+ MoreContent.allQuizzes` (the app showed 6 lessons and 4 quizzes and two `MoreContentTest` tests failed). Restored
- [x] Offline answers: "evaporation" added to the keywords of the water cycle's Evaporation section; "equivalent" removed from the Percentages section so "what are equivalent fractions" finds the Fractions section (`MoreContentTest`)
- [ ] Swipe direction uses the first finger only, so a diagonal two-finger swipe can read as horizontal. Idea: use the midpoint of both fingers and require vertical to win by about 1.2 times on the list screens. Only if the phone check shows wrong directions

## Design pass brief (from the user, 2026-10-11)
- One cohesive design system: consistent spacing, type, buttons, cards, icons, motion.
- Themes should feel like different experiences, not color swaps. So far only radius and outlines differ. Done since: per-theme motion (`MotionProfile`) and per-theme starting font and spacing (`ThemeTypeDefaults`). Ideas not done: a different focus shape or thickness per theme.
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
- **Gesture layer rule.** A screen with items you can tap AND a vertical swipe needs `ignoreChildTaps = true`, otherwise the row's own click and the centre tap both fire and the focused item wins. A screen whose list scrolls needs `twoFingerSwipe = true` or one finger can never scroll. Pinch versus two-finger swipe: judged by how far the gap between the fingers changes compared with how far the pair travels together, never by the gap alone (fingers are never perfectly parallel).
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
- Since 2026-10-22 the user also builds on Windows (repo `C:\Users\aleks\Resonant-AI`, `gradlew.bat`, cmd or PowerShell: cmd does not understand `$var` or `&`). Give `git` lines in that syntax, and use `git --no-pager diff` (the pager traps people).
- Before the user replaces a big or shared file, say which one and ask for `git diff` if they may have changed it: an old copy once dropped `ProfileMotion` and broke the tests.
- Do not bring up key rotation again; the user knows.=