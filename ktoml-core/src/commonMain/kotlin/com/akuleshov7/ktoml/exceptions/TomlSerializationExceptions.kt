/**
 * Internal exceptions used in the project
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml.exceptions

import kotlinx.serialization.SerializationException

public sealed class TomlDecodingException(message: String) : SerializationException(message)

internal class TomlParsingException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class InternalDecodingException(message: String) : TomlDecodingException(message)

internal class InternalAstException(message: String) : TomlDecodingException(message)

internal class UnknownNameException(keyField: String, parent: String?) : TomlDecodingException(
    "Unknown key received: <$keyField> in scope <$parent>." +
            " Switch TomlConfig.ignoreUnknownNames to true if you would like to skip unknown keys"
)

internal class InvalidEnumValueException(value: String, availableEnumValues: String) : TomlDecodingException(
    "Value <$value> is not a valid" +
            " enum option, permitted choices are: $availableEnumValues"
)

internal class NonNullValueException(property: String, lineNo: Int) : TomlDecodingException(
    "Nonnull property <$property> got a null value in the input. Please check the input (line: <$lineNo>)" +
            " or make the property nullable"
)

internal class TomlCastException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class IllegalTomlTypeException(message: String, lineNo: Int) : TomlDecodingException("Line $lineNo: $message")

internal class MissingRequiredFieldException(message: String) : TomlDecodingException(message)
