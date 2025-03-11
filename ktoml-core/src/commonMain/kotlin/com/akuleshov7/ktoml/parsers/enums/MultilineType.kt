package com.akuleshov7.ktoml.parsers.enums

/**
 * @property openSymbols - symbols indicating that the multi-line is opened
 * @property closingSymbols - symbols indicating that the multi-line is closed
 * @property isNestedSupported - if the multi-line type can be nested
 */
internal enum class MultilineType(
    val openSymbols: String,
    val closingSymbols: String,
    val isNestedSupported: Boolean,
) {
    ARRAY(
        "[",
        "]",
        true
    ),
    BASIC_STRING(
        "\"\"\"",
        "\"\"\"",
        false
    ),
    LITERAL_STRING(
        "'''",
        "'''",
        false
    ),
    NOT_A_MULTILINE(
        "",
        "",
        false
    ),
    ;
}
