package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.InternalAstException

/**
 * A root node for TOML Abstract Syntax Tree
 */
public class TomlFile(config: TomlConfig = TomlConfig()) : TomlNode(
    "rootNode",
    0,
    config
) {
    override val name: String = "rootNode"

    override fun getNeighbourNodes(): MutableSet<TomlNode> =
            throw InternalAstException("Invalid call to getNeighbourNodes() for TomlFile node")
}
