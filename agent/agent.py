"""
Resonant PC agent - the full local voice loop (+ state to the phone, interruptible).

  phone mic -> LiveKit -> Silero VAD -> faster-whisper (STT) -> Ollama (LLM, streamed)
  -> Kokoro (TTS, sentence by sentence) -> LiveKit -> phone speaker

Everything but the LiveKit relay runs on this PC. If the Kokoro model files are
missing the agent still runs, text only, and tells you what to download.

Run:  python agent.py dev
Env (all optional, in .env):
  WHISPER_MODEL=base.en       OLLAMA_MODEL=llama3.2     OLLAMA_URL=http://localhost:11434/v1
  KOKORO_MODEL=models/kokoro-v1.0.onnx   KOKORO_VOICES=models/voices-v1.0.bin
  KOKORO_VOICE=af_heart       KOKORO_SPEED=1.0
"""
import asyncio
import json
import logging
import os
import re
import time
from pathlib import Path

os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")

import numpy as np
from dotenv import load_dotenv
from livekit import agents, rtc
from livekit.agents import JobContext, WorkerOptions, cli, vad
from livekit.plugins import silero
from openai import AsyncOpenAI

load_dotenv()
log = logging.getLogger("resonant-agent")
for noisy in ("faster_whisper", "filelock", "huggingface_hub"):
    logging.getLogger(noisy).setLevel(logging.WARNING)

HERE = Path(__file__).parent
WHISPER_MODEL = os.getenv("WHISPER_MODEL", "base.en")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/v1")
KOKORO_MODEL = Path(os.getenv("KOKORO_MODEL", HERE / "models" / "kokoro-v1.0.onnx"))
KOKORO_VOICES = Path(os.getenv("KOKORO_VOICES", HERE / "models" / "voices-v1.0.bin"))
KOKORO_VOICE = os.getenv("KOKORO_VOICE", "af_heart")
KOKORO_SPEED = float(os.getenv("KOKORO_SPEED", "1.0"))

MIN_UTTERANCE_S = 0.5
JUNK = {"you", "so", "uh", "um", "ahem", "hmm", "oh", "thank you", "thanks for watching", "bye"}
SENTENCE_END = re.compile(r"[.!?]+[\"')\]]*\s")
ECHO_TAIL_S = 0.8  # ignore the mic briefly after we stop talking (room echo)
TTS_RATE = 24000

SYSTEM_PROMPT = (
    "You are Resonant, a friendly voice assistant. The user is talking to you by voice, "
    "so answer in one to three short, natural spoken sentences. No lists, no markdown, "
    "no emojis."
)


def prewarm(proc: agents.JobProcess) -> None:
    from faster_whisper import WhisperModel

    proc.userdata["vad"] = silero.VAD.load(
        min_speech_duration=0.3, min_silence_duration=0.8, activation_threshold=0.6
    )
    t0 = time.time()
    proc.userdata["whisper"] = WhisperModel(WHISPER_MODEL, device="cpu", compute_type="int8")
    log.info("whisper '%s' loaded in %.1fs", WHISPER_MODEL, time.time() - t0)

    proc.userdata["kokoro"] = None
    if KOKORO_MODEL.exists() and KOKORO_VOICES.exists():
        from kokoro_onnx import Kokoro

        t0 = time.time()
        proc.userdata["kokoro"] = Kokoro(str(KOKORO_MODEL), str(KOKORO_VOICES))
        log.info("kokoro loaded in %.1fs (voice %s)", time.time() - t0, KOKORO_VOICE)
    else:
        log.warning("Kokoro files not found (%s, %s) - running TEXT ONLY", KOKORO_MODEL, KOKORO_VOICES)


def frames_to_16k_mono(frames: list[rtc.AudioFrame]) -> np.ndarray:
    frame = rtc.combine_audio_frames(frames)
    pcm = np.frombuffer(frame.data, dtype=np.int16).astype(np.float32) / 32768.0
    if frame.num_channels > 1:
        pcm = pcm.reshape(-1, frame.num_channels).mean(axis=1)
    if frame.sample_rate != 16000:
        n_out = int(len(pcm) * 16000 / frame.sample_rate)
        pcm = np.interp(np.linspace(0, len(pcm) - 1, n_out), np.arange(len(pcm)), pcm).astype(np.float32)
    return pcm


class Conversation:
    def __init__(self, whisper, kokoro, voice_source: rtc.AudioSource, local: rtc.LocalParticipant) -> None:
        self.whisper = whisper
        self.kokoro = kokoro
        self.voice_source = voice_source
        self.local = local
        self.llm = AsyncOpenAI(base_url=OLLAMA_URL, api_key="ollama")
        self.history: list[dict] = [{"role": "system", "content": SYSTEM_PROMPT}]
        self.pending: list[rtc.AudioFrame] = []
        self.busy = False
        self.current: asyncio.Task | None = None  # the turn in progress (so it can be interrupted)
        self.speaking = False
        self.quiet_until = 0.0
        self.synth_lock = asyncio.Lock()  # one TTS inference at a time (CPU)
        self._state = ""
        self._captions: list[dict] = []  # last few, published to the phone as one attribute
        self._cap_n = 0

    def agent_is_talking(self) -> bool:
        return self.speaking or time.time() < self.quiet_until

    async def set_state(self, state: str) -> None:
        """Tell the phone what we are doing: listening / transcribing / thinking / speaking.
        It is published as a participant attribute and drives the dots and the tap-to-interrupt rule."""
        if state == self._state:
            return
        self._state = state
        try:
            await self.local.set_attributes({"state": state})
        except Exception as e:
            log.debug("could not publish state %r: %s", state, e)

    async def caption(self, role: str, text: str = "") -> None:
        """Send live captions to the phone: role is "user", "assistant" (one spoken sentence) or
        "done" (this exchange is finished). Published as ONE attribute holding the last few items,
        numbered, so the phone can't miss one even if it polls between two updates."""
        self._cap_n = max(self._cap_n + 1, int(time.time() * 1000))
        self._captions = (self._captions + [{"n": self._cap_n, "role": role, "text": text[:300]}])[-4:]
        try:
            await self.local.set_attributes({"captions": json.dumps(self._captions)})
        except Exception as e:
            log.debug("could not publish caption: %s", e)

    def interrupt(self) -> None:
        """The user tapped: stop thinking / talking right now."""
        log.info("interrupted from the phone")
        self.pending.clear()
        if self.current and not self.current.done():
            self.current.cancel()
        self.voice_source.clear_queue()

    async def warm_up(self) -> None:
        t0 = time.time()
        try:
            await self.llm.chat.completions.create(
                model=OLLAMA_MODEL, messages=[{"role": "user", "content": "hi"}], max_tokens=1
            )
            log.info("ollama '%s' warm in %.1fs", OLLAMA_MODEL, time.time() - t0)
        except Exception as e:
            log.error("Ollama warm-up failed (is it running? is '%s' pulled?): %s", OLLAMA_MODEL, e)
        if self.kokoro:  # first synthesis is slower; do it now, not on your first question
            try:
                await self._synth("Hello.")
                log.info("tts warm")
            except Exception as e:
                log.error("TTS warm-up failed: %s", e)

    # ---- speech to text -------------------------------------------------------------
    def _transcribe(self, audio: np.ndarray) -> str:
        segments, _ = self.whisper.transcribe(
            audio, language="en", beam_size=1, temperature=0.0,
            condition_on_previous_text=False, no_speech_threshold=0.6,
            without_timestamps=True, vad_filter=False,
        )
        return " ".join(s.text.strip() for s in segments).strip()

    # ---- text to speech -------------------------------------------------------------
    async def _synth(self, text: str) -> tuple[str, np.ndarray]:
        async with self.synth_lock:
            samples, sr = await asyncio.to_thread(
                self.kokoro.create, text, voice=KOKORO_VOICE, speed=KOKORO_SPEED, lang="en-us"
            )
        assert sr == TTS_RATE, f"unexpected Kokoro sample rate {sr}"
        return text, (np.clip(samples, -1.0, 1.0) * 32767).astype(np.int16)

    async def _play(self, q: "asyncio.Queue[asyncio.Task | None]", t_start: float) -> None:
        first = True
        try:
            while True:
                item = await q.get()
                if item is None:
                    break
                text, pcm = await item
                await self.caption("assistant", text)  # shown as it starts to be spoken
                if first:
                    first = False
                    self.speaking = True
                    await self.set_state("speaking")
                    log.info("first audio %.1fs after you stopped talking", time.time() - t_start)
                step = TTS_RATE // 50  # 20 ms frames
                for i in range(0, len(pcm), step):
                    chunk = pcm[i : i + step]
                    await self.voice_source.capture_frame(
                        rtc.AudioFrame(chunk.tobytes(), TTS_RATE, 1, len(chunk))
                    )
            if not first:
                await self.voice_source.wait_for_playout()
        finally:
            self.speaking = False
            self.quiet_until = time.time() + ECHO_TAIL_S

    # ---- one conversational turn ----------------------------------------------------
    async def submit(self, frames: list[rtc.AudioFrame]) -> None:
        self.pending.extend(frames)
        if self.busy:
            return
        self.busy = True
        try:
            while self.pending:
                batch, self.pending = self.pending, []
                self.current = asyncio.create_task(self._turn(batch))
                try:
                    await self.current
                except asyncio.CancelledError:
                    if asyncio.current_task().cancelling():
                        raise  # we ourselves are being shut down
                    # otherwise the turn was interrupted by the user: carry on
        finally:
            self.busy = False
            self.current = None

    async def _turn(self, frames: list[rtc.AudioFrame]) -> None:
        try:
            await self._turn_inner(frames)
        finally:
            await self.caption("done")
            await self.set_state("listening")

    async def _turn_inner(self, frames: list[rtc.AudioFrame]) -> None:
        t_start = time.time()
        audio = frames_to_16k_mono(frames)
        if len(audio) / 16000 < MIN_UTTERANCE_S:
            return
        await self.set_state("transcribing")
        text = await asyncio.to_thread(self._transcribe, audio)
        t1 = time.time()
        if not text or text.lower().strip(" .,!?") in JUNK:
            log.info("(ignored: %r)", text)
            return
        log.info("USER (%.1fs to transcribe): %s", t1 - t_start, text)
        await self.caption("user", text)
        await self.set_state("thinking")

        self.history.append({"role": "user", "content": text})
        q: asyncio.Queue = asyncio.Queue()
        synth: list[asyncio.Task] = []
        player = asyncio.create_task(self._play(q, t_start)) if self.kokoro else None
        reply, buf = "", ""
        stream = None

        def abort_audio() -> None:
            if player:
                player.cancel()
            for t in synth:
                t.cancel()
            self.voice_source.clear_queue()

        try:
            stream = await self.llm.chat.completions.create(
                model=OLLAMA_MODEL, messages=self.history, max_tokens=200, stream=True
            )
            async for chunk in stream:
                if not chunk.choices:
                    continue
                delta = chunk.choices[0].delta.content or ""
                reply += delta
                buf += delta
                while player and (m := SENTENCE_END.search(buf)):
                    sentence, buf = buf[: m.end()].strip(), buf[m.end():]
                    if sentence:
                        task = asyncio.create_task(self._synth(sentence))
                        synth.append(task)
                        await q.put(task)
            if player and buf.strip():
                task = asyncio.create_task(self._synth(buf.strip()))
                synth.append(task)
                await q.put(task)
            if player:
                await q.put(None)
                await player  # returns once everything has been played out
        except asyncio.CancelledError:
            abort_audio()
            partial = reply.strip()
            if partial:  # keep what she managed to say, so the next turn makes sense
                self.history.append({"role": "assistant", "content": partial})
            else:
                self.history.pop()
            raise
        except Exception as e:
            log.error("LLM/TTS failed: %s", e)
            abort_audio()
            if not reply:
                self.history.pop()
        finally:
            if stream is not None:
                try:
                    await stream.close()
                except BaseException:
                    pass

        reply = reply.strip()
        if reply and not player:  # text-only mode: no sentences were spoken, so caption it whole
            await self.caption("assistant", reply)
        if reply:
            self.history.append({"role": "assistant", "content": reply})
            log.info("RESONANT (%.1fs total): %s", time.time() - t1, reply)
        if len(self.history) > 13:
            self.history = [self.history[0]] + self.history[-12:]


async def listen(track: rtc.Track, vad_model: vad.VAD, convo: Conversation) -> None:
    audio_stream = rtc.AudioStream(track)
    vad_stream = vad_model.stream()

    async def push_frames() -> None:
        async for ev in audio_stream:
            vad_stream.push_frame(ev.frame)
        vad_stream.end_input()

    pusher = asyncio.create_task(push_frames())
    try:
        async for ev in vad_stream:
            if ev.type == vad.VADEventType.START_OF_SPEECH:
                log.info(">>> speech started")
            elif ev.type == vad.VADEventType.END_OF_SPEECH:
                log.info("<<< speech ended (%.2fs)", ev.speech_duration)
                if not ev.frames:
                    continue
                if convo.agent_is_talking():
                    log.info("(ignored: it was the agent's own voice / echo)")
                    continue
                asyncio.create_task(convo.submit(ev.frames))
    finally:
        pusher.cancel()
        await audio_stream.aclose()


async def entrypoint(ctx: JobContext) -> None:
    await ctx.connect(auto_subscribe=agents.AutoSubscribe.AUDIO_ONLY)
    log.info("joined room %s", ctx.room.name)

    # The agent's voice: one audio track the phone plays.
    voice_source = rtc.AudioSource(TTS_RATE, 1)
    voice_track = rtc.LocalAudioTrack.create_audio_track("resonant-voice", voice_source)
    await ctx.room.local_participant.publish_track(
        voice_track, rtc.TrackPublishOptions(source=rtc.TrackSource.SOURCE_MICROPHONE)
    )

    convo = Conversation(
        ctx.proc.userdata["whisper"], ctx.proc.userdata["kokoro"], voice_source, ctx.room.local_participant
    )
    await convo.set_state("listening")
    asyncio.create_task(convo.warm_up())
    tasks: list[asyncio.Task] = []

    @ctx.room.on("track_subscribed")
    def on_track_subscribed(track: rtc.Track, publication, participant: rtc.RemoteParticipant):
        if track.kind == rtc.TrackKind.KIND_AUDIO:
            log.info("audio track from %s", participant.identity)
            tasks.append(asyncio.create_task(listen(track, ctx.proc.userdata["vad"], convo)))

    @ctx.room.on("participant_attributes_changed")
    def on_attributes_changed(changed: dict, participant: rtc.Participant):
        # The phone bumps "interrupt" when the user taps while she is thinking or speaking.
        if "interrupt" in changed and participant.identity != ctx.room.local_participant.identity:
            convo.interrupt()

    @ctx.room.on("data_received")
    def on_data_received(packet: rtc.DataPacket):
        if packet.topic == "interrupt":
            convo.interrupt()

    @ctx.room.on("participant_disconnected")
    def on_participant_disconnected(participant: rtc.RemoteParticipant):
        log.info("%s left", participant.identity)


if __name__ == "__main__":
    cli.run_app(
        WorkerOptions(
            entrypoint_fnc=entrypoint,
            prewarm_fnc=prewarm,
            initialize_process_timeout=120,
        )
    )
