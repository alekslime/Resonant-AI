package com.resonant.app.content

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.resonant.app.network.OllamaClient

/**
 * Turns something the person shared or picked (a .txt / .md / .pdf file, a photo or screenshot, or plain shared text)
 * into saved material. [decode], [decodePdfText], [decodeImage], [isPdf] and [message] are pure and tested on the
 * JVM; [fromIntent] and [fromUri] do the Android reading. Run them off the main thread.
 */
object MaterialImporter {

    const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_PDF_BYTES = 30 * 1024 * 1024
    const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
    private const val MIN_PDF_LETTERS = 30
    private val TEXT_EXTENSIONS = setOf("txt", "md", "markdown", "text")

    sealed class Result {
        /** [textOnly]: a picture whose description could not be had, so only the words in it were kept. */
        data class Saved(val entry: MaterialStore.Entry, val textOnly: Boolean = false) : Result()
        object Empty : Result()
        object TooLarge : Result()
        object NotText : Result()
        object PdfLocked : Result()
        object PdfNoText : Result()
        object ImageNoText : Result()
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

    /**
     * Saves what was found in a picture: the [description] of it (null when the vision model could
     * not be reached) and the [text] written in it. Pure.
     */
    fun decodeImage(store: MaterialStore, fileName: String, text: String, description: String?): Result {
        val material = ImageMaterial.compose(text, description) ?: return Result.ImageNoText
        val entry = store.add(fileName, material) ?: return Result.Empty
        return Result.Saved(entry, textOnly = !ImageMaterial.hasDescription(description))
    }

    /** Said before a slow read starts, or null when the read is quick. Pictures and PDFs with graphs can take minutes. */
    fun startMessage(mimeType: String?): String? = when {
        mimeType?.startsWith("image/") == true -> "Reading the picture. This can take a minute."
        mimeType == "application/pdf" -> "Reading the PDF. If it has graphs, this can take a few minutes."
        else -> null
    }

    /** What to say out loud. */
    fun message(result: Result): String = when (result) {
        is Result.Saved ->
            if (result.textOnly) "Added ${result.entry.title} to your lessons. I only read the words in it, because I could not get the picture described."
            else "Added ${result.entry.title} to your lessons."
        Result.Empty -> "There was nothing to read in that."
        Result.TooLarge -> "That file is too big. The limit is 2 megabytes for text, 20 for photos and 30 for PDFs."
        Result.NotText -> "I can read text, Markdown and PDF files, and photos."
        Result.PdfLocked -> "This PDF is password protected, so I can't read it."
        Result.PdfNoText -> "I couldn't find any text in this PDF, even after looking at its pages."
        Result.ImageNoText -> "I could not read any words in that picture, and I could not get it described. Try a clearer photo, and check that the server is on."
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
            ImageMaterial.isImage(name, bytes) -> fromImage(store, name, bytes)
            else -> decode(store, name, bytes)
        }
    } catch (e: Exception) {
        Result.Failed
    }

    private fun fromPdf(context: Context, store: MaterialStore, fileName: String, bytes: ByteArray): Result {
        if (bytes.size > MAX_PDF_BYTES) return Result.TooLarge
        val content = try {
            PdfTextExtractor.read(context, bytes)
        } catch (e: PdfLockedException) {
            return Result.PdfLocked
        } catch (e: OutOfMemoryError) {
            return Result.TooLarge
        }
        val scanned = content.pages.sumOf { page -> page.count { it.isLetter() } } < MIN_PDF_LETTERS
        // No real text: a scan. Read the pages as pictures.
        val pageTexts = if (!scanned) content.pages else try {
            ImageTextReader.readScannedPdf(context, bytes)
        } catch (e: Exception) {
            emptyList()
        } catch (e: OutOfMemoryError) {
            emptyList()
        }
        val cleaned = PdfTextCleaner.cleanPages(pageTexts)
        val figures = describeFigures(context, bytes, cleaned, content.bigImages, scanned)
        return decodePdfText(store, fileName, PdfFigures.compose(cleaned, figures))
    }

    /**
     * Shows the pages that probably have a graph or figure to the vision model and returns its
     * explanation of each, keyed by zero-based page. Empty when the server cannot be reached
     * (two failures in a row stop the loop, so a dead server does not cost minutes).
     */
    private fun describeFigures(
        context: Context, bytes: ByteArray, pages: List<String>, bigImages: List<Int>, scanned: Boolean
    ): Map<Int, String> {
        val candidates = PdfFigures.pick(pages.indices.filter { PdfFigures.isCandidate(pages[it], bigImages.getOrElse(it) { 0 }, scanned) })
        if (candidates.isEmpty()) return emptyMap()
        val jpegs = try {
            ImageTextReader.renderPagesJpeg(context, bytes, candidates)
        } catch (e: Exception) {
            return emptyMap()
        } catch (e: OutOfMemoryError) {
            return emptyMap()
        }
        val out = linkedMapOf<Int, String>()
        var failures = 0
        for (index in candidates) {
            val jpeg = jpegs[index] ?: continue
            val reply = OllamaClient.describeImage(jpeg, PdfFigures.pagePrompt(pages[index]))
            if (reply == null) {
                if (++failures >= 2) break
                continue
            }
            failures = 0
            if (PdfFigures.usable(reply)) out[index] = reply
        }
        return out
    }

    /** Reads the words in a photo on the phone, then asks the vision model to describe it. Slow: off the main thread. */
    private fun fromImage(store: MaterialStore, fileName: String, bytes: ByteArray): Result {
        if (bytes.size > MAX_IMAGE_BYTES) return Result.TooLarge
        val read: ImageRead? = try {
            ImageTextReader.read(bytes)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
        if (read == null) return Result.Failed
        val description = OllamaClient.describeImage(read.jpeg, ImageMaterial.prompt(read.text))
        return decodeImage(store, fileName, read.text, description)
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
