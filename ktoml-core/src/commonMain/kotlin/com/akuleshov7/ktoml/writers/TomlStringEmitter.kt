package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlOutputConfig

/**
 * A [TomlEmitter] implementation that writes to a [StringBuilder].
 */
internal class TomlStringEmitter(
    private val stringBuilder: StringBuilder,
    config: TomlOutputConfig
) : TomlEmitter(config) {
    override fun emit(fragment: String): TomlEmitter {
        stringBuilder.append(fragment)

        return this
    }

    override fun emit(fragment: Char): TomlEmitter {
        stringBuilder.append(fragment)

        return this
    }
}
