"""Spoken quiz for the Resonant agent. Pure logic, no audio: Quiz.respond(text) returns the
lines to speak, or None when the text has nothing to do with a quiz. The quizzes come from
quizzes.json (made by export_quizzes.py)."""
import json
import random
import re
from pathlib import Path

START = re.compile(
    r"\b(quiz me|start (?:a |the |my |our )?quiz|give me a quiz|test me|let'?s do a quiz|"
    r"quiz time|do a quiz|take a quiz|play a quiz)\b"
)
EXIT = re.compile(
    r"\b(?:stop|end|quit|exit|cancel|leave)(?: the| this| my)? quiz\b|\bno more (?:questions|quiz)\b|"
    r"\bi'?m done\b|\bthat'?s enough\b|\bforget it\b"
)
REPEAT = re.compile(r"\b(repeat|again|say that again|what was the question|read it)\b")
SKIP = re.compile(r"\b(skip|pass|i don'?t know|no idea|not sure|give up)\b")
ANYTHING = re.compile(r"\b(any|anything|random|surprise me|you choose|you pick|whatever)\b")

LETTERS = {"a": 0, "ay": 0, "eh": 0, "b": 1, "be": 1, "bee": 1, "c": 2, "see": 2, "sea": 2, "d": 3, "dee": 3}
ORDINALS = {"first": 0, "second": 1, "third": 2, "fourth": 3, "last": 3}
LABELS = "ABCD"
ANSWER_FILLER = {
    "the", "answer", "is", "its", "it's", "it", "i", "think", "say", "going", "with", "pick",
    "choose", "go", "um", "uh", "maybe", "hmm", "option", "letter", "choice", "number", "my", "guess",
}
TOPIC_STOP = {
    "quiz", "about", "please", "start", "give", "test", "lets", "let's", "take", "play", "time",
    "like", "would", "want", "with", "some", "something", "the", "and", "on", "of", "for",
}
TEXT_STOP = {"the", "a", "an", "of", "and", "to", "in", "is", "they", "their", "it"}
MATCH_MIN = 0.75


def normalize(text: str) -> str:
    return " ".join(re.sub(r"[^a-z0-9' ]", " ", text.lower()).split())


def stem(word: str) -> str:
    return word[:-1] if len(word) > 3 and word.endswith("s") else word


def parse_answer(text: str, options: list[str]) -> int | None:
    """Which option (0-3) the person chose, or None if it is not clear."""
    t = normalize(text)
    explicit = re.search(r"\b(?:option|answer|letter|choice|number)\s+(\w+)", t)
    if explicit:
        word = explicit.group(1)
        if word in LETTERS:
            return LETTERS[word]
        if word in ORDINALS:
            return ORDINALS[word]
    rest = [w for w in t.split() if w not in ANSWER_FILLER]
    if len(rest) == 1 and rest[0] in LETTERS:
        return LETTERS[rest[0]]
    if len(rest) == 1 and rest[0] in ORDINALS:
        return ORDINALS[rest[0]]
    said = {stem(w) for w in t.split() if w not in TEXT_STOP}
    scores = []
    for option in options:
        tokens = {stem(w) for w in normalize(option).split() if w not in TEXT_STOP}
        scores.append(len(tokens & said) / len(tokens) if tokens else 0.0)
    ranked = sorted(scores, reverse=True)
    if ranked[0] >= MATCH_MIN and (len(ranked) < 2 or ranked[0] > ranked[1]):
        return scores.index(ranked[0])
    return None


class Quiz:
    def __init__(self, sets: list[dict]) -> None:
        self.sets = sets
        self.current: dict | None = None
        self.index = 0
        self.score = 0
        self.choosing = False

    @classmethod
    def load(cls, path: Path) -> "Quiz":
        try:
            return cls(json.loads(path.read_text(encoding="utf-8")))
        except (OSError, ValueError):
            return cls([])

    def in_progress(self) -> bool:
        return self.current is not None or self.choosing

    def wants_exit(self, text: str) -> bool:
        return self.in_progress() and bool(EXIT.search(normalize(text)))

    def end(self) -> None:
        self.current = None
        self.choosing = False

    # ---- picking a quiz ---------------------------------------------------------
    def _pick_set(self, t: str) -> dict | None:
        words = {stem(w) for w in t.split() if len(w) > 3 and w not in TOPIC_STOP}
        scored = []
        for s in self.sets:
            tokens = {stem(w) for w in normalize(s["title"] + " " + s["category"]).split() if len(w) > 3}
            scored.append((len(tokens & words), s))
        scored.sort(key=lambda pair: -pair[0])
        if scored and scored[0][0] > 0 and (len(scored) < 2 or scored[0][0] > scored[1][0]):
            return scored[0][1]
        for word in t.split():
            if word in ORDINALS and ORDINALS[word] < len(self.sets):
                return self.sets[ORDINALS[word]]
        return None

    def _names(self) -> str:
        titles = [s["title"] for s in self.sets]
        return ", ".join(titles[:-1]) + ", and " + titles[-1] if len(titles) > 1 else titles[0]

    def _begin(self, t: str) -> list[str]:
        chosen = self._pick_set(t)
        if chosen is None and len(self.sets) == 1:
            chosen = self.sets[0]
        if chosen is None and ANYTHING.search(t):
            chosen = random.choice(self.sets)
        if chosen is None:
            self.choosing = True
            return [f"I have {len(self.sets)} quizzes: {self._names()}.", "Which one would you like?"]
        self.choosing = False
        self.current, self.index, self.score = chosen, 0, 0
        return [f"Okay, {chosen['title']}. {len(chosen['questions'])} questions. Say A, B, C or D."] + self._question()

    def _question(self) -> list[str]:
        q = self.current["questions"][self.index]
        lines = [f"Question {self.index + 1} of {len(self.current['questions'])}. {q['prompt']}"]
        lines += [f"{LABELS[i]}. {text}." for i, text in enumerate(q["options"])]
        return lines

    def _finish(self) -> list[str]:
        total = len(self.current["questions"])
        score = self.score
        self.end()
        return [f"That was the last question. You got {score} out of {total}."]

    # ---- the one entry point ----------------------------------------------------
    def respond(self, text: str) -> list[str] | None:
        if not self.sets:
            return None
        t = normalize(text)
        if not self.in_progress():
            return self._begin(t) if START.search(t) else None
        if EXIT.search(t):
            if self.current is None:
                self.end()
                return ["Okay, no quiz."]
            answered = self.index
            self.end()
            return [f"Okay, quiz stopped. You got {self.score} out of {answered} so far."] if answered else ["Okay, quiz stopped."]
        if self.choosing:
            chosen = self._pick_set(t) or (random.choice(self.sets) if ANYTHING.search(t) else None)
            if chosen is None:
                return ["Sorry, which quiz? " + self._names() + "."]
            return self._begin(chosen["title"].lower())
        question = self.current["questions"][self.index]
        if REPEAT.search(t):
            return self._question()
        correct = question["correct"]
        answer_text = f"{LABELS[correct]}, {question['options'][correct]}."
        if SKIP.search(t):
            lines = [f"No problem. The answer is {answer_text}", question["explanation"]]
        else:
            picked = parse_answer(t, question["options"])
            if picked is None:
                return ["I didn't catch an answer. Say A, B, C or D, or say repeat."]
            if picked == correct:
                self.score += 1
                lines = ["Correct!", question["explanation"]]
            else:
                lines = [f"Not quite. The answer is {answer_text}", question["explanation"]]
        self.index += 1
        if self.index >= len(self.current["questions"]):
            return lines + self._finish()
        return lines + self._question()
