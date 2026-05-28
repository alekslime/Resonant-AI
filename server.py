"""
server.py — Resonant backend

Routes:
  GET  /                      → serve index.html
  POST /api/upload            → ingest a document into the RAG store
  DELETE /api/docs            → clear session documents
  WS   /ws                    → voice WebSocket (audio in, audio+text out)

WebSocket message protocol (JSON):
  Client → Server:
    { "type": "audio", "data": "<base64 wav>" }
    { "type": "clear_history" }

  Server → Client:
    { "type": "transcript", "text": "..." }           ← what the student said
    { "type": "response",   "text": "...", "audio": "<base64 wav>" }
    { "type": "error",      "message": "..." }
    { "type": "status",     "message": "..." }
"""

import base64
import json
import logging
import os
import uuid
from pathlib import Path

from dotenv import load_dotenv
from flask import Flask, jsonify, request, send_from_directory
from flask_cors import CORS
from flask_sock import Sock

from pipeline import ask_llm, synthesize, transcribe
from rag import RAGStore

load_dotenv(".env.local")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
)
logger = logging.getLogger("server")

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads")
STATIC_DIR = os.path.dirname(__file__)
ALLOWED_EXTENSIONS = {".pdf", ".docx", ".txt", ".md"}

os.makedirs(UPLOAD_DIR, exist_ok=True)

app = Flask(__name__, static_folder=STATIC_DIR)
CORS(app)
sock = Sock(app)

rag = RAGStore()


# ------------------------------------------------------------------ #
#  Static                                                              #
# ------------------------------------------------------------------ #

@app.route("/")
def index():
    return send_from_directory(STATIC_DIR, "index.html")


@app.route("/<path:filename>")
def static_files(filename):
    return send_from_directory(STATIC_DIR, filename)


# ------------------------------------------------------------------ #
#  Document upload                                                     #
# ------------------------------------------------------------------ #

@app.route("/api/upload", methods=["POST"])
def upload():
    if "file" not in request.files:
        return jsonify({"error": "No file provided"}), 400

    f = request.files["file"]
    session_id = request.form.get("session_id", "default")

    ext = Path(f.filename).suffix.lower()
    if ext not in ALLOWED_EXTENSIONS:
        return jsonify({"error": f"Unsupported file type: {ext}. Use PDF, DOCX, TXT, or MD."}), 400

    filename = f"{uuid.uuid4().hex}{ext}"
    filepath = os.path.join(UPLOAD_DIR, filename)
    f.save(filepath)

    try:
        chunk_count = rag.ingest_file(filepath, session_id)
        os.unlink(filepath)  # don't keep raw files after ingestion
        return jsonify({
            "ok": True,
            "chunks": chunk_count,
            "filename": f.filename,
        })
    except Exception as e:
        logger.error(f"Ingestion error: {e}")
        return jsonify({"error": str(e)}), 500


@app.route("/api/docs", methods=["DELETE"])
def clear_docs():
    session_id = request.args.get("session_id", "default")
    rag.delete_session(session_id)
    return jsonify({"ok": True})


# ------------------------------------------------------------------ #
#  Voice WebSocket                                                     #
# ------------------------------------------------------------------ #

@sock.route("/ws")
def voice_ws(ws):
    session_id = str(uuid.uuid4())
    history = []  # conversation memory for this connection

    logger.info(f"New WebSocket session: {session_id}")
    ws.send(json.dumps({
        "type": "status",
        "message": "Connected. Ready to listen.",
        "session_id": session_id,
    }))

    while True:
        try:
            raw = ws.receive()
            if raw is None:
                break

            msg = json.loads(raw)

            # ---- clear history ----------------------------------------
            if msg.get("type") == "clear_history":
                history.clear()
                ws.send(json.dumps({"type": "status", "message": "Conversation cleared."}))
                continue

            # ---- audio input -----------------------------------------
            if msg.get("type") != "audio":
                continue

            audio_bytes = base64.b64decode(msg["data"])

            # 1. Transcribe
            ws.send(json.dumps({"type": "status", "message": "Transcribing..."}))
            transcript = transcribe(audio_bytes)

            if not transcript:
                ws.send(json.dumps({"type": "status", "message": "Didn't catch that. Try again."}))
                continue

            ws.send(json.dumps({"type": "transcript", "text": transcript}))

            # 2. RAG retrieval
            context_chunks = []
            if rag.has_documents(session_id):
                ws.send(json.dumps({"type": "status", "message": "Searching your notes..."}))
                context_chunks = rag.retrieve(transcript, session_id)

            # 3. LLM
            ws.send(json.dumps({"type": "status", "message": "Thinking..."}))
            reply = ask_llm(transcript, history, context_chunks)

            # update history (keep last 10 turns to avoid context overflow)
            history.append({"role": "user", "content": transcript})
            history.append({"role": "assistant", "content": reply})
            if len(history) > 20:
                history = history[-20:]

            # 4. TTS
            ws.send(json.dumps({"type": "status", "message": "Speaking..."}))
            audio_out = synthesize(reply)
            audio_b64 = base64.b64encode(audio_out).decode()

            ws.send(json.dumps({
                "type": "response",
                "text": reply,
                "audio": audio_b64,
                "used_docs": len(context_chunks) > 0,
            }))

        except Exception as e:
            logger.error(f"WebSocket error: {e}", exc_info=True)
            try:
                ws.send(json.dumps({"type": "error", "message": "Something went wrong. Please try again."}))
            except Exception:
                break

    logger.info(f"Session ended: {session_id}")


# ------------------------------------------------------------------ #
#  Entry point                                                         #
# ------------------------------------------------------------------ #

if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    logger.info(f"Resonant server starting on http://localhost:{port}")
    app.run(host="0.0.0.0", port=port, debug=False)
