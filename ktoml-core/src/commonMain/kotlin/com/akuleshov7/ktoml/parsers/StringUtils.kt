/**
 * Common String Utilities
 */

package com.akuleshov7.ktoml.parsers

/**
 * Splitting dot-separated string to tokens:
 * a.b.c -> [a, b, c]; a."b.c".d -> [a, "b.c", d];
 * @return list with strings after the initial string was split
 */
fun String.splitKeyToTokens(): List<String> {
    var singleQuoteIsClosed = true
    var doubleQuoteIsClosed = true
    val dotSeparatedParts: MutableList<String> = mutableListOf()
    var currentPart = StringBuilder()
    // simple split() method won't work here, because in such case we could break following keys:
    // a."b.c.d".e (here only three tables: a/"b.c.d"/and e)
    this.forEach { ch ->
        when (ch) {
            '\'' -> {
                singleQuoteIsClosed = !singleQuoteIsClosed
                currentPart.append(ch)
            }
            '\"' -> {
                doubleQuoteIsClosed = !doubleQuoteIsClosed
                currentPart.append(ch)
            }
            '.' -> if (singleQuoteIsClosed && doubleQuoteIsClosed) {
                dotSeparatedParts.add(currentPart.toString())
                currentPart = StringBuilder()
            } else {
                currentPart.append(ch)
            }
            else -> currentPart.append(ch)
        }
    }
    // in the end of the word we should also add buffer to the list (in case we haven't found any dots)
    dotSeparatedParts.add(currentPart.toString())
    return dotSeparatedParts
}

/**
 * If this string starts and end with quotes("") - will return the string with quotes removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
fun String.trimQuotes(): String = trimSymbols(this, "\"", "\"")

/**
 * If this string starts and end with brackets([]) - will return the string with brackets removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
fun String.trimBrackets(): String = trimSymbols(this, "[", "]")

private fun trimSymbols(
    str: String,
    prefix: String,
    suffix: String
): String {
    if (str.startsWith(prefix) && str.endsWith(suffix)) {
        return str.removePrefix(prefix).removeSuffix(suffix)
    }
    return str
}
