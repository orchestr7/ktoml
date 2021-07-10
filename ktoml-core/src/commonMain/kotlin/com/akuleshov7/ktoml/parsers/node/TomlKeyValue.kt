package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.exceptions.TomlParsingException

/**
 * Interface that contains all common methods that are used in KeyValue nodes
 */
interface TomlKeyValue {
    var key: TomlKey
    val value: TomlValue
    val lineNo: Int

    /**
     * in TOML specification arrays have a very complex functionality (trailing commas, values of different types, etdc)
     * so we need a separate parser for these arrays
     *
     * @param contentStr
     * @param lineNo
     * @return an object of type Array that was parsed from string
     */
    fun parseList(contentStr: String, lineNo: Int) = TomlArray(contentStr, lineNo)

    /**
     * this is a small hack to support dotted keys
     * in case we have the following key: a.b.c = "val" we will simply create a new table:
     *  [a.b]
     *     c = "val"
     * and we will let our Table mechanism to do everything for us
     *
     * @param parentNode
     * @return the table that is parsed from a dotted key
     */
    fun createTomlTableFromDottedKey(parentNode: TomlNode): TomlTable {
        // for a key: a.b.c it will be [a, b]
        val syntheticTablePrefix = this.key.keyParts.dropLast(1)
        // creating new key with the last dot-separated fragment
        val realKeyWithoutDottedPrefix = TomlKey(key.content, lineNo)
        // updating current KeyValue with this key
        this.key = realKeyWithoutDottedPrefix
        // tables should contain fully qualified name, so we need to add parental name
        val parentalPrefix = if (parentNode is TomlTable) "${parentNode.fullTableName}." else ""
        // and creating a new table that will be created from dotted key
        return TomlTable(
            "[$parentalPrefix${syntheticTablePrefix.joinToString(".")}]",
            lineNo,
            true
        )
    }
}

/**
 * parse and split a string in a key-value format
 *
 * @param lineNo
 * @param ktomlConf
 * @return a resulted key-value pair
 * @throws TomlParsingException
 */
fun String.splitKeyValue(lineNo: Int, ktomlConf: KtomlConf): Pair<String, String> {
    // FixMe: need to cover a case, when '#' symbol is used inside the string ( a = "# hi") (supported by the spec)
    val keyValue = this.substringBefore("#")
        .split("=")
        .map { it.trim() }

    if (keyValue.size != 2) {
        throw TomlParsingException(
            "Incorrect format of Key-Value pair." +
                    " Should be <key = value>, but was: $this." +
                    " If you wanted to define table - use brackets []",
            lineNo
        )
    }

    val keyStr = keyValue.getKeyValuePart("key", 0, this, ktomlConf, lineNo)
    val valueStr = keyValue.getKeyValuePart("value", 1, this, ktomlConf, lineNo)
    return Pair(keyStr, valueStr)
}

/**
 * method to get proper value from content to get key or value
 *
 * @param log
 * @param index
 * @param content
 * @param ktomlConf
 * @param lineNo
 */
fun List<String>.getKeyValuePart(
    log: String,
    index: Int,
    content: String,
    ktomlConf: KtomlConf,
    lineNo: Int) =
        this[index].trim().also {
            // key should never be empty, but the value can be empty (and treated as null)
            // see the discussion: https://github.com/toml-lang/toml/issues/30
            if ((!ktomlConf.emptyValuesAllowed || index == 0) && it.isBlank()) {
                throw TomlParsingException(
                    "Incorrect format of Key-Value pair. It has empty $log: $content",
                    lineNo
                )
            }
        }

/**
 * factory method for parsing content of the string to the proper Node type
 * (for date -> TomlDate, string -> TomlString, e.t.c)
 *
 * @param lineNo
 * @return parsed TomlNode value
 */
fun String.parseValue(lineNo: Int) = when (this) {
    "null", "nil", "NULL", "NIL", "" -> TomlNull(lineNo)
    "true", "false" -> TomlBoolean(this, lineNo)
    else -> if (this.startsWith("\"")) {
        TomlBasicString(this, lineNo)
    } else {
        try {
            TomlInt(this, lineNo)
        } catch (e: NumberFormatException) {
            try {
                TomlDouble(this, lineNo)
            } catch (e: NumberFormatException) {
                TomlBasicString(this, lineNo)
            }
        }
    }
}
