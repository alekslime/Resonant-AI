#!/bin/bash
cd "$(dirname "$0")"

fuser -k 5000/tcp 2>/dev/null
sleep 0.5

if [ -d ".venv" ]; then
  source .venv/bin/activate
fi

# ── System dependency: espeak-ng ─────────────────────────────────────
if ! command -v espeak-ng &>/dev/null; then
  echo "Installing espeak-ng (text-to-speech engine)..."
  sudo apt install -y espeak-ng
fi

# ── Ollama ───────────────────────────────────────────────────────────
if ! curl -s http://localhost:11434 > /dev/null; then
  echo "Starting Ollama..."
  ollama serve &>/dev/null &
  sleep 3
fi

echo "Checking model..."
ollama pull llama3.2:3b 2>/dev/null || true

# ── Start server ─────────────────────────────────────────────────────
echo ""
echo "Starting Resonant..."
python3 server.py &
SERVER_PID=$!
sleep 2

echo "✓ Running at http://localhost:5000"

if [[ "$1" == "--ngrok" ]]; then
  if ! command -v ngrok &>/dev/null; then
    echo "ngrok not found: https://ngrok.com/download"
  else
    ngrok http 5000 --log=stdout &
    NGROK_PID=$!
    sleep 2
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | python3 -c \
      "import sys,json; print(json.load(sys.stdin)['tunnels'][0]['public_url'])" 2>/dev/null)
    [ -n "$NGROK_URL" ] && echo "Public URL: $NGROK_URL"
  fi
fi

echo "Press Ctrl+C to stop."

cleanup() {
  kill $SERVER_PID 2>/dev/null
  kill $NGROK_PID 2>/dev/null
  fuser -k 5000/tcp 2>/dev/null
}
trap cleanup EXIT INT TERM
wait $SERVER_PID
