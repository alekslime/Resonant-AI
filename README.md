# Resonant

**Resonant** is a voice-first AI learning assistant built for blind and visually impaired students — and anyone who learns better by listening.

Upload any study material, ask questions out loud, and receive real answers back in real time. Like having a tutor that never stops listening.

No endless scrolling. No screen dependency. Just conversation powered by AI.

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
- AI voice pipeline

---

## Project Structure

```
.
├── agent.py
├── server.py
├── index.html
├── demo.html
├── style.css
├── requirements.txt
├── .gitignore
├── logo.png
├── grad.png
├── ask.png
├── listen.png
├── upload.png
├── slide1.png
├── slide2.png
└── fourth-bg.png
```

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

---

## Running the Project

```bash
chmod +x start.sh
./start.sh
```

That's it. The script starts the backend, the agent, and the frontend server all at once. Then open:

```
http://localhost:8080
```

Press `Ctrl+C` to stop everything.

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
