@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi

/**
 * A config to change parsing behavior.
 * @property ignoreUnknownNames Whether to allow/prohibit unknown names during the deserialization
 * @property allowEmptyValues Whether to allow/prohibit empty values: a = # comment
 * @property allowNullValues Whether to allow/prohibit null values: a = null
 * @property allowEmptyToml Whether empty toml can be processed, if false - will throw an exception
 * @property allowEscapedQuotesInLiteralStrings Whether to allow/prohibit escaping of single quotes in literal strings
 * @property ignoreDefaultValues Whether to ignore default values
 * @property allowTableRedefinition Whether to allow redefining/duplicating tables and keys (e.g.
 *   declaring `[table]` twice or extending an already-defined table with dotted keys). The TOML
 *   spec forbids this, but ktoml has historically accepted it; set to `false` (as [compliant] does)
 *   for spec-conformant rejection.
 */
public data class TomlInputConfig(
    public val ignoreUnknownNames: Boolean = false,
    public val allowEmptyValues: Boolean = true,
    public val allowNullValues: Boolean = true,
    public val allowEmptyToml: Boolean = true,
    public val allowEscapedQuotesInLiteralStrings: Boolean = true,
    public val ignoreDefaultValues: Boolean = false,
    @property:ExperimentalKtomlApi
    public val allowTableRedefinition: Boolean = true,
) {
    /**
     * Retains the constructor and default-argument bridge used by upstream 0.7.1 binaries.
     */
    @Deprecated("Binary compatibility bridge for ktoml 0.7.1", level = DeprecationLevel.HIDDEN)
    public constructor(
        ignoreUnknownNames: Boolean = false,
        allowEmptyValues: Boolean = true,
        allowNullValues: Boolean = true,
        allowEmptyToml: Boolean = true,
        allowEscapedQuotesInLiteralStrings: Boolean = true,
        ignoreDefaultValues: Boolean = false,
    ) : this(
        ignoreUnknownNames,
        allowEmptyValues,
        allowNullValues,
        allowEmptyToml,
        allowEscapedQuotesInLiteralStrings,
        ignoreDefaultValues,
        allowTableRedefinition = true,
    )

    /**
     * Retains upstream 0.7.1 copy signatures while preserving this config's table validation policy.
     *
     * @param ignoreUnknownNames Whether to ignore unknown names during deserialization
     * @param allowEmptyValues Whether to allow empty values
     * @param allowNullValues Whether to allow null values
     * @param allowEmptyToml Whether to allow an empty document
     * @param allowEscapedQuotesInLiteralStrings Whether to allow escaped quotes in literal strings
     * @param ignoreDefaultValues Whether to ignore default values
     * @return A config with the requested values and the original table redefinition policy
     */
    @Suppress("TOO_MANY_PARAMETERS")
    @Deprecated("Binary compatibility bridge for ktoml 0.7.1", level = DeprecationLevel.HIDDEN)
    public fun copy(
        ignoreUnknownNames: Boolean = this.ignoreUnknownNames,
        allowEmptyValues: Boolean = this.allowEmptyValues,
        allowNullValues: Boolean = this.allowNullValues,
        allowEmptyToml: Boolean = this.allowEmptyToml,
        allowEscapedQuotesInLiteralStrings: Boolean = this.allowEscapedQuotesInLiteralStrings,
        ignoreDefaultValues: Boolean = this.ignoreDefaultValues,
    ): TomlInputConfig = TomlInputConfig(
        ignoreUnknownNames,
        allowEmptyValues,
        allowNullValues,
        allowEmptyToml,
        allowEscapedQuotesInLiteralStrings,
        ignoreDefaultValues,
        allowTableRedefinition = this.allowTableRedefinition,
    )

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
                allowEscapedQuotesInLiteralStrings = false,
                allowTableRedefinition = false,
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
