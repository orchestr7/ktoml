package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * class for parsing and storing simple single value types in AST
 * @property lineNo
 * @property key
 * @property value
 * @property name
 */
public class TomlKeyValuePrimitive(
    override var key: TomlKey,
    override val value: TomlValue,
    override val lineNo: Int,
    override val name: String,
    config: TomlInputConfig = TomlInputConfig()
) : TomlNode(
    key,
    value,
    lineNo,
    config
), TomlKeyValue {
    // adaptor for a string pair of key-value
    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        config: TomlInputConfig = TomlInputConfig()
    ) : this(
        TomlKey(keyValuePair.first, lineNo),
        keyValuePair.second.parseValue(lineNo, config),
        lineNo,
        TomlKey(keyValuePair.first, lineNo).content
    )

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead."
    )
    public constructor(
        key: TomlKey,
        value: TomlValue,
        lineNo: Int,
        name: String,
        config: TomlConfig
    ) : this(
        key,
        value,
        lineNo,
        name,
        config.input
    )

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead."
    )
    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        config: TomlConfig
    ) : this(
        keyValuePair,
        lineNo,
        config.input
    )

    public override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ): Unit = super<TomlKeyValue>.write(emitter, config, multiline)
}
