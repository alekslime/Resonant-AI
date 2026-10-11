package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MaterialStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun store() = MaterialStore(File(tmp.root, "materials"))

    @Test
    fun empty_store_lists_nothing() {
        assertTrue(store().list().isEmpty())
        assertTrue(store().lessons().isEmpty())
    }

    @Test
    fun added_material_is_listed_with_a_title_from_the_file_name() {
        val s = store()
        val entry = s.add("my_notes-final.md", "# Cells\nCells are small.", 1000)!!
        assertEquals("my notes final", entry.title)
        assertEquals(listOf(entry), s.list())
    }

    @Test
    fun material_survives_a_new_store_on_the_same_folder() {
        store().add("a.txt", "Hello there.", 1000)
        assertEquals(1, store().list().size)
        assertEquals("Hello there.", store().lessons().single().sections.single().units.single().text)
    }

    @Test
    fun lesson_is_parsed_from_the_saved_text() {
        val s = store()
        val entry = s.add("bio.md", "# Biology\nCells exist.\n\n## Plants\nPlants make food.", 1000)!!
        val lesson = s.lesson(entry.id)!!
        assertEquals(entry.id, lesson.id)
        assertEquals(listOf("Biology", "Plants"), lesson.sections.map { it.title })
    }

    @Test
    fun text_with_nothing_readable_is_refused_and_not_saved() {
        val s = store()
        assertNull(s.add("empty.txt", "  \n\n ", 1000))
        assertNull(s.add("rule.md", "***\n---\n___", 1001))
        assertTrue(s.list().isEmpty())
    }

    @Test
    fun material_comes_back_in_the_order_it_was_added() {
        val s = store()
        s.add("first.txt", "One.", 1000)
        s.add("second.txt", "Two.", 2000)
        assertEquals(listOf("first", "second"), s.list().map { it.title })
        assertEquals(listOf("first", "second"), s.lessons().map { it.title })
    }

    @Test
    fun two_items_added_at_the_same_moment_get_different_ids() {
        val s = store()
        val a = s.add("a.txt", "One.", 1000)!!
        val b = s.add("b.txt", "Two.", 1000)!!
        assertNotEquals(a.id, b.id)
        assertEquals(2, s.list().size)
    }

    @Test
    fun delete_removes_the_material_and_its_file() {
        val s = store()
        val a = s.add("a.txt", "One.", 1000)!!
        val b = s.add("b.txt", "Two.", 2000)!!
        assertTrue(s.delete(a.id))
        assertEquals(listOf(b.id), s.list().map { it.id })
        assertNull(s.lesson(a.id))
        assertFalse(File(tmp.root, "materials/${a.id}.txt").exists())
    }

    @Test
    fun deleting_something_unknown_does_nothing() {
        val s = store()
        s.add("a.txt", "One.", 1000)
        assertFalse(s.delete("nope"))
        assertEquals(1, s.list().size)
    }

    @Test
    fun unknown_lesson_id_is_null() {
        assertNull(store().lesson("nope"))
    }

    @Test
    fun a_corrupt_index_gives_an_empty_list() {
        val dir = File(tmp.root, "materials").apply { mkdirs() }
        File(dir, "index.json").writeText("not json {{")
        assertTrue(store().list().isEmpty())
    }

    @Test
    fun an_index_entry_without_its_file_is_skipped() {
        val s = store()
        val a = s.add("a.txt", "One.", 1000)!!
        s.add("b.txt", "Two.", 2000)
        File(tmp.root, "materials/${a.id}.txt").delete()
        assertEquals(listOf("b"), s.list().map { it.title })
        assertNotNull(s.lessons().single())
    }
}
