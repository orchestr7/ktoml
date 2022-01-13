@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml

@Deprecated(
    message = "Class name changed for convention.",
    replaceWith = ReplaceWith("TomlConfig", "com.akuleshov7.ktoml.TomlConfig")
)
public class KtomlConf(
    ignoreUnknownNames: Boolean = false,
    emptyValuesAllowed: Boolean = true,
    escapedQuotesInLiteralStringsAllowed: Boolean = true
) : TomlConfig(ignoreUnknownNames, emptyValuesAllowed, escapedQuotesInLiteralStringsAllowed)

/**
 * @property ignoreUnknownNames - a control to allow/prohibit unknown names during the deserialization
 * @property allowEmptyValues - a control to allow/prohibit empty values: a = # comment
 * @property allowEscapedQuotesInLiteralStrings - a control to allow/prohibit escaping of single quotes in literal strings
 */
public open class TomlConfig(
    public val ignoreUnknownNames: Boolean = false,
    public val allowEmptyValues: Boolean = true,
    public val allowEscapedQuotesInLiteralStrings: Boolean = true
)
