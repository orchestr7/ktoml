package com.akuleshov7.ktoml.writers

/**
 * How a TOML integer should be represented during encoding.
 *
 * @property BINARY A binary number prefixed with `0b`.
 * @property DECIMAL A decimal number.
 * @property GROUPED A grouped decimal number, such as `1_000_000`. Todo: Add support.
 * @property HEX A hexadecimal number prefixed with `0x`.
 * @property OCTAL An octal number prefixed with `0o`.
 */
public enum class IntegerRepresentation {
    BINARY,
    DECIMAL,
    GROUPED,
    HEX,
    OCTAL,
    ;
}
