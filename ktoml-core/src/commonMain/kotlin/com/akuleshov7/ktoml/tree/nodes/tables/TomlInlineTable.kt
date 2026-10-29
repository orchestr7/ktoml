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

                keyValue is TomlArrayOfTablesElement -> {
                    tomlTable.appendChild(keyValue)
                    keyValue.expandNestedInlineArraysOfTables(tomlFileHead)
                }

                // otherwise, it should simply append the keyValue to the parent
                else -> tomlTable.appendChild(keyValue)
            }
        }
        return tomlTable
    }

    /**
     * Expand inline arrays of tables nested inside this array-of-tables element (e.g. the
     * `children = [{ ... }]` in `kids = [{ name = "x", children = [{ ... }] }]`) into proper
     * `[[parent.child]]` tables nested under the element. Otherwise they would be left as raw
     * inline nodes and fail to decode as lists - see issues #360 and #29.
     */
    private fun TomlArrayOfTablesElement.expandNestedInlineArraysOfTables(tomlFileHead: TomlFile) {
        children
            .filterIsInstance<TomlInlineTable>()
            .filter { it.isInlineArrayOfTables() }
            .toList()
            .forEach { nested ->
                children.remove(nested)
                // the parent table is not in the file tree yet, so we attach the expanded
                // table directly to the element instead of going through insertTableToTree
                appendChild(nested.returnTable(tomlFileHead, this))
            }
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
                .splitInlineTableToKeyValue(config.allowEscapedQuotesInLiteralStrings)
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
         *  Split an array of inline tables ([ {..}, {..} ]) into the strings of its
         *  table elements. Splitting happens only on top-level commas: commas nested
         *  inside tables { }, arrays [ ] or quotes " "/' ' are kept as part of the element.
         */
        @Suppress("TOO_LONG_FUNCTION")
        private fun String.splitInlineArrayOfTables(): List<String> {
            val clearedString = this
                .removePrefix("[")
                .removeSuffix("]")
            val result: MutableList<String> = mutableListOf()
            val current = StringBuilder()
            var openQuoteChar: Char? = null
            var bracketDepth = 0

            clearedString.forEach { currentChar ->
                when {
                    // currentChar is inside quotes, so just add it
                    openQuoteChar != null -> {
                        if (currentChar == openQuoteChar) {
                            openQuoteChar = null
                        }
                        current.append(currentChar)
                    }
                    currentChar == '\"' || currentChar == '\'' -> {
                        openQuoteChar = currentChar
                        current.append(currentChar)
                    }
                    currentChar == '{' || currentChar == '[' -> {
                        bracketDepth++
                        current.append(currentChar)
                    }
                    currentChar == '}' || currentChar == ']' -> {
                        bracketDepth--
                        current.append(currentChar)
                    }
                    // a top-level comma separates two inline tables in the array
                    currentChar == ',' && bracketDepth == 0 -> {
                        if (current.isNotBlank()) {
                            result.add(current.toString().trim())
                        }
                        current.clear()
                    }
                    else -> current.append(currentChar)
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
         * nested tables { } and quotes " "/' '. The bracket depth is tracked so that inline
         * tables nested to any depth are split correctly (see issues #29 and #360).
         */
        @Suppress("TOO_LONG_FUNCTION")
        private fun String.splitInlineTableToKeyValue(
            allowEscapedQuotesInLiteralStrings: Boolean,
        ): List<String> {
            val clearedString = this.replaceEscaped(allowEscapedQuotesInLiteralStrings)
            val keyValueList: MutableList<String> = mutableListOf()
            var currentQuoteChar: Char? = null
            var bracketDepth = 0
            var prevIdx = 0

            clearedString.forEachIndexed { idx, ch ->
                when {
                    // inside a quoted string only the matching closing quote is meaningful
                    currentQuoteChar != null -> if (ch == currentQuoteChar) {
                        currentQuoteChar = null
                    }
                    ch == '\'' || ch == '\"' -> currentQuoteChar = ch
                    ch == '[' || ch == '{' -> bracketDepth++
                    ch == ']' || ch == '}' -> bracketDepth--
                    // only a top-level comma (not nested inside [] or {}) separates key-value pairs
                    ch == ',' && bracketDepth == 0 -> {
                        keyValueList.add(this.substring(prevIdx, idx).trim())
                        prevIdx = idx + 1
                    }
                }
            }
            keyValueList.add(this.substring(prevIdx).trim())

            return keyValueList
        }
    }
}
