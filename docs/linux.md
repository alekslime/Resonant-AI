# Running and developing on Linux (Arch / Omarchy)

Nothing in the Kotlin or Python code is Windows-specific. Linux only needed its own
versions of `dev.ps1` and the one-time setup.

## First time

```bash
./scripts/setup-linux.sh
```

Installs JDK 17, adb, uv and Ollama; the Android SDK pieces this project pins (platform 34,
build-tools 34.0.0, NDK 26.1.10909125, CMake 3.22.1); clones whisper.cpp v1.7.6; builds
`agent/.venv` (Python 3.13); downloads the Kokoro voice files; writes `local.properties`;
opens ports 8787 and 11434 to your LAN if `ufw` is active.

Then put your LiveKit values in `agent/.env` (or copy your existing `agent/.env` over it and
copy its `TOKEN_KEY` into `live.tokenKey` in `local.properties`).

## Every day

```bash
./dev.sh                                # Ollama (if needed) + token server + agent; Ctrl-C stops them
./gradlew :app:installDebug             # build and install on the USB-connected phone
./gradlew :app:testDebugUnitTest        # unit tests, no device needed
adb logcat --pid=$(adb shell pidof com.resonant.app)
```

Phone: enable Developer options and USB debugging. If `adb devices` says `no permissions`,
install `android-udev`. Wi-Fi alternative: Wireless debugging, then `adb pair` / `adb connect`.

## Editor

- Android Studio: `yay -S android-studio`. If the window opens blank on Hyprland, start it with
  `_JAVA_AWT_WM_NONREPARENTING=1`. Set Gradle JDK to 17 (Settings > Build > Gradle).
- Or any editor plus `./gradlew`; the project does not need Studio.

## Gotchas

- **JDK**: Gradle 8.7 / AGP 8.5.2 do not run on the newest JDKs. The setup script pins JDK 17 in
  `~/.gradle/gradle.properties`.
- **Ollama must listen on the LAN.** `dev.sh` starts it correctly. If the systemd service is
  already running it will say so and print the override needed.
- **Clock**: LiveKit rejects tokens when the clock is off. `dev.sh` warns if NTP is not synced.
- **Phone address**: your PC's LAN IP can change. Fix it on the phone (Settings > Debug Mode >
  swipe right > Server setup) instead of rebuilding.
- **Emulator**: `abiFilters` is arm only, so use a real phone (see ROADMAP).
- **GPU for Ollama**: setup installs the CPU `ollama`. For a GPU install `ollama-cuda` or
  `ollama-rocm` first and setup will keep it.
- `agent/requirements.txt` is unpinned, so a fresh venv gets the newest LiveKit packages. If the
  agent ever breaks after a fresh setup, run `uv pip freeze` in a working venv and pin it.
