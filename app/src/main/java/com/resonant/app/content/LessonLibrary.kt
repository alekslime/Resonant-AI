package com.resonant.app.content

/**
 * The built-in lessons followed by the person's own material, in that order.
 * The list screen only needs ids and titles, so [items] reads the small index and never parses.
 */
object LessonLibrary {

    data class Item(val id: String, val title: String, val own: Boolean)

    fun items(store: MaterialStore): List<Item> =
        LessonData.allLessons.map { Item(it.id, it.title, own = false) } +
            store.list().map { Item(it.id, it.title, own = true) }

    /** Null when no lesson has this id. Parses the material when it is the person's own. */
    fun find(store: MaterialStore, id: String?): Lesson? {
        if (id == null) return null
        return LessonData.allLessons.firstOrNull { it.id == id } ?: store.lesson(id)
    }
}
