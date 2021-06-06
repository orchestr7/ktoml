package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.ParserConf

interface TomlKeyValue {
    var key: TomlKey
    val value: TomlValue
    val lineNo: Int

    /**
     * in TOML arrays have a very complex functionality (trailing commas, values of different types, e.t.c)
     */
    fun parseList(contentStr: String, lineNo: Int): TomlArray {
        return TomlArray(contentStr, lineNo)
    }

    /**
     * parsing content of the string to the proper Node type (for date -> TomlDate, string -> TomlString, e.t.c)
     */
    fun parseValue(contentStr: String, lineNo: Int): TomlValue {
        return when (contentStr) {
            "null", "nil", "NULL", "NIL", "" -> TomlNull(lineNo)
            "true", "false" -> TomlBoolean(contentStr, lineNo)
            else -> try {
                TomlInt(contentStr, lineNo)
            } catch (e: NumberFormatException) {
                try {
                    TomlFloat(contentStr, lineNo)
                } catch (e: NumberFormatException) {
                    TomlString(contentStr, lineNo)
                }
            }
        }
    }

    /**
     * this is a small hack to support dotted keys
     * in case we have the following key: a.b.c = "val" we will simply create a new table:
     *  [a.b]
     *     c = "val"
     * and we will let our Table mechanism to do everything for us
     */
    fun createTomlTableFromDottedKey(parentNode: TomlNode): TomlTable {
        // for a key: a.b.c it will be [a, b]
        val syntheticTablePrefix = this.key.keyParts.dropLast(1)
        // creating new key with the last dor-separated fragment
        val realKeyWithoutDottedPrefix = TomlKey(key.keyParts.last(), lineNo)
        // updating current KeyValue with this key
        this.key = realKeyWithoutDottedPrefix
        // tables should contain fully qualified name, so we need to add parental name
        val parentalPrefix = if (parentNode is TomlTable) "${parentNode.fullTableName}." else ""
        // and creating a new table that will be created from dotted key
        val newTable = TomlTable(
            "[$parentalPrefix${syntheticTablePrefix.joinToString(".")}]",
            lineNo,
            true
        )

        return newTable
    }
}

fun String.splitKeyValue(lineNo: Int, parserConf: ParserConf): Pair<String, String> {
    // FixMe: need to cover a case, when '#' symbol is used inside the string ( a = "# hi") (supported by the spec)
    val keyValue = this.substringBefore("#")
        .split("=")
        .map { it.trim() }

    if (keyValue.size != 2) {
        throw TomlParsingException(
            "Incorrect format of Key-Value pair. Should be <key = value>, but was: $this",
            lineNo
        )
    }

    val keyStr = keyValue.getKeyValuePart("key", 0, this, parserConf, lineNo)
    val valueStr = keyValue.getKeyValuePart("value", 1, this, parserConf, lineNo)
    return Pair(keyStr, valueStr)
}

/**
 * method to get proper value from content to get key or value
 */
fun List<String>.getKeyValuePart(log: String, index: Int, content: String, parserConf: ParserConf, lineNo: Int) =
    this[index].trim().also {
        // key should never be empty, but the value can be empty (and treated as null)
        // see the discussion: https://github.com/toml-lang/toml/issues/30
        if ((!parserConf.emptyValuesAllowed || index == 0) && it.isBlank()) {
            throw TomlParsingException(
                "Incorrect format of Key-Value pair. It has empty $log: $content",
                lineNo
            )
        }
    }
