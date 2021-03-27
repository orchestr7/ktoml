package com.akuleshov7.ktoml.exceptions

class TomlParsingException(message:String, lineNo: Int): Exception("Line $lineNo: $message")

class InternalDecodingException(message:String): Exception(message)

class UnknownNameDecodingException(message:String): Exception(message)
