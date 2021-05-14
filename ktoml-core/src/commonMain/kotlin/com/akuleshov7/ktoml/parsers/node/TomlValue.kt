package com.akuleshov7.ktoml.parsers.node

class TomlKey(val content: String, val lineNo: Int)

sealed class TomlValue(val content: String, val lineNo: Int) {
    abstract var value: Any
}

class TomlString(content: String, lineNo: Int) : TomlValue(content, lineNo) {
    override var value: Any = content
}

class TomlInt(content: String, lineNo: Int) : TomlValue(content, lineNo) {
    override var value: Any = content.toInt()
}

class TomlFloat(content: String, lineNo: Int) : TomlValue(content, lineNo) {
    override var value: Any = content.toFloat()
}

class TomlBoolean(content: String, lineNo: Int) : TomlValue(content, lineNo) {
    override var value: Any = content.toBoolean()
}

class TomlNull(lineNo: Int) : TomlValue("null", lineNo) {
    override var value: Any = "null"
}
