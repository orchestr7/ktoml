@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml.exceptions

import com.akuleshov7.ktoml.utils.closestEnumName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames

/**
 * @param message
 */
public sealed class TomlDecodingException(message: String) : SerializationException(message)

/**
 * @param message
 * @param lineNo
 */
internal open class ParseException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

/**
 * @param invalid
 * @param lineNo
 */
internal class UnknownEscapeSymbolsException(invalid: String, lineNo: Int) : ParseException(
    "According to TOML documentation unknown" +
            " escape symbols are not allowed. Please check: [\\$invalid]",
    lineNo
)

/**
 * @param message
 */
internal class InternalDecodingException(message: String) : TomlDecodingException(message +
        " It's an internal error - you can do nothing with it, please report it to https://github.com/akuleshov7/ktoml/")

/**
 * @param message
 */
internal class InternalAstException(message: String) : TomlDecodingException(message)

/**
 * @param key
 * @param parent
 */
internal class UnknownNameException(key: String, parent: String?) : TomlDecodingException(
    "Unknown key received: <$key> in scope <$parent>." +
            " Switch the configuration option: 'TomlConfig.ignoreUnknownNames'" +
            " to true if you would like to skip unknown keys"
)

/**
 * @param value
 * @param enumSerialDescriptor
 * @param lineNo
 */
@OptIn(ExperimentalSerializationApi::class)
internal class InvalidEnumValueException(
    value: String,
    enumSerialDescriptor: SerialDescriptor,
    lineNo: Int
) : TomlDecodingException(
    "Line $lineNo: value <$value> is not a valid enum option." +
            " Did you mean <${enumSerialDescriptor.elementNames.closestEnumName(value)}>?" +
            " Permitted choices are: ${enumSerialDescriptor.elementNames.sorted().joinToString(", ")}."
)

/**
 * @param propertyName
 * @param lineNo
 */
internal class NullValueException(propertyName: String, lineNo: Int) : TomlDecodingException(
    "Non-null property <$propertyName> got a null value in the input." +
            " Please check the input (line: <$lineNo>) or make the property nullable"
)

/**
 * @param message
 * @param lineNo
 */
internal class IllegalTypeException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

/**
 * @param message
 */
internal class MissingRequiredPropertyException(message: String) : TomlDecodingException(message)

/**
 * @param message
 */
internal class UnsupportedDecoderException(message: String) : TomlDecodingException(message)
