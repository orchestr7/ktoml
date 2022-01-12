@file:Suppress("MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml.exceptions

import kotlinx.serialization.SerializationException

public sealed class TomlDecodingException(message: String) : SerializationException(message)

internal class ParsingException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class InternalDecodingException(message: String) : TomlDecodingException(message)

internal class InternalAstException(message: String) : TomlDecodingException(message)

internal class UnknownNameException(key: String, parent: String?) : TomlDecodingException(
    "Unknown key received: <$key> in scope <$parent>." +
            " Switch TomlConfig.ignoreUnknownNames to true if you would like to skip unknown keys"
)

internal class InvalidEnumValueException(value: String, availableEnumValues: String) : TomlDecodingException(
    "Value <$value> is not a valid" +
            " enum option, permitted choices are: $availableEnumValues"
)

internal class NullValueException(propertyName: String, lineNo: Int) : TomlDecodingException(
    "Non-null property <$propertyName> got a null value in the input. Please check the input (line: <$lineNo>)" +
            " or make the property nullable"
)

internal class CastException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class IllegalTomlTypeException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class MissingRequiredPropertyException(message: String) : TomlDecodingException(message)
