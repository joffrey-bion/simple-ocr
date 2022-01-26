package org.hildan.dumbocr

import org.hildan.dumbocr.test.TestCase
import org.hildan.dumbocr.test.TestCases
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.test.assertEquals

class DumbOcrTest {

    @ParameterizedTest(name = "recognizeText should recognize {0} correctly")
    @MethodSource("org.hildan.dumbocr.test.TestCases#allTestWords")
    fun recognizeText_words(testCase: TestCase) {
        val refImages = ReferenceImages.readFrom(Path("src/test/resources/imgs/words/baseLetters"))
        val actualText = try {
            DumbOcr(refImages, TestCases.wordsTextDetector, spaceWidthThreshold = 8).recognizeText(testCase.image)
        } catch (e: NoAcceptableMatchException) {
            val file = File("src/test/resources/imgs/words/failedLetters/${UUID.randomUUID()}.png")
            ImageIO.write(e.unmatchedSubImage, "png", file)
        }
        assertEquals(testCase.imageText, actualText)
    }

    @ParameterizedTest(name = "recognizeText should recognize {0} correctly")
    @MethodSource("org.hildan.dumbocr.test.TestCases#testNumbers")
    fun recognizeText_numbers(testCase: TestCase) {
        val refImages = ReferenceImages.readFrom(Path("src/test/resources/imgs/numbers/digits"))
        val actualText = DumbOcr(refImages, TestCases.numbersTextDetector, spaceWidthThreshold = 8).recognizeText(testCase.image)
        assertEquals(testCase.imageText, actualText)
    }
}
