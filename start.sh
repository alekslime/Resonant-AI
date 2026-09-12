#!/bin/bash
# Resonant — local dev startup
# Run from inside the project folder, with your venv already created:
#   python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt

cd "$(dirname "$0")"

# Kill anything still on port 5000 from a previous run (portable: lsof > fuser)
lsof -ti:5000 | xargs kill -9 2>/dev/null || true

source .venv/bin/activate 2>/dev/null || echo "No .venv found — run: python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt"

echo "Starting Resonant (fully local: Whisper + llama.cpp + Piper)..."
python3 server.py
