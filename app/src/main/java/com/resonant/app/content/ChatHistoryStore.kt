package com.resonant.app.content

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists the running chat conversation across app restarts and screen
 * navigation. Without this, [ChatExchange]s live only in ChatScreen's
 * in-memory state and vanish the moment the screen is left — for something
 * meant to be a primary way of interacting with this app, losing the whole
 * conversation on every navigation is a real gap, not a cosmetic one.
 *
 * Deliberately plain JSON in a file under filesDir, matching this app's
 * existing no-heavy-dependency style (no Room, no DataStore — see other
 * product decisions in this codebase) and its existing JSON library
 * (org.json, same as OllamaClient) — a history this size doesn't need a
 * database.
 *
 * Capped at [MAX_EXCHANGES] on every save. Unbounded history would grow two
 * things forever: the file, and — more importantly — what ChatScreen sends
 * to the model as context on every single question (see ChatScreen.askModel,
 * which rebuilds the full history into the request each time). Ollama's
 * context window isn't infinite, and a long-lived conversation would
 * eventually make every request slower or start failing outright. Trimming
 * to the most recent exchanges trades old context for staying responsive.
 */
object ChatHistoryStore {

    const val MAX_EXCHANGES = 20

    private fun file(context: Context): File = File(context.filesDir, "chat_history.json")

    /** Never throws — a missing or corrupt file just means starting fresh, not crashing Chat. */
    fun load(context: Context): List<ChatExchange> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return try {
            val array = JSONArray(f.readText())
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val chunksArray = obj.getJSONArray("assistantChunks")
                val chunks = (0 until chunksArray.length()).map { j ->
                    val unit = chunksArray.getJSONObject(j)
                    SemanticUnit(unit.getString("id"), unit.getString("text"))
                }
                ChatExchange(obj.getString("userText"), chunks)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Best-effort — a failed write loses history, which is bad, but shouldn't crash mid-conversation. */
    fun save(context: Context, exchanges: List<ChatExchange>) {
        val trimmed = exchanges.takeLast(MAX_EXCHANGES)
        val array = JSONArray()
        trimmed.forEach { exchange ->
            val obj = JSONObject()
            obj.put("userText", exchange.userText)
            val chunksArray = JSONArray()
            exchange.assistantChunks.forEach { unit ->
                val u = JSONObject()
                u.put("id", unit.id)
                u.put("text", unit.text)
                chunksArray.put(u)
            }
            obj.put("assistantChunks", chunksArray)
            array.put(obj)
        }
        try {
            file(context).writeText(array.toString())
        } catch (e: Exception) {
            // Swallowed on purpose — see the doc comment above.
        }
    }

    /** For a future "clear chat history" control — not wired to any UI yet. */
    fun clear(context: Context) {
        file(context).delete()
    }
}
