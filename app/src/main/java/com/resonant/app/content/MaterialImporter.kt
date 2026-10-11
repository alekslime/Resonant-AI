package com.resonant.app.content

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Turns something the person shared or picked (a .txt / .md file, or plain shared text) into
 * saved material. [decode] and [message] are pure and tested on the JVM; [fromIntent] and
 * [fromUri] do the Android reading. Run them off the main thread.
 */
object MaterialImporter {

    const val MAX_BYTES = 2 * 1024 * 1024
    private val TEXT_EXTENSIONS = setOf("txt", "md", "markdown", "text")

    sealed class Result {
        data class Saved(val entry: MaterialStore.Entry) : Result()
        object Empty : Result()
        object TooLarge : Result()
        object NotText : Result()
        object Failed : Result()
    }

    /** The saved-or-refused outcome for [fileName] and its raw [bytes]. Pure. */
    fun decode(store: MaterialStore, fileName: String, bytes: ByteArray): Result {
        if (bytes.size > MAX_BYTES) return Result.TooLarge
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isNotEmpty() && extension !in TEXT_EXTENSIONS) return Result.NotText
        if (bytes.any { it == 0.toByte() }) return Result.NotText
        val text = String(bytes, Charsets.UTF_8)
        val entry = store.add(fileName, text) ?: return Result.Empty
        return Result.Saved(entry)
    }

    /** What to say out loud. */
    fun message(result: Result): String = when (result) {
        is Result.Saved -> "Added ${result.entry.title} to your lessons."
        Result.Empty -> "There was nothing to read in that."
        Result.TooLarge -> "That file is too big. The limit is 2 megabytes."
        Result.NotText -> "I can only read text and Markdown files for now."
        Result.Failed -> "I could not open that."
    }

    fun fromIntent(context: Context, intent: Intent): Result? {
        if (intent.action != Intent.ACTION_SEND) return null
        val store = MaterialStore.forContext(context)
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (uri != null) return fromUri(context, store, uri)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        val name = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf { it.isNotBlank() } ?: "Shared text"
        return decode(store, "$name.txt", text.toByteArray(Charsets.UTF_8))
    }

    fun fromUri(context: Context, store: MaterialStore, uri: Uri): Result = try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_BYTES) break
            }
            out.toByteArray()
        }
        if (bytes == null) Result.Failed else decode(store, displayName(context, uri), bytes)
    } catch (e: Exception) {
        Result.Failed
    }

    private fun displayName(context: Context, uri: Uri): String {
        val fromProvider = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
        return fromProvider ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Shared file"
    }
}
