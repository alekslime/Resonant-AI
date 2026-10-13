#!/usr/bin/env bash
# Lets phones on your home network reach the token server (8787) and Ollama (11434).
# Opens the ports to private network ranges only, so it keeps working when your Wi-Fi changes
# and is never open to the internet. Safe to run again.
#   ./scripts/allow-lan.sh
set -euo pipefail

command -v ufw >/dev/null || { echo "ufw is not installed, so there is nothing to open."; exit 0; }
sudo ufw status | grep -q 'Status: active' || { echo "ufw is not active, so there is nothing to open."; exit 0; }

for net in 10.0.0.0/8 172.16.0.0/12 192.168.0.0/16; do
  sudo ufw allow from "$net" to any port 8787,11434 proto tcp >/dev/null
done
echo "Ports 8787 and 11434 are open to private networks (10.x, 172.16-31.x, 192.168.x)."
