package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.KtomlConf.Indentation.FOUR_SPACES

/**
 * @property ignoreUnknownNames - a control to allow/prohibit unknown names during the deserialization
 * @property emptyValuesAllowed - a control to allow/prohibit empty values: a = # comment
 * @property escapedQuotesInLiteralStringsAllowed - a control to allow/prohibit escaping of single quotes in literal strings
 * @property indentation
 */
public data class KtomlConf(
    val ignoreUnknownNames: Boolean = false,
    val emptyValuesAllowed: Boolean = true,
    val escapedQuotesInLiteralStringsAllowed: Boolean = true,
    val indentation: Indentation = FOUR_SPACES,
) {
    /**
     * @property value
     */
    public enum class Indentation(public val value: String) {
        FOUR_SPACES("    "),
        NONE(""),
        TAB("\t"),
        TWO_SPACES("  "),
        ;
    }
}
