# Resonant

## 1st Place — GDG Tirana 2026

# Your personal AI teacher that talks, reads, sees, and remembers.

Resonant is a multimodal AI learning platform designed to make education more personalized, accessible, and natural.

Students can chat with Resonant through text, voice, and visual input. It understands learning materials, explains difficult concepts, tracks progress, and adapts to each student's needs.

Built for every student — including blind and visually impaired learners.

---

# Why Resonant?

Traditional education tools provide information.

AI assistants provide answers.

But learning requires something more:

- Understanding how a student learns
- Remembering previous struggles
- Explaining concepts in different ways
- Making knowledge accessible
- Helping students build confidence

Resonant is designed to be a personal AI teacher that grows with the student.

---

# Features

## Multimodal AI Learning

Interact with Resonant however you learn best:

### Text Chat
A familiar ChatGPT-like experience:

- Ask questions
- Solve problems
- Request explanations
- Generate summaries
- Create quizzes
- Build study plans

---

### Voice Conversations

Learn naturally through speech:

- Real-time spoken conversations
- Hands-free studying
- Voice explanations
- Language practice
- Accessibility support

---

### Vision & Document Understanding

Resonant understands educational materials:

- Textbooks
- PDFs
- Homework
- Notes
- Images
- Diagrams
- Presentations

Examples:

"Explain this page."

"Why is my answer wrong?"

"Summarize this chapter."

"Describe this diagram."

---

# Personal Learning Memory

Resonant is not just a chatbot.

It remembers the student's learning journey.

It can track:

- Topics studied
- Previous mistakes
- Weak areas
- Learning goals
- Exam preparation
- Preferred explanation styles

The AI teacher improves as it learns about the student.

---

# Accessibility First

Resonant was originally created to support blind and visually impaired students.

Accessibility is not an extra feature. It is part of the foundation.

Supported experiences:

- Textbook reading
- Image descriptions
- Diagram explanations
- Voice-first navigation
- Audio learning
- Accessible quizzes
- Screen-reader compatibility

Education should not depend on eyesight.

---

# Language Learning

Resonant can act as a personal language tutor.

Support for:

- IELTS preparation
- DELF preparation
- TOEFL preparation
- Speaking practice
- Listening exercises
- Pronunciation feedback
- Conversation simulations

Example:

"Practice a DELF B2 speaking exam with me."

---

# Private Local AI

The future of Resonant is local-first.

Student data should remain private.

Goals:

- No required AI API subscriptions
- No external processing of private learning data
- Offline-capable AI
- Local model inference

Your education belongs to you.

---

# Architecture

Current prototype:


Student
|
|
Text / Voice / Images
|
|
Resonant Interface
|
|
AI Processing Pipeline
|
|
Personalized Learning Response


---

# Current Prototype Stack

The original GDG Tirana prototype uses:

- Python
- Flask
- HTML/CSS/JavaScript
- LiveKit
- GPT-4o
- Deepgram Nova-3
- Cartesia Sonic-3
- Silero VAD
- ai_coustics

This prototype proved the voice-learning experience.

The next generation focuses on local AI and personalization.

---

# Planned Local AI Architecture

Target stack:

## Language Model

Local LLM inference:

- llama.cpp
- Qwen
- Llama
- Mistral

## Speech Recognition

Local speech-to-text:

- Whisper.cpp
- Faster Whisper

## Text To Speech

Local voice generation:

- Piper
- Kokoro

## Vision

Local multimodal models:

- MiniCPM-V
- Qwen-VL

## Memory

Private learning database:

- SQLite
- Vector embeddings
- Local retrieval system

---

# Roadmap

## Phase 1 — Core Platform

- Text chat interface
- Voice conversations
- File uploads
- Basic AI tutoring

## Phase 2 — Understanding

- PDF processing
- Textbook understanding
- Image analysis
- Homework assistance

## Phase 3 — Personal Teacher

- Student profiles
- Learning memory
- Progress tracking
- Personalized explanations

## Phase 4 — Accessibility

- Blind student workflows
- Voice-only learning
- Accessible exams
- Tactile feedback integration

## Phase 5 — Local AI

- Remove external API dependency
- Fully local inference
- Offline education mode

---

# Vision

> Every student deserves a teacher that understands them.

---

# Built At

GDG Tirana 2026 — 1st Place

Supported by:

- Google
- AI Hub Albania
- Plug and Play
- Gjirafa Mall
- Codevider
- B2 Tech

---

# License

MIT License
