package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of Arbitrary 64-bit signed integers: key = 1
 * @property content
 */
public class TomlLong internal constructor(
    override var content: Any,
    lineNo: Int
) : TomlValue(lineNo) {
    public constructor(content: String, lineNo: Int) : this(content.toLong(), lineNo)

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ) {
        emitter.emitValue(content as Long)
    }
}
