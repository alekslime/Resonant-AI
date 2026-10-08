#!/usr/bin/env bash
# Linux version of dev.ps1: Ollama (if not running), token server and agent, in one terminal.
# Ctrl-C stops everything it started.
set -uo pipefail
set -m   # each background job gets its own process group, so Ctrl-C can stop its children too

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENT="$ROOT/agent"
PY="$AGENT/.venv/bin/python"
[ -x "$PY" ] || { echo "Missing $PY. Run scripts/setup-linux.sh first."; exit 1; }

pids=()
stopping=
cleanup() {
  trap - INT TERM EXIT
  stopping=1
  echo; echo "Stopping..."
  for p in "${pids[@]}"; do kill -INT -- "-$p" 2>/dev/null; done
  sleep 3
  for p in "${pids[@]}"; do kill -TERM -- "-$p" 2>/dev/null; done
  wait 2>/dev/null
}
trap cleanup INT TERM EXIT

start() {  # start NAME DIR COMMAND...
  local name=$1 dir=$2; shift 2
  ( cd "$dir" && exec "$@" ) > >(sed -u "s/^/[$name] /") 2>&1 &
  pids+=($!)
}

if [ "$(timedatectl show -p NTPSynchronized --value 2>/dev/null)" = "no" ]; then
  echo "WARNING: system clock is not synced. LiveKit rejects tokens when the clock is off."
  echo "         Fix: sudo timedatectl set-ntp true"
fi

listen="$(ss -ltnH 'sport = :11434' | awk '{print $4; exit}')"
if [ -z "$listen" ]; then
  start ollama "$ROOT" env OLLAMA_HOST=0.0.0.0 ollama serve
  for _ in $(seq 40); do curl -sf http://localhost:11434/api/version >/dev/null && break; sleep 0.5; done
else
  echo "Ollama is already running ($listen)."
  case "$listen" in
    127.0.0.1:*|\[::1\]:*)
      echo "WARNING: it only listens on this machine, so the phone cannot reach it."
      echo "         If it is the systemd service: sudo systemctl edit ollama  ->  add"
      echo "           [Service]"
      echo "           Environment=\"OLLAMA_HOST=0.0.0.0\""
      echo "         then: sudo systemctl restart ollama" ;;
  esac
fi

model="$(grep -E '^OLLAMA_MODEL=' "$AGENT/.env" 2>/dev/null | cut -d= -f2-)"
model="${model:-llama3.2}"
if ! ollama list 2>/dev/null | awk 'NR>1{print $1}' | grep -qE "^${model}(:latest)?$"; then
  echo "Pulling $model (first run only)..."
  ollama pull "$model"
fi

start token "$AGENT" "$PY" -u token_server.py
start agent "$AGENT" "$PY" -u agent.py dev

ip="$(ip -4 route get 1.1.1.1 2>/dev/null | awk '{for (i = 1; i < NF; i++) if ($i == "src") { print $(i + 1); exit }}')"
echo "PC address: ${ip:-unknown}  (phone token address: ${ip:-<your PC address>}:8787)"

wait -n
[ -n "$stopping" ] || echo "A process exited; stopping the rest."
