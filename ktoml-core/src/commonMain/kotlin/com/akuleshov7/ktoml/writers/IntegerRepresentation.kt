package com.akuleshov7.ktoml.writers

/**
 * How a TOML integer should be represented during encoding.
 *
 * @property prefix The prefix, if any, signalling this representation in TOML.
 * @property radix The radix or base number of the representation.
 */
@Suppress("MAGIC_NUMBER")
public enum class IntegerRepresentation(
    public val prefix: String = "",
    public val radix: Int = 10,
) {
    /**
     * A binary number prefixed with `0b`.
     */
    BINARY("0b", 2),

    /**
     * A decimal number.
     */
    DECIMAL,

    /**
     * A grouped decimal number, such as `1_000_000`.
     */
    GROUPED,

    /**
     * A hexadecimal number prefixed with `0x`.
     */
    HEX("0x", 16),

    /**
     * An octal number prefixed with `0o`.
     */
    OCTAL("0o", 8),
    ;
}
