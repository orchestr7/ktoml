package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.takeBeforeComment
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.pairs.values.*
import com.akuleshov7.ktoml.writers.TomlEmitter

private typealias ValueCreator = (String, Int) -> TomlValue

/**
 * Interface that contains all common methods that are used in KeyValue nodes
 */
internal interface TomlKeyValue {
    var key: TomlKey
    val value: TomlValue
    val lineNo: Int
    val comments: List<String>
    val inlineComment: String

    @Deprecated(
        message = "The config parameter was removed. Will be removed in future releases.",
        replaceWith = ReplaceWith("createTomlTableFromDottedKey(parentNode)")
    )
    fun createTomlTableFromDottedKey(
        parentNode: TomlNode,
        config: TomlInputConfig = TomlInputConfig()
    ): TomlTablePrimitive = createTomlTableFromDottedKey(parentNode)

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
    fun createTomlTableFromDottedKey(parentNode: TomlNode): TomlTablePrimitive {
        // for a key: a.b.c it will be [a, b]
        val syntheticTablePrefix = this.key.keyParts.dropLast(1)
        // creating new key with the last dot-separated fragment
        val realKeyWithoutDottedPrefix = TomlKey(key.last(), lineNo)
        // updating current KeyValue with this key
        this.key = realKeyWithoutDottedPrefix
        // tables should contain fully qualified name, so we need to add parental name
        val parentalPrefix = if (parentNode is TomlTable) parentNode.fullTableKey.keyParts else emptyList()
        // and creating a new table that will be created from dotted key
        return TomlTablePrimitive(
            TomlKey(parentalPrefix + syntheticTablePrefix),
            lineNo,
            comments,
            inlineComment,
            true
        )
    }

    fun isMultiline() = false

    fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        key.write(emitter)

        emitter.emitPairDelimiter()

        value.write(emitter, config)
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
    val pair = takeBeforeComment(config.allowEscapedQuotesInLiteralStrings)

    // searching for an equals sign that should be placed main part of the string (not in the comment)
    val firstEqualsSign = pair.indexOfFirst { it == '=' }

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
    val key = pair.substring(0, firstEqualsSign).trim()
    val value = pair.substring(firstEqualsSign + 1).trim()

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
    // ===== special values
    "+inf", "inf" -> TomlDouble(Double.POSITIVE_INFINITY)
    "-inf" -> TomlDouble(Double.NEGATIVE_INFINITY)
    "-nan", "+nan", "nan", "-NaN", "+NaN", "NaN" -> TomlDouble(Double.NaN)
    // ===== null values
    "null", "nil", "NULL", "NIL", "" -> if (config.allowNullValues) {
        TomlNull(lineNo)
    } else {
        throw ParseException("Null values are not allowed", lineNo)
    }
    // ===== boolean vales
    "true", "false" -> TomlBoolean(this, lineNo)
    // ===== strings
    else -> when (this.first()) {
        '\'' -> TomlLiteralString(this, lineNo, config)
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
