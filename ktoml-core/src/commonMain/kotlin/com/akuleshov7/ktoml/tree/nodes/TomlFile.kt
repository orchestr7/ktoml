package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * A root node for TOML Abstract Syntax Tree
 */
@Suppress("EMPTY_PRIMARY_CONSTRUCTOR")
public class TomlFile() : TomlNode(
    lineNo = 0,
    comments = emptyList(),
    inlineComment = ""
) {
    override val name: String = "rootNode"

    override fun getNeighbourNodes(): MutableList<TomlNode> =
        throw InternalAstException("Invalid call to getNeighbourNodes() for TomlFile node")

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ): Unit = emitter.writeChildren(children, config)

    override fun toString(): String = "rootNode"
}
