# Resonant (Local Stack)

Voice-first AI learning assistant. Fully local — no LiveKit, no cloud APIs.

## Stack

| Layer | Tool |
|---|---|
| STT | faster-whisper (base, CUDA) |
| LLM | Ollama + llama3.2:3b |
| TTS | Kokoro-82M |
| RAG | ChromaDB + sentence-transformers |
| Transport | Flask-Sock (WebSocket) |
| Public URL | ngrok (optional) |

---

## Setup

### 1. Install Ollama
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### 2. Install ngrok (optional, for sharing)
```bash
snap install ngrok
# then sign up free at ngrok.com and run:
ngrok config add-authtoken YOUR_TOKEN
```

### 3. Python environment
```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

---

## Run

**Local only:**
```bash
chmod +x start.sh
./start.sh
# open http://localhost:5000
```

**With public URL:**
```bash
./start.sh --ngrok
# outputs a public https URL you can share
```

---

## How it works

1. Student opens the app, optionally uploads PDF/DOCX/TXT notes
2. Holds the mic button and asks a question
3. Audio is sent over WebSocket to the server
4. `faster-whisper` transcribes the audio
5. `ChromaDB` searches the uploaded notes for relevant chunks
6. `Ollama` (llama3.2:3b) generates a spoken-language answer, grounded in the notes if available
7. `Kokoro` synthesizes the reply to audio
8. The browser plays the audio and shows the transcript

---

## Model notes

- **Whisper base** (~150MB, ~1-2s latency on GPU) — good enough for clear speech
- **llama3.2:3b** (~2.5GB VRAM) — fast, fits on 4GB GPU
- **Kokoro** (~82M params, CPU fine) — fast, good quality

For better accuracy on a stronger machine:
- Swap `WHISPER_MODEL=medium` in `.env.local`
- Swap `OLLAMA_MODEL=mistral` for richer answers

---

## Environment variables (.env.local)

```
OLLAMA_URL=http://localhost:11434   # default
OLLAMA_MODEL=llama3.2:3b           # default
WHISPER_MODEL=base                  # default, or: small, medium
PORT=5000                           # default
```

---

## File structure

```
.
├── server.py          — Flask + WebSocket server
├── pipeline.py        — STT / LLM / TTS
├── rag.py             — Document ingestion + retrieval
├── static/
│   └── index.html     — Frontend
├── uploads/           — Temp upload dir (auto-created)
├── vectorstore/       — ChromaDB persistent store (auto-created)
├── requirements.txt
└── start.sh
```
