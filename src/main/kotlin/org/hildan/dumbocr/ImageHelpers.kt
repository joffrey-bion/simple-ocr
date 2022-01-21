package org.hildan.dumbocr

import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * Reads an image from the file at this path.
 */
fun Path.readImage(): BufferedImage = ImageIO.read(toFile())

/**
 * Reads an image from a resource at the given absolute [resourcePath] (must start with a '/').
 */
fun resourceImage(resourcePath: String): BufferedImage =
    ImageIO.read(ReferenceImage::class.java.getResourceAsStream(resourcePath))
