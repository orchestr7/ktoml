package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of null:
 * null, nil, NULL, NIL or empty (key = )
 */
public class TomlNull(lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = "null"

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ) {
        emitter.emitNullValue()
    }
}
