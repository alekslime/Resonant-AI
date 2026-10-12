# Contributing to Resonant

Thanks for caring about this. Resonant is a solo, source-available project (see
[LICENSE](LICENSE)). It is not open source, so **code contributions are not accepted
by default**. Feedback is very welcome, and it is the most useful thing you can give.

## What helps most

- **Try it with a screen reader or eyes-free, and tell me what broke.** Real feedback from blind
  and low-vision users matters more than anything else here. TalkBack testing in particular is
  still missing (see the README's "Known prototype limitations").
- **Bug reports.** Open an issue with:
  - phone model and Android version
  - what you did (gestures, which screen), what you expected, what happened
  - whether TalkBack was on, and which theme, font and speech engine you used
  - for Chat problems: whether Live mode or the phone-only path was in use
- **Ideas and wording.** Spoken sentences, hints and gesture explanations are easy to improve.
  Suggest the exact words you would like to hear.

## Please do not post

- Tokens, API keys, `TOKEN_KEY`, the contents of `agent/.env` or `local.properties`, or your
  home network addresses.
- Screenshots or recordings of other people's private study material.

## Code and pull requests

Please open an issue and ask first. If a change is agreed, the contribution has to be licensed
to the project owner in writing before it can be merged, because the project is not under an
open source license. Pull requests opened without that agreement will be closed, with thanks.

## Ground rules for anything that is agreed

These come from the project's own rules in `ROADMAP.md`:

- Eyes-free first: every state change needs a haptic, a sound or speech, never only a visual cue.
- New animation goes through `ActiveMotion` and must be off with Reduce motion.
- Shapes and sizes come from the theme (`cardShape()`, `controlShape()`, `MinTouchTarget`).
- Every new theme keeps its card, bar and button edges visible, and `ThemePaletteTest` must pass.
- Run `./gradlew :app:testDebugUnitTest` before sending anything, and say plainly what you did
  not test.
