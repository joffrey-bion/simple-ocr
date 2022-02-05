package org.hildan.ocr

import kotlin.math.abs

/**
 * An ARGB color.
 *
 * The given [argb] is an unsigned integer representing the ARGB color components as 4 bytes.
 * There are therefore 8 bits of precision for each component.
 */
@JvmInline
value class Color(val argb: UInt) {
    val alpha get() = (argb shr 24).toUByte()
    val red get() = (argb shr 16).toUByte()
    val green get() = (argb shr 8).toUByte()
    val blue get() = argb.toUByte()

    constructor(alpha: UInt, red: UInt, green: UInt, blue: UInt) : this(argbUInt(alpha, red, green, blue))

    constructor(alpha: UByte, red: UByte, green: UByte, blue: UByte) :
        this(alpha.toUInt(), red.toUInt(), green.toUInt(), blue.toUInt())

    override fun toString(): String = "0x${argb.toString(16)}"

    companion object {
        val WHITE = Color(0xffffffffu)
        val BLACK = Color(0xff000000u)
        val TRANSPARENT = Color(0x00000000u)
    }
}

// can't do shift operations on bytes
private fun argbUInt(alpha: UInt, red: UInt, green: UInt, blue: UInt) =
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue

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
 * corresponding component in the [referenceColor] by no more than the given [rgbTolerance] (or [alphaTolerance],
 * accordingly).
 */
class ColorSimilarityFilter(
    private val referenceColor: Color,
    private val rgbTolerance: Int = 25,
    private val alphaTolerance: Int = 0,
): ColorFilter {
    init {
        require(rgbTolerance >= 0) { "rgbTolerance is a distance and must not be negative" }
        require(alphaTolerance >= 0) { "alphaTolerance is a distance and must not be negative" }
    }

    override fun matches(c: Color): Boolean = alphaCloseEnough(c.alpha, referenceColor.alpha)
        && rgbCloseEnough(c.red, referenceColor.red)
        && rgbCloseEnough(c.green, referenceColor.green)
        && rgbCloseEnough(c.blue, referenceColor.blue)

    private fun alphaCloseEnough(a: UByte, b: UByte) = distance(a, b) <= alphaTolerance

    private fun rgbCloseEnough(a: UByte, b: UByte) = distance(a, b) <= rgbTolerance

    private fun distance(a: UByte, b: UByte) = abs(a.toInt() - b.toInt())
}
