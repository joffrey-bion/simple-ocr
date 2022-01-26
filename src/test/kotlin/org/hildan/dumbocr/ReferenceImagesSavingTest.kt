package org.hildan.dumbocr

import org.hildan.dumbocr.generation.UniqueImageStore
import org.hildan.dumbocr.test.TestCase
import org.hildan.dumbocr.test.assertSameContent
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.io.path.createTempDirectory

class ReferenceImagesSavingTest {

    @ParameterizedTest(name = "splitAndSaveSubImages should split {0} correctly")
    @MethodSource("org.hildan.dumbocr.test.TestCases#allTestWords")
    fun splitAndSaveSubImages(testCase: TestCase) {
        val outputDir = createTempDirectory("generated-base-images").apply { toFile().deleteOnExit() }

        val actualImagePaths = TextDetector(Color.WHITE).splitAndSaveSubImages(testCase.image, outputDir)

        assertSameContent(testCase.expectedSubImagePaths, actualImagePaths)
    }

    @ParameterizedTest(name = "splitAndSaveSubImages should split {0} correctly")
    @MethodSource("org.hildan.dumbocr.test.TestCases#allTestWords")
    fun splitAndSaveSubImages_imageStore(testCase: TestCase) {
        val outputDir = createTempDirectory("generated-base-images").apply { toFile().deleteOnExit() }
        val imageStore = UniqueImageStore(outputDir)
        val actualImagePaths = TextDetector(Color.WHITE).splitAndSaveSubImages(testCase.image, imageStore)

        assertSameContent(testCase.expectedSubImagePaths, actualImagePaths)
    }

    @ParameterizedTest(name = "splitAndSaveCharacterImages should split {0} correctly")
    @MethodSource("org.hildan.dumbocr.test.TestCases#testWordsWithForgivingKerning")
    fun splitAndSaveCharacterImages(testCase: TestCase) {
        val outputDir = createTempDirectory("generated-base-images").apply { toFile().deleteOnExit() }

        val actualImagePaths = TextDetector(Color.WHITE).splitAndSaveCharacterImages(testCase.image, testCase.imageText, outputDir)

        assertSameContent(testCase.expectedSubImagePaths, actualImagePaths)
    }
}
