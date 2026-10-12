package com.resonant.app.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** What was read from one picture: the words in it, and a JPEG of it small enough to send to the vision model. */
class ImageRead(val text: String, val jpeg: ByteArray)

/**
 * Reads pictures on the phone: text recognition with ML Kit (the model ships inside the app, so it
 * works offline) and page rendering for scanned PDFs. Blocks until done: call off the main thread.
 */
object ImageTextReader {

    /** A scanned PDF is read this many pages deep, so a whole book cannot lock the phone up. */
    const val MAX_SCAN_PAGES = 30

    private const val JPEG_QUALITY = 85

    /** Recognises the text in a photo or screenshot. Null when the bytes are not a picture the phone can decode. */
    fun read(bytes: ByteArray): ImageRead? {
        val bitmap = decode(bytes) ?: return null
        return try {
            val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
            ImageRead(recognise(bitmap), jpeg)
        } finally {
            bitmap.recycle()
        }
    }

    /** Recognises the text on the pages of a scanned PDF, the first [MAX_SCAN_PAGES] of them, one string per page. */
    fun readScannedPdf(context: Context, bytes: ByteArray): List<String> {
        val file = File.createTempFile("scan", ".pdf", context.cacheDir)
        return try {
            file.writeBytes(bytes)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    (0 until minOf(renderer.pageCount, MAX_SCAN_PAGES)).map { index ->
                        renderer.openPage(index).use { page ->
                            val (w, h) = ImageMaterial.fitSize(page.width * 2, page.height * 2)
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            try {
                                Canvas(bitmap).drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                recognise(bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        } finally {
            file.delete()
        }
    }

    private fun recognise(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
            ImageMaterial.cleanText(result.textBlocks.map { it.text })
        } finally {
            recognizer.close()
        }
    }

    /** Decodes at a size that fits in memory, then turns it upright using the photo's own orientation tag. */
    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = ImageMaterial.sampleSize(bounds.outWidth, bounds.outHeight) }
        val decoded: Bitmap? = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: OutOfMemoryError) {
            null
        }
        var bitmap: Bitmap = decoded ?: return null

        val (w, h) = ImageMaterial.fitSize(bitmap.width, bitmap.height)
        if (w != bitmap.width || h != bitmap.height) {
            val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled
        }

        val degrees = try {
            when (ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
        if (degrees == 0f) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
