package org.hildan.dumbocr

import java.awt.image.BufferedImage
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.toList

private val whiteSpaceRegex = Regex("\\s+")

/**
 * A reference image to recognize text.
 */
data class ReferenceImage(
    /** The image data. */
    val image: BufferedImage,
    /** The text value corresponding to the image. */
    val text: String,
)

/**
 * Splits the given image into sub-images for each character symbol, and saves them as files.
 * The file for each character is saved at the path computed by [subImagePath] for the given character index.
 *
 * This function is useful for the initial setup of the OCR.
 * Use it on enough images to cover all possible characters, and create [ReferenceImage]s in your program
 * with the correct character value corresponding to these elementary images.
 */
fun DumbOcr.splitAndSaveSubImages(
    textImage: BufferedImage,
    subImagePath: (subImageIndex: Int) -> Path = { Path("${UUID.randomUUID()}.png") },
): List<Path> = splitIntoSubImages(textImage).mapIndexed { index, subImg ->
    subImagePath(index).also { path ->
        ImageIO.write(subImg, "png", path.toFile())
    }
}

/**
 * Splits the given image into sub-images for each character symbol, and saves them as files into the given [outputDir].
 * Each file is named based on the result of [subImageFilenameWithoutExt] for each character index.
 *
 * This is for the initial setup of the OCR.
 * Use this function on enough images to cover all possible characters, and create [ReferenceImage]s in your program
 * with the correct character value corresponding to these elementary images.
 */
fun DumbOcr.splitAndSaveSubImages(
    textImage: BufferedImage,
    outputDir: Path,
    subImageFilenameWithoutExt: (subImageIndex: Int) -> String = { UUID.randomUUID().toString() },
): List<Path> {
    outputDir.createDirectories()
    return splitAndSaveSubImages(textImage) { index ->
        outputDir.resolve(subImageFilenameWithoutExt(index) + ".png")
    }
}

/**
 * Splits the given image into sub-images for each character symbol, and saves them as files into the given [outputDir].
 * Each file is named based on the characters (more specifically, the unicode code points) in [imageText].
 * Characters that are not valid as file names are escaped.
 *
 * This is for the initial setup of the OCR.
 * Use this function on enough images to cover all possible characters, and create [ReferenceImage]s in your program
 * with the correct character value corresponding to these elementary images.
 */
fun DumbOcr.splitAndSaveCharacterImages(
    image: BufferedImage,
    imageText: String,
    outputDir: Path,
): Map<String, Path> {
    val textWithoutWhitespace = imageText.replace(whiteSpaceRegex, "")
    val codePoints = textWithoutWhitespace.codePoints().toList().map { String(Character.toChars(it)) }
    val paths = splitAndSaveSubImages(image, outputDir) { codePoints[it].escapeCharForFilename() }
    try {
        return codePoints.mapIndexed { index, c -> c to paths[index] }.toMap()
    } catch (e: Exception) {
        throw e
    }
}

fun DumbOcr.splitAndSaveCharacterImages(imagePath: Path, outputDir: Path) = splitAndSaveCharacterImages(
    image = imagePath.readImage(),
    imageText = inferTextFromPath(imagePath),
    outputDir = outputDir,
)

fun readReferenceImagesFrom(directory: Path): List<ReferenceImage> = directory.listDirectoryEntries().map {
    ReferenceImage(
        image = it.readImage(),
        text = inferTextFromPath(it),
    )
}

// ignores anything after a space to allow disambiguation on case-insensitive file systems
private fun inferTextFromPath(it: Path) = it.nameWithoutExtension.split(" ")[0].unescapeFilenameToChar()

// extending string to support code points above the BMP
private fun String.escapeCharForFilename() = when(this) {
    "." -> "dot"
    "-" -> "dash"
    "/" -> "slash"
    "\\" -> "backslash"
    else -> URLEncoder.encode(this, Charsets.UTF_8)
}

// returning string to support code points above the BMP
private fun String.unescapeFilenameToChar() = when(this) {
    "dot" -> "."
    "dash" -> "-"
    "slash" -> "/"
    "backslash" -> "\\"
    else -> URLDecoder.decode(this, Charsets.UTF_8)
}

/**
 * Reads a [ReferenceImage] from a resource image at the given absolute [resourcePath] (must start with a '/'), and
 * associated to the given [text].
 */
fun referenceImage(resourcePath: String, text: String): ReferenceImage =
    ReferenceImage(resourceImage(resourcePath), text)
