package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * this is a hack to cover empty TOML tables that have missing key-values
 * According the spec: "Empty tables are allowed and simply have no key/value pairs within them."
 *
 * Instances of this stub will be added as children to such parsed tables
 *
 * @param lineNo
 */
public class TomlStubEmptyNode(lineNo: Int) : TomlNode(
    lineNo,
    comments = emptyList(),
    inlineComment = ""
) {
    override val name: String = EMPTY_TECHNICAL_NODE

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        // Nothing to write in stub nodes.
    }

    override fun toString(): String = EMPTY_TECHNICAL_NODE
}
