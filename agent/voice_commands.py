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
GATE = re.compile(
    r"\b(lessons?|quiz|quizzes|settings?|home|menu|back|page|screen|open|go|take|show|switch|"
    r"navigate|launch|start|bring)\b"
)
INTENTS = {"home", "lessons", "quizzes", "settings", "back", "lesson", "quiz"}
LIST_OF = {"lesson": "lessons", "quiz": "quizzes"}


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

    def might_be_command(self, text: str) -> bool:
        """Cheap check: does the text have any word that could mean "move to another screen"?"""
        return bool(GATE.search(text.lower()))

    def router_messages(self, text: str) -> list[dict]:
        """The question put to the language model: which screen, if any, is the student asking for?"""
        lessons = "; ".join(f"{i} = {t}" for i, t in self.lessons) or "none"
        quizzes = "; ".join(f"{i} = {t}" for i, t in self.quizzes) or "none"
        lesson_id, lesson_title = self.lessons[0] if self.lessons else ("lesson_id", "a lesson")
        system = (
            "You are a router inside a learning app. Decide whether the student's message asks "
            "the app to move to another screen. Answer with one JSON object and nothing else, "
            'like {"intent": "none"}.\n'
            "Intents: none, home, lessons, quizzes, settings, back, lesson, quiz.\n"
            "lessons, quizzes and settings open that list or screen. back goes to the previous "
            "screen. home goes to the main menu.\n"
            "Use lesson or quiz only when the student names one of the items below, and add its id, "
            'like {"intent": "lesson", "id": "ID"}. If they name a lesson or quiz that is not in the '
            'list, use the same intent with "id": "unknown".\n'
            "Use none for everything else: questions about a topic, asking to be taught or quizzed, "
            "chatting, or talking about a lesson without asking to open it.\n"
            f"Lessons: {lessons}\nQuizzes: {quizzes}\n"
            "Examples:\n"
            'can you open up the lessons for me? i went to the wrong page -> {"intent": "lessons"}\n'
            'take me back -> {"intent": "back"}\n'
            f'i want to read {lesson_title} -> {{"intent": "lesson", "id": "{lesson_id}"}}\n'
            'what is photosynthesis -> {"intent": "none"}\n'
            "i did not understand that lesson, explain it again -> "
            '{"intent": "none"}'
        )
        return [{"role": "system", "content": system}, {"role": "user", "content": text}]

    def from_intent(self, data: dict) -> dict | None:
        """Turn the model's answer into a command. Anything unclear becomes None (just chat)."""
        if not isinstance(data, dict):
            return None
        intent = str(data.get("intent", "none")).strip().lower()
        if intent not in INTENTS:
            return None
        if intent == "back":
            return {"do": "back"}
        if intent in LIST_OF:
            items = self.lessons if intent == "lesson" else self.quizzes
            item_id = str(data.get("id", "")).strip()
            if not item_id:
                return {"do": "open", "to": LIST_OF[intent]}
            if item_id in {i for i, _ in items}:
                return {"do": "open", "to": intent, "id": item_id}
            return {"say": f"I couldn't find that {intent}."}
        return {"do": "open", "to": intent}

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

    def _named(self, kind: str, items: list[tuple[str, str]], rest: list[str]) -> dict | None:
        """A clear match only. Anything else returns None, so the language model decides."""
        wanted = {stem(w) for w in rest if w not in ("lesson", "lessons", "quiz", "quizzes")}
        if not wanted:
            return {"do": "open", "to": "lessons" if kind == "lesson" else "quizzes"}
        if not items:
            return None
        scored = []
        for item_id, title in items:
            tokens = {stem(w) for w in words_of(title) if w not in TITLE_STOP}
            scored.append((len(tokens & wanted) / len(tokens) if tokens else 0.0, item_id, title))
        scored.sort(reverse=True)
        best = scored[0]
        if best[0] < MATCH_MIN:
            return None
        if len([s for s in scored if s[0] == best[0]]) > 1:
            return None
        return {"do": "open", "to": kind, "id": best[1]}
