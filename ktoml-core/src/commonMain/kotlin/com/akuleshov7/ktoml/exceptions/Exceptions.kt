package com.akuleshov7.ktoml.exceptions

open class KtomlException(message: String) : Exception(message)

class TomlParsingException(message: String, lineNo: Int) : KtomlException("Line $lineNo: $message")

class InternalParsingException(message: String, lineNo: Int) : KtomlException("Line $lineNo: $message")

class InternalDecodingException(message: String) : KtomlException(message)

class InternalAstException(message: String) : KtomlException(message)

class UnknownNameDecodingException(keyField: String, parent: String?) : KtomlException(
    "Unknown key received: <$keyField> in scope <$parent>." +
            " Pass 'ignoreUnknownNames' option if you would like to skip unknown keys"
)

class InvalidEnumValueException(value: String, availableEnumValues: String) : KtomlException(
    "Value $value is not a valid" +
            " option, permitted choices are: $availableEnumValues"
)

class NonNullableValueException(field: String, lineNo: Int) : KtomlException(
    "Not-nullable field <$field> got a null value in the input. Please check the input (line: <$lineNo>)" +
            " or make the field nullable"
)

class MissingRequiredFieldException(message: String) : KtomlException(message)
