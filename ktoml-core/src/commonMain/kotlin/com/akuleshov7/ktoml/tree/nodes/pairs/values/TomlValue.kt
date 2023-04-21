/**
 * All representations of TOML value nodes are stored in this file
 */

package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Base class for all nodes that represent values
 */
public sealed class TomlValue {
    @Deprecated(message = "lineNo is deprecated. Will be removed in next releases.")
    public val lineNo: Int = 0
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

    @Deprecated(
        message = "The multiline parameter overload is deprecated, use the multiline" +
                " property on supported types instead. Will be removed in next releases.",
        replaceWith = ReplaceWith("write(emitter, config)")
    )
    public fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig = TomlOutputConfig(),
        multiline: Boolean
    ): Unit = write(emitter, config)

    /**
     * Writes this value to the specified [emitter].
     *
     * @param emitter
     * @param config
     */
    public abstract fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig = TomlOutputConfig()
    )
}
