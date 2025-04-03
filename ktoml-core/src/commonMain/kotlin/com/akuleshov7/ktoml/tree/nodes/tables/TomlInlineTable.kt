package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.indexOfNextOutsideQuotes
import com.akuleshov7.ktoml.parsers.parseTomlKeyValue
import com.akuleshov7.ktoml.parsers.replaceEscaped
import com.akuleshov7.ktoml.parsers.trimCurlyBraces
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Class for parsing and representing of inline tables: inline = { a = 5, b = 6 , c = 7 }
 *
 * @param lineNo
 * @param comments
 * @param inlineComment
 * @property tomlKeyValues The key-value pairs in the inline table
 * @property key
 */
public class TomlInlineTable internal constructor(
    public val key: TomlKey,
    internal val tomlKeyValues: List<TomlNode>,
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
        config: TomlInputConfig = TomlInputConfig()
    ) : this(
        TomlKey(keyValuePair.first, lineNo),
        keyValuePair.second.parseInlineTableValue(lineNo, config),
        lineNo,
        comments,
        inlineComment
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
        val tomlTable = TomlTable(
            TomlKey(
                if (currentParentalNode is TomlTable) {
                    currentParentalNode.fullTableKey.keyParts + key.keyParts
                } else {
                    listOf(name)
                }
            ),
            lineNo,
            type = TableType.PRIMITIVE,
            comments,
            inlineComment
        )

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
                .map { it.parseTomlKeyValue(lineNo, comments = emptyList(), inlineComment = "", config) }

            return parsedList
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
