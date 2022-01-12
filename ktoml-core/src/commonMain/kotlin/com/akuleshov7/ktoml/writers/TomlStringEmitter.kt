package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.KtomlConf

/**
 * A [TomlEmitter] implementation that writes to a [StringBuilder].
 */
internal class TomlStringEmitter(
    private val stringBuilder: StringBuilder,
    ktomlConf: KtomlConf
) : TomlEmitter(ktomlConf) {
    override fun emit(fragment: String) {
        stringBuilder.append(fragment)
    }

    override fun emit(fragment: Char) {
        stringBuilder.append(fragment)
    }
}
