package com.akuleshov7.ktoml.utils

internal actual fun StringBuilder.appendCodePointCompat(codePoint: Int): StringBuilder {
    return appendCodePoint(codePoint)
}