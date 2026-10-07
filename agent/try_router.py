"""Try the spoken-command router without the phone.
Usage:  python try_router.py "can you open up the lessons for me, i went to the wrong page"
        python try_router.py            (runs a list of sample phrases)
"""
import asyncio
import json
import os
import sys
import time

from dotenv import load_dotenv
from openai import AsyncOpenAI

from voice_commands import Commands

load_dotenv()
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/v1")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.2")

CATALOG = json.dumps({
    "lessons": [
        ["lesson_binary_search", "Introduction to Binary Search"],
        ["lesson_photosynthesis", "Introduction to Photosynthesis"],
        ["lesson_plate_tectonics", "Introduction to Plate Tectonics"],
        ["lesson_cells", "Cells and their parts"],
        ["lesson_ecosystems", "Energy in ecosystems"],
        ["lesson_plant_adaptations", "Plant adaptations"],
    ],
    "quizzes": [
        ["quiz_photosynthesis", "Photosynthesis basics"],
        ["quiz_cells", "Cells and their parts"],
        ["quiz_ecosystems", "Energy in ecosystems"],
        ["quiz_plant_adaptations", "Plant adaptations"],
    ],
})

SAMPLES = [
    "can you open up the lessons for me? i went to the wrong page",
    "hmm, i think i want to see the quizzes now",
    "take me to the cells lesson please",
    "i'd like to hear about plate tectonics, the lesson you have",
    "let's go back to where we were",
    "can you open my settings",
    "put me on the home screen",
    "open the volcano lesson",
    "what is the powerhouse of the cell",
    "i did not understand that lesson, explain it again",
    "teach me how to go home from school safely",
]


async def main() -> None:
    commands = Commands()
    commands.set_catalog(CATALOG)
    llm = AsyncOpenAI(base_url=OLLAMA_URL, api_key="ollama")
    phrases = sys.argv[1:] or SAMPLES
    for text in phrases:
        if not commands.might_be_command(text):
            print(f"{text!r}\n   -> (no navigation words, goes straight to the tutor)\n")
            continue
        t0 = time.time()
        resp = await llm.chat.completions.create(
            model=OLLAMA_MODEL,
            messages=commands.router_messages(text),
            max_tokens=60,
            temperature=0,
            response_format={"type": "json_object"},
        )
        raw = resp.choices[0].message.content or "{}"
        try:
            result = commands.from_intent(json.loads(raw))
        except ValueError:
            result = None
        print(f"{text!r}\n   model said {raw.strip()} ({time.time() - t0:.1f}s)\n   -> {result}\n")


asyncio.run(main())
