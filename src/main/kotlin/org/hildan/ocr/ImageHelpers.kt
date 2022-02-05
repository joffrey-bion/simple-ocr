package org.hildan.ocr

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * Reads a [BufferedImage] from the file at this path.
 */
fun Path.readImage(): BufferedImage = ImageIO.read(toFile())

/**
 * Reads a [BufferedImage] from this input stream.
 */
fun InputStream.readImage(): BufferedImage = ImageIO.read(this)

/**
 * Reads a [BufferedImage] from a resource at the given [resourcePath] relative to this instance's class.
 *
 * This is a helper on top of [Class.getResourceAsStream]. See that method for more information on the resolution.
 */
fun Class<*>.getResourceImage(resourcePath: String): BufferedImage = getResourceAsStream(resourcePath)?.readImage()
    ?: throw IllegalArgumentException("Resource not found at path $resourcePath")

/**
 * Converts this [BufferedImage] to bytes in the given [format].
 */
fun BufferedImage.toByteArray(format: String): ByteArray {
    val baos = ByteArrayOutputStream()
    ImageIO.write(this, format, baos)
    return baos.toByteArray()
}
