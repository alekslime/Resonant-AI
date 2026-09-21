package com.resonant.app.content

import com.resonant.app.speech.SentenceChunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAnswersTest {

    @Test
    fun every_reply_starts_with_the_offline_preface() {
        assertTrue(OfflineAnswers.answerFor("What is binary search?").startsWith(OfflineAnswers.PREFACE))
        assertTrue(OfflineAnswers.answerFor("banana kayak umbrella").startsWith("I can't reach the AI server right now"))
    }

    @Test
    fun a_question_matching_a_titles_words_returns_that_section() {
        val spoken = OfflineAnswers.answerFor("What is binary search?")
        assertTrue(spoken, spoken.contains("method for finding an item"))
    }

    @Test
    fun matching_is_case_insensitive() {
        val spoken = OfflineAnswers.answerFor("BINARY SEARCH")
        assertTrue(spoken, spoken.contains("method for finding an item"))
    }

    @Test
    fun a_sections_own_keywords_are_enough_to_match_it() {
        // "show" and "instance" only appear in section 4's keyword list, not its
        // title, so this exercises the keyword half of the index specifically.
        val spoken = OfflineAnswers.answerFor("Can you show an instance of this?")
        assertTrue(spoken, spoken.contains("searching for the number seven"))
    }

    @Test
    fun stemming_matches_a_different_inflection_of_a_keyword() {
        // The section title says "sorting"; asking about the list being "sorted"
        // should still land on it because both stem to the same root.
        val spoken = OfflineAnswers.answerFor("Does the list need to be sorted first?")
        assertTrue(spoken, spoken.contains("only works if the list is already sorted"))
    }

    @Test
    fun an_unrelated_question_gets_the_fallback_with_topic_list() {
        val spoken = OfflineAnswers.answerFor("Tell me about your favorite color")
        assertTrue(spoken, spoken.contains("couldn't match that to the lesson"))
        assertTrue(spoken, spoken.contains("what is binary search?"))
        assertTrue(spoken, spoken.contains("key takeaway"))
        assertFalse(spoken, spoken.startsWith(OfflineAnswers.PREFACE))
    }

    @Test
    fun a_question_of_only_stopwords_gets_the_fallback() {
        val spoken = OfflineAnswers.answerFor("What is this?")
        assertTrue(spoken, spoken.contains("couldn't match that to the lesson"))
    }

    @Test
    fun raw_server_style_text_never_appears() {
        // OfflineAnswers only ever plays back lesson content, so nothing here should
        // look like it came from a model or a server response.
        val spoken = OfflineAnswers.answerFor("What is binary search?")
        assertFalse(spoken, spoken.contains("{"))
    }

    @Test
    fun a_matched_answer_splits_into_several_speakable_sentences() {
        // ChatScreen feeds the offline reply through SentenceChunker so it becomes one
        // SemanticUnit per sentence, like a streamed answer — that's what makes pause,
        // repeat and step-back work on it. If the reply ever stopped splitting, the
        // offline path would silently become the one answer you can't navigate.
        val chunker = SentenceChunker()
        val text = OfflineAnswers.answerFor("What is binary search?")
        val sentences = chunker.feed(text) + listOfNotNull(chunker.flush())

        assertTrue(sentences.toString(), sentences.size >= 3)
        assertTrue(sentences.toString(), sentences.first().startsWith("I can't reach"))
        assertEquals(text.replace(Regex("\\s+"), " ").trim(), sentences.joinToString(" "))
    }

    @Test
    fun a_single_incidental_word_is_not_enough_to_match() {
        // "show" alone is worth one OTHER_WORD_WEIGHT point (it's a keyword on the
        // Example section), which sits below MIN_SCORE, so it should not be trusted
        // as a real match on its own.
        val spoken = OfflineAnswers.answerFor("show")
        assertTrue(spoken, spoken.contains("couldn't match that to the lesson"))
    }
}
