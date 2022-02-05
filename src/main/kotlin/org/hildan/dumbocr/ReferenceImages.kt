package org.hildan.dumbocr

import org.hildan.dumbocr.generation.UniqueImageStore
import java.awt.image.BufferedImage
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
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

object ReferenceImages {

    /**
     * Reads [ReferenceImage]s from the given [directory], associating them with text based on their names.
     *
     * If it contains no spaces, the name without extension of the image file is used as text for the image.
     * Otherwise, only the part of the name up to the first space is used as text.
     *
     * This way, more info can be added to the name after a space, which can be useful to map several images to the
     * same text, or have images for lowercase and uppercase letters on case-insensitive file systems (for example,
     * `"a.png"` and `"A upper.png"`.
     *
     * A [glob] pattern can be used to filter the files from the given [directory].
     */
    fun readFrom(directory: Path, glob: String = "*"): List<ReferenceImage> = directory.listDirectoryEntries(glob).map {
        ReferenceImage(image = it.readImage(), text = inferTextFromPath(it))
    }
}

// ignores anything after a space to allow disambiguation on case-insensitive file systems
private fun inferTextFromPath(it: Path) = it.nameWithoutExtension.split(" ")[0].unescapeFilenameToChar()

/**
 * Splits the given [image] into sub-images of text elements, and saves them as files into the given [outputDir].
 * Each file is named based on the result of [subImageFilenameWithoutExt], which is called for each sub-image.
 *
 * Sub-images are often individual characters, but sometimes several characters can be grouped together due to kerning.
 * For instance, a lowercase letter following an uppercase T ou V can be part of a single sub-image (Te, To, Va...).
 */
fun TextDetector.splitAndSaveSubImages(
    image: BufferedImage,
    outputDir: Path,
    subImageFilenameWithoutExt: (index: Int, subImg: BufferedImage) -> String = { _, _ -> UUID.randomUUID().toString() },
): List<Path> {
    outputDir.createDirectories()
    return splitTextElements(image).mapIndexed { index, subImg ->
        outputDir.resolve(subImageFilenameWithoutExt(index, subImg) + ".png").also { path ->
            ImageIO.write(subImg, "png", path.toFile())
        }
    }
}

/**
 * Splits the given [image] into sub-images of text elements, and saves them as files into the given [imageStore].
 * The image store reuses images and doesn't write duplicates.
 *
 * Sub-images are often individual characters, but sometimes several characters can be grouped together due to kerning.
 * For instance, a lowercase letter following an uppercase T ou V can be part of a single sub-image (Te, To, Va...).
 */
fun TextDetector.splitAndSaveSubImages(
    image: BufferedImage,
    imageStore: UniqueImageStore,
): List<Path> = splitTextElements(image).map { subImg ->
    imageStore.saveOrGetPath(subImg)
}

/**
 * Splits the given [image] into sub-images of text elements, and saves them as files into the given [outputDir].
 * Each file is named based on the characters (more specifically, the unicode code points) in [imageText].
 * Characters that are not valid as file names are escaped.
 *
 * ## Important note
 *
 * Sub-images are often individual characters, but sometimes several characters can be grouped together due to kerning.
 * For instance, a lowercase letter following an uppercase T ou V can be part of a single sub-image (Te, To, Va...).
 *
 * If the kerning of your font causes this kind of grouping, this method will not properly map images to characters.
 * In that case, please prefer [splitAndSaveSubImages].
 */
fun TextDetector.splitAndSaveCharacterImages(
    image: BufferedImage,
    imageText: String,
    outputDir: Path,
): List<Path> {
    val codePoints = imageText.replace(whiteSpaceRegex, "").splitCodePoints()
    return splitAndSaveSubImages(image, outputDir) { index, _ -> codePoints[index].escapeCharForFilename() }
}

private fun String.splitCodePoints() = codePoints().toList().map { Character.toString(it) }

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
