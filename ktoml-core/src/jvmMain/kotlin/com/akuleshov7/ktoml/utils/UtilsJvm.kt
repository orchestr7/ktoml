/**
 * Specific implementation for utilities
 */

package com.akuleshov7.ktoml.utils

internal actual fun StringBuilder.appendCodePointCompat(codePoint: Int): StringBuilder = appendCodePoint(codePoint)
