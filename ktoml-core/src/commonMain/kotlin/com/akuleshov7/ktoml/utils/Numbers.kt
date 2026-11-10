/**
 * Syntax validation shared by signed and unsigned TOML integer values.
 */

package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.exceptions.ParseException

private val decimalIntegerPattern = "^[+-]?(?:0|[1-9](?:_?[0-9])*)$".toRegex()
private val binaryIntegerPattern = "^0b[01](?:_?[01])*$".toRegex()
private val octalIntegerPattern = "^0o[0-7](?:_?[0-7])*$".toRegex()
private val hexadecimalIntegerPattern = "^0x[0-9a-fA-F](?:_?[0-9a-fA-F])*$".toRegex()

/**
 * Validate the spelling after a value has been recognized as an integer.
 *
 * Select a radix before matching so Kotlin/JS does not prematurely match the
 * leading zero of a radix-prefixed literal as a decimal alternative.
 *
 * @param lineNo source line number for parse errors
 * @throws ParseException if the spelling is not a TOML integer literal
 */
internal fun String.validateIntegerSyntax(lineNo: Int) {
    val valid = when {
        startsWith("0b") -> binaryIntegerPattern.matches(this)
        startsWith("0o") -> octalIntegerPattern.matches(this)
        startsWith("0x") -> hexadecimalIntegerPattern.matches(this)
        else -> decimalIntegerPattern.matches(this)
    }
    if (!valid) {
        throw ParseException("Invalid integer literal <$this>.", lineNo)
    }
}
