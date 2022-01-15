package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlConfig

/**
 * A [TomlEmitter] implementation that writes to a [StringBuilder].
 */
internal class TomlStringEmitter(
    private val stringBuilder: StringBuilder,
    config: TomlConfig
) : TomlEmitter(config) {
    override fun emit(fragment: String) {
        stringBuilder.append(fragment)
    }

    override fun emit(fragment: Char) {
        stringBuilder.append(fragment)
    }
}
