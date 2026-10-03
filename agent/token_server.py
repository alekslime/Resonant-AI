"""
Resonant token server - hands the phone a LiveKit join token, so the app no longer needs
LiveKit's dev sandbox. Runs on the same PC as agent.py and uses the same .env.

Run:  python token_server.py
Needs in agent/.env (the first three are the ones agent.py already uses):
  LIVEKIT_URL=wss://your-project.livekit.cloud
  LIVEKIT_API_KEY=...         LIVEKIT_API_SECRET=...
  TOKEN_KEY=some-long-random-string     (the phone must send it; refuses to start without it)
Optional:
  TOKEN_PORT=8787             TOKEN_HOST=0.0.0.0

On the phone, in local.properties (git-ignored), then rebuild:
  live.tokenUrl=http://<this PC's LAN or Tailscale address>:8787/token
  live.tokenKey=<the same TOKEN_KEY>

Plain http works in debug builds only (like the Ollama address). The key travels in clear text, so
keep this on your own network or Tailscale; do not put it on the open internet.
"""
import hmac
import json
import logging
import os
import secrets
import sys
from datetime import timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

from dotenv import load_dotenv
from livekit import api

load_dotenv()
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
log = logging.getLogger("resonant-token")

LIVEKIT_URL = os.getenv("LIVEKIT_URL", "")
API_KEY = os.getenv("LIVEKIT_API_KEY", "")
API_SECRET = os.getenv("LIVEKIT_API_SECRET", "")
TOKEN_KEY = os.getenv("TOKEN_KEY", "")
HOST = os.getenv("TOKEN_HOST", "0.0.0.0")
PORT = int(os.getenv("TOKEN_PORT", "8787"))
TOKEN_TTL = timedelta(hours=1)


def make_token(name: str) -> dict:
    # A fresh room per connection: the agent is dispatched into it when the phone joins, and no old
    # session's state (history, attributes) can leak into the next one.
    room = "resonant-" + secrets.token_hex(4)
    token = (
        api.AccessToken(API_KEY, API_SECRET)
        .with_identity(name)
        .with_name(name)
        .with_ttl(TOKEN_TTL)
        .with_grants(
            api.VideoGrants(
                room_join=True,
                room=room,
                can_publish=True,
                can_subscribe=True,
                can_publish_data=True,
                can_update_own_metadata=True,  # the phone sets attributes: interrupt, voice, ptt
            )
        )
        .to_jwt()
    )
    return {"serverUrl": LIVEKIT_URL, "participantToken": token, "room": room}


class Handler(BaseHTTPRequestHandler):
    def _send(self, code: int, body: dict) -> None:
        data = json.dumps(body).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self) -> None:
        url = urlparse(self.path)
        if url.path != "/token":
            self._send(404, {"error": "not found"})
            return
        sent_key = self.headers.get("X-Resonant-Key", "")
        if not hmac.compare_digest(sent_key.encode("utf-8"), TOKEN_KEY.encode("utf-8")):
            log.warning("refused a request from %s (wrong key)", self.client_address[0])
            self._send(401, {"error": "bad key"})
            return
        name = (parse_qs(url.query).get("name") or ["resonant-android"])[0][:64] or "resonant-android"
        body = make_token(name)
        log.info("token for %s from %s, room %s", name, self.client_address[0], body["room"])
        self._send(200, body)

    def log_message(self, format: str, *args) -> None:  # we log our own, shorter lines
        pass


def main() -> None:
    missing = [
        n
        for n, v in (
            ("LIVEKIT_URL", LIVEKIT_URL),
            ("LIVEKIT_API_KEY", API_KEY),
            ("LIVEKIT_API_SECRET", API_SECRET),
            ("TOKEN_KEY", TOKEN_KEY),
        )
        if not v
    ]
    if missing:
        sys.exit("Missing in agent/.env: " + ", ".join(missing))
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    log.info("token server on http://%s:%d/token", HOST, PORT)
    server.serve_forever()


if __name__ == "__main__":
    main()
