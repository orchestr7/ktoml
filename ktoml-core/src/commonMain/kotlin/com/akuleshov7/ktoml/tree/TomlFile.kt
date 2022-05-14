package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * A root node for TOML Abstract Syntax Tree
 */
public class TomlFile(config: TomlInputConfig = TomlInputConfig()) : TomlNode(
    "rootNode",
    0,
    config
) {
    override val name: String = "rootNode"

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead."
    )
    public constructor(config: TomlConfig) : this(config.input)

    override fun getNeighbourNodes(): MutableList<TomlNode> =
            throw InternalAstException("Invalid call to getNeighbourNodes() for TomlFile node")

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ): Unit =
            emitter.writeChildren(
                children,
                config,
                multiline
            )
}
