package org.hildan.ocr

import org.hildan.ocr.test.TestCase
import org.hildan.ocr.test.TestCases
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.test.assertEquals

class SimpleOcrTest {

    @ParameterizedTest(name = "recognizeText should recognize {0} correctly")
    @MethodSource("org.hildan.ocr.test.TestCases#allTestWords")
    fun recognizeText_words(testCase: TestCase) {
        val refImages = ReferenceImages.readFrom(Path("src/test/resources/imgs/words/baseLetters"))
        val actualText = try {
            SimpleOcr(refImages, TestCases.wordsTextDetector, spaceWidthThreshold = 8).recognizeText(testCase.image)
        } catch (e: NoAcceptableMatchException) {
            val file = File("src/test/resources/imgs/words/failedLetters/${UUID.randomUUID()}.png")
            ImageIO.write(e.unmatchedSubImage, "png", file)
        }
        assertEquals(testCase.imageText, actualText)
    }

    @ParameterizedTest(name = "recognizeText should recognize {0} correctly")
    @MethodSource("org.hildan.ocr.test.TestCases#testNumbers")
    fun recognizeText_numbers(testCase: TestCase) {
        val refImages = ReferenceImages.readFrom(Path("src/test/resources/imgs/numbers/digits"))
        val actualText = SimpleOcr(refImages, TestCases.numbersTextDetector, spaceWidthThreshold = 8).recognizeText(testCase.image)
        assertEquals(testCase.imageText, actualText)
    }
}
