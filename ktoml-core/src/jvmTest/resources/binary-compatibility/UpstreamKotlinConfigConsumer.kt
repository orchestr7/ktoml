package com.akuleshov7.ktoml.binarycompatibility.fixtures

import com.akuleshov7.ktoml.TomlInputConfig

fun constructWithDefaults(): TomlInputConfig =
    TomlInputConfig(ignoreUnknownNames = true, allowEmptyValues = false)

fun copyWithDefaults(config: TomlInputConfig): TomlInputConfig = config.copy(ignoreUnknownNames = true)

fun copyUnchanged(config: TomlInputConfig): TomlInputConfig = config.copy()
