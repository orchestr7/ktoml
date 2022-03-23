/**
 * Regexes used in tree writing.
 */

package com.akuleshov7.ktoml.utils

internal expect val controlCharacterRegex: Regex

internal expect val unescapedBackslashRegex: Regex

internal val commonControlCharacterRegex =
        Regex("""[\x00-\x08\x0A-\x1F\x7F\x80-\x9F]""")

internal val commonUnescapedBackslashRegex =
        Regex("""\\\\|\\(?![btnfr"]|u[0-9a-fA-F]{4}|U[0-9a-fA-F]{8})""")
