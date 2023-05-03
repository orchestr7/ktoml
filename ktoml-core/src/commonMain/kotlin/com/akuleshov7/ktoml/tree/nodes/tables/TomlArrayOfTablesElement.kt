/**
 * Array of tables https://toml.io/en/v1.0.0#array-of-tables
 */

package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * This class is used to store elements of array of tables (bucket for key-value records)
 */
public class TomlArrayOfTablesElement(
    lineNo: Int,
    comments: List<String>,
    inlineComment: String
) : TomlNode(
    lineNo,
    comments,
    inlineComment
) {
    override val name: String = EMPTY_TECHNICAL_NODE

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ): Unit = emitter.writeChildren(children, config)

    override fun toString(): String = EMPTY_TECHNICAL_NODE
}
