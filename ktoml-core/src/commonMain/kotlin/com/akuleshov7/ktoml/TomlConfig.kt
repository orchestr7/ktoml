@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml

/**
 * A config to change parsing behavior.
 * @property ignoreUnknownNames Whether to allow/prohibit unknown names during the deserialization
 * @property allowEmptyValues Whether to allow/prohibit empty values: a = # comment
 * @property allowNullValues Whether to allow/prohibit null values: a = null
 * @property allowEmptyToml Whether empty toml can be processed, if false - will throw an exception
 * @property allowEscapedQuotesInLiteralStrings Whether to allow/prohibit escaping of single quotes in literal strings
 * @property ignoreDefaultValues Whether to ignore default values
 */
public data class TomlInputConfig(
    public val ignoreUnknownNames: Boolean = false,
    public val allowEmptyValues: Boolean = true,
    public val allowNullValues: Boolean = true,
    public val allowEmptyToml: Boolean = true,
    public val allowEscapedQuotesInLiteralStrings: Boolean = true,
    public val ignoreDefaultValues: Boolean = false,
) {
    public companion object {
        /**
         * Creates a config populated with values compliant with the TOML spec.
         *
         * @param ignoreUnknownNames Whether to allow/prohibit unknown names during the deserialization
         * @param allowEmptyToml Whether empty toml can be processed, if false - will throw an exception
         * @return A TOML spec-compliant input config
         */
        public fun compliant(
            ignoreUnknownNames: Boolean = false,
            allowEmptyToml: Boolean = true
        ): TomlInputConfig =
            TomlInputConfig(
                ignoreUnknownNames,
                allowEmptyValues = false,
                allowNullValues = false,
                allowEmptyToml,
                allowEscapedQuotesInLiteralStrings = false
            )
    }
}

/**
 * A config to change writing behavior.
 *
 * @property indentation The number of spaces in the indents for the serialization
 * @property allowEscapedQuotesInLiteralStrings Whether to allow/prohibit escaping of single quotes in literal strings
 * @property ignoreNullValues Whether to ignore null values
 * @property ignoreDefaultValues Whether to ignore default values
 * @property explicitTables Whether to explicitly define parent tables
 */
public data class TomlOutputConfig(
    public val indentation: TomlIndentation = TomlIndentation.FOUR_SPACES,
    public val allowEscapedQuotesInLiteralStrings: Boolean = true,
    public val ignoreNullValues: Boolean = true,
    public val ignoreDefaultValues: Boolean = false,
    public val explicitTables: Boolean = false,
) {
    public companion object {
        /**
         * Creates a config populated with values compliant with the TOML spec.
         *
         * @param indentation The number of spaces in the indents for the serialization
         * @param ignoreDefaultValues Whether to ignore default values
         * @param explicitTables Whether to explicitly define parent tables
         * @return A TOML spec-compliant output config
         */
        public fun compliant(
            indentation: TomlIndentation = TomlIndentation.FOUR_SPACES,
            ignoreDefaultValues: Boolean = false,
            explicitTables: Boolean = false,
        ): TomlOutputConfig =
            TomlOutputConfig(
                indentation,
                allowEscapedQuotesInLiteralStrings = false,
                ignoreNullValues = true,
                ignoreDefaultValues,
                explicitTables
            )
    }
}

/**
 * @property value The indent string, used for the formatting during serialization
 */
public enum class TomlIndentation(public val value: String) {
    FOUR_SPACES("    "),
    NONE(""),
    TAB("\t"),
    TWO_SPACES("  "),
    ;
}
