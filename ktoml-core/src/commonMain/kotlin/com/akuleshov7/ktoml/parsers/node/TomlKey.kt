package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimQuotes

/**
 * Class that represents a toml key-value pair.
 * Key has TomlKey type, Value has TomlValue type
 *
 * @property rawContent
 * @property lineNo
 */
class TomlKey(val rawContent: String, val lineNo: Int) {

    val keyParts = rawContent.splitKeyToTokens(lineNo)
    val content = keyParts.last().trimQuotes().trim()
    val isDotted = isDottedKey()

    /**
     * checking that we face a key in the following format: a."ab.c".my-key
     *
     * @return true if the key is in dotted format (a.b.c)
     */
    private fun isDottedKey(): Boolean {
        var singleQuoteIsClosed = true
        var doubleQuoteIsClosed = true
        rawContent.forEach { ch ->
            when (ch) {
                '\'' -> singleQuoteIsClosed = !singleQuoteIsClosed
                '\"' -> doubleQuoteIsClosed = !doubleQuoteIsClosed
                else -> {
                    // this is a generated else block
                }
            }

            if (ch == '.' && doubleQuoteIsClosed && singleQuoteIsClosed) {
                return true
            }
        }
        return false
    }
}
