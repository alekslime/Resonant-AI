package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LessonLibraryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val store get() = MaterialStore(File(tmp.root, "materials"))

    @Test
    fun without_material_the_list_is_the_built_in_lessons() {
        val items = LessonLibrary.items(store)
        assertEquals(LessonData.allLessons.map { it.id }, items.map { it.id })
        assertTrue(items.none { it.own })
    }

    @Test
    fun own_material_comes_after_the_built_in_lessons() {
        val s = store
        val a = s.add("first.txt", "One.", 1000)!!
        val b = s.add("second.txt", "Two.", 2000)!!
        val items = LessonLibrary.items(s)
        val builtIn = LessonData.allLessons.size
        assertEquals(builtIn + 2, items.size)
        assertEquals(listOf(a.id, b.id), items.drop(builtIn).map { it.id })
        assertEquals(listOf("first", "second"), items.drop(builtIn).map { it.title })
        assertTrue(items.drop(builtIn).all { it.own })
        assertFalse(items.take(builtIn).any { it.own })
    }

    @Test
    fun find_returns_built_in_and_own_lessons() {
        val s = store
        val a = s.add("notes.txt", "Cells are small.", 1000)!!
        assertNotNull(LessonLibrary.find(s, LessonData.allLessons.first().id))
        assertEquals("notes", LessonLibrary.find(s, a.id)!!.title)
    }

    @Test
    fun find_is_null_for_unknown_or_missing_ids() {
        assertNull(LessonLibrary.find(store, "nope"))
        assertNull(LessonLibrary.find(store, null))
    }

    @Test
    fun a_deleted_material_leaves_the_list() {
        val s = store
        val a = s.add("notes.txt", "Cells are small.", 1000)!!
        s.delete(a.id)
        assertTrue(LessonLibrary.items(s).none { it.id == a.id })
        assertNull(LessonLibrary.find(s, a.id))
    }
}
