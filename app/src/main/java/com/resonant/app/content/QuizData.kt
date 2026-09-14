package com.resonant.app.content

object QuizData {

    val sampleQuiz = QuizSet(
        id = "quiz_1",
        title = "Quick Quiz",
        questions = listOf(
            QuizQuestion(
                id = "q1",
                prompt = SemanticUnit("q1_prompt", "Which planet is known as the Red Planet?"),
                options = listOf(
                    QuizOption('A', "Venus"),
                    QuizOption('B', "Mars"),
                    QuizOption('C', "Jupiter"),
                    QuizOption('D', "Mercury")
                ),
                correctIndex = 1,
                explanation = "Mars appears red due to iron oxide, or rust, covering much of its surface."
            ),
            QuizQuestion(
                id = "q2",
                prompt = SemanticUnit("q2_prompt", "Binary search requires the list to be in what condition?"),
                options = listOf(
                    QuizOption('A', "Sorted"),
                    QuizOption('B', "Reversed"),
                    QuizOption('C', "Shuffled"),
                    QuizOption('D', "Duplicated")
                ),
                correctIndex = 0,
                explanation = "Binary search relies on order to decide which half of the list to eliminate."
            ),
            QuizQuestion(
                id = "q3",
                prompt = SemanticUnit("q3_prompt", "What does each step of binary search eliminate?"),
                options = listOf(
                    QuizOption('A', "One item"),
                    QuizOption('B', "Nothing"),
                    QuizOption('C', "Half of the remaining items"),
                    QuizOption('D', "The entire list")
                ),
                correctIndex = 2,
                explanation = "Cutting the remaining range in half on every step is what gives binary search its speed."
            )
        )
    )
}
