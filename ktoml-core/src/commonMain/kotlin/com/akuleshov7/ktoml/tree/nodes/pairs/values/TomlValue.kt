/**
 * All representations of TOML value nodes are stored in this file
 */

package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Base class for all nodes that represent values
 * @property lineNo - line number of original file
 */
public sealed class TomlValue(public val lineNo: Int) {
    public abstract var content: Any

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlOutputConfig instead. Will be removed in next releases.",
        replaceWith = ReplaceWith(
            "write(emitter, config, multiline)",
            "com.akuleshov7.ktoml.TomlOutputConfig"
        )
    )
    public fun write(
        emitter: TomlEmitter,
        config: TomlConfig,
        multiline: Boolean = false
    ): Unit = write(emitter, config.output, multiline)

    /**
     * Writes this value to the specified [emitter], optionally writing the value
     * [multiline] (if supported by the value type).
     *
     * @param emitter
     * @param config
     * @param multiline
     */
    public abstract fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig = TomlOutputConfig(),
        multiline: Boolean = false
    )
}
