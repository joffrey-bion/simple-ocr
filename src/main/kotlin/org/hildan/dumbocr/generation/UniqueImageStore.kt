package org.hildan.dumbocr.generation

import org.hildan.dumbocr.toByteArray
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.HashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/**
 * Stores images in the given [imagesDir], detecting and preventing duplicates.
 *
 * The [imageFormat] is used to write [BufferedImage]s to files, and for the file extension of the images.
 * It can be any format in [ImageIO.getWriterFormatNames] (at least "png", "jpeg", "gif", "bmp").
 */
class UniqueImageStore(
    private val imagesDir: Path,
    private val imageFormat: String = "png",
) {
    init {
        imagesDir.createDirectories()
    }

    private val imagesPathByHash = imagesDir.listDirectoryEntries()
        .groupBy { it.hash() }
        .mapValuesTo(HashMap()) { (_, list) -> list.toMutableList() }

    /**
     * Ensures the given [image] is saved in this store and returns its [Path].
     *
     * If an identical image already exists, the existing file is kept and its path is returned.
     * If not, the given image is written as a new file and its path is returned.
     */
    fun saveOrGetPath(image: BufferedImage): Path = saveOrGetPath(image.toByteArray(imageFormat))

    /**
     * Ensures the given image is saved in this store and returns its [Path].
     * The [imageBytes] must be provided in the correct [imageFormat] for this store.
     *
     * If an identical image already exists, the existing file is kept and its path is returned.
     * If not, the given image is written as a new file and its path is returned.
     */
    fun saveOrGetPath(imageBytes: ByteArray): Path {
        val hash = imageBytes.hash()
        return findExistingImage(imageBytes, hash) ?: writeImage(imageBytes, hash)
    }

    private fun findExistingImage(imageBytes: ByteArray, hash: String): Path? {
        val similarImages = imagesPathByHash[hash]
        return similarImages?.firstOrNull { it.readBytes().contentEquals(imageBytes) }
    }

    private fun writeImage(imageBytes: ByteArray, hash: String): Path {
        val path = imagesDir.resolve("${UUID.randomUUID()}.$imageFormat")
        path.writeBytes(imageBytes)
        imagesPathByHash.getOrPut(hash) { mutableListOf() }.add(path)
        return path
    }
}

private fun Path.hash(): String = readBytes().hash()

private fun ByteArray.hash(): String = sha256().encodeBase64()

private fun ByteArray.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)

private fun ByteArray.encodeBase64(): String = Base64.getEncoder().encodeToString(this)
