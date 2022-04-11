/**
 * Common class for tables
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Abstract class to represent all types of tables: primitive/arrays/etc.
 * @property content - raw string name of the table
 * @property lineNo - line number
 * @property config - toml configuration
 * @property isSynthetic
 */
public abstract class TomlTable(
    override val content: String,
    override val lineNo: Int,
    override val config: TomlConfig = TomlConfig(),
    public val isSynthetic: Boolean = false
) : TomlNode(
    content,
    lineNo,
    config
) {
    public abstract var fullTableName: String
    public abstract var tablesList: List<String>
    public abstract val type: TableType

    override fun write(
        emitter: TomlEmitter,
        config: TomlConfig,
        multiline: Boolean
    ) {
        // Todo: Option to explicitly define super tables?

        // Determines whether the table should be explicitly defined. Synthetic
        // tables are implicit except when:
        //  - the first child is a pair, and there are other children (to exclude
        //    dotted pairs)
        //  - the table is empty
        fun isExplicit(
            children: List<TomlNode>
        ) = if (!isSynthetic) {
            true
        } else {
            when (children.first()) {
                is TomlStubEmptyNode -> true
                is TomlKeyValue,
                is TomlInlineTable -> children.size > 1
                else -> false
            }
        }

        val children = children

        val key = TomlKey(fullTableName, 0)

        if (isExplicit(children) && type == TableType.PRIMITIVE) {
            emitter.writeHeader(key, config)

            if (children.isNotEmpty()) {
                emitter.emitNewLine()
            }

            emitter.indent()

            emitter.writeChildren(key, children, config, multiline)

            emitter.dedent()
        } else {
            emitter.writeChildren(key, children, config, multiline)
        }
    }

    protected abstract fun TomlEmitter.writeChildren(
        headerKey: TomlKey,
        children: List<TomlNode>,
        config: TomlConfig,
        multiline: Boolean
    )

    protected abstract fun TomlEmitter.writeHeader(
        headerKey: TomlKey,
        config: TomlConfig
    )
}

/**
 * Special Enum that is used in a logic related to insertion of tables to AST
 */
public enum class TableType {
    ARRAY,
    PRIMITIVE,
    ;
}
