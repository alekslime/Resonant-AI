package com.resonant.app.content

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper

class PdfLockedException : Exception()

/** What was read from a PDF: the raw text of each page, and how many large pictures each page embeds. */
class PdfContent(val pages: List<String>, val bigImages: List<Int>)

/** Reads the text out of a PDF that has real text in it (not a scan). Slow for long files: call off the main thread. */
object PdfTextExtractor {

    /** A picture smaller than this is an icon or a logo, not a figure. */
    private const val MIN_IMAGE_WIDTH = 200
    private const val MIN_IMAGE_HEIGHT = 120

    fun extract(context: Context, bytes: ByteArray): String = PdfTextCleaner.clean(read(context, bytes).pages)

    /** The raw text and the large-picture count of every page. Throws [PdfLockedException] for a password-protected file. */
    fun read(context: Context, bytes: ByteArray): PdfContent {
        PDFBoxResourceLoader.init(context.applicationContext)
        return try {
            PDDocument.load(bytes).use { doc ->
                val stripper = PDFTextStripper().apply { sortByPosition = true }
                val pages = (1..doc.numberOfPages).map { page ->
                    stripper.startPage = page
                    stripper.endPage = page
                    stripper.getText(doc)
                }
                val images = (0 until doc.numberOfPages).map { countBigImages(doc.getPage(it)) }
                PdfContent(pages, images)
            }
        } catch (e: InvalidPasswordException) {
            throw PdfLockedException()
        }
    }

    private fun countBigImages(page: PDPage): Int {
        return try {
            val resources = page.resources ?: return 0
            resources.xObjectNames.count { name ->
                val x = resources.getXObject(name)
                x is PDImageXObject && x.width >= MIN_IMAGE_WIDTH && x.height >= MIN_IMAGE_HEIGHT
            }
        } catch (e: Exception) {
            0
        }
    }
}
