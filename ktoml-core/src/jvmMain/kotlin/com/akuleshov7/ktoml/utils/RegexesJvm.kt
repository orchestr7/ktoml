/**
 * Regexes used in tree writing.
 */

package com.akuleshov7.ktoml.utils

import kotlin.text.RegexOption.COMMENTS

internal actual val controlCharacterRegex = Regex("""[\p{Cc}\x7F&&[^\t]]""")

@Suppress("RegExpRepeatedSpace")
internal actual val unescapedBackslashRegex =
        Regex(
            """
            \\\\ | # Match already escaped slashes
            \\
            # Don't match a slash with escapes ahead
            (?!
                [btnfr"]       | # Compact escapes
                u[\da-fA-F]{4} | # Simple unicode
                U[\da-fA-F]{8}   # Complex unicode
            )
            """.trimIndent(),
            COMMENTS
        )
