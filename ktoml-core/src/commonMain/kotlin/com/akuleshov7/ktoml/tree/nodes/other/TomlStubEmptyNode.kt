package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * this is a hack to cover empty TOML tables that have missing key-values
 * According the spec: "Empty tables are allowed and simply have no key/value pairs within them."
 *
 * Instances of this stub will be added as children to such parsed tables
 */
public class TomlStubEmptyNode(lineNo: Int, config: TomlInputConfig = TomlInputConfig()) : TomlNode(
    EMPTY_TECHNICAL_NODE,
    lineNo,
    comments = emptyList(),
    inlineComment = "",
    config
) {
    override val name: String = EMPTY_TECHNICAL_NODE

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(lineNo: Int, config: TomlConfig) : this(lineNo, config.input)

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ) {
        // Nothing to write in stub nodes.
    }
}
