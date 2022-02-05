package org.hildan.ocr

import java.awt.image.BufferedImage

sealed class ImagePart {
    data class TextSubImage(val subImage: BufferedImage): ImagePart()
    data class Space(val width: Int): ImagePart()
}

fun List<ImagePart>.filterTextSubImages() = mapNotNull { (it as? ImagePart.TextSubImage)?.subImage }

/**
 * Detects text based on the given [textColorFilter]. The filter should match the color of the text.
 */
class TextDetector(
    /**
     * A [ColorFilter] that decides which pixels are part of the text.
     */
    private val textColorFilter: ColorFilter,
    /**
     * Whether to trim the space above and below the text in sub-images.
     *
     * This is done so that the text can be in a slightly different vertical position in the images, and still give
     * consistent sub-images. If the text is guaranteed to be at the same vertical position in the images, you may
     * disable this feature for more accuracy on the match.
     */
    private val trimSubImagesVertically: Boolean = true,
) {
    /**
     * Creates a new [TextDetector] using a [ColorSimilarityFilter] with the given [textColor] as reference.
     *
     * The given [textColor] is the expected color of the text to read.
     * Pixels with colors that are close enough to the [textColor] are considered part of the text.
     */
    constructor(textColor: Color) : this(ColorSimilarityFilter(textColor))

    /**
     * Returns the proportion of the images that match, as a score between 0.0 and 1.0.
     * A pixel is considered a match if it either represents text on both images or empty space on both images.
     */
    fun similarityScore(img1: BufferedImage, img2: BufferedImage): Double {
        if (img1.height != img2.height || img1.width != img2.width) {
            // FIXME this is bad, a single pixel difference could make the trimmed image of different size
            return 0.0
        }
        var score = 0
        for (i in 0 until img1.width) {
            for (j in 0 until img1.height) {
                if (img1.hasTextAtPixel(i, j) == img2.hasTextAtPixel(i, j)) {
                    score++
                }
            }
        }
        return score.toDouble() / (img1.width * img1.height)
    }

    /**
     * Splits the given [image] into text elements.
     */
    fun splitTextElements(image: BufferedImage): List<BufferedImage> = image.splitIntoParts().filterTextSubImages()

    /**
     * Splits the given [image] into parts of text and spaces.
     */
    fun splitTextAndSpaces(image: BufferedImage): List<ImagePart> = image.splitIntoParts()

    private fun BufferedImage.splitIntoParts(): List<ImagePart> {
        val parts = mutableListOf<ImagePart>()
        var currentPartHasText = hasTextInColumn(0)
        var currentPartStart = 0
        for (col in 1 until width) {
            val colHasText = hasTextInColumn(col)
            if (currentPartHasText != colHasText) {
                parts.add(extractPart(colRange = currentPartStart until col, hasText = currentPartHasText))
                currentPartHasText = colHasText
                currentPartStart = col
            }
        }
        parts.add(extractPart(colRange = currentPartStart until width, hasText = currentPartHasText))
        return parts
    }

    private fun BufferedImage.extractPart(colRange: IntRange, hasText: Boolean) = if (hasText) {
        ImagePart.TextSubImage(verticalSlice(colRange).maybeTrimTopAndBottom())
    } else {
        ImagePart.Space(width = colRange.length)
    }

    private fun BufferedImage.maybeTrimTopAndBottom() = if (trimSubImagesVertically) trimTopAndBottom() else this

    private fun BufferedImage.trimTopAndBottom(): BufferedImage {
        val firstTextRow = (0 until height).first { row -> hasTextInRow(row) }
        val lastTextRow = (height - 1 downTo firstTextRow).first { row -> hasTextInRow(row) }
        return horizontalSlice(firstTextRow..lastTextRow)
    }

    private fun BufferedImage.hasTextInColumn(col: Int) = (0 until height).any { row -> hasTextAtPixel(col, row) }

    private fun BufferedImage.hasTextInRow(row: Int) = (0 until width).any { col -> hasTextAtPixel(col, row) }

    private fun BufferedImage.hasTextAtPixel(col: Int, row: Int) = textColorFilter.matches(colorAt(col, row))
}

private fun BufferedImage.verticalSlice(colRange: IntRange): BufferedImage =
    getSubimage(colRange.first, 0, colRange.length, height)

private fun BufferedImage.horizontalSlice(rowRange: IntRange): BufferedImage =
    getSubimage(0, rowRange.first, width, rowRange.length)

private fun BufferedImage.colorAt(col: Int, row: Int) = Color(getRGB(col, row).toUInt())

private val IntRange.length: Int get() = last - first + 1
