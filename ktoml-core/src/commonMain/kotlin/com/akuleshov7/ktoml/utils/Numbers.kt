/**
 * Syntax validation shared by signed and unsigned TOML integer values.
 */

package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.exceptions.ParseException

private val integerPattern = (
        "[+-]?(?:0|[1-9](?:_?[0-9])*)|" +
                "0b[01](?:_?[01])*|0o[0-7](?:_?[0-7])*|0x[0-9a-fA-F](?:_?[0-9a-fA-F])*"
).toRegex()

/**
 * Validate the spelling after a value has been recognized as an integer.
 *
 * @param lineNo source line number for parse errors
 * @throws ParseException if the spelling is not a TOML integer literal
 */
internal fun String.validateIntegerSyntax(lineNo: Int) {
    if (!integerPattern.matches(this)) {
        throw ParseException("Invalid integer literal <$this>.", lineNo)
    }
}
