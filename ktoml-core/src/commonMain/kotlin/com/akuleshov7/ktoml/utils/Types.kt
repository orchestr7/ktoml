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
    CHAR(Char.MIN_VALUE.code.toLong(), Char.MAX_VALUE.code.toLong()),
    INT(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()),
    LONG(Long.MIN_VALUE, Long.MAX_VALUE),
    SHORT(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong()),
    ;
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

/**
 * @property min
 * @property max
 */
public enum class UnsignedIntegerLimitsEnum(public val min: ULong, public val max: ULong) {
    U_BYTE(UByte.MIN_VALUE.toULong(), UByte.MAX_VALUE.toULong()),
    U_SHORT(UShort.MIN_VALUE.toULong(), UShort.MAX_VALUE.toULong()),
    U_INT(UInt.MIN_VALUE.toULong(), UInt.MAX_VALUE.toULong()),
    U_LONG(ULong.MIN_VALUE, ULong.MAX_VALUE),
    ;
}
