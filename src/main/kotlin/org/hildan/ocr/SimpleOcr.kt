package org.hildan.ocr

import java.awt.image.BufferedImage

private const val DEFAULT_MIN_RECOGNITION_SCORE = 1.0

/**
 * Thrown when no reference image from the given list has matched a sub-image with sufficient score.
 * This often means that a reference image is missing for the character in question.
 * The [unmatchedSubImage] is available on this exception so it can be saved and manually labeled.
 */
class NoAcceptableMatchException(
    val unmatchedSubImage: BufferedImage,
    val originalImage: BufferedImage,
) : Exception()

/**
 * A `SimpleOcr` can read characters out of an image containing a single line of text.
 *
 * It does so by splitting the image into sub-images representing individual text elements, and matching the sub-images
 * against the given [referenceImages].
 *
 * Sub-images are often individual characters, but sometimes several characters can be grouped together due to kerning.
 * For instance, a lowercase letter following an uppercase T ou V can be part of a single sub-image (Te, To, Va...).
 *
 * The given [textDetector] defines pixels that are considered part of the text.
 */
class SimpleOcr(
    private val referenceImages: List<ReferenceImage>,
    private val textDetector: TextDetector,
    /**
     * The minimum score that the best matching reference image should have for a successful recognition.
     * If even the best match scores lower, a [NoAcceptableMatchException] is thrown.
     */
    private val minRecognitionScore: Double = DEFAULT_MIN_RECOGNITION_SCORE,
    /**
     * The minimum width of empty space (between 2 text elements) that is considered an actual whitespace character.
     */
    private val spaceWidthThreshold: Int = inferSpaceWidth(referenceImages),
) {
    init {
        require(referenceImages.isNotEmpty()) { "no reference image provided" }
    }

    /**
     * Creates a new [SimpleOcr] based on a default [ColorSimilarityFilter].
     *
     * The given [textColor] is the expected color of the text to read.
     * Pixels with colors that are close enough to the [textColor] are considered part of the text.
     */
    constructor(
        referenceImages: List<ReferenceImage>,
        textColor: Color,
        minRecognitionScore: Double = DEFAULT_MIN_RECOGNITION_SCORE,
        spaceWidthThreshold: Int = inferSpaceWidth(referenceImages),
    ) : this(
        referenceImages = referenceImages,
        textDetector = TextDetector(ColorSimilarityFilter(textColor)),
        minRecognitionScore = minRecognitionScore,
        spaceWidthThreshold = spaceWidthThreshold,
    )

    /**
     * Infers text from the given [image] based on the [referenceImages] of known characters.
     */
    fun recognizeText(image: BufferedImage): String =
        textDetector.splitTextAndSpaces(image).joinToString("") { recognizeTextPart(it, image) }.trim()

    private fun recognizeTextPart(part: ImagePart, originalImage: BufferedImage): String = when(part) {
        is ImagePart.TextSubImage -> findClosestReferenceText(part.subImage, originalImage)
        is ImagePart.Space -> if (part.width >= spaceWidthThreshold) " " else ""
    }

    private fun findClosestReferenceText(subImage: BufferedImage, originalImage: BufferedImage): String {
        val bestMatch = similarityScoresWithRefImages(subImage).maxByOrNull { it.similarityScore }!!
        if (bestMatch.similarityScore < minRecognitionScore) {
            throw NoAcceptableMatchException(subImage, originalImage)
        }
        return bestMatch.refImage.text
    }

    private fun similarityScoresWithRefImages(subImage: BufferedImage): List<ScoredImage> =
        referenceImages.map { ref -> ScoredImage(ref, textDetector.similarityScore(subImage, ref.image)) }
}

private data class ScoredImage(
    val refImage: ReferenceImage,
    val similarityScore: Double,
)

private fun inferSpaceWidth(referenceImages: List<ReferenceImage>) = (averageWidth(referenceImages) / 2.5).toInt()

private fun averageWidth(images: List<ReferenceImage>) = images.sumOf { it.image.width }.toDouble() / images.size
