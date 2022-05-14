package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.findBeginningOfTheComment
import com.akuleshov7.ktoml.writers.TomlEmitter

private typealias ValueCreator = (String, Int) -> TomlValue

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
     * @param config
     * @return the table that is parsed from a dotted key
     */
    fun createTomlTableFromDottedKey(parentNode: TomlNode, config: TomlInputConfig = TomlInputConfig()): TomlTablePrimitive {
        // for a key: a.b.c it will be [a, b]
        val syntheticTablePrefix = this.key.keyParts.dropLast(1)
        // creating new key with the last dot-separated fragment
        val realKeyWithoutDottedPrefix = TomlKey(key.content, lineNo)
        // updating current KeyValue with this key
        this.key = realKeyWithoutDottedPrefix
        // tables should contain fully qualified name, so we need to add parental name
        val parentalPrefix = if (parentNode is TomlTable) "${parentNode.fullTableName}." else ""
        // and creating a new table that will be created from dotted key
        return TomlTablePrimitive(
            "[$parentalPrefix${syntheticTablePrefix.joinToString(".")}]",
            lineNo,
            config,
            true
        )
    }

    fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ) {
        key.write(emitter)

        emitter.emitPairDelimiter()

        value.write(emitter, config, multiline)
    }
}

/**
 * in TOML specification arrays have a very complex functionality (trailing commas, values of different types, etc)
 * so we need a separate parser for these arrays. This: the string with the content
 *
 * @param lineNo
 * @param config
 * @return an object of type Array that was parsed from string
 */
public fun String.parseList(lineNo: Int, config: TomlInputConfig): TomlArray = TomlArray(this, lineNo, config)

/**
 * parse and split a string in a key-value format
 *
 * @param lineNo
 * @param config
 * @return a resulted key-value pair
 * @throws ParseException
 */
public fun String.splitKeyValue(lineNo: Int, config: TomlInputConfig = TomlInputConfig()): Pair<String, String> {
    // finding the index of the last quote, if no quotes are found, then use the length of the string
    val closingQuoteIndex = listOf(
        this.lastIndexOf("\""),
        this.lastIndexOf("\'"),
        this.lastIndexOf("\"\"\"")
    ).filterNot { it == -1 }.maxOrNull() ?: 0

    // finding the index of a commented part of the string
    // search starts goes from the closingQuoteIndex to the end of the string
    val firstHash = this.findBeginningOfTheComment(closingQuoteIndex)

    // searching for an equals sign that should be placed main part of the string (not in the comment)
    val firstEqualsSign = this.substring(0, firstHash).indexOfFirst { it == '=' }

    // equals sign not found in the string
    if (firstEqualsSign == -1) {
        throw ParseException(
            "Incorrect format of Key-Value pair (missing equals sign)." +
                    " Should be <key = value>, but was: $this." +
                    " If you wanted to define table - use brackets []",
            lineNo
        )
    }

    // k = v # comment -> key is `k`, value is `v`
    val key = this.substring(0, firstEqualsSign).trim()
    val value = this.substring(firstEqualsSign + 1, firstHash).trim()

    return Pair(
        key.checkNotEmpty("key", this, config, lineNo),
        value.checkNotEmpty("value", this, config, lineNo)
    )
}

/**
 * factory method for parsing content of the string to the proper Node type
 * (for date -> TomlDate, string -> TomlString, e.t.c)
 *
 * @param lineNo
 * @param config
 * @return parsed TomlNode value
 */
public fun String.parseValue(lineNo: Int, config: TomlInputConfig): TomlValue = when (this) {
    // ===== null values
    "null", "nil", "NULL", "NIL", "" -> if (config.allowNullValues) {
        TomlNull(lineNo)
    } else {
        throw ParseException("Null values are not allowed", lineNo)
    }
    // ===== boolean vales
    "true", "false" -> TomlBoolean(this, lineNo)
    else -> when (this[0]) {
        // ===== literal strings
        '\'' -> if (this.startsWith("'''")) {
            TomlBasicString(this, lineNo)
        } else {
            TomlLiteralString(this, lineNo, config)
        }
        // ===== basic strings
        '\"' -> TomlBasicString(this, lineNo)
        else -> tryParseValue<NumberFormatException>(lineNo, ::TomlLong)  // ==== integer values
            ?: tryParseValue<NumberFormatException>(lineNo, ::TomlDouble)  // ===== float values
            ?: tryParseValue<IllegalArgumentException>(lineNo, ::TomlDateTime)  // ===== date-time values
            ?: TomlBasicString(this, lineNo)  // ===== fallback strategy in case of invalid value
    }
}

private inline fun <reified E : Throwable> String.tryParseValue(
    lineNo: Int,
    transform: ValueCreator
): TomlValue? = try {
    transform(this, lineNo)
} catch (e: Throwable) {
    if (e is E) null else throw e
}

/**
 * method to get proper value from content to get key or value
 */
private fun String.checkNotEmpty(
    log: String,
    content: String,
    config: TomlInputConfig = TomlInputConfig(),
    lineNo: Int
): String =
        this.also {
            // key should never be empty, but the value can be empty (and treated as null)
            // see the discussion: https://github.com/toml-lang/toml/issues/30
            if ((!config.allowEmptyValues || log == "key") && it.isBlank()) {
                throw ParseException(
                    "Incorrect format of Key-Value pair. It has empty $log: $content",
                    lineNo
                )
            }
        }
