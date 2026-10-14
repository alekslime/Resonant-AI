"""Tests for the spoken app commands. Run from agent/:  python -m unittest test_voice_commands"""
import unittest

from voice_commands import Commands

PROFILE = {"do": "open", "to": "profile"}


class ProfileCommandTest(unittest.TestCase):
    def setUp(self) -> None:
        self.c = Commands()

    def test_plain_keywords_open_the_profile(self) -> None:
        for text in ["profile", "my profile", "open my profile", "show me the profile screen",
                     "go to profile", "my progress", "show my progress", "stats", "my stats",
                     "open statistics", "account", "take me to my account", "Profile, please"]:
            with self.subTest(text=text):
                self.assertEqual(self.c.parse(text), PROFILE)

    def test_how_am_i_doing_opens_the_profile(self) -> None:
        for text in ["how am I doing", "How am I doing?", "hey resonant, how am i going"]:
            with self.subTest(text=text):
                self.assertEqual(self.c.parse(text), PROFILE)

    def test_gate_lets_every_profile_phrase_through(self) -> None:
        for text in ["profile", "my progress", "show my stats", "how am i doing", "open account"]:
            with self.subTest(text=text):
                self.assertTrue(self.c.might_be_command(text))

    def test_longer_questions_are_not_profile_commands(self) -> None:
        for text in ["what is my progress in binary search", "explain statistics to me",
                     "what are you doing"]:
            with self.subTest(text=text):
                self.assertNotEqual(self.c.parse(text), PROFILE)

    def test_the_router_can_answer_profile(self) -> None:
        self.assertEqual(self.c.from_intent({"intent": "profile"}), PROFILE)

    def test_router_prompt_knows_about_the_profile(self) -> None:
        system = self.c.router_messages("show me my progress")[0]["content"]
        self.assertIn("profile", system)

    def test_existing_commands_still_work(self) -> None:
        self.assertEqual(self.c.parse("settings"), {"do": "open", "to": "settings"})
        self.assertEqual(self.c.parse("go back"), {"do": "back"})
        self.assertEqual(self.c.parse("home"), {"do": "open", "to": "home"})
        self.assertEqual(self.c.parse("lessons"), {"do": "open", "to": "lessons"})
        self.assertIsNone(self.c.parse("what is photosynthesis"))


if __name__ == "__main__":
    unittest.main()
