package org.hildan.ocr

import org.hildan.ocr.test.TestCase
import org.hildan.ocr.test.assertSameImages
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TextDetectorTest {

    @ParameterizedTest(name = "splitTextElements should split {0} correctly")
    @MethodSource("org.hildan.ocr.test.TestCases#allTestWords")
    fun splitTextElements(testCase: TestCase) {
        val actualImages = TextDetector(Color.WHITE).splitTextElements(testCase.image)
        assertSameImages(testCase.expectedSubImages, actualImages)
    }
}
