#!/bin/bash
# Resonant — local dev startup
# Run from inside the project folder, with your venv already created:
#   python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt

cd "$(dirname "$0")"

# Kill anything still on this port from a previous run
fuser -k 5000/tcp 2>/dev/null

source .venv/bin/activate 2>/dev/null || echo "No .venv found — run: python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt"

echo "Starting Resonant (fully local: Whisper + llama.cpp + Piper)..."
python3 server.py
