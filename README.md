# Resonant

> 1st place — GDG Tirana 2026

**Resonant** is a voice-first AI learning assistant built for blind and visually impaired students — and anyone who learns better by listening.

Upload any study material, ask questions out loud, and receive real answers back in real time. Like having a tutor that never stops listening.

No endless scrolling. No screen dependency. Just conversation powered by AI.

![Resonant Logo](logo.png)

---

## Features

- Voice-first AI interaction
- Real-time spoken questions and answers
- Textbook and study material understanding
- Accessible learning experience designed for blind and visually impaired students
- Lightweight web interface
- AI-powered tutoring workflow

---

## Tech Stack

- Python
- Flask
- LiveKit
- HTML / CSS / JavaScript
- OpenAI GPT-4o (via LiveKit inference)
- Deepgram Nova-3 (speech-to-text)
- Cartesia Sonic-3 (text-to-speech)
- Silero VAD (voice activity detection)
- ai_coustics (noise cancellation)

---

## How It Works

### `server.py`
A lightweight Flask API with a single endpoint (`/api/token`). When the frontend loads, it calls this server to generate a short-lived LiveKit access token. That token lets the browser join a private LiveKit room scurely without exposing any API keys to the client.

### `agent.py`
The LiveKit agent that runs the actual AI tutoring session. It connects to the same room as the student, listens via Deepgram's multilingual speech-to-text, processes the question through GPT-4o with a custom tutoring prompt, and speaks the answer back using Cartesia's voice. It also applies noise cancellation and voice activity detection so the conversation feels natural.

---

## Project Structure

```
.
├── agent.py          — LiveKit AI agent (voice pipeline + GPT-4o)
├── server.py         — Flask token server
├── index.html        — Main frontend
├── demo.html         — Demo/presentation page
├── style.css         — Styles
├── start.sh          — One-command launch script
├── requirements.txt  — Python dependencies
├── .gitignore
├── logo.png
├── grad.png
├── ask.png
├── listen.png
├── upload.png
├── slide1.png
└── slide2.png
```

---

## Requirements

- Python 3.9+
- A [LiveKit Cloud](https://cloud.livekit.io) account (free tier works)
- Internet connection (all AI models run via LiveKit's inference API)

---

## Installation

**Clone the repository:**

```bash
git clone https://github.com/alekslime/Resonant-AI.git
cd Resonant-AI
```

**Create and activate a virtual environment:**

```bash
python3 -m venv .venv
source .venv/bin/activate
```

**Install dependencies:**

```bash
pip install -r requirements.txt
```

**Create a `.env.local` file in the project root:**

```
LIVEKIT_URL=your_livekit_url
LIVEKIT_API_KEY=your_key
LIVEKIT_API_SECRET=your_secret
```

Get these from your [LiveKit Cloud dashboard](https://cloud.livekit.io) under Settings → API Keys.

---

## Running the Project

```bash
chmod +x start.sh
./start.sh
```

That's it. The script starts the Flask token server, the LiveKit agent, and the frontend file server all at once. Then open:

```
http://localhost:8080
```

Press `Ctrl+C` to stop everything.

---

## Known Limitations

- Requires a LiveKit Cloud account — does not run fully offline
- All AI models (STT, LLM, TTS) are called via LiveKit's hosted inference API, so a stable internet connection is required
- No file upload handling in the backend — the study material understanding is handled conversationally through the agent's prompt
- Tested on Linux; `fuser` in `start.sh` may not work on macOS (use `lsof -ti:5000 | xargs kill` instead)

---

## Security

API keys and environment variables are excluded via `.gitignore`. Never commit `.env` or `.env.local` files.

---

## Inspiration

Education should feel human, immediate, and accessible.

Resonant was built to explore how voice and AI can reduce friction in learning — and to give people who have always deserved better tools a way to interact with knowledge more naturally.

---

## Built At

**GDG Tirana 2026** — 1st place

Supported by:

- AI Hub Albania
- Google
- Plug and Play
- Gjirafa Mall
- Codevider
- B2 Tech

---

## License

MIT License
