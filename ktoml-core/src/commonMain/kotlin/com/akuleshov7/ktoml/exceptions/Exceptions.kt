/**
 * Internal exceptions used in the project
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml.exceptions

public sealed class KtomlException(message: String) : Exception(message)

internal class TomlParsingException(message: String, lineNo: Int) : KtomlException("Line $lineNo: $message")

internal class InternalDecodingException(message: String) : KtomlException(message)

internal class InternalAstException(message: String) : KtomlException(message)

internal class UnknownNameDecodingException(keyField: String, parent: String?) : KtomlException(
    "Unknown key received: <$keyField> in scope <$parent>." +
            " Pass 'ignoreUnknownNames' option if you would like to skip unknown keys"
)

internal class InvalidEnumValueException(value: String, availableEnumValues: String) : KtomlException(
    "Value <$value> is not a valid" +
            " enum option, permitted choices are: $availableEnumValues"
)

internal class NonNullableValueException(field: String, lineNo: Int) : KtomlException(
    "Not-nullable field <$field> got a null value in the input. Please check the input (line: <$lineNo>)" +
            " or make the field nullable"
)

internal class TomlCastException(message: String, lineNo: Int) : KtomlException(message)

internal class MissingRequiredFieldException(message: String) : KtomlException(message)
