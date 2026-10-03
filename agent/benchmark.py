"""Time every piece of the first-audio delay on THIS PC and say which setup is fastest.

Run from agent/ with the venv on, Ollama running:   python benchmark.py
It tries each Kokoro model file in models/ (fp32 and int8), each Whisper size in
BENCH_WHISPER (default tiny.en,base.en,small.en; a size you don't have is downloaded once),
and Ollama's time to the first word. Then it prints the lines to paste into .env.
The test speech is made by Kokoro itself, so it is cleaner than a phone mic: the accuracy
column is a ceiling, not a promise.
"""
import asyncio
import os
import time
from pathlib import Path

import numpy as np
from dotenv import load_dotenv

load_dotenv()
HERE = Path(__file__).parent
VOICES = Path(os.getenv("KOKORO_VOICES", HERE / "models" / "voices-v1.0.bin"))
VOICE = os.getenv("KOKORO_VOICE", "af_heart")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/v1")
WHISPER_SIZES = [s.strip() for s in os.getenv("BENCH_WHISPER", "tiny.en,base.en,small.en").split(",") if s.strip()]
SPEECH = "The chloroplast is where a plant cell makes sugar from sunlight, water and carbon dioxide."
FIRST_PIECE = "Chloroplasts make sugar from light, water and air."
VAD_WAIT_S = 0.8  # the agent waits this long after you stop talking before it starts working
MIN_ACCURACY = 0.9


def words(text: str) -> list[str]:
    return [w for w in "".join(c.lower() if c.isalnum() or c == " " else " " for c in text).split()]


def kokoro_files() -> list[Path]:
    found = [p for p in (HERE / "models" / "kokoro-v1.0.int8.onnx", HERE / "models" / "kokoro-v1.0.onnx") if p.exists()]
    extra = os.getenv("KOKORO_MODEL")
    if extra and Path(extra).exists() and Path(extra) not in found:
        found.append(Path(extra))
    return found


def bench_kokoro() -> tuple[list[dict], tuple[np.ndarray, int] | None]:
    results, speech = [], None
    if not VOICES.exists():
        print(f"voices file missing: {VOICES}")
        return results, speech
    from kokoro_onnx import Kokoro

    for path in kokoro_files():
        print(f"kokoro {path.name} ...", flush=True)
        t0 = time.time()
        kokoro = Kokoro(str(path), str(VOICES))
        load = time.time() - t0
        kokoro.create(FIRST_PIECE, voice=VOICE, speed=1.0, lang="en-us")  # warm-up
        t0 = time.time()
        samples, rate = kokoro.create(FIRST_PIECE, voice=VOICE, speed=1.0, lang="en-us")
        first_piece = time.time() - t0
        results.append({"path": path, "load": load, "first_piece": first_piece, "audio": len(samples) / rate})
        if speech is None:
            speech = kokoro.create(SPEECH, voice=VOICE, speed=1.0, lang="en-us")
    return results, speech


def to_16k(samples: np.ndarray, rate: int) -> np.ndarray:
    n_out = int(len(samples) * 16000 / rate)
    return np.interp(np.linspace(0, len(samples) - 1, n_out), np.arange(len(samples)), samples).astype(np.float32)


def bench_whisper(audio: np.ndarray) -> list[dict]:
    from faster_whisper import WhisperModel

    reference = set(words(SPEECH))
    results = []
    for size in WHISPER_SIZES:
        print(f"whisper {size} ...", flush=True)
        try:
            t0 = time.time()
            model = WhisperModel(size, device="cpu", compute_type="int8")
            load = time.time() - t0
        except Exception as e:
            print(f"  skipped {size}: {e}")
            continue

        def run() -> str:
            segments, _ = model.transcribe(
                audio, language="en", beam_size=1, temperature=0.0,
                condition_on_previous_text=False, without_timestamps=True, vad_filter=False,
            )
            return " ".join(s.text.strip() for s in segments)

        run()  # warm-up
        t0 = time.time()
        text = run()
        took = time.time() - t0
        heard = set(words(text))
        results.append({
            "size": size, "load": load, "time": took,
            "accuracy": len(reference & heard) / len(reference),
        })
    return results


async def bench_ollama() -> float | None:
    from openai import AsyncOpenAI

    print(f"ollama {OLLAMA_MODEL} ...", flush=True)
    client = AsyncOpenAI(base_url=OLLAMA_URL, api_key="ollama")
    first_word = None
    try:
        for attempt in range(2):  # the first call loads the model
            t0 = time.time()
            stream = await client.chat.completions.create(
                model=OLLAMA_MODEL, stream=True, max_tokens=30,
                messages=[{"role": "user", "content": "Say hello in one short sentence."}],
            )
            async for chunk in stream:
                if chunk.choices and chunk.choices[0].delta.content:
                    first_word = time.time() - t0
                    break
            await stream.close()
        return first_word
    except Exception as e:
        print(f"  ollama failed (is it running, is '{OLLAMA_MODEL}' pulled?): {e}")
        return None


def main() -> None:
    kokoro, speech = bench_kokoro()
    whisper = bench_whisper(to_16k(*speech)) if speech else []
    llm = asyncio.run(bench_ollama())

    print("\n--- results (seconds) ---")
    for k in kokoro:
        print(f"kokoro  {k['path'].name:<26} load {k['load']:5.1f}   first piece {k['first_piece']:5.2f}  (makes {k['audio']:.1f}s of speech)")
    for w in whisper:
        print(f"whisper {w['size']:<26} load {w['load']:5.1f}   transcribe {w['time']:5.2f}   words right {w['accuracy']:.0%}")
    if llm is not None:
        print(f"ollama  {OLLAMA_MODEL:<26} first word {llm:5.2f}")
    if not (kokoro and whisper and llm is not None):
        print("\nSomething above is missing, so no total can be worked out. Fix that and run again.")
        return

    best_voice = min(kokoro, key=lambda k: k["first_piece"])
    good = [w for w in whisper if w["accuracy"] >= MIN_ACCURACY] or whisper
    best_ears = min(good, key=lambda w: w["time"])
    total = VAD_WAIT_S + best_ears["time"] + llm + best_voice["first_piece"]
    print(
        f"\nEstimated wait for the first audio: about {total:.1f}s "
        f"({VAD_WAIT_S}s waiting for you to finish + {best_ears['time']:.1f}s hearing + "
        f"{llm:.1f}s thinking + {best_voice['first_piece']:.1f}s first voice piece)"
    )
    print("\nPaste into agent/.env:")
    print(f"KOKORO_MODEL={best_voice['path'].relative_to(HERE).as_posix()}")
    print(f"WHISPER_MODEL={best_ears['size']}")
    if best_ears["size"] != whisper[0]["size"] and best_ears["accuracy"] < 1:
        print("(the faster Whisper sizes misheard some words; real mic audio will be worse than this test)")


if __name__ == "__main__":
    main()
