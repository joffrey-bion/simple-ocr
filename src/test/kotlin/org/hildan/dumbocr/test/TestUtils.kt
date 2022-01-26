package org.hildan.dumbocr.test

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.fail

fun assertSameContent(
    expectedImages: List<Path>,
    actualImages: List<Path>,
) {
    assertEquals(expectedImages.size, actualImages.size, "different number of paths")
    repeat(expectedImages.size) { index ->
        if (Files.mismatch(expectedImages[index], actualImages[index]) > 0) {
            fail("Images at index $index are different")
        }
    }
}

fun assertSameImages(
    expectedImages: List<BufferedImage>,
    actualImages: List<BufferedImage>,
) {
    assertEquals(expectedImages.size, actualImages.size, "different number of images")
    repeat(expectedImages.size) { index ->
        assertSameImages(expectedImages[index], actualImages[index], "Images at index $index are different")
    }
}

fun assertSameImages(expected: BufferedImage, actual: BufferedImage, message: String = "Images are different") {
    assertEquals(expected.width, actual.width, "$message\nBoth images should have the same width")
    assertEquals(expected.height, actual.height, "$message\nBoth images should have the same height")

    repeat(expected.height) { y ->
        repeat(expected.width) { x ->
            val expectedRgb = expected.getRGB(x, y)
            val actualRgb = actual.getRGB(x, y)
            assertEquals(
                expectedRgb,
                actualRgb,
                "$message\nPixel ($x, $y) is different. Expected ${expectedRgb.toHexString()}, got ${actualRgb.toHexString()}",
            )
        }
    }
}

private fun Int.toHexString() = "0x${toString(16).padStart(8, '0')}"
