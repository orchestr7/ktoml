/**
 * Syntax validation shared by signed and unsigned TOML integer values.
 */

package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.exceptions.ParseException

/**
 * Validates integer spelling without recursion, including tokens too large for numeric conversion.
 *
 * @param lineNo source line number for parse errors
 * @throws ParseException if the spelling is not a TOML integer literal
 */
internal fun String.validateIntegerSyntax(lineNo: Int) {
    val radix = when {
        startsWith("0b") -> BIN_RADIX
        startsWith("0o") -> OCT_RADIX
        startsWith("0x") -> HEX_RADIX
        else -> DEC_RADIX
    }
    val digitsStart = if (radix == DEC_RADIX) {
        if (firstOrNull() == '+' || firstOrNull() == '-') 1 else 0
    } else {
        2
    }
    val hasLeadingZero = radix == DEC_RADIX && getOrNull(digitsStart) == '0' && length > digitsStart + 1
    if (hasLeadingZero || !hasIntegerDigits(digitsStart, radix)) {
        throw ParseException("Invalid integer literal <$this>.", lineNo)
    }
}

private fun String.hasIntegerDigits(startIndex: Int, radix: Int): Boolean {
    var index = startIndex
    if (index >= length) {
        return false
    }
    while (index < length) {
        if (!this[index].isIntegerDigit(radix)) {
            return false
        }
        index++
        if (getOrNull(index) == '_') {
            index++
            if (index == length) {
                return false
            }
        }
    }
    return true
}

private fun Char.isIntegerDigit(radix: Int): Boolean = when (radix) {
    BIN_RADIX -> this in '0'..'1'
    OCT_RADIX -> this in '0'..'7'
    HEX_RADIX -> this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    else -> this in '0'..'9'
}
