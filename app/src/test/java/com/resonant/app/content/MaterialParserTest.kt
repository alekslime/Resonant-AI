package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialParserTest {

    private fun parse(text: String) = MaterialParser.parse("m1", "My notes", text)
    private fun units(text: String) = parse(text)!!.sections.flatMap { it.units }.map { it.text }

    @Test
    fun paragraphs_become_one_unit_per_sentence() {
        assertEquals(listOf("Cells are small.", "They make up life.", "Is that clear?"), units("Cells are small. They make up life.\nIs that clear?"))
    }

    @Test
    fun hard_wrapped_lines_are_joined_into_one_paragraph() {
        assertEquals(listOf("This sentence was wrapped by an editor."), units("This sentence was wrapped\nby an editor."))
    }

    @Test
    fun headings_become_sections_with_their_titles() {
        val lesson = parse("# Biology\nCells exist.\n\n## Plants\nPlants make food.\n\n## Animals\nAnimals eat.")!!
        assertEquals(listOf("Biology", "Plants", "Animals"), lesson.sections.map { it.title })
        assertEquals(listOf("Plants make food."), lesson.sections[1].units.map { it.text })
    }

    @Test
    fun text_before_the_first_heading_takes_the_material_title() {
        val lesson = parse("Intro text here.\n\n# Chapter\nBody.")!!
        assertEquals(listOf("My notes", "Chapter"), lesson.sections.map { it.title })
    }

    @Test
    fun plain_text_without_headings_is_one_section_named_after_the_material() {
        assertEquals(listOf("My notes"), parse("Just some text. Nothing else.")!!.sections.map { it.title })
    }

    @Test
    fun markdown_is_cleaned_for_the_ear() {
        assertEquals(
            listOf("This is bold and italic and code.", "See the docs now.", "Look: a chart."),
            units("This is **bold** and *italic* and `code`.\n\nSee [the docs](https://example.com/x) now.\n\nLook: ![a chart](img.png).")
        )
    }

    @Test
    fun urls_are_not_read_out_letter_by_letter() {
        assertEquals(listOf("Visit a link today."), units("Visit https://example.com/a/b?c=d today."))
    }

    @Test
    fun snake_case_words_keep_their_parts_and_are_not_mangled_by_emphasis() {
        assertEquals(listOf("Use binary search tree here."), units("Use binary_search_tree here."))
    }

    @Test
    fun code_blocks_are_announced_not_read() {
        val spoken = units("Before.\n\n```kotlin\nval x = 1\nprintln(x)\n```\n\nAfter.")
        assertEquals(listOf("Before.", MaterialParser.CODE_NOTE, "After."), spoken)
    }

    @Test
    fun list_items_are_separate_units_without_their_markers() {
        assertEquals(listOf("Apples", "Pears", "First step", "Second step"), units("- Apples\n* Pears\n1. First step\n2) Second step"))
    }

    @Test
    fun table_rows_are_read_as_comma_separated_cells_and_the_separator_is_dropped() {
        assertEquals(listOf("Name, Age", "Ann, 30"), units("| Name | Age |\n|---|---|\n| Ann | 30 |"))
    }

    @Test
    fun front_matter_rules_and_quote_markers_are_dropped() {
        assertEquals(listOf("A wise saying.", "Done."), units("---\ntitle: x\n---\n> A wise saying.\n\n---\n\nDone."))
    }

    @Test
    fun abbreviations_decimals_and_initials_do_not_split_sentences() {
        assertEquals(
            listOf("Dr. Smith paid 3.5 dollars, e.g. for J. K. Rowling.", "Next sentence."),
            units("Dr. Smith paid 3.5 dollars, e.g. for J. K. Rowling. Next sentence.")
        )
    }

    @Test
    fun a_sentence_after_a_year_still_splits() {
        assertEquals(listOf("It began in 2020.", "Then it grew."), units("It began in 2020. Then it grew."))
    }

    @Test
    fun a_very_long_sentence_is_cut_so_no_unit_is_too_long() {
        val long = (1..120).joinToString(", ") { "item number $it" } + "."
        val spoken = units(long)
        assertTrue(spoken.size > 1)
        assertTrue(spoken.all { it.length <= MaterialParser.MAX_UNIT_CHARS })
        assertEquals(121, spoken.joinToString(" ").split("item number").size)
    }

    @Test
    fun a_long_section_is_split_into_parts() {
        val paragraph = (1..60).joinToString(" ") { "word$it" } + "."
        val lesson = parse("# Long\n" + (1..15).joinToString("\n\n") { paragraph })!!
        assertTrue(lesson.sections.size > 1)
        assertEquals("Long, part 1", lesson.sections[0].title)
    }

    @Test
    fun long_plain_text_is_split_into_numbered_parts() {
        val paragraph = (1..60).joinToString(" ") { "word$it" } + "."
        val lesson = parse((1..15).joinToString("\n\n") { paragraph })!!
        assertEquals("Part 1", lesson.sections[0].title)
        assertEquals("Part 2", lesson.sections[1].title)
    }

    @Test
    fun ids_are_unique() {
        val lesson = parse("# A\nOne. Two.\n\n# B\nThree.")!!
        val ids = lesson.sections.map { it.id } + lesson.sections.flatMap { s -> s.units.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals("m1", lesson.id)
    }

    @Test
    fun nothing_readable_gives_null() {
        assertNull(parse(""))
        assertNull(parse("   \n\n---\n\n"))
        assertNotNull(parse("x"))
    }

    @Test
    fun title_comes_from_the_file_name() {
        assertEquals("my notes final", MaterialParser.titleFromFileName("my_notes-final.md"))
        assertEquals("notes", MaterialParser.titleFromFileName("notes"))
        assertEquals("Untitled material", MaterialParser.titleFromFileName(".md"))
    }
}
