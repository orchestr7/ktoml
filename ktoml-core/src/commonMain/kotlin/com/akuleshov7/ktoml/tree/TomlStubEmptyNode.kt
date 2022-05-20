package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * this is a hack to cover empty TOML tables that have missing key-values
 * According the spec: "Empty tables are allowed and simply have no key/value pairs within them."
 *
 * Instances of this stub will be added as children to such parsed tables
 */
public class TomlStubEmptyNode(lineNo: Int, config: TomlConfig = TomlConfig()) : TomlNode(
    EMPTY_TECHNICAL_NODE,
    lineNo,
    comments = emptyList(),
    inlineComment = "",
    config
) {
    override val name: String = EMPTY_TECHNICAL_NODE

    override fun write(
        emitter: TomlEmitter,
        config: TomlConfig,
        multiline: Boolean
    ) {
        // Nothing to write in stub nodes.
    }
}
