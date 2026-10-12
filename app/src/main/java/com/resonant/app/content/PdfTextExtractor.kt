package com.resonant.app.content

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper

class PdfLockedException : Exception()

/** Reads the text out of a PDF that has real text in it (not a scan). Slow for long files: call off the main thread. */
object PdfTextExtractor {

    fun extract(context: Context, bytes: ByteArray): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val pages = try {
            PDDocument.load(bytes).use { doc ->
                val stripper = PDFTextStripper().apply { sortByPosition = true }
                (1..doc.numberOfPages).map { page ->
                    stripper.startPage = page
                    stripper.endPage = page
                    stripper.getText(doc)
                }
            }
        } catch (e: InvalidPasswordException) {
            throw PdfLockedException()
        }
        return PdfTextCleaner.clean(pages)
    }
}
