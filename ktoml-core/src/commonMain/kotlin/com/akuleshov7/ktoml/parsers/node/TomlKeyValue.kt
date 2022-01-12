package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.exceptions.ParsingException

/**
 * Interface that contains all common methods that are used in KeyValue nodes
 */
internal interface TomlKeyValue {
    var key: TomlKey
    val value: TomlValue
    val lineNo: Int

    /**
     * this is a small hack to support dotted keys
     * in case we have the following key: a.b.c = "val" we will simply create a new table:
     *  [a.b]
     *     c = "val"
     * and we will let our Table mechanism to do everything for us
     *
     * @param parentNode
     * @param ktomlConf
     * @return the table that is parsed from a dotted key
     */
    fun createTomlTableFromDottedKey(parentNode: TomlNode, ktomlConf: KtomlConf = KtomlConf()): TomlTable {
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
            ktomlConf,
            true
        )
    }
}

/**
 * in TOML specification arrays have a very complex functionality (trailing commas, values of different types, etdc)
 * so we need a separate parser for these arrays. This: the string with the content
 *
 * @param lineNo
 * @param ktomlConf
 * @return an object of type Array that was parsed from string
 */
public fun String.parseList(lineNo: Int, ktomlConf: KtomlConf): TomlArray = TomlArray(this, lineNo, ktomlConf)

/**
 * parse and split a string in a key-value format
 *
 * @param lineNo
 * @param ktomlConf
 * @return a resulted key-value pair
 * @throws ParsingException
 */
public fun String.splitKeyValue(lineNo: Int, ktomlConf: KtomlConf = KtomlConf()): Pair<String, String> {
    // finding the index of the last quote, if no quotes are found, then use the length of the string
    val closingQuoteIndex = listOf(
        this.lastIndexOf("\""),
        this.lastIndexOf("\'"),
        this.lastIndexOf("\"\"\"")
    ).filterNot { it == -1 }.maxOrNull() ?: 0

    // finding the index of a commented part of the string
    // search starts goes from the closingQuoteIndex to the end of the string
    val firstHash = (closingQuoteIndex until this.length).filter { this[it] == '#' }.minOrNull() ?: this.length

    // searching for an equals sign that should be placed main part of the string (not in the comment)
    val firstEqualsSign = this.substring(0, firstHash).indexOfFirst { it == '=' }

    // equals sign not found in the string
    if (firstEqualsSign == -1) {
        throw ParsingException(
            "Incorrect format of Key-Value pair (missing equals sign)." +
                    " Should be <key = value>, but was: $this." +
                    " If you wanted to define table - use brackets []",
            lineNo
        )
    }

    // aaaa = bbbb # comment -> aaaa
    val key = this.substring(0, firstEqualsSign).trim()
    // aaaa = bbbb # comment -> bbbb
    val value = this.substring(firstEqualsSign + 1, firstHash).trim()

    return Pair(
        key.checkNotEmpty("key", this, ktomlConf, lineNo),
        value.checkNotEmpty("value", this, ktomlConf, lineNo)
    )
}

/**
 * factory method for parsing content of the string to the proper Node type
 * (for date -> TomlDate, string -> TomlString, e.t.c)
 *
 * @param lineNo
 * @param ktomlConf
 * @return parsed TomlNode value
 */
public fun String.parseValue(lineNo: Int, ktomlConf: KtomlConf): TomlValue = when (this) {
    // ===== null values
    "null", "nil", "NULL", "NIL", "" -> TomlNull(lineNo)
    // ===== boolean vales
    "true", "false" -> TomlBoolean(this, lineNo)
    else -> when (this[0]) {
        // ===== literal strings
        '\'' -> if (this.startsWith("'''")) TomlBasicString(this, lineNo) else TomlLiteralString(this, lineNo, ktomlConf)
        // ===== basic strings
        '\"' -> TomlBasicString(this, lineNo)
        else ->
            try {
                // ===== integer values
                TomlLong(this, lineNo)
            } catch (e: NumberFormatException) {
                try {
                    // ===== float values
                    TomlDouble(this, lineNo)
                } catch (e: NumberFormatException) {
                    // ===== fallback strategy in case of invalid value
                    TomlBasicString(this, lineNo)
                }
            }
    }
}

/**
 * method to get proper value from content to get key or value
 */
private fun String.checkNotEmpty(
    log: String,
    content: String,
    ktomlConf: KtomlConf = KtomlConf(),
    lineNo: Int
): String =
        this.also {
            // key should never be empty, but the value can be empty (and treated as null)
            // see the discussion: https://github.com/toml-lang/toml/issues/30
            if ((!ktomlConf.emptyValuesAllowed || log == "key") && it.isBlank()) {
                throw ParsingException(
                    "Incorrect format of Key-Value pair. It has empty $log: $content",
                    lineNo
                )
            }
        }
