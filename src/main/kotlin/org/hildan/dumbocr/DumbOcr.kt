package org.hildan.dumbocr

import java.awt.image.BufferedImage

private const val DEFAULT_MIN_RECOGNITION_SCORE = 0.9

data class ScoredImage(
    val refImage: ReferenceImage,
    val similarityScore: Double,
)

class NoAcceptableMatchException(val unmatchedImage: BufferedImage) : Exception()

/**
 * A `DumbOcr` can read characters out of an image containing a single line of text.
 *
 * It does so by splitting the image into sub-images representing individual characters, and matching the sub-images
 * against known character images.
 *
 * The given [textColorFilter] defines pixels that are considered part of the text.
 */
class DumbOcr(
    private val textColorFilter: ColorFilter,
    private val minRecognitionScore: Double = DEFAULT_MIN_RECOGNITION_SCORE,
) {
    /**
     * Creates a new [DumbOcr] based on a [ColorSimilarityFilter].
     *
     * The given [textColor] is the expected color of the text to read.
     * Pixels with colors that are close enough to the [textColor] are considered part of the text.
     * Colors are considered "close enough" to the reference color when each of the ARGB component is different from the
     * corresponding component in the [textColor] by no more than the given [tolerance].
     */
    constructor(
        textColor: Color,
        tolerance: Int = 25,
        minRecognitionScore: Double = DEFAULT_MIN_RECOGNITION_SCORE,
    ) : this(ColorSimilarityFilter(textColor, tolerance), minRecognitionScore)

    /**
     * Infers text from the given [image] based on the given [referenceImages] of known characters.
     */
    fun recognizeText(image: BufferedImage, referenceImages: List<ReferenceImage>): String =
        splitIntoSubImages(image).joinToString("") { findClosestCharacter(it, referenceImages) }

    private fun findClosestCharacter(subImage: BufferedImage, referenceImages: List<ReferenceImage>): String {
        require(referenceImages.isNotEmpty()) { "no reference image provided" }
        val bestMatch = scoreSimilarity(subImage, referenceImages).maxByOrNull { it.similarityScore }!!
        if (bestMatch.similarityScore < minRecognitionScore) {
            throw NoAcceptableMatchException(subImage)
        }
        return bestMatch.refImage.text
    }

    private fun scoreSimilarity(image: BufferedImage, referenceImages: List<ReferenceImage>): List<ScoredImage> =
        referenceImages.map { ScoredImage(it, similarityScore(image, it.image)) }

    private fun similarityScore(img1: BufferedImage, img2: BufferedImage): Double {
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
     * Splits the given [image] into sub-images, each containing only one character of the text.
     */
    fun splitIntoSubImages(image: BufferedImage): List<BufferedImage> =
        image.getColumnRangesWithText().map { colRange -> image.verticalSlice(colRange).trimTopAndBottom() }

    private fun BufferedImage.getColumnRangesWithText(): List<IntRange> {
        val rangeBounds = buildList {
            add(-1)
            addAll(getEmptyColumnsIndices())
            add(width)
        }
        return rangeBounds.zipWithNext { start, end -> (start + 1) until end }.filter { !it.isEmpty() }
    }

    private fun BufferedImage.trimTopAndBottom(): BufferedImage {
        val firstTextRow = (0 until height).first { row -> hasTextInRow(row) }
        val lastTextRow = (height - 1 downTo firstTextRow).first { row -> hasTextInRow(row) }
        return horizontalSlice(firstTextRow..lastTextRow)
    }

    private fun BufferedImage.getEmptyColumnsIndices() = (0 until width).filter { col -> !hasTextInColumn(col) }

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
