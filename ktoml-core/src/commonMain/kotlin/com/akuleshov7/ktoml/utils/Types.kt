/**
 * File contains enums with minimum and maximum values of corresponding types
 */

package com.akuleshov7.ktoml.utils

/**
 * @property min
 * @property max
 */
public enum class IntegerLimitsEnum(public val min: Long, public val max: Long) {
    BYTE(Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong()),
    INT(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()),
    LONG(Long.MIN_VALUE, Long.MAX_VALUE),
    SHORT(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong()),
    ;
    // Unsigned values are not supported now, and I think
    // that will not be supported, because TOML spec says the following:
    // Arbitrary 64-bit signed integers (from −2^63 to 2^63−1) should be accepted and handled losslessly.
    // If an integer cannot be represented losslessly, an error must be thrown.
    // U_BYTE(UByte.MIN_VALUE.toLong(), UByte.MAX_VALUE.toLong()),
    // U_SHORT(UShort.MIN_VALUE.toLong(), UShort.MAX_VALUE.toLong()),
    // U_INT(UInt.MIN_VALUE.toLong(), UInt.MAX_VALUE.toLong()),
    // U_LONG(ULong.MIN_VALUE.toLong(), ULong.MAX_VALUE.toLong()),
}

/**
 * @property min
 * @property max
 */
public enum class FloatingPointLimitsEnum(public val min: Double, public val max: Double) {
    DOUBLE(-Double.MAX_VALUE, Double.MAX_VALUE),
    FLOAT(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble()),
    ;
}
