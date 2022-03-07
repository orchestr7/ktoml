package com.akuleshov7.ktoml.utils

internal actual fun StringBuilder.appendCodePointCompat(codePoint: Int): StringBuilder {
    return when (codePoint) {
        in 0 until MIN_SUPPLEMENTARY_CODE_POINT -> append(codePoint.toChar())
        in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT -> {
            append(Char.MIN_HIGH_SURROGATE + ((codePoint - 0x10000) shr 10))
            append(Char.MIN_LOW_SURROGATE + (codePoint and 0x3ff))
        }
        else -> throw IllegalArgumentException()
    }
}

private const val MAX_CODE_POINT = 0X10FFFF
private const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000
private const val MIN_LOW_SURROGATE: Int = '\uDC00'.code
private const val MIN_HIGH_SURROGATE: Int = '\uD800'.code