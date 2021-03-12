package com.akuleshov7.ktoml.exceptions

class TomlParsingException(message:String, lineNo: Int): Exception("Line $lineNo: $message")
