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
            ),
            QuizQuestion(
                id = "q4",
                prompt = SemanticUnit("q4_prompt", "Which three things does photosynthesis need?"),
                options = listOf(
                    QuizOption('A', "Sunlight, water, and carbon dioxide"),
                    QuizOption('B', "Soil, wind, and heat"),
                    QuizOption('C', "Only sunlight"),
                    QuizOption('D', "Oxygen, salt, and shade")
                ),
                correctIndex = 0,
                explanation = "A plant combines sunlight, water, and carbon dioxide to make its own sugar."
            ),
            QuizQuestion(
                id = "q5",
                prompt = SemanticUnit("q5_prompt", "What gas do plants release during photosynthesis?"),
                options = listOf(
                    QuizOption('A', "Carbon dioxide"),
                    QuizOption('B', "Nitrogen"),
                    QuizOption('C', "Hydrogen"),
                    QuizOption('D', "Oxygen")
                ),
                correctIndex = 3,
                explanation = "Splitting water to power the reaction leaves oxygen over, which the plant releases into the air."
            ),
            QuizQuestion(
                id = "q6",
                prompt = SemanticUnit("q6_prompt", "What is Earth's outer shell broken into, according to plate tectonics?"),
                options = listOf(
                    QuizOption('A', "A single solid shell"),
                    QuizOption('B', "Large moving plates"),
                    QuizOption('C', "Layers of ice"),
                    QuizOption('D', "Floating islands only"),
                ),
                correctIndex = 1,
                explanation = "Earth's crust is broken into large plates that carry the continents and ocean floors."
            ),
            QuizQuestion(
                id = "q7",
                prompt = SemanticUnit("q7_prompt", "What drags tectonic plates along?"),
                options = listOf(
                    QuizOption('A', "Ocean tides"),
                    QuizOption('B', "Earth's rotation"),
                    QuizOption('C', "Slow currents in the mantle"),
                    QuizOption('D', "Wind erosion")
                ),
                correctIndex = 2,
                explanation = "Heat drives slow currents in the mantle, and those currents drag the plates above them along."
            )
        )
    )
}
