/**
 * Regexes used in tree writing.
 */

package com.akuleshov7.ktoml.utils

/**
 * Matches control characters not allowed in strings. Specifically, this matches
 * Unicode block [`Cc`](https://www.compart.com/en/unicode/category/Cc) excluding
 * tab.
 *
 * Used to replace these characters with valid Unicode escapes when writing basic
 * strings, or throw an exception for them when writing literal strings.
 */
internal val controlCharacterRegex: Regex =
        Regex("""[\x00-\x08\x0A-\x1F\x7F\x80-\x9F]""")

/**
 * Matches an unescaped backlash character. A backslash will be excluded if followed
 * by:
 * - another backslash
 * - any of the "compact escapes" (i.e. `\t`, `\"`, etc.)
 * - a Unicode escape, "u" or "U" followed by 4 or 8 hex digits respectively
 */
internal val unescapedBackslashRegex: Regex =
        Regex("""\\\\|\\(?![btnfr"]|u[0-9a-fA-F]{4}|U[0-9a-fA-F]{8})""")
