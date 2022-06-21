/**
 * Specific implementation for utilities
 */

package com.akuleshov7.ktoml.utils

@Suppress("MAGIC_NUMBER")
internal actual fun StringBuilder.appendCodePointCompat(codePoint: Int): StringBuilder = when (codePoint) {
    in 0 until Char.MIN_SUPPLEMENTARY_CODE_POINT -> append(codePoint.toChar())
    in Char.MIN_SUPPLEMENTARY_CODE_POINT..Char.MAX_CODE_POINT -> {
        append(Char.MIN_HIGH_SURROGATE + ((codePoint - 0x10000) shr 10))
        append(Char.MIN_LOW_SURROGATE + (codePoint and 0x3ff))
    }
    else -> throw IllegalArgumentException()
}
