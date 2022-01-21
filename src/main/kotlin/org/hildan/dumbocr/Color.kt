package org.hildan.dumbocr

import kotlin.math.abs

/**
 * An ARGB color.
 *
 * The given [argb] is an unsigned integer representing the ARGB color components as 4 bytes.
 * There are therefore 8 bits of precision for each component.
 */
@JvmInline
value class Color(private val argb: UInt) {
    val alpha get() = (argb shr 24).toUByte()
    val red get() = (argb shr 16).toUByte()
    val green get() = (argb shr 8).toUByte()
    val blue get() = argb.toUByte()

    override fun toString(): String = "0x${argb.toString(16)}"

    companion object {
        val WHITE = Color(0xffffffffu)
        val BLACK = Color(0xff000000u)
    }
}

/**
 * A predicate on [Color].
 */
fun interface ColorFilter {

    fun matches(c: Color): Boolean
}

/**
 * A [ColorFilter] that matches colors that are close enough to the given [referenceColor].
 *
 * Colors are considered "close enough" to the reference color when each of the ARGB component is different from the
 * corresponding component in the [referenceColor] by no more than the given [tolerance].
 */
class ColorSimilarityFilter(
    private val referenceColor: Color,
    private val tolerance: Int = 25,
): ColorFilter {
    init {
        require(tolerance >= 0) { "tolerance is a distance and must not be negative"}
    }

    override fun matches(c: Color): Boolean = closeEnough(c.alpha, referenceColor.alpha)
        && closeEnough(c.red, referenceColor.red)
        && closeEnough(c.green, referenceColor.green)
        && closeEnough(c.blue, referenceColor.blue)

    private fun closeEnough(a: UByte, b: UByte) = abs(a.toInt() - b.toInt()) <= tolerance
}
