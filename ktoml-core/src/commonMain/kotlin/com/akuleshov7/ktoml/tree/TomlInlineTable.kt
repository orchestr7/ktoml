package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.parseTomlKeyValue
import com.akuleshov7.ktoml.parsers.trimCurlyBraces
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Class for parsing and representing of inline tables: inline = { a = 5, b = 6 , c = 7 }
 * @property name
 * @property tomlKeyValues The key-value pairs in the inline table
 */
public class TomlInlineTable internal constructor(
    content: String,
    lineNo: Int,
    override val name: String,
    private val tomlKeyValues: List<TomlNode>,
    comments: List<String> = emptyList(),
    inlineComment: String = "",
    config: TomlInputConfig = TomlInputConfig()
) : TomlNode(
    content,
    lineNo,
    comments,
    inlineComment,
    config
) {
    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        config: TomlInputConfig = TomlInputConfig()
    ) : this(
        "${keyValuePair.first} = ${keyValuePair.second}",
        lineNo,
        keyValuePair.first,
        keyValuePair.second.parseInlineTableValue(lineNo, config),
        comments,
        inlineComment,
        config
    )

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        config: TomlConfig
    ) : this(
        keyValuePair,
        lineNo,
        comments,
        inlineComment,
        config.input
    )

    public fun returnTable(tomlFileHead: TomlFile, currentParentalNode: TomlNode): TomlTable {
        val tomlTable = TomlTablePrimitive(
            "[${if (currentParentalNode is TomlTable) "${currentParentalNode.fullTableName}." else ""}$name]",
            lineNo,
            comments,
            inlineComment,
            config
        )

        // FixMe: this code duplication can be unified with the logic in TomlParser
        tomlKeyValues.forEach { keyValue ->
            when {
                keyValue is TomlKeyValue && keyValue.key.isDotted -> {
                    // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                    // because table is the same as dotted key
                    val newTableSection = keyValue.createTomlTableFromDottedKey(tomlTable, config)

                    tomlFileHead
                        .insertTableToTree(newTableSection)
                        .appendChild(keyValue)
                }

                keyValue is TomlInlineTable -> tomlFileHead.insertTableToTree(
                    keyValue.returnTable(tomlFileHead, tomlTable)
                )

                // otherwise, it should simply append the keyValue to the parent
                else -> tomlTable.appendChild(keyValue)

            }
        }
        return tomlTable
    }

    public override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ) {
        val key = TomlKey(name, 0)

        key.write(emitter)

        emitter.emitPairDelimiter()
            .startInlineTable()

        tomlKeyValues.forEachIndexed { i, pair ->
            if (i > 0) {
                emitter.emitElementDelimiter()
            }

            emitter.emitWhitespace()

            pair.write(emitter, config)
        }

        emitter.emitWhitespace()
            .endInlineTable()
    }

    public companion object {
        private fun String.parseInlineTableValue(
            lineNo: Int,
            config: TomlInputConfig
        ): List<TomlNode> {
            val parsedList = this
                .trimCurlyBraces()
                .trim()
                .also {
                    if (it.endsWith(",")) {
                        throw ParseException(
                            "Trailing commas are not permitted in inline tables: [$this] ", lineNo
                        )
                    }
                }
                .split(",")
                .map { it.parseTomlKeyValue(lineNo, comments = emptyList(), inlineComment = "", config) }

            return parsedList
        }
    }
}
