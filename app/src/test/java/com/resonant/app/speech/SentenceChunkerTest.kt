package com.resonant.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceChunkerTest {

    /** Feeds [text] in one piece; returns (sentences released by feed, flushed remainder). */
    private fun chunk(text: String): Pair<List<String>, String?> {
        val c = SentenceChunker()
        val sentences = c.feed(text)
        return sentences to c.flush()
    }

    @Test
    fun splits_on_terminator_followed_by_whitespace() {
        val (sentences, rest) = chunk("Hello there. How are you? Fine.")
        assertEquals(listOf("Hello there.", "How are you?"), sentences)
        assertEquals("Fine.", rest)
    }

    @Test
    fun waits_for_the_whitespace_before_releasing_a_sentence() {
        val c = SentenceChunker()
        assertTrue(c.feed("Hi there.").isEmpty())
        assertEquals(listOf("Hi there."), c.feed(" "))
    }

    @Test
    fun does_not_split_inside_a_decimal_number() {
        val (sentences, rest) = chunk("It costs 3.5 dollars. Okay.")
        assertEquals(listOf("It costs 3.5 dollars."), sentences)
        assertEquals("Okay.", rest)
    }

    @Test
    fun does_not_split_after_common_abbreviations() {
        val (sentences, rest) = chunk("Ask Dr. Smith today. Then go.")
        assertEquals(listOf("Ask Dr. Smith today."), sentences)
        assertEquals("Then go.", rest)
    }

    @Test
    fun a_bare_list_number_is_not_its_own_sentence() {
        val (sentences, rest) = chunk("Steps:\n1. Boil water. 2. Add tea.")
        assertEquals(listOf("Steps:", "1. Boil water."), sentences)
        assertEquals("2. Add tea.", rest)
    }

    @Test
    fun a_closing_quote_stays_with_its_sentence() {
        val (sentences, rest) = chunk("He said \"no.\" Then he left.")
        assertEquals(listOf("He said \"no.\""), sentences)
        assertEquals("Then he left.", rest)
    }

    @Test
    fun an_ellipsis_ends_one_sentence_not_three() {
        val (sentences, rest) = chunk("Well... maybe. Yes")
        assertEquals(listOf("Well...", "maybe."), sentences)
        assertEquals("Yes", rest)
    }

    @Test
    fun feeding_one_character_at_a_time_matches_feeding_all_at_once() {
        val text = "First one. Second, with 2.5 in it! Third? Dr. Who said \"hi.\" Done"
        val whole = SentenceChunker().let { it.feed(text) + listOfNotNull(it.flush()) }

        val byChar = SentenceChunker().let { c ->
            val out = mutableListOf<String>()
            text.forEach { out += c.feed(it.toString()) }
            out + listOfNotNull(c.flush())
        }
        assertEquals(whole, byChar)
    }

    @Test
    fun flush_on_an_empty_buffer_is_null() {
        assertNull(SentenceChunker().flush())
    }

    @Test
    fun markdown_the_model_should_not_have_sent_is_stripped() {
        val (sentences, rest) = chunk("**Hello** `world`. - item one. Next")
        assertEquals(listOf("Hello world.", "item one."), sentences)
        assertEquals("Next", rest)
    }

    @Test
    fun clean_collapses_whitespace() {
        assertEquals("a b c", SpeechText.clean("  a \n b\t c  "))
    }
}
