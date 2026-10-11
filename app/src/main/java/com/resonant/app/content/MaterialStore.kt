package com.resonant.app.content

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * The person's own material, kept as plain files in app storage: one text file per item plus
 * an index. The text is stored as given and parsed into a [Lesson] when it is opened, so a
 * better parser later improves old material too. Plain JSON, no database, like the other stores.
 * Callers run it off the main thread.
 */
class MaterialStore(private val dir: File) {

    data class Entry(
        val id: String,
        val title: String,
        val fileName: String,
        val addedAt: Long
    )

    private val indexFile get() = File(dir, "index.json")

    /** Oldest first, so new material lands at the end of the Lessons list. Never throws. */
    fun list(): List<Entry> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val array = JSONArray(indexFile.readText())
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id")
                if (id.isEmpty() || !File(dir, "$id.txt").exists()) return@mapNotNull null
                Entry(id, o.optString("title"), o.optString("fileName"), o.optLong("addedAt"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Saves the material. Null when the text has nothing readable in it or it cannot be written. */
    fun add(fileName: String, text: String, now: Long = System.currentTimeMillis()): Entry? {
        val title = MaterialParser.titleFromFileName(fileName)
        val entries = list()
        var stamp = now
        while (entries.any { it.id == "m$stamp" }) stamp++
        val id = "m$stamp"
        if (MaterialParser.parse(id, title, text) == null) return null
        val entry = Entry(id, title, fileName, now)
        return try {
            dir.mkdirs()
            File(dir, "$id.txt").writeText(text)
            writeIndex(entries + entry)
            entry
        } catch (e: Exception) {
            File(dir, "$id.txt").delete()
            null
        }
    }

    /** The material as a lesson, or null when it is gone or unreadable. */
    fun lesson(id: String): Lesson? {
        val entry = list().firstOrNull { it.id == id } ?: return null
        return try {
            MaterialParser.parse(entry.id, entry.title, File(dir, "${entry.id}.txt").readText())
        } catch (e: Exception) {
            null
        }
    }

    /** All material as lessons, in the order it was added. */
    fun lessons(): List<Lesson> = list().mapNotNull { lesson(it.id) }

    fun delete(id: String): Boolean {
        val entries = list()
        if (entries.none { it.id == id }) return false
        File(dir, "$id.txt").delete()
        return try {
            writeIndex(entries.filter { it.id != id })
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeIndex(entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("fileName", it.fileName)
                    .put("addedAt", it.addedAt)
            )
        }
        indexFile.writeText(array.toString())
    }

    companion object {
        fun forContext(context: Context) = MaterialStore(File(context.filesDir, "materials"))
    }
}
