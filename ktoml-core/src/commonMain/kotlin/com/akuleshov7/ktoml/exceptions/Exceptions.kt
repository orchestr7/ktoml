package com.akuleshov7.ktoml.exceptions

class TomlParsingException(message: String, lineNo: Int) : Exception("Line $lineNo: $message")

class InternalParsingException(message: String, lineNo: Int) : Exception("Line $lineNo: $message")

class InternalDecodingException(message: String) : Exception(message)

class InternalAstException(message: String) : Exception(message)

class UnknownNameDecodingException(keyField: String, parent: String?) : Exception(
    "Unknown key received: <$keyField> in scope <$parent>." +
            " Pass 'ignoreUnknownNames' option if you would like to skip unknown keys"
)

class InvalidEnumValueException(value: String, availableEnumValues: String) : Exception(
    "Value $value is not a valid" +
            " option, permitted choices are: $availableEnumValues"
)

class MissingRequiredFieldException(message: String) : Exception(message)
