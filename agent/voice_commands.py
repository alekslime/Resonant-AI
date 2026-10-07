"""Spoken app commands for the Resonant agent. Pure logic, no audio.

Commands.parse(text) returns one of:
  {"do": "open", "to": "home" | "lessons" | "quizzes" | "settings"}
  {"do": "open", "to": "lesson" | "quiz", "id": "..."}
  {"do": "back"}
  {"say": "..."}   the agent should answer aloud (a name was not found, or is unclear)
  None             the text is not a command

The lesson and quiz names come from the phone (set_catalog), so nothing is hard-coded here.
"""
import json
import re

VERBS = {
    "open", "go", "take", "show", "launch", "start", "play", "read", "begin", "bring",
    "switch", "navigate",
}
ANY_OPEN = {"open", "go", "show", "take", "switch", "navigate", "bring", "launch"}
FILLER = {
    "please", "ok", "okay", "hey", "resonant", "now", "can", "could", "would", "you", "i",
    "want", "like", "the", "a", "an", "my", "our", "lets", "let's", "let", "us", "me", "to",
    "into", "up", "screen", "page", "tab", "for", "on", "about", "of",
}
TITLE_STOP = {"introduction", "to", "the", "and", "their", "in", "of", "a", "an"}
MATCH_MIN = 0.5


def stem(word: str) -> str:
    return word[:-1] if len(word) > 3 and word.endswith("s") else word


def words_of(text: str) -> list[str]:
    return re.sub(r"[^a-z0-9' ]", " ", text.lower()).split()


class Commands:
    def __init__(self) -> None:
        self.lessons: list[tuple[str, str]] = []
        self.quizzes: list[tuple[str, str]] = []

    def set_catalog(self, raw: str | None) -> None:
        try:
            data = json.loads(raw or "")
            self.lessons = [(str(i), str(t)) for i, t in data.get("lessons", [])]
            self.quizzes = [(str(i), str(t)) for i, t in data.get("quizzes", [])]
        except (ValueError, TypeError, AttributeError):
            pass

    def parse(self, text: str) -> dict | None:
        words = words_of(text)
        verbs = set(words) & VERBS
        rest = [w for w in words if w not in VERBS and w not in FILLER]
        if not rest:
            return None
        if rest == ["back"] and len(words) <= 3:
            return {"do": "back"}
        if rest in (["home"], ["main", "menu"], ["menu"]):
            return {"do": "open", "to": "home"}
        if rest in (["settings"], ["setting"]):
            return {"do": "open", "to": "settings"}
        if rest == ["lessons"]:
            return {"do": "open", "to": "lessons"}
        if rest == ["quizzes"]:
            return {"do": "open", "to": "quizzes"}
        if not verbs:
            return None
        if rest == ["lesson"] and verbs & ANY_OPEN:
            return {"do": "open", "to": "lessons"}
        if rest == ["quiz"] and verbs & ANY_OPEN:
            return {"do": "open", "to": "quizzes"}
        if rest in (["lesson"], ["quiz"]):
            return None  # "start a quiz" stays with the spoken quiz
        if "lesson" in rest or "lessons" in rest:
            return self._named("lesson", self.lessons, rest)
        if "quiz" in rest or "quizzes" in rest:
            return self._named("quiz", self.quizzes, rest)
        return None

    def _named(self, kind: str, items: list[tuple[str, str]], rest: list[str]) -> dict:
        wanted = {stem(w) for w in rest if w not in ("lesson", "lessons", "quiz", "quizzes")}
        if not wanted:
            return {"do": "open", "to": "lessons" if kind == "lesson" else "quizzes"}
        if not items:
            return {"say": f"I don't have your {kind}s yet. Try again in a moment."}
        scored = []
        for item_id, title in items:
            tokens = {stem(w) for w in words_of(title) if w not in TITLE_STOP}
            scored.append((len(tokens & wanted) / len(tokens) if tokens else 0.0, item_id, title))
        scored.sort(reverse=True)
        best = scored[0]
        if best[0] < MATCH_MIN:
            return {"say": f"I couldn't find a {kind} called {' '.join(rest_name(rest))}."}
        tied = [s for s in scored if s[0] == best[0]]
        if len(tied) > 1:
            names = ", or ".join(t for _, _, t in tied[:3])
            return {"say": f"Which {kind} do you mean: {names}?"}
        return {"do": "open", "to": kind, "id": best[1]}


def rest_name(rest: list[str]) -> list[str]:
    return [w for w in rest if w not in ("lesson", "lessons", "quiz", "quizzes")]
