# Phone test: the "Alive" pass and new content

Nothing here has been seen on a phone yet. About 25 minutes. Tick each line, and if it is wrong, change the number in the last column (smaller or bigger as said) and run `gradlew.bat installDebug` again.

## Before you start
- Install: `gradlew.bat installDebug` (installs over the old copy, data stays).
- Animation scale must be 1x: `"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" -s RZGL505B37Y shell settings get global animator_duration_scale` should print `1` or `1.0`.
- Settings > Reduce motion must be **off** for the first pass. Test it on at the end.
- Start with the default theme (Resonant native).

## 1. Focus ping (Home, Settings, Lessons, Quiz list)
Move focus with a two-finger swipe.
- [ ] A soft wave leaves the focused card once and fades in about half a second.
- [ ] It does not loop. The card is still afterwards.
- [ ] Not too faint, not too big, not distracting.

| Wrong | Change (`ui/components/Enter.kt`, `GlowTiming`) |
|---|---|
| Too faint / too strong | `START_ALPHA` 0.26 (up / down) |
| Too small / too big | `MAX_SPREAD_DP` 12 |
| Too fast / too slow | `PING_MS` 520 |

## 2. Springs (press and focus)
Press and hold a card, then let go. Move focus around Home.
- [ ] The card gives a little and springs back with a hair of overshoot.
- [ ] It never wobbles or feels bouncy.

| Wrong | Change (`ui/components/Press.kt`, `SpringFeel`) |
|---|---|
| Too bouncy | `PRESS_DAMPING` 0.6, `FOCUS_DAMPING` 0.55 (raise towards 1.0) |
| Too stiff or flat | lower the same two (not below 0.4) |

## 3. Background drift (Home, Settings, Profile)
Look at a plain area for 15 to 20 seconds.
- [ ] Very faint darker patches slowly wander. No big colour change.
- [ ] Lessons, Quiz and Chat: do they drift too, or look flat? (Flat means that screen paints its own background. Tell me which.)

| Wrong | Change (`ui/components/ResonantSurface.kt`, `AmbientMotion`) |
|---|---|
| Can't see it / too visible | `PATCH_ALPHA` 0.08 |
| Patches too small / large | `RADIUS_SHARE` 0.9 |
| Too fast / too slow | `PERIOD_A_S` 150 and `PERIOD_B_S` 210 (bigger = slower) |

## 4. Profile (Home > Profile)
- [ ] Your name appears as one braille cell per letter, all of them, shrinking if the name is long.
- [ ] Each cell's dots light one after another, then one light runs along the name.
- [ ] The numbers count up from zero once.
- [ ] A "Milestones: N of 14" row. Tap it: it speaks what you reached and the next goal.
- [ ] First open after the update: about two seconds in, a haptic and one spoken line about new milestones. Leave and come back: it must **not** repeat.
- [ ] The text and the focus cards look right with a long name.

| Wrong | Change |
|---|---|
| Cells arrive too slowly | `AvatarMotion.CELL_GAP_MS` 110, `CELL_MS` 520 (`ui/profile/ProfileMotion.kt`) |
| Shimmer too slow or fast | `ProfileMotion.SWEEP_MS` 1400 (`ui/profile/ProfileSummary.kt`) |
| Count-up too slow | the `900` in `ProfileScreen.kt` (`countProgress.animateTo`) |
| Celebration comes too early or late | the `1800` delay in `ProfileScreen.kt` |
| Milestone thresholds | the list in `ui/profile/Milestones.kt` |

## 5. Chat (open Chat, talk or type)
- [ ] Empty chat: "Hi name!" rises in softly.
- [ ] When it changes between listening, thinking and speaking, a soft wave leaves the dots once.
- [ ] No ring spam when the state flickers.
- [ ] Swipe between exchanges: the answer crossfades. While an answer streams in, the text must **not** flicker.

| Wrong | Change (`ui/components/DotsPing.kt`, `DotsPingTiming`) |
|---|---|
| Too faint / strong | `START_ALPHA` 0.22 |
| Too many pings | `MIN_GAP_MS` 1200 (bigger = fewer) |
| Crossfade too slow | the `250` in `ChatScreen.kt` (`Crossfade`) |

## 6. Themes (Settings > Color theme, tap to cycle)
| Theme | Expect |
|---|---|
| Resonant native, Blue on yellow, White on blue | Metropolis font, ping, drift |
| Warm ambiance, Soft cream, Paper gray | Lexend font, softer ping, fainter slower drift |
| Photophobia, Night red | Lexend font, **no ping, no drift**, calm presses |
| Low vision, Cataracts, Handsfree | Atkinson font, **no ping, flat background, a thick steady ring** around the focused card |
| Monochrome | Metropolis font, no ping, flat background, thick ring |

- [ ] The thick ring is clearly visible and does not clash with the yellow outline of Low vision.
- [ ] Fonts and spacing change with the theme, until you pick a font yourself. After you pick one in Settings, it stays through theme changes.

| Wrong | Change |
|---|---|
| Ring too thick / thin | `focusRingDp` in `CLEAR` (`ui/theme/MotionProfile.kt`) |
| A theme should move more or less | its group in `MotionProfiles.forTheme` |
| A theme's font or spacing | `ui/theme/ThemeTypeDefaults.kt` |

## 7. Reduce motion (Settings > Reduce motion: on)
- [ ] Nothing animates: no ping, no drift, no shimmer, no count-up, no springs, no crossfade.
- [ ] The thick ring in the Clear themes **stays** (it is not motion).
- [ ] Spoken cues and haptics still work.

## 8. Quiz results
Finish any quiz.
- [ ] No bar. The score is already there, and the card rises in softly.
- [ ] The hint line under the card rises in after it.
- [ ] A perfect score: a second, brighter tap just after the first. Anything below perfect: one tap only.
- [ ] The spoken result is not delayed.
- [ ] Reduce motion on: no rise, no second tap delay problems (the second tap still comes).

| Wrong | Change |
|---|---|
| Second tap too late or too early | the `650` in `QuizResultsScreen.kt` |
| Card rises too slowly | `EnterTiming` in `ui/components/Enter.kt` |

## 9. Lesson finished
Open a lesson and let it play to the end (do it once in Auto and once in Manual by tapping through).
- [ ] After the **last sentence has been read out**, a bright tap and "Lesson finished. Long press the right edge to go back."
- [ ] A "Lesson finished" card rises in at the bottom and the page scrolls down to it.
- [ ] It does **not** fire while the last sentence is still being read, or while paused.
- [ ] It happens once per visit. Swipe back through the lesson and reach the end again: no second celebration.
- [ ] Profile afterwards counts the lesson as finished (Milestones row, "First lesson finished" the first time).

| Wrong | Change (`ui/lessons/LessonScreen.kt`) |
|---|---|
| Fires too early | check `audio.isSpeaking` handling; tell me which mode |
| Scroll to the card too fast or slow | the `450` and the `delay(120)` |

## 10. New content
Lessons list and Quiz list now have 18 of each.
- [ ] The lists scroll and focus moves through all of them, nothing cut off at 200% text size.
- [ ] Open three new lessons: nothing reads oddly (numbers, "l y" in the parts of speech lesson, "Newton's").
- [ ] Offline: turn the PC off and ask Chat about a new topic ("tell me about evaporation"). The answer comes from the lesson.
- [ ] Offline, ask something with no lesson: the "I don't have a lesson on that" reply now lists every topic and is long. Note if it is too long.
- [ ] Quizzes: the right answer is not always in the same place; each explanation reads well.

## Report back
Send me: what felt wrong, on which screen and theme, and a screenshot if it is a layout problem. If something crashed: `adb -s RZGL505B37Y logcat -d -s AndroidRuntime:E`.
