package com.resonant.app.content

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Turns something the person shared or picked (a .txt / .md / .pdf file, or plain shared text)
 * into saved material. [decode], [decodePdfText], [isPdf] and [message] are pure and tested on the
 * JVM; [fromIntent] and [fromUri] do the Android reading. Run them off the main thread.
 */
object MaterialImporter {

    const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_PDF_BYTES = 30 * 1024 * 1024
    private const val MIN_PDF_LETTERS = 30
    private val TEXT_EXTENSIONS = setOf("txt", "md", "markdown", "text")

    sealed class Result {
        data class Saved(val entry: MaterialStore.Entry) : Result()
        object Empty : Result()
        object TooLarge : Result()
        object NotText : Result()
        object PdfLocked : Result()
        object PdfNoText : Result()
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

    /** True for a .pdf name, or for content that starts like a PDF. Pure. */
    fun isPdf(fileName: String, bytes: ByteArray): Boolean =
        fileName.substringAfterLast('.', "").equals("pdf", ignoreCase = true) ||
            (bytes.size >= 4 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "%PDF")

    /** Saves the text read out of a PDF, or says it had none (a scan). Pure. */
    fun decodePdfText(store: MaterialStore, fileName: String, text: String): Result {
        if (text.count { it.isLetter() } < MIN_PDF_LETTERS) return Result.PdfNoText
        val entry = store.add(fileName, text) ?: return Result.Empty
        return Result.Saved(entry)
    }

    /** What to say out loud. */
    fun message(result: Result): String = when (result) {
        is Result.Saved -> "Added ${result.entry.title} to your lessons."
        Result.Empty -> "There was nothing to read in that."
        Result.TooLarge -> "That file is too big. The limit is 2 megabytes for text and 30 for PDFs."
        Result.NotText -> "I can read text, Markdown and PDF files."
        Result.PdfLocked -> "This PDF is password protected, so I can't read it."
        Result.PdfNoText -> "I couldn't find any text in this PDF. It may be a scan or a photo of pages. Reading those is coming soon."
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
                if (out.size() > MAX_PDF_BYTES) break
            }
            out.toByteArray()
        }
        val name = displayName(context, uri)
        when {
            bytes == null -> Result.Failed
            isPdf(name, bytes) -> fromPdf(context, store, name, bytes)
            else -> decode(store, name, bytes)
        }
    } catch (e: Exception) {
        Result.Failed
    }

    private fun fromPdf(context: Context, store: MaterialStore, fileName: String, bytes: ByteArray): Result {
        if (bytes.size > MAX_PDF_BYTES) return Result.TooLarge
        val text = try {
            PdfTextExtractor.extract(context, bytes)
        } catch (e: PdfLockedException) {
            return Result.PdfLocked
        } catch (e: OutOfMemoryError) {
            return Result.TooLarge
        }
        return decodePdfText(store, fileName, text)
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
