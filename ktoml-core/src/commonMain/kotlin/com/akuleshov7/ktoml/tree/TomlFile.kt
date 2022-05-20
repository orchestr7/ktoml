package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * A root node for TOML Abstract Syntax Tree
 */
public class TomlFile(config: TomlConfig = TomlConfig()) : TomlNode(
    "rootNode",
    0,
    comments = emptyList(),
    inlineComment = "",
    config
) {
    override val name: String = "rootNode"

    override fun getNeighbourNodes(): MutableList<TomlNode> =
            throw InternalAstException("Invalid call to getNeighbourNodes() for TomlFile node")

    override fun write(
        emitter: TomlEmitter,
        config: TomlConfig,
        multiline: Boolean
    ): Unit =
            emitter.writeChildren(
                children,
                config,
                multiline
            )
}
