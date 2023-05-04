/**
 * All representations of TOML value nodes are stored in this file
 */

package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Base class for all nodes that represent values
 */
public sealed class TomlValue {
    public abstract var content: Any

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
