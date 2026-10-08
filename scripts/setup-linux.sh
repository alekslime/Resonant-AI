#!/usr/bin/env bash
# One-time Linux setup for Arch / Omarchy. Safe to run again.
#   ./scripts/setup-linux.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
JDK17=/usr/lib/jvm/java-17-openjdk
KOKORO_URL=https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0

say() { printf '\n==> %s\n' "$*"; }

lan_ip() {
  ip -4 route get 1.1.1.1 2>/dev/null |
    awk '{for (i = 1; i < NF; i++) if ($i == "src") { print $(i + 1); exit }}'
}

find_sdkmanager() {
  local c
  for c in "$SDK/cmdline-tools/latest/bin/sdkmanager" \
           /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager \
           "$(command -v sdkmanager || true)"; do
    if [ -n "$c" ] && [ -x "$c" ]; then echo "$c"; return 0; fi
  done
  return 0
}

fetch() {  # fetch URL DEST (skips if DEST already exists)
  if [ -s "$2" ]; then return 0; fi
  curl -L --fail --progress-bar -o "$2.part" "$1" && mv "$2.part" "$2"
}

command -v pacman >/dev/null || { echo "This script is for Arch / Omarchy (needs pacman)."; exit 1; }

say "Packages (JDK 17, git, adb, uv, ollama)"
pkgs=(jdk17-openjdk git android-tools uv)
command -v ollama >/dev/null || pkgs+=(ollama)   # keeps ollama-cuda / ollama-rocm if you already have one
sudo pacman -S --needed --noconfirm "${pkgs[@]}"

say "Gradle uses JDK 17 (this Gradle/AGP combo breaks on the newest JDKs)"
mkdir -p "$HOME/.gradle"
if ! grep -qs '^org.gradle.java.home=' "$HOME/.gradle/gradle.properties"; then
  echo "org.gradle.java.home=$JDK17" >> "$HOME/.gradle/gradle.properties"
fi

say "Android SDK in $SDK"
SDKM="$(find_sdkmanager)"
if [ -z "$SDKM" ]; then
  command -v yay >/dev/null || {
    echo "No sdkmanager found and no yay. Install Android Studio (yay -S android-studio),"
    echo "let it set up the SDK, then run this script again."
    exit 1
  }
  yay -S --needed --noconfirm android-sdk-cmdline-tools-latest
  SDKM="$(find_sdkmanager)"
fi
mkdir -p "$SDK"
export JAVA_HOME="$JDK17"
yes | "$SDKM" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDKM" --sdk_root="$SDK" "platform-tools" "platforms;android-34" "build-tools;34.0.0" \
  "ndk;26.1.10909125" "cmake;3.22.1"

say "whisper.cpp v1.7.6 (offline speech-to-text, built into the app)"
W="$ROOT/app/src/main/cpp/whisper.cpp"
if [ ! -f "$W/CMakeLists.txt" ]; then
  git clone --depth 1 --branch v1.7.6 https://github.com/ggerganov/whisper.cpp.git "$W"
fi

say "Python agent (venv + Kokoro voice files)"
cd "$ROOT/agent"
[ -d .venv ] || uv venv --python 3.13 .venv
uv pip install --python .venv/bin/python -r requirements.txt
mkdir -p models
fetch "$KOKORO_URL/kokoro-v1.0.onnx" models/kokoro-v1.0.onnx
fetch "$KOKORO_URL/voices-v1.0.bin" models/voices-v1.0.bin

if [ ! -f .env ]; then
  key="$(.venv/bin/python -c 'import secrets; print(secrets.token_hex(24))')"
  ( umask 077; cat > .env <<ENV
LIVEKIT_URL=
LIVEKIT_API_KEY=
LIVEKIT_API_SECRET=
TOKEN_KEY=$key
ENV
  )
  NEED_ENV=1
fi

say "local.properties"
LAN="$(lan_ip)"
if [ -z "$LAN" ]; then LAN=192.168.1.50; echo "Could not detect your LAN address; using $LAN. Edit local.properties."; fi
LP="$ROOT/local.properties"
if [ ! -f "$LP" ]; then
  TOKEN_KEY="$(grep '^TOKEN_KEY=' "$ROOT/agent/.env" | cut -d= -f2-)"
  cat > "$LP" <<PROPS
ollama.baseUrl=http://$LAN:11434
ollama.model=llama3.2
live.tokenUrl=http://$LAN:8787/token
live.tokenKey=$TOKEN_KEY
PROPS
fi
cur="$(grep -E '^sdk\.dir=' "$LP" | cut -d= -f2- || true)"
if [ -z "$cur" ] || [ ! -d "$cur" ]; then   # also replaces a copied-over Windows path
  sed -i '/^sdk\.dir=/d' "$LP"
  echo "sdk.dir=$SDK" >> "$LP"
fi

if command -v ufw >/dev/null && sudo ufw status | grep -q 'Status: active'; then
  say "Firewall: letting your LAN reach the token server (8787) and Ollama (11434)"
  sudo ufw allow from "${LAN%.*}.0/24" to any port 8787,11434 proto tcp
fi

say "Done"
if [ -n "${NEED_ENV:-}" ]; then
  echo "Fill in LIVEKIT_URL / LIVEKIT_API_KEY / LIVEKIT_API_SECRET in agent/.env"
  echo "(or copy your existing agent/.env over it, then copy its TOKEN_KEY into live.tokenKey in local.properties)."
fi
echo "Then: ./dev.sh   and   ./gradlew :app:installDebug   (phone plugged in with USB debugging on)"
