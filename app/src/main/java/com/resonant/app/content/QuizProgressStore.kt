package com.resonant.app.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Persists per-quiz progress across restarts. Deliberately mirrors
 * [ChatHistoryStore]'s design: plain JSON in filesDir, no Room, no DataStore.
 *
 * Progress for each quiz is stored as:
 *   { "status": "not_started"|"in_progress"|"completed", "bestScore": 0..N }
 *
 * "bestScore" is the highest correct-answer count across all attempts.
 * "in_progress" is set when a quiz is started but not yet finished — currently
 * that transition happens on first open, not mid-quiz (resuming mid-question
 * is not in scope yet).
 */
object QuizProgressStore {

    enum class Status { NOT_STARTED, IN_PROGRESS, COMPLETED }

    data class QuizProgress(
        val status: Status = Status.NOT_STARTED,
        val bestScore: Int = 0,
        val totalQuestions: Int = 0
    ) {
        val percentComplete: Int
            get() = if (totalQuestions == 0) 0 else (bestScore * 100 / totalQuestions)

        val statusLabel: String get() = when (status) {
            Status.NOT_STARTED -> "Not started"
            Status.IN_PROGRESS -> "Ready to continue"
            Status.COMPLETED   -> "Completed"
        }

        val buttonLabel: String get() = when (status) {
            Status.NOT_STARTED -> "Start quiz"
            Status.IN_PROGRESS -> "Continue quiz"
            Status.COMPLETED   -> "Review answers"
        }
    }

    private fun file(context: Context): File =
        File(context.filesDir, "quiz_progress.json")

    /** Never throws — missing or corrupt file returns empty map. */
    suspend fun loadAll(context: Context): Map<String, QuizProgress> =
        withContext(Dispatchers.IO) {
            val f = file(context)
            if (!f.exists()) return@withContext emptyMap()
            try {
                val obj = JSONObject(f.readText())
                val result = mutableMapOf<String, QuizProgress>()
                obj.keys().forEach { id ->
                    val entry = obj.getJSONObject(id)
                    val status = when (entry.optString("status")) {
                        "in_progress" -> Status.IN_PROGRESS
                        "completed"   -> Status.COMPLETED
                        else          -> Status.NOT_STARTED
                    }
                    result[id] = QuizProgress(
                        status = status,
                        bestScore = entry.optInt("bestScore", 0),
                        totalQuestions = entry.optInt("totalQuestions", 0)
                    )
                }
                result
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun markInProgress(context: Context, quizId: String, totalQuestions: Int) =
        withContext(Dispatchers.IO) {
            update(context, quizId) { existing ->
                // Don't downgrade completed -> in_progress on a re-open.
                if (existing.status == Status.COMPLETED) existing
                else existing.copy(status = Status.IN_PROGRESS, totalQuestions = totalQuestions)
            }
        }

    suspend fun markCompleted(context: Context, quizId: String, correct: Int, total: Int) =
        withContext(Dispatchers.IO) {
            update(context, quizId) { existing ->
                existing.copy(
                    status = Status.COMPLETED,
                    bestScore = maxOf(existing.bestScore, correct),
                    totalQuestions = total
                )
            }
        }

    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        file(context).delete()
    }

    private fun update(
        context: Context,
        quizId: String,
        transform: (QuizProgress) -> QuizProgress
    ) {
        val f = file(context)
        val obj = if (f.exists()) {
            try { JSONObject(f.readText()) } catch (e: Exception) { JSONObject() }
        } else JSONObject()

        val existing = if (obj.has(quizId)) {
            val e = obj.getJSONObject(quizId)
            val status = when (e.optString("status")) {
                "in_progress" -> Status.IN_PROGRESS
                "completed"   -> Status.COMPLETED
                else          -> Status.NOT_STARTED
            }
            QuizProgress(status, e.optInt("bestScore", 0), e.optInt("totalQuestions", 0))
        } else QuizProgress()

        val updated = transform(existing)
        val entry = JSONObject()
        entry.put("status", when (updated.status) {
            Status.NOT_STARTED -> "not_started"
            Status.IN_PROGRESS -> "in_progress"
            Status.COMPLETED   -> "completed"
        })
        entry.put("bestScore", updated.bestScore)
        entry.put("totalQuestions", updated.totalQuestions)
        obj.put(quizId, entry)

        try { f.writeText(obj.toString()) } catch (e: Exception) { /* best-effort */ }
    }
}
