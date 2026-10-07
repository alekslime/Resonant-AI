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
    (if models/kokoro-v1.0.int8.onnx exists it is used instead: about 3x faster on CPU)
  KOKORO_VOICE=af_heart       KOKORO_SPEED=1.0   (defaults; the phone's Settings override them)
  STOP_WORDS=1                say "stop" / "wait" / "never mind" to interrupt her (0 = off)
Mic mode (always open / hold to talk) is chosen on the phone too.
Say "quiz me" (or "quiz me on photosynthesis") for a spoken quiz; run export_quizzes.py once first.
"""
import asyncio
import json
import logging
import os
import re
import socket
import time
from pathlib import Path

os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")

import numpy as np
from dotenv import load_dotenv
from livekit import agents, rtc
from livekit.agents import JobContext, WorkerOptions, cli, vad
from livekit.plugins import silero
from openai import AsyncOpenAI
from voice_commands import Commands
from voice_quiz import Quiz

load_dotenv()
log = logging.getLogger("resonant-agent")
for noisy in ("faster_whisper", "filelock", "huggingface_hub"):
    logging.getLogger(noisy).setLevel(logging.WARNING)

HERE = Path(__file__).parent
WHISPER_MODEL = os.getenv("WHISPER_MODEL", "base.en")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/v1")
_KOKORO_FP32 = HERE / "models" / "kokoro-v1.0.onnx"
_KOKORO_INT8 = HERE / "models" / "kokoro-v1.0.int8.onnx"
KOKORO_MODEL = Path(os.getenv("KOKORO_MODEL") or (_KOKORO_INT8 if _KOKORO_INT8.exists() else _KOKORO_FP32))
KOKORO_VOICES = Path(os.getenv("KOKORO_VOICES", HERE / "models" / "voices-v1.0.bin"))
KOKORO_VOICE = os.getenv("KOKORO_VOICE", "af_heart")
KOKORO_SPEED = float(os.getenv("KOKORO_SPEED", "1.0"))
MIN_SPEED, MAX_SPEED = 0.5, 2.0

MIN_UTTERANCE_S = 0.5
JUNK = {"you", "so", "uh", "um", "ahem", "hmm", "oh", "thank you", "thanks for watching", "bye"}
SENTENCE_END = re.compile(r"[.!?]+[\"')\]]*\s")
CLAUSE_END = re.compile(r"[,;:]\s")
FIRST_CHUNK_MIN_WORDS = 5  # the first spoken piece may stop at a comma, so audio starts sooner
ECHO_TAIL_S = 0.8  # ignore the mic briefly after we stop talking (room echo)
STOP_WORDS = os.getenv("STOP_WORDS", "1") != "0"
STOP_MAX_S = 2.5  # only short utterances are checked for a stop word
STOP_PHRASES = {
    "stop", "stop it", "stop talking", "wait", "hold on", "pause", "quiet", "be quiet",
    "shush", "enough", "that's enough", "never mind", "nevermind", "cancel", "okay stop",
}
STOP_FILLER = {"ok", "okay", "hey", "please", "resonant", "alright", "um", "uh", "now", "yeah"}
TTS_RATE = 24000

SYSTEM_PROMPT = (
    "You are Resonant, a warm private tutor for a student who learns by listening. "
    "Everything you say is spoken aloud, so use two to four short, natural sentences. "
    "No lists, no markdown, no emojis, no symbols. "
    "Teach one small idea at a time. Before a new topic, ask what the student already knows. "
    "After each idea, ask one short question to check they understood. "
    "Listen to the answer: praise what is right, gently correct what is wrong, then build on it. "
    "If the student is lost, explain it again in a different way with an everyday example. "
    "When a few connected ideas are done, you may offer a quiz once, in one short sentence. "
    "If you are not sure of a fact, say so instead of guessing. "
    "If the student interrupts, answer their question first, then offer to continue."
)

CONTINUE_PHRASES = {
    "continue", "go on", "keep going", "carry on", "go ahead", "proceed", "resume",
}
REPEAT_PHRASES = {
    "repeat", "repeat that", "say that again", "say it again", "once more", "again",
    "come again", "pardon",
}
FILLER_WORDS = {"ok", "okay", "please", "alright", "now", "hey", "resonant"}


def tutor_ask(text: str) -> str:
    """What the model is told when the student only says 'continue' or 'repeat'."""
    words = re.sub(r"[^a-z' ]", " ", text.lower()).split()
    t = " ".join(w for w in words if w not in FILLER_WORDS)
    if t in CONTINUE_PHRASES:
        return "Continue exactly from where you stopped. Do not repeat what you already said."
    if t in REPEAT_PHRASES:
        return "Say your last answer again, in simpler words."
    return text


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
        try:  # first synthesis is slow; do it here, once, before any phone joins
            proc.userdata["kokoro"].create("Hello.", voice=KOKORO_VOICE, speed=1.0, lang="en-us")
        except Exception as e:
            log.warning("kokoro warm-up failed: %s", e)
    else:
        log.warning("Kokoro files not found (%s, %s) - running TEXT ONLY", KOKORO_MODEL, KOKORO_VOICES)


def split_chunk(buf: str, first: bool) -> tuple[str, str] | None:
    """Cut the next piece to speak off the front of buf, or None if there is no complete one yet.
    The first piece of a reply may end at a comma once it has a few words: the sooner the first
    synthesis starts, the sooner you hear her."""
    sentence = SENTENCE_END.search(buf)
    if first:
        for clause in CLAUSE_END.finditer(buf):
            if sentence and clause.end() >= sentence.end():
                break
            if len(buf[: clause.end()].split()) >= FIRST_CHUNK_MIN_WORDS:
                return buf[: clause.end()].strip(), buf[clause.end():]
    if sentence:
        return buf[: sentence.end()].strip(), buf[sentence.end():]
    return None


def is_stop_phrase(text: str) -> bool:
    """True for a short "stop talking" request such as "stop", "okay wait" or "never mind"."""
    words = re.sub(r"[^a-z' ]", " ", text.lower()).split()
    while words and words[0] in STOP_FILLER:
        words.pop(0)
    while words and words[-1] in STOP_FILLER:
        words.pop()
    return " ".join(words) in STOP_PHRASES


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
        self.voice = KOKORO_VOICE  # the phone can change these from Settings
        self.speed = KOKORO_SPEED
        self.hold_mode = False  # hold-to-talk: the phone says when a turn starts and ends
        self.ptt_recording = False
        self.ptt_tail_until = 0.0
        self.ptt_finishing = False
        self.ptt_frames: list[rtc.AudioFrame] = []
        self.ptt_seq = 0
        self.pending_texts: list[str] = []  # typed messages from the phone
        self.text_seq = 0
        self.quiz = Quiz.load(HERE / "quizzes.json")
        if not self.quiz.sets:
            log.info("no quizzes.json found - run python export_quizzes.py to enable 'quiz me'")
        self._state = ""
        self._captions: list[dict] = []  # last few, published to the phone as one attribute
        self._cap_n = 0
        self._cmd_n = 0
        self.commands = Commands()

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

    async def send_command(self, cmd: dict) -> None:
        """Tell the phone to move: published as one numbered attribute, like the captions."""
        self._cmd_n = max(self._cmd_n + 1, int(time.time() * 1000))
        try:
            await self.local.set_attributes({"cmd": json.dumps({**cmd, "n": self._cmd_n})})
        except Exception as e:
            log.debug("could not publish command: %s", e)

    def collecting(self) -> bool:
        return self.ptt_recording or time.time() < self.ptt_tail_until

    def ptt_signal(self, value: str) -> None:
        """Hold-to-talk marker from the phone: "start:N" or "end:N". N only goes up; stale or
        repeated markers (the phone sends each on two routes) are ignored."""
        kind, _, num = (value or "").partition(":")
        try:
            n = int(num)
        except ValueError:
            return
        if n <= self.ptt_seq or kind not in ("start", "end"):
            return
        self.ptt_seq = n
        if kind == "start":
            if not self.ptt_finishing:
                self.ptt_frames = []
            self.ptt_recording = True
        elif self.ptt_recording:
            self.ptt_recording = False
            self.ptt_tail_until = time.time() + 0.25  # frames still in flight
            self.ptt_finishing = True
            asyncio.create_task(self._ptt_finish())

    async def _ptt_finish(self) -> None:
        await asyncio.sleep(0.3)
        frames, self.ptt_frames = self.ptt_frames, []
        self.ptt_finishing = False
        if not frames:
            return
        if self.agent_is_talking():
            log.info("(ignored: held turn overlapped the agent's own voice)")
            return
        await self.submit(frames)

    def apply_settings(
        self,
        voice: str | None = None,
        speed: str | float | None = None,
        mode: str | None = None,
    ) -> None:
        """The phone chose a voice, speed or mic mode in Settings. Bad values are ignored, not fatal."""
        if mode in ("hold", "open") and (mode == "hold") != self.hold_mode:
            self.hold_mode = mode == "hold"
            log.info("mic mode: %s", "hold to talk" if self.hold_mode else "always open")
        if voice and voice != self.voice:
            known = self.kokoro.get_voices() if self.kokoro else []
            if voice in known:
                self.voice = voice
                log.info("voice set to %s", voice)
            else:
                log.warning("phone asked for unknown voice %r", voice)
        if speed is not None:
            try:
                value = min(MAX_SPEED, max(MIN_SPEED, float(speed)))
            except (TypeError, ValueError):
                log.warning("phone sent a bad speed %r", speed)
                return
            if value != self.speed:
                self.speed = value
                log.info("speed set to %.2f", value)

    async def heard_while_busy(self, frames: list[rtc.AudioFrame]) -> None:
        """Speech detected while she is thinking or speaking. A short "stop" interrupts her.
        Anything else is her own voice coming back (dropped) or, if she is only thinking,
        the user's next question (queued, as before)."""
        audio = frames_to_16k_mono(frames)
        if STOP_WORDS and len(audio) / 16000 <= STOP_MAX_S:
            text = await asyncio.to_thread(self._transcribe, audio)
            if is_stop_phrase(text):
                self.interrupt("a spoken stop word")
                return
            if self.quiz.wants_exit(text):
                self.interrupt("a spoken quiz exit")
                self.quiz.end()
                return
            log.info("(heard while busy: %r)", text)
        if not self.speaking and time.time() >= self.quiet_until:
            await self.submit(frames)
        else:
            log.info("(ignored: it was the agent's own voice / echo)")

    def interrupt(self, source: str = "the phone") -> None:
        """The user tapped or said stop: stop thinking / talking right now."""
        log.info("interrupted by %s", source)
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
            lang = "en-gb" if self.voice.startswith("b") else "en-us"
            samples, sr = await asyncio.to_thread(
                self.kokoro.create, text, voice=self.voice, speed=self.speed, lang=lang
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

    async def _speak_fixed(self, lines: list[str], t_start: float) -> None:
        """Speak ready-made lines (no LLM), the same way a reply is spoken."""
        if not self.kokoro:
            await self.caption("assistant", " ".join(lines))
            return
        q: asyncio.Queue = asyncio.Queue()
        synth = [asyncio.create_task(self._synth(line)) for line in lines]
        for task in synth:
            await q.put(task)
        await q.put(None)
        player = asyncio.create_task(self._play(q, t_start))
        try:
            await player
        except asyncio.CancelledError:
            player.cancel()
            for task in synth:
                task.cancel()
            self.voice_source.clear_queue()
            raise

    # ---- one conversational turn ----------------------------------------------------
    async def submit(self, frames: list[rtc.AudioFrame]) -> None:
        self.pending.extend(frames)
        await self._run()

    async def submit_text(self, text: str) -> None:
        self.pending_texts.append(text)
        if self.busy:
            self.interrupt()  # typing barges in, like tapping
        await self._run()

    def text_signal(self, value: str) -> None:
        """Typed message from the phone: "N|text". N only goes up; the phone sends it on two
        routes, so a repeat is ignored."""
        num, _, text = (value or "").partition("|")
        try:
            n = int(num)
        except ValueError:
            return
        text = text.strip()
        if n <= self.text_seq or not text:
            return
        self.text_seq = n
        log.info("TYPED: %s", text)
        asyncio.create_task(self.submit_text(text))

    async def _run(self) -> None:
        if self.busy:
            return
        self.busy = True
        try:
            while self.pending or self.pending_texts:
                if self.pending_texts:
                    work = self._turn_text(self.pending_texts.pop(0))
                else:
                    batch, self.pending = self.pending, []
                    work = self._turn(batch)
                self.current = asyncio.create_task(work)
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

    async def _turn_text(self, text: str) -> None:
        try:
            now = time.time()
            await self._answer(text, now, now)
        finally:
            await self.caption("done")
            await self.set_state("listening")

    async def _turn_inner(self, frames: list[rtc.AudioFrame]) -> None:
        t_start = time.time()
        audio = frames_to_16k_mono(frames)
        if len(audio) / 16000 < (0.25 if self.quiz.in_progress() else MIN_UTTERANCE_S):  # "B" is short
            return
        await self.set_state("transcribing")
        text = await asyncio.to_thread(self._transcribe, audio)
        t1 = time.time()
        if not text or text.lower().strip(" .,!?") in JUNK:
            log.info("(ignored: %r)", text)
            return
        log.info("USER (%.1fs to transcribe): %s", t1 - t_start, text)
        await self._answer(text, t_start, t1)

    async def _answer(self, text: str, t_start: float, t1: float) -> None:
        await self.caption("user", text)
        await self.set_state("thinking")

        command = None if self.quiz.in_progress() else self.commands.parse(text)
        if command is not None:
            log.info("COMMAND: %s", command)
            if "say" in command:
                await self._speak_fixed([command["say"]], t_start)
            else:
                await self.send_command(command)
            return

        quiz_lines = self.quiz.respond(text)
        if quiz_lines is not None:
            log.info("QUIZ: %s", " ".join(quiz_lines))
            await self._speak_fixed(quiz_lines, t_start)
            return

        self.history.append({"role": "user", "content": tutor_ask(text)})
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
                model=OLLAMA_MODEL, messages=self.history, max_tokens=260, stream=True
            )
            async for chunk in stream:
                if not chunk.choices:
                    continue
                delta = chunk.choices[0].delta.content or ""
                reply += delta
                buf += delta
                while player and (piece := split_chunk(buf, not synth)):
                    sentence, buf = piece
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
        if len(self.history) > 25:
            self.history = [self.history[0]] + self.history[-24:]


async def listen(track: rtc.Track, vad_model: vad.VAD, convo: Conversation) -> None:
    audio_stream = rtc.AudioStream(track)
    vad_stream = vad_model.stream()

    async def push_frames() -> None:
        async for ev in audio_stream:
            if convo.hold_mode:
                if convo.collecting():
                    convo.ptt_frames.append(ev.frame)
            else:
                vad_stream.push_frame(ev.frame)
        vad_stream.end_input()

    pusher = asyncio.create_task(push_frames())
    try:
        async for ev in vad_stream:
            if ev.type == vad.VADEventType.START_OF_SPEECH:
                log.info(">>> speech started")
            elif ev.type == vad.VADEventType.END_OF_SPEECH:
                log.info("<<< speech ended (%.2fs)", ev.speech_duration)
                if convo.hold_mode or not ev.frames:
                    continue
                if convo.speaking or convo.busy:
                    asyncio.create_task(convo.heard_while_busy(ev.frames))
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
    try:
        await ctx.room.local_participant.set_attributes({"host": socket.gethostname()})
    except Exception as e:
        log.debug("could not publish host name: %s", e)
    await convo.set_state("listening")
    asyncio.create_task(convo.warm_up())
    tasks: list[asyncio.Task] = []

    @ctx.room.on("track_subscribed")
    def on_track_subscribed(track: rtc.Track, publication, participant: rtc.RemoteParticipant):
        if track.kind == rtc.TrackKind.KIND_AUDIO:
            log.info("audio track from %s", participant.identity)
            tasks.append(asyncio.create_task(listen(track, ctx.proc.userdata["vad"], convo)))

    def apply_phone_settings(participant: rtc.Participant) -> None:
        if participant.identity == ctx.room.local_participant.identity:
            return
        attrs = participant.attributes
        convo.apply_settings(attrs.get("voice"), attrs.get("speed"), attrs.get("mode"))
        convo.commands.set_catalog(attrs.get("catalog"))

    for existing in ctx.room.remote_participants.values():
        apply_phone_settings(existing)  # the phone may have joined before we did
        convo.text_signal(existing.attributes.get("text", ""))  # and may already have typed

    @ctx.room.on("participant_connected")
    def on_participant_connected(participant: rtc.RemoteParticipant):
        apply_phone_settings(participant)

    @ctx.room.on("participant_attributes_changed")
    def on_attributes_changed(changed: dict, participant: rtc.Participant):
        if "voice" in changed or "speed" in changed or "mode" in changed or "catalog" in changed:
            apply_phone_settings(participant)
        if "ptt" in changed and participant.identity != ctx.room.local_participant.identity:
            convo.ptt_signal(changed["ptt"])
        # The phone bumps "interrupt" when the user taps while she is thinking or speaking.
        if "interrupt" in changed and participant.identity != ctx.room.local_participant.identity:
            convo.interrupt()
        if "text" in changed and participant.identity != ctx.room.local_participant.identity:
            convo.text_signal(changed["text"])

    @ctx.room.on("data_received")
    def on_data_received(packet: rtc.DataPacket):
        if packet.topic == "interrupt":
            convo.interrupt()
        elif packet.topic == "voice":
            try:
                data = json.loads(packet.data.decode("utf-8"))
                convo.apply_settings(data.get("voice"), data.get("speed"), data.get("mode"))
            except (ValueError, AttributeError) as e:
                log.warning("bad voice packet: %s", e)
        elif packet.topic == "text":
            convo.text_signal(packet.data.decode("utf-8", errors="ignore"))
        elif packet.topic == "ptt":
            convo.ptt_signal(packet.data.decode("utf-8", errors="ignore"))

    @ctx.room.on("participant_disconnected")
    def on_participant_disconnected(participant: rtc.RemoteParticipant):
        log.info("%s left", participant.identity)


if __name__ == "__main__":
    cli.run_app(
        WorkerOptions(
            entrypoint_fnc=entrypoint,
            prewarm_fnc=prewarm,
            initialize_process_timeout=120,
            num_idle_processes=1,  # keep one process with the models loaded, ready for the next phone
        )
    )
