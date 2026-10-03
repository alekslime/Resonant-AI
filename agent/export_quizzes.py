"""Copy the app's quizzes into agent/quizzes.json so the voice agent can run them.
Run from the repo root or from agent/:  python export_quizzes.py
Run it again whenever QuizData.kt changes."""
import json
import re
from pathlib import Path

HERE = Path(__file__).parent
SOURCE = HERE.parent / "app/src/main/java/com/resonant/app/content/QuizData.kt"
TARGET = HERE / "quizzes.json"

STRING = r'"((?:\\.|[^"\\])*)"'


def unescape(text: str) -> str:
    return text.replace('\\"', '"').replace("\\\\", "\\").replace("\\n", " ")


def main() -> None:
    source = SOURCE.read_text(encoding="utf-8")
    sets = []
    for block in re.split(r"\bval \w+ = QuizSet\(", source)[1:]:
        title = re.search(r"title = " + STRING, block)
        category = re.search(r"category = " + STRING, block)
        questions = []
        for q in block.split("QuizQuestion(")[1:]:
            prompt = re.search(r"SemanticUnit\(" + STRING + r",\s*" + STRING, q)
            options = re.findall(r"QuizOption\('([A-D])',\s*" + STRING + r"\)", q)
            correct = re.search(r"correctIndex = (\d+)", q)
            explanation = re.search(r"explanation = " + STRING, q)
            if not (prompt and options and correct and explanation):
                raise SystemExit("could not read a question near: " + q[:80])
            questions.append({
                "prompt": unescape(prompt.group(2)),
                "options": [unescape(text) for _, text in options],
                "correct": int(correct.group(1)),
                "explanation": unescape(explanation.group(1)),
            })
        if not (title and questions):
            raise SystemExit("could not read a quiz set")
        sets.append({
            "title": unescape(title.group(1)),
            "category": unescape(category.group(1)) if category else "",
            "questions": questions,
        })
    TARGET.write_text(json.dumps(sets, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"wrote {TARGET.name}: {len(sets)} quizzes, {sum(len(s['questions']) for s in sets)} questions")


if __name__ == "__main__":
    main()
