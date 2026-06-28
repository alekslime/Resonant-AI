import os
import time
import uuid
from dotenv import load_dotenv
from flask import Flask, jsonify
from flask_cors import CORS
from livekit.api import AccessToken, VideoGrants

load_dotenv(".env.local")

app = Flask(__name__)
CORS(app)  # Allows your HTML page to call this server


@app.route("/api/token")
def get_token():
    api_key = os.environ.get("LIVEKIT_API_KEY")
    api_secret = os.environ.get("LIVEKIT_API_SECRET")
    lk_url = os.environ.get("LIVEKIT_URL")

    if not api_key or not api_secret or not lk_url:
        return (
            jsonify(
                {
                    "error": "Missing LIVEKIT_API_KEY, LIVEKIT_API_SECRET, or LIVEKIT_URL in .env.local"
                }
            ),
            500,
        )

    identity = f"student-{int(time.time())}"

    grants = VideoGrants(
        room_join=True,
        room=f"tutor-{uuid.uuid4().hex[:8]}",
        can_publish=True,
        can_subscribe=True,
        room_record=False,
    )

    token = (
        AccessToken(api_key, api_secret)
        .with_identity(identity)
        .with_name("Student")
        .with_grants(grants)
    )

    return jsonify({"token": token.to_jwt(), "url": lk_url})


if __name__ == "__main__":
    print("✅ Token server running at http://localhost:5000")
    print("   Waiting for requests to /api/token ...")
    app.run(port=5000, debug=True)