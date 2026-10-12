package com.resonant.app.content

/**
 * The pure side of reading a picture: what counts as an image, how big to make it, how the text
 * found in it and the description of it become one piece of material, and what the vision model
 * is asked. Pure Kotlin, tested on the JVM. The Android reading is in [ImageTextReader].
 */
object ImageMaterial {

    /** The longest side of the picture handed to text recognition and to the vision model. */
    const val MAX_SIDE = 1568

    /** Fewer letters than this in the recognised text is treated as noise when nothing else was found. */
    const val MIN_TEXT_LETTERS = 8
    const val MIN_DESCRIPTION_LETTERS = 20

    /** At most this much recognised text is shown to the vision model, as a hint for exact words and numbers. */
    private const val HINT_CHARS = 1500

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif")

    /** True for an image name, or for content that starts like a JPEG, PNG or WebP. Pure. */
    fun isImage(fileName: String, bytes: ByteArray): Boolean {
        if (fileName.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS) return true
        fun at(i: Int) = if (i < bytes.size) bytes[i].toInt() and 0xFF else -1
        val jpeg = at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF
        val png = at(0) == 0x89 && at(1) == 'P'.code && at(2) == 'N'.code && at(3) == 'G'.code
        val webp = bytes.size >= 12 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "RIFF" &&
            String(bytes, 8, 4, Charsets.ISO_8859_1) == "WEBP"
        return jpeg || png || webp
    }

    /** The largest power-of-two shrink that keeps the longest side at or above [MAX_SIDE]. */
    fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= MAX_SIDE) {
            sample *= 2
            longest /= 2
        }
        return sample
    }

    /** [width] x [height] scaled down so the longest side is at most [MAX_SIDE]; never scaled up. */
    fun fitSize(width: Int, height: Int): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= MAX_SIDE || width <= 0 || height <= 0) return width to height
        val scale = MAX_SIDE.toDouble() / longest
        return maxOf(1, Math.round(width * scale).toInt()) to maxOf(1, Math.round(height * scale).toInt())
    }

    /**
     * Recognised text blocks, one string each, as paragraphs: the line breaks inside a block are
     * only where the picture wrapped, so they become spaces, and a word split by a line-end hyphen is joined.
     */
    fun cleanText(blocks: List<String>): String =
        blocks.map { block ->
            block.replace("\r", "\n")
                .replace(Regex("""(\p{L})-\n(\p{Ll})"""), "$1$2")
                .replace(Regex("""\s*\n\s*"""), " ")
                .replace(Regex("""[ \t]+"""), " ")
                .trim()
        }.filter { it.isNotEmpty() }.joinToString("\n\n")

    /** True when [text] has enough letters to be worth keeping as the only thing found in a picture. */
    fun hasText(text: String) = text.count { it.isLetter() } >= MIN_TEXT_LETTERS

    fun hasDescription(description: String?) =
        description != null && description.count { it.isLetter() } >= MIN_DESCRIPTION_LETTERS

    /**
     * The material for a picture: the description first (what it is, and for a graph its axes and
     * trend), then the words that are written in it. Null when neither is worth keeping.
     * [description] is null when the vision model could not be reached.
     */
    fun compose(text: String, description: String?): String? {
        val described = hasDescription(description)
        val lettered = if (described) text.count { it.isLetter() } >= 3 else hasText(text)
        if (!described && !lettered) return null
        val parts = mutableListOf<String>()
        if (described) parts += "## What the picture shows\n\n${tidyDescription(description!!)}"
        if (lettered) parts += (if (described) "## Text in the picture\n\n" else "") + text.trim()
        return parts.joinToString("\n\n")
    }

    /** The model's description without the Markdown it was asked not to use, as plain paragraphs. */
    fun tidyDescription(raw: String): String =
        raw.replace("\r", "")
            .lines()
            .map { it.trim().removePrefix("#").trimStart('#').trim().replace(Regex("""^[-*•]\s+"""), "").replace("**", "").replace("__", "") }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()

    /** What the vision model is asked. The recognised [text] is a hint so exact words and numbers are not guessed. */
    fun prompt(text: String): String {
        val hint = text.trim().take(HINT_CHARS)
        return buildString {
            append("Describe this picture for a listener who cannot see it. It will be read aloud, so write plain ")
            append("sentences and short paragraphs: no Markdown, no bullet points, no headings. ")
            append("If it is a chart or graph: say what kind it is and its title; what each axis shows and its units; ")
            append("what each line, bar, slice or series stands for; the overall trend; the highest and lowest values; ")
            append("and how the main groups compare. Give numbers when you can read them, and say 'about' when you are estimating. ")
            append("If it is a table, read it row by row. If it is a diagram, name the parts and say how they connect, in order. ")
            append("If it is a page of text, say what the page is about in one or two sentences; its words are read separately. ")
            append("If it is a photo or drawing, describe what matters for learning. ")
            append("Never invent a number or a label you cannot read.")
            if (hint.isNotEmpty()) {
                append("\n\nText already found in the picture, which may contain mistakes:\n")
                append(hint)
            }
        }
    }
}
