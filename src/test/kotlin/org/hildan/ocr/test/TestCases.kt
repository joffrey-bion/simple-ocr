package org.hildan.ocr.test

import org.hildan.ocr.*
import org.hildan.ocr.reference.ReferenceImage
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.*

object TestCases {

    val wordsTextDetector = TextDetector(ColorSimilarityFilter(Color.WHITE))
    val numbersTextDetector = TextDetector(ColorSimilarityFilter(Color.BLACK, 255, 0), trimSubImagesVertically = false)

    @JvmStatic
    fun testNumbers() = listOf(
        numberTestCase("175.000", listOf("1", "7", "5", "dot", "0", "0", "0")),
        numberTestCase("385.000", listOf("3", "8", "5", "dot", "0", "0", "0")),
        numberTestCase("0", listOf("0"), filename = "zero.png"),
    )

    @JvmStatic
    fun testWordsWithForgivingKerning() = listOf(
        wordTestCase("Planète"),
        wordTestCase("101 dalmatiens"),
    )

    @JvmStatic
    fun testWordsWithTightKerning() = listOf(
        wordTestCase("Taille", listOf("Ta", "i", "l", "l", "e")), // 2 letters due to kerning
        wordTestCase("Vacher", listOf("Va", "c", "h", "e", "r")), // 2 letters due to kerning
    )

    @JvmStatic
    fun allTestWords() = testWordsWithForgivingKerning() + testWordsWithTightKerning()
}

data class TestCase(
    val imageText: String,
    val expectedSubImagesText: List<String>,
    val image: BufferedImage,
    val expectedSubImagePaths: List<Path> = expectedSubImagesText.map { Path("src/test/resources/imgs/words/baseLetters/$it.png") },
    val expectedSubImages: List<BufferedImage> = expectedSubImagePaths.map { it.readImage() },
) {
    override fun toString(): String = imageText
}

private fun wordTestCase(
    word: String,
    expectedLetters: List<String> = word.replace(" ", "").map { "$it" },
) = TestCase(
    imageText = word,
    expectedSubImagesText = expectedLetters,
    image = resourceImage("/imgs/words/$word.png"),
    expectedSubImagePaths = expectedLetters.map { Path("src/test/resources/imgs/words/baseLetters/$it.png") },
    expectedSubImages = expectedLetters.map { resourceImage("/imgs/words/baseLetters/$it.png") },
)

private fun numberTestCase(
    numberText: String,
    expectedDigits: List<String> = numberText.map { "$it" },
    filename: String = "$numberText.png",
) = TestCase(
    imageText = numberText,
    expectedSubImagesText = expectedDigits,
    image = resourceImage("/imgs/numbers/$filename"),
    expectedSubImagePaths = expectedDigits.map { Path("src/test/resources/imgs/numbers/digits/$it.png") },
    expectedSubImages = expectedDigits.map { resourceImage("/imgs/numbers/digits/$it.png") },
)

private fun resourceImage(resourcePath: String) = ReferenceImage::class.java.getResourceImage(resourcePath)
