/**
 * Common class for tables
 */

package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.takeBeforeComment
import com.akuleshov7.ktoml.parsers.trimBrackets
import com.akuleshov7.ktoml.parsers.trimDoubleBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.writers.TomlEmitter
import com.akuleshov7.ktoml.writers.TomlStringEmitter
import kotlin.jvm.JvmStatic

/**
 * Abstract class to represent all types of tables: primitive/arrays/etc.
 * @property lineNo - line number
 * @property isSynthetic
 * @property fullTableKey
 */
@Suppress("COMMENT_WHITE_SPACE")
public abstract class TomlTable(
    public var fullTableKey: TomlKey,
    override val lineNo: Int,
    comments: List<String>,
    inlineComment: String,
    public var isSynthetic: Boolean = false
) : TomlNode(
    lineNo,
    comments,
    inlineComment
) {
    @Deprecated(
        message = "fullTableName was replaced with fullTableKey; will be removed in future releases.",
        replaceWith = ReplaceWith("fullTableKey.toString()")
    )
    @Suppress("NO_CORRESPONDING_PROPERTY", "CUSTOM_GETTERS_SETTERS")
    public var fullTableName: String
        get() = fullTableKey.toString()
        set(value) {
            fullTableKey = TomlKey(value, lineNo)
        }

    // list of tables (including sub-tables) that are included in this table  (e.g.: {a, a.b, a.b.c} in a.b.c)
    public var tablesList: List<String> = fullTableKey.keyParts.runningReduce { prev, cur -> "$prev.$cur" }

    // short table name (only the name without parental prefix, like a - it is used in decoder and encoder)
    public override val name: String = fullTableKey.keyParts.last().trimQuotes()
    public abstract val type: TableType

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        content: String,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        config: TomlConfig,
        isSynthetic: Boolean = false
    ) : this(
        TomlKey(content.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings).trim('[', ']'), lineNo),
        // Todo: Temporary workaround until this constructor is removed
        lineNo,
        comments,
        inlineComment,
        isSynthetic
    )

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        // Todo: Option to explicitly define super tables?
        // Todo: Support dotted key-value pairs (i.e. a.b.c.d = 7)

        val firstChild = children.first()

        if (isExplicit(firstChild) && type == TableType.PRIMITIVE) {
            emitter.writeHeader(config)

            if (inlineComment.isNotEmpty()) {
                emitter.emitComment(inlineComment, inline = true)
            }

            if (firstChild !is TomlStubEmptyNode) {
                emitter.emitNewLine()
            }

            emitter.indent()

            emitter.writeChildren(children, config)

            emitter.dedent()
        } else {
            emitter.writeChildren(children, config)
        }
    }

    protected abstract fun TomlEmitter.writeHeader(
        config: TomlOutputConfig
    )

    /**
     * Determines whether the table should be explicitly defined. Synthetic tables
     * are implicit except when:
     *  - the first child is a pair, and there are other children (to exclude
     *    dotted pairs)
     *  - the table is empty
     */
    private fun isExplicit(
        firstChild: TomlNode
    ) = if (!isSynthetic) {
        true
    } else {
        when (firstChild) {
            is TomlStubEmptyNode -> true
            is TomlKeyValue,
            is TomlInlineTable -> children.size > 1
            else -> false
        }
    }

    override fun toString(): String = buildString {
        val config = TomlOutputConfig()
        val emitter = TomlStringEmitter(this, config)

        emitter.writeHeader(config)
    }

    public companion object {
        @JvmStatic
        protected fun parseSection(
            content: String,
            lineNo: Int,
            isArray: Boolean
        ): TomlKey {
            val close = if (isArray) "]]" else "]"

            val lastIndexOfBrace = content.lastIndexOf(close)
            if (lastIndexOfBrace == -1) {
                throw ParseException(
                    "Invalid Tables provided: $content." +
                            " It has missing closing bracket${if (isArray) "s" else ""}: '$close'", lineNo
                )
            }
            val sectionFromContent = content.takeBeforeComment(false).trim().let {
                if (isArray) {
                    it.trimDoubleBrackets()
                } else {
                    it.trimBrackets()
                }
            }
                .trim()

            if (sectionFromContent.isBlank()) {
                throw ParseException("Incorrect blank name for ${if (isArray) "array of tables" else "table"}: $content", lineNo)
            }

            return TomlKey(sectionFromContent, lineNo)
        }
    }
}

/**
 * Special Enum that is used in a logic related to insertion of tables to AST
 */
public enum class TableType {
    ARRAY,
    PRIMITIVE,
    ;
}
