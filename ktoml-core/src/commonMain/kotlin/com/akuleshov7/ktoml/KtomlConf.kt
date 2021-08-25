package com.akuleshov7.ktoml

/**
 * @property ignoreUnknownNames - a control to allow/prohibit unknown names during the deserialization
 * @property emptyValuesAllowed - a control to allow/prohibit empty values: a = # comment
 */
public data class KtomlConf(
    val ignoreUnknownNames: Boolean = false,
    val emptyValuesAllowed: Boolean = true,
    val escapedQuotesInLiteralStringsAllowed: Boolean = false
)
