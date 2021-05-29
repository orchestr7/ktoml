package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.trimQuotes

class TomlKey(val rawContent: String, val lineNo: Int) {
    // removed quotes at the beginning and at the end of input
    val content = rawContent.trimQuotes()
    val isDotted = isDottedKey()

    init {
        validateQuotes()
        validateSymbols()
    }

    /**
     * small validation for quotes: each quote should be closed in a key
     */
    private fun validateQuotes() {
        if (rawContent.count { it == '\"' } % 2 != 0 || rawContent.count { it == '\'' } % 2 != 0) {
            throw TomlParsingException(
                "Not able to parse the key: [$rawContent] as it does not have closing quote." +
                        " Please note, that you cannot use even escaped quotes in the bare keys.",
                lineNo
            )
        }
    }

    /**
     * validate that bare key parts (not quoted) contain only valid symbols A..Z, a..z, 0..9, -, _
     */
    private fun validateSymbols() {
        var singleQuoteIsClosed = true
        var doubleQuoteIsClosed = true
        rawContent.forEach loopThroughContent@{ ch ->
            when (ch) {
                '\'' -> singleQuoteIsClosed = !singleQuoteIsClosed
                '\"' -> doubleQuoteIsClosed = !doubleQuoteIsClosed
            }

            if (doubleQuoteIsClosed && singleQuoteIsClosed) {
                if (!setOf('_', '-', '.', '"', '\'').contains(ch) && !ch.isLetterOrDigit())
                    throw TomlParsingException(
                        "Not able to parse the key: [$rawContent] as it contains invalid symbols." +
                                " In case you would like to use special symbols - use quotes.",
                        lineNo
                    )
            }
        }
    }

    /**
     * checking that we face a key in the following format: a."ab.c".my-key
     */
    fun isDottedKey(): Boolean {
        var singleQuoteIsClosed = true
        var doubleQuoteIsClosed = true
        rawContent.forEach { ch ->
            when (ch) {
                '\'' -> singleQuoteIsClosed = !singleQuoteIsClosed
                '\"' -> doubleQuoteIsClosed = !doubleQuoteIsClosed
            }

            if (ch == '.' && doubleQuoteIsClosed && singleQuoteIsClosed) return true
        }
        return false
    }


}
