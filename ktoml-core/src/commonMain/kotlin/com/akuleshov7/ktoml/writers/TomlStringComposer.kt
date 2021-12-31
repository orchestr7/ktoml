package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.KtomlConf

internal class TomlStringComposer(
    private val stringBuilder: StringBuilder,
    ktomlConf: KtomlConf
) : AbstractTomlComposer(ktomlConf) {
    override fun emit(fragment: String) {
        stringBuilder.append(fragment)
    }

    override fun emit(fragment: Char) {
        stringBuilder.append(fragment)
    }

    override fun emit(fragment1: String, fragment2: String) {
        stringBuilder.append(fragment1)
                     .append(fragment2)
    }

    override fun emit(fragment1: String, fragment2: String, fragment3: String) {
        stringBuilder.append(fragment1)
                     .append(fragment2)
                     .append(fragment3)
    }

    override fun emit(fragment1: Char, fragment2: String, fragment3: Char) {
        stringBuilder.append(fragment1)
                     .append(fragment2)
                     .append(fragment3)
    }
}