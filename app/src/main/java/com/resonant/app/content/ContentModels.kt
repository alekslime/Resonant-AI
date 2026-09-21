package com.resonant.app.content

/**
 * The atomic unit of speech. Nothing in Resonant speaks a whole page at once —
 * everything is broken into semantic units so it can be paused, repeated, and
 * resumed without losing the user's place.
 */
data class SemanticUnit(
    val id: String,
    val text: String
)

data class LessonSection(
    val id: String,
    val title: String,
    val units: List<SemanticUnit>,
    /**
     * Extra words a person might use when asking about this section, beyond the ones
     * already in its title and unit text ("steps" for the algorithm, "instance" for
     * the example). Used only by [com.resonant.app.content.OfflineAnswers] to pick a
     * section when the AI server is unreachable. Leave question words (what / how / why)
     * out of these lists — they'd match every section equally and add nothing.
     */
    val keywords: List<String> = emptyList()
)

data class Lesson(
    val id: String,
    val title: String,
    val sections: List<LessonSection>
)

data class QuizOption(
    val letter: Char,
    val text: String
)

data class QuizQuestion(
    val id: String,
    val prompt: SemanticUnit,
    val options: List<QuizOption>,
    val correctIndex: Int,
    val explanation: String
)

data class QuizSet(
    val id: String,
    val title: String,
    val questions: List<QuizQuestion>
)

data class ChatExchange(
    val userText: String,
    val assistantChunks: List<SemanticUnit>
)
