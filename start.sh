#!/bin/bash
cd "/home/aleks/res v1/res"

# Kill anything still on these ports
fuser -k 5000/tcp 2>/dev/null
fuser -k 8080/tcp 2>/dev/null

source .venv/bin/activate

echo "Starting Resonant..."
python3 server.py &
python3 -m http.server 8080 &
python3 agent.py dev &

echo "All running at http://localhost:8080 — Ctrl+C to stop."
wait
