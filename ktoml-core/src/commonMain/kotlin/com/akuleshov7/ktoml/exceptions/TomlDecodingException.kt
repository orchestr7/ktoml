@file:Suppress("MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml.exceptions

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames

public sealed class TomlDecodingException(message: String) : SerializationException(message)

internal class ParseException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class InternalDecodingException(message: String) : TomlDecodingException(message)

internal class InternalAstException(message: String) : TomlDecodingException(message)

internal class UnknownNameException(key: String, parent: String?) : TomlDecodingException(
    "Unknown key received: <$key> in scope <$parent>." +
            " Switch TomlConfig.ignoreUnknownNames to true if you would like to skip unknown keys"
)

@OptIn(ExperimentalSerializationApi::class)
internal class InvalidEnumValueException(value: String, enumSerialDescriptor: SerialDescriptor) : TomlDecodingException(
    "Value <$value> is not a valid enum option." +
            " Permitted choices are: ${enumSerialDescriptor.elementNames.sorted().joinToString(", ")}"
)

internal class NullValueException(propertyName: String, lineNo: Int) : TomlDecodingException(
    "Non-null property <$propertyName> got a null value in the input." +
            " Please check the input (line: <$lineNo>) or make the property nullable"
)

internal class CastException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class IllegalTypeException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class MissingRequiredPropertyException(message: String) : TomlDecodingException(message)
