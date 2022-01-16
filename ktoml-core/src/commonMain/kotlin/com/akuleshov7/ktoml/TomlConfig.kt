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
) : TomlConfig(
    ignoreUnknownNames,
    emptyValuesAllowed,
    escapedQuotesInLiteralStringsAllowed
)

/**
 * @property ignoreUnknownNames - a control to allow/prohibit unknown names during the deserialization
 * @property allowEmptyValues - a control to allow/prohibit empty values: a = # comment
 * @property allowEscapedQuotesInLiteralStrings - a control to allow/prohibit escaping of single quotes in literal strings
 * @property indentation - the number of spaces in the indents for the serialization
 * @property allowEmptyToml - controls if empty toml can be processed, if false - will throw an exception
 */
public open class TomlConfig(
    public val ignoreUnknownNames: Boolean = false,
    public val allowEmptyValues: Boolean = true,
    public val allowEscapedQuotesInLiteralStrings: Boolean = true,
    public val indentation: Indentation = Indentation.FOUR_SPACES,
    public val allowEmptyToml: Boolean = true,
) {
    /**
     * @property value - string with indents, used for the formatting of serialization
     */
    public enum class Indentation(public val value: String) {
        FOUR_SPACES("    "),
        NONE(""),
        TAB("\t"),
        TWO_SPACES("  "),
        ;
    }
}
