package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of boolean types: key = true | false
 * @property content
 */
public class TomlBoolean internal constructor(
    override var content: Any
) : TomlValue() {
    public constructor(content: String, lineNo: Int) : this(content.toBoolean())

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        emitter.emitValue(content as Boolean)
    }
}
