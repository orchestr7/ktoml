package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.*
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.tables.InlineTableType
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Class for parsing and representing of inline tables: inline = { a = 5, b = 6 , c = 7 }
 *
 * @param lineNo
 * @param comments
 * @param inlineComment
 * @param inlineTableType type of inline table (primitive or array)
 * @property tomlKeyValues The key-value pairs in the inline table
 * @property multiline whether the inline table should be written in multiple lines
 * @property key null when this inline table is part of array of tables
 */
public class TomlInlineTable internal constructor(
    public val key: TomlKey?,
    internal val tomlKeyValues: List<TomlNode>,
    private val inlineTableType: InlineTableType,
    public val multiline: Boolean = false,
    lineNo: Int,
    comments: List<String> = emptyList(),
    inlineComment: String = ""
) : TomlNode(
    lineNo,
    comments,
    inlineComment
) {
    @Suppress("CUSTOM_GETTERS_SETTERS")
    override val name: String get() = key.toString()

    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        config: TomlInputConfig = TomlInputConfig(),
        inlineTableType: InlineTableType = InlineTableType.PRIMITIVE,
        multiline: Boolean = false,
    ) : this(
        TomlKey(keyValuePair.first, lineNo),
        keyValuePair.second.parseInlineTableValue(keyValuePair, lineNo, config),
        inlineTableType,
        multiline,
        lineNo,
        comments,
        inlineComment,
    )

    public fun returnTable(tomlFileHead: TomlFile, currentParentalNode: TomlNode): TomlTable {
        val tomlTable = createTableRoot(currentParentalNode)

        // FixMe: this code duplication can be unified with the logic in TomlParser
        tomlKeyValues.forEach { keyValue ->
            when {
                keyValue is TomlKeyValue && keyValue.key.isDotted -> {
                    // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                    // because table is the same as dotted key
                    val newTableSection = keyValue.createTomlTableFromDottedKey(tomlTable)

                    tomlFileHead
                        .insertTableToTree(newTableSection)
                        .appendChild(keyValue)
                }

                keyValue is TomlInlineTable -> tomlFileHead.insertTableToTree(
                    keyValue.returnTable(tomlFileHead, tomlTable)
                )

                keyValue is TomlArrayOfTablesElement -> tomlTable.appendChild(keyValue)

                // otherwise, it should simply append the keyValue to the parent
                else -> tomlTable.appendChild(keyValue)
            }
        }
        return tomlTable
    }

    public override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        key?.let {
            it.write(emitter)
            emitter.emitPairDelimiter()
        }

        when (inlineTableType) {
            InlineTableType.PRIMITIVE -> emitter.startInlineTable()
            InlineTableType.ARRAY -> emitter.startArray()
        }

        val isMultiline = multiline && inlineTableType == InlineTableType.ARRAY
        if (isMultiline) {
            emitter.indent()
        }
        tomlKeyValues.forEachIndexed { i, pair ->
            if (i > 0) {
                emitter.emitElementDelimiter()
            }
            if (isMultiline) {
                emitter.emitNewLine()
            } else {
                emitter.emitWhitespace()
            }

            pair.write(emitter, config)
        }
        if (isMultiline) {
            emitter.dedent()
        }

        if (!isMultiline) {
            emitter.emitWhitespace()
        }

        writeEnding(emitter, isMultiline)
    }

    private fun writeEnding(emitter: TomlEmitter, isMultiline: Boolean) {
        when (inlineTableType) {
            InlineTableType.PRIMITIVE -> emitter.endInlineTable()
            InlineTableType.ARRAY -> {
                if (isMultiline) {
                    emitter.emitNewLine()
                }
                emitter.emitIndent()
                    .endArray()
            }
        }
    }

    private fun createTableRoot(currentParentalNode: TomlNode): TomlTable = TomlTable(
        TomlKey(
            when (currentParentalNode) {
                is TomlTable -> currentParentalNode.fullTableKey.keyParts + key!!.keyParts
                is TomlArrayOfTablesElement -> (currentParentalNode.parent as TomlTable)
                    .fullTableKey.keyParts + key!!.keyParts
                else -> listOf(name)
            },
        ),
        lineNo,
        type = if (this.isInlineArrayOfTables()) {
            TableType.ARRAY
        } else {
            TableType.PRIMITIVE
        },
        comments,
        inlineComment
    )

    private fun isInlineArrayOfTables(): Boolean = tomlKeyValues.any { it is TomlArrayOfTablesElement }

    public companion object {
        private fun String.parseInlineTableValue(
            keyValuePair: Pair<String, String>,
            lineNo: Int,
            config: TomlInputConfig
        ): List<TomlNode> {
            if (this.startsWithIgnoreAllWhitespaces("[{")) {
                return parseInlineArrayOfTables(keyValuePair, lineNo, config)
            }

            val inlineTableValueString = this.trimCurlyBraces().trim()
            val parsedList = inlineTableValueString
                .also {
                    if (it.isEmpty()) {
                        return listOf(TomlStubEmptyNode(lineNo))
                    }
                    if (it.endsWith(",")) {
                        throw ParseException(
                            "Trailing commas are not permitted in inline tables: [$this] ", lineNo
                        )
                    }
                }
                .splitInlineTableToKeyValue(config.allowEscapedQuotesInLiteralStrings, lineNo)
                .map {
                    it.parseTomlKeyValue(lineNo, comments = emptyList(), inlineComment = "", config)
                }

            return parsedList
        }

        private fun String.parseInlineArrayOfTables(
            keyValuePair: Pair<String, String>,
            lineNo: Int,
            config: TomlInputConfig
        ): List<TomlNode> {
            val inlineTableValues = this.splitInlineArrayOfTables()

            return inlineTableValues.map { tableValue ->
                val inlineTableValues = tableValue.parseInlineTableValue(
                    keyValuePair.first to tableValue.trim(),
                    lineNo,
                    config,
                )

                TomlArrayOfTablesElement(lineNo, emptyList(), "").also { arrayOfTableElement ->
                    inlineTableValues.forEach { value ->
                        arrayOfTableElement.appendChild(value)
                    }
                }
            }
        }

        /**
         *  Split by "}," - but skip all whitespaces between '}' and ','
         *  Also ignore characters inside strings and keep closing '}' in each value
         */
        @Suppress("TOO_LONG_FUNCTION")
        private fun String.splitInlineArrayOfTables(): List<String> {
            val clearedString = this
                .removePrefix("[")
                .removeSuffix("]")
            val result: MutableList<String> = mutableListOf()
            val current = StringBuilder()
            var openQuoteChar: Char? = null
            var isCurlyBracesFound = false

            clearedString.forEach { currentChar ->
                // currentChar is inside quotes, so just add it
                if (openQuoteChar != null && currentChar != openQuoteChar) {
                    current.append(currentChar)
                    return@forEach
                }

                when (currentChar) {
                    openQuoteChar -> {
                        openQuoteChar = null
                        current.append(currentChar)
                    }

                    '\"', '\'' -> {
                        openQuoteChar = currentChar
                        current.append(currentChar)
                    }

                    '}' -> {
                        isCurlyBracesFound = true
                        current.append(currentChar)
                    }

                    ',' -> if (isCurlyBracesFound) {
                        // comma between inline tables
                        isCurlyBracesFound = false
                        result.add(current.toString().trim())
                        current.clear()
                    } else {
                        // comma between inline table values
                        current.append(currentChar)
                    }

                    else -> if (!isCurlyBracesFound) {
                        current.append(currentChar)
                    }
                }
            }

            // 'current' is blank when array has trailing comma
            if (current.isNotBlank()) {
                result.add(current.toString().trim())
            }
            return result
        }

        /**
         * That's basically split(",") function, but we ignore all commas inside arrays [ ],
         * nested tables { } and quotes " "/' '
         */
        @Suppress("TOO_LONG_FUNCTION")
        private fun String.splitInlineTableToKeyValue(
            allowEscapedQuotesInLiteralStrings: Boolean,
            lineNo: Int,
        ): List<String> {
            val clearedString = this.replaceEscaped(allowEscapedQuotesInLiteralStrings)
            val keyValueList: MutableList<String> = mutableListOf()
            var isLastAdded = false
            var currentQuoteChar: Char? = null
            var prevIdx = 0
            var curIdx = 0

            while (curIdx < clearedString.length) {
                val ch = clearedString[curIdx]
                if (ch == ',' && currentQuoteChar == null) {
                    keyValueList.add(this.substring(prevIdx, curIdx).trim())
                    prevIdx = curIdx + 1
                } else if (currentQuoteChar == null && (ch == '[' || ch == '{')) {
                    val closeBracketIdx = this.indexOfNextOutsideQuotes(
                        allowEscapedQuotesInLiteralStrings = allowEscapedQuotesInLiteralStrings,
                        searchChar = getCloseBracket(ch, lineNo),
                        startIndex = curIdx,
                    )
                    keyValueList.add(this.substring(prevIdx, closeBracketIdx + 1).trim())
                    val nextCommaIdx = this.indexOfNextOutsideQuotes(
                        allowEscapedQuotesInLiteralStrings = allowEscapedQuotesInLiteralStrings,
                        searchChar = ',',
                        startIndex = closeBracketIdx,
                    )
                    if (nextCommaIdx == -1) {
                        isLastAdded = true
                        break
                    }
                    prevIdx = nextCommaIdx + 1
                    curIdx = nextCommaIdx + 1
                } else if (ch == '\'' || ch == '\"') {
                    if (currentQuoteChar == null) {
                        currentQuoteChar = ch
                    } else if (currentQuoteChar == ch) {
                        currentQuoteChar = null
                    }
                }
                curIdx++
            }
            if (!isLastAdded) {
                keyValueList.add(this.substring(prevIdx, this.length).trim())
            }

            return keyValueList
        }

        private fun getCloseBracket(openBracket: Char, lineNo: Int): Char = if (openBracket == '[') {
            ']'
        } else if (openBracket == '{') {
            '}'
        } else {
            throw ParseException("Invalid open bracket: $openBracket, should never happen", lineNo)
        }
    }
}
