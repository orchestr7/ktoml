package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
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
    comments: List<String>,
    inlineComment: String,
    override val name: String,
    config: TomlConfig = TomlConfig()
) : TomlNode(
    key,
    value,
    lineNo,
    comments,
    inlineComment,
    config
), TomlKeyValue {
    // adaptor for a string pair of key-value
    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        config: TomlConfig = TomlConfig()
    ) : this(
        TomlKey(keyValuePair.first, lineNo),
        keyValuePair.second.parseValue(lineNo, config),
        lineNo,
        comments,
        inlineComment,
        TomlKey(keyValuePair.first, lineNo).content
    )

    public override fun write(
        emitter: TomlEmitter,
        config: TomlConfig,
        multiline: Boolean
    ): Unit = super.write(emitter, config, multiline)
}
