package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlOutputConfig

/**
 * Emits TOML fragments through callbacks supplied by an optional adapter module.
 *
 * @param config output configuration
 * @param emitString callback for a string fragment
 * @param emitChar callback for a character fragment
 */
internal class TomlCallbackEmitter(
    config: TomlOutputConfig,
    private val emitString: (String) -> Unit,
    private val emitChar: (Char) -> Unit,
) : TomlEmitter(config) {
    override fun emit(fragment: String): TomlEmitter {
        emitString(fragment)
        return this
    }

    override fun emit(fragment: Char): TomlEmitter {
        emitChar(fragment)
        return this
    }
}
