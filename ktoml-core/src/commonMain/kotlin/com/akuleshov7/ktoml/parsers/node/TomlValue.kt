package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.trimQuotes


sealed class TomlValue(val lineNo: Int) {
    abstract var content: Any
}

class TomlString(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = if (content.startsWith("\"") && content.endsWith("\"")) {
        val stringWithoutQuotes = content.trimQuotes()
        checkOtherQuotesAreEscaped(stringWithoutQuotes)
        convertSpecialCharacters(stringWithoutQuotes)
    } else {
        throw TomlParsingException(
            "According to the TOML specification string values (even Enums)" +
                    " should be wrapped with quotes (\"\"): <$content>", lineNo
        )
    }

    private fun checkOtherQuotesAreEscaped(stringWithoutQuotes: String) {
        stringWithoutQuotes.forEachIndexed { index, ch ->
            if (ch == '\"' && (index == 0 || stringWithoutQuotes[index - 1] != '\\')) {
                throw TomlParsingException(
                    "Found invalid quote that is not escaped, index = [$index]." +
                            " Please remove the quote or use escape symbols in <$content>", lineNo
                )
            }
        }
    }

    private fun convertSpecialCharacters(stringWithoutQuotes: String): String {
        val resultString = StringBuilder()
        var updatedOnPreviousStep = false
        var i = 0
        while (i < stringWithoutQuotes.length) {
            val newCharacter = if (stringWithoutQuotes[i] == '\\' && i != stringWithoutQuotes.length - 1) {
                updatedOnPreviousStep = true
                when (stringWithoutQuotes[i + 1]) {
                    '\\' -> '\\'
                    't' -> '\t'
                    'b' -> '\b'
                    'r' -> '\r'
                    'n' -> '\n'
                    else -> throw TomlParsingException(
                        "According to TOML documentation unknown" +
                                " escape symbols are not allowed. Please check [\\${stringWithoutQuotes[i + 1]}]",
                        lineNo
                    )
                }
            } else {
                stringWithoutQuotes[i]
            }
            // need to skip the next character if we have processed special escaped symbol
            if (updatedOnPreviousStep) {
                updatedOnPreviousStep = false
                i += 2
            } else {
                i += 1
            }

            resultString.append(newCharacter)
        }
        return resultString.toString()
    }
}

class TomlInt(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toInt()
}

class TomlFloat(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toFloat()
}

class TomlBoolean(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toBoolean()
}

class TomlNull(lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = "null"
}

class TomlArray(val rawContent: String, lineNo: Int) : TomlValue(lineNo) {
    override lateinit var content: Any

    init {
        validateBrackets()
        var singleQuoteIsClosed = true
        var doubleQuoteIsClosed = true
        val dotSeparatedParts: MutableList<String> = mutableListOf()
        var currentPart = StringBuilder()
        // simple split won't work here, because in such case we could break following keys:
        // a."b.c.d".e (here only three tables: a/"b.c.d"/and e)
        rawContent.trimQuotes().forEach { ch ->
            when (ch) {
                '\'' -> singleQuoteIsClosed = !singleQuoteIsClosed
                '\"' -> doubleQuoteIsClosed = !doubleQuoteIsClosed
                '.' -> {
                    if (singleQuoteIsClosed && doubleQuoteIsClosed) {
                        dotSeparatedParts.add(currentPart.toString())
                        currentPart = StringBuilder()
                    } else {
                        currentPart.append(ch)
                    }
                }
                else -> currentPart.append(ch)
            }
        }
        // in the end of the word we should also add buffer to the list (as we haven't faced any dots)
        dotSeparatedParts.add(currentPart.toString())

        this.content = listOf(TomlInt("1", lineNo), TomlInt("2", lineNo))
    }

    /**
     * small validation for quotes: each quote should be closed in a key
     */
    private fun validateBrackets() {
        if (rawContent.count { it == '\"' } % 2 != 0 || rawContent.count { it == '\'' } % 2 != 0) {
            throw TomlParsingException(
                "Not able to parse the key: [$rawContent] as it does not have closing bracket",
                lineNo
            )
        }
    }
}
