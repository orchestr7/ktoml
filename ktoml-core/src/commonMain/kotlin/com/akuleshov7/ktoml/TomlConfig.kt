package com.akuleshov7.ktoml

@Deprecated(
    message = "Class name changed for convention.",
    replaceWith = ReplaceWith("TomlConfig", "com.akuleshov7.ktoml.TomlConfig")
)
public data class KtomlConf(
    val ignoreUnknownNames: Boolean = false,
    val emptyValuesAllowed: Boolean = true,
    val escapedQuotesInLiteralStringsAllowed: Boolean = true
)

/**
 * @property ignoreUnknownNames - a control to allow/prohibit unknown names during the deserialization
 * @property allowEmptyValues - a control to allow/prohibit empty values: a = # comment
 * @property allowEscapedQuotesInLiteralStrings - a control to allow/prohibit escaping of single quotes in literal strings
 */
public data class TomlConfig(
    val ignoreUnknownNames: Boolean = false,
    val allowEmptyValues: Boolean = true,
    val allowEscapedQuotesInLiteralStrings: Boolean = true
)
