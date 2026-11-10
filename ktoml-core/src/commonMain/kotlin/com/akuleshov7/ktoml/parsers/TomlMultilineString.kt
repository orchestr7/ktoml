package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.enums.MultilineType
import com.akuleshov7.ktoml.utils.LinesIteratorWrapper
import com.akuleshov7.ktoml.utils.newLineChar

private const val TRIPLE_QUOTE_LENGTH = 3

/**
 * @param config
 * @param linesIteratorWrapper - iterator with the rest of the toml data
 * @param firstLine - first line of multiline value where it was detected
 */
internal class TomlMultilineString(
    private val config: TomlInputConfig,
    private val linesIteratorWrapper: LinesIteratorWrapper<String>,
    firstLine: String,
) {
    private val comments: MutableList<String> = mutableListOf()
    private val lines: MutableList<String> = mutableListOf()

    // For each stored line, the closing delimiter (`"""` or `'''`) of the multiline string it
    // *begins* inside, or null if it does not begin inside one. A line that begins inside such a
    // string carries its *closing* delimiter, so any `#` after that close is a real comment that
    // naive per-line stripping (which would read the leading delimiter as an *opening* one) misses.
    private val openMultilineStringDelimiterAtLineStart: MutableList<String?> = mutableListOf()
    private val startLineNo = linesIteratorWrapper.lineNo
    private val multilineType = getMultilineType(firstLine, config)
    private var isInMultilineBasic = false
    private var isInMultilineLiteral = false

    // If isNested is null, we don't know yet if the type is nested
    private var isNested = if (multilineType.isNestedSupported) null else false

    init {
        if (multilineType == MultilineType.NOT_A_MULTILINE) {
            throw ParseException("Internal parse exception", startLineNo)
        }
        openMultilineStringDelimiterAtLineStart.add(null)
        trackMultilineString(firstLine)
        lines.add(firstLine.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings))
        parseMultiline()
    }

    fun getLine(): String = if (multilineType == MultilineType.ARRAY || multilineType == MultilineType.INLINE_TABLE) {
        lines.mapIndexed { index, line ->
            line.takeBeforeCommentFrom(openMultilineStringDelimiterAtLineStart.getOrElse(index) { null })
        }.joinToString(newLineChar().toString())
    } else {
        // we can't have comments inside multi-line basic/literal string
        lines.joinToString(newLineChar().toString())
    }

    fun getComments(): List<String> = comments

    private fun parseMultiline() {
        var hasFoundEnd = false

        while (linesIteratorWrapper.hasNext()) {
            val line = linesIteratorWrapper.next()
            // State *before* this line is processed: the closing delimiter of the multiline string
            // this line opens inside (or null), so we know its leading delimiter is a close.
            val openDelimiter = openMultilineStringDelimiter()
            openMultilineStringDelimiterAtLineStart.add(openDelimiter)
            trackMultilineString(line)

            if (!stringTypes.contains(multilineType)) {
                if (!isInMultilineString()) {
                    comments.add(line.trimCommentFrom(openDelimiter))
                    lines.add(line.takeBeforeCommentFrom(openDelimiter))
                } else {
                    // We're inside multiline basic/literal string element, so there's no comments
                    lines.add(line)
                }
            } else {
                // We have multiline basic/literal string MultilineType; They don't have comments inside
                lines.add(line)
            }

            if (!isInMultilineString() && isEndOfMultilineValue(multilineType)) {
                hasFoundEnd = true
                break
            }
        }

        if (!hasFoundEnd) {
            throw ParseException(
                "Expected (${multilineType.closingSymbols}) in the end of ${multilineType.name}",
                startLineNo,
            )
        }
    }

    /**
     * When we have an array with multiline strings, and we're parsing line X
     * we want to know if multiline string was open before line X
     */
    private fun trackMultilineString(line: String) {
        if (stringTypes.contains(multilineType)) {
            return
        }
        for (i in 0..line.length - 3) {
            // Stumbled upon a comment, no need to analyze for the rest of the line
            if (!isInMultilineBasic && !isInMultilineLiteral && line[i] == '#') {
                break
            }

            if (!isInMultilineLiteral && isNextThreeQuotes(line, i, '"')) {
                isInMultilineBasic = !isInMultilineBasic
            } else if (!isInMultilineBasic && isNextThreeQuotes(line, i, '\'')) {
                isInMultilineLiteral = !isInMultilineLiteral
            }
        }
    }

    private fun isNextThreeQuotes(
        line: String,
        index: Int,
        quote: Char
    ): Boolean = line[index] == quote && line[index + 1] == quote && line[index + 2] == quote

    private fun isInMultilineString(): Boolean = isInMultilineBasic || isInMultilineLiteral

    /**
     * @return the closing delimiter (`"""` or `'''`) of the multiline string that is currently
     *   open, or null when no multiline basic/literal string is open.
     */
    private fun openMultilineStringDelimiter(): String? = when {
        isInMultilineBasic -> MultilineType.BASIC_STRING.closingSymbols
        isInMultilineLiteral -> MultilineType.LITERAL_STRING.closingSymbols
        else -> null
    }

    /**
     * Like [takeBeforeComment], but aware of a multiline string left open by a previous line:
     * when [openDelimiter] is non-null this line begins inside that string, so we first skip past
     * its closing delimiter and only then look for the comment `#`.
     *
     * @param openDelimiter the closing delimiter of a string open at the start of this line, or null
     * @return the text of this line before its comment (if any)
     */
    private fun String.takeBeforeCommentFrom(openDelimiter: String?): String {
        val searchStart = commentSearchStart(openDelimiter) ?: return this
        val commentIdx = indexOfNextOutsideQuotes(config.allowEscapedQuotesInLiteralStrings, '#', searchStart)
        return if (commentIdx == -1) this else substring(0, commentIdx)
    }

    /**
     * Like [trimComment], but aware of a multiline string left open by a previous line (see
     * [takeBeforeCommentFrom]).
     *
     * @param openDelimiter the closing delimiter of a string open at the start of this line, or null
     * @return the comment text of this line (without the `#`), or an empty string if there is none
     */
    private fun String.trimCommentFrom(openDelimiter: String?): String {
        val searchStart = commentSearchStart(openDelimiter) ?: return ""
        val commentIdx = indexOfNextOutsideQuotes(config.allowEscapedQuotesInLiteralStrings, '#', searchStart)
        return if (commentIdx == -1) "" else drop(commentIdx + 1).trim()
    }

    /**
     * The index from which to start scanning this line for a comment `#`. When [openDelimiter] is
     * non-null, the line opens inside a multiline string, so scanning must start right after that
     * string's closing delimiter; null means the close was not found on this line (no comment).
     */
    private fun String.commentSearchStart(openDelimiter: String?): Int? {
        val delimiter = openDelimiter ?: return 0
        val closeIdx = indexOf(delimiter)
        return if (closeIdx == -1) null else closeIdx + delimiter.length
    }

    /**
     * @return true if string is a last line of multiline value declaration
     */
    private fun isEndOfMultilineValue(multilineType: MultilineType): Boolean {
        if (multilineType == MultilineType.INLINE_TABLE) {
            // An inline table is complete once its braces are balanced. Brace counting ignores
            // braces inside quotes/strings, so nested arrays and multiline strings are handled.
            return getLine().inlineTableBraceDepth(config.allowEscapedQuotesInLiteralStrings) <= 0
        }
        if (multilineType == MultilineType.ARRAY) {
            return hasClosedMultilineArray()
        }
        isNested ?: run {
            isNested = hasTwoConsecutiveSymbolsIgnoreWhitespaces(getLine(), multilineType.openSymbols[0])
        }

        return if (isNested == true) {
            val clearedString = lines.joinToString("")
                .filter { !it.isWhitespace() }

            clearedString.endsWith(multilineType.closingSymbols + multilineType.closingSymbols)
        } else {
            // Checks if this line ends with closing symbols, allowing for whitespace or a comment after those
            // symbols. Note that we're not using [indexOfNextOutsideOfQuotes] here because the last line of a
            // multiline string (eg `""" # this`) would consider the comment inside the quote.
            val closingSymbolsIdx = lines.last().lastIndexOf(multilineType.closingSymbols)
            if (closingSymbolsIdx < 0) {
                return false
            }
            lines.last()
                .substring(closingSymbolsIdx + multilineType.closingSymbols.length)
                .takeBeforeComment(config.allowEscapedQuotesInLiteralStrings)
                .trim()
                .isEmpty()
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "NESTED_BLOCK")
    private fun hasClosedMultilineArray(): Boolean {
        val value = getLine()
            .substringAfter('=')
            .replaceEscaped(config.allowEscapedQuotesInLiteralStrings)

        var bracketBalance = 0
        var currentQuoteStr: String? = null
        var idx = 0

        while (idx < value.length) {
            val symbol = value[idx]
            val quoteStr = currentQuoteStr

            if (quoteStr == null) {
                when (symbol) {
                    '[' -> bracketBalance++
                    ']' -> bracketBalance--
                    '"', '\'' -> {
                        currentQuoteStr = if (idx + 2 < value.length && value[idx + 1] == symbol && value[idx + 2] == symbol) {
                            "$symbol$symbol$symbol"
                        } else {
                            symbol.toString()
                        }
                        idx += currentQuoteStr!!.length
                        continue
                    }
                    else -> Unit
                }
            } else if (quoteStr[0] == symbol && idx + quoteStr.length <= value.length) {
                val candidate = value.substring(idx, idx + quoteStr.length)
                if (candidate == quoteStr) {
                    currentQuoteStr = null
                    idx += candidate.length
                    continue
                }
            }

            idx += 1
        }

        return bracketBalance == 0
    }

    private fun hasTwoConsecutiveSymbolsIgnoreWhitespaces(value: String, searchSymbol: Char): Boolean? {
        val firstIndex = value.indexOf(searchSymbol)
        if (firstIndex == -1) {
            return false
        }

        val nextIndex = value.indexOf(searchSymbol, firstIndex + 1)

        if (nextIndex != -1) {
            val between = value.substring(firstIndex + 1, nextIndex)
            return between.all { it.isWhitespace() }
        }

        val isRestHasOnlyWhitespaces = !value.substring(firstIndex + 1).any { !it.isWhitespace() }
        return if (isRestHasOnlyWhitespaces) {
            null
        } else {
            false
        }
    }

    companion object {
        private val stringTypes = listOf(MultilineType.BASIC_STRING, MultilineType.LITERAL_STRING)

        /**
         * Important! We treat a multi-line that is declared in one line ("""abc""") as a regular not multiline string
         *
         * @param line
         * @param config
         * @return MultilineType
         */
        fun getMultilineType(line: String, config: TomlInputConfig): MultilineType {
            val line = line.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings)
            val firstEqualsSign = line.indexOfFirst { it == '=' }
            if (firstEqualsSign == -1) {
                return MultilineType.NOT_A_MULTILINE
            }
            val value = line.substring(firstEqualsSign + 1).trim()

            if (value.startsWith(MultilineType.ARRAY.openSymbols) &&
                    !value.endsWith(MultilineType.ARRAY.closingSymbols)
            ) {
                return MultilineType.ARRAY
            }

            // TOML 1.1: an inline table whose braces are still open on this line continues onto
            // the next ones (newline between pairs, trailing comma, or a value that spans lines).
            if (value.startsWith(MultilineType.INLINE_TABLE.openSymbols) &&
                    value.inlineTableBraceDepth(config.allowEscapedQuotesInLiteralStrings) > 0
            ) {
                return MultilineType.INLINE_TABLE
            }

            // If we have more than 1 combination of (""") - it means that
            // multi-line is declared in one line, and we can handle it as not a multi-line
            if (value.startsWith(MultilineType.BASIC_STRING.openSymbols) && value.getCountOfOccurrencesOfSubstring(MultilineType.BASIC_STRING.openSymbols) == 1
            ) {
                return MultilineType.BASIC_STRING
            }
            if (value.startsWith(MultilineType.LITERAL_STRING.openSymbols) &&
                    value.getCountOfOccurrencesOfSubstring(MultilineType.LITERAL_STRING.openSymbols) == 1
            ) {
                return MultilineType.LITERAL_STRING
            }

            return MultilineType.NOT_A_MULTILINE
        }
    }
}

/**
 * Net brace depth (`{` minus `}`) counted outside of quotes and comments. Single, literal and
 * triple-quoted (multiline) strings are skipped, as is any text after a `#`. A positive result
 * means a multiline inline table is still open; `<= 0` means its braces are balanced.
 *
 * @param allowEscapedQuotesInLiteralStrings value from TomlInputConfig
 * @return the net brace depth
 */
@Suppress("NESTED_BLOCK")
private fun String.inlineTableBraceDepth(allowEscapedQuotesInLiteralStrings: Boolean): Int {
    val chars = this.replaceEscaped(allowEscapedQuotesInLiteralStrings)
    var depth = 0
    var quote: String? = null
    var idx = 0
    while (idx < chars.length) {
        val symbol = chars[idx]
        when {
            quote != null -> if (chars.startsWith(quote, idx)) {
                idx += quote.lastIndex
                quote = null
            }
            symbol == '#' -> idx = (chars.indexOf(newLineChar(), idx).takeIf { it != -1 } ?: chars.length) - 1
            symbol == '\"' || symbol == '\'' -> quote = chars.openingQuoteAt(idx, symbol)
            symbol == '{' -> depth += 1
            symbol == '}' -> depth -= 1
            else -> {}
        }
        idx += 1
    }
    return depth
}

/** The quote token opening at [idx]: a triple quote if three [symbol]s appear, otherwise a single one. */
private fun String.openingQuoteAt(idx: Int, symbol: Char): String =
    if (idx + 2 < length && this[idx + 1] == symbol && this[idx + 2] == symbol) {
        symbol.toString().repeat(TRIPLE_QUOTE_LENGTH)
    } else {
        symbol.toString()
    }
