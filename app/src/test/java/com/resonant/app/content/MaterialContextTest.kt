package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialContextTest {

    private fun lesson(vararg sections: Pair<String, List<String>>) = Lesson(
        "m1", "My notes",
        sections.mapIndexed { i, (title, units) ->
            LessonSection("s$i", title, units.mapIndexed { u, t -> SemanticUnit("s${i}u$u", t) })
        }
    )

    @Test
    fun text_has_section_titles_then_their_sentences() {
        val l = lesson("Cells" to listOf("Cells are small.", "They make up life."), "Plants" to listOf("Plants make food."))
        assertEquals("Cells. Cells are small. They make up life. Plants. Plants make food.", MaterialContext.text(l))
    }

    @Test
    fun a_title_that_already_ends_in_punctuation_is_not_doubled() {
        val l = lesson("Why cells?" to listOf("They matter."))
        assertEquals("Why cells. They matter.", MaterialContext.text(l))
    }

    @Test
    fun long_material_is_cut_at_a_whole_unit() {
        val l = lesson("A" to listOf("First sentence here.", "Second sentence here.", "Third sentence here."))
        val cut = MaterialContext.text(l, maxChars = 40)
        assertEquals("A. First sentence here.", cut)
        assertTrue(cut.length <= 40)
    }

    @Test
    fun the_default_cap_is_respected() {
        val big = lesson("Big" to List(2000) { "This is sentence number $it." })
        assertTrue(MaterialContext.text(big).length <= MaterialContext.MAX_CHARS)
    }

    @Test
    fun no_material_leaves_the_prompt_alone() {
        assertEquals("Base.", MaterialContext.systemPrompt("Base.", null))
    }

    @Test
    fun material_is_added_after_the_base_prompt() {
        val p = MaterialContext.systemPrompt("Base.", lesson("Cells" to listOf("Cells are small.")))
        assertTrue(p.startsWith("Base. "))
        assertTrue(p.contains("\"My notes\""))
        assertTrue(p.endsWith("Cells. Cells are small."))
        assertFalse(p.contains("null"))
    }
}
